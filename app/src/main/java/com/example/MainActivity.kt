package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cutpilot.model.MainNavigationTab
import com.example.cutpilot.ui.components.BottomNavBar
import com.example.cutpilot.ui.components.TopStudioBar
import com.example.cutpilot.ui.screens.CanvaStudioScreen
import com.example.cutpilot.ui.screens.EditorDeckScreen
import com.example.cutpilot.ui.screens.HostCompanionScreen
import com.example.cutpilot.ui.screens.JogWheelScreen
import com.example.cutpilot.ui.screens.SettingsScreen
import com.example.cutpilot.ui.screens.TrackpadScreen
import com.example.cutpilot.ui.theme.CutPilotTheme
import com.example.cutpilot.ui.theme.DarkBg
import com.example.cutpilot.viewmodel.CutPilotViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CutPilotTheme {
                val vm: CutPilotViewModel = viewModel()
                val settings by vm.settings.collectAsStateWithLifecycle()

                // Keep screen awake for video editing sessions
                LaunchedEffect(settings.keepScreenAwake) {
                    if (settings.keepScreenAwake) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                // Bluetooth permissions on Android 12+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Permissions result handled gracefully by HID manager */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val permissions = arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                        val needsPrompt = permissions.any {
                            ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (needsPrompt) {
                            permissionLauncher.launch(permissions)
                        }
                    }
                }

                CutPilotApp(vm)
            }
        }
    }
}

@Composable
fun CutPilotApp(vm: CutPilotViewModel) {
    val currentTab by vm.currentTab.collectAsStateWithLifecycle()
    val connectionMode by vm.connectionMode.collectAsStateWithLifecycle()
    val connectionStatus by vm.connectionStatus.collectAsStateWithLifecycle()
    val connectedDeviceName by vm.connectedDeviceName.collectAsStateWithLifecycle()
    val latencyInfo by vm.latencyInfo.collectAsStateWithLifecycle()
    val batteryInfo by vm.batteryInfo.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val activeProfile by vm.activeProfile.collectAsStateWithLifecycle()
    val simState by vm.simState.collectAsStateWithLifecycle()

    // BackHandler returns to Trackpad from any other tab
    BackHandler(enabled = currentTab != MainNavigationTab.TRACKPAD) {
        vm.selectTab(MainNavigationTab.TRACKPAD)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopStudioBar(
                connectionMode = connectionMode,
                connectionStatus = connectionStatus,
                connectedDeviceName = connectedDeviceName,
                latencyInfo = latencyInfo,
                batteryInfo = batteryInfo,
                isSniperMode = settings.sniperMode,
                onToggleSniperMode = { vm.toggleSniperMode() },
                onOpenConnection = { vm.selectTab(MainNavigationTab.CONNECTION) },
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            BottomNavBar(
                currentTab = currentTab,
                onTabSelected = { vm.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { targetTab ->
                when (targetTab) {
                    MainNavigationTab.TRACKPAD -> {
                        TrackpadScreen(
                            settings = settings,
                            isPlaying = simState.isPlaying,
                            activeProfileName = activeProfile.name,
                            onMouseMove = { dx, dy -> vm.onMouseMove(dx, dy) },
                            onMouseClick = { btn -> vm.onMouseClick(btn) },
                            onScroll = { dy, dx -> vm.onScroll(dy, dx) },
                            onTogglePlayPause = { vm.togglePlayPause() },
                            onQuickCut = {
                                activeProfile.shortcuts.find { it.label.contains("Cutter") || it.label.contains("Blade") || it.label.contains("Diviser") }?.let {
                                    vm.triggerShortcut(it)
                                } ?: vm.triggerShortcut(activeProfile.shortcuts.first())
                            },
                            onQuickUndo = {
                                activeProfile.shortcuts.find { it.label.contains("Annuler") }?.let {
                                    vm.triggerShortcut(it)
                                }
                            },
                            onZoomIn = {
                                activeProfile.shortcuts.find { it.label.contains("Zoom +") }?.let {
                                    vm.triggerShortcut(it)
                                }
                            },
                            onZoomOut = {
                                activeProfile.shortcuts.find { it.label.contains("Zoom -") }?.let {
                                    vm.triggerShortcut(it)
                                }
                            },
                            onUpdateSensitivity = { vm.updateSettings(settings.copy(sensitivity = it)) }
                        )
                    }

                    MainNavigationTab.JOG_WHEEL -> {
                        JogWheelScreen(
                            currentFrame = simState.currentFrame,
                            totalFrames = simState.totalFrames,
                            isPlaying = simState.isPlaying,
                            timecode = vm.formatTimecode(simState.currentFrame),
                            inPoint = simState.inPoint,
                            outPoint = simState.outPoint,
                            onJogTick = { steps, fine -> vm.onJogTick(steps, fine) },
                            onTogglePlayPause = { vm.togglePlayPause() },
                            onShuttleReverse = { vm.shuttleReverse() },
                            onShuttleForward = { vm.shuttleForward() },
                            onMarkIn = {
                                activeProfile.shortcuts.find { it.label.contains("IN") || it.label.contains("Entrée") }?.let {
                                    vm.triggerShortcut(it)
                                }
                            },
                            onMarkOut = {
                                activeProfile.shortcuts.find { it.label.contains("OUT") || it.label.contains("Sortie") }?.let {
                                    vm.triggerShortcut(it)
                                }
                            },
                            onQuickCut = {
                                activeProfile.shortcuts.find { it.label.contains("Cutter") || it.label.contains("Blade") || it.label.contains("Diviser") }?.let {
                                    vm.triggerShortcut(it)
                                } ?: vm.triggerShortcut(activeProfile.shortcuts.first())
                            }
                        )
                    }

                    MainNavigationTab.CANVA -> {
                        CanvaStudioScreen(
                            isPlaying = simState.isPlaying,
                            onTriggerKey = { mods, key -> vm.triggerKeyDirect(mods, key) },
                            onTogglePlayPause = { vm.togglePlayPause() }
                        )
                    }

                    MainNavigationTab.DECK -> {
                        EditorDeckScreen(
                            profiles = profiles,
                            activeProfile = activeProfile,
                            onSelectProfile = { vm.selectProfile(it) },
                            onTriggerShortcut = { vm.triggerShortcut(it) },
                            onSaveShortcut = { vm.addOrUpdateShortcut(it) },
                            onCreateProfile = { name, category -> vm.createCustomProfile(name, category) }
                        )
                    }

                    MainNavigationTab.CONNECTION -> {
                        HostCompanionScreen(
                            connectionMode = connectionMode,
                            connectionStatus = connectionStatus,
                            connectedDeviceName = connectedDeviceName,
                            latencyInfo = latencyInfo,
                            simState = simState,
                            localWifiIp = vm.getLocalWifiIp(),
                            pythonServerCode = vm.getPythonServerScript(),
                            onSelectConnectionMode = { vm.setConnectionMode(it) },
                            onConfigureWifiTarget = { vm.setTargetWifiIp(it) }
                        )
                    }

                    MainNavigationTab.SETTINGS -> {
                        SettingsScreen(
                            settings = settings,
                            batteryInfo = batteryInfo,
                            onUpdateSettings = { vm.updateSettings(it) }
                        )
                    }
                }
            }
        }
    }
}
