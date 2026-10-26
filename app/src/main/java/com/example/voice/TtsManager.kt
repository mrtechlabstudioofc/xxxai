package com.example.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.R
import com.example.data.model.LanguageDetector
import com.example.data.model.SupportedLanguage
import com.example.data.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentVoiceId = "friday_despina"
    private var currentSpeed = 1.0f

    val availableVoices = listOf(
        VoiceProfile(
            id = "friday_despina",
            name = "Friday (Despina)",
            gender = "Female",
            description = "Custom Voice Clone (Hindi/English/Bengali ready)",
            pitch = 1.05f,
            speechRate = 1.0f,
            isCustomVoice = true,
            audioSampleResId = R.raw.voice_friday_despina,
            supportedLanguages = listOf("en", "hi", "bn")
        ),
        VoiceProfile(
            id = "maya_zephyr",
            name = "Maya (Zephyr)",
            gender = "Female",
            description = "Custom Warm & Expressive Voice Clone",
            pitch = 1.18f,
            speechRate = 1.0f,
            isCustomVoice = true,
            audioSampleResId = R.raw.voice_maya_zephyr,
            supportedLanguages = listOf("en", "hi", "bn")
        ),
        VoiceProfile(
            id = "aria",
            name = "Aria (Female)",
            gender = "Female",
            description = "Natural & Friendly Neural Voice",
            pitch = 1.15f,
            speechRate = 1.05f
        ),
        VoiceProfile(
            id = "james",
            name = "James (Male)",
            gender = "Male",
            description = "Calm & Professional Executive Voice",
            pitch = 0.85f,
            speechRate = 0.95f
        ),
        VoiceProfile(
            id = "sophia",
            name = "Sophia (Female)",
            gender = "Female",
            description = "Warm & Expressive",
            pitch = 1.25f,
            speechRate = 1.0f
        ),
        VoiceProfile(
            id = "daniel",
            name = "Daniel (Male)",
            gender = "Male",
            description = "Deep & Confident Voice",
            pitch = 0.75f,
            speechRate = 0.9f
        ),
        VoiceProfile(
            id = "emma",
            name = "Emma (Female)",
            gender = "Female",
            description = "Energetic & Clear",
            pitch = 1.35f,
            speechRate = 1.1f
        ),
        VoiceProfile(
            id = "oliver",
            name = "Oliver (Male)",
            gender = "Male",
            description = "Casual & Natural",
            pitch = 0.95f,
            speechRate = 1.0f
        )
    )

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupProgressListener()
            applyVoiceConfig(Locale.US)
        } else {
            Log.e("TtsManager", "Failed to initialize TTS, status: $status")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                Log.e("TtsManager", "TTS error code: $errorCode")
            }
        })
    }

    fun setVoiceProfile(voiceId: String, speed: Float = currentSpeed) {
        currentVoiceId = voiceId
        currentSpeed = speed
        applyVoiceConfig(Locale.US)
    }

    private fun applyVoiceConfig(targetLocale: Locale) {
        val profile = availableVoices.find { it.id == currentVoiceId } ?: availableVoices.first()

        try {
            val availability = tts?.isLanguageAvailable(targetLocale)
            if (availability != TextToSpeech.LANG_NOT_SUPPORTED && availability != TextToSpeech.LANG_MISSING_DATA) {
                tts?.language = targetLocale
            } else {
                Log.w("TtsManager", "Locale $targetLocale not fully supported or data missing; using default engine locale")
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Could not set TTS locale: ${e.message}")
        }

        tts?.setPitch(profile.pitch)
        tts?.setSpeechRate(profile.speechRate * currentSpeed)
    }

    /**
     * Speaks text with automatic language matching:
     * - Bengali text is spoken using Bengali locale (bn-BD / bn-IN)
     * - Hindi text is spoken using Hindi locale (hi-IN)
     * - English text is spoken using English locale (en-US)
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        stopPlayback()

        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }

        // Automatic Language Detection for Speech Output
        val detectedLang = LanguageDetector.detectLanguage(text)
        val targetLocale = when (detectedLang) {
            SupportedLanguage.BENGALI -> Locale("bn", "BD")
            SupportedLanguage.HINDI -> Locale("hi", "IN")
            SupportedLanguage.ENGLISH, SupportedLanguage.AUTO -> Locale.US
        }

        applyVoiceConfig(targetLocale)

        val utteranceId = "msg_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    onDone?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    onDone?.invoke()
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    onDone?.invoke()
                }
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Plays the reference audio sample directly for custom voices,
     * or speaks sample text for standard voices.
     */
    fun testVoice(voiceId: String, onSampleEnded: (() -> Unit)? = null) {
        stopPlayback()
        val profile = availableVoices.find { it.id == voiceId } ?: availableVoices.first()
        setVoiceProfile(voiceId)

        if (profile.audioSampleResId != null) {
            try {
                mediaPlayer = MediaPlayer.create(context, profile.audioSampleResId).apply {
                    _isSpeaking.value = true
                    setOnCompletionListener {
                        _isSpeaking.value = false
                        it.release()
                        mediaPlayer = null
                        onSampleEnded?.invoke()
                    }
                    setOnErrorListener { mp, _, _ ->
                        _isSpeaking.value = false
                        mp.release()
                        mediaPlayer = null
                        onSampleEnded?.invoke()
                        true
                    }
                    start()
                }
                return
            } catch (e: Exception) {
                Log.e("TtsManager", "Error playing sample audio: ${e.message}")
            }
        }

        // Standard synthesis preview
        speak("Hello! I am ${profile.name}. This is how my voice sounds in MR AI Assistant.") {
            onSampleEnded?.invoke()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null

        tts?.stop()
        _isSpeaking.value = false
    }

    fun stop() {
        stopPlayback()
    }

    fun destroy() {
        stopPlayback()
        tts?.shutdown()
        tts = null
    }
}
