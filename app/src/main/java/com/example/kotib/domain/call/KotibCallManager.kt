package com.example.kotib.domain.call

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.kotib.ui.call.CallActivity

class KotibCallManager(private val context: Context) {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Qo'ng'iroq jiringlashi va vibratsiyani boshlash
     */
    fun startRinging() {
        stopRinging()
        try {
            // Ringtone
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, notificationUri).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }

            // Vibratsiya
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    /**
     * Jiringlash va vibratsiyani to'xtatish
     */
    fun stopRinging() {
        try {
            ringtone?.stop()
            ringtone = null
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    /**
     * Ovozli karnayni yoqish/o'chirish
     */
    fun setSpeakerphoneOn(on: Boolean) {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = on
        } catch (_: Exception) {}
    }

    /**
     * To'liq ekranli kiruvchi qo'ng'iroqni ishga tushirish
     */
    fun triggerIncomingCall(
        summaryText: String = "Shoshilinch xabar: Akmal muhim xabar yozdi",
        urgencyLevel: Int = 5,
        attempt: Int = 1
    ) {
        val intent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallActivity.EXTRA_SUMMARY, summaryText)
            putExtra(CallActivity.EXTRA_URGENCY, urgencyLevel)
            putExtra(CallActivity.EXTRA_ATTEMPT, attempt)
            putExtra(CallActivity.EXTRA_IS_INCOMING, true)
        }

        context.startActivity(intent)
    }

    /**
     * Foydalanuvchi bevosita Kotibga ovozli qo'ng'iroq qilishni boshlashi
     */
    fun startDirectCall() {
        val intent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallActivity.EXTRA_IS_INCOMING, false)
        }
        context.startActivity(intent)
    }

    /**
     * Javob berilmagan qo'ng'iroq bildirishnomasi
     */
    fun showMissedCallNotification(summary: String) {
        val appIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            102,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "kotib_foreground_channel")
            .setContentTitle("Kotib: Javobsiz qolgan shoshilinch qo'ng'iroq")
            .setContentText(summary)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(2002, notification)
    }
}
