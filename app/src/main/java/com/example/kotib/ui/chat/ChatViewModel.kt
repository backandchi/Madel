package com.example.kotib.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotib.KotibContainer
import com.example.kotib.domain.agent.AgentEngine
import com.example.kotib.domain.voice.SpeechInputState
import com.example.kotib.service.KotibForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val container: KotibContainer,
    private val context: Context
) : ViewModel() {

    private val chatRepository = container.chatRepository
    private val apiKeyRepository = container.apiKeyRepository
    private val agentEngine = container.agentEngine
    private val speechRecognizerManager = container.speechRecognizerManager
    private val textToSpeechManager = container.textToSpeechManager

    private val _inputText = MutableStateFlow("")
    private val _isServiceRunning = MutableStateFlow(KotibForegroundService.isServiceRunning)

    private val baseStateFlow = combine(
        chatRepository.messages,
        agentEngine.agentState,
        apiKeyRepository.allKeys
    ) { messages, agentState, keys ->
        val active = keys.firstOrNull { it.isActive } ?: keys.firstOrNull()
        Triple(messages, agentState, active)
    }

    private val voiceStateFlow = combine(
        textToSpeechManager.isSpeaking,
        textToSpeechManager.isAutoTtsEnabled,
        speechRecognizerManager.speechState
    ) { isSpeaking, isAutoTts, speechState ->
        Triple(isSpeaking, isAutoTts, speechState)
    }

    val uiState: StateFlow<ChatUiState> = combine(
        baseStateFlow,
        voiceStateFlow,
        _inputText,
        _isServiceRunning
    ) { (messages, agentState, activeKey), (isSpeaking, isAutoTts, speechState), input, isServiceRunning ->
        // Agar ovoz orqali yangi yakuniy natija olingan bo'lsa, avtomatik inputga qo'yish
        if (speechState is SpeechInputState.FinalResult && speechState.text.isNotBlank()) {
            _inputText.value = speechState.text
            speechRecognizerManager.resetState()
        } else if (speechState is SpeechInputState.PartialResult) {
            _inputText.value = speechState.text
        }

        ChatUiState(
            messages = messages,
            agentState = agentState,
            activeKey = activeKey,
            isSpeaking = isSpeaking,
            isAutoTtsEnabled = isAutoTts,
            speechInputState = speechState,
            isForegroundServiceRunning = isServiceRunning,
            inputText = input
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        _inputText.value = ""

        agentEngine.processPrompt(text) { responseText ->
            if (textToSpeechManager.isAutoTtsEnabled.value) {
                textToSpeechManager.speak(responseText)
            }
        }
    }

    fun startVoiceInput() {
        speechRecognizerManager.startListening()
    }

    fun stopVoiceInput() {
        speechRecognizerManager.stopListening()
    }

    fun toggleAutoTts() {
        textToSpeechManager.toggleAutoTts()
    }

    fun speakText(text: String) {
        textToSpeechManager.speak(text)
    }

    fun stopSpeaking() {
        textToSpeechManager.stop()
    }

    fun emergencyStop() {
        agentEngine.emergencyStop()
        textToSpeechManager.stop()
        speechRecognizerManager.stopListening()
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }

    fun toggleForegroundService() {
        if (_isServiceRunning.value) {
            KotibForegroundService.stop(context)
            _isServiceRunning.value = false
        } else {
            KotibForegroundService.start(context)
            _isServiceRunning.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.stopListening()
        textToSpeechManager.stop()
    }

    class Factory(
        private val container: KotibContainer,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(container, context) as T
        }
    }
}
