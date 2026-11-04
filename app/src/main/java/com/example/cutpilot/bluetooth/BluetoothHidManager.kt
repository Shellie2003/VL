package com.example.cutpilot.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.cutpilot.input.InputSink
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.network.VolavoProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

/**
 * Bluetooth HID (mouse + keyboard) transport: the computer sees a real Bluetooth mouse,
 * no host software required.
 *
 * Performance notes:
 *  - every `sendReport` (a binder call) runs on a dedicated URGENT_DISPLAY thread, never
 *    on the UI thread;
 *  - motion is coalesced: when the radio is congested, deltas are merged instead of queued,
 *    so the cursor never lags behind the finger;
 *  - deltas larger than ±127 are split into several reports instead of being clamped;
 *  - real button state (press / release) so drag & drop and drag-lock work.
 */
@SuppressLint("MissingPermission")
class BluetoothHidManager(context: Context) : InputSink {

    private val tag = "VolavoHid"
    private val appContext = context.applicationContext

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val thread = HandlerThread("volavo-hid", Process.THREAD_PRIORITY_URGENT_DISPLAY).apply { start() }
    private val handler = Handler(thread.looper)
    private val executor = Executor { handler.post(it) }

    @Volatile private var hidDevice: BluetoothHidDevice? = null
    @Volatile private var connectedDevice: BluetoothDevice? = null
    @Volatile private var pendingMac: String? = null
    private var proxyRequested = false

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- report state (HID thread only) --------------------------------------
    private val mouseReport = ByteArray(5)
    private val keyReport = ByteArray(8)
    private val emptyKeyReport = ByteArray(8)
    private var buttons = 0
    private var accScrollV = 0
    private var accScrollH = 0

    private val moveLock = Any()
    private var accDx = 0
    private var accDy = 0
    private var moveScheduled = false
    private val flushMoveTask = Runnable { flushMove() }

    private val sent = AtomicLong()
    override val packetsSent: Long get() = sent.get()

