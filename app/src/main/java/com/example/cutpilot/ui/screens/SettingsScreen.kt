package com.example.cutpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.model.BatteryInfo
import com.example.cutpilot.model.ControllerSettings
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
fun SettingsScreen(
    settings: ControllerSettings,
    batteryInfo: BatteryInfo,
    onUpdateSettings: (ControllerSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Personnalisez la souris, les gestes et le confort d'utilisation.",
            color = TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )

        // Precision & Sensitivity Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mouse, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pointeur", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Sensitivity slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vitesse du curseur", color = TextSecondary, fontSize = 12.sp)
                        Text("${String.format("%.2f", settings.sensitivity)}x", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = settings.sensitivity,
                        onValueChange = { onUpdateSettings(settings.copy(sensitivity = it)) },
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SurfaceContainerHighDark
                        ),
                        modifier = Modifier.testTag("sensitivity_slider")
                    )
                }

                HorizontalDivider(color = SurfaceContainerHighDark)

                // Dynamic acceleration switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accélération dynamique", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Mouvements rapides = curseur plus loin, mouvements lents = plus précis.", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.acceleration,
                        onCheckedChange = { onUpdateSettings(settings.copy(acceleration = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }

                HorizontalDivider(color = SurfaceContainerHighDark)

                // Sniper ratio multiplier
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vitesse en mode précision", color = TextSecondary, fontSize = 12.sp)
                        Text("${(settings.sniperMultiplier * 100).toInt()}% de vitesse", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = settings.sniperMultiplier,
                        onValueChange = { onUpdateSettings(settings.copy(sniperMultiplier = it)) },
                        valueRange = 0.1f..0.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberAccent,
                            activeTrackColor = AmberAccent,
                            inactiveTrackColor = SurfaceContainerHighDark
                        )
                    )
                }
            }
        }

        // Haptic & Touch Interactions Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gestes et confort", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibrations", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Petite vibration à chaque clic et cran de molette.", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.hapticFeedback,
                        onCheckedChange = { onUpdateSettings(settings.copy(hapticFeedback = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AmberAccent, checkedTrackColor = AmberAccent.copy(alpha = 0.3f))
                    )
                }

                HorizontalDivider(color = SurfaceContainerHighDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Taper pour cliquer", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Toucher le trackpad fait un clic gauche.", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.tapToClick,
                        onCheckedChange = { onUpdateSettings(settings.copy(tapToClick = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }

                HorizontalDivider(color = SurfaceContainerHighDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Défilement naturel", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Le contenu suit les doigts, comme sur un portable.", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.naturalScroll,
                        onCheckedChange = { onUpdateSettings(settings.copy(naturalScroll = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }

                HorizontalDivider(color = SurfaceContainerHighDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ordinateur en AZERTY", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("À activer si le clavier du PC est en AZERTY (aussi réglable dans l'écran Clavier).", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.azertyHost,
                        onCheckedChange = { onUpdateSettings(settings.copy(azertyHost = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }

                HorizontalDivider(color = SurfaceContainerHighDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Garder l'écran allumé", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Le téléphone ne se met pas en veille pendant l'utilisation.", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.keepScreenAwake,
                        onCheckedChange = { onUpdateSettings(settings.copy(keepScreenAwake = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Live Battery Diagnostics Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryFull, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batterie du téléphone", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerDark)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Niveau", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${batteryInfo.percentage}%", color = EmeraldGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(if (batteryInfo.isCharging) "En charge" else "Sur batterie", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Health
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerDark)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("État", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(batteryInfo.health, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("", color = TextTertiary, fontSize = 11.sp)
                        }
                    }

                    // Temp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerDark)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Température", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", batteryInfo.temperatureCelsius)}°C", color = AmberAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("", color = TextTertiary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // About & Zero-Driver Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Vôlavo 1.1", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Bluetooth HID sans pilote · Wi-Fi faible latence", color = TextTertiary, fontSize = 11.sp)
        }
    }
}
