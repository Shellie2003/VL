package com.example.cutpilot.input

/** Keyboard layout configured on the *computer*. */
enum class HostLayout(val label: String) {
    QWERTY("QWERTY"),
    AZERTY("AZERTY")
}

/** One key press: HID modifier bits + HID usage (physical position). */
data class KeyStroke(val modifiers: Int, val usage: Int)

/**
 * A key of the on-screen keyboard.
 * [usage] is the physical position (HID usage), 0 for modifier keys handled by the screen.
 */
data class VirtualKey(
    val usage: Int,
    val label: String,
    val shiftLabel: String? = null,
    val altGrLabel: String? = null,
    val weight: Float = 1f,
    val modifier: Int = 0
)

object KeyboardLayouts {
    const val CTRL = 0x01
    const val SHIFT = 0x02
    const val ALT = 0x04
    const val META = 0x08
    const val ALTGR = 0x40

    private const val SPACE = 0x2C
    private const val ENTER = 0x28
    private const val TAB = 0x2B
    private const val BACKSPACE = 0x2A
    private const val CAPS = 0x39

    // ------------------------------------------------------------------------
    // Character -> key strokes (used for Bluetooth HID, which cannot send Unicode)
    // ------------------------------------------------------------------------
    private val qwertyMap: Map<Char, List<KeyStroke>> by lazy { buildQwerty() }
    private val azertyMap: Map<Char, List<KeyStroke>> by lazy { buildAzerty() }

    /** Key strokes producing [ch] on a computer using [layout], or null if impossible. */
    fun strokesFor(ch: Char, layout: HostLayout): List<KeyStroke>? =
        (if (layout == HostLayout.AZERTY) azertyMap else qwertyMap)[ch]

    private fun one(mods: Int, usage: Int) = listOf(KeyStroke(mods, usage))

    private fun buildCommon(m: MutableMap<Char, List<KeyStroke>>) {
        m[' '] = one(0, SPACE)
        m['\n'] = one(0, ENTER)
        m['\t'] = one(0, TAB)
    }

    private fun buildQwerty(): Map<Char, List<KeyStroke>> {
        val m = HashMap<Char, List<KeyStroke>>()
        buildCommon(m)
        for (i in 0 until 26) {
            m['a' + i] = one(0, 0x04 + i)
            m['A' + i] = one(SHIFT, 0x04 + i)
        }
        val digits = "1234567890"
        val shifted = "!@#$%^&*()"
        for (i in 0 until 10) {
            m[digits[i]] = one(0, 0x1E + i)
            m[shifted[i]] = one(SHIFT, 0x1E + i)
        }
        val pairs = listOf(
            Triple(0x2D, '-', '_'), Triple(0x2E, '=', '+'), Triple(0x2F, '[', '{'),
            Triple(0x30, ']', '}'), Triple(0x31, '\\', '|'), Triple(0x33, ';', ':'),
            Triple(0x34, '\'', '"'), Triple(0x35, '`', '~'), Triple(0x36, ',', '<'),
            Triple(0x37, '.', '>'), Triple(0x38, '/', '?')
        )
        for ((u, a, b) in pairs) {
            m[a] = one(0, u)
            m[b] = one(SHIFT, u)
        }
        return m
    }

