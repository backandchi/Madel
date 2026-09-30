package com.example.kotib.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Xabarlar markazi jadvali:
 * NotificationListenerService va SMS Receiver orqali kelgan barcha xabarlar saqlanadi.
 * Gemini har biriga 1-5 muhimlik darajasi, xulosa va tavsiya etilgan amal beradi.
 */
@Entity(tableName = "incoming_messages")
data class NotificationMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String, // "Telegram", "WhatsApp", "SMS", "Instagram", etc.
    val sender: String,
    val title: String,
    val rawContent: String,
    val cleanContent: String, // Maskalangan xavfsiz matn
    val timestamp: Long = System.currentTimeMillis(),
    val urgencyLevel: Int = 1, // 1 dan 5 gacha
    val summary: String = "", // Gemini bergan qisqa xulosa
    val suggestedAction: String = "", // e.g. "Darhol javob yozish", "Qo'ng'iroq qilish", "E'tiborsiz qoldirish"
    val isImportant: Boolean = false, // VIP kontakt yoki daraja >= 4
    val isProcessed: Boolean = false,
    val messageType: String = TYPE_NOTIFICATION // "NOTIFICATION" yoki "SMS"
) {
    companion object {
        const val TYPE_NOTIFICATION = "NOTIFICATION"
        const val TYPE_SMS = "SMS"
    }
}
