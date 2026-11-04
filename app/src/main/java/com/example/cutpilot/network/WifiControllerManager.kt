package com.example.cutpilot.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.example.cutpilot.input.InputSink
import com.example.cutpilot.model.ConnectionStatus
import com.example.cutpilot.model.LatencyInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.PortUnreachableException
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * Low-latency Wi-Fi transport talking to "Vôlavo Hôte" over UDP.
 *
 * Performance notes:
 *  - compact binary packets (5 bytes for a move instead of ~80 bytes of JSON);
 *  - a single dedicated, high-priority sender thread with pre-allocated buffers (no
 *    allocation and no coroutine launch per touch event);
 *  - motion coalescing: if the radio is busy, pending deltas are merged instead of
 *    queued, so latency never piles up;
 *  - connected socket + DSCP "Expedited Forwarding" (WMM voice queue) + Wi-Fi
 *    low-latency lock (disables Wi-Fi power save while connected);
 *  - real round-trip time measured with PING/PONG, automatic re-connection.
 */
class WifiControllerManager(context: Context) : InputSink {

    private val tag = "VolavoWifi"
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _latencyInfo = MutableStateFlow(LatencyInfo(pingMs = 0, packetsSent = 0, packetRateHz = 0, measured = false))
    val latencyInfo: StateFlow<LatencyInfo> = _latencyInfo.asStateFlow()

    private val _hostName = MutableStateFlow<String?>(null)
    val hostName: StateFlow<String?> = _hostName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _targetIp = MutableStateFlow("")
    val targetIp: StateFlow<String> = _targetIp.asStateFlow()