    /** French AZERTY (Windows / Linux "fr" layout). */
    private fun buildAzerty(): Map<Char, List<KeyStroke>> {
        val m = HashMap<Char, List<KeyStroke>>()
        buildCommon(m)
        // Letters: same positions as QWERTY except A<->Q, Z<->W and M on the ";" key.
        for (i in 0 until 26) {
            val c = 'a' + i
            val usage = when (c) {
                'a' -> 0x14
                'q' -> 0x04
                'z' -> 0x1A
                'w' -> 0x1D
                'm' -> 0x33
                else -> 0x04 + i
            }
            m[c] = one(0, usage)
            m[c.uppercaseChar()] = one(SHIFT, usage)
        }
        // Number row: unshifted symbols, Shift = digits, AltGr = programming symbols.
        val base = "&é\"'(-è_çà"
        val digits = "1234567890"
        for (i in 0 until 10) {
            m[base[i]] = one(0, 0x1E + i)
            m[digits[i]] = one(SHIFT, 0x1E + i)
        }
        val altGrRow = mapOf(0x20 to '#', 0x21 to '{', 0x22 to '[', 0x23 to '|', 0x25 to '\\', 0x26 to '^', 0x27 to '@')
        for ((u, c) in altGrRow) m[c] = one(ALTGR, u)
        m[')'] = one(0, 0x2D); m['°'] = one(SHIFT, 0x2D); m[']'] = one(ALTGR, 0x2D)
        m['='] = one(0, 0x2E); m['+'] = one(SHIFT, 0x2E); m['}'] = one(ALTGR, 0x2E)
        m['$'] = one(0, 0x30); m['£'] = one(SHIFT, 0x30); m['¤'] = one(ALTGR, 0x30)
        m['ù'] = one(0, 0x34); m['%'] = one(SHIFT, 0x34)
        m['*'] = one(0, 0x32); m['µ'] = one(SHIFT, 0x32)
        m['²'] = one(0, 0x35)
        m['<'] = one(0, 0x64); m['>'] = one(SHIFT, 0x64)
        m[','] = one(0, 0x10); m['?'] = one(SHIFT, 0x10)
        m[';'] = one(0, 0x36); m['.'] = one(SHIFT, 0x36)
        m[':'] = one(0, 0x37); m['/'] = one(SHIFT, 0x37)
        m['!'] = one(0, 0x38); m['§'] = one(SHIFT, 0x38)
        m['€'] = one(ALTGR, 0x08)
        // Dead keys: followed by a space to produce the character itself.
        m['~'] = listOf(KeyStroke(ALTGR, 0x1F), KeyStroke(0, SPACE))
        m['`'] = listOf(KeyStroke(ALTGR, 0x24), KeyStroke(0, SPACE))
        m['¨'] = listOf(KeyStroke(SHIFT, 0x2F), KeyStroke(0, SPACE))
        // Circumflex / diaeresis accents through the dead key.
        val circ = mapOf('â' to 'a', 'ê' to 'e', 'î' to 'i', 'ô' to 'o', 'û' to 'u')
        val diae = mapOf('ä' to 'a', 'ë' to 'e', 'ï' to 'i', 'ö' to 'o', 'ü' to 'u', 'ÿ' to 'y')
        for ((acc, v) in circ) {
            m[acc] = listOf(KeyStroke(0, 0x2F)) + m.getValue(v)
            m[acc.uppercaseChar()] = listOf(KeyStroke(0, 0x2F)) + m.getValue(v.uppercaseChar())
        }
        for ((acc, v) in diae) {
            m[acc] = listOf(KeyStroke(SHIFT, 0x2F)) + m.getValue(v)
            m[acc.uppercaseChar()] = listOf(KeyStroke(SHIFT, 0x2F)) + m.getValue(v.uppercaseChar())
        }
        return m
    }

    // ------------------------------------------------------------------------
    // On-screen keyboard rows (labels follow the computer's layout)
    // ------------------------------------------------------------------------
    val functionRow: List<VirtualKey> =
        listOf(VirtualKey(0x29, "Échap", weight = 1.3f)) +
            (1..12).map { VirtualKey(0x3A + it - 1, "F$it") }

    fun rows(layout: HostLayout): List<List<VirtualKey>> =
        if (layout == HostLayout.AZERTY) azertyRows else qwertyRows

    private fun letterKeys(chars: String, usages: List<Int>) =
        chars.mapIndexed { i, c -> VirtualKey(usages[i], c.toString(), c.uppercase()) }

    private fun usagesOf(chars: String, layout: HostLayout) =
        chars.map { c -> (if (layout == HostLayout.AZERTY) azertyMap else qwertyMap).getValue(c).last().usage }

    private val bottomRow = listOf(
        VirtualKey(0, "Ctrl", weight = 1.3f, modifier = CTRL),
        VirtualKey(0, "Win", weight = 1.2f, modifier = META),
        VirtualKey(0, "Alt", weight = 1.2f, modifier = ALT),
        VirtualKey(SPACE, "Espace", weight = 4.6f),
        VirtualKey(0, "AltGr", weight = 1.3f, modifier = ALTGR),
        VirtualKey(0x65, "Menu", weight = 1.2f)
    )

