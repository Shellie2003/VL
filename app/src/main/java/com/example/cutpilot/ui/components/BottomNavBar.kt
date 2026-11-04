package com.example.cutpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cutpilot.model.MainNavigationTab
import com.example.cutpilot.ui.theme.DarkBg
import com.example.cutpilot.ui.theme.NeonCyan
import com.example.cutpilot.ui.theme.SurfaceContainerDark
import com.example.cutpilot.ui.theme.SurfaceDark
import com.example.cutpilot.ui.theme.TextSecondary
import com.example.cutpilot.ui.theme.TextTertiary

@Composable
fun BottomNavBar(
    currentTab: MainNavigationTab,
    onTabSelected: (MainNavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .background(DarkBg)
            .border(width = 0.5.dp, color = SurfaceContainerDark)
            .navigationBarsPadding(),
        containerColor = DarkBg,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentTab == MainNavigationTab.TRACKPAD,
            onClick = { onTabSelected(MainNavigationTab.TRACKPAD) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Mouse,
                    contentDescription = "Trackpad",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "Trackpad",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == MainNavigationTab.TRACKPAD) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            ),
            modifier = Modifier.testTag("tab_trackpad")
        )

        NavigationBarItem(
            selected = currentTab == MainNavigationTab.KEYBOARD,
            onClick = { onTabSelected(MainNavigationTab.KEYBOARD) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Clavier",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "Clavier",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == MainNavigationTab.KEYBOARD) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            ),
            modifier = Modifier.testTag("tab_keyboard")
        )

        NavigationBarItem(
            selected = currentTab == MainNavigationTab.JOG_WHEEL,
            onClick = { onTabSelected(MainNavigationTab.JOG_WHEEL) },
            icon = {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Jog Wheel",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "Jog Wheel",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == MainNavigationTab.JOG_WHEEL) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            ),
            modifier = Modifier.testTag("tab_jog_wheel")
        )

        NavigationBarItem(
            selected = currentTab == MainNavigationTab.CANVA,
            onClick = { onTabSelected(MainNavigationTab.CANVA) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Canva Studio",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "Canva",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == MainNavigationTab.CANVA) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF00C4CC),
                selectedTextColor = Color(0xFF00C4CC),
                indicatorColor = Color(0xFF00C4CC).copy(alpha = 0.15f),
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            ),
            modifier = Modifier.testTag("tab_canva")
        )

        NavigationBarItem(
            selected = currentTab == MainNavigationTab.DECK,
            onClick = { onTabSelected(MainNavigationTab.DECK) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Raccourcis",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "Raccourcis",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == MainNavigationTab.DECK) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            ),
            modifier = Modifier.testTag("tab_deck")
        )

        NavigationBarItem(
            selected = currentTab == MainNavigationTab.CONNECTION,
            onClick = { onTabSelected(MainNavigationTab.CONNECTION) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Connexion & Sim",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "Liaison",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == MainNavigationTab.CONNECTION) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            ),
            modifier = Modifier.testTag("tab_connection")
        )

        NavigationBarItem(
            selected = currentTab == MainNavigationTab.SETTINGS,
            onClick = { onTabSelected(MainNavigationTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Réglages",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "Réglages",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == MainNavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan.copy(alpha = 0.15f),
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            ),
            modifier = Modifier.testTag("tab_settings")
        )
    }
}
