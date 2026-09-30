package com.example.kotib.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.KotibApp
import com.example.kotib.data.local.entity.NotificationMessageEntity

class KotibNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val packageName = sbn.packageName ?: return

        // O'z ilovamiz va tizim doimiy bildirishnomalarini e'tiborsiz qoldirish
        if (packageName == applicationContext.packageName) return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val content = if (bigText.isNotBlank()) bigText else text
        if (title.isBlank() && content.isBlank()) return

        // Ilova nomini aniqlash
        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            when {
                packageName.contains("telegram", ignoreCase = true) -> "Telegram"
                packageName.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
                packageName.contains("instagram", ignoreCase = true) -> "Instagram"
                packageName.contains("mms", ignoreCase = true) || packageName.contains("messaging", ignoreCase = true) -> "SMS"
                else -> packageName.substringAfterLast('.')
            }
        }

        try {
            val app = application as? KotibApp ?: return
            val engine = app.container.messageAnalysisEngine

            engine.onNewMessageReceived(
                packageName = packageName,
                appName = appName,
                sender = title,
                title = title,
                content = content,
                messageType = NotificationMessageEntity.TYPE_NOTIFICATION
            )
        } catch (_: Exception) {}
    }
}
