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

class TodoistIntegrationTool(
    private val integrationRepository: IntegrationRepository
) : AgentTool {
    override val name: String = "manage_todoist"
    override val description: String =
        "Todoist vazifalar menejeri: yangi vazifa qo'shish, bugungi vazifalarni ko'rish yoki bajarilgan deb belgilash."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal: 'create_task' (yangi vazifa), 'list_tasks' (vazifalar ro'yxati), 'close_task' (yakunlash)"
            ),
            "content" to mapOf(
                "type" to "STRING",
                "description" to "Vazifa nomi yoki tavsifi"
            ),
            "due_string" to mapOf(
                "type" to "STRING",
                "description" to "Muddati (masalan: 'today', 'tomorrow at 10am', 'every day')"
            ),
            "task_id" to mapOf(
                "type" to "STRING",
                "description" to "Yakunlanishi kerak bo'lgan vazifa ID si"
            )
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val config = integrationRepository.getConfig(IntegrationConfigEntity.SERVICE_TODOIST)
        if (config == null || !config.isEnabled || config.apiKeyOrToken.isBlank()) {
            return@withContext ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Todoist integratsiyasi yoqilmagan yoki API token kiritilmagan."),
                userSummary = "Todoist ulanmagan. Iltimos, Sozlamalar -> Integratsiyalar bo'limidan Todoist API tokenini kiriting."
            )
        }

        val action = args["action"]?.toString() ?: "list_tasks"

        try {
            when (action) {
                "create_task" -> {
                    val content = args["content"]?.toString() ?: "Yangi vazifa"
                    val dueString = args["due_string"]?.toString() ?: "today"

                    val json = JSONObject().apply {
                        put("content", content)
                        put("due_string", dueString)
                    }

                    val reqBody = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://api.todoist.com/rest/v2/tasks")
                        .header("Authorization", "Bearer ${config.apiKeyOrToken.trim()}")
                        .post(reqBody)
                        .build()

                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    val respBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val respJson = JSONObject(respBody)
                        val id = respJson.optString("id")
                        val url = respJson.optString("url")
                        ToolExecutionResult(
                            isSuccess = true,
                            output = mapOf("taskId" to id, "content" to content, "url" to url),
                            userSummary = "Todoist'ga vazifa qo'shildi: '$content' (Muddati: $dueString)"
                        )
                    } else {
                        ToolExecutionResult(
                            isSuccess = false,
                            output = mapOf("error" to respBody),
                            userSummary = "Todoist'da vazifa yaratishda xatolik (${response.code})."
                        )
                    }
                }

                "close_task" -> {
                    val taskId = args["task_id"]?.toString() ?: ""
                    val request = Request.Builder()
                        .url("https://api.todoist.com/rest/v2/tasks/$taskId/close")
                        .header("Authorization", "Bearer ${config.apiKeyOrToken.trim()}")
                        .post("{}".toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        ToolExecutionResult(
                            isSuccess = true,
                            output = mapOf("status" to "closed", "taskId" to taskId),
                            userSummary = "Todoist vazifasi muvaffaqiyatli yakunlandi."
                        )
                    } else {
                        ToolExecutionResult(
                            isSuccess = false,
                            output = mapOf("error" to "Vazifani yakunlab bo'lmadi"),
                            userSummary = "Todoist vazifasini yakunlashda xatolik."
                        )
                    }
                }

                else -> {
                    // list_tasks
                    val request = Request.Builder()
                        .url("https://api.todoist.com/rest/v2/tasks")
                        .header("Authorization", "Bearer ${config.apiKeyOrToken.trim()}")
                        .get()
                        .build()

                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    val respBody = response.body?.string() ?: "[]"

                    if (response.isSuccessful) {
                        val arr = JSONArray(respBody)
                        val tasks = mutableListOf<String>()
                        for (i in 0 until minOf(arr.length(), 6)) {
                            val item = arr.getJSONObject(i)
                            val due = item.optJSONObject("due")?.optString("string") ?: ""
                            tasks.add("• ${item.optString("content")}${if (due.isNotBlank()) " ($due)" else ""}")
                        }
                        ToolExecutionResult(
                            isSuccess = true,
                            output = mapOf("count" to arr.length(), "tasks" to tasks),
                            userSummary = if (tasks.isEmpty()) "Todoist'da faol vazifalar yo'q." else "Todoist vazifalari:\n" + tasks.joinToString("\n")
                        )
                    } else {
                        ToolExecutionResult(
                            isSuccess = false,
                            output = mapOf("error" to respBody),
                            userSummary = "Todoist vazifalarini yuklashda xatolik (${response.code})."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xatolik")),
                userSummary = "Todoist bilan aloqa xatosi: ${e.message}"
            )
        }
    }
}
