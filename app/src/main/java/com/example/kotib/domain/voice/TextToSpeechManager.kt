package com.example.kotib.domain.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isAutoTtsEnabled = MutableStateFlow(true)
    val isAutoTtsEnabled: StateFlow<Boolean> = _isAutoTtsEnabled.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Avval O'zbek tili, agar mavjud bo'lmasa sukut bo'yicha tilni sozlash
            val uzbek = Locale.forLanguageTag("uz-UZ")
            val result = tts?.setLanguage(uzbek)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }

            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun toggleAutoTts() {
        _isAutoTtsEnabled.value = !_isAutoTtsEnabled.value
        if (!_isAutoTtsEnabled.value) {
            stop()
        }
    }

    fun setAutoTtsEnabled(enabled: Boolean) {
        _isAutoTtsEnabled.value = enabled
        if (!enabled) {
            stop()
        }
    }

    fun speak(text: String) {
        if (!isInitialized || text.isBlank()) return
        // Belgilarni tozalash (Markdown yulduzchalari va havola belgilarini)
        val cleanText = text
            .replace(Regex("""[*#_`>\[\]]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "kotib_speech_${System.currentTimeMillis()}")
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
