package com.example.cutpilot.input

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Turns raw finger deltas (screen pixels) into integer host-pointer deltas.
 *
 *  - Sub-pixel accumulation: the fractional part of every movement is carried over,
 *    so slow, precise motions are never lost (the previous `toInt()` truncation
 *    swallowed every movement below one pixel per event).
 *  - Smooth velocity-based acceleration curve: below ~1x for slow, precise work and
 *    up to ~2.2x for fast flicks across the screen.
 *  - Precision ("sniper") multiplier applied on top.
 */
class PointerProcessor {

    private var remX = 0f
    private var remY = 0f
    private var remScrollV = 0f
    private var remScrollH = 0f

    /** Packed result: high 32 bits = dx, low 32 bits = dy (avoids allocating a Pair at 240 Hz). */
    fun process(
        rawDx: Float,
        rawDy: Float,
        dtMs: Long,
        sensitivity: Float,
        acceleration: Boolean,
        precisionMultiplier: Float
    ): Long {
        val gain = sensitivity * precisionMultiplier *
            (if (acceleration) accelerationFactor(rawDx, rawDy, dtMs) else 1f)
        val fx = rawDx * gain + remX
        val fy = rawDy * gain + remY
        val ix = fx.toInt() // truncates toward zero, remainder keeps the sign
        val iy = fy.toInt()
        remX = fx - ix
        remY = fy - iy
        return pack(ix, iy)
    }

    /**
     * Two-finger scroll: finger pixels -> 1/120 notch units.
     * [natural] = content follows the fingers (like a laptop touchpad).
     */
    fun processScroll(rawDy: Float, rawDx: Float, speed: Float, natural: Boolean): Long {
        val dir = if (natural) 1f else -1f
        val fv = rawDy * dir * SCROLL_UNITS_PER_PX * speed + remScrollV
        val fh = -rawDx * dir * SCROLL_UNITS_PER_PX * speed + remScrollH
        val iv = fv.toInt()
        val ih = fh.toInt()
        remScrollV = fv - iv
        remScrollH = fh - ih
        return pack(iv, ih)
    }

    fun reset() {
        remX = 0f
        remY = 0f
        remScrollV = 0f
        remScrollH = 0f
    }

    companion object {
        /** ~40 px of finger travel = one wheel notch. */
        const val SCROLL_UNITS_PER_PX = 3f

        fun accelerationFactor(dx: Float, dy: Float, dtMs: Long): Float {
            val dist = sqrt(dx * dx + dy * dy)
            val speed = dist / dtMs.coerceIn(4L, 50L) // px per ms
            val s = speed.toDouble().pow(1.3).toFloat()
            return 0.6f + 1.6f * s / (s + 1.5f)
        }

        fun pack(a: Int, b: Int): Long = (a.toLong() shl 32) or (b.toLong() and 0xFFFFFFFFL)
        fun unpackFirst(v: Long): Int = (v shr 32).toInt()
        fun unpackSecond(v: Long): Int = v.toInt()

    }
}
