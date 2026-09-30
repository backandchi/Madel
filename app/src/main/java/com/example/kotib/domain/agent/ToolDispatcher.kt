package com.example.kotib.domain.agent

import com.example.kotib.data.api.ToolDeclaration
import com.example.kotib.domain.agent.tools.AgentTool
import com.example.kotib.domain.agent.tools.ToolExecutionResult
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

class ToolDispatcher(
    private val tools: List<AgentTool>
) {
    private val toolMap = tools.associateBy { it.name }

    /**
     * Gemini REST API uchun 'tools' JSON massivini tuzish
     */
    fun buildGeminiToolsJson(): JSONArray {
        val toolsArray = JSONArray()
        val toolContainer = JSONObject()
        val funcDeclarations = JSONArray()

        tools.forEach { tool ->
            val decl = ToolDeclaration(
                name = tool.name,
                description = tool.description,
                parameters = tool.parametersSchema
            )
            funcDeclarations.put(decl.toJson())
        }

        toolContainer.put("functionDeclarations", funcDeclarations)
        toolsArray.put(toolContainer)
        return toolsArray
    }

    fun getTool(name: String): AgentTool? = toolMap[name]

    /**
     * Vositani xavfsiz va timeout (15 sekund) bilan ishga tushirish
     */
    suspend fun executeTool(name: String, args: Map<String, Any?>): ToolExecutionResult {
        val tool = toolMap[name]
            ?: return ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to "Vosita topilmadi: $name"),
                userSummary = "Vosita topilmadi: $name"
            )

        return try {
            withTimeout(15000L) {
                tool.execute(args)
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                output = mapOf("error" to (e.message ?: "Noma'lum xatolik")),
                userSummary = "'$name' vositasini bajarishda xatolik: ${e.message}"
            )
        }
    }
}