    private val qwertyRows: List<List<VirtualKey>> by lazy {
        val q = HostLayout.QWERTY
        listOf(
            listOf(VirtualKey(0x35, "`", "~")) +
                "1234567890".mapIndexed { i, c -> VirtualKey(0x1E + i, c.toString(), "!@#$%^&*()"[i].toString()) } +
                listOf(VirtualKey(0x2D, "-", "_"), VirtualKey(0x2E, "=", "+"), VirtualKey(BACKSPACE, "⌫", weight = 1.6f)),
            listOf(VirtualKey(TAB, "Tab", weight = 1.4f)) + letterKeys("qwertyuiop", usagesOf("qwertyuiop", q)) +
                listOf(VirtualKey(0x2F, "[", "{"), VirtualKey(0x30, "]", "}"), VirtualKey(0x31, "\\", "|")),
            listOf(VirtualKey(CAPS, "Maj⇪", weight = 1.7f)) + letterKeys("asdfghjkl", usagesOf("asdfghjkl", q)) +
                listOf(VirtualKey(0x33, ";", ":"), VirtualKey(0x34, "'", "\""), VirtualKey(ENTER, "Entrée", weight = 2f)),
            listOf(VirtualKey(0, "⇧", weight = 2.1f, modifier = SHIFT)) + letterKeys("zxcvbnm", usagesOf("zxcvbnm", q)) +
                listOf(VirtualKey(0x36, ",", "<"), VirtualKey(0x37, ".", ">"), VirtualKey(0x38, "/", "?"),
                    VirtualKey(0, "⇧", weight = 2.1f, modifier = SHIFT)),
            bottomRow
        )
    }

    private val azertyRows: List<List<VirtualKey>> by lazy {
        val a = HostLayout.AZERTY
        val altGrDigits = mapOf(1 to "~", 2 to "#", 3 to "{", 4 to "[", 5 to "|", 6 to "`", 7 to "\\", 8 to "^", 9 to "@")
        listOf(
            listOf(VirtualKey(0x35, "²")) +
                "&é\"'(-è_çà".mapIndexed { i, c ->
                    VirtualKey(0x1E + i, c.toString(), "1234567890"[i].toString(), altGrDigits[i])
                } +
                listOf(VirtualKey(0x2D, ")", "°", "]"), VirtualKey(0x2E, "=", "+", "}"), VirtualKey(BACKSPACE, "⌫", weight = 1.6f)),
            listOf(VirtualKey(TAB, "Tab", weight = 1.4f)) +
                letterKeys("azertyuiop", usagesOf("azertyuiop", a)).map { if (it.label == "e") it.copy(altGrLabel = "€") else it } +
                listOf(VirtualKey(0x2F, "^", "¨"), VirtualKey(0x30, "$", "£", "¤")),
            listOf(VirtualKey(CAPS, "Maj⇪", weight = 1.7f)) + letterKeys("qsdfghjklm", usagesOf("qsdfghjklm", a)) +
                listOf(VirtualKey(0x34, "ù", "%"), VirtualKey(0x32, "*", "µ"), VirtualKey(ENTER, "Entrée", weight = 1.8f)),
            listOf(VirtualKey(0, "⇧", weight = 1.4f, modifier = SHIFT), VirtualKey(0x64, "<", ">")) +
                letterKeys("wxcvbn", usagesOf("wxcvbn", a)) +
                listOf(VirtualKey(0x10, ",", "?"), VirtualKey(0x36, ";", "."), VirtualKey(0x37, ":", "/"),
                    VirtualKey(0x38, "!", "§"), VirtualKey(0, "⇧", weight = 1.8f, modifier = SHIFT)),
            bottomRow
        )
    }

    /** Navigation cluster + arrows. */
    val navigationKeys: List<VirtualKey> = listOf(
        VirtualKey(0x46, "Impr"), VirtualKey(0x49, "Inser"), VirtualKey(0x4A, "Début"),
        VirtualKey(0x4B, "Pg↑"), VirtualKey(0x4C, "Suppr"), VirtualKey(0x4D, "Fin"), VirtualKey(0x4E, "Pg↓")
    )
    val arrowUp = VirtualKey(0x52, "↑")
    val arrowLeft = VirtualKey(0x50, "←")
    val arrowDown = VirtualKey(0x51, "↓")
    val arrowRight = VirtualKey(0x4F, "→")
}
