package com.example.domain.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Initializing : SpeechState
    data class Speaking(val text: String, val utteranceId: String) : SpeechState
    data object Paused : SpeechState
    data class Unavailable(val reason: String) : SpeechState
    data class Error(val message: String) : SpeechState
}

/**
 * Robust, leak-free Text-To-Speech manager targeting Hindi spoken astrology.
 *
 * Ensures:
 * 1. Single TTS instance per manager lifecycle.
 * 2. Strict Hindi Locale enforcement (Locale("hi", "IN") / Locale("hi")).
 * 3. Graceful fallback/unavailable notification without silently playing English or crashing.
 * 4. Proper resource cleanup on dispose.
 */
class JyotirAiSpeechManager(
    private val context: Context,
    private val targetLocale: Locale = Locale("hi", "IN")
) : TextToSpeech.OnInitListener {

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isLanguageAvailable = false
    private var currentTextToSpeak: String? = null
    private var pendingCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        _speechState.value = SpeechState.Initializing
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("Failed to initialize speech engine: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localTts = tts ?: return
            var langResult = localTts.setLanguage(targetLocale)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Try fallback to generic Hindi "hi"
                langResult = localTts.setLanguage(Locale("hi"))
            }

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                isLanguageAvailable = false
                _speechState.value = SpeechState.Unavailable(
                    "Hindi speech engine is not installed on this device. Please install Hindi TTS voice data in Android settings."
                )
            } else {
                isLanguageAvailable = true
                isInitialized = true
                localTts.setPitch(1.0f)
                localTts.setSpeechRate(0.92f) // Slightly relaxed pace for dignified Vedic reading

                localTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        currentTextToSpeak?.let { text ->
                            _speechState.value = SpeechState.Speaking(text, utteranceId ?: "")
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        _speechState.value = SpeechState.Idle
                        pendingCallback?.invoke()
                        pendingCallback = null
                    }

                    override fun onError(utteranceId: String?) {
                        _speechState.value = SpeechState.Error("Audio playback encountered an error")
                        pendingCallback = null
                    }
                })

                _speechState.value = SpeechState.Idle

                // If text was queued before init completed
                currentTextToSpeak?.let { text ->
                    speakInternal(text)
                }
            }
        } else {
            isInitialized = false
            _speechState.value = SpeechState.Error("Text-to-Speech service initialization failed ($status)")
        }
    }

    /**
     * Reads the specified text in Hindi.
     */
    fun speak(text: String, onFinished: () -> Unit = {}) {
        if (text.isBlank()) return
        currentTextToSpeak = text
        pendingCallback = onFinished

        if (!isInitialized) {
            if (_speechState.value is SpeechState.Unavailable) {
                return
            }
            // Will speak once initialized in onInit()
            return
        }

        if (!isLanguageAvailable) {
            _speechState.value = SpeechState.Unavailable(
                "Hindi speech engine is not installed on this device."
            )
            return
        }

        speakInternal(text)
    }

    private fun speakInternal(text: String) {
        val localTts = tts ?: return
        val utteranceId = UUID.randomUUID().toString()
        _speechState.value = SpeechState.Speaking(text, utteranceId)
        localTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Toggles playback: stops if currently speaking, or speaks the provided text.
     */
    fun toggleSpeak(text: String, onFinished: () -> Unit = {}) {
        if (isSpeaking()) {
            stop()
        } else {
            speak(text, onFinished)
        }
    }

    fun isSpeaking(): Boolean {
        return _speechState.value is SpeechState.Speaking
    }

    /**
     * Stops current speech output and resets to Idle.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        currentTextToSpeak = null
        pendingCallback = null
        if (_speechState.value !is SpeechState.Unavailable && _speechState.value !is SpeechState.Error) {
            _speechState.value = SpeechState.Idle
        }
    }

    /**
     * Releases TTS resources.
     */
    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
        _speechState.value = SpeechState.Idle
    }
}
