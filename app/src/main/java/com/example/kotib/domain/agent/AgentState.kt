package com.example.kotib.domain.agent

sealed interface AgentState {
    data object Idle : AgentState

    data class Thinking(val statusText: String = "Kotib o'ylanmoqda...") : AgentState

    data class ExecutingTool(
        val toolName: String,
        val args: Map<String, Any?>
    ) : AgentState

    data class RotatingKey(
        val oldLabel: String,
        val newLabel: String,
        val reason: String
    ) : AgentState

    data class WaitingConfirmation(
        val toolName: String,
        val description: String,
        val args: Map<String, Any?>,
        val onConfirm: suspend () -> Unit,
        val onCancel: () -> Unit
    ) : AgentState

    data object Speaking : AgentState

    data class Error(val message: String) : AgentState
}
