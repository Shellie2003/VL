package com.example.cutpilot.ui.screens

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
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.model.ConnectionMode
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.model.LatencyInfo
import com.example.cutpilot.network.PairingInfo
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
    pairedHost: PairingInfo?,
    connectionError: String?,
    onSelectConnectionMode: (ConnectionMode) -> Unit,
    onScanQr: () -> Unit,
    onManualConnect: (ip: String, code: String) -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForgetPairing: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Connexion, 1: Simulateur PC en direct, 2: Logiciel PC
    var inputIp by remember { mutableStateOf(pairedHost?.hosts?.firstOrNull() ?: "") }
    var inputCode by remember { mutableStateOf("") }

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
                text = { Text("Appairer", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Simulateur", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Logiciel PC", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSubTab) {
            0 -> {
                QuickPairCard(
                    connectionMode = connectionMode,
                    connectionStatus = connectionStatus,
                    connectedDeviceName = connectedDeviceName,
                    latencyInfo = latencyInfo,
                    pairedHost = pairedHost,
                    connectionError = connectionError,
                    onScanQr = onScanQr,
                    onReconnect = onReconnect,
                    onDisconnect = onDisconnect,
                    onForgetPairing = onForgetPairing
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                        "Le plus simple : scannez le QR code de Vôlavo Hôte, le téléphone se connecte directement au Bluetooth du PC.",
                        "Sans logiciel : touchez « Rendre visible » ci-dessous, puis sur le PC ouvrez Paramètres › Bluetooth › Ajouter un appareil.",
                        "Sélectionnez « Vôlavo Mouse & Deck » : il est reconnu comme une vraie souris + clavier.",
                        "Les fois suivantes, la reconnexion est automatique."
                    )
                    steps.forEachIndexed { i, step ->
                        Text(text = "${i + 1}. $step", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onMakeDiscoverable,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighDark, contentColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rendre le téléphone visible (2 min)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                            Text(
                                if (latencyInfo.measured) "Latence aller-retour mesurée : ${latencyInfo.pingMs} ms" else "Liaison Bluetooth HID native",
                                color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp
                            )
                            Text("${latencyInfo.packetRateHz} paquets/s • envoi direct, mouvements fusionnés sans file d'attente", color = TextTertiary, fontSize = 9.sp)
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "💻 Vôlavo Hôte — le petit logiciel pour l'ordinateur",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val steps = listOf(
                        "Copiez le dossier « host » du projet Vôlavo sur l'ordinateur (Windows, macOS ou Linux, Python 3.8+).",
                        "Windows : double-cliquez sur start_windows.bat. macOS / Linux : lancez ./start_mac_linux.sh.",
                        "Une fenêtre affiche un QR code : touchez « Scanner le QR code » dans l'onglet Appairage Rapide.",
                        "C'est tout. Le téléphone se reconnecte tout seul aux prochains lancements."
                    )
                    steps.forEachIndexed { i, step ->
                        Text(text = "${i + 1}. $step", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Avantages Wi-Fi : défilement haute résolution, latence mesurée en direct, raccourcis indépendants de la disposition clavier (AZERTY/QWERTY).",
                        color = AmberAccent,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "⌨️ Connexion manuelle (sans caméra)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "IP de ce téléphone : $localWifiIp — saisissez l'adresse et le code affichés par Vôlavo Hôte.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputIp,
                        onValueChange = { inputIp = it.trim() },
                        label = { Text("Adresse IP du PC (ex : 192.168.1.50)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it.uppercase() },
                        label = { Text("Code (ex : ABCD-EFGH-JKLM-NPQR)") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onManualConnect(inputIp, inputCode) },
                        enabled = inputIp.isNotBlank() && PairingInfo.isValidKey(PairingInfo.normalizeKey(inputCode)),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = DarkBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connecter en Wi-Fi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPairCard(
    connectionMode: ConnectionMode,
    connectionStatus: ConnectionStatus,
    connectedDeviceName: String?,
    latencyInfo: LatencyInfo,
    pairedHost: PairingInfo?,
    connectionError: String?,
    onScanQr: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForgetPairing: () -> Unit
) {
    val statusColor = when (connectionStatus) {
        ConnectionStatus.CONNECTED -> EmeraldGreen
        ConnectionStatus.CONNECTING, ConnectionStatus.SCANNING -> AmberAccent
        ConnectionStatus.ERROR -> RazorRed
        ConnectionStatus.DISCONNECTED -> TextTertiary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Button(
            onClick = onScanQr,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("scan_qr_button")
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanner le QR code du PC", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> "Connecté — ${connectedDeviceName ?: "ordinateur"}"
                    ConnectionStatus.CONNECTING -> "Connexion en cours…"
                    ConnectionStatus.SCANNING -> "Recherche…"
                    ConnectionStatus.ERROR -> "Échec de connexion"
                    ConnectionStatus.DISCONNECTED -> "Non connecté"
                },
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            if (connectionStatus == ConnectionStatus.CONNECTED && latencyInfo.measured) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("${latencyInfo.pingMs} ms", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        connectionError?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = RazorRed, fontSize = 10.sp)
        }

        if (pairedHost != null && connectionMode != ConnectionMode.SIMULATOR) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PC mémorisé : ${pairedHost.hostName} (${pairedHost.hosts.first()})" +
                    (pairedHost.bluetoothMac?.let { " • BT $it" } ?: ""),
                color = TextTertiary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val chipColors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighDark, contentColor = TextPrimary)
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    Button(onClick = onDisconnect, colors = chipColors, modifier = Modifier.weight(1f)) {
                        Text("Déconnecter", fontSize = 11.sp)
                    }
                } else {
                    Button(onClick = onReconnect, colors = chipColors, modifier = Modifier.weight(1f)) {
                        Text("Reconnecter", fontSize = 11.sp)
                    }
                }
                Button(onClick = onForgetPairing, colors = chipColors, modifier = Modifier.weight(1f)) {
                    Text("Oublier ce PC", fontSize = 11.sp)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Lancez « Vôlavo Hôte » sur l'ordinateur puis scannez le code affiché. Astuce : sur la plupart des téléphones, l'appareil photo fonctionne aussi.",
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
