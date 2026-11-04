package com.example.cutpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ControlPointDuplicate
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.bluetooth.HidConstants
import com.example.cutpilot.model.ModifierKey
import com.example.cutpilot.ui.theme.AmberAccent
import com.example.cutpilot.ui.theme.DarkBg
import com.example.cutpilot.ui.theme.ElectricBlue
import com.example.cutpilot.ui.theme.EmeraldGreen
import com.example.cutpilot.ui.theme.NeonCyan
import com.example.cutpilot.ui.theme.PurpleAccent
import com.example.cutpilot.ui.theme.RazorRed
import com.example.cutpilot.ui.theme.SurfaceContainerDark
import com.example.cutpilot.ui.theme.SurfaceContainerHighDark
import com.example.cutpilot.ui.theme.SurfaceDark
import com.example.cutpilot.ui.theme.TextPrimary
import com.example.cutpilot.ui.theme.TextSecondary
import com.example.cutpilot.ui.theme.TextTertiary

@Composable
fun CanvaStudioScreen(
    isPlaying: Boolean,
    onTriggerKey: (Set<ModifierKey>, String) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTenPixelNudge by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Canva Banner Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF7D2AE8).copy(alpha = 0.25f),
                            Color(0xFF00C4CC).copy(alpha = 0.2f)
                        )
                    )
                )
                .border(1.dp, Color(0xFF00C4CC).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C4CC))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vôlavo pour Canva Studio",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Montage vidéo, scission de scènes et alignement millimétré",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00C4CC).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF00C4CC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("CANVA PRO", color = Color(0xFF00C4CC), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Section 1: Canva Video Timeline & Scène Controls
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MONTAGE VIDÉO CANVA (TIMELINE & SCÈNES)",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Split Video (S) - The essential Canva video cut tool!
                    Button(
                        onClick = { onTriggerKey(emptySet(), "S") },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("canva_split_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RazorRed.copy(alpha = 0.25f),
                            contentColor = RazorRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RazorRed)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("SCINDER (S)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Coupe vidéo Canva", fontSize = 8.sp, color = TextPrimary)
                            }
                        }
                    }

                    // Play / Pause Video
                    Button(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("canva_play_pause_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) EmeraldGreen.copy(alpha = 0.3f) else SurfaceContainerHighDark,
                            contentColor = if (isPlaying) EmeraldGreen else TextPrimary
                        ),
                        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen) else null
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "PAUSE" else "PLAY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Scene controls (Duplicate scene, New scene, Delete, Undo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvaActionButton(
                        icon = Icons.Default.ControlPointDuplicate,
                        label = "Dupliquer Scène",
                        shortcut = "Ctrl + D",
                        color = NeonCyan,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "D") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.Add,
                        label = "Nouvelle Scène",
                        shortcut = "Ctrl + Entrée",
                        color = EmeraldGreen,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "ENTER") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.Delete,
                        label = "Supprimer",
                        shortcut = "Suppr",
                        color = RazorRed,
                        onClick = { onTriggerKey(emptySet(), "DEL") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.Undo,
                        label = "Annuler",
                        shortcut = "Ctrl + Z",
                        color = TextSecondary,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "Z") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 2: Quick Elements 1-Tap Insert (Texte, Rectangle, Cercle, Ligne)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "INSERTION INSTANTANÉE D'ÉLÉMENTS SUR CANVA",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvaElementTile(
                        icon = Icons.Default.TextFields,
                        title = "Texte (T)",
                        subtitle = "Titre / texte",
                        accentColor = AmberAccent,
                        onClick = { onTriggerKey(emptySet(), "T") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaElementTile(
                        icon = Icons.Default.CropLandscape,
                        title = "Rectangle (R)",
                        subtitle = "Bandeau / fond",
                        accentColor = Color(0xFF00C4CC),
                        onClick = { onTriggerKey(emptySet(), "R") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaElementTile(
                        icon = Icons.Default.RadioButtonUnchecked,
                        title = "Cercle (C)",
                        subtitle = "Pastille / sticker",
                        accentColor = PurpleAccent,
                        onClick = { onTriggerKey(emptySet(), "C") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaElementTile(
                        icon = Icons.Default.ShowChart,
                        title = "Ligne (L)",
                        subtitle = "Séparateur",
                        accentColor = ElectricBlue,
                        onClick = { onTriggerKey(emptySet(), "L") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 3: Precision Nudge Pad (Ajustement Millimétré au pixel près)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PAVÉ D'ALIGNEMENT MILLIMÉTRÉ (NUDGE)",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTenPixelNudge) "Pas rapide : 10 pixels (Shift + Flèche)" else "Précision chirurgicale : 1 pixel (Flèche)",
                            color = if (isTenPixelNudge) AmberAccent else NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("10px", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isTenPixelNudge,
                            onCheckedChange = { isTenPixelNudge = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberAccent,
                                checkedTrackColor = AmberAccent.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // D-Pad Cross
                val nudgeMods = if (isTenPixelNudge) setOf(ModifierKey.SHIFT) else emptySet()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // UP
                    IconButton(
                        onClick = { onTriggerKey(nudgeMods, "HAUT") },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHighDark)
                            .testTag("nudge_up_btn")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Haut", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }

                    // LEFT - CENTER (Fit 100%) - RIGHT
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onTriggerKey(nudgeMods, "GAUCHE") },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHighDark)
                                .testTag("nudge_left_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Gauche", tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }

                        // Center: Fit to screen / 100% (Ctrl + 0)
                        Button(
                            onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "0") },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .testTag("canva_fit_100_btn"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = "Ajuster 100%", tint = NeonCyan, modifier = Modifier.size(22.dp))
                        }

                        IconButton(
                            onClick = { onTriggerKey(nudgeMods, "DROITE") },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHighDark)
                                .testTag("nudge_right_btn")
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Droite", tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }
                    }

                    // DOWN
                    IconButton(
                        onClick = { onTriggerKey(nudgeMods, "BAS") },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHighDark)
                            .testTag("nudge_down_btn")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Bas", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Section 4: Layers, Grouping & Canva View
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CALQUES, GROUPES & ZOOM CANVA",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                // Row 1: Layer order
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvaActionButton(
                        icon = Icons.Default.FlipToFront,
                        label = "Premier Plan",
                        shortcut = "Ctrl+Shift+]",
                        color = Color(0xFF00C4CC),
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL, ModifierKey.SHIFT), "]") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.Layers,
                        label = "Avancer (+1)",
                        shortcut = "Ctrl + ]",
                        color = ElectricBlue,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "]") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.Layers,
                        label = "Reculer (-1)",
                        shortcut = "Ctrl + [",
                        color = ElectricBlue,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "[") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.FlipToBack,
                        label = "Arrière-Plan",
                        shortcut = "Ctrl+Shift+[",
                        color = Color(0xFF00C4CC),
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL, ModifierKey.SHIFT), "[") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Grouping, Lock & Presentation Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvaActionButton(
                        icon = Icons.Default.Group,
                        label = "Grouper",
                        shortcut = "Ctrl + G",
                        color = PurpleAccent,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "G") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.GroupWork,
                        label = "Dissocier",
                        shortcut = "Ctrl+Shift+G",
                        color = PurpleAccent,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL, ModifierKey.SHIFT), "G") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.Lock,
                        label = "Verrouiller",
                        shortcut = "Alt+Shift+L",
                        color = AmberAccent,
                        onClick = { onTriggerKey(setOf(ModifierKey.ALT, ModifierKey.SHIFT), "L") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.PresentToAll,
                        label = "Présentation",
                        shortcut = "Ctrl+Alt+P",
                        color = EmeraldGreen,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL, ModifierKey.ALT), "P") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Zoom & Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvaActionButton(
                        icon = Icons.Default.ZoomIn,
                        label = "Zoomer",
                        shortcut = "Ctrl + +",
                        color = TextPrimary,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "+") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.ZoomOut,
                        label = "Dézoomer",
                        shortcut = "Ctrl + -",
                        color = TextPrimary,
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL), "-") },
                        modifier = Modifier.weight(1f)
                    )

                    CanvaActionButton(
                        icon = Icons.Default.Download,
                        label = "Télécharger",
                        shortcut = "Ctrl+Shift+E",
                        color = Color(0xFFE91E63),
                        onClick = { onTriggerKey(setOf(ModifierKey.CTRL, ModifierKey.SHIFT), "E") },
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun CanvaElementTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerDark)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextTertiary, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
fun CanvaActionButton(
    icon: ImageVector,
    label: String,
    shortcut: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerDark)
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(shortcut, color = TextTertiary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
