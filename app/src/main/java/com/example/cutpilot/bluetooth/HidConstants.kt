package com.example.cutpilot.bluetooth

object HidConstants {
    // Report IDs
    const val REPORT_ID_MOUSE: Byte = 1
    const val REPORT_ID_KEYBOARD: Byte = 2

    // Mouse Buttons Mask
    const val MOUSE_BUTTON_NONE = 0
    const val MOUSE_BUTTON_LEFT = 1 shl 0
    const val MOUSE_BUTTON_RIGHT = 1 shl 1
    const val MOUSE_BUTTON_MIDDLE = 1 shl 2

    // Modifier Keys
    const val MOD_LEFT_CTRL: Byte = 0x01
    const val MOD_LEFT_SHIFT: Byte = 0x02
    const val MOD_LEFT_ALT: Byte = 0x04
    const val MOD_LEFT_META: Byte = 0x08
    const val MOD_RIGHT_CTRL: Byte = 0x10
    const val MOD_RIGHT_SHIFT: Byte = 0x20
    const val MOD_RIGHT_ALT: Byte = 0x40
    const val MOD_RIGHT_META: Byte = -0x80 // 0x80 in byte

    // Standard HID Usage Keycodes (USB HID Usage Tables)
    const val KEY_A: Byte = 0x04
    const val KEY_B: Byte = 0x05
    const val KEY_C: Byte = 0x06
    const val KEY_D: Byte = 0x07
    const val KEY_E: Byte = 0x08
    const val KEY_F: Byte = 0x09
    const val KEY_G: Byte = 0x0A
    const val KEY_H: Byte = 0x0B
    const val KEY_I: Byte = 0x0C
    const val KEY_J: Byte = 0x0D
    const val KEY_K: Byte = 0x0E
    const val KEY_L: Byte = 0x0F
    const val KEY_M: Byte = 0x10
    const val KEY_N: Byte = 0x11
    const val KEY_O: Byte = 0x12
    const val KEY_P: Byte = 0x13
    const val KEY_Q: Byte = 0x14
    const val KEY_R: Byte = 0x15
    const val KEY_S: Byte = 0x16
    const val KEY_T: Byte = 0x17
    const val KEY_U: Byte = 0x18
    const val KEY_V: Byte = 0x19
    const val KEY_W: Byte = 0x1A
    const val KEY_X: Byte = 0x1B
    const val KEY_Y: Byte = 0x1C
    const val KEY_Z: Byte = 0x1D
    const val KEY_SPACE: Byte = 0x2C
    const val KEY_ENTER: Byte = 0x28
    const val KEY_ESCAPE: Byte = 0x29
    const val KEY_DELETE: Byte = 0x4C
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_RIGHT_ARROW: Byte = 0x4F
    const val KEY_LEFT_ARROW: Byte = 0x50
    const val KEY_DOWN_ARROW: Byte = 0x51
    const val KEY_UP_ARROW: Byte = 0x52
    const val KEY_MINUS: Byte = 0x2D
    const val KEY_EQUAL: Byte = 0x2E
    const val KEY_LEFT_BRACKET: Byte = 0x2F
    const val KEY_RIGHT_BRACKET: Byte = 0x30
    const val KEY_BACKSLASH: Byte = 0x31
    const val KEY_TILDE: Byte = 0x35
    const val KEY_F9: Byte = 0x42
    const val KEY_0: Byte = 0x27

