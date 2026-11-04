package com.example.cutpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.model.ModifierKey
import com.example.cutpilot.model.ShortcutKey
import com.example.cutpilot.model.SoftwareProfile
import com.example.cutpilot.ui.theme.AmberAccent
import com.example.cutpilot.ui.theme.DarkBg
import com.example.cutpilot.ui.theme.EmeraldGreen
import com.example.cutpilot.ui.theme.NeonCyan
import com.example.cutpilot.ui.theme.RazorRed
import com.example.cutpilot.ui.theme.SurfaceContainerDark
import com.example.cutpilot.ui.theme.SurfaceContainerHighDark
import com.example.cutpilot.ui.theme.SurfaceDark
import com.example.cutpilot.ui.theme.TextPrimary
import com.example.cutpilot.ui.theme.TextSecondary
import com.example.cutpilot.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorDeckScreen(
    profiles: List<SoftwareProfile>,
    activeProfile: SoftwareProfile,
    onSelectProfile: (SoftwareProfile) -> Unit,
    onTriggerShortcut: (ShortcutKey) -> Unit,
    onSaveShortcut: (ShortcutKey) -> Unit,
    onCreateProfile: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Tous") }
    var editingShortcut by remember { mutableStateOf<ShortcutKey?>(null) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }

    val categories = remember(activeProfile) {
        listOf("Tous") + activeProfile.shortcuts.map { it.category }.distinct()
    }

    val filteredShortcuts = remember(activeProfile, selectedCategory) {
        if (selectedCategory == "Tous") {
            activeProfile.shortcuts
        } else {
            activeProfile.shortcuts.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Software profile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logiciel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            TextButton(
                onClick = { showCreateProfileDialog = true },
                modifier = Modifier.testTag("add_custom_profile_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nouveau profil", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(profiles) { profile ->
                val isSelected = profile.id == activeProfile.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectProfile(profile) },
                    label = { Text(profile.name, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("profile_item_${profile.id}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.18f),
                        selectedLabelColor = TextPrimary,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = SurfaceContainerHighDark,
                        selectedBorderColor = NeonCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Catégorie", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(categories) { cat ->
                val sel = selectedCategory == cat
                FilterChip(
                    selected = sel,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 13.sp) },
                    modifier = Modifier.height(36.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = DarkBg,
                        containerColor = SurfaceContainerDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sel,
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Shortcut pads: 2 per row on a phone, big readable labels
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filteredShortcuts) { shortcut ->
                val padColor = Color(shortcut.colorHex)
                Row(
                    modifier = Modifier
                        .height(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(14.dp))
                        .clickable { onTriggerShortcut(shortcut) }
                        .testTag("macro_pad_${shortcut.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category color accent
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(padColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = shortcut.label,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comboLabel(shortcut),
                            color = padColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { editingShortcut = shortcut },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier ${shortcut.label}",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .height(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            editingShortcut = ShortcutKey(
                                id = "custom_${System.currentTimeMillis()}",
                                label = "Nouveau raccourci",
                                category = if (selectedCategory != "Tous") selectedCategory else "Outils",
                                keySymbol = "X",
                                colorHex = 0xFF8B7CFF
                            )
                        }
                        .testTag("add_shortcut_pad"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajouter", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Modal Shortcut Editor Dialog
    editingShortcut?.let { keyToEdit ->
        var label by remember { mutableStateOf(keyToEdit.label) }
        var symbol by remember { mutableStateOf(keyToEdit.keySymbol) }
        var category by remember { mutableStateOf(keyToEdit.category) }
        var hasCtrl by remember { mutableStateOf(keyToEdit.modifiers.contains(ModifierKey.CTRL)) }
        var hasShift by remember { mutableStateOf(keyToEdit.modifiers.contains(ModifierKey.SHIFT)) }
        var hasAlt by remember { mutableStateOf(keyToEdit.modifiers.contains(ModifierKey.ALT)) }
        var hasCmd by remember { mutableStateOf(keyToEdit.modifiers.contains(ModifierKey.CMD)) }

        AlertDialog(
            onDismissRequest = { editingShortcut = null },
            title = {
                Text("Configurer le raccourci clavier", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Nom de l'action (ex: Coupe cutter)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it },
                        label = { Text("Touche (ex: C, K, Espace, Del)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Catégorie (Découpe, Outils...)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Touches Modificateurs :", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasCtrl, onCheckedChange = { hasCtrl = it })
                            Text("Ctrl", color = TextPrimary, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasShift, onCheckedChange = { hasShift = it })
                            Text("Maj", color = TextPrimary, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasAlt, onCheckedChange = { hasAlt = it })
                            Text("Alt", color = TextPrimary, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasCmd, onCheckedChange = { hasCmd = it })
                            Text("Win", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mods = mutableSetOf<ModifierKey>()
                        if (hasCtrl) mods.add(ModifierKey.CTRL)
                        if (hasShift) mods.add(ModifierKey.SHIFT)
                        if (hasAlt) mods.add(ModifierKey.ALT)
                        if (hasCmd) mods.add(ModifierKey.CMD)

                        val updated = keyToEdit.copy(
                            label = label.trim().ifEmpty { "Raccourci" },
                            keySymbol = symbol.trim().ifEmpty { "A" },
                            category = category.trim().ifEmpty { "Outils" },
                            modifiers = mods
                        )
                        onSaveShortcut(updated)
                        editingShortcut = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg)
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingShortcut = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Modal Create Software Profile Dialog
    if (showCreateProfileDialog) {
        var profileName by remember { mutableStateOf("") }
        var profileCategory by remember { mutableStateOf("Mon Logiciel") }

        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            title = {
                Text("Créer un profil logiciel personnalisé", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Duplique les outils de base et vous permet d'adapter chaque raccourci à votre station de travail.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Nom du logiciel (ex: Blender, Avid, CapCut)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = profileCategory,
                        onValueChange = { profileCategory = it },
                        label = { Text("Type (Montage, FX, 3D...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onCreateProfile(profileName.trim(), profileCategory.trim())
                            showCreateProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg)
                ) {
                    Text("Créer le profil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

/** "Ctrl + Maj + K" style label (French key names). */
private fun comboLabel(shortcut: ShortcutKey): String {
    val mods = shortcut.modifiers.sortedBy { it.ordinal }.map {
        when (it) {
            ModifierKey.CTRL -> "Ctrl"
            ModifierKey.SHIFT -> "Maj"
            ModifierKey.ALT -> "Alt"
            ModifierKey.CMD -> "Win"
        }
    }
    val key = when (shortcut.keySymbol.uppercase()) {
        "ESPACE" -> "Espace"
        "DEL" -> "Suppr"
        "ENTRER" -> "Entrée"
        "HAUT" -> "↑"
        "BAS" -> "↓"
        "GAUCHE" -> "←"
        "DROITE" -> "→"
        else -> shortcut.keySymbol
    }
    return (mods + key).joinToString(" + ")
}
