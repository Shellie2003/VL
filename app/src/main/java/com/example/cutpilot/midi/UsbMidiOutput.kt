package com.example.cutpilot.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * USB MIDI: when the phone's USB mode is set to "MIDI", the computer sees a class-compliant
 * USB MIDI keyboard (no driver, ~1-3 ms). Android exposes that link to apps as a MIDI device
 * of type USB whose *input* port goes to the computer.
 */
class UsbMidiOutput(context: Context) {

    private val tag = "VolavoUsbMidi"
    private val manager: MidiManager? =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
        } else null

    private val thread = HandlerThread("volavo-midi", Process.THREAD_PRIORITY_URGENT_AUDIO).apply { start() }
    private val handler = Handler(thread.looper)

    @Volatile private var port: MidiInputPort? = null
    private var device: MidiDevice? = null
    private var deviceId = -1
    private val lock = Any()

    private val _deviceName = MutableStateFlow<String?>(null)
    /** Name of the USB MIDI link when ready, else null. */
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    val isSupported: Boolean get() = manager != null
    val isReady: Boolean get() = port != null

    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(info: MidiDeviceInfo) {
            if (port == null && isCandidate(info)) open(info)
        }

        override fun onDeviceRemoved(info: MidiDeviceInfo) {
            if (info.id == deviceId) close()
        }
    }

    init {
        manager?.let {
            @Suppress("DEPRECATION")
            it.registerDeviceCallback(callback, handler)
            handler.post { scan() }
        }
    }

    @Suppress("DEPRECATION")
    private fun allDevices(): List<MidiDeviceInfo> {
        val m = manager ?: return emptyList()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            m.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM).toList()
        } else {
            m.devices.toList()
        }
    }

    private fun isCandidate(info: MidiDeviceInfo) =
        info.type == MidiDeviceInfo.TYPE_USB && info.inputPortCount > 0

    /** Looks for the USB link again (e.g. after the user switched the USB mode to MIDI). */
    fun scan() {
        handler.post {
            if (port != null) return@post
            allDevices().firstOrNull { isCandidate(it) }?.let { open(it) }
        }
    }

    private fun open(info: MidiDeviceInfo) {
        val m = manager ?: return
        try {
            m.openDevice(info, { dev ->
                if (dev == null) return@openDevice
                val inputIndex = info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }?.portNumber ?: 0
                val p = dev.openInputPort(inputIndex)
                if (p == null) {
                    dev.close()
                    return@openDevice
                }
                synchronized(lock) {
                    device = dev
                    deviceId = info.id
                    port = p
                }
                val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT) ?: "USB MIDI"
                _deviceName.value = name
                Log.i(tag, "USB MIDI ready: $name")
            }, handler)
        } catch (e: Exception) {
            Log.w(tag, "openDevice failed", e)
        }
    }

    private fun close() {
        synchronized(lock) {
            runCatching { port?.close() }
            runCatching { device?.close() }
            port = null
            device = null
            deviceId = -1
        }
        _deviceName.value = null
    }

    /** Sends directly from the caller's thread: a local socket write of a few bytes. */
    fun send(message: ByteArray): Boolean {
        val p = port ?: return false
        return try {
            synchronized(lock) { p.send(message, 0, message.size) }
            true
        } catch (e: Exception) {
            Log.w(tag, "send failed", e)
            close()
            false
        }
    }

    fun release() {
        @Suppress("DEPRECATION")
        runCatching { manager?.unregisterDeviceCallback(callback) }
        close()
        thread.quitSafely()
    }
}
