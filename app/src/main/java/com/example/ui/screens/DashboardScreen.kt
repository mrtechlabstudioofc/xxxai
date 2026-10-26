package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.OrbStyleType
import com.example.data.model.TaskProgress
import com.example.data.model.VoiceState
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.TaskProgressCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LuminousYellow
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    voiceState: VoiceState,
    amplitude: Float,
    statusText: String,
    lastAiResponse: String,
    currentTask: TaskProgress?,
    orbType: OrbStyleType,
    orbOpacity: Float,
    isListening: Boolean,
    isCallActive: Boolean,
    onToggleListening: () -> Unit,
    onNavigateLiveCall: () -> Unit,
    onNavigateMemory: () -> Unit,
    onNavigateChat: () -> Unit,
    onNavigateSoul: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header matching Reference Images
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "MR AI Assistant Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NeonGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MR ",
                            color = CyanAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI Assistant",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isCallActive) "Live Call Active • কথা চলছে"
                        else if (isListening) "Always Listening... শুনছি"
                        else statusText,
                        color = if (isCallActive || isListening) NeonGreen else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Live Call launcher button on Top Right
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isCallActive) Color(0xFFEF4444).copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.15f))
                    .border(1.dp, if (isCallActive) Color(0xFFEF4444) else NeonGreen, RoundedCornerShape(20.dp))
                    .clickable { onNavigateLiveCall() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("dashboard_top_live_call_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Live Call",
                        tint = if (isCallActive) Color(0xFFEF4444) else NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCallActive) "ইন-কল" else "লাইভ কল",
                        color = if (isCallActive) Color(0xFFEF4444) else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Central Orb with Left Node Branches: [MEMORY], [CHAT], [SOUL], [SETTINGS]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Central 3D Particle Orb
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
            ) {
                AnimatedOrb(
                    size = 230.dp,
                    styleType = orbType,
                    voiceState = voiceState,
                    amplitude = amplitude,
                    opacity = orbOpacity
                )
            }

            // Left side 4 interactive node buttons
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OrbFeatureNodePill(
                    icon = Icons.Default.Lightbulb,
                    title = "MEMORY",
                    accentColor = CyanAccent,
                    testTag = "dashboard_memory_button",
                    onClick = onNavigateMemory
                )
                OrbFeatureNodePill(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "CHAT",
                    accentColor = LuminousYellow,
                    testTag = "dashboard_chat_button",
                    onClick = onNavigateChat
                )
                OrbFeatureNodePill(
                    icon = Icons.Default.Psychology,
                    title = "SOUL",
                    accentColor = PurpleGlow,
                    testTag = "dashboard_soul_button",
                    onClick = onNavigateSoul
                )
                OrbFeatureNodePill(
                    icon = Icons.Default.Settings,
                    title = "SETTINGS",
                    accentColor = NeonGreen,
                    testTag = "dashboard_settings_button",
                    onClick = onNavigateSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Voice Call Calling System Card (Direct shortcut into live continuous call)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(DarkSurface, Color(0xFF0F172A))
                    )
                )
                .border(1.dp, if (isCallActive) Color(0xFFEF4444).copy(alpha = 0.6f) else NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { onNavigateLiveCall() }
                .padding(14.dp)
                .testTag("dashboard_live_call_banner")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isCallActive) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                    else listOf(NeonGreen, Color(0xFF059669))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Calling System",
                            tint = DarkBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "কলিং সিস্টেম (Live Voice Call)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCallActive) "চলমান কল সক্রিয় • কথা বলতে ট্যাপ করুন" else "একটানা কথা বলতে এখানে চাপুন",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCallActive) Color(0xFFEF4444).copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isCallActive) "Open Call" else "Start Call",
                        color = if (isCallActive) Color(0xFFEF4444) else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multi-Step Task Progress component
        AnimatedVisibility(
            visible = currentTask != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            currentTask?.let { task ->
                TaskProgressCard(
                    task = task,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }

        // Tap to Speak & Microphone Interaction Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(DarkSurface)
                .border(1.dp, if (isListening) NeonGreen else DarkCardBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Microphone Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isListening) {
                                listOf(NeonGreen, Color(0xFF059669))
                            } else {
                                listOf(DarkSurfaceVariant, DarkBackground)
                            }
                        )
                    )
                    .border(2.dp, NeonGreen, CircleShape)
                    .clickable { onToggleListening() }
                    .testTag("voice_mic_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Microphone",
                    tint = if (isListening) DarkBackground else NeonGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateChat() }
            ) {
                Text(
                    text = if (isListening) "Listening to your voice..." else "Tap to speak...",
                    color = if (isListening) NeonGreen else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = lastAiResponse.ifBlank { "Hello! I am your AI Assistant. How can I help you today?" },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Plus action button to jump into chat
            IconButton(
                onClick = onNavigateChat,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("dashboard_add_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New interaction",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OrbFeatureNodePill(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}
