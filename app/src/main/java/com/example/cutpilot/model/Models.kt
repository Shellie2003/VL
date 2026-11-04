package com.example.cutpilot.model

enum class ConnectionMode {
    BLUETOOTH_HID,
    WIFI,
    SIMULATOR
}

enum class ConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class BatteryInfo(
    val percentage: Int = 85,
    val isCharging: Boolean = false,
    val health: String = "Bon état",
    val temperatureCelsius: Float = 27.5f
)

data class LatencyInfo(
    val pingMs: Int = 0,
    val packetsSent: Long = 0,
    val packetRateHz: Int = 0,
    /** true when [pingMs] is a real round-trip measurement (Wi-Fi PING/PONG). */
    val measured: Boolean = false
)

enum class ModifierKey {
    CTRL,
    SHIFT,
    ALT,
    CMD
}

data class ShortcutKey(
    val id: String,
    val label: String,
    val category: String, // Navigation, Découpe, Marqueurs, Outils, Zoom
    val keySymbol: String,
    val modifiers: Set<ModifierKey> = emptySet(),
    val hidCode: Byte = 0,
    val colorHex: Long = 0xFF00E5FF,
    val description: String = ""
)

data class SoftwareProfile(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val shortcuts: List<ShortcutKey>
)

data class ControllerSettings(
    val sensitivity: Float = 1.0f,
    val acceleration: Boolean = true,
    val sniperMode: Boolean = false,
    val sniperMultiplier: Float = 0.22f,
    val hapticFeedback: Boolean = true,
    val naturalScroll: Boolean = false,
    val tapToClick: Boolean = true,
    val jogSensitivity: Float = 1.0f,
    val keepScreenAwake: Boolean = true,
    val highContrastPointers: Boolean = true,
    /** Bluetooth HID sends physical key positions: remap letters for an AZERTY computer. */
    val azertyHost: Boolean = false,
    val scrollSpeed: Float = 1.0f,
    /** Keyboard shown in landscape, full width like a real keyboard ("piano" mode). */
    val keyboardLandscape: Boolean = false
)

enum class MainNavigationTab {
    TRACKPAD,
    KEYBOARD,
    JOG_WHEEL,
    CANVA,
    DECK,
    CONNECTION,
    SETTINGS
}
