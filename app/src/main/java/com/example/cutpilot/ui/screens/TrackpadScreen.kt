package com.example.cutpilot.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.bluetooth.HidConstants
import com.example.cutpilot.model.ControllerSettings
import com.example.cutpilot.model.ShortcutKey
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

@Composable
fun TrackpadScreen(
    settings: ControllerSettings,
    isPlaying: Boolean,
    activeProfileName: String,
    onMouseMove: (Float, Float) -> Unit,
    onMouseClick: (Int) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onQuickCut: () -> Unit,
    onQuickUndo: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var isDragLocked by remember { mutableStateOf(false) }
    var showSensitivitySlider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(8.dp)
    ) {
        // Quick Studio Action Bar (Cut, Play/Pause, Undo, Zoom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activeProfileName,
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Razor Cut quick trigger
                IconButton(
                    onClick = onQuickCut,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RazorRed.copy(alpha = 0.15f))
                        .testTag("quick_cut_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Outil Découpe",
                        tint = RazorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play / Pause
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) EmeraldGreen.copy(alpha = 0.25f) else SurfaceContainerHighDark)
                        .testTag("quick_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Lecture / Pause",
                        tint = if (isPlaying) EmeraldGreen else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom Out
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Dézoomer",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zoom In
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoomer",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Undo
                IconButton(
                    onClick = onQuickUndo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Annuler",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Touchpad Area & Vertical Scroll Strip
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Touchpad Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SurfaceContainerDark, SurfaceDark),
                            radius = 900f
                        )
                    )
                    .border(
                        1.dp,
                        if (settings.sniperMode) NeonCyan.copy(alpha = 0.6f) else SurfaceContainerHighDark,
                        RoundedCornerShape(16.dp)
                    )
                    .pointerInput(settings) {
                        detectTapGestures(
                            onTap = { offset ->
                                touchPos = offset
                                onMouseClick(HidConstants.MOUSE_BUTTON_LEFT)
                            },
                            onDoubleTap = { offset ->
                                touchPos = offset
                                onMouseClick(HidConstants.MOUSE_BUTTON_LEFT)
                                onMouseClick(HidConstants.MOUSE_BUTTON_LEFT)
                            },
                            onLongPress = { offset ->
                                touchPos = offset
                                onMouseClick(HidConstants.MOUSE_BUTTON_RIGHT)
                            }
                        )
                    }
                    .pointerInput(settings) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                touchPos = offset
                                isDragging = true
                            },
                            onDragEnd = {
                                touchPos = null
                                isDragging = false
                            },
                            onDragCancel = {
                                touchPos = null
                                isDragging = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                touchPos = change.position
                                onMouseMove(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
                    .testTag("touchpad_surface")
            ) {
                // Precision grid and crosshair canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    val gridSpacing = 40.dp.toPx()
                    val gridColor = Color(0x18FFFFFF)
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

                    var x = gridSpacing
                    while (x < w) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                        x += gridSpacing
                    }

                    var y = gridSpacing
                    while (y < h) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                        y += gridSpacing
                    }

                    // Draw touch position indicator
                    touchPos?.let { pos ->
                        // Crosshair rings
                        drawCircle(
                            color = if (settings.sniperMode) NeonCyan else AmberAccent,
                            radius = 24.dp.toPx(),
                            center = pos,
                            alpha = 0.35f
                        )
                        drawCircle(
                            color = if (settings.sniperMode) NeonCyan else Color.White,
                            radius = 6.dp.toPx(),
                            center = pos
                        )
                        // Crosshair hairs
                        drawLine(
                            color = if (settings.sniperMode) NeonCyan else Color.White,
                            start = Offset(pos.x - 20f, pos.y),
                            end = Offset(pos.x + 20f, pos.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = if (settings.sniperMode) NeonCyan else Color.White,
                            start = Offset(pos.x, pos.y - 20f),
                            end = Offset(pos.x, pos.y + 20f),
                            strokeWidth = 2f
                        )
                    }
                }

                // Sub-pixel sniper banner
                if (settings.sniperMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎯 MODE PRÉCISION CHIRURGICALE ACTIF (1:1 PIXEL)",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center guide label
                if (touchPos == null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Surface Tactile Haute Résolution",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Glisser pour déplacer le curseur • Taper pour cliquer",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Appui long = Clic droit contextuel",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Vertical Timeline Scroll Strip (Molette de défilement)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onScroll(dragAmount.y, 0f)
                        }
                    }
                    .testTag("vertical_scroll_strip"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Défilement / Pan",
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )

                    // Tick lines
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(10) { i ->
                            Box(
                                modifier = Modifier
                                    .width(if (i % 2 == 0) 18.dp else 10.dp)
                                    .height(2.dp)
                                    .background(if (i % 2 == 0) TextSecondary else TextTertiary)
                            )
                        }
                    }

                    Text(
                        text = "SCROLL",
                        color = TextTertiary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Physical-Style Mouse Buttons (Left, Middle / Drag Lock, Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Click (Large)
            Button(
                onClick = {
                    onMouseClick(HidConstants.MOUSE_BUTTON_LEFT)
                },
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .testTag("left_click_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceContainerDark,
                    contentColor = TextPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CLIC GAUCHE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Sélection & Déplacement", fontSize = 9.sp, color = TextSecondary)
                }
            }

            // Middle Click with Drag Lock
            Button(
                onClick = {
                    isDragLocked = !isDragLocked
                    onMouseClick(HidConstants.MOUSE_BUTTON_MIDDLE)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("middle_click_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDragLocked) AmberAccent.copy(alpha = 0.25f) else SurfaceContainerDark,
                    contentColor = if (isDragLocked) AmberAccent else TextSecondary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Verrouillage de glissement",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isDragLocked) "GLISSEMENT ACTIF" else "MILIEU / PAN",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                }
            }

            // Right Click
            Button(
                onClick = {
                    onMouseClick(HidConstants.MOUSE_BUTTON_RIGHT)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .testTag("right_click_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceContainerDark,
                    contentColor = TextPrimary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CLIC DROIT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Menu contextuel", fontSize = 9.sp, color = TextSecondary)
                }
            }
        }
    }
}
