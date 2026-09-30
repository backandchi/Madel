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

class GitHubIntegrationTool(
    private val integrationRepository: IntegrationRepository
) : AgentTool {
    override val name: String = "manage_github"
    override val description: String =
        "GitHub bilan ishlash: repository'da issue ochish, issue'lar ro'yxatini ko'rish yoki bildirishnomalarni tekshirish."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "STRING",
                "description" to "Amal turi: 'create_issue' (yangi issue yaratish) yoki 'list_issues' (mavjud issue'larni o'qish)"
            ),
            "repo" to mapOf(
                "type" to "STRING",
                "description" to "Repozitoriy nomi 'owner/repo' (masalan: 'octocat/Hello-World')"
            ),
            "title" to mapOf(
                "type" to "STRING",
                "description" to "Issue sarlavhasi (create_issue uchun majburiy)"
            ),
            "body" to mapOf(
                "type" to "STRING",
                "description" to "Issue matni yoki tavsifi"
            )
        ),
        "required" to listOf("action")
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val config = integrationRepository.getConfig(IntegrationConfigEntity.SERVICE_GITHUB)
        if (config == null || !config.isEnabled || config.apiKeyOrToken.isBlank()) {
            return@withContext ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "GitHub integratsiyasi yoqilmagan yoki token kiritilmagan."),
                userSummary = "GitHub integratsiyasi faol emas. Sozlamalardan GitHub Personal Access Tokenini kiriting."
            )
        }

        val action = args["action"]?.toString() ?: "list_issues"
        val repo = args["repo"]?.toString()?.ifBlank { null }
            ?: config.extraParam1.ifBlank { null }

        if (repo.isNullOrBlank()) {
            return@withContext ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Repozitoriy ko'rsatilmadi (masalan: 'foydalanuvchi/loyiha')."),
                userSummary = "Iltimos, GitHub repozitoriyasini ko'rsating (masalan: 'username/reponame')."
            )
        }

        try {
            when (action) {
                "create_issue" -> {
                    val title = args["title"]?.toString() ?: "Kotib AI Issue"
                    val bodyText = args["body"]?.toString() ?: ""

                    val json = JSONObject().apply {
                        put("title", title)
                        put("body", bodyText)
                    }

                    val reqBody = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/$repo/issues")
                        .header("Authorization", "token ${config.apiKeyOrToken.trim()}")
                        .header("User-Agent", "Kotib-Android-App")
                        .post(reqBody)
                        .build()

                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val respJson = JSONObject(respBody)
                        val issueNumber = respJson.optInt("number")
                        val htmlUrl = respJson.optString("html_url")
                        ToolExecutionResult(
                            isSuccess = true,
                            output = mapOf("issueNumber" to issueNumber, "url" to htmlUrl),
                            userSummary = "GitHub'da #$issueNumber raqamli issue ochildi: '$title' ($htmlUrl)"
                        )
                    } else {
                        ToolExecutionResult(
                            isSuccess = false,
                            output = mapOf("error" to respBody),
                            userSummary = "GitHub'da issue ochishda xatolik yuz berdi (${response.code})."
                        )
                    }
                }

                else -> {
                    // list_issues
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/$repo/issues?state=open&per_page=5")
                        .header("Authorization", "token ${config.apiKeyOrToken.trim()}")
                        .header("User-Agent", "Kotib-Android-App")
                        .get()
                        .build()

                    val response = RetrofitClient.okHttpClient.newCall(request).execute()
                    val respBody = response.body?.string() ?: "[]"
                    if (response.isSuccessful) {
                        val arr = JSONArray(respBody)
                        val issues = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            issues.add("#${item.optInt("number")}: ${item.optString("title")}")
                        }
                        ToolExecutionResult(
                            isSuccess = true,
                            output = mapOf("count" to issues.size, "issues" to issues),
                            userSummary = if (issues.isEmpty()) "Ochiq issue'lar topilmadi." else issues.joinToString("\n")
                        )
                    } else {
                        ToolExecutionResult(
                            isSuccess = false,
                            output = mapOf("error" to respBody),
                            userSummary = "GitHub issue'larini yuklashda xatolik (${response.code})."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Xatolik")),
                userSummary = "GitHub bilan aloqa xatosi: ${e.message}"
            )
        }
    }
}
