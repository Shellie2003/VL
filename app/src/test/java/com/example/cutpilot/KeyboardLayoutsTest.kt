package com.example.cutpilot

import com.example.cutpilot.bluetooth.HidConstants
import com.example.cutpilot.data.ShortcutLibrary
import com.example.cutpilot.input.HostLayout
import com.example.cutpilot.input.KeyStroke
import com.example.cutpilot.input.KeyboardLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutsTest {
    private val shift = KeyboardLayouts.SHIFT
    private val altGr = KeyboardLayouts.ALTGR

    @Test
    fun qwertyCharacters() {
        val q = HostLayout.QWERTY
        assertEquals(listOf(KeyStroke(0, 0x04)), KeyboardLayouts.strokesFor('a', q))
        assertEquals(listOf(KeyStroke(shift, 0x1F)), KeyboardLayouts.strokesFor('@', q))
        assertEquals(listOf(KeyStroke(shift, 0x38)), KeyboardLayouts.strokesFor('?', q))
        assertNull(KeyboardLayouts.strokesFor('é', q))
    }

    @Test
    fun azertyCharacters() {
        val a = HostLayout.AZERTY
        assertEquals(listOf(KeyStroke(0, 0x14)), KeyboardLayouts.strokesFor('a', a))
        assertEquals(listOf(KeyStroke(0, 0x1A)), KeyboardLayouts.strokesFor('z', a))
        assertEquals(listOf(KeyStroke(0, 0x33)), KeyboardLayouts.strokesFor('m', a))
        assertEquals(listOf(KeyStroke(0, 0x1F)), KeyboardLayouts.strokesFor('é', a))
        assertEquals(listOf(KeyStroke(shift, 0x1E)), KeyboardLayouts.strokesFor('1', a))
        assertEquals(listOf(KeyStroke(altGr, 0x27)), KeyboardLayouts.strokesFor('@', a))
        assertEquals(listOf(KeyStroke(shift, 0x36)), KeyboardLayouts.strokesFor('.', a))
        assertEquals(listOf(KeyStroke(altGr, 0x08)), KeyboardLayouts.strokesFor('€', a))
        assertEquals(listOf(KeyStroke(0, 0x2F), KeyStroke(0, 0x08)), KeyboardLayouts.strokesFor('ê', a))
    }

    @Test
    fun frenchSentenceFullyTypableOnAzerty() {
        val text = "Bonjour, ça va ? L'été à Noël coûte 15 € ! (test@volavo.fr)"
        val missing = text.filter { KeyboardLayouts.strokesFor(it, HostLayout.AZERTY) == null }
        assertEquals("", missing)
    }

    @Test
    fun onScreenRowsHaveValidUsages() {
        for (layout in HostLayout.entries) {
            val rows = KeyboardLayouts.rows(layout)
            assertEquals(5, rows.size)
            rows.flatten().forEach { k ->
                assertTrue("${k.label} sans usage", k.usage in 1..0x65 || k.modifier != 0)
            }
        }
    }

    @Test
    fun everyLibraryShortcutResolvesToAKey() {
        assertTrue(ShortcutLibrary.all.size >= 150)
        ShortcutLibrary.all.forEach { sc ->
            if (sc.key.isEmpty()) {
                assertTrue(sc.label, sc.modifiers.isNotEmpty())
            } else {
                assertNotNull(sc.label, sc.key)
                assertTrue("Touche inconnue pour '${sc.label}' : ${sc.key}", HidConstants.symbolToHidCode(sc.key).toInt() != 0)
            }
        }
    }
}
