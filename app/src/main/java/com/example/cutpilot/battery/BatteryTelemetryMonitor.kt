package com.example.cutpilot.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.cutpilot.model.BatteryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryTelemetryMonitor(private val context: Context) {

    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)

                val percentage = if (level >= 0 && scale > 0) {
                    (level * 100 / scale)
                } else {
                    85
                }

                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val healthText = when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Excellente santé"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Surchauffe"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Batterie épuisée"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Surtension"
                    else -> "Bon état"
                }

                val tempCelsius = tempTenths / 10f

                _batteryInfo.value = BatteryInfo(
                    percentage = percentage,
                    isCharging = isCharging,
                    health = healthText,
                    temperatureCelsius = tempCelsius
                )
            }
        }
    }

    fun startListening() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stopListening() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
    }
}
