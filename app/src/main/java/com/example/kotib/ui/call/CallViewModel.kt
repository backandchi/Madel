package com.example.kotib.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotib.KotibContainer
import com.example.kotib.domain.agent.AgentState
import com.example.kotib.domain.call.CallState
import com.example.kotib.domain.voice.SpeechInputState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallViewModel(
    private val container: KotibContainer
) : ViewModel() {

    private val callManager = container.callManager
    private val ttsManager = container.textToSpeechManager
    private val speechManager = container.speechRecognizerManager
    private val agentEngine = container.agentEngine

    private val _callState = MutableStateFlow<CallState>(CallState.Ended)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var durationTimerJob: Job? = null
    private var initialSummary: String = ""
    private var currentAttempt: Int = 1

    fun initCall(isIncoming: Boolean, summary: String = "", urgency: Int = 5, attempt: Int = 1) {
        initialSummary = summary.ifBlank { "Muhim hodisa yuzasidan xabar berilmoqda" }
        currentAttempt = attempt

        if (isIncoming) {
            _callState.value = CallState.Incoming(
                callerName = "Aiko",
                summaryText = initialSummary,
                urgencyLevel = urgency,
                attemptCount = attempt
            )
            callManager.startRinging()
        } else {
            startDirectActiveCall()
        }
    }

    fun answerCall() {
        callManager.stopRinging()
        callManager.setSpeakerphoneOn(true)

        _callState.value = CallState.Active(
            durationSeconds = 0,
            isAgentSpeaking = true,
            isUserListening = false,
            isMuted = false,
            isSpeakerOn = true,
            transcript = "",
            agentMessage = "Assalomu alaykum! Men Aiko'man. $initialSummary. Sizni tinglayapman."
        )

        startDurationTimer()

        // Aiko hisobotni TTS orqali o'qiydi
        speakAndThenListen(
            "Assalomu alaykum! Men Aiko'man. Sizni tinglayapman."
        )
    }

    private fun startDirectActiveCall() {
        callManager.stopRinging()
        callManager.setSpeakerphoneOn(true)

        _callState.value = CallState.Active(
            durationSeconds = 0,
            isAgentSpeaking = true,
            isUserListening = false,
            isMuted = false,
            isSpeakerOn = true,
            transcript = "",
            agentMessage = "Assalomu alaykum! Men Aiko'man, buyrug'ingizni ayting."
        )

        startDurationTimer()

        speakAndThenListen(
            "Assalomu alaykum! Men Aiko'man, sizni tinglayapman."
        )
    }

    /**
     * Ovozli suhbat tsikli: Kotib gapiradi -> keyin mikrofon ochilib foydalanuvchini tinglaydi
     */
    private fun speakAndThenListen(messageToSpeak: String) {
        viewModelScope.launch {
            ttsManager.speak(messageToSpeak)

            // TTS tugashini kutish
            while (ttsManager.isSpeaking.value) {
                delay(200)
            }
            delay(400)

            val current = _callState.value
            if (current is CallState.Active && !current.isMuted) {
                _callState.value = current.copy(
                    isAgentSpeaking = false,
                    isUserListening = true
                )
                startListeningForCommand()
            }
        }
    }

    private fun startListeningForCommand() {
        speechManager.startListening()

        viewModelScope.launch {
            speechManager.speechState.collect { speechState ->
                val current = _callState.value
                if (current !is CallState.Active) return@collect

                when (speechState) {
                    is SpeechInputState.PartialResult -> {
                        _callState.value = current.copy(transcript = speechState.text)
                    }
                    is SpeechInputState.FinalResult -> {
                        val text = speechState.text.trim()
                        if (text.isNotBlank()) {
                            speechManager.resetState()
                            speechManager.stopListening()

                            _callState.value = current.copy(
                                transcript = text,
                                isUserListening = false,
                                isAgentSpeaking = true,
                                agentMessage = "Kotib buyruqni tahlil qilmoqda..."
                            )

                            // Agentga yuborish
                            processVoiceCommand(text)
                        }
                    }
                    is SpeechInputState.Error -> {
                        val currentActive = _callState.value
                        if (currentActive is CallState.Active) {
                            _callState.value = currentActive.copy(
                                isUserListening = false,
                                isAgentSpeaking = false,
                                agentMessage = "Sizni tinglayapman. Ovoz bilan ayting yoki quyidagi tezkor buyruqlardan birini bosing:"
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun processVoiceCommand(command: String) {
        agentEngine.processPrompt(command) { agentResponse ->
            val current = _callState.value
            if (current is CallState.Active) {
                _callState.value = current.copy(
                    agentMessage = agentResponse,
                    isAgentSpeaking = true,
                    isUserListening = false
                )
                speakAndThenListen(agentResponse)
            }
        }
    }

    fun rejectCall() {
        callManager.stopRinging()
        _callState.value = CallState.Ended

        // Agar javob berilmasa: 2 daqiqadan keyin qayta jiringlaydi, 3 urinishdan keyin bildirishnomaga aylanadi
        if (currentAttempt < 3) {
            val nextAttempt = currentAttempt + 1
            viewModelScope.launch {
                delay(120_000L) // 2 daqiqa
                callManager.triggerIncomingCall(initialSummary, 5, nextAttempt)
            }
        } else {
            callManager.showMissedCallNotification(initialSummary)
        }
    }

    fun endCall() {
        durationTimerJob?.cancel()
        callManager.stopRinging()
        ttsManager.stop()
        speechManager.stopListening()
        callManager.setSpeakerphoneOn(false)
        _callState.value = CallState.Ended
    }

    fun toggleMute() {
        val current = _callState.value
        if (current is CallState.Active) {
            val newMute = !current.isMuted
            _callState.value = current.copy(isMuted = newMute)
            if (newMute) {
                speechManager.stopListening()
            } else {
                speechManager.startListening()
            }
        }
    }

    fun toggleSpeaker() {
        val current = _callState.value
        if (current is CallState.Active) {
            val newSpeaker = !current.isSpeakerOn
            _callState.value = current.copy(isSpeakerOn = newSpeaker)
            callManager.setSpeakerphoneOn(newSpeaker)
        }
    }

    fun executeCommandDirect(command: String) {
        val current = _callState.value
        if (current is CallState.Active) {
            speechManager.stopListening()
            _callState.value = current.copy(
                transcript = command,
                isUserListening = false,
                isAgentSpeaking = true,
                agentMessage = "Buyruq bajarilmoqda..."
            )
            processVoiceCommand(command)
        }
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = viewModelScope.launch {
            var seconds = 0L
            while (true) {
                delay(1000L)
                seconds++
                val current = _callState.value
                if (current is CallState.Active) {
                    _callState.value = current.copy(durationSeconds = seconds)
                } else {
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        endCall()
    }

    class Factory(private val container: KotibContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CallViewModel(container) as T
        }
    }
}