    fun symbolToHidCode(symbol: String): Byte {
        return when (symbol.uppercase().trim()) {
            "A" -> KEY_A
            "B" -> KEY_B
            "C" -> KEY_C
            "D" -> KEY_D
            "E" -> KEY_E
            "F" -> KEY_F
            "G" -> KEY_G
            "H" -> KEY_H
            "I" -> KEY_I
            "J" -> KEY_J
            "K" -> KEY_K
            "L" -> KEY_L
            "M" -> KEY_M
            "N" -> KEY_N
            "O" -> KEY_O
            "P" -> KEY_P
            "Q" -> KEY_Q
            "R" -> KEY_R
            "S" -> KEY_S
            "T" -> KEY_T
            "U" -> KEY_U
            "V" -> KEY_V
            "W" -> KEY_W
            "X" -> KEY_X
            "Y" -> KEY_Y
            "Z" -> KEY_Z
            "ESPACE", "SPACE" -> KEY_SPACE
            "ENTRER", "ENTER" -> KEY_ENTER
            "DEL", "DELETE", "SUPPR" -> KEY_DELETE
            "HAUT", "UP" -> KEY_UP_ARROW
            "BAS", "DOWN" -> KEY_DOWN_ARROW
            "GAUCHE", "LEFT" -> KEY_LEFT_ARROW
            "DROITE", "RIGHT" -> KEY_RIGHT_ARROW
            "+", "=" -> KEY_EQUAL
            "-" -> KEY_MINUS
            "[" -> KEY_LEFT_BRACKET
            "]" -> KEY_RIGHT_BRACKET
            "\\", "BACKSLASH" -> KEY_BACKSLASH
            "~" -> KEY_TILDE
            "F9" -> KEY_F9
            "0" -> KEY_0
            else -> KEY_SPACE
        }
    }

    /**
     * Standard USB HID descriptor defining a Mouse (Report ID 1) and Keyboard (Report ID 2)
     */
    val HID_REPORT_DESCRIPTOR = byteArrayOf(
        // Mouse (Report ID 1)
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_MOUSE, // REPORT_ID (1)
        0x09.toByte(), 0x01.toByte(), // USAGE (Pointer)
        0xA1.toByte(), 0x00.toByte(), // COLLECTION (Physical)
        // Buttons (3 buttons: Left, Right, Middle)
        0x05.toByte(), 0x09.toByte(), // USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(), // USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (Button 3)
        0x15.toByte(), 0x00.toByte(), // LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), // LOGICAL_MAXIMUM (1)
        0x95.toByte(), 0x03.toByte(), // REPORT_COUNT (3)
        0x75.toByte(), 0x01.toByte(), // REPORT_SIZE (1)
        0x81.toByte(), 0x02.toByte(), // INPUT (Data,Var,Abs)
        // Padding for buttons (5 bits)
        0x95.toByte(), 0x01.toByte(), // REPORT_COUNT (1)
        0x75.toByte(), 0x05.toByte(), // REPORT_SIZE (5)
        0x81.toByte(), 0x03.toByte(), // INPUT (Cnst,Var,Abs)
        // X, Y position deltas
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), // USAGE (X)
        0x09.toByte(), 0x31.toByte(), // USAGE (Y)
        0x15.toByte(), 0x81.toByte(), // LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), // LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), // REPORT_SIZE (8)
        0x95.toByte(), 0x02.toByte(), // REPORT_COUNT (2)
        0x81.toByte(), 0x06.toByte(), // INPUT (Data,Var,Rel)
        // Wheel / Scroll
        0x09.toByte(), 0x38.toByte(), // USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(), // LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), // LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), // REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(), // REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(), // INPUT (Data,Var,Rel)
        0xC0.toByte(), // END_COLLECTION
        0xC0.toByte(), // END_COLLECTION

        // Keyboard (Report ID 2)
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD, // REPORT_ID (2)
        // Modifiers (8 bits)
        0x05.toByte(), 0x07.toByte(), // USAGE_PAGE (Keyboard)
        0x19.toByte(), 0xE0.toByte(), // USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(), // USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(), // LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), // LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), // REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(), // REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(), // INPUT (Data,Var,Abs)
        // Reserved (1 byte)
        0x95.toByte(), 0x01.toByte(), // REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(), // REPORT_SIZE (8)
        0x81.toByte(), 0x01.toByte(), // INPUT (Cnst,Ary,Abs)
        // Key codes (6 bytes)
        0x95.toByte(), 0x06.toByte(), // REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(), // REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(), // LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x65.toByte(), // LOGICAL_MAXIMUM (101)
        0x05.toByte(), 0x07.toByte(), // USAGE_PAGE (Keyboard)
        0x19.toByte(), 0x00.toByte(), // USAGE_MINIMUM (Reserved)
        0x29.toByte(), 0x65.toByte(), // USAGE_MAXIMUM (Keyboard Application)
        0x81.toByte(), 0x00.toByte(), // INPUT (Data,Ary,Abs)
        0xC0.toByte()  // END_COLLECTION
    )
}
