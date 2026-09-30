package com.example.kotib.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Chat xabarlari jadvali.
 * Foydalanuvchi, Kotib va Tool chaqiruvlari saqlanadi.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "model", "tool_call", "tool_result", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String? = null,
    val toolArgs: String? = null,
    val toolResult: String? = null,
    val modelUsed: String? = null,
    val keyLabelUsed: String? = null
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_MODEL = "model"
        const val ROLE_TOOL_CALL = "tool_call"
        const val ROLE_TOOL_RESULT = "tool_result"
        const val ROLE_SYSTEM = "system"
    }
}
