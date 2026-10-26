package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.actions.ActionResult
import com.example.data.model.AiModelInfo
import com.example.data.model.ChatMessage
import com.example.data.model.LanguageDetector
import com.example.data.model.SupportedLanguage
import com.example.data.model.VoiceState
import com.example.service.AssistantNotificationListenerService
import com.example.service.FloatingOrbService
import com.example.service.OngoingCallService
import com.example.ui.components.CameraVisionManager
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiSettingsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveCallScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OrbSettingsScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SoulScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextSecondary
import com.example.voice.SpeechManager
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    ONBOARDING,
    DASHBOARD,
    CALL,
    HISTORY,
    SETTINGS,
    CHAT,
    AI_SETTINGS,
    PERMISSIONS,
    VOICE,
    ORB,
    NOTIFICATIONS,
    MEMORY,
    SOUL,
    ABOUT
}

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AssistantApplication
        val prefs = app.preferencesRepository
        val aiApi = app.aiApiService
        val tts = app.ttsManager
        val actionExecutor = app.actionExecutor

        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()

                // Persistent preferences
                val onboardingDone by prefs.onboardingCompleted.collectAsState()
                val provider by prefs.selectedProvider.collectAsState()
                val geminiKey by prefs.geminiApiKey.collectAsState()
                val openRouterKey by prefs.openRouterApiKey.collectAsState()
                val selectedModel by prefs.selectedModel.collectAsState()
                val continuousListening by prefs.continuousListening.collectAsState()
                val selectedVoiceId by prefs.selectedVoiceId.collectAsState()
                val selectedLanguage by prefs.selectedLanguage.collectAsState()
                val speechRate by prefs.speechRate.collectAsState()
                val selectedOrbType by prefs.selectedOrbType.collectAsState()
                val orbOpacity by prefs.orbOpacity.collectAsState()
                val showFloatingOverlay by prefs.showFloatingOverlay.collectAsState()
                val memories by prefs.memories.collectAsState()
                val chatHistory by prefs.chatHistory.collectAsState()
                val soul by prefs.soul.collectAsState()

                // Ongoing Call & Camera states
                val isCallActive by OngoingCallService.isCallActive.collectAsState()
                val isMuted by OngoingCallService.isMuted.collectAsState()
                val isSpeakerOn by OngoingCallService.isSpeakerActive.collectAsState()
                val isCameraVisionActive by OngoingCallService.isCameraVisionActive.collectAsState()
                val callDurationFormatted by OngoingCallService.callDurationFormatted.collectAsState()

                val cameraVisionManager = remember { CameraVisionManager(this@MainActivity) }

                // UI Screen Navigation State
                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
                var voiceState by remember { mutableStateOf(VoiceState.IDLE) }
                var lastAiResponse by remember { mutableStateOf("") }
                var isProcessing by remember { mutableStateOf(false) }
                val currentTask by actionExecutor.currentTask.collectAsState()
                val notifications by AssistantNotificationListenerService.notifications.collectAsState()

                // Models catalog
                var modelsList by remember { mutableStateOf<List<AiModelInfo>>(emptyList()) }
                var isLoadingModels by remember { mutableStateOf(false) }

                // Runtime Permission Launchers
                val singlePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Toast.makeText(this@MainActivity, "Permission granted", Toast.LENGTH_SHORT).show()
                    }
                }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        OngoingCallService.start(this@MainActivity)
                        speechManager.startListening()
                    } else {
                        Toast.makeText(this@MainActivity, "Microphone permission is required for voice call", Toast.LENGTH_LONG).show()
                    }
                }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        OngoingCallService.toggleCameraVision(true)
                    } else {
                        Toast.makeText(this@MainActivity, "Camera permission needed for vision queries", Toast.LENGTH_SHORT).show()
                    }
                }

                fun refreshModels() {
                    coroutineScope.launch {
                        isLoadingModels = true
                        val key = if (provider == com.example.data.model.AiProvider.GEMINI) geminiKey else openRouterKey
                        modelsList = aiApi.fetchModels(provider, key)
                        isLoadingModels = false
                    }
                }

                LaunchedEffect(provider) {
                    refreshModels()
                }

                fun speakAiResponse(text: String) {
                    if (isSpeakerOn) {
                        voiceState = VoiceState.SPEAKING
                        speechManager.isAssistantSpeaking = true
                        tts.speak(text) {
                            speechManager.isAssistantSpeaking = false
                            voiceState = VoiceState.IDLE
                            if (continuousListening && isCallActive && !isMuted) {
                                speechManager.resumeAfterResponse()
                            }
                        }
                    } else {
                        voiceState = VoiceState.IDLE
                        if (continuousListening && isCallActive && !isMuted) {
                            speechManager.resumeAfterResponse()
                        }
                    }
                }

                // AI Camera Frame Analysis
                fun analyzeCurrentCameraFrame(customPrompt: String = "") {
                    val detectedLang = if (customPrompt.isNotBlank()) LanguageDetector.detectLanguage(customPrompt) else selectedLanguage
                    val userQuery = if (customPrompt.isNotBlank()) customPrompt else "Describe in detail what you see in front of the camera."

                    voiceState = VoiceState.PROCESSING
                    lastAiResponse = when (detectedLang) {
                        SupportedLanguage.BENGALI -> "ক্যামেরার দৃশ্য বিশ্লেষণ করা হচ্ছে..."
                        SupportedLanguage.HINDI -> "कैमरे का दृश्य विश्लेषण किया जा रहा है..."
                        else -> "Analyzing camera frame..."
                    }

                    cameraVisionManager.captureFrameForAi(
                        onSuccess = { base64Jpeg ->
                            coroutineScope.launch {
                                val activeKey = if (provider == com.example.data.model.AiProvider.GEMINI) geminiKey else openRouterKey
                                val languageDirective = when (detectedLang) {
                                    SupportedLanguage.BENGALI -> "Respond completely in fluent Bengali (বাংলা). Describe what is visible in front of the camera."
                                    SupportedLanguage.HINDI -> "Respond completely in fluent Hindi (हिन्दी). Describe what is visible in front of the camera."
                                    else -> "Respond concisely in English describing what is in front of the camera."
                                }

                                val sysPrompt = """
                                    You are MR AI Assistant with real-time camera vision capabilities.
                                    $languageDirective
                                    Personality: ${soul.style}. Tone: ${soul.tone}.
                                """.trimIndent()

                                val result = aiApi.generateMultimodalResponse(
                                    provider = provider,
                                    apiKey = activeKey,
                                    model = selectedModel,
                                    systemInstruction = sysPrompt,
                                    prompt = userQuery,
                                    imageBase64Jpeg = base64Jpeg
                                )

                                result.onSuccess { visionReply ->
                                    lastAiResponse = visionReply
                                    prefs.addChatMessage(ChatMessage(isUser = false, text = visionReply))
                                    speakAiResponse(visionReply)
                                }.onFailure { err ->
                                    val errReply = "Vision analysis error: ${err.message}"
                                    lastAiResponse = errReply
                                    prefs.addChatMessage(ChatMessage(isUser = false, text = errReply))
                                    speakAiResponse(errReply)
                                }
                            }
                        },
                        onError = { errorMsg ->
                            lastAiResponse = errorMsg
                            speakAiResponse(errorMsg)
                        }
                    )
                }

                // Voice recognition handler
                fun handleUserUtterance(query: String) {
                    coroutineScope.launch {
                        isProcessing = true
                        voiceState = VoiceState.PROCESSING

                        // Add user query to chat history
                        prefs.addChatMessage(ChatMessage(isUser = true, text = query))

                        // First try local Android Action Executor
                        val actionResult = actionExecutor.executeCommand(query)
                        when (actionResult) {
                            is ActionResult.Executed -> {
                                voiceState = VoiceState.EXECUTING
                                lastAiResponse = actionResult.responseSpeech
                                prefs.addChatMessage(
                                    ChatMessage(
                                        isUser = false,
                                        text = actionResult.responseSpeech,
                                        taskProgress = actionResult.taskProgress
                                    )
                                )
                                speakAiResponse(actionResult.responseSpeech)
                            }

                            is ActionResult.CameraVisionRequested -> {
                                // Automatically ensure camera vision is active and analyze frame
                                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    OngoingCallService.toggleCameraVision(true)
                                    analyzeCurrentCameraFrame(actionResult.query)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }

                            is ActionResult.EndCallRequested -> {
                                OngoingCallService.stop(this@MainActivity)
                                speechManager.stopListening()
                                val byeSpeech = "কল শেষ করা হয়েছে। আবার কথা বলতে চাইলে কল বাটনে ট্যাপ করুন।"
                                lastAiResponse = byeSpeech
                                prefs.addChatMessage(ChatMessage(isUser = false, text = byeSpeech))
                                speakAiResponse(byeSpeech)
                            }

                            is ActionResult.MuteToggled -> {
                                speechManager.isMuted = actionResult.isMuted
                            }

                            is ActionResult.Error -> {
                                voiceState = VoiceState.ERROR
                                lastAiResponse = actionResult.message
                                prefs.addChatMessage(
                                    ChatMessage(
                                        isUser = false,
                                        text = actionResult.message,
                                        taskProgress = actionResult.taskProgress
                                    )
                                )
                                speakAiResponse(actionResult.message)
                            }

                            is ActionResult.Unhandled -> {
                                // Delegate to Generative AI model with automatic language matching
                                val activeKey = if (provider == com.example.data.model.AiProvider.GEMINI) geminiKey else openRouterKey
                                val memoryContext = if (memories.isNotEmpty()) {
                                    "User facts you remember: " + memories.joinToString("; ") { "${it.key}: ${it.value}" } + "\n"
                                } else ""

                                val detectedLang = LanguageDetector.detectLanguage(query)
                                val languageDirective = when (detectedLang) {
                                    SupportedLanguage.BENGALI ->
                                        "CRITICAL LANGUAGE RULE: The user is communicating in Bengali (বাংলা). You MUST answer completely in fluent Bengali using Bengali script. Do not reply in English."
                                    SupportedLanguage.HINDI ->
                                        "CRITICAL LANGUAGE RULE: The user is communicating in Hindi (हिन्दी). You MUST answer completely in fluent Hindi using Devanagari script. Do not reply in English."
                                    else ->
                                        "The user is communicating in English. Answer in clear English."
                                }

                                val fullSystemInstruction = """
                                    You are MR AI Assistant, a personal smart AI companion on an active voice call.
                                    $languageDirective
                                    Personality style: ${soul.style}. Tone: ${soul.tone}. Verbosity: ${soul.responseLength}.
                                    ${soul.customInstruction}
                                    $memoryContext
                                """.trimIndent()

                                val aiRes = aiApi.generateResponse(
                                    provider = provider,
                                    apiKey = activeKey,
                                    model = selectedModel,
                                    systemInstruction = fullSystemInstruction,
                                    prompt = query
                                )

                                aiRes.onSuccess { reply ->
                                    lastAiResponse = reply
                                    prefs.addChatMessage(ChatMessage(isUser = false, text = reply))
                                    speakAiResponse(reply)
                                }.onFailure { error ->
                                    voiceState = VoiceState.ERROR
                                    val errSpeech = when (detectedLang) {
                                        SupportedLanguage.BENGALI -> "একটি ত্রুটি হয়েছে: ${error.message ?: "অনুরোধ সম্পন্ন করা যায়নি"}"
                                        SupportedLanguage.HINDI -> "एक त्रुटि हुई: ${error.message ?: "अनुरोध पूरा नहीं हो सका"}"
                                        else -> "I encountered an error: ${error.message ?: "Could not complete request"}"
                                    }
                                    lastAiResponse = errSpeech
                                    prefs.addChatMessage(ChatMessage(isUser = false, text = errSpeech))
                                    speakAiResponse(errSpeech)
                                }
                            }
                        }

                        isProcessing = false
                    }
                }

                // Initialize SpeechManager
                DisposableEffect(Unit) {
                    speechManager = SpeechManager(
                        context = this@MainActivity,
                        onResult = { text -> handleUserUtterance(text) }
                    )
                    speechManager.isContinuousModeEnabled = continuousListening
                    speechManager.targetLanguage = selectedLanguage

                    onDispose {
                        speechManager.destroy()
                    }
                }

                // Synchronize Call Active status with SpeechManager
                LaunchedEffect(isCallActive) {
                    if (::speechManager.isInitialized) {
                        speechManager.isCallActive = isCallActive
                        if (isCallActive) {
                            if (!isMuted) {
                                speechManager.startListening()
                            }
                        } else {
                            speechManager.stopListening()
                        }
                    }
                }

                // Synchronize Mute status between Notification / Service and SpeechManager
                LaunchedEffect(isMuted) {
                    if (::speechManager.isInitialized) {
                        speechManager.isMuted = isMuted
                        if (isMuted) {
                            speechManager.stopListening()
                        } else if (isCallActive) {
                            speechManager.startListening()
                        }
                    }
                }

                LaunchedEffect(continuousListening) {
                    if (::speechManager.isInitialized) {
                        speechManager.isContinuousModeEnabled = continuousListening
                    }
                }

                LaunchedEffect(selectedLanguage) {
                    if (::speechManager.isInitialized) {
                        speechManager.targetLanguage = selectedLanguage
                    }
                }

                LaunchedEffect(selectedVoiceId, speechRate) {
                    tts.setVoiceProfile(selectedVoiceId, speechRate)
                }

                // Floating Overlay sync
                fun handleOverlayToggle(enabled: Boolean) {
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                            Toast.makeText(this@MainActivity, "Please grant overlay permission", Toast.LENGTH_LONG).show()
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${packageName}")
                            )
                            startActivity(intent)
                        } else {
                            prefs.setShowFloatingOverlay(true)
                            FloatingOrbService.start(this@MainActivity)
                        }
                    } else {
                        prefs.setShowFloatingOverlay(false)
                        FloatingOrbService.stop(this@MainActivity)
                    }
                }

                // SpeechManager state flows
                val isMicListening by if (::speechManager.isInitialized) speechManager.isListening.collectAsState() else remember { mutableStateOf(false) }
                val micAmplitude by if (::speechManager.isInitialized) speechManager.amplitude.collectAsState() else remember { mutableStateOf(0f) }
                val micStatusText by if (::speechManager.isInitialized) speechManager.currentStatus.collectAsState() else remember { mutableStateOf("Tap to speak...") }

                val currentVoiceState = when {
                    voiceState == VoiceState.PROCESSING -> VoiceState.PROCESSING
                    voiceState == VoiceState.SPEAKING -> VoiceState.SPEAKING
                    voiceState == VoiceState.EXECUTING -> VoiceState.EXECUTING
                    voiceState == VoiceState.ERROR -> VoiceState.ERROR
                    isMicListening -> VoiceState.LISTENING
                    else -> VoiceState.IDLE
                }

                // Back navigation handlers
                BackHandler(enabled = currentScreen != Screen.DASHBOARD && currentScreen != Screen.SPLASH) {
                    when (currentScreen) {
                        Screen.ONBOARDING -> finish()
                        Screen.CALL, Screen.HISTORY, Screen.SETTINGS -> currentScreen = Screen.DASHBOARD
                        Screen.CHAT, Screen.MEMORY, Screen.SOUL -> currentScreen = Screen.DASHBOARD
                        Screen.AI_SETTINGS, Screen.PERMISSIONS, Screen.VOICE, Screen.ORB, Screen.NOTIFICATIONS, Screen.ABOUT -> {
                            currentScreen = Screen.SETTINGS
                        }
                        else -> currentScreen = Screen.DASHBOARD
                    }
                }

                // Screen Rendering with 4 primary destinations
                val showBottomBar = currentScreen in listOf(Screen.DASHBOARD, Screen.CALL, Screen.HISTORY, Screen.SETTINGS)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                containerColor = DarkSurface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.DASHBOARD,
                                    onClick = { currentScreen = Screen.DASHBOARD },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Dashboard",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("ড্যাশবোর্ড", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonGreen,
                                        selectedTextColor = NeonGreen,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = NeonGreen.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_dashboard")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.CALL,
                                    onClick = { currentScreen = Screen.CALL },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Calling System",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("কলিং সিস্টেম", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonGreen,
                                        selectedTextColor = NeonGreen,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = NeonGreen.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_call")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.HISTORY,
                                    onClick = { currentScreen = Screen.HISTORY },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "History",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("হিস্ট্রি", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonGreen,
                                        selectedTextColor = NeonGreen,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = NeonGreen.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_history")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.SETTINGS,
                                    onClick = { currentScreen = Screen.SETTINGS },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("সেটিংস", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonGreen,
                                        selectedTextColor = NeonGreen,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = NeonGreen.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.SPLASH -> {
                                SplashScreen(
                                    onFinished = {
                                        currentScreen = if (onboardingDone) Screen.DASHBOARD else Screen.ONBOARDING
                                    }
                                )
                            }

                            Screen.ONBOARDING -> {
                                OnboardingScreen(
                                    onComplete = {
                                        prefs.setOnboardingCompleted(true)
                                        currentScreen = Screen.DASHBOARD
                                    }
                                )
                            }

                            Screen.DASHBOARD -> {
                                DashboardScreen(
                                    voiceState = currentVoiceState,
                                    amplitude = micAmplitude,
                                    statusText = micStatusText,
                                    lastAiResponse = lastAiResponse,
                                    currentTask = currentTask,
                                    orbType = selectedOrbType,
                                    orbOpacity = orbOpacity,
                                    isListening = isMicListening,
                                    isCallActive = isCallActive,
                                    onToggleListening = {
                                        if (isCallActive) {
                                            OngoingCallService.stop(this@MainActivity)
                                            speechManager.stopListening()
                                        } else {
                                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                OngoingCallService.start(this@MainActivity)
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                    onNavigateLiveCall = { currentScreen = Screen.CALL },
                                    onNavigateMemory = { currentScreen = Screen.MEMORY },
                                    onNavigateChat = { currentScreen = Screen.CHAT },
                                    onNavigateSoul = { currentScreen = Screen.SOUL },
                                    onNavigateSettings = { currentScreen = Screen.SETTINGS }
                                )
                            }

                            Screen.CALL -> {
                                LiveCallScreen(
                                    voiceState = currentVoiceState,
                                    amplitude = micAmplitude,
                                    statusText = micStatusText,
                                    lastAiResponse = lastAiResponse,
                                    orbType = selectedOrbType,
                                    orbOpacity = orbOpacity,
                                    isListening = isMicListening,
                                    isCallActive = isCallActive,
                                    isMuted = isMuted,
                                    isSpeakerOn = isSpeakerOn,
                                    isCameraVisionActive = isCameraVisionActive,
                                    callDurationFormatted = callDurationFormatted,
                                    cameraVisionManager = cameraVisionManager,
                                    onStartCall = {
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            OngoingCallService.start(this@MainActivity)
                                            speechManager.startListening()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    onEndCall = {
                                        OngoingCallService.stop(this@MainActivity)
                                        speechManager.stopListening()
                                        Toast.makeText(this@MainActivity, "Call ended (কল সমাপ্ত)", Toast.LENGTH_SHORT).show()
                                    },
                                    onToggleMute = {
                                        OngoingCallService.toggleMute()
                                    },
                                    onToggleSpeaker = {
                                        OngoingCallService.toggleSpeaker(this@MainActivity)
                                    },
                                    onToggleCameraVision = {
                                        if (!isCameraVisionActive) {
                                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                OngoingCallService.toggleCameraVision(true)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        } else {
                                            OngoingCallService.toggleCameraVision(false)
                                        }
                                    },
                                    onAnalyzeCameraFrame = {
                                        analyzeCurrentCameraFrame()
                                    },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }

                            Screen.CHAT -> {
                                ChatScreen(
                                    messages = chatHistory,
                                    currentTask = currentTask,
                                    isProcessing = isProcessing,
                                    isListening = isMicListening,
                                    onSendMessage = { text -> handleUserUtterance(text) },
                                    onToggleMic = {
                                        if (isCallActive) {
                                            OngoingCallService.stop(this@MainActivity)
                                            speechManager.stopListening()
                                        } else {
                                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                OngoingCallService.start(this@MainActivity)
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                    onSpeakText = { text ->
                                        tts.speak(text)
                                    },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }

                            Screen.HISTORY -> {
                                HistoryScreen(
                                    historyMessages = chatHistory,
                                    onClearHistory = { prefs.clearChatHistory() },
                                    onSelectMessage = { msg ->
                                        if (!msg.isUser) tts.speak(msg.text)
                                    }
                                )
                            }

                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    onNavigateAiSettings = { currentScreen = Screen.AI_SETTINGS },
                                    onNavigatePermissions = { currentScreen = Screen.PERMISSIONS },
                                    onNavigateVoice = { currentScreen = Screen.VOICE },
                                    onNavigateOrb = { currentScreen = Screen.ORB },
                                    onNavigateNotifications = { currentScreen = Screen.NOTIFICATIONS },
                                    onNavigateSoul = { currentScreen = Screen.SOUL },
                                    onNavigateAbout = { currentScreen = Screen.ABOUT },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }

                            Screen.AI_SETTINGS -> {
                                AiSettingsScreen(
                                    currentProvider = provider,
                                    geminiApiKey = geminiKey,
                                    openRouterApiKey = openRouterKey,
                                    selectedModel = selectedModel,
                                    modelsList = modelsList,
                                    isLoadingModels = isLoadingModels,
                                    onSelectProvider = { newProv ->
                                        prefs.setProvider(newProv)
                                    },
                                    onSaveKey = { prov, key ->
                                        if (prov == com.example.data.model.AiProvider.GEMINI) {
                                            prefs.setGeminiApiKey(key)
                                        } else {
                                            prefs.setOpenRouterApiKey(key)
                                        }
                                        Toast.makeText(this@MainActivity, "API Key saved successfully", Toast.LENGTH_SHORT).show()
                                        refreshModels()
                                    },
                                    onSelectModel = { modelId ->
                                        prefs.setSelectedModel(modelId)
                                    },
                                    onRefreshModels = { refreshModels() },
                                    onBack = { currentScreen = Screen.SETTINGS }
                                )
                            }

                            Screen.PERMISSIONS -> {
                                PermissionsScreen(
                                    onRequestPermission = { perm ->
                                        singlePermissionLauncher.launch(perm)
                                    },
                                    onBack = { currentScreen = Screen.SETTINGS }
                                )
                            }

                            Screen.VOICE -> {
                                VoiceSettingsScreen(
                                    voices = tts.availableVoices,
                                    selectedVoiceId = selectedVoiceId,
                                    currentLanguage = selectedLanguage,
                                    speechRate = speechRate,
                                    isContinuousListening = continuousListening,
                                    onSelectVoice = { vId -> prefs.setSelectedVoiceId(vId) },
                                    onSelectLanguage = { lang -> prefs.setSelectedLanguage(lang) },
                                    onChangeSpeechRate = { rate -> prefs.setSpeechRate(rate) },
                                    onToggleContinuousListening = { enabled -> prefs.setContinuousListening(enabled) },
                                    onTestVoice = { vId -> tts.testVoice(vId) },
                                    onBack = { currentScreen = Screen.SETTINGS }
                                )
                            }

                            Screen.ORB -> {
                                OrbSettingsScreen(
                                    currentOrbType = selectedOrbType,
                                    currentOpacity = orbOpacity,
                                    showOverlay = showFloatingOverlay,
                                    onSelectOrbType = { type -> prefs.setSelectedOrbType(type) },
                                    onChangeOpacity = { op -> prefs.setOrbOpacity(op) },
                                    onToggleOverlay = { enabled -> handleOverlayToggle(enabled) },
                                    onBack = { currentScreen = Screen.SETTINGS }
                                )
                            }

                            Screen.NOTIFICATIONS -> {
                                NotificationsScreen(
                                    notifications = notifications,
                                    onReadNotification = { notif ->
                                        val speech = "Notification from ${notif.appName}. ${notif.title} says: ${notif.text}"
                                        tts.speak(speech)
                                    },
                                    onExecuteVoiceCommand = { cmd ->
                                        currentScreen = Screen.CHAT
                                        handleUserUtterance(cmd)
                                    },
                                    onBack = { currentScreen = Screen.SETTINGS }
                                )
                            }

                            Screen.MEMORY -> {
                                MemoryScreen(
                                    memories = memories,
                                    onAddMemory = { k, v -> prefs.addMemory(k, v) },
                                    onDeleteMemory = { id -> prefs.deleteMemory(id) },
                                    onClearAll = { prefs.clearMemories() },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }

                            Screen.SOUL -> {
                                SoulScreen(
                                    soul = soul,
                                    onSaveSoul = { style, tone, length, instr ->
                                        prefs.updateSoul(style, tone, length, instr)
                                        Toast.makeText(this@MainActivity, "Soul personality saved", Toast.LENGTH_SHORT).show()
                                    },
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }

                            Screen.ABOUT -> {
                                AboutScreen(
                                    onBack = { currentScreen = Screen.SETTINGS }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
