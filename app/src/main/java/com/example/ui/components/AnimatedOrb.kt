package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbStyle
import com.example.data.model.OrbStyleType
import com.example.data.model.VoiceState
import kotlin.math.cos
import kotlin.math.sin

private data class Particle(
    val theta: Float,
    val phi: Float,
    val speed: Float,
    val radiusFactor: Float,
    val size: Float,
    val colorRatio: Float
)

@Composable
fun AnimatedOrb(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    styleType: OrbStyleType = OrbStyleType.NEON_EMERALD,
    voiceState: VoiceState = VoiceState.IDLE,
    amplitude: Float = 0f,
    opacity: Float = 1.0f
) {
    val orbStyle = remember(styleType) {
        OrbStyle.ALL_STYLES.find { it.type == styleType } ?: OrbStyle.ALL_STYLES.first()
    }

    val particleCount = orbStyle.particleCount

    val particles = remember(styleType) {
        val random = java.util.Random(styleType.ordinal.toLong() + 42)
        List(particleCount) {
            Particle(
                theta = random.nextFloat() * 2f * Math.PI.toFloat(),
                phi = (random.nextFloat() - 0.5f) * Math.PI.toFloat(),
                speed = 0.5f + random.nextFloat() * 1.5f,
                radiusFactor = 0.65f + random.nextFloat() * 0.45f,
                size = 2.5f + random.nextFloat() * 4.5f,
                colorRatio = random.nextFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.LISTENING -> 4000
                    VoiceState.PROCESSING -> 2000
                    VoiceState.SPEAKING -> 3500
                    VoiceState.EXECUTING -> 2500
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val effectivePulse = breathingPulse + (amplitude * 0.25f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOrb(
                orbStyle = orbStyle,
                particles = particles,
                rotationAngle = rotationAngle,
                pulse = effectivePulse,
                voiceState = voiceState,
                amplitude = amplitude,
                globalOpacity = opacity
            )
        }
    }
}

private fun DrawScope.drawOrb(
    orbStyle: OrbStyle,
    particles: List<Particle>,
    rotationAngle: Float,
    pulse: Float,
    voiceState: VoiceState,
    amplitude: Float,
    globalOpacity: Float
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val baseRadius = (size.minDimension / 2.3f) * pulse

    val primary = when (voiceState) {
        VoiceState.ERROR -> Color(0xFFEF4444)
        else -> orbStyle.primaryColor
    }

    val secondary = when (voiceState) {
        VoiceState.ERROR -> Color(0xFFF87171)
        else -> orbStyle.secondaryColor
    }

    // 1. Central Ambient Radial Glow
    val glowRadius = baseRadius * (1.15f + amplitude * 0.35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.45f * globalOpacity),
                secondary.copy(alpha = 0.25f * globalOpacity),
                primary.copy(alpha = 0.08f * globalOpacity),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    // 2. Inner Core Sphere
    val coreRadius = baseRadius * 0.45f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f * globalOpacity),
                primary.copy(alpha = 0.7f * globalOpacity),
                secondary.copy(alpha = 0.3f * globalOpacity),
                Color.Transparent
            ),
            center = center,
            radius = coreRadius
        ),
        radius = coreRadius,
        center = center
    )

    // 3. Orbiting Particles projected in 3D
    val radRot = Math.toRadians(rotationAngle.toDouble()).toFloat()

    particles.forEach { p ->
        val currentTheta = p.theta + (radRot * p.speed)
        val r = baseRadius * p.radiusFactor

        // Spherical to 3D Cartesian coordinates
        val x3d = r * cos(p.phi.toDouble()) * cos(currentTheta.toDouble())
        val y3d = r * sin(p.phi.toDouble())
        val z3d = r * cos(p.phi.toDouble()) * sin(currentTheta.toDouble())

        // Depth perspective projection
        val perspective = 1f + (z3d.toFloat() / (baseRadius * 3f))
        val projX = center.x + (x3d.toFloat() * perspective)
        val projY = center.y + (y3d.toFloat() * perspective)

        // Depth-based alpha
        val depthAlpha = ((z3d.toFloat() + baseRadius) / (2f * baseRadius)).coerceIn(0.25f, 1f)
        val pAlpha = (depthAlpha * globalOpacity).coerceIn(0.1f, 1f)

        // Interpolate color between primary and secondary
        val pColor = if (p.colorRatio < 0.5f) {
            primary.copy(alpha = pAlpha)
        } else {
            secondary.copy(alpha = pAlpha)
        }

        val pSize = (p.size * perspective * (1f + amplitude * 0.4f)).coerceAtLeast(1.5f)

        drawCircle(
            color = pColor,
            radius = pSize,
            center = Offset(projX, projY)
        )
    }

    // 4. Outer Orbital Energy Rings for special states (Speaking, Executing)
    if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.EXECUTING) {
        val ringRadius = baseRadius * (1.1f + amplitude * 0.2f)
        drawCircle(
            color = secondary.copy(alpha = (0.35f + amplitude * 0.35f) * globalOpacity),
            radius = ringRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
        )
    }
}
