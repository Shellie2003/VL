package com.example.cutpilot.input

/**
 * A transport able to inject pointer & keyboard events on the host.
 * All methods are non-blocking and may be called from the UI thread at touch rate:
 * implementations queue the work on their own high-priority I/O thread.
 */
interface InputSink {
    /** Relative pointer motion in host pixels. Bursts are coalesced by the transport. */
    fun move(dx: Int, dy: Int)

    /** Full button state (bit0 left, bit1 right, bit2 middle). */
    fun setButtons(mask: Int)

    /** Scroll in 1/120 notch units. Positive vertical = away from the user, positive horizontal = right. */
    fun scroll(vertical: Int, horizontal: Int)

    /** HID modifier bits + HID usage id; action = KEY_TAP / KEY_DOWN / KEY_UP. */
    fun key(modifiers: Int, usage: Int, action: Int)

    /** Total packets/reports actually sent, used for the live rate indicator. */
    val packetsSent: Long
}
