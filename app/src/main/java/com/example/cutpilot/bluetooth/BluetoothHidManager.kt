package com.example.cutpilot.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.model.ModifierKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

class BluetoothHidManager(private val context: Context) {

    private val tag = "BluetoothHidManager"

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val executor = Executors.newSingleThreadExecutor()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as? BluetoothHidDevice
                Log.d(tag, "Bluetooth HID Device Service connected")
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isRegistered.value = false
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _connectedDeviceName.value = null
                Log.d(tag, "Bluetooth HID Device Service disconnected")
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(tag, "onAppStatusChanged: registered=$registered, device=${pluggedDevice?.name}")
            _isRegistered.value = registered
            if (registered && pluggedDevice != null) {
                connectedDevice = pluggedDevice
                _connectedDeviceName.value = pluggedDevice.name ?: pluggedDevice.address
                _connectionStatus.value = ConnectionStatus.CONNECTED
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(tag, "onConnectionStateChanged: state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _connectedDeviceName.value = device?.name ?: device?.address ?: "Ordinateur hôte"
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionStatus.value = ConnectionStatus.CONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice == device) {
                        connectedDevice = null
                        _connectedDeviceName.value = null
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    }
                }
            }
        }
    }

    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun initialize() {
        try {
            if (bluetoothAdapter == null) {
                _errorMessage.value = "Le matériel Bluetooth n'est pas disponible sur cet appareil."
                return
            }
            if (!bluetoothAdapter.isEnabled) {
                _errorMessage.value = "Veuillez activer le Bluetooth dans les paramètres."
            }

            bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        } catch (e: Exception) {
            Log.e(tag, "Error initializing HID proxy", e)
            _errorMessage.value = "Initialisation HID: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val proxy = hidDevice ?: return
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "Vôlavo Mouse & Deck",
                "Souris sans fil et console de montage vidéo & Canva haute précision",
                "Vôlavo Studio",
                0x80.toByte(), // Combo Mouse & Keyboard subclass
                HidConstants.HID_REPORT_DESCRIPTOR
            )

            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800,
                9,
                0,
                11250,
                BluetoothHidDeviceAppQosSettings.MAX
            )

            proxy.registerApp(sdp, null, qos, executor, hidCallback)
            Log.d(tag, "Registered HID App successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error registering HID app", e)
            _errorMessage.value = "Échec d'enregistrement HID: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMouseMove(dx: Int, dy: Int, buttons: Int = 0, wheel: Int = 0): Boolean {
        val proxy = hidDevice ?: return false
        val device = connectedDevice ?: return false

        val clampedDx = dx.coerceIn(-127, 127).toByte()
        val clampedDy = dy.coerceIn(-127, 127).toByte()
        val clampedWheel = wheel.coerceIn(-127, 127).toByte()
        val buttonByte = (buttons and 0x07).toByte()

        val report = byteArrayOf(buttonByte, clampedDx, clampedDy, clampedWheel)
        return try {
            proxy.sendReport(device, HidConstants.REPORT_ID_MOUSE.toInt(), report)
        } catch (e: Exception) {
            Log.e(tag, "Error sending mouse report", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun sendKeyPress(modifiers: Set<ModifierKey>, hidCode: Byte): Boolean {
        val proxy = hidDevice ?: return false
        val device = connectedDevice ?: return false

        var modByte: Byte = 0
        modifiers.forEach { mod ->
            when (mod) {
                ModifierKey.CTRL -> modByte = (modByte.toInt() or HidConstants.MOD_LEFT_CTRL.toInt()).toByte()
                ModifierKey.SHIFT -> modByte = (modByte.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                ModifierKey.ALT -> modByte = (modByte.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
                ModifierKey.CMD -> modByte = (modByte.toInt() or HidConstants.MOD_LEFT_META.toInt()).toByte()
            }
        }

        val pressReport = byteArrayOf(
            modByte,
            0x00.toByte(), // Reserved
            hidCode,
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte()
        )

        val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)

        return try {
            val pressed = proxy.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), pressReport)
            executor.execute {
                Thread.sleep(25) // standard key down duration
                proxy.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), releaseReport)
            }
            pressed
        } catch (e: Exception) {
            Log.e(tag, "Error sending key report", e)
            false
        }
    }

    fun disconnect() {
        hidDevice?.let { proxy ->
            try {
                connectedDevice?.let { proxy.disconnect(it) }
            } catch (e: Exception) {
                Log.e(tag, "Error disconnecting", e)
            }
        }
        connectedDevice = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _connectedDeviceName.value = null
    }

    fun release() {
        try {
            hidDevice?.let { proxy ->
                proxy.unregisterApp()
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing HID proxy", e)
        }
    }
}
