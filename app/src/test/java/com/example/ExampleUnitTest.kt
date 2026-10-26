package com.example

import com.example.data.model.LanguageDetector
import com.example.data.model.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `detect Bengali language from native script`() {
        val bengaliQuery = "আজকের আবহাওয়া কেমন?"
        val detected = LanguageDetector.detectLanguage(bengaliQuery)
        assertEquals(SupportedLanguage.BENGALI, detected)
    }

    @Test
    fun `detect Hindi language from Devanagari script`() {
        val hindiQuery = "आप मेरी क्या मदद कर सकते हैं?"
        val detected = LanguageDetector.detectLanguage(hindiQuery)
        assertEquals(SupportedLanguage.HINDI, detected)
    }

    @Test
    fun `detect English language from standard text`() {
        val englishQuery = "What can you do?"
        val detected = LanguageDetector.detectLanguage(englishQuery)
        assertEquals(SupportedLanguage.ENGLISH, detected)
    }

    @Test
    fun `detect mixed Bengali code switching correctly`() {
        val mixedQuery = "আমার WhatsApp খোলো এবং রহিমকে মেসেজ দাও"
        val detected = LanguageDetector.detectLanguage(mixedQuery)
        assertEquals(SupportedLanguage.BENGALI, detected)
    }

    @Test
    fun `detect mixed Hindi code switching correctly`() {
        val mixedQuery = "मेरा YouTube खोलो और गाना चलाओ"
        val detected = LanguageDetector.detectLanguage(mixedQuery)
        assertEquals(SupportedLanguage.HINDI, detected)
    }

    @Test
    fun `test call control keywords detection`() {
        val bengaliEndCall = "কল কাটো"
        val isBengaliEnd = bengaliEndCall.contains("কল কাটো") || bengaliEndCall.contains("কল শেষ")
        assertEquals(true, isBengaliEnd)

        val hindiEndCall = "कॉल काटो"
        val isHindiEnd = hindiEndCall.contains("कॉल काटो")
        assertEquals(true, isHindiEnd)

        val engEndCall = "end call now"
        val isEngEnd = engEndCall.contains("end call")
        assertEquals(true, isEngEnd)
    }
}
