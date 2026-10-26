package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.LanguageDetector
import com.example.data.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _currentStatus = MutableStateFlow("Tap to speak...")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    var isCallActive: Boolean = false
    var isContinuousModeEnabled: Boolean = true
    var isAssistantSpeaking: Boolean = false
    var isMuted: Boolean = false
    var targetLanguage: SupportedLanguage = SupportedLanguage.AUTO

    private var isExplicitlyStopped: Boolean = false

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        handler.post {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                } catch (e: Exception) {
                    Log.e("SpeechManager", "Failed to create SpeechRecognizer: ${e.message}")
                }
            } else {
                _currentStatus.value = "Speech recognition not available on device"
            }
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (!isCallActive || isAssistantSpeaking || isMuted) {
                stopListening()
                return
            }
            _isListening.value = true
            _currentStatus.value = when (targetLanguage) {
                SupportedLanguage.BENGALI -> "শুনছি... বলুন"
                SupportedLanguage.HINDI -> "सुन रहा हूँ... बोलिए"
                else -> "Listening..."
            }
        }

        override fun onBeginningOfSpeech() {
            if (!isCallActive || isAssistantSpeaking || isMuted) return
            _currentStatus.value = when (targetLanguage) {
                SupportedLanguage.BENGALI -> "শুনছি..."
                SupportedLanguage.HINDI -> "सुन रहा हूँ..."
                else -> "Hearing you..."
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (!isCallActive || isAssistantSpeaking || isMuted) {
                _amplitude.value = 0f
                return
            }
            // rmsdB is usually between -2 and 10 dB
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _amplitude.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _currentStatus.value = "Processing speech..."
            _amplitude.value = 0f
        }

        override fun onError(error: Int) {
            _amplitude.value = 0f
            _isListening.value = false
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition engine busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Recognition error ($error)"
            }
            Log.w("SpeechManager", "SpeechRecognizer error: $errorMsg")
            _currentStatus.value = errorMsg

            // Indestructible continuous call loop: as long as call is active, auto re-arm immediately!
            if (isCallActive && !isExplicitlyStopped && !isMuted && !isAssistantSpeaking && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    initRecognizer()
                }
                handler.postDelayed({
                    if (isCallActive && !isExplicitlyStopped && !isMuted && !isAssistantSpeaking) {
                        startListening()
                    }
                }, 300)
            }
        }

        override fun onResults(results: Bundle?) {
            _amplitude.value = 0f
            _isListening.value = false

            // Prevent self-transcription if assistant is speaking
            if (isAssistantSpeaking || !isCallActive) {
                return
            }

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim()
            if (!recognizedText.isNullOrBlank()) {
                _currentStatus.value = "Recognized: $recognizedText"
                onResult(recognizedText)
            } else {
                _currentStatus.value = "শুনছি... বলুন"
                if (isCallActive && !isExplicitlyStopped && !isAssistantSpeaking && !isMuted) {
                    handler.postDelayed({ startListening() }, 500)
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isCallActive || isAssistantSpeaking || isMuted) return
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim()
            if (!partial.isNullOrBlank()) {
                _currentStatus.value = partial
                onPartialResult(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        // STRICT GATE: Microphone NEVER opens unless an active call is established!
        if (!isCallActive) {
            Log.d("SpeechManager", "startListening ignored because call is not active.")
            _isListening.value = false
            _amplitude.value = 0f
            _currentStatus.value = "কল শুরু করতে কল বাটনে চাপুন"
            return
        }
        if (isMuted) {
            _currentStatus.value = "Microphone is muted (মিউট করা)"
            _isListening.value = false
            _amplitude.value = 0f
            return
        }
        if (isAssistantSpeaking) {
            Log.d("SpeechManager", "Assistant is currently speaking; delaying startListening")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _currentStatus.value = "Microphone permission required"
            return
        }

        isExplicitlyStopped = false
        handler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                    // Multilingual configuration (Bengali, Hindi, English)
                    if (targetLanguage == SupportedLanguage.AUTO) {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN", "hi-IN", "en-US"))
                    } else {
                        val localeTag = LanguageDetector.getSpeechLocaleTag(targetLanguage)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                    }
                }
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _currentStatus.value = "Listening..."
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error starting listening: ${e.message}")
                _isListening.value = false
                _currentStatus.value = "Tap to speak..."
            }
        }
    }

    fun stopListening() {
        isExplicitlyStopped = true
        handler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _amplitude.value = 0f
                _currentStatus.value = if (!isCallActive) "কল শুরু করতে কল বাটনে চাপুন" else "Tap to speak..."
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error stopping listening: ${e.message}")
            }
        }
    }

    fun resumeAfterResponse() {
        if (isCallActive && !isExplicitlyStopped && !isMuted && !isAssistantSpeaking) {
            handler.postDelayed({
                if (isCallActive && !isExplicitlyStopped && !isMuted && !isAssistantSpeaking) {
                    startListening()
                }
            }, 500)
        }
    }

    fun destroy() {
        isExplicitlyStopped = true
        handler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error destroying SpeechRecognizer: ${e.message}")
            }
        }
    }
}
