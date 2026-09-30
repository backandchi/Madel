package com.example.kotib.data.repository

import com.example.kotib.data.api.RetrofitClient
import com.example.kotib.data.local.dao.AgentActionLogDao
import com.example.kotib.data.local.dao.ApiKeyDao
import com.example.kotib.data.local.entity.AgentActionLogEntity
import com.example.kotib.data.local.entity.ApiKeyEntity
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApiKeyRepository(
    private val apiKeyDao: ApiKeyDao,
    private val actionLogDao: AgentActionLogDao
) {
    val allKeys: Flow<List<ApiKeyEntity>> = apiKeyDao.getAllKeysFlow()

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Boshlang'ich tekshirish: agar kalitlar bo'lmasa, BuildConfig kalitini yoki zaxira kalitni qo'shadi
     */
    suspend fun initializeDefaultKeysIfEmpty(buildConfigKey: String?) {
        val today = getTodayDate()
        apiKeyDao.resetDailyCountsIfNewDay(today)

        val count = apiKeyDao.getKeyCount()
        if (count == 0) {
            val keyToUse = if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                buildConfigKey
            } else {
                ""
            }

            if (keyToUse.isNotBlank()) {
                apiKeyDao.insertKey(
                    ApiKeyEntity(
                        key = keyToUse,
                        label = "Birlamchi Kalit (Asosiy)",
                        isActive = true,
                        isDefault = true,
                        lastUsedDate = today,
                        status = ApiKeyEntity.STATUS_ACTIVE
                    )
                )
            }
        }
    }

    suspend fun getAllKeysDirect(): List<ApiKeyEntity> {
        val today = getTodayDate()
        apiKeyDao.resetDailyCountsIfNewDay(today)
        return apiKeyDao.getAllKeys()
    }

    /**
     * Hozirgi faol va yaroqli kalitni olish
     */
    suspend fun getActiveKey(): ApiKeyEntity? {
        val today = getTodayDate()
        apiKeyDao.resetDailyCountsIfNewDay(today)

        val activeList = apiKeyDao.getActiveKeys()
        return activeList.firstOrNull { it.isActive } ?: activeList.firstOrNull()
    }

    /**
     * 429 (Rate Limit / Quota) xatoligi yuz berganda avtomatik keyingi kalitga o'tish
     */
    suspend fun rotateOnRateLimit(failedKeyId: Long, errorMessage: String): ApiKeyEntity? {
        val failedKey = apiKeyDao.getKeyById(failedKeyId)
        if (failedKey != null) {
            val updated = failedKey.copy(
                status = ApiKeyEntity.STATUS_RATE_LIMITED,
                lastError = errorMessage
            )
            apiKeyDao.updateKey(updated)

            actionLogDao.insertLog(
                AgentActionLogEntity(
                    actionType = AgentActionLogEntity.TYPE_KEY_ROTATION,
                    description = "429 Quota xatosi: '${failedKey.label}' kaliti vaqtincha cheklandi. Keyingisiga o'tilmoqda.",
                    isSuccess = false
                )
            )
        }

        // Keyingi faol kalitni topish
        val nextKey = apiKeyDao.getActiveKeys().firstOrNull { it.id != failedKeyId }
        if (nextKey != null) {
            actionLogDao.insertLog(
                AgentActionLogEntity(
                    actionType = AgentActionLogEntity.TYPE_KEY_ROTATION,
                    description = "Yangi faol kalit tanlandi: '${nextKey.label}'",
                    isSuccess = true
                )
            )
        } else {
            actionLogDao.insertLog(
                AgentActionLogEntity(
                    actionType = AgentActionLogEntity.TYPE_KEY_ROTATION,
                    description = "Barcha asosiy kalitlar limitga yetdi! Zaxira (Flash-Lite) modeliga o'tilmoqda.",
                    isSuccess = false
                )
            )
        }
        return nextKey
    }

    /**
     * Kalit muvaffaqiyatli ishlatilganda kunlik hisoblagichni oshirish
     */
    suspend fun recordKeyUsage(keyId: Long) {
        val today = getTodayDate()
        val key = apiKeyDao.getKeyById(keyId) ?: return
        val newCount = if (key.lastUsedDate == today) key.dailyUsageCount + 1 else 1
        apiKeyDao.updateKey(
            key.copy(
                dailyUsageCount = newCount,
                lastUsedDate = today,
                status = ApiKeyEntity.STATUS_ACTIVE,
                lastError = null
            )
        )
    }

    suspend fun addKey(key: String, label: String): Long {
        val cleanKey = key.trim()
        val cleanLabel = if (label.isBlank()) "Kalit #${System.currentTimeMillis() % 1000}" else label.trim()
        val id = apiKeyDao.insertKey(
            ApiKeyEntity(
                key = cleanKey,
                label = cleanLabel,
                isActive = true,
                status = ApiKeyEntity.STATUS_ACTIVE,
                lastUsedDate = getTodayDate()
            )
        )
        actionLogDao.insertLog(
            AgentActionLogEntity(
                actionType = AgentActionLogEntity.TYPE_KEY_ROTATION,
                description = "Yangi kalit qo'shildi: '$cleanLabel'",
                isSuccess = true
            )
        )
        return id
    }

    suspend fun deleteKey(id: Long) {
        val key = apiKeyDao.getKeyById(id)
        apiKeyDao.deleteKeyById(id)
        if (key != null) {
            actionLogDao.insertLog(
                AgentActionLogEntity(
                    actionType = AgentActionLogEntity.TYPE_KEY_ROTATION,
                    description = "Kalit o'chirildi: '${key.label}'",
                    isSuccess = true
                )
            )
        }
    }

    suspend fun setActiveKey(id: Long) {
        val all = apiKeyDao.getAllKeys()
        all.forEach {
            apiKeyDao.updateKey(it.copy(isActive = (it.id == id)))
        }
    }

    suspend fun reactivateAllKeys() {
        apiKeyDao.reactivateRateLimitedKeys()
        actionLogDao.insertLog(
            AgentActionLogEntity(
                actionType = AgentActionLogEntity.TYPE_KEY_ROTATION,
                description = "Barcha 429 xatodagi kalitlar qayta faollashtirildi",
                isSuccess = true
            )
        )
    }

    /**
     * Kalitni tekshirish (test ping so'rovi)
     */
    suspend fun testKey(key: String): Pair<Boolean, String> {
        return try {
            val pingJson = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Salom, test"))
                        })
                    })
                }
                put("contents", contentsArr)
            }
            val body = pingJson.toString().toRequestBody("application/json".toMediaType())
            val response = RetrofitClient.api.generateContent(
                model = "gemini-3.5-flash",
                apiKey = key,
                requestBody = body
            )

            if (response.isSuccessful) {
                Pair(true, "Kalit to'g'ri ishlamoqda (200 OK)")
            } else {
                val code = response.code()
                val err = response.errorBody()?.string() ?: ""
                val msg = when (code) {
                    429 -> "429: Limit / Quota tugagan"
                    400, 403 -> "Xato: Kalit yaroqsiz yoki ruxsat yo'q ($code)"
                    else -> "Xatolik kodi: $code"
                }
                Pair(false, msg)
            }
        } catch (e: Exception) {
            Pair(false, "Tarmoq xatosi: ${e.message}")
        }
    }
}
