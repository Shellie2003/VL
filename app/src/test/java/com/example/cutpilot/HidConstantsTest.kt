package com.example.cutpilot

import com.example.cutpilot.bluetooth.HidConstants
import com.example.cutpilot.model.ModifierKey
import org.junit.Assert.assertEquals
import org.junit.Test

class HidConstantsTest {

    @Test
    fun symbols() {
        assertEquals(0x04, HidConstants.symbolToHidCode("a").toInt())
        assertEquals(0x1D, HidConstants.symbolToHidCode("Z").toInt())
        assertEquals(0x1E, HidConstants.symbolToHidCode("1").toInt())
        assertEquals(0x27, HidConstants.symbolToHidCode("0").toInt())
        assertEquals(0x2C, HidConstants.symbolToHidCode("ESPACE").toInt())
        assertEquals(0x4C, HidConstants.symbolToHidCode("DEL").toInt())
        assertEquals(0x45, HidConstants.symbolToHidCode("F12").toInt())
        assertEquals(0x31, HidConstants.symbolToHidCode("\\").toInt())
        // Unknown symbols no longer fall back to SPACE (accidental play/pause).
        assertEquals(0, HidConstants.symbolToHidCode("???").toInt())
    }

    @Test
    fun azertyRemap() {
        assertEquals(0x14, HidConstants.remapForAzerty(0x04)) // A -> Q position
        assertEquals(0x1A, HidConstants.remapForAzerty(0x1D)) // Z -> W position
        assertEquals(0x33, HidConstants.remapForAzerty(0x10)) // M -> ; position
        assertEquals(0x06, HidConstants.remapForAzerty(0x06)) // C unchanged
    }

    @Test
    fun modifiers() {
        assertEquals(0x03, HidConstants.modifierBits(setOf(ModifierKey.CTRL, ModifierKey.SHIFT)))
        assertEquals(0x08, HidConstants.modifierBits(setOf(ModifierKey.CMD)))
    }

    @Test
    fun descriptorMouseReportIsFiveBytes() {
        // 3 buttons + 5 padding bits, X, Y, wheel, pan
        val d = HidConstants.HID_REPORT_DESCRIPTOR
        assertEquals(0xC0.toByte(), d.last())
    }
}
