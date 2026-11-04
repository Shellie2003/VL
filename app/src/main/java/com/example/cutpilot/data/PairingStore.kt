package com.example.cutpilot.data

import android.content.Context
import com.example.cutpilot.model.ConnectionMode
import com.example.cutpilot.network.PairingInfo

/** Remembers the last paired computer so the phone reconnects automatically at launch. */
class PairingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("volavo_pairing", Context.MODE_PRIVATE)

    var lastPairing: PairingInfo?
        get() = prefs.getString(KEY_URI, null)?.let { PairingInfo.parse(it) }
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_URI) else putString(KEY_URI, value.toUri())
        }.apply()

    var lastBluetoothHost: String?
        get() = prefs.getString(KEY_BT, null)
        set(value) = prefs.edit().putString(KEY_BT, value).apply()

    var connectionMode: ConnectionMode
        get() = runCatching { ConnectionMode.valueOf(prefs.getString(KEY_MODE, null) ?: "") }
            .getOrDefault(ConnectionMode.BLUETOOTH_HID)
        set(value) = prefs.edit().putString(KEY_MODE, value.name).apply()

    var azertyHost: Boolean
        get() = prefs.getBoolean(KEY_AZERTY, false)
        set(value) = prefs.edit().putBoolean(KEY_AZERTY, value).apply()

    private companion object {
        const val KEY_AZERTY = "azerty_host"
        const val KEY_URI = "last_pairing_uri"
        const val KEY_BT = "last_bt_host"
        const val KEY_MODE = "connection_mode"
    }
}
