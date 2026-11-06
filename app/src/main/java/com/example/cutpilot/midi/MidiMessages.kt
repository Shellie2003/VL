package com.example.cutpilot.midi

/** Pure MIDI helpers (unit-tested on the JVM). */
object MidiMessages {
    const val PITCH_CENTER = 8192

    fun noteOn(channel: Int, note: Int, velocity: Int) =
        byteArrayOf((0x90 or (channel and 0x0F)).toByte(), note.coerceIn(0, 127).toByte(), velocity.coerceIn(1, 127).toByte())

    fun noteOff(channel: Int, note: Int) =
        byteArrayOf((0x80 or (channel and 0x0F)).toByte(), note.coerceIn(0, 127).toByte(), 0)

    fun controlChange(channel: Int, controller: Int, value: Int) =
        byteArrayOf((0xB0 or (channel and 0x0F)).toByte(), controller.coerceIn(0, 127).toByte(), value.coerceIn(0, 127).toByte())

    /** [bend] from -8192 (down) to +8191 (up); 0 = centre. Returns the 14-bit value 0..16383. */
    fun bendTo14(bend: Int): Int = (bend + PITCH_CENTER).coerceIn(0, 16383)

    fun pitchBend(channel: Int, bend: Int): ByteArray {
        val v = bendTo14(bend)
        return byteArrayOf((0xE0 or (channel and 0x0F)).toByte(), (v and 0x7F).toByte(), ((v shr 7) and 0x7F).toByte())
    }

    private val NAMES_FR = arrayOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    private val NAMES_EN = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** FL Studio octave convention: note 60 = C5 (Do5). */
    fun noteName(note: Int, french: Boolean = true): String =
        (if (french) NAMES_FR else NAMES_EN)[note % 12] + (note / 12)

    /** Semitone offsets of the white keys within an octave (C D E F G A B). */
    val WHITE_OFFSETS = intArrayOf(0, 2, 4, 5, 7, 9, 11)

    /** White-key index inside an octave after which a black key sits (C#, D#, F#, G#, A#). */
    fun hasBlackAfter(whiteIndexInOctave: Int) = whiteIndexInOctave in intArrayOf(0, 1, 3, 4, 5)

    /**
     * Note under (x, y) on a piano of [whiteCount] white keys starting at C [lowestNote],
     * black keys covering the top [blackHeightRatio] of the height. Null outside the keys.
     */
    fun noteAt(
        x: Float, y: Float, width: Float, height: Float,
        lowestNote: Int, whiteCount: Int, blackHeightRatio: Float = 0.62f, blackWidthRatio: Float = 0.6f
    ): Int? {
        if (x < 0 || y < 0 || x >= width || y >= height || whiteCount <= 0) return null
        val whiteW = width / whiteCount
        if (y < height * blackHeightRatio) {
            val boundary = Math.round(x / whiteW) // nearest boundary between two white keys
            val left = boundary - 1
            if (left in 0 until whiteCount - 1 && hasBlackAfter(left % 7)) {
                if (Math.abs(x - boundary * whiteW) < whiteW * blackWidthRatio / 2) {
                    return whiteNote(lowestNote, left) + 1
                }
            }
        }
        val idx = (x / whiteW).toInt().coerceIn(0, whiteCount - 1)
        return whiteNote(lowestNote, idx)
    }

    fun whiteNote(lowestNote: Int, whiteIndex: Int): Int =
        lowestNote + (whiteIndex / 7) * 12 + WHITE_OFFSETS[whiteIndex % 7]

    /** Velocity from where the key is struck: near the front edge = louder, like a real key. */
    fun velocityFromY(y: Float, keyHeight: Float): Int =
        (28 + 99 * (y / keyHeight).coerceIn(0f, 1f)).toInt().coerceIn(1, 127)
}
