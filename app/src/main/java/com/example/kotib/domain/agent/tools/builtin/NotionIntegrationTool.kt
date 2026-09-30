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
import org.json.JSONArray
import org.json.JSONObject

class NotionIntegrationTool(
    private val integrationRepository: IntegrationRepository
) : AgentTool {
    override val name: String = "manage_notion"
    override val description: String =
        "Notion'da yangi sahifa yoki qayd yaratish, ma'lumotlar bazasiga g'oya qo'shish."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "title" to mapOf(
                "type" to "STRING",
                "description" to "Notion sahifasi yoki g'oyaning sarlavhasi"
            ),
            "content" to mapOf(
                "type" to "STRING",
                "description" to "Sahifa matni yoki batafsil mazmuni"
            ),
            "database_id" to mapOf(
                "type" to "STRING",
                "description" to "Ixtiyoriy Notion Database ID (kiritilmasa sozlamalardagisi ishlatiladi)"
            )
        ),
        "required" to listOf("title")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val config = integrationRepository.getConfig(IntegrationConfigEntity.SERVICE_NOTION)
        if (config == null || !config.isEnabled || config.apiKeyOrToken.isBlank()) {
            return@withContext ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Notion integratsiyasi yoqilmagan yoki API token kiritilmagan."),
                userSummary = "Notion integratsiyasi faol emas. Sozlamalardan Notion API tokenini kiriting."
            )
        }

        val title = args["title"]?.toString() ?: "Yangi g'oya"
        val content = args["content"]?.toString() ?: ""
        val dbId = args["database_id"]?.toString()?.ifBlank { null }
            ?: config.extraParam1.ifBlank { null }

        if (dbId.isNullOrBlank()) {
            return@withContext ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Notion Database ID kiritilmagan."),
                userSummary = "Notion Database ID sozlanmagan. Iltimos, Sozlamalarda Database ID kiriting."
            )
        }

        try {
            val json = JSONObject().apply {
                put("parent", JSONObject().put("database_id", dbId.trim()))
                val properties = JSONObject().apply {
                    put("Name", JSONObject().put("title", JSONArray().apply {
                        put(JSONObject().put("text", JSONObject().put("content", title)))
                    }))
                }
                put("properties", properties)

                if (content.isNotBlank()) {
                    val children = JSONArray().apply {
                        put(JSONObject().apply {
                            put("object", "block")
                            put("type", "paragraph")
                            put("paragraph", JSONObject().put("rich_text", JSONArray().apply {
                                put(JSONObject().put("type", "text").put("text", JSONObject().put("content", content)))
                            }))
                        })
                    }
                    put("children", children)
                }
            }

            val reqBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.notion.com/v1/pages")
                .header("Authorization", "Bearer ${config.apiKeyOrToken.trim()}")
                .header("Notion-Version", "2022-06-28")
                .post(reqBody)
                .build()

            val response = RetrofitClient.okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val respJson = JSONObject(respBody)
                val pageUrl = respJson.optString("url")
                ToolExecutionResult(
                    isSuccess = true,
                    output = mapOf("status" to "created", "url" to pageUrl),
                    userSummary = "Notion'ga yangi qayd saqlandi: '$title' ($pageUrl)"
                )
            } else {
                ToolExecutionResult(
                    isSuccess = false,
                    output = mapOf("error" to respBody),
                    userSummary = "Notion'da qayd yaratishda xatolik (${response.code})."
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xatolik")),
                userSummary = "Notion bilan aloqa xatosi: ${e.message}"
            )
        }
    }
}
