package com.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.example.cutpilot.model.MainNavigationTab
import com.example.cutpilot.model.ConnectionMode
import com.example.cutpilot.ui.components.ConnectionBanner
import com.example.cutpilot.ui.components.VolavoTopBar
import com.example.cutpilot.ui.screens.CanvaStudioScreen
import com.example.cutpilot.ui.screens.EditorDeckScreen
import com.example.cutpilot.ui.screens.HostCompanionScreen
import com.example.cutpilot.ui.screens.JogWheelScreen
import com.example.cutpilot.ui.screens.KeyboardScreen
import com.example.cutpilot.ui.screens.MidiScreen
import com.example.cutpilot.input.HostLayout
import com.example.cutpilot.ui.screens.SettingsScreen
import com.example.cutpilot.ui.screens.TrackpadScreen
import com.example.cutpilot.ui.theme.CutPilotTheme
import com.example.cutpilot.ui.theme.DarkBg
import com.example.cutpilot.viewmodel.CutPilotViewModel

class MainActivity : ComponentActivity() {

    private val vm: CutPilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handlePairingIntent(intent)

        setContent {
            CutPilotTheme {
                val settings by vm.settings.collectAsStateWithLifecycle()
                val context = LocalContext.current

                // Keep screen awake for video editing sessions
                LaunchedEffect(settings.keepScreenAwake) {
                    if (settings.keepScreenAwake) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                // Keyboard "piano" mode: landscape + full screen, only on the keyboard screen
                val tab by vm.currentTab.collectAsStateWithLifecycle()
                val pianoMode = (tab == MainNavigationTab.KEYBOARD && settings.keyboardLandscape) ||
                    tab == MainNavigationTab.MIDI
                LaunchedEffect(pianoMode) {
                    requestedOrientation = if (pianoMode) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                    val insets = WindowCompat.getInsetsController(window, window.decorView)
                    if (pianoMode) {
                        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        insets.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        insets.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                // One-shot messages from the ViewModel
                LaunchedEffect(Unit) {
                    vm.userMessages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                }

                // Bluetooth permissions on Android 12+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { vm.onBluetoothPermissionsResult() }

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

                // QR code scanner (handles the camera permission itself)
                val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
                    result.contents?.let { vm.handlePairingPayload(it) }
                }
                val scanQr: () -> Unit = {
                    scanLauncher.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("Visez le QR code affiché par Vôlavo Hôte")
                            .setBeepEnabled(false)
                            .setOrientationLocked(false)
                    )
                }

                // Make the phone discoverable so the computer can find "Vôlavo Mouse & Deck"
                val discoverableLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { }
                val makeDiscoverable: () -> Unit = {
                    try {
                        discoverableLauncher.launch(
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Bluetooth indisponible", Toast.LENGTH_SHORT).show()
                    }
                }

                CutPilotApp(vm, onScanQr = scanQr, onMakeDiscoverable = makeDiscoverable)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePairingIntent(intent)
    }

    /** `volavo://pair?...` links: scanning the QR code with the system camera opens the app. */
    private fun handlePairingIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme.equals("volavo", ignoreCase = true)) {
            vm.handlePairingPayload(data.toString())
            vm.selectTab(MainNavigationTab.CONNECTION)
        }
    }
}

@Composable
fun CutPilotApp(
    vm: CutPilotViewModel,
    onScanQr: () -> Unit = {},
    onMakeDiscoverable: () -> Unit = {}
) {
    val currentTab by vm.currentTab.collectAsStateWithLifecycle()
    val connectionMode by vm.connectionMode.collectAsStateWithLifecycle()
    val connectionStatus by vm.connectionStatus.collectAsStateWithLifecycle()
    val connectedDeviceName by vm.connectedDeviceName.collectAsStateWithLifecycle()
    val latencyInfo by vm.latencyInfo.collectAsStateWithLifecycle()
    val batteryInfo by vm.batteryInfo.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val activeProfile by vm.activeProfile.collectAsStateWithLifecycle()
    // simState is collected inside the screens that use it, so frequent timeline
    // updates only recompose the visible screen, not the whole app shell.

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
            if (!(currentTab == MainNavigationTab.KEYBOARD && settings.keyboardLandscape) &&
                currentTab != MainNavigationTab.MIDI
            ) VolavoTopBar(
                currentTab = currentTab,
                connectionMode = connectionMode,
                connectionStatus = connectionStatus,
                connectedDeviceName = connectedDeviceName,
                latencyInfo = latencyInfo,
                isPrecisionMode = settings.sniperMode,
                onTogglePrecision = { vm.toggleSniperMode() },
                onNavigate = { vm.selectTab(it) },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            val connectionError by vm.connectionError.collectAsStateWithLifecycle()
            val showBanner = connectionMode != ConnectionMode.SIMULATOR &&
                currentTab != MainNavigationTab.CONNECTION && currentTab != MainNavigationTab.SETTINGS &&
                !(currentTab == MainNavigationTab.KEYBOARD && settings.keyboardLandscape) &&
                currentTab != MainNavigationTab.MIDI
            Column(Modifier.fillMaxSize()) {
            if (showBanner) {
                ConnectionBanner(
                    connectionStatus = connectionStatus,
                    errorMessage = connectionError,
                    onConnect = { vm.selectTab(MainNavigationTab.CONNECTION) }
                )
            }
            AnimatedContent(
                targetState = currentTab,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { targetTab ->
                when (targetTab) {
                    MainNavigationTab.TRACKPAD -> {
                        val simState by vm.simState.collectAsStateWithLifecycle()
                        val dragLocked by vm.dragLocked.collectAsStateWithLifecycle()
                        TrackpadScreen(
                            settings = settings,
                            isPlaying = simState.isPlaying,
                            activeProfileName = activeProfile.name,
                            isDragLocked = dragLocked,
                            onMouseMove = vm::onMouseMove,
                            onMouseClick = vm::onMouseClick,
                            onMouseButton = vm::onMouseButton,
                            onToggleDragLock = { vm.setDragLock(!dragLocked) },
                            onScroll = vm::onScroll,
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
                            onUpdateSensitivity = { vm.updateSettings(settings.copy(sensitivity = it)) },
                            onOpenKeyboard = { vm.selectTab(MainNavigationTab.KEYBOARD) }
                        )
                    }

                    MainNavigationTab.KEYBOARD -> {
                        KeyboardScreen(
                            layout = if (settings.azertyHost) HostLayout.AZERTY else HostLayout.QWERTY,
                            onLayoutChange = vm::setHostLayout,
                            onPhysicalKey = vm::sendPhysicalKey,
                            onTypeText = vm::typeText,
                            onMedia = vm::sendMedia,
                            onShortcut = vm::triggerLibraryShortcut,
                            landscape = settings.keyboardLandscape,
                            onToggleLandscape = { vm.setKeyboardLandscape(!settings.keyboardLandscape) },
                            onOpenTrackpad = { vm.selectTab(MainNavigationTab.TRACKPAD) }
                        )
                    }

                    MainNavigationTab.MIDI -> {
                        val route by vm.midiRoute.collectAsStateWithLifecycle()
                        val label by vm.midiOutputLabel.collectAsStateWithLifecycle()
                        MidiScreen(
                            route = route,
                            outputLabel = label,
                            onNoteOn = vm::midiNoteOn,
                            onNoteOff = vm::midiNoteOff,
                            onPitchBend = vm::midiPitchBend,
                            onModulation = vm::midiModulation,
                            onSustain = vm::midiSustain,
                            onPanic = vm::midiPanic,
                            onRescanUsb = vm::midiRescan,
                            onOpenTrackpad = { vm.selectTab(MainNavigationTab.TRACKPAD) }
                        )
                    }

                    MainNavigationTab.JOG_WHEEL -> {
                        val simState by vm.simState.collectAsStateWithLifecycle()
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
                        val simState by vm.simState.collectAsStateWithLifecycle()
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
                        val simState by vm.simState.collectAsStateWithLifecycle()
                        val pairedHost by vm.pairedHost.collectAsStateWithLifecycle()
                        val localIp = remember { vm.getLocalWifiIp() }
                        HostCompanionScreen(
                            connectionMode = connectionMode,
                            connectionStatus = connectionStatus,
                            connectedDeviceName = connectedDeviceName,
                            latencyInfo = latencyInfo,
                            simState = simState,
                            localWifiIp = localIp,
                            pairedHost = pairedHost,
                            connectionError = connectionError,
                            onSelectConnectionMode = { vm.setConnectionMode(it) },
                            onScanQr = onScanQr,
                            onManualConnect = { ip, code -> vm.connectManual(ip, code) },
                            onReconnect = { vm.reconnect() },
                            onDisconnect = { vm.disconnect() },
                            onForgetPairing = { vm.forgetPairing() },
                            onMakeDiscoverable = onMakeDiscoverable
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
}
