package com.example.cutpilot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.model.BatteryInfo
import com.example.cutpilot.model.ConnectionMode
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.model.LatencyInfo
import com.example.cutpilot.ui.theme.AmberAccent
import com.example.cutpilot.ui.theme.DarkBg
import com.example.cutpilot.ui.theme.EmeraldGreen
import com.example.cutpilot.ui.theme.NeonCyan
import com.example.cutpilot.ui.theme.RazorRed
import com.example.cutpilot.ui.theme.SurfaceContainerDark
import com.example.cutpilot.ui.theme.TextPrimary
import com.example.cutpilot.ui.theme.TextSecondary
import com.example.cutpilot.ui.theme.TextTertiary

@Composable
fun TopStudioBar(
    connectionMode: ConnectionMode,
    connectionStatus: ConnectionStatus,
    connectedDeviceName: String?,
    latencyInfo: LatencyInfo,
    batteryInfo: BatteryInfo,
    isSniperMode: Boolean,
    onToggleSniperMode: () -> Unit,
    onOpenConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Connection status pill
        val statusColor by animateColorAsState(
            targetValue = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> EmeraldGreen
                ConnectionStatus.CONNECTING, ConnectionStatus.SCANNING -> AmberAccent
                ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> RazorRed
            },
            label = "status_color"
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainerDark)
                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { onOpenConnection() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("connection_status_badge"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (connectionMode) {
                    ConnectionMode.BLUETOOTH_HID -> Icons.Default.Bluetooth
                    ConnectionMode.WIFI -> Icons.Default.Wifi
                    ConnectionMode.SIMULATOR -> Icons.Default.Speed
                },
                contentDescription = "Mode de connexion",
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = connectedDeviceName ?: "Prêt pour appairage",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> if (latencyInfo.measured) "${latencyInfo.pingMs} ms • ${latencyInfo.packetRateHz} pkt/s" else "Liaison directe • ${latencyInfo.packetRateHz} pkt/s"
                        ConnectionStatus.CONNECTING -> "Connexion en cours…"
                        ConnectionStatus.SCANNING -> "Recherche hôte…"
                        ConnectionStatus.DISCONNECTED -> "Non appairé"
                        ConnectionStatus.ERROR -> "Erreur liaison"
                    },
                    color = if (connectionStatus == ConnectionStatus.CONNECTED) EmeraldGreen else TextTertiary,
                    fontSize = 9.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sniper / Precision Mode Toggle
            val sniperBg = if (isSniperMode) NeonCyan.copy(alpha = 0.2f) else SurfaceContainerDark
            val sniperBorder = if (isSniperMode) NeonCyan else Color.Transparent
            val sniperColor = if (isSniperMode) NeonCyan else TextSecondary

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(sniperBg)
                    .border(1.dp, sniperBorder, RoundedCornerShape(16.dp))
                    .clickable { onToggleSniperMode() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("sniper_mode_toggle"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Mode Sniper",
                    tint = sniperColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSniperMode) "SNIPER 0.2x" else "1:1",
                    color = sniperColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Real-time Battery Telemetry
            val batteryColor = when {
                batteryInfo.percentage > 50 -> EmeraldGreen
                batteryInfo.percentage > 20 -> AmberAccent
                else -> RazorRed
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerDark)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("battery_gauge"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        batteryInfo.isCharging -> Icons.Default.BatteryChargingFull
                        batteryInfo.percentage <= 15 -> Icons.Default.BatteryAlert
                        else -> Icons.Default.BatteryFull
                    },
                    contentDescription = "Niveau de batterie",
                    tint = batteryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${batteryInfo.percentage}%",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (batteryInfo.isCharging) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "En charge",
                        tint = AmberAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
