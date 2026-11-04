package com.example.cutpilot

import com.example.cutpilot.network.PairingInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingInfoTest {

    @Test
    fun parsesHostQrCode() {
        val info = PairingInfo.parse(
            "volavo://pair?v=1&h=192.168.1.20,10.0.0.4&p=50505&k=ND6QW6U27KJMDZD3&n=Mon%20PC&b=aa:bb:cc:dd:ee:ff"
        )!!
        assertEquals(listOf("192.168.1.20", "10.0.0.4"), info.hosts)
        assertEquals(50505, info.port)
        assertEquals("ND6QW6U27KJMDZD3", info.key)
        assertEquals("Mon PC", info.hostName)
        assertEquals("AA:BB:CC:DD:EE:FF", info.bluetoothMac)
    }

    @Test
    fun roundTrip() {
        val info = PairingInfo(listOf("192.168.0.7"), 6000, "ABCDEFGHJKLMNPQR", "Studio & Co", null)
        assertEquals(info, PairingInfo.parse(info.toUri()))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(PairingInfo.parse("https://example.com"))
        assertNull(PairingInfo.parse("volavo://pair?v=1&h=192.168.1.2&k=short"))
        assertNull(PairingInfo.parse("volavo://pair?v=2&h=192.168.1.2&k=ND6QW6U27KJMDZD3"))
        assertNull(PairingInfo.parse("volavo://pair?v=1&h=999.1.1.1&k=ND6QW6U27KJMDZD3"))
    }

    @Test
    fun manualCodeIsNormalized() {
        assertEquals("ND6QW6U27KJMDZD3", PairingInfo.normalizeKey("nd6q-w6u2-7kjm-dzd3"))
    }
}
