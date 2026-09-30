package com.example.kotib.data.repository

import com.example.kotib.data.local.dao.ChatMessageDao
import com.example.kotib.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatMessageDao: ChatMessageDao
) {
    val messages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessagesFlow()

    suspend fun getRecentMessages(limit: Int = 20): List<ChatMessageEntity> {
        return chatMessageDao.getRecentMessages(limit).reversed()
    }

    suspend fun addUserMessage(text: String): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                role = ChatMessageEntity.ROLE_USER,
                content = text
            )
        )
    }

    suspend fun addModelMessage(text: String, model: String?, keyLabel: String?): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                role = ChatMessageEntity.ROLE_MODEL,
                content = text,
                modelUsed = model,
                keyLabelUsed = keyLabel
            )
        )
    }

    suspend fun addToolCallMessage(toolName: String, argsJson: String): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                role = ChatMessageEntity.ROLE_TOOL_CALL,
                content = "Vosita chaqirilmoqda: $toolName",
                toolName = toolName,
                toolArgs = argsJson
            )
        )
    }

    suspend fun addToolResultMessage(toolName: String, resultSummary: String, rawResultJson: String): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                role = ChatMessageEntity.ROLE_TOOL_RESULT,
                content = resultSummary,
                toolName = toolName,
                toolResult = rawResultJson
            )
        )
    }

    suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }
}
