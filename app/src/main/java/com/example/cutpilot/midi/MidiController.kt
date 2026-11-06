package com.example.cutpilot.midi

import com.example.cutpilot.network.WifiControllerManager

/** Where notes go: USB MIDI link first (lowest latency), else Vôlavo Hôte over Wi-Fi. */
enum class MidiRoute { USB, WIFI, NONE }

/**
 * Phone-as-MIDI-keyboard engine: notes (with velocity), pitch bend, modulation, sustain.
 * Keeps track of held notes so nothing can stay stuck (panic on leave / disconnect).
 */
class MidiController(
    private val usb: UsbMidiOutput,
    private val wifi: WifiControllerManager
) {
    @Volatile var channel: Int = 0
    private val held = IntArray(128) // reference count per note (several fingers on one key)
    private var lastBend = 0
    private var sustainOn = false

    val route: MidiRoute
        get() = when {
            usb.isReady -> MidiRoute.USB
            wifi.isReady -> MidiRoute.WIFI
            else -> MidiRoute.NONE
        }

    @Synchronized
    fun noteOn(note: Int, velocity: Int) {
        if (note !in 0..127) return
        held[note]++
        if (held[note] == 1) send(MidiMessages.noteOn(channel, note, velocity))
    }

    @Synchronized
    fun noteOff(note: Int) {
        if (note !in 0..127 || held[note] == 0) return
        held[note]--
        if (held[note] == 0) send(MidiMessages.noteOff(channel, note))
    }

    /** -8192..8191, 0 = centre. */
    fun pitchBend(bend: Int) {
        if (bend == lastBend) return
        lastBend = bend
        when (route) {
            MidiRoute.USB -> usb.send(MidiMessages.pitchBend(channel, bend))
            MidiRoute.WIFI -> wifi.midiPitchBend(channel, MidiMessages.bendTo14(bend))
            MidiRoute.NONE -> {}
        }
    }

    fun modulation(value: Int) = send(MidiMessages.controlChange(channel, 1, value))

    fun sustain(on: Boolean) {
        if (on == sustainOn) return
        sustainOn = on
        send(MidiMessages.controlChange(channel, 64, if (on) 127 else 0))
    }

    /** Releases everything: held notes, sustain, pitch bend. */
    @Synchronized
    fun panic() {
        for (n in 0..127) {
            if (held[n] > 0) {
                held[n] = 0
                send(MidiMessages.noteOff(channel, n))
            }
        }
        if (sustainOn) sustain(false)
        if (lastBend != 0) pitchBend(0)
        send(MidiMessages.controlChange(channel, 123, 0)) // All Notes Off
    }

    private fun send(message: ByteArray) {
        when (route) {
            MidiRoute.USB -> usb.send(message)
            MidiRoute.WIFI -> wifi.midi(message)
            MidiRoute.NONE -> {}
        }
    }
}
