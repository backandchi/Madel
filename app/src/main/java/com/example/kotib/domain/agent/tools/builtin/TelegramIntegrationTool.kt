package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.data.api.RetrofitClient
import com.example.kotib.data.local.entity.IntegrationConfigEntity
import com.example.kotib.data.repository.IntegrationRepository
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TelegramIntegrationTool(
    private val integrationRepository: IntegrationRepository
) : AgentTool {
    override val name: String = "send_telegram_message"
    override val description: String =
        "Telegram Bot orqali foydalanuvchiga xabar, hisobot yoki eslatma yuboradi. Telegram integratsiyasi faol bo'lishi kerak."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "text" to mapOf(
                "type" to "STRING",
                "description" to "Telegram orqali jo'natilishi kerak bo'lgan xabar matni"
            ),
            "chat_id" to mapOf(
                "type" to "STRING",
                "description" to "Ixtiyoriy Chat ID (kiritilmasa, sozlamalardagi asosiy chatga jo'natiladi)"
            )
        ),
        "required" to listOf("text")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val config = integrationRepository.getConfig(IntegrationConfigEntity.SERVICE_TELEGRAM)
        if (config == null || !config.isEnabled || config.apiKeyOrToken.isBlank()) {
            return@withContext ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Telegram integratsiyasi yoqilmagan yoki bot tokeni kiritilmagan."),
                userSummary = "Telegram boti ulanmagan. Iltimos, Sozlamalar -> Integratsiyalar bo'limidan Telegram bot tokenini kiriting."
            )
        }

        val text = args["text"]?.toString() ?: ""
        val targetChatId = args["chat_id"]?.toString()?.ifBlank { null }
            ?: config.extraParam1.ifBlank { null }

        if (targetChatId.isNullOrBlank()) {
            return@withContext ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Telegram Chat ID kiritilmagan."),
                userSummary = "Telegram Chat ID sozlanmagan. Iltimos, sozlamalarda Chat ID kiriting."
            )
        }

        try {
            val json = JSONObject().apply {
                put("chat_id", targetChatId)
                put("text", text)
                put("parse_mode", "HTML")
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.telegram.org/bot${config.apiKeyOrToken.trim()}/sendMessage")
                .post(body)
                .build()

            val response = RetrofitClient.okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            val respJson = JSONObject(respBody)

            if (respJson.optBoolean("ok", false)) {
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("status" to "sent", "chatId" to targetChatId),
                    userSummary = "Xabar Telegram orqali muvaffaqiyatli jo'natildi."
                )
            } else {
                val err = respJson.optString("description", "Xatolik yuz berdi")
                ToolExecutionResult(
                    isSuccess = false,
                    output = mapOf("error" to err),
                    userSummary = "Telegram'ga yuborishda xatolik: $err"
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Tarmoq xatosi")),
                userSummary = "Telegram bilan aloqa xatosi: ${e.message}"
            )
        }
    }
}
