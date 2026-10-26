package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrbStyleType
import com.example.data.model.VoiceState
import com.example.ui.components.AnimatedOrb
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.LuminousYellow
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

data class OnboardingStep(
    val title: String,
    val description: String,
    val orbType: OrbStyleType
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val steps = listOf(
        OnboardingStep(
            title = "Voice Control",
            description = "Just speak, and I'll handle the rest. Your personal AI assistant is always listening.",
            orbType = OrbStyleType.NEON_EMERALD
        ),
        OnboardingStep(
            title = "Smart Actions",
            description = "Open apps, send messages, make calls, and more — all with your voice.",
            orbType = OrbStyleType.GOLDEN_CORE
        ),
        OnboardingStep(
            title = "Your AI, Your Style",
            description = "Customize your AI orb, voice, and settings to make it truly yours.",
            orbType = OrbStyleType.CELESTIAL_RING
        )
    )

    var currentStep by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize(),
            label = "onboarding_step"
        ) { stepIndex ->
            val step = steps[stepIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Central Graphic matching each onboarding reference
                when (stepIndex) {
                    0 -> {
                        // Step 1: Glowing Orb with central Mic
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                            AnimatedOrb(
                                size = 230.dp,
                                styleType = step.orbType,
                                voiceState = VoiceState.LISTENING
                            )
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkBackground.copy(alpha = 0.8f))
                                    .border(2.dp, NeonGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    1 -> {
                        // Step 2: Glowing Orb surrounded by app icons
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                            AnimatedOrb(
                                size = 160.dp,
                                styleType = step.orbType,
                                voiceState = VoiceState.EXECUTING
                            )
                            // Surrounding app icon nodes
                            val icons: List<Pair<ImageVector, Color>> = listOf(
                                Icons.AutoMirrored.Filled.Chat to NeonGreen, // WhatsApp
                                Icons.Default.VideoLibrary to Color(0xFFEF4444), // YouTube
                                Icons.Default.PlayArrow to Color(0xFF00E5FF), // TikTok
                                Icons.Default.Call to Color(0xFF10B981), // Call
                                Icons.Default.Search to Color(0xFF38BDF8), // Search
                                Icons.Default.Mic to LuminousYellow // Voice
                            )
                            val radius = 105f
                            icons.forEachIndexed { i, pair ->
                                val angle = (i * (2 * Math.PI / icons.size)).toFloat()
                                val x = radius * cos(angle)
                                val y = radius * sin(angle)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.Center)
                                        .padding(
                                            start = (x * 1.8f).coerceAtLeast(0f).dp,
                                            end = (-x * 1.8f).coerceAtLeast(0f).dp,
                                            top = (y * 1.8f).coerceAtLeast(0f).dp,
                                            bottom = (-y * 1.8f).coerceAtLeast(0f).dp
                                        )
                                        .clip(CircleShape)
                                        .background(Color(0xFF0D1824))
                                        .border(1.dp, pair.second.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = pair.first,
                                        contentDescription = null,
                                        tint = pair.second,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Step 3: Celestial rings orb
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                            AnimatedOrb(
                                size = 230.dp,
                                styleType = step.orbType,
                                voiceState = VoiceState.SPEAKING
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = step.title,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = step.description,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Bottom Navigation Bar: Skip (left), Dots (center), Next/Get Started (right)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Skip Button
            if (currentStep < 2) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("skip_button")
                ) {
                    Text(text = "Skip", color = TextSecondary, fontSize = 15.sp)
                }
            } else {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("skip_button")
                ) {
                    Text(text = "Skip", color = TextSecondary, fontSize = 15.sp)
                }
            }

            // Page Indicator Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val isActive = index == currentStep
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isActive) NeonGreen else DarkCardBorder)
                    )
                }
            }

            // Next or Get Started Button
            Button(
                onClick = {
                    if (currentStep < 2) {
                        currentStep++
                    } else {
                        onComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .testTag(if (currentStep == 2) "get_started_button" else "next_button")
                    .height(46.dp)
            ) {
                Text(
                    text = if (currentStep == 2) "Get Started" else "Next",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
