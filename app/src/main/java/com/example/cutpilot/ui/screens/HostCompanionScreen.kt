package com.example.cutpilot.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.model.ConnectionMode
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.model.LatencyInfo
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
import com.example.cutpilot.viewmodel.HostSimState

@Composable
fun HostCompanionScreen(
    connectionMode: ConnectionMode,
    connectionStatus: ConnectionStatus,
    connectedDeviceName: String?,
    latencyInfo: LatencyInfo,
    simState: HostSimState,
    localWifiIp: String,
    pythonServerCode: String,
    onSelectConnectionMode: (ConnectionMode) -> Unit,
    onConfigureWifiTarget: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Connexion, 1: Simulateur PC en direct, 2: Script Wi-Fi
    var inputIp by remember { mutableStateOf("192.168.1.100") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Sub-tabs
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = SurfaceDark,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = NeonCyan
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Appairage Rapide", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Simulateur PC", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Serveur Wi-Fi", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSubTab) {
            0 -> {
                // Connection Mode Selector
                Text(
                    text = "PROTOCOLE DE LIAISON",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bluetooth HID Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectConnectionMode(ConnectionMode.BLUETOOTH_HID) }
                            .testTag("mode_bluetooth_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (connectionMode == ConnectionMode.BLUETOOTH_HID)
                                NeonCyan.copy(alpha = 0.15f) else SurfaceContainerDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (connectionMode == ConnectionMode.BLUETOOTH_HID) NeonCyan else SurfaceContainerHighDark
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonCyan)
                                if (connectionMode == ConnectionMode.BLUETOOTH_HID) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonCyan)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACTIF", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bluetooth HID", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Zéro pilote • Natif PC & Mac", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reconnu immédiatement comme souris et clavier physique sans rien installer sur l'ordinateur.", color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    // Wi-Fi Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectConnectionMode(ConnectionMode.WIFI) }
                            .testTag("mode_wifi_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (connectionMode == ConnectionMode.WIFI)
                                AmberAccent.copy(alpha = 0.15f) else SurfaceContainerDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (connectionMode == ConnectionMode.WIFI) AmberAccent else SurfaceContainerHighDark
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = AmberAccent)
                                if (connectionMode == ConnectionMode.WIFI) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AmberAccent)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACTIF", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Wi-Fi Réseau", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Portée maximale • UDP", color = AmberAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Idéal pour contrôler votre station de travail à travers toute la pièce via le réseau local.", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zero-Install Bluetooth Pairing Guide
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "🚀 Appairage Sans Aucun Logiciel Hôte (Bluetooth HID)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val steps = listOf(
                        "1. Sur votre PC (Windows 10/11) ou Mac : ouvrez les Paramètres Bluetooth.",
                        "2. Cliquez sur 'Ajouter un appareil' ou 'Associer un nouvel appareil'.",
                        "3. Sélectionnez 'Vôlavo Mouse & Deck' dans la liste.",
                        "4. Terminé ! Le système hôte le reconnaît immédiatement comme une véritable souris sans fil."
                    )
                    steps.forEach { step ->
                        Text(text = step, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerDark)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Latence mesurée : ${latencyInfo.pingMs} ms", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Taux de rafraîchissement 125 Hz • Zéro saccade sur timeline", color = TextTertiary, fontSize = 9.sp)
                        }
                    }
                }
            }

            1 -> {
                // Live Interactive PC Screen & Timeline Simulator
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Laptop, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Moniteur Virtuel de la Station Hôte", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("EN DIRECT", color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Ce canevas reproduit en temps réel le comportement de votre écran de montage PC (curseur, découpes, tête de lecture, zoom) :",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Virtual Screen Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A0C10))
                            .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(12.dp))
                            .testTag("virtual_pc_screen")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw simulated video preview window (top half)
                            val previewH = h * 0.45f
                            drawRect(
                                color = Color(0xFF141720),
                                topLeft = Offset(0f, 0f),
                                size = Size(w, previewH)
                            )
                            drawLine(
                                color = Color(0xFF262A38),
                                start = Offset(0f, previewH),
                                end = Offset(w, previewH),
                                strokeWidth = 2f
                            )

                            // Timeline tracks (bottom half)
                            val timelineY = previewH + 4.dp.toPx()
                            val trackH = (h - timelineY) / 2.5f

                            // Draw video track 1 clips
                            simState.clips.forEach { clip ->
                                val startX = (clip.startFrame.toFloat() / simState.totalFrames.toFloat()) * w
                                val endX = (clip.endFrame.toFloat() / simState.totalFrames.toFloat()) * w
                                val clipW = (endX - startX).coerceAtLeast(4f)

                                drawRoundRect(
                                    color = Color(clip.colorHex),
                                    topLeft = Offset(startX + 2f, timelineY + 2f),
                                    size = Size(clipW - 4f, trackH - 4f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )
                            }

                            // Draw audio track
                            drawRoundRect(
                                color = Color(0xFF22C55E).copy(alpha = 0.5f),
                                topLeft = Offset(4f, timelineY + trackH + 2f),
                                size = Size(w - 8f, trackH - 4f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )

                            // In-Out selection overlay
                            val inX = (simState.inPoint ?: 0).toFloat() / simState.totalFrames * w
                            val outX = (simState.outPoint ?: simState.totalFrames).toFloat() / simState.totalFrames * w
                            drawRect(
                                color = NeonCyan.copy(alpha = 0.15f),
                                topLeft = Offset(inX, timelineY),
                                size = Size(outX - inX, h - timelineY)
                            )

                            // Playhead line
                            val playheadX = (simState.currentFrame.toFloat() / simState.totalFrames.toFloat()) * w
                            drawLine(
                                color = RazorRed,
                                start = Offset(playheadX, 0f),
                                end = Offset(playheadX, h),
                                strokeWidth = 3f
                            )
                            // Playhead triangular cap
                            drawCircle(
                                color = RazorRed,
                                radius = 5.dp.toPx(),
                                center = Offset(playheadX, 8.dp.toPx())
                            )

                            // Virtual Mouse Cursor
                            val curX = simState.cursorX * w
                            val curY = simState.cursorY * h

                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = Offset(curX, curY)
                            )
                            drawCircle(
                                color = NeonCyan,
                                radius = 8.dp.toPx(),
                                center = Offset(curX, curY),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Real-time Action Log
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerDark)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = simState.lastActionLog,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }

            2 -> {
                // Wi-Fi Host Companion Script & Manual Target
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "📡 Configuration Wi-Fi (Alternative au Bluetooth)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Adresse IP locale du smartphone : $localWifiIp",
                        color = AmberAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputIp,
                        onValueChange = { inputIp = it },
                        label = { Text("Adresse IP du PC hôte (ex: 192.168.1.50)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onConfigureWifiTarget(inputIp.trim())
                            Toast.makeText(context, "Cible Wi-Fi définie sur $inputIp:8888", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = DarkBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connecter à l'adresse IP de la station", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Script Python Compagnon (15 lignes)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("CutPilot Server Script", pythonServerCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Script copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighDark, contentColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copier", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF08090D))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = pythonServerCode,
                            color = Color(0xFF6EE7B7),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
