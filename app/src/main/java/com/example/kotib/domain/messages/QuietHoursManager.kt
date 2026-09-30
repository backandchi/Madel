package com.example.kotib.domain.messages

import android.content.Context
import java.util.Calendar

class QuietHoursManager(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("kotib_quiet_hours", Context.MODE_PRIVATE)
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean("is_enabled", true)
        set(value) = prefs.edit().putBoolean("is_enabled", value).apply()

    var startHour: Int
        get() = prefs.getInt("start_hour", 23) // 23:00
        set(value) = prefs.edit().putInt("start_hour", value).apply()

    var startMinute: Int
        get() = prefs.getInt("start_minute", 0)
        set(value) = prefs.edit().putInt("start_minute", value).apply()

    var endHour: Int
        get() = prefs.getInt("end_hour", 7) // 07:00
        set(value) = prefs.edit().putInt("end_hour", value).apply()

    var endMinute: Int
        get() = prefs.getInt("end_minute", 0)
        set(value) = prefs.edit().putInt("end_minute", value).apply()

    var allowVipInQuietHours: Boolean
        get() = prefs.getBoolean("allow_vip", true)
        set(value) = prefs.edit().putBoolean("allow_vip", value).apply()

    /**
     * Hozirgi vaqt "Jim soatlar" (Quiet Hours) oralig'idami yoki yo'qligini aniqlaydi.
     */
    fun isQuietHoursNow(): Boolean {
        if (!isEnabled) return false

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes > endMinutes) {
            // Tun orqali o'tish (masalan: 23:00 dan 07:00 gacha)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes until endMinutes
        }
    }

    fun getTimeRangeString(): String {
        return String.format(java.util.Locale.US, "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }
}
