package com.example.cutpilot.network

import java.net.URLDecoder

/**
 * Content of the QR code shown by "Vôlavo Hôte":
 * `volavo://pair?v=1&h=192.168.1.20,10.0.0.4&p=50505&k=KEY16CHARS&n=Mon%20PC&b=AA:BB:CC:DD:EE:FF`
 */
data class PairingInfo(
    val hosts: List<String>,
    val port: Int,
    val key: String,
    val hostName: String,
    val bluetoothMac: String? = null
) {
    fun toUri(): String = buildString {
        append("volavo://pair?v=").append(VolavoProtocol.VERSION)
        append("&h=").append(hosts.joinToString(","))
        append("&p=").append(port)
        append("&k=").append(key)
        append("&n=").append(java.net.URLEncoder.encode(hostName, "UTF-8").replace("+", "%20"))
        bluetoothMac?.let { append("&b=").append(it) }
    }

    companion object {
        private val IPV4 = Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$")
        private val MAC = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")
        private val KEY = Regex("^[A-Z2-7]{16}$")

        fun normalizeKey(raw: String): String =
            raw.replace("-", "").replace(" ", "").uppercase()

        fun isValidKey(key: String): Boolean = KEY.matches(key)

        /** Returns null when [raw] is not a valid Vôlavo pairing link. */
        fun parse(raw: String): PairingInfo? {
            val text = raw.trim()
            if (!text.startsWith("volavo://pair", ignoreCase = true)) return null
            val query = text.substringAfter('?', "")
            if (query.isEmpty()) return null
            val params = query.split('&').mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null
                else part.substring(0, idx) to runCatching {
                    URLDecoder.decode(part.substring(idx + 1), "UTF-8")
                }.getOrDefault("")
            }.toMap()

            val version = params["v"]?.toIntOrNull() ?: return null
            if (version != VolavoProtocol.VERSION) return null
            val hosts = params["h"].orEmpty().split(',').map { it.trim() }.filter { IPV4.matches(it) }
            if (hosts.isEmpty()) return null
            val port = params["p"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: VolavoProtocol.DEFAULT_PORT
            val key = normalizeKey(params["k"].orEmpty())
            if (!isValidKey(key)) return null
            val name = params["n"].orEmpty().ifBlank { hosts.first() }.take(40)
            val mac = params["b"]?.uppercase()?.replace('-', ':')?.takeIf { MAC.matches(it) }
            return PairingInfo(hosts, port, key, name, mac)
        }
    }
}
