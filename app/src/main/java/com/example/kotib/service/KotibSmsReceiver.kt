package com.example.kotib.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.KotibApp
import com.example.kotib.data.local.entity.NotificationMessageEntity

class KotibSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Telephony.Sms.Intents.getMessagesFromIntent(intent)
            } else {
                @Suppress("DEPRECATION")
                val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
                pdus.mapNotNull { pdu ->
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }.toTypedArray()
            }

            if (messages.isNullOrEmpty()) return

            val sender = messages.firstOrNull()?.originatingAddress ?: "SMS"
            val fullBody = messages.joinToString("") { it.messageBody ?: "" }

            val app = context.applicationContext as? KotibApp ?: return
            val engine = app.container.messageAnalysisEngine

            engine.onNewMessageReceived(
                packageName = "com.android.mms",
                appName = "SMS",
                sender = sender,
                title = "Yangi SMS ($sender)",
                content = fullBody,
                messageType = NotificationMessageEntity.TYPE_SMS
            )
        } catch (_: Exception) {}
    }
}
