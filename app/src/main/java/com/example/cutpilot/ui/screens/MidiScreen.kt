package com.example.cutpilot.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.midi.MidiMessages
import com.example.cutpilot.midi.MidiRoute
import com.example.cutpilot.ui.theme.Accent
import com.example.cutpilot.ui.theme.Bg
import com.example.cutpilot.ui.theme.Danger
import com.example.cutpilot.ui.theme.OnBg
import com.example.cutpilot.ui.theme.OnBgMuted
import com.example.cutpilot.ui.theme.OnBgSubtle
import com.example.cutpilot.ui.theme.Primary
import com.example.cutpilot.ui.theme.PrimarySoft
import com.example.cutpilot.ui.theme.Success
import com.example.cutpilot.ui.theme.Surface1
import com.example.cutpilot.ui.theme.Surface2
import com.example.cutpilot.ui.theme.Surface3
import com.example.cutpilot.ui.theme.Warning
import kotlin.math.roundToInt

/**
 * Phone as a MIDI keyboard for FL Studio (or any DAW), landscape full screen:
 * pitch-bend strip (springs back to centre), modulation strip, sustain, multi-touch piano.
 */
@Composable
fun MidiScreen(
    route: MidiRoute,
    outputLabel: String,
    onNoteOn: (note: Int, velocity: Int) -> Unit,
    onNoteOff: (note: Int) -> Unit,
    onPitchBend: (Int) -> Unit,
    onModulation: (Int) -> Unit,
    onSustain: (Boolean) -> Unit,
    onPanic: () -> Unit,
    onRescanUsb: () -> Unit,
    onOpenTrackpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    var octave by rememberSaveable { mutableIntStateOf(4) }      // lowest C = 12 * octave (FL: C4 = 48)
    var octaves by rememberSaveable { mutableIntStateOf(2) }     // 1..3 octaves on screen
    var velocityByPosition by rememberSaveable { mutableStateOf(true) }
    var showHelp by remember { mutableStateOf(false) }

    val lowest = (octave * 12).coerceIn(0, 127 - octaves * 12)

    // Never leave a note hanging when the screen goes away.
    DisposableEffect(Unit) { onDispose { onPanic() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // --- Toolbar -----------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = onOpenTrackpad, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Mouse, contentDescription = "Trackpad", tint = Primary)
            }
            ToolChip(Icons.Default.Remove, "Octave −", enabled = lowest > 0) { octave = (octave - 1).coerceAtLeast(0) }
            Text(
                "${MidiMessages.noteName(lowest)} – ${MidiMessages.noteName(lowest + octaves * 12)}",
                color = OnBg,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(96.dp),
                maxLines = 1
            )
            ToolChip(Icons.Default.Add, "Octave +", enabled = lowest + octaves * 12 < 120) { octave += 1 }
            TextPill("${octaves} oct.") { octaves = if (octaves >= 3) 1 else octaves + 1 }
            TextPill(if (velocityByPosition) "Vélocité : frappe" else "Vélocité : fixe") { velocityByPosition = !velocityByPosition }
            Spacer(Modifier.weight(1f))
            RouteChip(route, outputLabel, onClick = onRescanUsb)
            IconButton(onClick = { showHelp = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Configurer FL Studio", tint = OnBgMuted)
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Left controllers: pitch bend, modulation, sustain
            BendStrip(onPitchBend, Modifier.width(58.dp).fillMaxHeight())
            Column(Modifier.width(58.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModStrip(onModulation, Modifier.weight(1f).fillMaxWidth())
                SustainPedal(onSustain, Modifier.height(64.dp).fillMaxWidth())
            }
            PianoKeyboard(
                lowestNote = lowest,
                octaves = octaves,
                velocityByPosition = velocityByPosition,
                onNoteOn = onNoteOn,
                onNoteOff = onNoteOff,
                modifier = Modifier.weight(1f).fillMaxHeight().testTag("midi_piano")
            )
        }
    }

    if (showHelp) MidiHelpDialog(onDismiss = { showHelp = false })
}

// =============================================================================
// Piano
// =============================================================================
@Composable
private fun PianoKeyboard(
    lowestNote: Int,
    octaves: Int,
    velocityByPosition: Boolean,
    onNoteOn: (Int, Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val whiteCount = octaves * 7 + 1 // ends on a C, like a real keyboard
    // Pressed notes are only read while drawing: touching keys never recomposes the screen.
    val pressed = remember { mutableStateOf(emptySet<Int>()) }
    val noteOn by rememberUpdatedState(onNoteOn)
    val noteOff by rememberUpdatedState(onNoteOff)
    val byPosition by rememberUpdatedState(velocityByPosition)
    val measurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(lowestNote, whiteCount) {
                val fingerNote = HashMap<PointerId, Int>()
                val count = IntArray(128)
                fun press(n: Int, y: Float) {
                    count[n]++
                    if (count[n] == 1) {
                        pressed.value = pressed.value + n
                    }
                    val vel = if (byPosition) MidiMessages.velocityFromY(y, size.height.toFloat()) else 100
                    noteOn(n, vel)
                }
                fun release(n: Int) {
                    if (count[n] > 0) count[n]--
                    if (count[n] == 0) pressed.value = pressed.value - n
                    noteOff(n)
                }
                try {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (c in event.changes) {
                                if (c.pressed) {
                                    val n = MidiMessages.noteAt(
                                        c.position.x, c.position.y, size.width.toFloat(), size.height.toFloat(),
                                        lowestNote, whiteCount
                                    )
                                    val prev = fingerNote[c.id]
                                    if (prev != n) { // new touch or glissando onto another key
                                        if (prev != null) release(prev)
                                        if (n != null) {
                                            fingerNote[c.id] = n
                                            press(n, c.position.y)
                                        } else {
                                            fingerNote.remove(c.id)
                                        }
                                    }
                                } else {
                                    fingerNote.remove(c.id)?.let { release(it) }
                                }
                                c.consume()
                            }
                        }
                    }
                } finally {
                    fingerNote.values.forEach { release(it) }
                    fingerNote.clear()
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val whiteW = w / whiteCount
        val blackW = whiteW * 0.6f
        val blackH = h * 0.62f
        val down = pressed.value

        for (i in 0 until whiteCount) {
            val note = MidiMessages.whiteNote(lowestNote, i)
            val isDown = note in down
            drawRoundRect(
                color = if (isDown) Primary else Color(0xFFE9ECF3),
                topLeft = Offset(i * whiteW + 1.5f, 0f),
                size = Size(whiteW - 3f, h),
                cornerRadius = CornerRadius(8f, 8f)
            )
            if (note % 12 == 0) { // label every C
                val label = measurer.measure(
                    MidiMessages.noteName(note),
                    TextStyle(color = if (isDown) Color.White else Color(0xFF5B6275), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                )
                drawText(
                    label,
                    topLeft = Offset(i * whiteW + (whiteW - label.size.width) / 2, h - label.size.height - 10f)
                )
            }
        }
        for (i in 0 until whiteCount - 1) {
            if (!MidiMessages.hasBlackAfter(i % 7)) continue
            val note = MidiMessages.whiteNote(lowestNote, i) + 1
            val isDown = note in down
            drawRoundRect(
                color = if (isDown) Primary else Color(0xFF151922),
                topLeft = Offset((i + 1) * whiteW - blackW / 2, 0f),
                size = Size(blackW, blackH),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

// =============================================================================
// Controllers
// =============================================================================
/** Pitch bend: drag up/down, springs back to the centre on release. */
@Composable
private fun BendStrip(onBend: (Int) -> Unit, modifier: Modifier) {
    var pos by remember { mutableFloatStateOf(0f) } // -1..1
    val bend by rememberUpdatedState(onBend)
    StripFrame("Bend", modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("midi_pitch_bend")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(y: Float) {
                            val half = size.height / 2f
                            pos = ((half - y) / half).coerceIn(-1f, 1f)
                            bend((pos * if (pos >= 0) 8191 else 8192).roundToInt())
                        }
                        update(down.position.y)
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            update(c.position.y)
                            c.consume()
                        }
                        pos = 0f
                        bend(0)
                    }
                }
        ) {
            val cy = size.height / 2f
            drawLine(OnBgSubtle, Offset(8f, cy), Offset(size.width - 8f, cy), strokeWidth = 2f)
            val y = cy - pos * cy
            drawRoundRect(
                color = if (pos == 0f) Surface3 else Primary,
                topLeft = Offset(6f, y - 22f),
                size = Size(size.width - 12f, 44f),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }
    }
}

/** Modulation wheel (CC1): stays where it is left. */
@Composable
private fun ModStrip(onMod: (Int) -> Unit, modifier: Modifier) {
    var value by rememberSaveable { mutableIntStateOf(0) }
    val mod by rememberUpdatedState(onMod)
    StripFrame("Mod", modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("midi_modulation")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(y: Float) {
                            val v = (127 * (1f - y / size.height)).roundToInt().coerceIn(0, 127)
                            if (v != value) {
                                value = v
                                mod(v)
                            }
                        }
                        update(down.position.y)
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            update(c.position.y)
                            c.consume()
                        }
                    }
                }
        ) {
            val fill = size.height * value / 127f
            drawRoundRect(
                color = Accent.copy(alpha = 0.35f),
                topLeft = Offset(6f, size.height - fill),
                size = Size(size.width - 12f, fill),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
private fun StripFrame(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Surface3, RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = OnBgMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

/** Sustain (CC64): held while the finger is on it, like a pedal. */
@Composable
private fun SustainPedal(onSustain: (Boolean) -> Unit, modifier: Modifier) {
    var down by remember { mutableStateOf(false) }
    val cb by rememberUpdatedState(onSustain)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (down) Warning.copy(alpha = 0.35f) else Surface2)
            .border(1.dp, if (down) Warning else Surface3, RoundedCornerShape(12.dp))
            .testTag("midi_sustain")
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    down = true
                    cb(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        down = false
                        cb(false)
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Sustain", color = if (down) Warning else OnBgMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// =============================================================================
// Toolbar bits
// =============================================================================
@Composable
private fun ToolChip(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = description, tint = if (enabled) OnBg else OnBgSubtle)
    }
}

@Composable
private fun TextPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = OnBg, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun RouteChip(route: MidiRoute, label: String, onClick: () -> Unit) {
    val color = when (route) {
        MidiRoute.USB -> Success
        MidiRoute.WIFI -> Accent
        MidiRoute.NONE -> Danger
    }
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PrimarySoft.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag("midi_route"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = OnBg, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun MidiHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Utiliser avec FL Studio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("USB — recommandé (latence ~1–3 ms, aucun pilote)", color = Success, fontWeight = FontWeight.SemiBold)
                Text(
                    "1. Branchez le téléphone au PC avec un câble USB.\n" +
                        "2. Dans la notification Android « Chargement via USB », choisissez « MIDI ».\n" +
                        "3. Touchez le voyant de sortie en haut à droite pour détecter la liaison.\n" +
                        "4. FL Studio › Options › MIDI settings : sélectionnez le téléphone dans Input, " +
                        "cochez « Enable », Controller type « (generic controller) ».",
                    color = OnBgMuted, fontSize = 13.sp
                )
                Text("Wi-Fi — via Vôlavo Hôte (~3–10 ms)", color = Accent, fontWeight = FontWeight.SemiBold)
                Text(
                    "1. Installez une fois loopMIDI (gratuit, tobias-erichsen.de) sur le PC.\n" +
                        "2. Lancez Vôlavo Hôte et connectez le téléphone en Wi-Fi (QR code).\n" +
                        "3. FL Studio › Options › MIDI settings : activez l'entrée « Volavo MIDI ».",
                    color = OnBgMuted, fontSize = 13.sp
                )
                Text(
                    "Pitch bend : glissez la bande « Bend », elle revient au centre en la lâchant. " +
                        "L'amplitude (±2 demi-tons par défaut) se règle dans l'instrument de FL Studio. " +
                        "« Mod » envoie la modulation (CC1), « Sustain » la pédale (CC64). " +
                        "Plus vous frappez une touche bas, plus la note est forte (vélocité).",
                    color = OnBgSubtle, fontSize = 12.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Compris", color = Primary) } },
        containerColor = Surface1
    )
}