    val isReady: Boolean get() = hidDevice != null && connectedDevice != null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as? BluetoothHidDevice
                Log.d(tag, "HID Device service connected")
                handler.post { registerHidApp() }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                connectedDevice = null
                _isRegistered.value = false
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _connectedDeviceName.value = null
                _connectedDeviceAddress.value = null
            }
        }
    }

    /** Bluetooth HID Device exists only on Android 9+ (API 28): created lazily, never touched before. */
    private val hidSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    private val hidCallback by lazy { createHidCallback() }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun createHidCallback() = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(tag, "onAppStatusChanged registered=$registered")
            _isRegistered.value = registered
            if (registered) {
                _errorMessage.value = null
                if (pluggedDevice != null) {
                    markConnected(pluggedDevice)
                } else {
                    pendingMac?.let { connectTo(it) }
                }
            } else {
                connectedDevice = null
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(tag, "onConnectionStateChanged state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> device?.let { markConnected(it) }
                BluetoothProfile.STATE_CONNECTING -> _connectionStatus.value = ConnectionStatus.CONNECTING
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice == null || connectedDevice == device) {
                        connectedDevice = null
                        buttons = 0
                        _connectedDeviceName.value = null
                        _connectedDeviceAddress.value = null
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            val proxy = hidDevice ?: return
            val data = if (id == HidConstants.REPORT_ID_KEYBOARD) emptyKeyReport else ByteArray(mouseReport.size)
            safely { proxy.replyReport(device, type, id, data) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun markConnected(device: BluetoothDevice) {
        connectedDevice = device
        _connectedDeviceName.value = safely { device.name } ?: device.address ?: "Ordinateur hôte"
        _connectedDeviceAddress.value = device.address
        _connectionStatus.value = ConnectionStatus.CONNECTED
        _errorMessage.value = null
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    /** Safe to call several times (e.g. again after the permission dialog). */
    fun initialize() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _errorMessage.value = "Le matériel Bluetooth n'est pas disponible sur cet appareil."
            return
        }
        if (!hasConnectPermission()) {
            _errorMessage.value = "Autorisez l'accès Bluetooth pour utiliser le mode souris HID."
            return
        }
        if (!adapter.isEnabled) {
            _errorMessage.value = "Veuillez activer le Bluetooth."
        }
        if (!hidSupported) {
            _errorMessage.value = "Le mode souris Bluetooth nécessite Android 9 ou plus récent. Utilisez le Wi-Fi."
            return
        }
        if (proxyRequested) return
        try {
            proxyRequested = adapter.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE)
            if (!proxyRequested) _errorMessage.value = "Ce téléphone ne prend pas en charge le profil Bluetooth HID."
        } catch (e: Exception) {
            Log.e(tag, "Error initializing HID proxy", e)
            _errorMessage.value = "Initialisation HID : ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        if (!hidSupported) return
        val proxy = hidDevice ?: return
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "Vôlavo Mouse & Deck",
                "Souris sans fil et console de montage haute précision",
                "Vôlavo Studio",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HidConstants.HID_REPORT_DESCRIPTOR
            )
            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
            )
            if (!proxy.registerApp(sdp, null, qos, executor, hidCallback)) {
                _errorMessage.value = "Enregistrement HID refusé (une autre app l'utilise peut-être)."
            }
        } catch (e: Exception) {
            Log.e(tag, "Error registering HID app", e)
            _errorMessage.value = "Échec d'enregistrement HID : ${e.message}"
        }
    }

    /**
     * Connects directly to the computer whose Bluetooth address came from the QR code.
     * If the HID app is not registered yet the connection is attempted as soon as it is.
     */
    @SuppressLint("MissingPermission")
    fun connectTo(mac: String) {
        pendingMac = mac
        handler.post {
            val proxy = hidDevice
            val adapter = bluetoothAdapter
            if (proxy == null || adapter == null || !_isRegistered.value) return@post
            if (connectedDevice?.address.equals(mac, ignoreCase = true)) return@post
            try {
                val device = adapter.getRemoteDevice(mac)
                _connectionStatus.value = ConnectionStatus.CONNECTING
                if (!proxy.connect(device)) {
                    _connectionStatus.value = ConnectionStatus.ERROR
                    _errorMessage.value = "Connexion Bluetooth impossible. Le Bluetooth du PC est-il activé ?"
                }
            } catch (e: Exception) {
                Log.e(tag, "connectTo($mac) failed", e)
                _connectionStatus.value = ConnectionStatus.ERROR
                _errorMessage.value = "Connexion Bluetooth impossible : ${e.message}"
            }
        }
    }

    // =========================================================================
    // InputSink — hot path
    // =========================================================================
    override fun move(dx: Int, dy: Int) {
        if (!isReady || (dx == 0 && dy == 0)) return
        synchronized(moveLock) {
            accDx += dx
            accDy += dy
            if (moveScheduled) return
            moveScheduled = true
        }
        handler.post(flushMoveTask)
    }

    /** HID thread only. */
    private fun flushMove() {
        var dx: Int
        var dy: Int
        synchronized(moveLock) {
            dx = accDx
            dy = accDy
            accDx = 0
            accDy = 0
            moveScheduled = false
        }
        while (dx != 0 || dy != 0) {
            val sx = dx.coerceIn(-127, 127)
            val sy = dy.coerceIn(-127, 127)
            sendMouse(sx, sy, 0, 0)
            dx -= sx
            dy -= sy
        }
    }

    override fun setButtons(mask: Int) {
        if (!isReady) return
        handler.post {
            flushMove()
            buttons = mask and 0x07
            sendMouse(0, 0, 0, 0)
        }
    }

    override fun scroll(vertical: Int, horizontal: Int) {
        if (!isReady || (vertical == 0 && horizontal == 0)) return
        handler.post {
            flushMove()
            accScrollV += vertical
            accScrollH += horizontal
            val nv = accScrollV / VolavoProtocol.NOTCH
            val nh = accScrollH / VolavoProtocol.NOTCH
            accScrollV -= nv * VolavoProtocol.NOTCH
            accScrollH -= nh * VolavoProtocol.NOTCH
            if (nv != 0 || nh != 0) sendMouse(0, 0, nv.coerceIn(-127, 127), nh.coerceIn(-127, 127))
        }
    }

    override fun key(modifiers: Int, usage: Int, action: Int) {
        if (!isReady) return
        handler.post {
            flushMove()
            when (action) {
                VolavoProtocol.KEY_TAP -> {
                    // Press and release as two distinct reports: repeated taps of the same
                    // key (jog wheel) are never merged into a single press.
                    sendKey(modifiers, usage)
                    sendKey(0, 0)
                }
                VolavoProtocol.KEY_DOWN -> sendKey(modifiers, usage)
                else -> sendKey(0, 0)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendMouse(dx: Int, dy: Int, wheel: Int, pan: Int) {
        val proxy = hidDevice ?: return
        val device = connectedDevice ?: return
        mouseReport[0] = buttons.toByte()
        mouseReport[1] = dx.toByte()
        mouseReport[2] = dy.toByte()
        mouseReport[3] = wheel.toByte()
        mouseReport[4] = pan.toByte()
        if (safely { proxy.sendReport(device, HidConstants.REPORT_ID_MOUSE.toInt(), mouseReport) } == true) {
            sent.incrementAndGet()
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendKey(modifiers: Int, usage: Int) {
        val proxy = hidDevice ?: return
        val device = connectedDevice ?: return
        keyReport[0] = modifiers.toByte()
        keyReport[2] = usage.toByte()
        if (safely { proxy.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyReport) } == true) {
            sent.incrementAndGet()
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================
    @SuppressLint("MissingPermission")
    fun disconnect() {
        pendingMac = null
        handler.post {
            buttons = 0
            sendMouse(0, 0, 0, 0)
            sendKey(0, 0)
            connectedDevice?.let { dev -> safely { hidDevice?.disconnect(dev) } }
            connectedDevice = null
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _connectedDeviceName.value = null
            _connectedDeviceAddress.value = null
        }
    }

    @SuppressLint("MissingPermission")
    fun release() {
        handler.post {
            try {
                hidDevice?.let { proxy ->
                    proxy.unregisterApp()
                    bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error releasing HID proxy", e)
            }
            hidDevice = null
            thread.quitSafely()
        }
    }

    private inline fun <T> safely(block: () -> T): T? = try {
        block()
    } catch (e: SecurityException) {
        _errorMessage.value = "Permission Bluetooth manquante."
        null
    } catch (e: Exception) {
        Log.w(tag, "HID call failed", e)
        null
    }
}
