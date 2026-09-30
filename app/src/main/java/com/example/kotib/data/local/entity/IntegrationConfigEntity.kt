package com.example.kotib.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tashqi xizmatlar (Integratsiyalar) konfiguratsiyasi:
 * Telegram Bot, GitHub, Notion, Todoist, Obsidian.
 * Har biri alohida yoqiladi/o'chiriladi, shaxsiy tokeni va sozlamalari saqlanadi.
 */
@Entity(tableName = "integration_configs")
data class IntegrationConfigEntity(
    @PrimaryKey
    val serviceId: String, // "telegram", "github", "notion", "todoist", "obsidian"
    val displayName: String,
    val isEnabled: Boolean = false,
    val apiKeyOrToken: String = "",
    val extraParam1: String = "", // Telegram: chatId, GitHub: defaultRepo, Notion: databaseId, Obsidian: vaultPath
    val extraParam2: String = "", // GitHub: username
    val lastSyncTimestamp: Long = 0,
    val lastStatusMessage: String = "Ulanmagan"
) {
    companion object {
        const val SERVICE_TELEGRAM = "telegram"
        const val SERVICE_GITHUB = "github"
        const val SERVICE_NOTION = "notion"
        const val SERVICE_TODOIST = "todoist"
        const val SERVICE_OBSIDIAN = "obsidian"
    }
}