    // --- sender thread (all socket writes happen here) -----------------------
    private val sender = Executors.newSingleThreadExecutor { r ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            r.run()
        }, "volavo-udp-tx").apply { isDaemon = true }
    }
    private val txBuf = ByteArray(64)
    private val txPacket = DatagramPacket(txBuf, txBuf.size)

    @Volatile private var socket: DatagramSocket? = null
    @Volatile private var ready = false
    @Volatile private var helloBytes: ByteArray? = null
    @Volatile private var lastPongAt = 0L
    @Volatile private var rttMs = -1.0

    private val moveLock = Any()
    private var accDx = 0
    private var accDy = 0
    private var moveScheduled = false
    private val flushMoveTask = Runnable { flushMove() }

    private val sent = AtomicLong()
    override val packetsSent: Long get() = sent.get()

    private var session: Job? = null
    private var wifiLock: WifiManager.WifiLock? = null

    val isReady: Boolean get() = ready

    // =========================================================================
    // Session management
    // =========================================================================
    fun connect(pairing: PairingInfo, deviceName: String) {
        session?.cancel()
        _targetIp.value = pairing.hosts.first()
        _hostName.value = pairing.hostName
        helloBytes = buildHello(pairing.key, deviceName)
        session = scope.launch { runSession(pairing) }
    }

    fun disconnect() {
        if (ready) post { txBuf[0] = VolavoProtocol.T_BYE; transmit(1) }
        session?.cancel()
        session = null
        _status.value = ConnectionStatus.DISCONNECTED
        _errorMessage.value = null
    }

    fun close() {
        disconnect()
        sender.shutdown()
    }

    private sealed class Handshake {
        class Ok(val socket: DatagramSocket, val host: String, val hostName: String) : Handshake()
        class Fatal(val message: String) : Handshake()
        object Unreachable : Handshake()
    }

    private suspend fun runSession(pairing: PairingInfo) {
        _status.value = ConnectionStatus.CONNECTING
        _errorMessage.value = null
        acquireWifiLock()
        var backoffMs = 400L
        try {
            while (currentCoroutineContext().isActive) {
                when (val hs = handshake(pairing)) {
                    is Handshake.Fatal -> {
                        _errorMessage.value = hs.message
                        _status.value = ConnectionStatus.ERROR
                        return
                    }
                    Handshake.Unreachable -> {
                        _status.value = ConnectionStatus.CONNECTING
                        _errorMessage.value = "Hôte injoignable — Vôlavo Hôte est-il lancé sur le même réseau ?"
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(4000L)
                    }
                    is Handshake.Ok -> {
                        backoffMs = 400L
                        runConnected(hs)
                    }
                }
            }
        } finally {
            ready = false
            releaseWifiLock()
        }
    }

    private suspend fun runConnected(hs: Handshake.Ok) = coroutineScope {
        val sock = hs.socket
        socket = sock
        _targetIp.value = hs.host
        _hostName.value = hs.hostName
        _errorMessage.value = null
        lastPongAt = System.nanoTime()
        rttMs = -1.0
        ready = true
        _status.value = ConnectionStatus.CONNECTED
        Log.i(tag, "Connected to ${hs.hostName} (${hs.host})")

        val rx = launch(Dispatchers.IO) { receiveLoop(sock) }
        try {
            var lastCount = sent.get()
            var lastTick = System.nanoTime()
            while (isActive) {
                post {
                    txBuf[0] = VolavoProtocol.T_PING
                    VolavoProtocol.putLong(txBuf, 1, System.nanoTime())
                    transmit(9)
                }
                delay(1000)
                val now = System.nanoTime()
                val count = sent.get()
                val rate = ((count - lastCount) * 1e9 / (now - lastTick)).roundToInt()
                lastCount = count
                lastTick = now
                _latencyInfo.value = LatencyInfo(
                    pingMs = if (rttMs >= 0) rttMs.roundToInt().coerceAtLeast(1) else 0,
                    packetsSent = count,
                    packetRateHz = rate,
                    measured = rttMs >= 0
                )
                if (now - lastPongAt > 3_500_000_000L) {
                    Log.w(tag, "Host silent, reconnecting")
                    break
                }
            }
            _status.value = ConnectionStatus.CONNECTING
        } finally {
            ready = false
            rx.cancel()
            // Let a pending BYE / queued packets leave before the socket closes.
            runCatching { sender.submit {}.get(200, TimeUnit.MILLISECONDS) }
            sock.close()
            socket = null
        }
    }

    private fun handshake(pairing: PairingInfo): Handshake {
        val hello = helloBytes ?: return Handshake.Fatal("Clé manquante")
        for (host in pairing.hosts) {
            val sock = try {
                DatagramSocket().apply {
                    soTimeout = 300
                    runCatching { trafficClass = 0xB8 } // DSCP EF -> WMM voice queue
                    runCatching { sendBufferSize = 8 * 1024 }
                    connect(InetAddress.getByName(host), pairing.port)
                }
            } catch (e: Exception) {
                Log.w(tag, "Cannot open socket to $host", e)
                continue
            }
            val buf = ByteArray(128)
            val pk = DatagramPacket(buf, buf.size)
            try {
                repeat(3) {
                    sock.send(DatagramPacket(hello, hello.size))
                    try {
                        pk.length = buf.size
                        sock.receive(pk)
                        if (pk.length >= 2 && buf[0] == VolavoProtocol.T_HELLO_ACK) {
                            when (buf[1].toInt()) {
                                VolavoProtocol.ACK_OK -> {
                                    val name = String(buf, 2, pk.length - 2, Charsets.UTF_8)
                                        .ifBlank { pairing.hostName }
                                    sock.soTimeout = 500
                                    return Handshake.Ok(sock, host, name)
                                }
                                VolavoProtocol.ACK_BAD_KEY -> {
                                    sock.close()
                                    return Handshake.Fatal("Clé refusée par l'ordinateur — rescannez le QR code.")
                                }
                                else -> {
                                    sock.close()
                                    return Handshake.Fatal("Version de Vôlavo Hôte incompatible — mettez-la à jour.")
                                }
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                    }
                }
            } catch (_: PortUnreachableException) {
            } catch (e: Exception) {
                Log.w(tag, "Handshake with $host failed", e)
            }
            sock.close()
        }
        return Handshake.Unreachable
    }

    private fun receiveLoop(sock: DatagramSocket) {
        val buf = ByteArray(128)
        val pk = DatagramPacket(buf, buf.size)
        while (ready && !sock.isClosed) {
            try {
                pk.length = buf.size
                sock.receive(pk)
            } catch (_: SocketTimeoutException) {
                continue
            } catch (_: Exception) {
                break
            }
            when (buf[0]) {
                VolavoProtocol.T_PONG -> if (pk.length >= 9) {
                    val now = System.nanoTime()
                    val sample = (now - VolavoProtocol.getLong(buf, 1)) / 1_000_000.0
                    rttMs = if (rttMs < 0) sample else rttMs * 0.7 + sample * 0.3
                    lastPongAt = now
                }
                VolavoProtocol.T_HELLO_ACK -> lastPongAt = System.nanoTime()
                VolavoProtocol.T_NEED_AUTH -> {
                    // Host restarted: re-authenticate transparently.
                    val hello = helloBytes ?: continue
                    post {
                        runCatching { socket?.send(DatagramPacket(hello, hello.size)) }
                    }
                }
            }
        }
    }

    private fun buildHello(key: String, deviceName: String): ByteArray {
        val keyBytes = key.toByteArray(Charsets.US_ASCII)
        val nameBytes = deviceName.toByteArray(Charsets.UTF_8).let { if (it.size > 48) it.copyOf(48) else it }
        val out = ByteArray(2 + VolavoProtocol.KEY_LEN + nameBytes.size)
        out[0] = VolavoProtocol.T_HELLO
        out[1] = VolavoProtocol.VERSION.toByte()
        System.arraycopy(keyBytes, 0, out, 2, VolavoProtocol.KEY_LEN.coerceAtMost(keyBytes.size))
        System.arraycopy(nameBytes, 0, out, 2 + VolavoProtocol.KEY_LEN, nameBytes.size)
        return out
    }

    // =========================================================================
    // InputSink — hot path
    // =========================================================================
    override fun move(dx: Int, dy: Int) {
        if (!ready || (dx == 0 && dy == 0)) return
        synchronized(moveLock) {
            accDx += dx
            accDy += dy
            if (moveScheduled) return
            moveScheduled = true
        }
        post(flushMoveTask)
    }

    /** Sender thread only. */
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
            val sx = dx.coerceIn(-32767, 32767)
            val sy = dy.coerceIn(-32767, 32767)
            txBuf[0] = VolavoProtocol.T_MOVE
            VolavoProtocol.putShort(txBuf, 1, sx)
            VolavoProtocol.putShort(txBuf, 3, sy)
            transmit(5)
            dx -= sx
            dy -= sy
        }
    }

    override fun setButtons(mask: Int) {
        if (!ready) return
        post {
            flushMove()
            txBuf[0] = VolavoProtocol.T_BUTTONS
            txBuf[1] = (mask and 0x07).toByte()
            transmit(2)
            transmit(2) // full-state packet: sending it twice makes a lost release harmless
        }
    }

    override fun scroll(vertical: Int, horizontal: Int) {
        if (!ready || (vertical == 0 && horizontal == 0)) return
        post {
            flushMove()
            txBuf[0] = VolavoProtocol.T_SCROLL
            VolavoProtocol.putShort(txBuf, 1, vertical.coerceIn(-32767, 32767))
            VolavoProtocol.putShort(txBuf, 3, horizontal.coerceIn(-32767, 32767))
            transmit(5)
        }
    }

    override fun key(modifiers: Int, usage: Int, action: Int) {
        if (!ready) return
        post {
            flushMove()
            txBuf[0] = VolavoProtocol.T_KEY
            txBuf[1] = modifiers.toByte()
            txBuf[2] = usage.toByte()
            txBuf[3] = action.toByte()
            transmit(4)
        }
    }

    override fun keyPhysical(modifiers: Int, usage: Int, action: Int) {
        if (!ready) return
        post {
            flushMove()
            txBuf[0] = VolavoProtocol.T_KEY_PHYS
            txBuf[1] = modifiers.toByte()
            txBuf[2] = usage.toByte()
            txBuf[3] = action.toByte()
            transmit(4)
        }
    }

    override fun text(text: String): Boolean {
        if (!ready) return true
        if (text.isEmpty()) return true
        // Split into UTF-8 chunks that fit comfortably in one datagram (code points never split).
        val chunks = ArrayList<ByteArray>()
        val sb = StringBuilder()
        var size = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val n = Character.charCount(cp)
            val bytes = text.substring(i, i + n).toByteArray(Charsets.UTF_8).size
            if (size + bytes > 900) {
                chunks.add(sb.toString().toByteArray(Charsets.UTF_8))
                sb.setLength(0)
                size = 0
            }
            sb.appendCodePoint(cp)
            size += bytes
            i += n
        }
        if (sb.isNotEmpty()) chunks.add(sb.toString().toByteArray(Charsets.UTF_8))
        post {
            flushMove()
            val s = socket ?: return@post
            for (c in chunks) {
                val pkt = ByteArray(c.size + 1)
                pkt[0] = VolavoProtocol.T_TEXT
                System.arraycopy(c, 0, pkt, 1, c.size)
                try {
                    s.send(DatagramPacket(pkt, pkt.size))
                    sent.incrementAndGet()
                } catch (_: Exception) {
                }
            }
        }
        return true
    }

    override fun media(usage: Int) {
        if (!ready) return
        post {
            flushMove()
            txBuf[0] = VolavoProtocol.T_MEDIA
            VolavoProtocol.putShort(txBuf, 1, usage)
            transmit(3)
        }
    }

    private fun post(task: Runnable) {
        try {
            sender.execute(task)
        } catch (_: java.util.concurrent.RejectedExecutionException) {
        }
    }

    /** Sender thread only. */
    private fun transmit(length: Int) {
        val s = socket ?: return
        try {
            txPacket.setData(txBuf, 0, length)
            s.send(txPacket)
            sent.incrementAndGet()
        } catch (e: Exception) {
            Log.v(tag, "send failed: ${e.message}")
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    @Suppress("DEPRECATION")
    private fun acquireWifiLock() {
        try {
            val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = (wifiLock ?: wm.createWifiLock(mode, "volavo:low-latency").apply {
                setReferenceCounted(false)
            }).also { if (!it.isHeld) it.acquire() }
        } catch (e: Exception) {
            Log.w(tag, "Wi-Fi lock unavailable", e)
        }
    }

    private fun releaseWifiLock() {
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
    }

    fun getLocalIpAddress(): String = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .sortedByDescending { it.name.startsWith("wlan") }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress ?: "—"
    } catch (_: Exception) {
        "—"
    }
}
