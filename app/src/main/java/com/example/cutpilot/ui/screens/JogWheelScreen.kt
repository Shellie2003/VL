package com.example.cutpilot.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JogWheelScreen(
    currentFrame: Int,
    totalFrames: Int,
    isPlaying: Boolean,
    timecode: String,
    inPoint: Int?,
    outPoint: Int?,
    onJogTick: (Int, Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onShuttleReverse: () -> Unit,
    onShuttleForward: () -> Unit,
    onMarkIn: () -> Unit,
    onMarkOut: () -> Unit,
    onQuickCut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialAngleDegrees by remember { mutableFloatStateOf(0f) }
    var lastTouchAngle by remember { mutableDoubleStateOf(0.0) }
    var angleAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Timeline Progress Bar & Timecode Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TIMECODE SMPTE",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timecode,
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("jog_timecode_display")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "IMAGE CLÉ",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currentFrame / $totalFrames",
                        color = AmberAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini visual timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceContainerDark)
            ) {
                // In/Out span
                val progress = (currentFrame.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
                val inFraction = inPoint?.let { (it.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f) } ?: 0f
                val outFraction = outPoint?.let { (it.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f) } ?: 1f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // In-to-Out zone
                    val inX = inFraction * w
                    val outX = outFraction * w
                    drawRect(
                        color = EmeraldGreen.copy(alpha = 0.25f),
                        topLeft = Offset(inX, 0f),
                        size = androidx.compose.ui.geometry.Size((outX - inX).coerceAtLeast(0f), h)
                    )

                    // Playhead line
                    val playheadX = progress * w
                    drawLine(
                        color = RazorRed,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        // Rotary Jog / Shuttle Wheel (Molette tactile de précision)
        Box(
            modifier = Modifier
                .size(240.dp)
                .aspectRatio(1f)
                .testTag("jog_wheel_rotary")
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cx = 120.dp.toPx()
                            val cy = 120.dp.toPx()
                            lastTouchAngle = Math.toDegrees(atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble()))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val cx = 120.dp.toPx()
                            val cy = 120.dp.toPx()
                            val currentAngle = Math.toDegrees(atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble()))

                            var delta = (currentAngle - lastTouchAngle).toFloat()
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f

                            dialAngleDegrees += delta
                            angleAccumulator += delta
                            lastTouchAngle = currentAngle

                            // Each 10 degrees is 1 frame tick
                            val stepDegrees = 9.0f
                            if (Math.abs(angleAccumulator) >= stepDegrees) {
                                val steps = (angleAccumulator / stepDegrees).toInt()
                                onJogTick(steps, false)
                                angleAccumulator -= steps * stepDegrees
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.width / 2 - 8.dp.toPx()
                val innerRadius = outerRadius - 32.dp.toPx()

                // Outer metallic ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SurfaceContainerHighDark, SurfaceDark, DarkBg),
                        center = center,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = center
                )

                // Dial border
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.4f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Notches along circumference
                val totalNotches = 36
                for (i in 0 until totalNotches) {
                    val angleRad = Math.toRadians((i * (360.0 / totalNotches) + dialAngleDegrees))
                    val isMajor = i % 3 == 0
                    val tickLen = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                    val p1 = Offset(
                        center.x + (outerRadius - tickLen) * cos(angleRad).toFloat(),
                        center.y + (outerRadius - tickLen) * sin(angleRad).toFloat()
                    )
                    val p2 = Offset(
                        center.x + outerRadius * cos(angleRad).toFloat(),
                        center.y + outerRadius * sin(angleRad).toFloat()
                    )
                    drawLine(
                        color = if (isMajor) NeonCyan else TextTertiary,
                        start = p1,
                        end = p2,
                        strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Inner core
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(SurfaceDark, SurfaceContainerDark),
                        start = Offset(center.x - innerRadius, center.y - innerRadius),
                        end = Offset(center.x + innerRadius, center.y + innerRadius)
                    ),
                    radius = innerRadius,
                    center = center
                )
                drawCircle(
                    color = AmberAccent.copy(alpha = 0.3f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Finger dimple (Creux pour doigt)
                val dimpleDist = innerRadius * 0.65f
                val rad = Math.toRadians(dialAngleDegrees.toDouble())
                val dimpleCenter = Offset(
                    center.x + dimpleDist * cos(rad).toFloat(),
                    center.y + dimpleDist * sin(rad).toFloat()
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan, Color.Black),
                        center = dimpleCenter,
                        radius = 18.dp.toPx()
                    ),
                    radius = 16.dp.toPx(),
                    center = dimpleCenter
                )
            }

            // Central status indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "JOG",
                    color = NeonCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "1 image / cran",
                    color = TextTertiary,
                    fontSize = 9.sp
                )
            }
        }

        // Fine steps: short single-line labels, equal widths, 48 dp targets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StepButton("−1 s", "jog_minus_1s", emphasized = false, Modifier.weight(1f)) { onJogTick(-24, false) }
            StepButton("−1 img", "jog_minus_1frame", emphasized = true, Modifier.weight(1f)) { onJogTick(-1, true) }
            StepButton("+1 img", "jog_plus_1frame", emphasized = true, Modifier.weight(1f)) { onJogTick(1, true) }
            StepButton("+1 s", "jog_plus_1s", emphasized = false, Modifier.weight(1f)) { onJogTick(24, false) }
        }

        // Transport J · K · L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShuttleReverse,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerDark)
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Navette arrière (J)", tint = TextPrimary)
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) EmeraldGreen.copy(alpha = 0.25f) else NeonCyan.copy(alpha = 0.18f))
                    .border(2.dp, if (isPlaying) EmeraldGreen else NeonCyan, CircleShape)
                    .testTag("jog_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Lecture",
                    tint = if (isPlaying) EmeraldGreen else NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = onShuttleForward,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerDark)
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "Navette avant (L)", tint = TextPrimary)
            }
        }

        // Markers and cut: three equal, labelled buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton("Point IN", Icons.Default.BookmarkBorder, EmeraldGreen, "jog_mark_in", Modifier.weight(1f), onMarkIn)
            ActionButton("Couper", Icons.Default.ContentCut, RazorRed, "jog_razor_cut_btn", Modifier.weight(1f), onQuickCut)
            ActionButton("Point OUT", Icons.Default.Bookmark, EmeraldGreen, "jog_mark_out", Modifier.weight(1f), onMarkOut)
        }
    }
}

@Composable
private fun StepButton(label: String, tag: String, emphasized: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (emphasized) SurfaceContainerHighDark else SurfaceContainerDark)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (emphasized) NeonCyan else TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tag: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
    }
}
