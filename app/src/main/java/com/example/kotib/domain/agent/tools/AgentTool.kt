package com.example.kotib.domain.agent.tools

/**
 * Kotib agentining har bir vositasi (tool) uchun umumiy interfeys
 */
interface AgentTool {
    val name: String
    val description: String
    val parametersSchema: Map<String, Any?>
    val requiresConfirmation: Boolean

    suspend fun execute(args: Map<String, Any?>): ToolExecutionResult
}

data class ToolExecutionResult(
    val isSuccess: Boolean,
    val output: Map<String, Any?>,
    val userSummary: String
)
