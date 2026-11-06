package com.example.cutpilot.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cutpilot.battery.BatteryTelemetryMonitor
import com.example.cutpilot.bluetooth.BluetoothHidManager
import com.example.cutpilot.bluetooth.HidConstants
import com.example.cutpilot.data.DefaultPresets
import com.example.cutpilot.data.PairingStore
import com.example.cutpilot.data.LibraryShortcut
import com.example.cutpilot.input.HostLayout
import com.example.cutpilot.input.InputSink
import com.example.cutpilot.input.KeyboardLayouts
import com.example.cutpilot.midi.MidiController
import com.example.cutpilot.midi.MidiRoute
import com.example.cutpilot.midi.UsbMidiOutput
import com.example.cutpilot.input.PointerProcessor
import com.example.cutpilot.network.PairingInfo
import com.example.cutpilot.network.VolavoProtocol
import com.example.cutpilot.model.BatteryInfo
import com.example.cutpilot.model.ConnectionMode
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.model.ControllerSettings
import com.example.cutpilot.model.LatencyInfo
import com.example.cutpilot.model.MainNavigationTab
import com.example.cutpilot.model.ModifierKey
import com.example.cutpilot.model.ShortcutKey
import com.example.cutpilot.model.SoftwareProfile
import com.example.cutpilot.network.WifiControllerManager
import com.example.cutpilot.util.HapticHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SimulatedClip(
    val id: String,
    val name: String,
    val startFrame: Int,
    val endFrame: Int,
    val colorHex: Long
)

data class HostSimState(
    val cursorX: Float = 0.5f,
    val cursorY: Float = 0.5f,
    val currentFrame: Int = 360,
    val totalFrames: Int = 1800, // 1 min 15s at 24 fps
    val isPlaying: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val inPoint: Int? = 120,
    val outPoint: Int? = 640,
    val clips: List<SimulatedClip> = listOf(
        SimulatedClip("c1", "A-Roll Interview", 0, 480, 0xFF3B82F6),
        SimulatedClip("c2", "B-Roll Bourse 4K", 480, 960, 0xFF10B981),
        SimulatedClip("c3", "Drone Sunset.mp4", 960, 1440, 0xFFF59E0B),
        SimulatedClip("c4", "Outro Logo Animation", 1440, 1800, 0xFF8B5CF6)
    ),
    val lastActionLog: String = "Système prêt. Scannez le QR code de Vôlavo Hôte pour connecter le PC.",
    val lastEventTime: Long = System.currentTimeMillis()
)

class CutPilotViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothHid = BluetoothHidManager(application)
    private val wifiController = WifiControllerManager(application)
    private val batteryMonitor = BatteryTelemetryMonitor(application)
    private val haptic = HapticHelper(application)
    private val pairingStore = PairingStore(application)
    private val pointer = PointerProcessor()
    private val usbMidi = UsbMidiOutput(application)
    private val midi = MidiController(usbMidi, wifiController)
    private val deviceName: String = (Build.MODEL ?: "Android").take(40)

    // Navigation & Connection
    // First launch (nothing paired yet): open the connection screen directly.
    private val _currentTab = MutableStateFlow(
        if (pairingStore.lastPairing == null && pairingStore.lastBluetoothHost == null) MainNavigationTab.CONNECTION
        else MainNavigationTab.TRACKPAD
    )
    val currentTab: StateFlow<MainNavigationTab> = _currentTab.asStateFlow()

    private val _connectionMode = MutableStateFlow(pairingStore.connectionMode)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    /** Last computer paired by QR code (shown on the connection screen). */
    private val _pairedHost = MutableStateFlow(pairingStore.lastPairing)
    val pairedHost: StateFlow<PairingInfo?> = _pairedHost.asStateFlow()

    /** One-shot messages (toasts). */
    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    // Battery
    val batteryInfo: StateFlow<BatteryInfo> = batteryMonitor.batteryInfo

    // Latency & Telemetry (published at 1 Hz — never per touch event)
    private val _latencyInfo = MutableStateFlow(LatencyInfo())
    val latencyInfo: StateFlow<LatencyInfo> = _latencyInfo.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(
        ControllerSettings(azertyHost = pairingStore.azertyHost, keyboardLandscape = pairingStore.keyboardLandscape)
    )
    val settings: StateFlow<ControllerSettings> = _settings.asStateFlow()

    // Software Profiles
    private val _profiles = MutableStateFlow(DefaultPresets.allProfiles)
    val profiles: StateFlow<List<SoftwareProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(DefaultPresets.premierePro)
    val activeProfile: StateFlow<SoftwareProfile> = _activeProfile.asStateFlow()

    // Host Simulator Screen state
    private val _simState = MutableStateFlow(HostSimState())
    val simState: StateFlow<HostSimState> = _simState.asStateFlow()

    private val _dragLocked = MutableStateFlow(false)
    val dragLocked: StateFlow<Boolean> = _dragLocked.asStateFlow()

    private var playbackJob: Job? = null
    private var buttonMask = 0

    // MIDI keyboard: current output (USB link > Wi-Fi host > none), refreshed when either changes
    private val _midiRoute = MutableStateFlow(MidiRoute.NONE)
    val midiRoute: StateFlow<MidiRoute> = _midiRoute.asStateFlow()
    private val _midiOutputLabel = MutableStateFlow("Aucune sortie")
    val midiOutputLabel: StateFlow<String> = _midiOutputLabel.asStateFlow()

    init {
        batteryMonitor.startListening()
        bluetoothHid.initialize()

        viewModelScope.launch {
            combine(
                _connectionMode,
                bluetoothHid.connectionStatus,
                bluetoothHid.connectedDeviceName,
                bluetoothHid.errorMessage
            ) { mode, st, name, err -> if (mode == ConnectionMode.BLUETOOTH_HID) Triple(st, name, err) else null }
                .collect { t ->
                    t ?: return@collect
                    _connectionStatus.value = t.first
                    _connectedDeviceName.value = t.second ?: "Bluetooth HID"
                    _connectionError.value = t.third
                }
        }
        viewModelScope.launch {
            combine(
                _connectionMode,
                wifiController.status,
                wifiController.hostName,
                wifiController.errorMessage
            ) { mode, st, name, err -> if (mode == ConnectionMode.WIFI) Triple(st, name, err) else null }
                .collect { t ->
                    t ?: return@collect
                    _connectionStatus.value = t.first
                    _connectedDeviceName.value = t.second?.let { "Wi-Fi • $it" } ?: "Wi-Fi"
                    _connectionError.value = t.third
                }
        }
        viewModelScope.launch {
            combine(_connectionMode, wifiController.latencyInfo) { mode, lat -> mode to lat }
                .collect { (mode, lat) -> if (mode == ConnectionMode.WIFI) _latencyInfo.value = lat }
        }
        // Remember the computer that connected over Bluetooth for automatic reconnection.
        viewModelScope.launch {
            bluetoothHid.connectedDeviceAddress.collect { mac ->
                if (mac != null) pairingStore.lastBluetoothHost = mac
            }
        }
        // Bluetooth has no ping: publish the live report rate once per second.
        viewModelScope.launch {
            var last = bluetoothHid.packetsSent
            while (isActive) {
                delay(1000)
                val now = bluetoothHid.packetsSent
                if (_connectionMode.value == ConnectionMode.BLUETOOTH_HID) {
                    _latencyInfo.value = LatencyInfo(packetsSent = now, packetRateHz = (now - last).toInt(), measured = false)
                }
                last = now
            }
        }

        viewModelScope.launch {
            combine(usbMidi.deviceName, wifiController.status, wifiController.hostName) { usbName, wifiSt, host ->
                when {
                    usbName != null -> MidiRoute.USB to "USB · $usbName"
                    wifiSt == ConnectionStatus.CONNECTED -> MidiRoute.WIFI to "Wi-Fi · ${host ?: "PC"}"
                    else -> MidiRoute.NONE to "Aucune sortie MIDI"
                }
            }.collect { (r, label) ->
                _midiRoute.value = r
                _midiOutputLabel.value = label
            }
        }

        applyConnectionMode(_connectionMode.value, autoConnect = true)
    }

    // =========================================================================
    // MIDI keyboard (FL Studio)
    // =========================================================================
    fun midiNoteOn(note: Int, velocity: Int) = midi.noteOn(note, velocity)
    fun midiNoteOff(note: Int) = midi.noteOff(note)
    fun midiPitchBend(bend: Int) = midi.pitchBend(bend)
    fun midiModulation(value: Int) = midi.modulation(value)
    fun midiSustain(on: Boolean) = midi.sustain(on)
    fun midiPanic() = midi.panic()

    /** Looks for the USB MIDI link again; explains how to enable it when absent. */
    fun midiRescan() {
        usbMidi.scan()
        if (_midiRoute.value == MidiRoute.NONE) {
            _userMessages.tryEmit(
                if (usbMidi.isSupported) "USB : choisissez « MIDI » dans la notification USB d'Android, ou connectez le PC en Wi-Fi."
                else "Ce téléphone n'a pas le MIDI USB : connectez le PC en Wi-Fi (Vôlavo Hôte + loopMIDI)."
            )
        }
    }

    fun selectTab(tab: MainNavigationTab) {
        _currentTab.value = tab
    }

    // =========================================================================
    // Connection management
    // =========================================================================
    fun setConnectionMode(mode: ConnectionMode) {
        if (mode == _connectionMode.value) return
        releaseAllInputs()
        _connectionMode.value = mode
        pairingStore.connectionMode = mode
        applyConnectionMode(mode, autoConnect = true)
    }

    private fun applyConnectionMode(mode: ConnectionMode, autoConnect: Boolean) {
        if (mode != ConnectionMode.WIFI) wifiController.disconnect()
        when (mode) {
            ConnectionMode.BLUETOOTH_HID -> {
                _connectionStatus.value = bluetoothHid.connectionStatus.value
                _connectedDeviceName.value = bluetoothHid.connectedDeviceName.value ?: "Bluetooth HID"
                if (autoConnect && !bluetoothHid.isReady) {
                    pairingStore.lastBluetoothHost?.let { bluetoothHid.connectTo(it) }
                }
            }
            ConnectionMode.WIFI -> {
                if (autoConnect && !wifiController.isReady) {
                    pairingStore.lastPairing?.let { wifiController.connect(it, deviceName) }
                }
            }
            ConnectionMode.SIMULATOR -> {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _connectedDeviceName.value = "Simulateur Timeline Intégré"
                _connectionError.value = null
                _latencyInfo.value = LatencyInfo()
            }
        }
    }

    /**
     * Entry point for a scanned QR code or a `volavo://pair` link.
     * Returns false when the content is not a Vôlavo pairing code.
     */
    fun handlePairingPayload(raw: String?): Boolean {
        val info = raw?.let { PairingInfo.parse(it) }
        if (info == null) {
            _userMessages.tryEmit("Ce QR code n'est pas un code Vôlavo Hôte.")
            return false
        }
        pairWith(info)
        return true
    }

    /** Manual fallback: IP address + the code displayed by Vôlavo Hôte. */
    fun connectManual(ip: String, code: String, port: Int = VolavoProtocol.DEFAULT_PORT): Boolean {
        val key = PairingInfo.normalizeKey(code)
        val info = PairingInfo.parse(
            PairingInfo(listOf(ip.trim()), port, key, ip.trim()).toUri()
        )
        if (info == null) {
            _userMessages.tryEmit("Adresse IP ou code invalide.")
            return false
        }
        pairWith(info)
        return true
    }

    private fun pairWith(info: PairingInfo) {
        pairingStore.lastPairing = info
        _pairedHost.value = info
        triggerHapticClick()
        val mac = info.bluetoothMac
        if (_connectionMode.value == ConnectionMode.BLUETOOTH_HID && mac != null && bluetoothHid.isBluetoothEnabled()) {
            pairingStore.lastBluetoothHost = mac
            bluetoothHid.connectTo(mac)
            _userMessages.tryEmit("Connexion Bluetooth à ${info.hostName}… acceptez l'appairage sur l'ordinateur.")
        } else {
            if (_connectionMode.value != ConnectionMode.WIFI) {
                releaseAllInputs()
                _connectionMode.value = ConnectionMode.WIFI
                pairingStore.connectionMode = ConnectionMode.WIFI
                applyConnectionMode(ConnectionMode.WIFI, autoConnect = false)
            }
            wifiController.connect(info, deviceName)
            _userMessages.tryEmit("Connexion Wi-Fi à ${info.hostName}…")
        }
        logAction("Appairage QR : ${info.hostName}")
    }

    fun reconnect() = applyConnectionMode(_connectionMode.value, autoConnect = true)

    fun disconnect() {
        releaseAllInputs()
        when (_connectionMode.value) {
            ConnectionMode.WIFI -> wifiController.disconnect()
            ConnectionMode.BLUETOOTH_HID -> bluetoothHid.disconnect()
            ConnectionMode.SIMULATOR -> {}
        }
    }

    fun forgetPairing() {
        disconnect()
        pairingStore.lastPairing = null
        pairingStore.lastBluetoothHost = null
        _pairedHost.value = null
    }

    /** Called once the Bluetooth runtime permissions have been answered. */
    fun onBluetoothPermissionsResult() {
        bluetoothHid.initialize()
        if (_connectionMode.value == ConnectionMode.BLUETOOTH_HID) {
            pairingStore.lastBluetoothHost?.let { bluetoothHid.connectTo(it) }
        }
    }

    fun setTargetWifiIp(ip: String, code: String) {
        connectManual(ip, code)
    }

    fun selectProfile(profile: SoftwareProfile) {
        _activeProfile.value = profile
        triggerHapticTick()
        logAction("Profil activé : ${profile.name}")
    }

    fun updateSettings(newSettings: ControllerSettings) {
        if (newSettings.azertyHost != _settings.value.azertyHost) pairingStore.azertyHost = newSettings.azertyHost
        if (newSettings.keyboardLandscape != _settings.value.keyboardLandscape) {
            pairingStore.keyboardLandscape = newSettings.keyboardLandscape
        }
        _settings.value = newSettings
    }

    // =========================================================================
    // Full keyboard (Clavier tab)
    // =========================================================================
    val hostLayout: HostLayout
        get() = if (_settings.value.azertyHost) HostLayout.AZERTY else HostLayout.QWERTY

    fun setKeyboardLandscape(landscape: Boolean) {
        updateSettings(_settings.value.copy(keyboardLandscape = landscape))
        triggerHapticTick()
    }

    fun setHostLayout(layout: HostLayout) {
        updateSettings(_settings.value.copy(azertyHost = layout == HostLayout.AZERTY))
        triggerHapticTick()
    }

    /** A key of the on-screen keyboard: physical position, labels already follow the PC layout. */
    fun sendPhysicalKey(modifiers: Int, usage: Int) {
        if (usage == 0 && modifiers == 0) return
        triggerHapticTick()
        sink()?.keyPhysical(modifiers, usage, VolavoProtocol.KEY_TAP)
    }

    /** Types [text] on the computer (any character over Wi-Fi; layout-mapped over Bluetooth). */
    fun typeText(text: String, pressEnter: Boolean) {
        if (text.isEmpty() && !pressEnter) return
        triggerHapticClick()
        when (_connectionMode.value) {
            ConnectionMode.WIFI -> {
                wifiController.text(if (pressEnter) text + "\n" else text)
            }
            ConnectionMode.BLUETOOTH_HID -> {
                val layout = hostLayout
                var skipped = 0
                val full = if (pressEnter) text + "\n" else text
                for (ch in full) {
                    val strokes = KeyboardLayouts.strokesFor(ch, layout)
                    if (strokes == null) {
                        skipped++
                        continue
                    }
                    for (k in strokes) bluetoothHid.key(k.modifiers, k.usage, VolavoProtocol.KEY_TAP)
                }
                if (skipped > 0) {
                    _userMessages.tryEmit("$skipped caractère(s) impossible(s) en Bluetooth ($layout) — utilisez le Wi-Fi pour tout envoyer.")
                }
            }
            ConnectionMode.SIMULATOR -> {}
        }
        logAction("Texte envoyé (${text.length} caractères)")
    }

    fun sendMedia(usage: Int) {
        triggerHapticClick()
        sink()?.media(usage)
    }

    fun triggerLibraryShortcut(shortcut: LibraryShortcut) {
        triggerHapticClick()
        sendKey(shortcut.modifiers, shortcut.key)
        logAction("Raccourci : ${shortcut.label} (${shortcut.combo})")
    }

    fun toggleSniperMode() {
        val current = _settings.value.sniperMode
        _settings.value = _settings.value.copy(sniperMode = !current)
        triggerHapticClick()
        logAction(if (!current) "Mode Sniper Chirurgical activé (${(_settings.value.sniperMultiplier * 100).toInt()} %)" else "Mode Sniper désactivé (1.0x)")
    }

    // =========================================================================
    // Pointer — hot path (called for every touch event, keep allocation-free)
    // =========================================================================
    private fun sink(): InputSink? = when (_connectionMode.value) {
        ConnectionMode.BLUETOOTH_HID -> bluetoothHid
        ConnectionMode.WIFI -> wifiController
        ConnectionMode.SIMULATOR -> null
    }

    fun onMouseMove(rawDx: Float, rawDy: Float, dtMs: Long = 8L) {
        val s = _settings.value
        val packed = pointer.process(
            rawDx, rawDy, dtMs,
            sensitivity = s.sensitivity,
            acceleration = s.acceleration,
            precisionMultiplier = if (s.sniperMode) s.sniperMultiplier else 1f
        )
        val dx = PointerProcessor.unpackFirst(packed)
        val dy = PointerProcessor.unpackSecond(packed)
        if (dx == 0 && dy == 0) return

        val out = sink()
        if (out != null) {
            out.move(dx, dy)
        } else {
            // Simulator only: the virtual host cursor is UI state.
            val cur = _simState.value
            _simState.value = cur.copy(
                cursorX = (cur.cursorX + dx / 1000f).coerceIn(0.02f, 0.98f),
                cursorY = (cur.cursorY + dy / 800f).coerceIn(0.05f, 0.95f)
            )
        }
    }

    /** Physical-style button: [down] = pressed, false = released. */
    fun onMouseButton(button: Int, down: Boolean) {
        val newMask = if (down) buttonMask or button else buttonMask and button.inv()
        if (newMask == buttonMask) return
        buttonMask = newMask
        if (button == HidConstants.MOUSE_BUTTON_LEFT && !down) _dragLocked.value = false
        if (down) triggerHapticClick()
        sink()?.setButtons(buttonMask)
    }

    fun onMouseClick(button: Int) { // 1 = Left, 2 = Right, 4 = Middle
        triggerHapticClick()
        val out = sink()
        if (out != null) {
            out.setButtons(buttonMask or button)
            out.setButtons(buttonMask)
        }
        if (_connectionMode.value == ConnectionMode.SIMULATOR) {
            val btnName = when (button) {
                HidConstants.MOUSE_BUTTON_LEFT -> "Clic Gauche"
                HidConstants.MOUSE_BUTTON_RIGHT -> "Clic Droit"
                HidConstants.MOUSE_BUTTON_MIDDLE -> "Clic Milieu"
                else -> "Clic"
            }
            logAction("Événement Souris : $btnName")
        }
    }

    fun onMouseDoubleClick() {
        onMouseClick(HidConstants.MOUSE_BUTTON_LEFT)
        onMouseClick(HidConstants.MOUSE_BUTTON_LEFT)
    }

    /** Drag lock keeps the left button held so a long drag can span several swipes. */
    fun setDragLock(locked: Boolean) {
        onMouseButton(HidConstants.MOUSE_BUTTON_LEFT, locked)
        _dragLocked.value = locked
        logAction(if (locked) "Verrou de glissement activé" else "Verrou de glissement relâché")
    }

    /** Two-finger / strip scroll, raw finger pixels. */
    fun onScroll(deltaY: Float, deltaX: Float = 0f) {
        val s = _settings.value
        val packed = pointer.processScroll(deltaY, deltaX, s.scrollSpeed, s.naturalScroll)
        val v = PointerProcessor.unpackFirst(packed)
        val h = PointerProcessor.unpackSecond(packed)
        if (v == 0 && h == 0) return
        val out = sink()
        if (out != null) {
            out.scroll(v, h)
        } else {
            val cur = _simState.value
            val newFrame = (cur.currentFrame - v / 10).coerceIn(0, cur.totalFrames)
            _simState.value = cur.copy(currentFrame = newFrame)
        }
    }

    private fun releaseAllInputs() {
        if (buttonMask != 0) sink()?.setButtons(0)
        buttonMask = 0
        _dragLocked.value = false
        pointer.reset()
    }

    /** Sends a key through the active transport (layout-aware for Bluetooth HID). */
    private fun sendKey(modifiers: Set<ModifierKey>, keySymbol: String) {
        val usage = if (keySymbol.isEmpty()) 0 else HidConstants.symbolToHidCode(keySymbol).toInt() and 0xFF
        val mods = HidConstants.modifierBits(modifiers)
        if (usage == 0 && (mods == 0 || keySymbol.isNotEmpty())) return // unknown key: send nothing
        sendKeyUsage(mods, usage)
    }

    private fun sendKeyUsage(mods: Int, usage: Int) {
        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> {
                val u = if (_settings.value.azertyHost) HidConstants.remapForAzerty(usage) else usage
                bluetoothHid.key(mods, u, VolavoProtocol.KEY_TAP)
            }
            ConnectionMode.WIFI -> wifiController.key(mods, usage, VolavoProtocol.KEY_TAP)
            ConnectionMode.SIMULATOR -> {}
        }
    }

    // Jog Wheel / Timeline Scrubbing
    fun onJogTick(steps: Int, isFineSubpixel: Boolean = false) {
        val frameStep = if (isFineSubpixel || _settings.value.sniperMode) 1 else 3
        val deltaFrames = steps * frameStep

        triggerHapticTick()

        // One arrow key tap per step (frame-accurate in every NLE), capped for huge flicks.
        val arrow = (if (steps > 0) HidConstants.KEY_RIGHT_ARROW else HidConstants.KEY_LEFT_ARROW).toInt()
        repeat(kotlin.math.abs(deltaFrames).coerceAtMost(48)) { sendKeyUsage(0, arrow) }

        // Update simulated timeline
        val cur = _simState.value
        val newFrame = (cur.currentFrame + deltaFrames).coerceIn(0, cur.totalFrames)
        _simState.value = cur.copy(
            currentFrame = newFrame,
            isPlaying = false
        )
        playbackJob?.cancel()

        logAction("Jog Wheel : ${if (steps > 0) "+$deltaFrames" else "$deltaFrames"} frames (Frame $newFrame)")
    }

    // Shuttle J-K-L playback
    fun togglePlayPause() {
        triggerHapticClick()
        val currentlyPlaying = _simState.value.isPlaying

        sendKey(emptySet(), "ESPACE")

        if (currentlyPlaying) {
            playbackJob?.cancel()
            _simState.value = _simState.value.copy(isPlaying = false)
            logAction("Lecture arrêtée")
        } else {
            _simState.value = _simState.value.copy(isPlaying = true)
            logAction("Lecture 1x en cours (Espace)")
            startSimulatedPlayback()
        }
    }

    fun shuttleReverse() {
        triggerHapticClick()
        sendKey(emptySet(), "J")
        logAction("Navette arrière (J)")
        val cur = _simState.value
        _simState.value = cur.copy(currentFrame = (cur.currentFrame - 24).coerceAtLeast(0))
    }

    fun shuttleForward() {
        triggerHapticClick()
        sendKey(emptySet(), "L")
        logAction("Navette avant (L)")
        val cur = _simState.value
        _simState.value = cur.copy(currentFrame = (cur.currentFrame + 24).coerceAtMost(cur.totalFrames))
    }

    private fun startSimulatedPlayback() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive && _simState.value.isPlaying) {
                delay(41) // ~24 fps
                val cur = _simState.value
                val nextFrame = cur.currentFrame + 1
                if (nextFrame >= cur.totalFrames) {
                    _simState.value = cur.copy(currentFrame = 0, isPlaying = false)
                    break
                } else {
                    _simState.value = cur.copy(currentFrame = nextFrame)
                }
            }
        }
    }

    // Trigger customizable shortcut
    fun triggerShortcut(shortcut: ShortcutKey) {
        triggerHapticClick()

        sendKey(shortcut.modifiers, shortcut.keySymbol)

        // Handle specific editing operations in host simulation
        handleSimulatedEditOperation(shortcut)
    }

    fun triggerKeyDirect(modifiers: Set<ModifierKey>, keySymbol: String) {
        val shortcut = ShortcutKey(
            id = "direct_${System.currentTimeMillis()}",
            label = keySymbol,
            category = "Canva",
            keySymbol = keySymbol,
            modifiers = modifiers
        )
        triggerShortcut(shortcut)
    }

    private fun handleSimulatedEditOperation(shortcut: ShortcutKey) {
        val cur = _simState.value
        val label = shortcut.label.lowercase()

        when {
            label.contains("cutter") || label.contains("blade") || label.contains("scinder") || label.contains("cut") -> {
                // Split clip at current playhead!
                val frame = cur.currentFrame
                val targetClip = cur.clips.find { frame > it.startFrame && frame < it.endFrame }
                if (targetClip != null) {
                    val clip1 = targetClip.copy(id = "${targetClip.id}_a", endFrame = frame)
                    val clip2 = targetClip.copy(id = "${targetClip.id}_b", name = "${targetClip.name} (Coupe)", startFrame = frame)
                    val newClips = cur.clips.filter { it.id != targetClip.id } + listOf(clip1, clip2)
                    _simState.value = cur.copy(clips = newClips.sortedBy { it.startFrame })
                    logAction("✂️ Coupe chirurgicale effectuée à ${formatTimecode(frame)}")
                } else {
                    logAction("Outil Coupe : Aucun clip à scinder à la position actuelle")
                }
            }
            label.contains("in") || label.contains("entrée") -> {
                _simState.value = cur.copy(inPoint = cur.currentFrame)
                logAction("Point IN défini à ${formatTimecode(cur.currentFrame)}")
            }
            label.contains("out") || label.contains("sortie") -> {
                _simState.value = cur.copy(outPoint = cur.currentFrame)
                logAction("Point OUT défini à ${formatTimecode(cur.currentFrame)}")
            }
            label.contains("zoom +") || label.contains("zoom in") -> {
                val newZoom = (cur.zoomLevel * 1.3f).coerceAtMost(3.0f)
                _simState.value = cur.copy(zoomLevel = newZoom)
                logAction("Zoom Timeline : ${(newZoom * 100).toInt()}%")
            }
            label.contains("zoom -") || label.contains("zoom out") -> {
                val newZoom = (cur.zoomLevel / 1.3f).coerceAtLeast(0.5f)
                _simState.value = cur.copy(zoomLevel = newZoom)
                logAction("Zoom Timeline : ${(newZoom * 100).toInt()}%")
            }
            label.contains("suppr") || label.contains("ripple") || label.contains("del") -> {
                // Remove clip containing cursor or active
                val frame = cur.currentFrame
                val targetClip = cur.clips.find { frame >= it.startFrame && frame <= it.endFrame }
                if (targetClip != null && cur.clips.size > 1) {
                    val duration = targetClip.endFrame - targetClip.startFrame
                    val remaining = cur.clips.filter { it.id != targetClip.id }.map { c ->
                        if (c.startFrame > targetClip.startFrame) {
                            c.copy(startFrame = c.startFrame - duration, endFrame = c.endFrame - duration)
                        } else c
                    }
                    _simState.value = cur.copy(clips = remaining, totalFrames = cur.totalFrames - duration)
                    logAction("🗑️ Suppression raccord (Ripple Delete) réussie")
                } else {
                    logAction("Raccourci exécuté : ${shortcut.label}")
                }
            }
            else -> {
                logAction("Raccourci : ${shortcut.label} (${shortcut.modifiers.joinToString("+") { it.name }} ${shortcut.keySymbol})")
            }
        }
    }

    // Add or customize shortcut in active profile
    fun addOrUpdateShortcut(newShortcut: ShortcutKey) {
        val curProfile = _activeProfile.value
        val updatedShortcuts = curProfile.shortcuts.filter { it.id != newShortcut.id } + newShortcut
        val updatedProfile = curProfile.copy(shortcuts = updatedShortcuts)
        _activeProfile.value = updatedProfile

        // Update in profiles list
        _profiles.value = _profiles.value.map {
            if (it.id == updatedProfile.id) updatedProfile else it
        }
        triggerHapticClick()
        logAction("Raccourci '${newShortcut.label}' enregistré avec succès")
    }

    fun createCustomProfile(name: String, category: String, baseProfile: SoftwareProfile = _activeProfile.value) {
        val newId = "custom_${System.currentTimeMillis()}"
        val newProfile = SoftwareProfile(
            id = newId,
            name = name,
            category = category,
            description = "Configuration personnalisée par l'utilisateur",
            shortcuts = baseProfile.shortcuts
        )
        _profiles.value = _profiles.value + newProfile
        _activeProfile.value = newProfile
        triggerHapticClick()
        logAction("Nouveau profil créé : $name")
    }

    private fun triggerHapticTick() {
        if (_settings.value.hapticFeedback) {
            haptic.tick()
        }
    }

    private fun triggerHapticClick() {
        if (_settings.value.hapticFeedback) {
            haptic.click()
        }
    }

    private fun logAction(action: String) {
        _simState.value = _simState.value.copy(
            lastActionLog = action,
            lastEventTime = System.currentTimeMillis()
        )
    }

    fun formatTimecode(frames: Int, fps: Int = 24): String {
        val totalSeconds = frames / fps
        val f = frames % fps
        val s = totalSeconds % 60
        val m = (totalSeconds / 60) % 60
        val h = totalSeconds / 3600
        return String.format("%02d:%02d:%02d:%02d", h, m, s, f)
    }

    fun getLocalWifiIp(): String = wifiController.getLocalIpAddress()

    override fun onCleared() {
        super.onCleared()
        releaseAllInputs()
        midi.panic()
        usbMidi.release()
        batteryMonitor.stopListening()
        bluetoothHid.release()
        wifiController.close()
        playbackJob?.cancel()
    }
}
