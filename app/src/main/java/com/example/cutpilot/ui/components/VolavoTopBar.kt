package com.example.cutpilot.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cutpilot.model.ConnectionMode
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.model.LatencyInfo
import com.example.cutpilot.model.MainNavigationTab
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
import com.example.cutpilot.ui.theme.Warning

/** Destinations of the app menu, in display order. */
data class Destination(
    val tab: MainNavigationTab,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val group: Int
)

val Destinations = listOf(
    Destination(MainNavigationTab.TRACKPAD, "Trackpad", "Souris tactile", Icons.Default.Mouse, 0),
    Destination(MainNavigationTab.KEYBOARD, "Clavier", "AZERTY / QWERTY, texte, raccourcis", Icons.Default.Keyboard, 0),
    Destination(MainNavigationTab.JOG_WHEEL, "Molette Jog", "Timeline image par image", Icons.Default.RotateRight, 0),
    Destination(MainNavigationTab.DECK, "Deck de montage", "Premiere, DaVinci, Final Cut…", Icons.Default.Dashboard, 0),
    Destination(MainNavigationTab.CANVA, "Canva", "Outils de design Canva", Icons.Default.Palette, 0),
    Destination(MainNavigationTab.CONNECTION, "Connexion", "Appairer l'ordinateur (QR code)", Icons.Default.Link, 1),
    Destination(MainNavigationTab.SETTINGS, "Réglages", "Sensibilité, défilement, vibrations", Icons.Default.Settings, 1)
)

fun destinationOf(tab: MainNavigationTab) = Destinations.first { it.tab == tab }

/**
 * App bar: screen title, live connection status (tap → connection screen),
 * precision toggle where it matters, and the app menu at the top right.
 */
@Composable
fun VolavoTopBar(
    currentTab: MainNavigationTab,
    connectionMode: ConnectionMode,
    connectionStatus: ConnectionStatus,
    connectedDeviceName: String?,
    latencyInfo: LatencyInfo,
    isPrecisionMode: Boolean,
    onTogglePrecision: () -> Unit,
    onNavigate: (MainNavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val dest = destinationOf(currentTab)
    val statusColor = statusColor(connectionStatus)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Bg)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title + status (the status line is the shortcut to the connection screen)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onNavigate(MainNavigationTab.CONNECTION) }
                .padding(vertical = 4.dp)
                .testTag("connection_status_badge")
        ) {
            // App name + current screen (users always know where they are)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vôlavo", style = MaterialTheme.typography.titleLarge, color = OnBg, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Text(
                    dest.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimarySoft)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(connectionStatus)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusLine(connectionMode, connectionStatus, connectedDeviceName, latencyInfo),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connectionStatus == ConnectionStatus.CONNECTED) OnBgMuted else statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Precision mode only where the pointer is used
        if (currentTab == MainNavigationTab.TRACKPAD || currentTab == MainNavigationTab.JOG_WHEEL) {
            IconButton(
                onClick = onTogglePrecision,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isPrecisionMode) PrimarySoft else Color.Transparent,
                    contentColor = if (isPrecisionMode) Primary else OnBgMuted
                ),
                modifier = Modifier
                    .testTag("sniper_mode_toggle")
                    .semantics {
                        contentDescription = if (isPrecisionMode) "Désactiver le mode précision" else "Activer le mode précision"
                    }
            ) {
                Icon(Icons.Default.GpsFixed, contentDescription = null)
            }
        }

        // App menu (top right)
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag("app_menu_button")
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = OnBg)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier
                    .background(Surface1)
                    .widthIn(min = 280.dp)
            ) {
                Destinations.forEachIndexed { i, d ->
                    if (i > 0 && Destinations[i - 1].group != d.group) {
                        HorizontalDivider(color = Surface2, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    val selected = d.tab == currentTab
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    d.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Primary else OnBg
                                )
                                Text(d.description, style = MaterialTheme.typography.bodySmall, color = OnBgSubtle)
                            }
                        },
                        leadingIcon = {
                            Icon(d.icon, contentDescription = null, tint = if (selected) Primary else OnBgMuted)
                        },
                        trailingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = "Écran actuel", tint = Primary) }
                        } else null,
                        onClick = {
                            menuOpen = false
                            onNavigate(d.tab)
                        },
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PrimarySoft else Color.Transparent)
                            .testTag("menu_${d.tab.name.lowercase()}")
                    )
                }
            }
        }
    }
}

/**
 * Shown under the app bar on control screens while no computer is connected:
 * tells the user *why* nothing happens and offers the fix in one tap.
 */
@Composable
fun ConnectionBanner(
    connectionStatus: ConnectionStatus,
    errorMessage: String?,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (connectionStatus == ConnectionStatus.CONNECTED) return
    val connecting = connectionStatus == ConnectionStatus.CONNECTING || connectionStatus == ConnectionStatus.SCANNING
    // Compact (one line, ~48 dp): explains why nothing happens and offers the fix.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .clickable(enabled = !connecting, onClick = onConnect)
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SyncProblem,
                contentDescription = null,
                tint = if (connecting) Warning else Danger,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (connecting) "Connexion à l'ordinateur…" else "Aucun ordinateur connecté",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBgMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!connecting) {
                TextButton(
                    onClick = onConnect,
                    modifier = Modifier.testTag("banner_connect_button")
                ) { Text("Connecter", color = Primary, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (connecting) {
            LinearProgressIndicator(
                color = Warning,
                trackColor = Surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}

@Composable
private fun StatusDot(status: ConnectionStatus) {
    val pulsing = status == ConnectionStatus.CONNECTING || status == ConnectionStatus.SCANNING
    val alpha = if (pulsing) {
        val t = rememberInfiniteTransition(label = "dot")
        t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dotAlpha").value
    } else 1f
    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(statusColor(status))
    )
}

fun statusColor(status: ConnectionStatus): Color = when (status) {
    ConnectionStatus.CONNECTED -> Success
    ConnectionStatus.CONNECTING, ConnectionStatus.SCANNING -> Warning
    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> Danger
}

private fun statusLine(
    mode: ConnectionMode,
    status: ConnectionStatus,
    device: String?,
    latency: LatencyInfo
): String {
    val via = when (mode) {
        ConnectionMode.BLUETOOTH_HID -> "Bluetooth"
        ConnectionMode.WIFI -> "Wi-Fi"
        ConnectionMode.SIMULATOR -> "Simulateur"
    }
    return when (status) {
        ConnectionStatus.CONNECTED -> buildString {
            append(device?.removePrefix("Wi-Fi • ") ?: "Connecté")
            append(" · ").append(via)
            if (latency.measured) append(" · ").append(latency.pingMs).append(" ms")
        }
        ConnectionStatus.CONNECTING, ConnectionStatus.SCANNING -> "Connexion en $via…"
        ConnectionStatus.ERROR -> "Échec de connexion · toucher pour réessayer"
        ConnectionStatus.DISCONNECTED -> "Non connecté · toucher pour connecter"
    }
}
