package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateAiSettings: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onNavigateVoice: () -> Unit,
    onNavigateOrb: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateSoul: () -> Unit,
    onNavigateAbout: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            SettingsNavRow(
                icon = Icons.Default.AutoAwesome,
                iconColor = NeonGreen,
                title = "AI",
                subtitle = "API, Models, Voice, Orb",
                testTag = "settings_ai_item",
                onClick = onNavigateAiSettings
            )

            SettingsNavRow(
                icon = Icons.Default.Lock,
                iconColor = CyanAccent,
                title = "Permissions",
                subtitle = "Manage app permissions",
                testTag = "settings_permissions_item",
                onClick = onNavigatePermissions
            )

            SettingsNavRow(
                icon = Icons.Default.Mic,
                iconColor = LuminousYellow,
                title = "Voice",
                subtitle = "Voice selection & settings",
                testTag = "settings_voice_item",
                onClick = onNavigateVoice
            )

            SettingsNavRow(
                icon = Icons.Default.Radio,
                iconColor = NeonGreen,
                title = "Orb",
                subtitle = "AI Orb design & behavior",
                testTag = "settings_orb_item",
                onClick = onNavigateOrb
            )

            SettingsNavRow(
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFFF97316),
                title = "Notifications",
                subtitle = "Notification settings",
                testTag = "settings_notifications_item",
                onClick = onNavigateNotifications
            )

            SettingsNavRow(
                icon = Icons.Default.Psychology,
                iconColor = PurpleGlow,
                title = "Soul & Personality",
                subtitle = "App preferences & style",
                testTag = "settings_soul_item",
                onClick = onNavigateSoul
            )

            SettingsNavRow(
                icon = Icons.Default.Info,
                iconColor = Color(0xFF38BDF8),
                title = "About",
                subtitle = "App info & links",
                testTag = "settings_about_item",
                onClick = onNavigateAbout
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}
