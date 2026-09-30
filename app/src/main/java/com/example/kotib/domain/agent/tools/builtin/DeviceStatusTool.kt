package com.example.kotib.domain.agent.tools.builtin

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult

class DeviceStatusTool(private val context: Context) : AgentTool {
    override val name: String = "get_device_status"
    override val description: String =
        "Telefonning joriy holatini tekshiradi: batareya zaryadi, quvvatlanish, internet aloqasi turi (Wi-Fi/Mobil), ovoz rejimi va xotira."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "detailLevel" to mapOf(
                "type" to "STRING",
                "description" to "Holat darajasi: 'quick' (tezkor) yoki 'full' (to'liq)"
            )
        ),
        "required" to emptyList<String>()
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        return try {
            // Batareya holati
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            // Tarmoq holati
            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connManager?.activeNetwork
            val capabilities = connManager?.getNetworkCapabilities(activeNetwork)
            val networkType = when {
                capabilities == null -> "Aloqa yo'q (Oflayn)"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobil internet (4G/5G)"
                else -> "Boshqa tarmoq"
            }

            // Ovoz rejimi
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val ringerMode = when (audioManager?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "Ovozsiz (Silent)"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibratsiya"
                AudioManager.RINGER_MODE_NORMAL -> "Oddiy (Ovozli)"
                else -> "Noma'lum"
            }

            // Xotira
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val freeRamMb = (memInfo.availMem / (1024 * 1024))

            val data = mapOf(
                "batteryPercent" to batteryPct,
                "isCharging" to isCharging,
                "network" to networkType,
                "ringerMode" to ringerMode,
                "availableRamMb" to freeRamMb
            )

            val summary = "Batareya: $batteryPct% ${if (isCharging) "(Quvvatlanmoqda)" else ""}, Tarmoq: $networkType, Ovoz: $ringerMode, Bo'sh RAM: ${freeRamMb}MB"

            ToolExecutionResult(
                isSuccess = true,
                output = data,
                userSummary = summary
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xatolik yuz berdi")),
                userSummary = "Qurilma holatini aniqlashda xatolik: ${e.message}"
            )
        }
    }
}
