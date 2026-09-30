package com.example.kotib.domain.call

sealed interface CallState {
    data class Incoming(
        val callerName: String = "Kotib",
        val summaryText: String = "Muhim hodisa bo'yicha shoshilinch hisobot",
        val urgencyLevel: Int = 5,
        val attemptCount: Int = 1
    ) : CallState

    data object Connecting : CallState

    data class Active(
        val durationSeconds: Long = 0,
        val isAgentSpeaking: Boolean = false,
        val isUserListening: Boolean = false,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = true,
        val transcript: String = "",
        val agentMessage: String = "Assalomu alaykum! Kotib aloqada, sizni tinglayapman."
    ) : CallState

    data object Ended : CallState
}
