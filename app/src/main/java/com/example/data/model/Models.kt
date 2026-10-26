package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.OrangeGlow
import com.example.ui.theme.PurpleGlow

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    EXECUTING,
    ERROR
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val taskProgress: TaskProgress? = null
)

data class TaskProgress(
    val taskName: String,
    val currentStep: String,
    val progressPercent: Int = 0,
    val isCompleted: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

data class VoiceProfile(
    val id: String,
    val name: String,
    val gender: String,
    val description: String,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val languageCode: String = "en-US",
    val isCustomVoice: Boolean = false,
    val audioSampleResId: Int? = null,
    val supportedLanguages: List<String> = listOf("en", "bn", "hi")
)

enum class OrbStyleType {
    NEON_EMERALD,
    QUANTUM_AZURE,
    NEBULA_VIOLET,
    SOLAR_FLARE,
    GOLDEN_CORE,
    CELESTIAL_RING
}

data class OrbStyle(
    val type: OrbStyleType,
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val particleCount: Int = 120
) {
    companion object {
        val ALL_STYLES = listOf(
            OrbStyle(OrbStyleType.NEON_EMERALD, "Neon Emerald", NeonGreen, CyanAccent, 130),
            OrbStyle(OrbStyleType.QUANTUM_AZURE, "Quantum Azure", CyanAccent, Color(0xFF3B82F6), 140),
            OrbStyle(OrbStyleType.NEBULA_VIOLET, "Nebula Violet", PurpleGlow, Color(0xFFEC4899), 120),
            OrbStyle(OrbStyleType.SOLAR_FLARE, "Solar Flare", OrangeGlow, LuminousGold, 125),
            OrbStyle(OrbStyleType.GOLDEN_CORE, "Golden Core", LuminousGold, NeonGreen, 130),
            OrbStyle(OrbStyleType.CELESTIAL_RING, "Celestial Ring", Color(0xFF10B981), LuminousGold, 150)
        )
    }
}

enum class AiProvider(val displayName: String, val description: String) {
    GEMINI("Google Gemini", "Use Gemini API"),
    OPEN_ROUTER("OpenRouter", "Use OpenRouter API")
}

data class AiModelInfo(
    val id: String,
    val name: String,
    val provider: AiProvider,
    val badge: String? = null,
    val description: String = "",
    val supportsAudio: Boolean = false
)

data class AppNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestampMillis: Long,
    val category: String? = null
)

data class MemoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SoulPersonality(
    val style: String = "Helpful & Friendly",
    val tone: String = "Warm, concise, and proactive",
    val responseLength: String = "Concise",
    val customInstruction: String = "You are MR AI Assistant, a powerful personal AI companion for Android."
)
