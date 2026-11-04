package com.example.cutpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.data.LibraryShortcut
import com.example.cutpilot.data.ShortcutLibrary
import com.example.cutpilot.input.HostLayout
import com.example.cutpilot.input.KeyboardLayouts
import com.example.cutpilot.input.VirtualKey
import com.example.cutpilot.network.VolavoProtocol
import com.example.cutpilot.ui.theme.AmberAccent
import com.example.cutpilot.ui.theme.DarkBg
import com.example.cutpilot.ui.theme.NeonCyan
import com.example.cutpilot.ui.theme.PurpleAccent
import com.example.cutpilot.ui.theme.SurfaceContainerDark
import com.example.cutpilot.ui.theme.SurfaceContainerHighDark
import com.example.cutpilot.ui.theme.SurfaceDark
import com.example.cutpilot.ui.theme.TextPrimary
import com.example.cutpilot.ui.theme.TextSecondary
import com.example.cutpilot.ui.theme.TextTertiary

/**
 * Full computer keyboard: AZERTY / QWERTY layout matching the PC, sticky modifiers,
 * function / navigation / media keys, text sending and a searchable shortcut library.
 */
@Composable
fun KeyboardScreen(
    layout: HostLayout,
    onLayoutChange: (HostLayout) -> Unit,
    onPhysicalKey: (modifiers: Int, usage: Int) -> Unit,
    onTypeText: (text: String, pressEnter: Boolean) -> Unit,
    onMedia: (usage: Int) -> Unit,
    onShortcut: (LibraryShortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    // Sticky modifiers: one tap = active for the next key, second tap = locked, third = off.
    var oneShot by remember { mutableIntStateOf(0) }
    var locked by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var category by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val activeMods = oneShot or locked

    fun toggleModifier(bit: Int) {
        when {
            locked and bit != 0 -> locked = locked and bit.inv()
            oneShot and bit != 0 -> {
                oneShot = oneShot and bit.inv()
                locked = locked or bit
            }
            else -> oneShot = oneShot or bit
        }
    }

    fun press(key: VirtualKey) {
        if (key.modifier != 0) {
            toggleModifier(key.modifier)
            return
        }
        onPhysicalKey(activeMods, key.usage)
        oneShot = 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        // --- Layout selector -------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Clavier de l'ordinateur :", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            HostLayout.entries.forEach { l ->
                val sel = l == layout
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) NeonCyan else SurfaceContainerDark)
                        .clickable { onLayoutChange(l) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("layout_${l.name.lowercase()}")
                ) {
                    Text(l.label, color = if (sel) DarkBg else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- Text sender -----------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Écrire du texte à envoyer au PC…", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Button(
                    onClick = { onTypeText(text, false); text = "" },
                    enabled = text.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) { Icon(Icons.Default.Send, contentDescription = "Envoyer", modifier = Modifier.size(18.dp)) }
                Button(
                    onClick = { onTypeText(text, true); text = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighDark, contentColor = NeonCyan),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) { Text("+ ⏎", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Function row ----------------------------------------------------
        KeyRow(KeyboardLayouts.functionRow, activeMods, layout, height = 34.dp) { press(it) }
        Spacer(Modifier.height(6.dp))

        // --- Main keyboard ---------------------------------------------------
        KeyboardLayouts.rows(layout).forEach { row ->
            KeyRow(row, activeMods, layout, height = 46.dp, lockedMods = locked) { press(it) }
            Spacer(Modifier.height(4.dp))
        }

        if (activeMods != 0) {
            Text(
                text = "Modificateurs actifs : " + modifierNames(activeMods) +
                    if (locked != 0) "  (verrouillés : ${modifierNames(locked)})" else "",
                color = AmberAccent,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        // --- Navigation + arrows --------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1.6f)) {
                KeyRow(KeyboardLayouts.navigationKeys.take(4), activeMods, layout, height = 38.dp) { press(it) }
                Spacer(Modifier.height(4.dp))
                KeyRow(KeyboardLayouts.navigationKeys.drop(4), activeMods, layout, height = 38.dp) { press(it) }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Spacer(Modifier.weight(1f))
                    KeyCap(KeyboardLayouts.arrowUp, activeMods, layout, Modifier.weight(1f).height(38.dp)) { press(it) }
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    listOf(KeyboardLayouts.arrowLeft, KeyboardLayouts.arrowDown, KeyboardLayouts.arrowRight).forEach { k ->
                        KeyCap(k, activeMods, layout, Modifier.weight(1f).padding(horizontal = 2.dp).height(38.dp)) { press(it) }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Media keys ------------------------------------------------------
        SectionTitle("MULTIMÉDIA")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "⏮" to VolavoProtocol.MEDIA_PREVIOUS, "⏯" to VolavoProtocol.MEDIA_PLAY_PAUSE,
                "⏭" to VolavoProtocol.MEDIA_NEXT, "⏹" to VolavoProtocol.MEDIA_STOP,
                "🔉" to VolavoProtocol.MEDIA_VOLUME_DOWN, "🔊" to VolavoProtocol.MEDIA_VOLUME_UP,
                "🔇" to VolavoProtocol.MEDIA_MUTE
            ).forEach { (label, usage) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerDark)
                        .clickable { onMedia(usage) },
                    contentAlignment = Alignment.Center
                ) { Text(label, fontSize = 16.sp) }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Shortcut library ------------------------------------------------
        SectionTitle("RACCOURCIS (${ShortcutLibrary.all.size})")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Rechercher un raccourci (ex : capture, onglet…)", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))

        val searching = query.isNotBlank()
        if (!searching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShortcutLibrary.categories.forEachIndexed { i, c ->
                    val sel = i == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) PurpleAccent.copy(alpha = 0.3f) else SurfaceContainerDark)
                            .border(1.dp, if (sel) PurpleAccent else SurfaceContainerHighDark, RoundedCornerShape(16.dp))
                            .clickable { category = i }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(c.name, color = if (sel) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        val items = if (searching) {
            val q = query.trim().lowercase()
            ShortcutLibrary.all.filter { it.label.lowercase().contains(q) || it.combo.lowercase().contains(q) }
        } else {
            ShortcutLibrary.categories[category].items
        }
        if (items.isEmpty()) {
            Text("Aucun raccourci trouvé.", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        }
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { sc ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(10.dp))
                            .clickable { onShortcut(sc) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(sc.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(sc.combo, color = NeonCyan, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
        }

        Text(
            "Les touches du clavier suivent la disposition choisie : ce qui est affiché correspond à ce que tape le PC. " +
                "En Wi-Fi, le texte est envoyé tel quel (accents, émojis). En Bluetooth, il est converti selon AZERTY/QWERTY.",
            color = TextTertiary,
            fontSize = 10.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
}

private fun modifierNames(bits: Int): String = buildList {
    if (bits and KeyboardLayouts.CTRL != 0) add("Ctrl")
    if (bits and KeyboardLayouts.SHIFT != 0) add("Maj")
    if (bits and KeyboardLayouts.ALT != 0) add("Alt")
    if (bits and KeyboardLayouts.ALTGR != 0) add("AltGr")
    if (bits and KeyboardLayouts.META != 0) add("Win")
}.joinToString(" + ")

@Composable
private fun KeyRow(
    keys: List<VirtualKey>,
    activeMods: Int,
    layout: HostLayout,
    height: androidx.compose.ui.unit.Dp,
    lockedMods: Int = 0,
    onPress: (VirtualKey) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        keys.forEach { k ->
            KeyCap(k, activeMods, layout, Modifier.weight(k.weight).height(height), lockedMods, onPress)
        }
    }
}

@Composable
private fun KeyCap(
    key: VirtualKey,
    activeMods: Int,
    layout: HostLayout,
    modifier: Modifier,
    lockedMods: Int = 0,
    onPress: (VirtualKey) -> Unit
) {
    val isMod = key.modifier != 0
    val modActive = isMod && (activeMods and key.modifier) != 0
    val modLocked = isMod && (lockedMods and key.modifier) != 0
    val shift = activeMods and KeyboardLayouts.SHIFT != 0
    val altGr = activeMods and KeyboardLayouts.ALTGR != 0
    val main = when {
        isMod && key.modifier == KeyboardLayouts.META && layout == HostLayout.QWERTY -> "Win/⌘"
        altGr && key.altGrLabel != null -> key.altGrLabel
        shift && key.shiftLabel != null -> key.shiftLabel
        else -> key.label
    }
    val bg = when {
        modLocked -> AmberAccent.copy(alpha = 0.45f)
        modActive -> AmberAccent.copy(alpha = 0.25f)
        isMod -> SurfaceContainerHighDark
        else -> SurfaceContainerDark
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, if (modActive) AmberAccent else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
            .clickable { onPress(key) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = main,
                color = if (modActive) AmberAccent else TextPrimary,
                fontSize = if (main.length > 2) 10.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            // Secondary hint (AltGr symbol) in small print
            if (!altGr && key.altGrLabel != null) {
                Text(key.altGrLabel, color = TextTertiary, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}
