package com.example.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.OrbStyleType
import com.example.data.model.VoiceState
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.CameraVisionManager
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveCallScreen(
    voiceState: VoiceState,
    amplitude: Float,
    statusText: String,
    lastAiResponse: String,
    orbType: OrbStyleType,
    orbOpacity: Float,
    isListening: Boolean,
    isCallActive: Boolean,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isCameraVisionActive: Boolean,
    callDurationFormatted: String,
    cameraVisionManager: CameraVisionManager,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCameraVision: () -> Unit,
    onAnalyzeCameraFrame: () -> Unit,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation & Call Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("call_screen_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            // Connection Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, if (isCallActive) NeonGreen.copy(alpha = 0.5f) else DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCallActive && !isMuted) NeonGreen.copy(alpha = pulseGlow)
                            else if (isMuted) Color(0xFFFFB300)
                            else Color(0xFFEF4444)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCallActive) (if (isMuted) "MUTED • মিউট" else "LIVE • অবিরাম কল")
                    else "READY TO CALL",
                    color = if (isCallActive && !isMuted) NeonGreen else if (isMuted) Color(0xFFFFB300) else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Calling Partner Info & Digital Clock
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "MR AI Assistant",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCallActive) callDurationFormatted else "00:00",
                color = if (isCallActive) CyanAccent else TextSecondary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCallActive && !isMuted) "Continuous Microphone Active • অবিরাম কথা শুনছি"
                else if (isMuted) "Microphone is Muted • আনমিউট করতে ট্যাপ করুন"
                else "Tap Green Call Button below to start call",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Center: AI Orb OR Camera Vision Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (isCameraVisionActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurface)
                        .border(2.dp, NeonGreen, RoundedCornerShape(24.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                cameraVisionManager.bindCamera(lifecycleOwner, this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Camera Vision",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CAMERA VISION",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(NeonGreen)
                                .clickable { onAnalyzeCameraFrame() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("call_camera_analyze_btn")
                        ) {
                            Text(
                                text = "Analyze Frame",
                                color = DarkBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                AnimatedOrb(
                    size = 250.dp,
                    styleType = orbType,
                    voiceState = voiceState,
                    amplitude = if (isMuted || !isCallActive) 0f else amplitude,
                    opacity = orbOpacity
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-Time Transcript / Subtitle Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(1.dp, if (isListening && !isMuted) NeonGreen.copy(alpha = 0.4f) else DarkCardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isListening && !isMuted) NeonGreen.copy(alpha = 0.15f) else DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening && !isMuted) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Voice State",
                        tint = if (isListening && !isMuted) NeonGreen else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!isCallActive) "কল শুরু করতে নিচের সবুজ বাটনে চাপুন"
                        else if (isMuted) "Microphone muted (মিউট করা)"
                        else if (isListening) "শুনছি... কথা বলুন (Continuous Listening)"
                        else statusText,
                        color = if (isListening && !isMuted) NeonGreen else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lastAiResponse.ifBlank { "বলুন, আমি শুনছি..." },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // BOTTOM CONTROLS PANEL
        // Exact User Specification:
        // 1. Microphone Mute/Unmute
        // 2. Speaker Control (Default OFF, toggle for loudspeaker)
        // 3. Round Call Button: NO extra text above it. Green to start, Red when connected to end!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(32.dp))
                .padding(vertical = 18.dp, horizontal = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Microphone Mute / Unmute Button
                CallOptionButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = isMuted,
                    activeBgColor = Color(0xFFFFB300),
                    activeContentColor = DarkBackground,
                    testTag = "call_screen_mute_btn",
                    onClick = onToggleMute
                )

                // 2. Speaker Control Button (Next to mute, default OFF)
                CallOptionButton(
                    icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    label = if (isSpeakerOn) "Speaker ON" else "Speaker OFF",
                    isActive = isSpeakerOn,
                    activeBgColor = CyanAccent,
                    activeContentColor = DarkBackground,
                    testTag = "call_screen_speaker_btn",
                    onClick = onToggleSpeaker
                )

                // 3. Camera Vision Toggle Button
                CallOptionButton(
                    icon = if (isCameraVisionActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = "Vision",
                    isActive = isCameraVisionActive,
                    activeBgColor = NeonGreen.copy(alpha = 0.25f),
                    activeContentColor = NeonGreen,
                    testTag = "call_screen_camera_btn",
                    onClick = onToggleCameraVision
                )

                // 4. Main Round Call Button (NO extra text above it)
                // When disconnected -> Beautiful Green Call Button
                // When connected -> Red Call End Button
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isCallActive) {
                                    listOf(Color(0xFFEF4444), Color(0xFFDC2626)) // Red when connected
                                } else {
                                    listOf(Color(0xFF22C55E), Color(0xFF16A34A)) // Green when disconnected
                                }
                            )
                        )
                        .clickable {
                            if (isCallActive) onEndCall() else onStartCall()
                        }
                        .testTag("call_screen_main_call_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCallActive) Icons.Default.CallEnd else Icons.Default.Call,
                        contentDescription = if (isCallActive) "End Call" else "Start Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun CallOptionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeBgColor: Color,
    activeContentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isActive) activeBgColor else DarkSurfaceVariant)
                .border(1.dp, if (isActive) activeBgColor else DarkCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeContentColor else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActive) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
