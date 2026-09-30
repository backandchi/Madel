package com.example.kotib.domain.automation

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

class SystemAutomationController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Ovoz rejimini o'zgartirish:
     * mode: "silent", "vibrate", "normal"
     */
    fun setRingerMode(mode: String): Boolean {
        return try {
            when (mode.lowercase()) {
                "silent", "jimjit" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    true
                }
                "vibrate", "tebranish" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    true
                }
                "normal", "odatiy" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    true
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Ovoz balandligini foizda (0-100%) o'rnatish
     * streamType: "music", "ring", "notification", "alarm"
     */
    fun setVolumePercent(streamType: String, percent: Int): Boolean {
        val stream = when (streamType.lowercase()) {
            "music", "musiqa" -> AudioManager.STREAM_MUSIC
            "ring", "qo'ng'iroq" -> AudioManager.STREAM_RING
            "notification", "bildirishnoma" -> AudioManager.STREAM_NOTIFICATION
            "alarm", "budilnik" -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }

        return try {
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val targetVolume = ((percent.coerceIn(0, 100) / 100.0) * maxVolume).toInt()
            audioManager.setStreamVolume(stream, targetVolume, AudioManager.FLAG_SHOW_UI)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getVolumeSummary(): Map<String, Any> {
        val ringer = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Jimjit"
            AudioManager.RINGER_MODE_VIBRATE -> "Tebranish (Vibratsiya)"
            else -> "Odatiy"
        }
        val musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val musicCur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val musicPercent = if (musicMax > 0) (musicCur * 100 / musicMax) else 0

        val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val ringCur = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val ringPercent = if (ringMax > 0) (ringCur * 100 / ringMax) else 0

        return mapOf(
            "ringerMode" to ringer,
            "musicVolumePercent" to musicPercent,
            "ringVolumePercent" to ringPercent,
            "isPowerSaveMode" to powerManager.isPowerSaveMode,
            "isInteractive" to powerManager.isInteractive
        )
    }

    fun vibrate(durationMs: Long = 300) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    /**
     * Tizim sozlamalari oynasini ochish
     */
    fun openSystemSetting(settingType: String): Boolean {
        val action = when (settingType.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "airplane", "samolyot" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            "sound", "ovoz" -> Settings.ACTION_SOUND_SETTINGS
            "battery", "batareya" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "display", "ekran" -> Settings.ACTION_DISPLAY_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Ilovani paket nomi yoki qidiruv orqali ishga tushirish
     */
    fun launchApp(appNameOrPackage: String): Pair<Boolean, String> {
        val pm = context.packageManager

        // 1. Agar to'liq paket nomi bo'lsa
        var intent = pm.getLaunchIntentForPackage(appNameOrPackage)
        if (intent != null) {
            context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            return Pair(true, "$appNameOrPackage ilovasi ochildi")
        }

        // 2. O'rnatilgan ilovalardan ism bo'yicha qidirish
        val packages = pm.getInstalledApplications(0)
        val match = packages.firstOrNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString()
            label.contains(appNameOrPackage, ignoreCase = true) ||
                    appInfo.packageName.contains(appNameOrPackage, ignoreCase = true)
        }

        if (match != null) {
            intent = pm.getLaunchIntentForPackage(match.packageName)
            if (intent != null) {
                context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                val label = pm.getApplicationLabel(match).toString()
                return Pair(true, "$label ($match.packageName) ochildi")
            }
        }

        return Pair(false, "'$appNameOrPackage' nomli ilova topilmadi")
    }
}
