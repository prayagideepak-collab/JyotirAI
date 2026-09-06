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
    data class Speaking(val text: String, val utteranceId: String, val activeSegmentIndex: Int = -1) : SpeechState
    data object Paused : SpeechState
    data class Unavailable(val reason: String) : SpeechState
    data class Error(val message: String) : SpeechState
}

data class SpeechSegment(
    val index: Int,
    val text: String
)

/**
 * Robust, leak-free Text-To-Speech manager targeting Hindi spoken astrology.
 *
 * Ensures:
 * 1. Single active narration globally across the app (stops any other active screen/manager when new speech starts).
 * 2. Strict Hindi Locale enforcement (Locale("hi", "IN") / Locale("hi")).
 * 3. Segment breakdown and live-read highlighting progression.
 * 4. Graceful pause, resume, stop, and resource cleanup.
 */
class JyotirAiSpeechManager(
    private val context: Context,
    private val targetLocale: Locale = Locale.forLanguageTag("hi-IN")
) : TextToSpeech.OnInitListener {

    companion object {
        private val activeManagers = mutableSetOf<JyotirAiSpeechManager>()
    }

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _activeSegmentIndex = MutableStateFlow<Int>(-1)
    val activeSegmentIndex: StateFlow<Int> = _activeSegmentIndex.asStateFlow()

    private val _segments = MutableStateFlow<List<SpeechSegment>>(emptyList())
    val segments: StateFlow<List<SpeechSegment>> = _segments.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isLanguageAvailable = false
    private var currentTextToSpeak: String? = null
    private var pendingCallback: (() -> Unit)? = null
    private var currentSegmentIndex = 0

    init {
        synchronized(activeManagers) {
            activeManagers.add(this)
        }
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
                langResult = localTts.setLanguage(Locale.forLanguageTag("hi"))
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
                localTts.setSpeechRate(0.92f) // Relaxed pace for Vedic reading

                localTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        currentTextToSpeak?.let { text ->
                            _speechState.value = SpeechState.Speaking(text, utteranceId ?: "", currentSegmentIndex)
                            _activeSegmentIndex.value = currentSegmentIndex
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        val segList = _segments.value
                        if (segList.isNotEmpty() && currentSegmentIndex < segList.size - 1) {
                            // Speak next segment sequentially for continuous live read highlighting
                            currentSegmentIndex++
                            speakSegment(currentSegmentIndex)
                        } else {
                            _speechState.value = SpeechState.Idle
                            _activeSegmentIndex.value = -1
                            pendingCallback?.invoke()
                            pendingCallback = null
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        _speechState.value = SpeechState.Error("Audio playback encountered an error")
                        _activeSegmentIndex.value = -1
                        pendingCallback = null
                    }
                })

                _speechState.value = SpeechState.Idle
            }
        } else {
            isInitialized = false
            _speechState.value = SpeechState.Error("Text-to-Speech service initialization failed ($status)")
        }
    }

    /**
     * Splits text into stable sentence/paragraph segments for live read highlighting.
     */
    fun prepareSegments(text: String) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) {
            _segments.value = emptyList()
            return
        }
        // Split by Hindi full stop (।), English period (.), or newlines
        val rawParts = cleaned.split(Regex("[।.\n]+")).map { it.trim() }.filter { it.isNotBlank() }
        val segs = rawParts.mapIndexed { index, part ->
            SpeechSegment(index = index, text = part + if (part.endsWith("।") || part.endsWith(".")) "" else "।")
        }
        _segments.value = segs.ifEmpty { listOf(SpeechSegment(0, cleaned)) }
    }

    /**
     * Reads the specified text in Hindi with live-read segmentation.
     */
    fun speak(text: String, onFinished: () -> Unit = {}) {
        if (text.isBlank()) return

        // Enforce Single Active TTS Rule globally across all screens/managers
        synchronized(activeManagers) {
            for (manager in activeManagers) {
                if (manager !== this) {
                    manager.stopSilently()
                }
            }
        }

        prepareSegments(text)
        currentTextToSpeak = text
        pendingCallback = onFinished
        currentSegmentIndex = 0

        if (!isInitialized) {
            if (_speechState.value is SpeechState.Unavailable) return
            return
        }

        if (!isLanguageAvailable) {
            _speechState.value = SpeechState.Unavailable("Hindi speech engine is not installed on this device.")
            return
        }

        speakSegment(0)
    }

    private fun speakSegment(index: Int) {
        val localTts = tts ?: return
        val segs = _segments.value
        if (index !in segs.indices) {
            stop()
            return
        }
        currentSegmentIndex = index
        val segment = segs[index]
        val utteranceId = "SEG-${index}-${UUID.randomUUID().toString().take(6)}"
        _activeSegmentIndex.value = index
        _speechState.value = SpeechState.Speaking(segment.text, utteranceId, index)
        localTts.speak(segment.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Pauses current speech (where supported) or stops safely.
     */
    fun pause() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _speechState.value = SpeechState.Paused
    }

    /**
     * Resumes speech from current segment index.
     */
    fun resume() {
        if (_speechState.value is SpeechState.Paused) {
            speakSegment(currentSegmentIndex)
        } else if (currentTextToSpeak != null) {
            speak(currentTextToSpeak!!)
        }
    }

    /**
     * Toggles playback: stops if currently speaking, or speaks the provided text.
     */
    fun toggleSpeak(text: String, onFinished: () -> Unit = {}) {
        if (isSpeaking() || _speechState.value is SpeechState.Paused) {
            stop()
        } else {
            speak(text, onFinished)
        }
    }

    fun isSpeaking(): Boolean {
        return _speechState.value is SpeechState.Speaking
    }

    internal fun stopSilently() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _activeSegmentIndex.value = -1
        if (_speechState.value !is SpeechState.Unavailable && _speechState.value !is SpeechState.Error) {
            _speechState.value = SpeechState.Idle
        }
    }

    /**
     * Stops current speech output and resets to Idle.
     */
    fun stop() {
        stopSilently()
        currentTextToSpeak = null
        pendingCallback = null
        currentSegmentIndex = 0
        _segments.value = emptyList()
    }

    /**
     * Releases TTS resources.
     */
    fun release() {
        stop()
        synchronized(activeManagers) {
            activeManagers.remove(this)
        }
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
        _speechState.value = SpeechState.Idle
    }
}
