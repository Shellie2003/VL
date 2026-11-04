package com.example.cutpilot

import com.example.cutpilot.input.PointerProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerProcessorTest {

    @Test
    fun slowMotionIsNotLost() {
        // 100 events of 0.3 px used to be truncated to 0 each time.
        val p = PointerProcessor()
        var total = 0
        repeat(100) {
            total += PointerProcessor.unpackFirst(p.process(0.3f, 0f, 8, 1f, false, 1f))
        }
        assertEquals(30, total)
    }

    @Test
    fun negativeMotionKeepsRemainder() {
        val p = PointerProcessor()
        var total = 0
        repeat(10) { total += PointerProcessor.unpackSecond(p.process(0f, -0.55f, 8, 1f, false, 1f)) }
        assertEquals(-5, total)
    }

    @Test
    fun accelerationCurveIsMonotonic() {
        val slow = PointerProcessor.accelerationFactor(0.5f, 0f, 8)
        val medium = PointerProcessor.accelerationFactor(8f, 0f, 8)
        val fast = PointerProcessor.accelerationFactor(40f, 0f, 8)
        assertTrue(slow < 1f)
        assertTrue(slow < medium && medium < fast)
        assertTrue(fast < 2.3f)
    }

    @Test
    fun packingRoundTrips() {
        val v = PointerProcessor.pack(-123456, 789)
        assertEquals(-123456, PointerProcessor.unpackFirst(v))
        assertEquals(789, PointerProcessor.unpackSecond(v))
    }

    @Test
    fun scrollDirection() {
        val p = PointerProcessor()
        // Fingers move down (dy > 0): traditional scroll = scroll down (negative).
        assertTrue(PointerProcessor.unpackFirst(p.processScroll(10f, 0f, 1f, natural = false)) < 0)
        p.reset()
        assertTrue(PointerProcessor.unpackFirst(p.processScroll(10f, 0f, 1f, natural = true)) > 0)
    }
}
