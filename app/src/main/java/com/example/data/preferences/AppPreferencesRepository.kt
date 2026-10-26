package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AiProvider
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryItem
import com.example.data.model.OrbStyleType
import com.example.data.model.SoulPersonality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AppPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mr_ai_prefs", Context.MODE_PRIVATE)

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _selectedProvider = MutableStateFlow(
        try {
            AiProvider.valueOf(prefs.getString(KEY_PROVIDER, AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name)
        } catch (_: Exception) {
            AiProvider.GEMINI
        }
    )
    val selectedProvider: StateFlow<AiProvider> = _selectedProvider.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString(KEY_GEMINI_KEY, "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow(prefs.getString(KEY_OPENROUTER_KEY, "") ?: "")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getString(KEY_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _continuousListening = MutableStateFlow(prefs.getBoolean(KEY_CONTINUOUS_LISTENING, true))
    val continuousListening: StateFlow<Boolean> = _continuousListening.asStateFlow()

    private val _selectedVoiceId = MutableStateFlow(prefs.getString(KEY_VOICE_ID, "friday_despina") ?: "friday_despina")
    val selectedVoiceId: StateFlow<String> = _selectedVoiceId.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        com.example.data.model.SupportedLanguage.fromCode(prefs.getString(KEY_LANGUAGE, "auto") ?: "auto")
    )
    val selectedLanguage: StateFlow<com.example.data.model.SupportedLanguage> = _selectedLanguage.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_SPEECH_RATE, 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _selectedOrbType = MutableStateFlow(
        try {
            OrbStyleType.valueOf(prefs.getString(KEY_ORB_TYPE, OrbStyleType.NEON_EMERALD.name) ?: OrbStyleType.NEON_EMERALD.name)
        } catch (_: Exception) {
            OrbStyleType.NEON_EMERALD
        }
    )
    val selectedOrbType: StateFlow<OrbStyleType> = _selectedOrbType.asStateFlow()

    private val _orbOpacity = MutableStateFlow(prefs.getFloat(KEY_ORB_OPACITY, 0.85f))
    val orbOpacity: StateFlow<Float> = _orbOpacity.asStateFlow()

    private val _showFloatingOverlay = MutableStateFlow(prefs.getBoolean(KEY_FLOATING_OVERLAY, false))
    val showFloatingOverlay: StateFlow<Boolean> = _showFloatingOverlay.asStateFlow()

    private val _memories = MutableStateFlow(loadMemories())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _chatHistory = MutableStateFlow(loadChatHistory())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _soul = MutableStateFlow(loadSoul())
    val soul: StateFlow<SoulPersonality> = _soul.asStateFlow()

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
        _selectedProvider.value = provider
        if (provider == AiProvider.GEMINI && !_selectedModel.value.startsWith("gemini")) {
            setSelectedModel("gemini-2.5-flash")
        } else if (provider == AiProvider.OPEN_ROUTER && _selectedModel.value.startsWith("gemini")) {
            setSelectedModel("google/gemini-2.5-flash")
        }
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_KEY, key.trim()).apply()
        _geminiApiKey.value = key.trim()
    }

    fun setOpenRouterApiKey(key: String) {
        prefs.edit().putString(KEY_OPENROUTER_KEY, key.trim()).apply()
        _openRouterApiKey.value = key.trim()
    }

    fun setSelectedModel(modelId: String) {
        prefs.edit().putString(KEY_MODEL, modelId).apply()
        _selectedModel.value = modelId
    }

    fun setContinuousListening(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONTINUOUS_LISTENING, enabled).apply()
        _continuousListening.value = enabled
    }

    fun setSelectedVoiceId(voiceId: String) {
        prefs.edit().putString(KEY_VOICE_ID, voiceId).apply()
        _selectedVoiceId.value = voiceId
    }

    fun setSelectedLanguage(lang: com.example.data.model.SupportedLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
        _selectedLanguage.value = lang
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _speechRate.value = rate
    }

    fun setSelectedOrbType(type: OrbStyleType) {
        prefs.edit().putString(KEY_ORB_TYPE, type.name).apply()
        _selectedOrbType.value = type
    }

    fun setOrbOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_ORB_OPACITY, opacity).apply()
        _orbOpacity.value = opacity
    }

    fun setShowFloatingOverlay(show: Boolean) {
        prefs.edit().putBoolean(KEY_FLOATING_OVERLAY, show).apply()
        _showFloatingOverlay.value = show
    }

    fun addMemory(key: String, value: String) {
        val list = _memories.value.toMutableList()
        list.add(0, MemoryItem(key = key, value = value))
        saveMemories(list)
    }

    fun deleteMemory(id: String) {
        val list = _memories.value.filterNot { it.id == id }
        saveMemories(list)
    }

    fun clearMemories() {
        saveMemories(emptyList())
    }

    fun addChatMessage(message: ChatMessage) {
        val list = _chatHistory.value.toMutableList()
        list.add(message)
        // Keep last 100 messages
        val trimmed = if (list.size > 100) list.takeLast(100) else list
        saveChatHistory(trimmed)
    }

    fun clearChatHistory() {
        saveChatHistory(emptyList())
    }

    fun updateSoul(style: String, tone: String, responseLength: String, customInstruction: String) {
        val soul = SoulPersonality(style, tone, responseLength, customInstruction)
        val json = JSONObject().apply {
            put("style", soul.style)
            put("tone", soul.tone)
            put("responseLength", soul.responseLength)
            put("customInstruction", soul.customInstruction)
        }
        prefs.edit().putString(KEY_SOUL, json.toString()).apply()
        _soul.value = soul
    }

    private fun loadMemories(): List<MemoryItem> {
        val raw = prefs.getString(KEY_MEMORIES, null) ?: return defaultMemories()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<MemoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MemoryItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        key = obj.optString("key", ""),
                        value = obj.optString("value", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultMemories()
        }
    }

    private fun defaultMemories(): List<MemoryItem> {
        return listOf(
            MemoryItem(key = "Owner Name", value = "User"),
            MemoryItem(key = "Preferred Messaging", value = "WhatsApp"),
            MemoryItem(key = "Assistant Title", value = "MR AI Assistant")
        )
    }

    private fun saveMemories(list: List<MemoryItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("key", item.key)
                put("value", item.value)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_MEMORIES, arr.toString()).apply()
        _memories.value = list
    }

    private fun loadChatHistory(): List<ChatMessage> {
        val raw = prefs.getString(KEY_CHAT_HISTORY, null) ?: return listOf(
            ChatMessage(
                isUser = false,
                text = "Hello! I am your MR AI Assistant. How can I help you today?"
            )
        )
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id"),
                        isUser = obj.optBoolean("isUser"),
                        text = obj.optString("text"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
            if (list.isEmpty()) {
                listOf(ChatMessage(isUser = false, text = "Hello! I am your MR AI Assistant. How can I help you today?"))
            } else list
        } catch (_: Exception) {
            listOf(ChatMessage(isUser = false, text = "Hello! I am your MR AI Assistant. How can I help you today?"))
        }
    }

    private fun saveChatHistory(list: List<ChatMessage>) {
        val arr = JSONArray()
        list.forEach { msg ->
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("isUser", msg.isUser)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CHAT_HISTORY, arr.toString()).apply()
        _chatHistory.value = list
    }

    private fun loadSoul(): SoulPersonality {
        val raw = prefs.getString(KEY_SOUL, null) ?: return SoulPersonality()
        return try {
            val obj = JSONObject(raw)
            SoulPersonality(
                style = obj.optString("style", "Helpful & Friendly"),
                tone = obj.optString("tone", "Warm, concise, and proactive"),
                responseLength = obj.optString("responseLength", "Concise"),
                customInstruction = obj.optString("customInstruction", "You are MR AI Assistant, a powerful personal AI companion for Android.")
            )
        } catch (_: Exception) {
            SoulPersonality()
        }
    }

    companion object {
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_KEY = "gemini_key"
        private const val KEY_OPENROUTER_KEY = "openrouter_key"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_CONTINUOUS_LISTENING = "continuous_listening"
        private const val KEY_VOICE_ID = "voice_id"
        private const val KEY_LANGUAGE = "language_code"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_ORB_TYPE = "orb_type"
        private const val KEY_ORB_OPACITY = "orb_opacity"
        private const val KEY_FLOATING_OVERLAY = "floating_overlay"
        private const val KEY_MEMORIES = "memories"
        private const val KEY_CHAT_HISTORY = "chat_history"
        private const val KEY_SOUL = "soul"
    }
}
