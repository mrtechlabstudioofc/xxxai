package com.example.data.model

import java.util.Locale

enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val defaultLocale: Locale
) {
    AUTO("auto", "Auto Detect", "স্বয়ংক্রিয় / स्वचालित / Auto", Locale.getDefault()),
    BENGALI("bn", "Bengali", "বাংলা", Locale("bn", "BD")),
    ENGLISH("en", "English", "English", Locale.US),
    HINDI("hi", "Hindi", "हिन्दी", Locale("hi", "IN"));

    companion object {
        fun fromCode(code: String): SupportedLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: AUTO
        }
    }
}

object LanguageDetector {

    /**
     * Automatically detects whether the text is in Bengali, Hindi, or English
     * by inspecting script unicode ranges and linguistic characteristics.
     */
    fun detectLanguage(text: String): SupportedLanguage {
        if (text.isBlank()) return SupportedLanguage.ENGLISH

        var bengaliCharCount = 0
        var devanagariCharCount = 0
        var latinCharCount = 0

        for (ch in text) {
            val codePoint = ch.code
            when {
                // Bengali Unicode block: U+0980 to U+09FF
                codePoint in 0x0980..0x09FF -> bengaliCharCount++
                // Devanagari Unicode block (Hindi): U+0900 to U+097F
                codePoint in 0x0900..0x097F -> devanagariCharCount++
                // Latin / ASCII alphabets
                (codePoint in 0x0041..0x005A) || (codePoint in 0x0061..0x007A) -> latinCharCount++
            }
        }

        return when {
            bengaliCharCount > 0 && bengaliCharCount >= devanagariCharCount -> SupportedLanguage.BENGALI
            devanagariCharCount > 0 && devanagariCharCount > bengaliCharCount -> SupportedLanguage.HINDI
            else -> SupportedLanguage.ENGLISH
        }
    }

    /**
     * Returns the recommended SpeechRecognizer language tag for an intended language.
     */
    fun getSpeechLocaleTag(lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "bn-BD"
            SupportedLanguage.HINDI -> "hi-IN"
            SupportedLanguage.ENGLISH -> "en-US"
            SupportedLanguage.AUTO -> "en-US"
        }
    }
}
