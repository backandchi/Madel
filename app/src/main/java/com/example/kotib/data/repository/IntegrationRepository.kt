package com.example.kotib.data.repository

import com.example.kotib.data.api.RetrofitClient
import com.example.kotib.data.local.dao.IntegrationConfigDao
import com.example.kotib.data.local.entity.IntegrationConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class IntegrationRepository(
    private val dao: IntegrationConfigDao
) {
    val allConfigs: Flow<List<IntegrationConfigEntity>> = dao.getAllConfigsFlow()

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        val defaults = listOf(
            IntegrationConfigEntity(
                serviceId = IntegrationConfigEntity.SERVICE_TELEGRAM,
                displayName = "Telegram Bot",
                lastStatusMessage = "Bot tokeni kiritilmagan"
            ),
            IntegrationConfigEntity(
                serviceId = IntegrationConfigEntity.SERVICE_GITHUB,
                displayName = "GitHub",
                lastStatusMessage = "Personal Access Token kiritilmagan"
            ),
            IntegrationConfigEntity(
                serviceId = IntegrationConfigEntity.SERVICE_NOTION,
                displayName = "Notion",
                lastStatusMessage = "API Token yoki Database ID kiritilmagan"
            ),
            IntegrationConfigEntity(
                serviceId = IntegrationConfigEntity.SERVICE_TODOIST,
                displayName = "Todoist",
                lastStatusMessage = "API Token kiritilmagan"
            ),
            IntegrationConfigEntity(
                serviceId = IntegrationConfigEntity.SERVICE_OBSIDIAN,
                displayName = "Obsidian (Lokal Vault)",
                extraParam1 = "KotibVault",
                lastStatusMessage = "Lokal xotira tayyor"
            )
        )

        defaults.forEach { def ->
            if (dao.getConfigById(def.serviceId) == null) {
                dao.insertOrUpdate(def)
            }
        }
    }

    suspend fun getConfig(serviceId: String): IntegrationConfigEntity? = withContext(Dispatchers.IO) {
        dao.getConfigById(serviceId)
    }

    suspend fun saveConfig(config: IntegrationConfigEntity) = withContext(Dispatchers.IO) {
        dao.insertOrUpdate(config)
    }

    suspend fun toggleEnabled(serviceId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.setEnabled(serviceId, enabled)
    }

    /**
     * Integratsiya ulanishini sinab ko'rish (Test Ping)
     */
    suspend fun testConnection(serviceId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val config = dao.getConfigById(serviceId)
            ?: return@withContext Pair(false, "Xizmat topilmadi")

        if (config.apiKeyOrToken.isBlank() && serviceId != IntegrationConfigEntity.SERVICE_OBSIDIAN) {
            return@withContext Pair(false, "API kalit yoki token kiritilmagan")
        }

        try {
            when (serviceId) {
                IntegrationConfigEntity.SERVICE_TELEGRAM -> {
                    // https://api.telegram.org/bot<token>/getMe
                    val request = Request.Builder()
                        .url("https://api.telegram.org/bot${config.apiKeyOrToken.trim()}/getMe")
                        .build()
                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.optBoolean("ok", false)) {
                        val botName = json.getJSONObject("result").optString("first_name", "Bot")
                        val username = json.getJSONObject("result").optString("username", "")
                        val msg = "Ulandi: @$username ($botName)"
                        dao.insertOrUpdate(config.copy(lastSyncTimestamp = System.currentTimeMillis(), lastStatusMessage = msg))
                        Pair(true, msg)
                    } else {
                        val desc = json.optString("description", "Noto'g'ri token")
                        Pair(false, desc)
                    }
                }

                IntegrationConfigEntity.SERVICE_GITHUB -> {
                    // https://api.github.com/user
                    val request = Request.Builder()
                        .url("https://api.github.com/user")
                        .header("Authorization", "token ${config.apiKeyOrToken.trim()}")
                        .header("User-Agent", "Kotib-Android-App")
                        .build()
                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val login = json.optString("login", "Foydalanuvchi")
                        val msg = "Ulandi: GitHub @$login"
                        dao.insertOrUpdate(config.copy(lastSyncTimestamp = System.currentTimeMillis(), lastStatusMessage = msg))
                        Pair(true, msg)
                    } else {
                        Pair(false, "GitHub xatosi: ${response.code} (Token noto'g'ri)")
                    }
                }

                IntegrationConfigEntity.SERVICE_TODOIST -> {
                    // https://api.todoist.com/rest/v2/projects
                    val request = Request.Builder()
                        .url("https://api.todoist.com/rest/v2/projects")
                        .header("Authorization", "Bearer ${config.apiKeyOrToken.trim()}")
                        .build()
                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val msg = "Ulandi: Todoist hisobi faol"
                        dao.insertOrUpdate(config.copy(lastSyncTimestamp = System.currentTimeMillis(), lastStatusMessage = msg))
                        Pair(true, msg)
                    } else {
                        Pair(false, "Todoist xatosi: ${response.code}")
                    }
                }

                IntegrationConfigEntity.SERVICE_NOTION -> {
                    // https://api.notion.com/v1/users/me
                    val request = Request.Builder()
                        .url("https://api.notion.com/v1/users/me")
                        .header("Authorization", "Bearer ${config.apiKeyOrToken.trim()}")
                        .header("Notion-Version", "2022-06-28")
                        .build()
                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val msg = "Ulandi: Notion integratsiyasi faol"
                        dao.insertOrUpdate(config.copy(lastSyncTimestamp = System.currentTimeMillis(), lastStatusMessage = msg))
                        Pair(true, msg)
                    } else {
                        Pair(false, "Notion xatosi: ${response.code}")
                    }
                }

                IntegrationConfigEntity.SERVICE_OBSIDIAN -> {
                    val msg = "Lokal Vault tayyor: ${config.extraParam1.ifBlank { "KotibVault" }}"
                    dao.insertOrUpdate(config.copy(lastSyncTimestamp = System.currentTimeMillis(), lastStatusMessage = msg))
                    Pair(true, msg)
                }

                else -> Pair(false, "Noma'lum xizmat")
            }
        } catch (e: Exception) {
            Pair(false, "Aloqa xatosi: ${e.message}")
        }
    }
}
