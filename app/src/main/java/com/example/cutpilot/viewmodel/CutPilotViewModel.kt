package com.example.cutpilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cutpilot.battery.BatteryTelemetryMonitor
import com.example.cutpilot.bluetooth.BluetoothHidManager
import com.example.cutpilot.bluetooth.HidConstants
import com.example.cutpilot.data.DefaultPresets
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    val lastActionLog: String = "Système prêt. Connecté ou prêt pour simulation.",
    val lastEventTime: Long = System.currentTimeMillis()
)

class CutPilotViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothHid = BluetoothHidManager(application)
    private val wifiController = WifiControllerManager(application)
    private val batteryMonitor = BatteryTelemetryMonitor(application)
    private val haptic = HapticHelper(application)

    // Navigation & Connection
    private val _currentTab = MutableStateFlow(MainNavigationTab.TRACKPAD)
    val currentTab: StateFlow<MainNavigationTab> = _currentTab.asStateFlow()

    private val _connectionMode = MutableStateFlow(ConnectionMode.BLUETOOTH_HID)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>("MacBook Pro M3 Max")
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    // Battery
    val batteryInfo: StateFlow<BatteryInfo> = batteryMonitor.batteryInfo

    // Latency & Telemetry
    private val _latencyInfo = MutableStateFlow(LatencyInfo(pingMs = 3, packetsSent = 1420, packetRateHz = 125))
    val latencyInfo: StateFlow<LatencyInfo> = _latencyInfo.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(ControllerSettings())
    val settings: StateFlow<ControllerSettings> = _settings.asStateFlow()

    // Software Profiles
    private val _profiles = MutableStateFlow(DefaultPresets.allProfiles)
    val profiles: StateFlow<List<SoftwareProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(DefaultPresets.premierePro)
    val activeProfile: StateFlow<SoftwareProfile> = _activeProfile.asStateFlow()

    // Host Simulator Screen state
    private val _simState = MutableStateFlow(HostSimState())
    val simState: StateFlow<HostSimState> = _simState.asStateFlow()

    private var playbackJob: Job? = null

    init {
        batteryMonitor.startListening()
        bluetoothHid.initialize()

        // Sync connection state
        viewModelScope.launch {
            bluetoothHid.connectionStatus.collect { status ->
                if (_connectionMode.value == ConnectionMode.BLUETOOTH_HID) {
                    _connectionStatus.value = status
                }
            }
        }
        viewModelScope.launch {
            bluetoothHid.connectedDeviceName.collect { name ->
                if (_connectionMode.value == ConnectionMode.BLUETOOTH_HID) {
                    _connectedDeviceName.value = name
                }
            }
        }
        viewModelScope.launch {
            wifiController.status.collect { status ->
                if (_connectionMode.value == ConnectionMode.WIFI) {
                    _connectionStatus.value = status
                }
            }
        }
        viewModelScope.launch {
            wifiController.latencyInfo.collect { lat ->
                if (_connectionMode.value == ConnectionMode.WIFI) {
                    _latencyInfo.value = lat
                }
            }
        }
    }

    fun selectTab(tab: MainNavigationTab) {
        _currentTab.value = tab
    }

    fun setConnectionMode(mode: ConnectionMode) {
        _connectionMode.value = mode
        when (mode) {
            ConnectionMode.BLUETOOTH_HID -> {
                _connectionStatus.value = bluetoothHid.connectionStatus.value
                _connectedDeviceName.value = bluetoothHid.connectedDeviceName.value ?: "Appairage Bluetooth HID"
            }
            ConnectionMode.WIFI -> {
                _connectionStatus.value = wifiController.status.value
                _connectedDeviceName.value = "Wi-Fi (${wifiController.targetIp.value})"
            }
            ConnectionMode.SIMULATOR -> {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _connectedDeviceName.value = "Simulateur Timeline Intégré"
            }
        }
    }

    fun setTargetWifiIp(ip: String, port: Int = 8888) {
        wifiController.configureTarget(ip, port)
        _connectedDeviceName.value = "Hôte Wi-Fi ($ip)"
    }

    fun selectProfile(profile: SoftwareProfile) {
        _activeProfile.value = profile
        triggerHapticTick()
        logAction("Profil activé : ${profile.name}")
    }

    fun updateSettings(newSettings: ControllerSettings) {
        _settings.value = newSettings
    }

    fun toggleSniperMode() {
        val current = _settings.value.sniperMode
        _settings.value = _settings.value.copy(sniperMode = !current)
        triggerHapticClick()
        logAction(if (!current) "Mode Sniper Chirurgical activé (0.22x)" else "Mode Sniper désactivé (1.0x)")
    }

    // Touchpad & Mouse movements
    fun onMouseMove(rawDx: Float, rawDy: Float) {
        val sensitivity = _settings.value.sensitivity
        val sniperMultiplier = if (_settings.value.sniperMode) _settings.value.sniperMultiplier else 1.0f

        val accelFactor = if (_settings.value.acceleration) {
            val speed = Math.hypot(rawDx.toDouble(), rawDy.toDouble()).toFloat()
            if (speed > 20f) 1.5f else 1.0f
        } else 1.0f

        val finalDx = (rawDx * sensitivity * sniperMultiplier * accelFactor).toInt()
        val finalDy = (rawDy * sensitivity * sniperMultiplier * accelFactor).toInt()

        // Send via active transport
        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> {
                bluetoothHid.sendMouseMove(finalDx, finalDy)
            }
            ConnectionMode.WIFI -> {
                wifiController.sendMouseMove(finalDx, finalDy)
            }
            ConnectionMode.SIMULATOR -> {}
        }

        // Update virtual host cursor
        val cur = _simState.value
        val newX = (cur.cursorX + finalDx / 1000f).coerceIn(0.02f, 0.98f)
        val newY = (cur.cursorY + finalDy / 800f).coerceIn(0.05f, 0.95f)
        _simState.value = cur.copy(cursorX = newX, cursorY = newY)

        // Increment packet count
        _latencyInfo.value = _latencyInfo.value.copy(
            packetsSent = _latencyInfo.value.packetsSent + 1
        )
    }

    fun onMouseClick(button: Int) { // 1 = Left, 2 = Right, 4 = Middle
        triggerHapticClick()
        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> {
                bluetoothHid.sendMouseMove(0, 0, button)
                viewModelScope.launch {
                    delay(20)
                    bluetoothHid.sendMouseMove(0, 0, 0)
                }
            }
            ConnectionMode.WIFI -> {
                wifiController.sendMouseMove(0, 0, button)
            }
            ConnectionMode.SIMULATOR -> {}
        }

        val btnName = when (button) {
            HidConstants.MOUSE_BUTTON_LEFT -> "Clic Gauche"
            HidConstants.MOUSE_BUTTON_RIGHT -> "Clic Droit"
            HidConstants.MOUSE_BUTTON_MIDDLE -> "Clic Milieu"
            else -> "Clic"
        }
        logAction("Événement Souris : $btnName")
    }

    fun onScroll(deltaY: Float, deltaX: Float = 0f) {
        val direction = if (_settings.value.naturalScroll) -1 else 1
        val wheel = (deltaY * direction).toInt().coerceIn(-127, 127)

        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> {
                bluetoothHid.sendMouseMove(0, 0, 0, wheel)
            }
            ConnectionMode.WIFI -> {
                wifiController.sendMouseMove(0, 0, 0, wheel)
            }
            ConnectionMode.SIMULATOR -> {}
        }

        // In simulator, scroll moves the timeline playhead or zoom
        val cur = _simState.value
        val frameShift = wheel * 4
        val newFrame = (cur.currentFrame + frameShift).coerceIn(0, cur.totalFrames)
        _simState.value = cur.copy(currentFrame = newFrame)
    }

    // Jog Wheel / Timeline Scrubbing
    fun onJogTick(steps: Int, isFineSubpixel: Boolean = false) {
        val frameStep = if (isFineSubpixel || _settings.value.sniperMode) 1 else 3
        val deltaFrames = steps * frameStep

        triggerHapticTick()

        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> {
                // Send Left / Right arrow HID keys
                val key = if (steps > 0) HidConstants.KEY_RIGHT_ARROW else HidConstants.KEY_LEFT_ARROW
                bluetoothHid.sendKeyPress(emptySet(), key)
            }
            ConnectionMode.WIFI -> {
                wifiController.sendJogTick(steps, isFineSubpixel)
            }
            ConnectionMode.SIMULATOR -> {}
        }

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

        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> {
                bluetoothHid.sendKeyPress(emptySet(), HidConstants.KEY_SPACE)
            }
            ConnectionMode.WIFI -> {
                wifiController.sendKeyPress(emptySet(), "ESPACE")
            }
            ConnectionMode.SIMULATOR -> {}
        }

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
        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> bluetoothHid.sendKeyPress(emptySet(), HidConstants.KEY_J)
            ConnectionMode.WIFI -> wifiController.sendKeyPress(emptySet(), "J")
            ConnectionMode.SIMULATOR -> {}
        }
        logAction("Navette arrière (J)")
        val cur = _simState.value
        _simState.value = cur.copy(currentFrame = (cur.currentFrame - 24).coerceAtLeast(0))
    }

    fun shuttleForward() {
        triggerHapticClick()
        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> bluetoothHid.sendKeyPress(emptySet(), HidConstants.KEY_L)
            ConnectionMode.WIFI -> wifiController.sendKeyPress(emptySet(), "L")
            ConnectionMode.SIMULATOR -> {}
        }
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

        val hidCode = HidConstants.symbolToHidCode(shortcut.keySymbol)
        when (_connectionMode.value) {
            ConnectionMode.BLUETOOTH_HID -> {
                bluetoothHid.sendKeyPress(shortcut.modifiers, hidCode)
            }
            ConnectionMode.WIFI -> {
                wifiController.sendKeyPress(shortcut.modifiers, shortcut.keySymbol)
            }
            ConnectionMode.SIMULATOR -> {}
        }

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

    fun getPythonServerScript(): String = WifiControllerManager.PYTHON_SERVER_SCRIPT

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.stopListening()
        bluetoothHid.release()
        wifiController.close()
        playbackJob?.cancel()
    }
}
