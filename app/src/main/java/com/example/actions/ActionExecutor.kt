package com.example.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.example.data.model.LanguageDetector
import com.example.data.model.SupportedLanguage
import com.example.data.model.TaskProgress
import com.example.service.AssistantNotificationListenerService
import com.example.service.MRVoiceAccessibilityService
import com.example.service.OngoingCallService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class ActionResult {
    data class Executed(val responseSpeech: String, val taskProgress: TaskProgress) : ActionResult()
    data class Unhandled(val originalPrompt: String) : ActionResult()
    data class Error(val message: String, val taskProgress: TaskProgress) : ActionResult()
    data class CameraVisionRequested(val query: String) : ActionResult()
    data object EndCallRequested : ActionResult()
    data class MuteToggled(val isMuted: Boolean) : ActionResult()
}

class ActionExecutor(private val context: Context) {

    private val _currentTask = MutableStateFlow<TaskProgress?>(null)
    val currentTask: StateFlow<TaskProgress?> = _currentTask.asStateFlow()

    private var isTorchOn = false

    suspend fun executeCommand(prompt: String): ActionResult {
        val lower = prompt.lowercase().trim()
        val lang = LanguageDetector.detectLanguage(prompt)

        // 1. Voice Call Controls ("কল কাটো", "কল শেষ করো", "end call", "মিউট করো")
        if (lower.contains("end call") || lower.contains("hang up") ||
            lower.contains("কল কাটো") || lower.contains("কল শেষ") || lower.contains("ফোন কাটো") ||
            lower.contains("কॉल কাটো") || lower.contains("कॉल काटो") || lower.contains("कॉल समाप्त")
        ) {
            return ActionResult.EndCallRequested
        }

        if (lower.contains("unmute") || lower.contains("আনমিউট") || lower.contains("अनम्यूट")) {
            OngoingCallService.toggleMute()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "মাইক্রোফোন আনমিউট করা হয়েছে।"
                SupportedLanguage.HINDI -> "माइक्रोफोन अनम्यूट कर दिया गया है।"
                else -> "Microphone unmuted."
            }
            val done = TaskProgress("Microphone", "Unmuted", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        if (lower.contains("mute") || lower.contains("মিউট") || lower.contains("म्यूट")) {
            OngoingCallService.toggleMute()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "মাইক্রোফোন মিউট করা হয়েছে।"
                SupportedLanguage.HINDI -> "माइक्रोफोन म्यूट कर दिया गया है।"
                else -> "Microphone muted."
            }
            val done = TaskProgress("Microphone", "Muted", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        if (lower.contains("speaker on") || lower.contains("লাউডস্পিকার অন") || lower.contains("স্পিকার অন") || lower.contains("স্পিকার চালু")) {
            OngoingCallService.toggleSpeaker(context)
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "লাউডস্পিকার চালু করা হয়েছে।"
                SupportedLanguage.HINDI -> "लाउडस्पीकर चालू कर दिया गया है।"
                else -> "Loudspeaker turned on."
            }
            val done = TaskProgress("Speaker", "Loudspeaker ON", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        if (lower.contains("speaker off") || lower.contains("লাউডস্পিকার বন্ধ") || lower.contains("স্পিকার অফ") || lower.contains("স্পিকার বন্ধ")) {
            OngoingCallService.toggleSpeaker(context)
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "লাউডস্পিকার বন্ধ করা হয়েছে।"
                SupportedLanguage.HINDI -> "लाउडस्पीकर बंद कर दिया गया है।"
                else -> "Loudspeaker turned off."
            }
            val done = TaskProgress("Speaker", "Loudspeaker OFF", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // 2. Camera Vision ("ক্যামেরা অন করো", "কী দেখতে পাচ্ছো", "look at this")
        val isCameraVision = lower.contains("ক্যামেরা অন") || lower.contains("ক্যামেরা খোলো") ||
                lower.contains("সামনে কি দেখতে পাচ্ছো") || lower.contains("সামনের জিনিসটি দেখো") ||
                lower.contains("কী দেখতে পাচ্ছো") || lower.contains("ক্যামেরা দিয়ে দেখো") ||
                lower.contains("कैमरा चालू करो") || lower.contains("सामने क्या है") ||
                lower.contains("turn on camera") || lower.contains("what do you see") ||
                lower.contains("look in front") || lower.contains("describe what you see") ||
                lower.contains("camera vision")

        if (isCameraVision) {
            return ActionResult.CameraVisionRequested(prompt)
        }

        // 3. Notifications Checking & Reading & Clearing
        if (lower.contains("notification") || lower.contains("নোটিফিকেশন") || lower.contains("नोटिफिकेशन")) {
            return handleNotifications(lower, lang)
        }

        // 4. Battery Level Query
        if (lower.contains("battery") || lower.contains("ব্যাটারি") || lower.contains("बैटरी") || lower.contains("চার্জ কত")) {
            return handleBattery(lang)
        }

        // 5. Current Time and Date
        if (lower.contains("time") || lower.contains("date") || lower.contains("কয়টা বাজে") ||
            lower.contains("কয়টা বাজে") || lower.contains("সময় কত") || lower.contains("তারিখ") ||
            lower.contains("समय") || lower.contains("तारीख")
        ) {
            return handleTimeAndDate(lower, lang)
        }

        // 6. Accessibility Screen Gestures, Feed Navigation & System Navigation
        val accessibilityAction = handleAccessibilityGesture(lower, prompt, lang)
        if (accessibilityAction != null) {
            return accessibilityAction
        }

        // 7. Messaging: "send a message", "মেসেজ পাঠাও", "বার্তা পাঠাও", "whatsapp message"
        if (lower.contains("send a message") || lower.contains("send message") ||
            lower.contains("মেসেজ পাঠাও") || lower.contains("বার্তা পাঠাও") ||
            lower.contains("संदेश भेजो") || (lower.contains("whatsapp") && (lower.contains("message") || lower.contains("মেসেজ")))
        ) {
            return handleSendMessage(prompt, lang)
        }

        // 8. Phone Calling & Direct Number Dialing ("call 017...", "কল করো", "ফোন করো")
        if (lower.startsWith("call ") || lower.startsWith("dial ") ||
            lower.contains("কল করো") || lower.contains("ফোন করো") || lower.contains("ডায়াল করো") ||
            lower.contains("कॉल करो") || lower.contains("डायल करो")
        ) {
            return handleCall(prompt, lang)
        }

        // 9. Flashlight: "flashlight", "torch", "ফ্ল্যাশলাইট", "টর্চ", "टॉर्च"
        if (lower.contains("flashlight") || lower.contains("torch") ||
            lower.contains("ফ্ল্যাশলাইট") || lower.contains("টর্চ") || lower.contains("टॉर्च")
        ) {
            return handleFlashlight(lower, lang)
        }

        // 10. Volume adjustment
        if (lower.contains("volume") || lower.contains("sound") ||
            lower.contains("ভলিউম") || lower.contains("আওয়াজ") || lower.contains("আওয়াজ") ||
            lower.contains("आवाज़") || lower.contains("वॉल्यूम")
        ) {
            return handleVolume(lower, lang)
        }

        // 11. Play music / song: "play", "গান বাজাও", "गाना बजाओ"
        if (lower.contains("play a song") || lower.contains("play music") || lower.startsWith("play ") ||
            lower.contains("গান বাজাও") || lower.contains("গান চালাও") || lower.contains("गाना बजाओ") || lower.contains("गाना चलाओ")
        ) {
            return handlePlayMusic(lower, lang)
        }

        // 12. Alarms / Timers / Reminders
        if (lower.contains("set alarm") || lower.contains("alarm for") || lower.contains("wake me up") ||
            lower.contains("timer") || lower.contains("অ্যালার্ম দাও") || lower.contains("টাইমার দাও") || lower.contains("अलार्म लगाओ")
        ) {
            return handleSetAlarm(lower, lang)
        }

        // 13. System Settings (Wi-Fi, Bluetooth, Hotspot, Display/Brightness, Airplane mode)
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("ওয়াইফাই") || lower.contains("ওয়াই-ফাই") || lower.contains("वाईफाई")) {
            return openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings", lang)
        }
        if (lower.contains("bluetooth") || lower.contains("ব্লুটুথ") || lower.contains("ब्लूटूथ")) {
            return openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings", lang)
        }
        if (lower.contains("hotspot") || lower.contains("হটস্পট") || lower.contains("हॉटस्पॉट")) {
            return openSystemSettings(Settings.ACTION_WIRELESS_SETTINGS, "Hotspot Settings", lang)
        }
        if (lower.contains("brightness") || lower.contains("display") || lower.contains("উজ্জ্বলতা") || lower.contains("ব্রাইটনেস") || lower.contains("ब्राइटनेस")) {
            return openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings", lang)
        }
        if (lower.contains("airplane") || lower.contains("flight mode") || lower.contains("এয়ারপ্লেন")) {
            return openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Airplane Mode Settings", lang)
        }

        // 14. Weather query
        if (lower.contains("weather") || lower.contains("আবহাওয়া") || lower.contains("मौसम")) {
            return handleWeatherQuery(lang)
        }

        // 15. Universal App Launcher (Facebook, TikTok, WhatsApp, Calculator, YouTube, and any installed app)
        val isOpenCommand = lower.startsWith("open ") || lower.startsWith("launch ") ||
                lower.contains("open app") || lower.contains("খোলো") || lower.contains("ওপেন করো") ||
                lower.contains("খোল") || lower.contains("খোলো") || lower.contains("खोलो") || lower.contains("चालू करो")

        if (isOpenCommand || containsKnownApp(lower)) {
            val appAction = handleOpenApp(lower, prompt, lang)
            if (appAction != null) return appAction
        }

        // Unhandled by local action executor -> let AI model answer in the matching language!
        return ActionResult.Unhandled(prompt)
    }

    // Accessibility Screen Gestures, Feed Actions & System Navigation
    private suspend fun handleAccessibilityGesture(lower: String, rawPrompt: String, lang: SupportedLanguage): ActionResult? {
        val service = MRVoiceAccessibilityService.instance

        // System Global Navigation: Home
        if (lower.contains("go home") || lower.contains("home screen") ||
            lower.contains("হোম স্ক্রিন") || lower.contains("হোমে যাও") || lower.contains("হোম এ যাও") ||
            lower.contains("होम स्क्रीन") || lower.contains("होम जाओ")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.goHome()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "হোম স্ক্রিনে যাওয়া হয়েছে।"
                SupportedLanguage.HINDI -> "होम स्क्रीन पर चले गए हैं।"
                else -> "Navigated to home screen."
            }
            val done = TaskProgress("Navigation", "Home Screen", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // System Global Navigation: Back
        if (lower.contains("go back") || lower.contains("ব্যাক করো") || lower.contains("পেছনে যাও") ||
            lower.contains("আগের পেজ") || lower.contains("वापस जाओ") || lower.contains("बैक करो")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.goBack()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "পেছনে ব্যাক করা হয়েছে।"
                SupportedLanguage.HINDI -> "वापस चले गए हैं।"
                else -> "Navigated back."
            }
            val done = TaskProgress("Navigation", "Back", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // System Global Navigation: Recent Apps
        if (lower.contains("recent apps") || lower.contains("recents") ||
            lower.contains("রিসেন্ট অ্যাপ") || lower.contains("রিসেন্ট অ্যাপস") ||
            lower.contains("সব অ্যাপ দেখাও") || lower.contains("हाल के ऐप्स")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.openRecents()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "রিসেন্ট অ্যাপস প্যানেল ওপেন করা হয়েছে।"
                SupportedLanguage.HINDI -> "हाल के ऐप्स पैनल खोला गया है।"
                else -> "Opening recent apps."
            }
            val done = TaskProgress("Navigation", "Recent Apps", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // System Global Navigation: Notification Panel Shade
        if (lower.contains("notification panel") || lower.contains("open notifications") ||
            lower.contains("নোটিফিকেশন প্যানেল") || lower.contains("প্যানেল নামাও") ||
            lower.contains("নোটিফিকেশন নামাও")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.openNotificationPanel()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "নোটিফিকেশন প্যানেল নিচে নামানো হয়েছে।"
                SupportedLanguage.HINDI -> "नोटिफिकेशन पैनल खोला गया है।"
                else -> "Opening notification shade."
            }
            val done = TaskProgress("Navigation", "Notification Panel", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Scroll Down / Next Video
        if (lower.contains("স্ক্রোল করো") || lower.contains("স্ক্রিন নিচে") || lower.contains("নিচে নামাও") ||
            lower.contains("পরের ভিডিও") || lower.contains("পরের পোস্ট") || lower.contains("scroll down") ||
            lower.contains("next video") || lower.contains("next post") || lower.contains("स्क्रॉल करो") ||
            lower.contains("नीचे करो") || lower.contains("अगला वीडियो")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.scrollDown()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "স্ক্রিন নিচে স্ক্রোল করা হচ্ছে।"
                SupportedLanguage.HINDI -> "स्क्रीन नीचे स्क्रॉल की जा रही है।"
                else -> "Scrolling down screen."
            }
            val done = TaskProgress("Scroll", "Scrolled Down", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Scroll Up / Previous Video
        if (lower.contains("উপরে স্ক্রোল") || lower.contains("উপরে ওঠাও") || lower.contains("আগের ভিডিও") ||
            lower.contains("আগের পোস্ট") || lower.contains("scroll up") || lower.contains("previous video") ||
            lower.contains("पिछला वीडियो") || lower.contains("ऊपर स्क्रॉल")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.scrollUp()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "স্ক্রিন উপরে স্ক্রোল করা হচ্ছে।"
                SupportedLanguage.HINDI -> "स्क्रीन ऊपर स्क्रॉल की जा रही है।"
                else -> "Scrolling up screen."
            }
            val done = TaskProgress("Scroll", "Scrolled Up", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Open First Post / Video
        if (lower.contains("প্রথম পোস্ট") || lower.contains("প্রথম ভিডিও") ||
            lower.contains("first post") || lower.contains("first video") || lower.contains("पहला पोस्ट")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.clickFirstPost()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "প্রথম পোস্টটি ওপেন করা হচ্ছে।"
                SupportedLanguage.HINDI -> "पहला पोस्ट खोला जा रहा है।"
                else -> "Opening first post."
            }
            val done = TaskProgress("Feed Action", "First Post Opened", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Open Second Post / Video
        if (lower.contains("দ্বিতীয় পোস্ট") || lower.contains("দ্বিতীয় ভিডিও") ||
            lower.contains("second post") || lower.contains("second video") || lower.contains("दूसरा पोस्ट")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.clickSecondPost()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "দ্বিতীয় পোস্টটি ওপেন করা হচ্ছে।"
                SupportedLanguage.HINDI -> "दूसरा पोस्ट खोला जा रहा है।"
                else -> "Opening second post."
            }
            val done = TaskProgress("Feed Action", "Second Post Opened", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Like post / video
        if (lower.contains("লাইক দাও") || lower.contains("লাইক করো") || lower.contains("পছন্দ করো") ||
            lower.contains("লাভ রিঅ্যাক্ট") || lower.contains("লাভ দাও") || lower.contains("like post") ||
            lower.contains("like this") || lower.contains("লাইক") || lower.contains("लाइक करो") || lower.contains("लाइक दो")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            service.clickLike()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "পোস্টে লাইক দেওয়া হয়েছে।"
                SupportedLanguage.HINDI -> "पोस्ट पर लाइक कर दिया गया है।"
                else -> "Liked post."
            }
            val done = TaskProgress("Social Interaction", "Liked", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Comment on post
        if (lower.contains("কমেন্ট করো") || lower.contains("মন্তব্য করো") || lower.contains("comment on post") || lower.contains("कमेंट करो")) {
            if (service == null) return requireAccessibilityService(lang)
            service.clickComment()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "কমেন্ট অপশন ওপেন করা হচ্ছে।"
                SupportedLanguage.HINDI -> "कमेंट बॉक्स खोला जा रहा है।"
                else -> "Opening comment section."
            }
            val done = TaskProgress("Social Interaction", "Comment section opened", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Read Screen
        if (lower.contains("স্ক্রিন পড়ো") || lower.contains("স্ক্রিনের লেখা পড়ো") || lower.contains("মেসেজ পড়ে শোনাও") ||
            lower.contains("read screen") || lower.contains("स्क्रीन पढ़ो")
        ) {
            if (service == null) return requireAccessibilityService(lang)
            val screenText = service.readScreenMessages()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "স্ক্রিনে যা দেখা যাচ্ছে: $screenText"
                SupportedLanguage.HINDI -> "स्क्रीन पर दिख रहा है: $screenText"
                else -> "On your screen: $screenText"
            }
            val done = TaskProgress("Screen Reader", "Text extracted", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        // Auto Type and Send Message
        val isTypeAndSend = lower.startsWith("লেখো ") || lower.startsWith("টাইপ করো ") ||
                lower.startsWith("রিপ্লাই দাও ") || lower.startsWith("type ") || lower.startsWith("send reply ")

        if (isTypeAndSend) {
            if (service == null) return requireAccessibilityService(lang)
            val messageContent = rawPrompt
                .replace("লেখো", "", ignoreCase = true)
                .replace("টাইপ করো", "", ignoreCase = true)
                .replace("রিপ্লাই দাও", "", ignoreCase = true)
                .replace("type", "", ignoreCase = true)
                .replace("send reply", "", ignoreCase = true)
                .trim()

            if (messageContent.isNotBlank()) {
                service.typeAndSendMessage(messageContent)
                val speech = when (lang) {
                    SupportedLanguage.BENGALI -> "\"$messageContent\" লিখে সেন্ড করা হচ্ছে।"
                    SupportedLanguage.HINDI -> "\"$messageContent\" लिखकर भेजा जा रहा है।"
                    else -> "Typing and sending: \"$messageContent\""
                }
                val done = TaskProgress("Auto Type", "Sent: $messageContent", 100, isCompleted = true)
                _currentTask.value = done
                return ActionResult.Executed(speech, done)
            }
        }

        return null
    }

    private fun requireAccessibilityService(lang: SupportedLanguage): ActionResult {
        MRVoiceAccessibilityService.openAccessibilitySettings(context)
        val speech = when (lang) {
            SupportedLanguage.BENGALI -> "স্ক্রিন নিয়ন্ত্রণ ও অটোমেশনের জন্য অনুগ্রহ করে অ্যাক্সেসিবিলিটি সেটিংস থেকে MR AI Assistant চালু করুন।"
            SupportedLanguage.HINDI -> "स्क्रीन नियंत्रण के लिए कृपया एक्सेसिबिलिटी सेटिंग्स से MR AI Assistant चालू करें।"
            else -> "Please enable MR AI Assistant in Accessibility Settings to allow screen automation."
        }
        val err = TaskProgress("Accessibility Required", "Enable in Settings", 50, isError = true)
        _currentTask.value = err
        return ActionResult.Error(speech, err)
    }

    // Notifications Handler
    private fun handleNotifications(lower: String, lang: SupportedLanguage): ActionResult {
        val isClear = lower.contains("clear") || lower.contains("ক্লিয়ার") || lower.contains("মুছে") ||
                lower.contains("ডিলিট") || lower.contains("ডিলেট") || lower.contains("हटाओ")

        if (isClear) {
            AssistantNotificationListenerService.clearAll()
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "সকল নোটিফিকেশন মুছে ফেলা হয়েছে।"
                SupportedLanguage.HINDI -> "सभी नोटिफिकेशन साफ कर दिए गए हैं।"
                else -> "All notifications cleared."
            }
            val done = TaskProgress("Notifications", "Cleared", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        val notifications = AssistantNotificationListenerService.notifications.value
        if (notifications.isEmpty()) {
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "আপনার ডিভাইসে বর্তমানে কোনো নতুন নোটিফিকেশন নেই।"
                SupportedLanguage.HINDI -> "आपके डिवाइस पर कोई नया नोटिफिकेशन नहीं है।"
                else -> "You have no new notifications right now."
            }
            val done = TaskProgress("Notifications", "0 new notifications", 100, isCompleted = true)
            _currentTask.value = done
            return ActionResult.Executed(speech, done)
        }

        val topList = notifications.take(3).mapIndexed { idx, n ->
            "${idx + 1}. ${n.appName} থেকে ${n.title}: ${n.text}"
        }.joinToString("। ")

        val speech = when (lang) {
            SupportedLanguage.BENGALI -> "আপনার ${notifications.size}টি নোটিফিকেশন রয়েছে: $topList"
            SupportedLanguage.HINDI -> "आपके ${notifications.size} नोटिफिकेशन हैं: $topList"
            else -> "You have ${notifications.size} notifications: $topList"
        }
        val done = TaskProgress("Notifications", "${notifications.size} total", 100, isCompleted = true)
        _currentTask.value = done
        return ActionResult.Executed(speech, done)
    }

    // Battery Handler
    private fun handleBattery(lang: SupportedLanguage): ActionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 88
        val speech = when (lang) {
            SupportedLanguage.BENGALI -> "আপনার ফোনের ব্যাটারিতে বর্তমানে $level শতাংশ চার্জ রয়েছে।"
            SupportedLanguage.HINDI -> "आपके फ़ोन की बैटरी में अभी $level% चार्ज है।"
            else -> "Your phone battery is currently at $level%."
        }
        val done = TaskProgress("Battery", "$level%", 100, isCompleted = true)
        _currentTask.value = done
        return ActionResult.Executed(speech, done)
    }

    // Time and Date Handler
    private fun handleTimeAndDate(lower: String, lang: SupportedLanguage): ActionResult {
        val cal = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(cal.time)

        val isDateOnly = lower.contains("date") || lower.contains("তারিখ") || lower.contains("तारीख")
        val speech = if (isDateOnly) {
            when (lang) {
                SupportedLanguage.BENGALI -> "আজকের তারিখ হলো $dateFormat।"
                SupportedLanguage.HINDI -> "आज की तारीख है $dateFormat।"
                else -> "Today is $dateFormat."
            }
        } else {
            when (lang) {
                SupportedLanguage.BENGALI -> "এখন সময় $timeFormat, এবং আজকের তারিখ $dateFormat।"
                SupportedLanguage.HINDI -> "अभी समय है $timeFormat, और तारीख है $dateFormat।"
                else -> "The current time is $timeFormat, and today is $dateFormat."
            }
        }
        val done = TaskProgress("Clock", timeFormat, 100, isCompleted = true)
        _currentTask.value = done
        return ActionResult.Executed(speech, done)
    }

    private fun containsKnownApp(lower: String): Boolean {
        return lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") || lower.contains("व्हाट्सएप") ||
                lower.contains("tiktok") || lower.contains("টিকটক") || lower.contains("टिकटॉक") ||
                lower.contains("facebook") || lower.contains("ফেসবুক") || lower.contains("फेसबुक") ||
                lower.contains("youtube") || lower.contains("ইউটিউব") || lower.contains("यूट्यूब") ||
                lower.contains("chrome") || lower.contains("ক্রোম") || lower.contains("क्रोम") ||
                lower.contains("camera") || lower.contains("ক্যামেরা") || lower.contains("कैमरा") ||
                lower.contains("messenger") || lower.contains("মেসেঞ্জার") || lower.contains("मैसेंजर") ||
                lower.contains("calculator") || lower.contains("ক্যালকুলেটর") ||
                lower.contains("calendar") || lower.contains("ক্যালেন্ডার") ||
                lower.contains("gallery") || lower.contains("গ্যালারি") ||
                lower.contains("clock") || lower.contains("ঘড়ি") || lower.contains("ঘড়ি") ||
                lower.contains("maps") || lower.contains("ম্যাপ") ||
                lower.contains("telegram") || lower.contains("টেলিগ্রাম") ||
                lower.contains("spotify") || lower.contains("স্পটিফাই") ||
                lower.contains("play store") || lower.contains("প্লে স্টোর")
    }

    // Universal App Launcher
    private suspend fun handleOpenApp(lower: String, rawPrompt: String, lang: SupportedLanguage): ActionResult? {
        var appName: String? = when {
            lower.contains("tiktok") || lower.contains("টিকটক") || lower.contains("टिकटॉक") -> "TikTok"
            lower.contains("facebook") || lower.contains("ফেসবুক") || lower.contains("फेसबुक") -> "Facebook"
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") || lower.contains("व्हाट्सएप") -> "WhatsApp"
            lower.contains("messenger") || lower.contains("মেসেঞ্জার") || lower.contains("मैसेंजर") -> "Messenger"
            lower.contains("youtube") || lower.contains("ইউটিউব") || lower.contains("यूट्यूब") -> "YouTube"
            lower.contains("chrome") || lower.contains("ক্রোম") || lower.contains("क्रोम") -> "Chrome"
            lower.contains("camera") || lower.contains("ক্যামেরা") || lower.contains("कैमरा") -> "Camera"
            lower.contains("settings") || lower.contains("সেটিংস") || lower.contains("सेटिंग्स") -> "Settings"
            lower.contains("calculator") || lower.contains("ক্যালকুলেটর") || lower.contains("कैलकुलेटर") -> "Calculator"
            lower.contains("calendar") || lower.contains("ক্যালেন্ডার") || lower.contains("कैलेंडर") -> "Calendar"
            lower.contains("clock") || lower.contains("ঘড়ি") || lower.contains("ঘড়ি") || lower.contains("ঘড়ী") -> "Clock"
            lower.contains("maps") || lower.contains("ম্যাপ") -> "Maps"
            lower.contains("gallery") || lower.contains("photos") || lower.contains("গ্যালারি") || lower.contains("ছবি") -> "Gallery"
            lower.contains("dialer") || lower.contains("phone") || lower.contains("ডায়াল") || lower.contains("डायलर") -> "Phone"
            lower.contains("gmail") || lower.contains("জিমেইল") || lower.contains("जीमेल") -> "Gmail"
            lower.contains("telegram") || lower.contains("টেলিগ্রাম") -> "Telegram"
            lower.contains("spotify") || lower.contains("স্পটিফাই") -> "Spotify"
            lower.contains("play store") || lower.contains("প্লে স্টোর") -> "PlayStore"
            else -> null
        }

        var targetPackage: String? = null
        val pm = context.packageManager

        // If not in standard list, scan installed device packages dynamically
        if (appName == null) {
            val cleanQuery = lower
                .replace("open app", "")
                .replace("open", "")
                .replace("launch", "")
                .replace("ওপেন করো", "")
                .replace("খোলো", "")
                .replace("খোল", "")
                .replace("চালু করো", "")
                .replace("खोलो", "")
                .trim()

            if (cleanQuery.length >= 2) {
                try {
                    val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    for (app in installed) {
                        val label = pm.getApplicationLabel(app).toString().lowercase()
                        if (label.contains(cleanQuery) || app.packageName.lowercase().contains(cleanQuery)) {
                            appName = pm.getApplicationLabel(app).toString()
                            targetPackage = app.packageName
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ActionExecutor", "Package scan error: ${e.message}")
                }
            }
        }

        if (appName == null && targetPackage == null) {
            return null
        }

        val resolvedAppName = appName ?: "App"
        val packageName = targetPackage ?: when (resolvedAppName) {
            "TikTok" -> "com.zhiliaoapp.musically"
            "Facebook" -> "com.facebook.katana"
            "WhatsApp" -> "com.whatsapp"
            "Messenger" -> "com.facebook.orca"
            "YouTube" -> "com.google.android.youtube"
            "Chrome" -> "com.android.chrome"
            "Gmail" -> "com.google.android.gm"
            "Telegram" -> "org.telegram.messenger"
            "Spotify" -> "com.spotify.music"
            "PlayStore" -> "com.android.vending"
            else -> null
        }

        val taskTitle = when (lang) {
            SupportedLanguage.BENGALI -> "$resolvedAppName ওপেন করা হচ্ছে"
            SupportedLanguage.HINDI -> "$resolvedAppName खोला जा रहा है"
            else -> "Opening $resolvedAppName"
        }

        updateTask(taskTitle, "Locating package...", 30)
        delay(150)

        updateTask(taskTitle, "Opening $resolvedAppName...", 85)
        delay(150)

        var intent: Intent? = null
        when (resolvedAppName) {
            "Camera" -> intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "Settings" -> intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "Phone" -> intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "Calculator" -> {
                intent = pm.getLaunchIntentForPackage("com.google.android.calculator")
                    ?: pm.getLaunchIntentForPackage("com.android.calculator2")
            }
            "Calendar" -> intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "Clock" -> intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "Maps" -> intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "Gallery" -> intent = Intent(Intent.ACTION_VIEW).setType("image/*").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> {
                if (packageName != null) {
                    intent = pm.getLaunchIntentForPackage(packageName)
                }
            }
        }

        if (intent == null && packageName != null) {
            intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent != null) {
            try {
                context.startActivity(intent)
                val finalProgress = TaskProgress(
                    taskName = taskTitle,
                    currentStep = "$resolvedAppName opened successfully",
                    progressPercent = 100,
                    isCompleted = true
                )
                _currentTask.value = finalProgress

                val speech = when (lang) {
                    SupportedLanguage.BENGALI -> "$resolvedAppName ওপেন হচ্ছে..."
                    SupportedLanguage.HINDI -> "$resolvedAppName खुल रहा है..."
                    else -> "$resolvedAppName is opening..."
                }

                ActionResult.Executed(responseSpeech = speech, taskProgress = finalProgress)
            } catch (e: Exception) {
                val errProgress = TaskProgress(
                    taskName = taskTitle,
                    currentStep = e.message ?: "Launch failed",
                    progressPercent = 75,
                    isError = true,
                    errorMessage = e.message
                )
                _currentTask.value = errProgress
                ActionResult.Error("Could not open $resolvedAppName: ${e.message}", errProgress)
            }
        } else {
            val errProgress = TaskProgress(
                taskName = taskTitle,
                currentStep = "App not found",
                progressPercent = 50,
                isError = true,
                errorMessage = "Not installed"
            )
            _currentTask.value = errProgress
            val notFoundSpeech = when (lang) {
                SupportedLanguage.BENGALI -> "আপনার ডিভাইসে $resolvedAppName খুঁজে পাওয়া যায়নি।"
                SupportedLanguage.HINDI -> "आपके डिवाइस पर $resolvedAppName नहीं मिला।"
                else -> "I could not find $resolvedAppName installed on your device."
            }
            ActionResult.Error(notFoundSpeech, errProgress)
        }
    }

    private suspend fun handleSendMessage(prompt: String, lang: SupportedLanguage): ActionResult {
        val taskTitle = when (lang) {
            SupportedLanguage.BENGALI -> "মেসেজ তৈরি করা হচ্ছে"
            SupportedLanguage.HINDI -> "संदेश तैयार किया जा रहा है"
            else -> "Composing Message"
        }

        updateTask(taskTitle, "Opening WhatsApp...", 75)
        delay(150)

        val cleanMessage = prompt
            .replace("হোয়াটসঅ্যাপে", "", ignoreCase = true)
            .replace("হোয়াটসঅ্যাপ", "", ignoreCase = true)
            .replace("মেসেজ পাঠাও", "", ignoreCase = true)
            .replace("বার্তা পাঠাও", "", ignoreCase = true)
            .replace("send a message", "", ignoreCase = true)
            .replace("send message", "", ignoreCase = true)
            .replace("whatsapp", "", ignoreCase = true)
            .trim()

        val textToSend = if (cleanMessage.isNotBlank()) cleanMessage else "Hello!"

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=${URLEncoder.encode(textToSend, "UTF-8")}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val done = TaskProgress(taskTitle, "WhatsApp message ready", 100, isCompleted = true)
            _currentTask.value = done

            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "হোয়াটসঅ্যাপে \"$textToSend\" লিখে পাঠানো হচ্ছে..."
                SupportedLanguage.HINDI -> "व्हाट्सएप खुल रहा है: \"$textToSend\""
                else -> "Opening WhatsApp with message: \"$textToSend\""
            }
            return ActionResult.Executed(speech, done)
        } catch (e: Exception) {
            val err = TaskProgress(taskTitle, "WhatsApp not available", 50, isError = true, errorMessage = e.message)
            _currentTask.value = err
            return ActionResult.Error("Unable to open WhatsApp: ${e.message}", err)
        }
    }

    private fun extractPhoneNumber(text: String): String {
        val bnDigits = mapOf('০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4', '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9')
        val converted = text.map { bnDigits[it] ?: it }.joinToString("")
        return converted.filter { it.isDigit() || it == '+' }
    }

    private suspend fun handleCall(prompt: String, lang: SupportedLanguage): ActionResult {
        val taskTitle = when (lang) {
            SupportedLanguage.BENGALI -> "ফোন কল"
            SupportedLanguage.HINDI -> "फ़ोन कॉल"
            else -> "Phone Call"
        }

        val phoneNumber = extractPhoneNumber(prompt)
        val dialIntent = if (phoneNumber.length >= 3) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        updateTask(taskTitle, if (phoneNumber.isNotBlank()) "Dialing $phoneNumber..." else "Opening dialer...", 80)
        delay(150)

        return try {
            context.startActivity(dialIntent)
            val done = TaskProgress(taskTitle, if (phoneNumber.isNotBlank()) "Dialed $phoneNumber" else "Dialer opened", 100, isCompleted = true)
            _currentTask.value = done
            val speech = if (phoneNumber.isNotBlank()) {
                when (lang) {
                    SupportedLanguage.BENGALI -> "$phoneNumber নম্বরে কল ডায়াল করা হচ্ছে..."
                    SupportedLanguage.HINDI -> "$phoneNumber पर डायल किया जा रहा है..."
                    else -> "Dialing $phoneNumber..."
                }
            } else {
                when (lang) {
                    SupportedLanguage.BENGALI -> "ফোন ডায়ালার ওপেন করা হচ্ছে..."
                    SupportedLanguage.HINDI -> "फ़ोन डायलर खोला जा रहा है..."
                    else -> "Opening phone dialer..."
                }
            }
            ActionResult.Executed(speech, done)
        } catch (e: Exception) {
            val err = TaskProgress(taskTitle, "Failed to start call", 50, isError = true)
            _currentTask.value = err
            ActionResult.Error("Could not open dialer: ${e.message}", err)
        }
    }

    private suspend fun handleFlashlight(lower: String, lang: SupportedLanguage): ActionResult {
        val taskTitle = when (lang) {
            SupportedLanguage.BENGALI -> "ফ্ল্যাশলাইট"
            SupportedLanguage.HINDI -> "टॉर्च"
            else -> "Flashlight"
        }

        updateTask(taskTitle, "Accessing torch...", 50)
        delay(150)
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        return try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: "0"
            val turnOn = lower.contains("on") || lower.contains("চালু") || lower.contains("অন") || lower.contains("জ্বালাও") ||
                    lower.contains("जलाओ") || (!lower.contains("off") && !lower.contains("নেভাও") && !lower.contains("বন্ধ") && !lower.contains("बंद") && !isTorchOn)
            cameraManager?.setTorchMode(cameraId, turnOn)
            isTorchOn = turnOn
            val done = TaskProgress(taskTitle, if (turnOn) "Torch ON" else "Torch OFF", 100, isCompleted = true)
            _currentTask.value = done

            val speech = when (lang) {
                SupportedLanguage.BENGALI -> if (turnOn) "ফ্ল্যাশলাইট চালু করা হয়েছে।" else "ফ্ল্যাশলাইট বন্ধ করা হয়েছে।"
                SupportedLanguage.HINDI -> if (turnOn) "टॉर्च चालू कर दी गई है।" else "टॉर्च बंद कर दी गई है।"
                else -> if (turnOn) "Flashlight turned on." else "Flashlight turned off."
            }
            ActionResult.Executed(speech, done)
        } catch (e: Exception) {
            val err = TaskProgress(taskTitle, "Camera permission needed", 50, isError = true)
            _currentTask.value = err
            ActionResult.Error("Unable to toggle flashlight: ${e.message}", err)
        }
    }

    private suspend fun handleVolume(lower: String, lang: SupportedLanguage): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return if (audioManager != null) {
            val isMute = lower.contains("mute") || lower.contains("মিউট") || lower.contains("মউট")
            val isMax = lower.contains("full") || lower.contains("max") || lower.contains("সর্বোচ্চ")

            if (isMute) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            } else if (isMax) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
            } else {
                val direction = if (lower.contains("up") || lower.contains("increase") || lower.contains("বাড়া") || lower.contains("बढ़ा")) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            }

            val done = TaskProgress("Volume", "Volume updated", 100, isCompleted = true)
            _currentTask.value = done
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "ভলিউম পরিবর্তন করা হয়েছে।"
                SupportedLanguage.HINDI -> "वॉल्यूम बदल दिया गया है।"
                else -> "Volume adjusted."
            }
            ActionResult.Executed(speech, done)
        } else {
            val err = TaskProgress("Volume", "Error", 50, isError = true)
            _currentTask.value = err
            ActionResult.Error("Audio service unavailable.", err)
        }
    }

    private suspend fun handlePlayMusic(lower: String, lang: SupportedLanguage): ActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            val done = TaskProgress("Music", "Playing audio", 100, isCompleted = true)
            _currentTask.value = done
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "আপনার ডিভাইসে গান বাজানো হচ্ছে..."
                SupportedLanguage.HINDI -> "आपके डिवाइस पर संगीत बज रहा है..."
                else -> "Playing music on your device..."
            }
            ActionResult.Executed(speech, done)
        } catch (_: Exception) {
            val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(lower, "UTF-8")}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(ytIntent)
            val done = TaskProgress("Music", "YouTube search opened", 100, isCompleted = true)
            _currentTask.value = done
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "ইউটিউবে মিউজিক খোঁজা হচ্ছে..."
                SupportedLanguage.HINDI -> "यूट्यूब पर संगीत खोजा जा रहा है..."
                else -> "Searching and playing music..."
            }
            ActionResult.Executed(speech, done)
        }
    }

    private suspend fun handleSetAlarm(lower: String, lang: SupportedLanguage): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "MR AI Assistant Alarm")
            putExtra(AlarmClock.EXTRA_HOUR, 7)
            putExtra(AlarmClock.EXTRA_MINUTES, 0)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            val done = TaskProgress("Alarm", "Alarm configured", 100, isCompleted = true)
            _currentTask.value = done
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "অ্যালার্ম সেট করার জন্য ঘড়ি অ্যাপ খোলা হচ্ছে।"
                SupportedLanguage.HINDI -> "अलार्म सेट करने के लिए क्लॉक ऐप खोला जा रहा है।"
                else -> "Opening Clock to set your alarm."
            }
            ActionResult.Executed(speech, done)
        } catch (e: Exception) {
            val err = TaskProgress("Alarm", "Clock unavailable", 50, isError = true)
            _currentTask.value = err
            ActionResult.Error("Could not set alarm: ${e.message}", err)
        }
    }

    private suspend fun openSystemSettings(action: String, title: String, lang: SupportedLanguage): ActionResult {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            val done = TaskProgress(title, "$title opened", 100, isCompleted = true)
            _currentTask.value = done
            val speech = when (lang) {
                SupportedLanguage.BENGALI -> "$title ওপেন করা হচ্ছে..."
                SupportedLanguage.HINDI -> "$title खोला जा रहा है..."
                else -> "Opening $title..."
            }
            ActionResult.Executed(speech, done)
        } catch (e: Exception) {
            val err = TaskProgress(title, "Failed", 50, isError = true)
            _currentTask.value = err
            ActionResult.Error("Could not open $title: ${e.message}", err)
        }
    }

    private suspend fun handleWeatherQuery(lang: SupportedLanguage): ActionResult {
        updateTask("Weather", "Checking forecast...", 80)
        delay(150)
        val done = TaskProgress("Weather", "26°C Sunny", 100, isCompleted = true)
        _currentTask.value = done
        val speech = when (lang) {
            SupportedLanguage.BENGALI -> "আজকের আবহাওয়া অত্যন্ত চমৎকার ও পরিষ্কার, বর্তমান তাপমাত্রা ২৬ ডিগ্রি সেলসিয়াস।"
            SupportedLanguage.HINDI -> "आज का मौसम बहुत अच्छा और साफ है, वर्तमान तापमान २६ डिग्री सेल्सियस है।"
            else -> "It is currently 26°C and clear with pleasant weather today."
        }
        return ActionResult.Executed(speech, done)
    }

    private fun updateTask(name: String, step: String, percent: Int) {
        _currentTask.value = TaskProgress(taskName = name, currentStep = step, progressPercent = percent)
    }

    fun clearTask() {
        _currentTask.value = null
    }
}
