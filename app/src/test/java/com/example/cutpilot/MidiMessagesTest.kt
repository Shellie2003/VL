package com.example.cutpilot

import com.example.cutpilot.midi.MidiMessages
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiMessagesTest {
    private fun b(vararg v: Int) = v.map { it.toByte() }.toByteArray()

    @Test
    fun messages() {
        assertArrayEquals(b(0x90, 60, 100), MidiMessages.noteOn(0, 60, 100))
        assertArrayEquals(b(0x83, 61, 0), MidiMessages.noteOff(3, 61))
        assertArrayEquals(b(0xB0, 64, 127), MidiMessages.controlChange(0, 64, 127))
        assertArrayEquals(b(0xE0, 0x00, 0x40), MidiMessages.pitchBend(0, 0))      // centre
        assertArrayEquals(b(0xE0, 0x7F, 0x7F), MidiMessages.pitchBend(0, 8191))   // max up
        assertArrayEquals(b(0xE0, 0x00, 0x00), MidiMessages.pitchBend(0, -8192))  // max down
        assertEquals(1, MidiMessages.noteOn(0, 60, 0)[2].toInt()) // velocity 0 would be a note off
    }

    @Test
    fun names() {
        assertEquals("Do5", MidiMessages.noteName(60))   // FL Studio convention
        assertEquals("La5", MidiMessages.noteName(69))
        assertEquals("C#4", MidiMessages.noteName(49, french = false))
    }

    @Test
    fun keyUnderFinger() {
        // 2 octaves + C = 15 white keys, 1500 px wide => 100 px per white key, 1000 px high
        val w = 1500f; val h = 1000f; val low = 48
        fun at(x: Float, y: Float) = MidiMessages.noteAt(x, y, w, h, low, 15)
        assertEquals(48, at(50f, 900f))        // C (white, bottom)
        assertEquals(49, at(100f, 300f))       // C# (black, top, on the C/D boundary)
        assertEquals(50, at(150f, 300f))       // D white between two black keys
        assertEquals(52, at(250f, 300f))       // E: no black key between E and F
        assertEquals(53, at(300f, 300f))       // F: boundary E/F has no black key
        assertEquals(72, at(1450f, 500f))      // last C
        assertEquals(null, at(-1f, 10f))
    }

    @Test
    fun velocity() {
        assertTrue(MidiMessages.velocityFromY(950f, 1000f) > 110)
        assertTrue(MidiMessages.velocityFromY(10f, 1000f) < 40)
    }
}
