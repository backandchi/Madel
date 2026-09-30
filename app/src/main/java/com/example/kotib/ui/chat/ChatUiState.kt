package com.example.kotib.ui.chat

import com.example.kotib.data.local.entity.ApiKeyEntity
import com.example.kotib.data.local.entity.ChatMessageEntity
import com.example.kotib.domain.agent.AgentState
import com.example.kotib.domain.voice.SpeechInputState

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val agentState: AgentState = AgentState.Idle,
    val activeKey: ApiKeyEntity? = null,
    val isSpeaking: Boolean = false,
    val isAutoTtsEnabled: Boolean = true,
    val speechInputState: SpeechInputState = SpeechInputState.Idle,
    val isForegroundServiceRunning: Boolean = false,
    val inputText: String = ""
)
