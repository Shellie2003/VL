package com.example.cutpilot.network

/**
 * Vôlavo wire protocol v1 (binary UDP, big-endian). Mirrors host/volavo_protocol.py.
 */
object VolavoProtocol {
    const val VERSION = 1
    const val DEFAULT_PORT = 50505
    const val KEY_LEN = 16

    const val T_HELLO: Byte = 0x01
    const val T_MOVE: Byte = 0x02
    const val T_BUTTONS: Byte = 0x03
    const val T_SCROLL: Byte = 0x04
    const val T_KEY: Byte = 0x05
    const val T_PING: Byte = 0x06
    const val T_BYE: Byte = 0x07
    const val T_KEY_PHYS: Byte = 0x08
    const val T_TEXT: Byte = 0x09
    const val T_MEDIA: Byte = 0x0A

    const val T_HELLO_ACK: Byte = 0x81.toByte()
    const val T_PONG: Byte = 0x86.toByte()
    const val T_NEED_AUTH: Byte = 0x8F.toByte()

    const val ACK_OK = 0
    const val ACK_BAD_KEY = 1
    const val ACK_BAD_VERSION = 2

    const val KEY_TAP = 0
    const val KEY_DOWN = 1
    const val KEY_UP = 2

    /** AltGr (right Alt) bit in the HID modifier byte. */
    const val MOD_RALT = 0x40

    // HID consumer usages (media keys)
    const val MEDIA_VOLUME_UP = 0xE9
    const val MEDIA_VOLUME_DOWN = 0xEA
    const val MEDIA_MUTE = 0xE2
    const val MEDIA_PLAY_PAUSE = 0xCD
    const val MEDIA_NEXT = 0xB5
    const val MEDIA_PREVIOUS = 0xB6
    const val MEDIA_STOP = 0xB7

    /** Scroll unit: 1/120 of a wheel notch (Windows WHEEL_DELTA convention). */
    const val NOTCH = 120

    fun putShort(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value shr 8).toByte()
        buf[offset + 1] = value.toByte()
    }

    fun putLong(buf: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) buf[offset + i] = (value shr (56 - 8 * i)).toByte()
    }

    fun getLong(buf: ByteArray, offset: Int): Long {
        var v = 0L
        for (i in 0 until 8) v = (v shl 8) or (buf[offset + i].toLong() and 0xFF)
        return v
    }
}
