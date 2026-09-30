package com.example.kotib.domain.agent.tools.builtin

import com.example.kotib.data.repository.ApiKeyRepository
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult

class KeyStatusTool(private val apiKeyRepository: ApiKeyRepository) : AgentTool {
    override val name: String = "check_key_status"
    override val description: String =
        "Gemini API kalitlarining hozirgi holati, faol kalit nomi, kunlik hisoblagich va zaxira holatini tekshiradi."

    override val parametersSchema: Map<String, Any?> = mapOf(
        "type" to "OBJECT",
        "properties" to emptyMap<String, Any>(),
        "required" to emptyList<String>()
    )

    override val requiresConfirmation: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val keys = apiKeyRepository.getAllKeysDirect()
        val active = apiKeyRepository.getActiveKey()

        val keysInfo = keys.map {
            mapOf(
                "label" to it.label,
                "status" to it.status,
                "dailyUsage" to it.dailyUsageCount,
                "isActive" to (it.id == active?.id)
            )
        }

        val summary = "Jami ${keys.size} ta kalit mavjud. Hozirgi faol kalit: '${active?.label ?: "Mavjud emas"}' (Bugungi so'rovlar: ${active?.dailyUsageCount ?: 0})."

        return ToolExecutionResult(
            isSuccess = true,
            output = mapOf(
                "totalKeys" to keys.size,
                "activeKeyLabel" to (active?.label ?: "None"),
                "keys" to keysInfo
            ),
            userSummary = summary
        )
    }
}
