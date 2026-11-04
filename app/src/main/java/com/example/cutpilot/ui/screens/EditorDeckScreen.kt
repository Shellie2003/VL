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
        // Profile Selector Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOGICIEL ACTIF",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { showCreateProfileDialog = true },
                modifier = Modifier.testTag("add_custom_profile_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nouveau profil", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(profiles) { profile ->
                val isSelected = profile.id == activeProfile.id
                val borderCol = if (isSelected) NeonCyan else SurfaceContainerHighDark
                val bgCol = if (isSelected) NeonCyan.copy(alpha = 0.15f) else SurfaceDark

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                        .clickable { onSelectProfile(profile) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("profile_item_${profile.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = profile.name,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent.copy(alpha = 0.2f),
                        selectedLabelColor = AmberAccent,
                        containerColor = SurfaceContainerDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == cat,
                        borderColor = SurfaceContainerHighDark,
                        selectedBorderColor = AmberAccent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid of Illuminated Macro Pads
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filteredShortcuts) { shortcut ->
                val padColor = Color(shortcut.colorHex)

                Box(
                    modifier = Modifier
                        .height(105.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SurfaceContainerDark,
                                    SurfaceDark
                                )
                            )
                        )
                        .border(1.5.dp, padColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .clickable { onTriggerShortcut(shortcut) }
                        .padding(8.dp)
                        .testTag("macro_pad_${shortcut.id}")
                ) {
                    // Category & Edit icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shortcut.category.uppercase(),
                            color = padColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { editingShortcut = shortcut },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Personnaliser",
                                tint = TextTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Key Combination Badge (e.g. Ctrl + K)
                    val modText = shortcut.modifiers.joinToString(" ") { it.name }
                    val keyDisplay = if (modText.isNotEmpty()) "$modText + ${shortcut.keySymbol}" else shortcut.keySymbol

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(padColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = keyDisplay,
                                color = padColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = shortcut.label,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // Bottom subtle dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(16.dp, 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(padColor)
                    )
                }
            }

            // Custom shortcut addition tile
            item {
                Box(
                    modifier = Modifier
                        .height(105.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(14.dp))
                        .clickable {
                            editingShortcut = ShortcutKey(
                                id = "custom_${System.currentTimeMillis()}",
                                label = "Nouveau Raccourci",
                                category = if (selectedCategory != "Tous") selectedCategory else "Outils",
                                keySymbol = "X",
                                colorHex = 0xFF00E5FF
                            )
                        }
                        .padding(8.dp)
                        .testTag("add_shortcut_pad"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ajouter",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ajouter Touche",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                            Text("Ctrl", color = TextPrimary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasShift, onCheckedChange = { hasShift = it })
                            Text("Shift", color = TextPrimary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasAlt, onCheckedChange = { hasAlt = it })
                            Text("Alt", color = TextPrimary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasCmd, onCheckedChange = { hasCmd = it })
                            Text("Cmd", color = TextPrimary, fontSize = 11.sp)
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
