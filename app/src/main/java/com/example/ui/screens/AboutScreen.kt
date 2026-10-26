package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "About",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("about_back_button")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo & Version Card matching Image 3 Screen 9
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Official App Logo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NeonGreen, CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MR ",
                            color = CyanAccent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI Assistant",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Developer Info
            Text(
                text = "Developer",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .clickable { openUrl(context, "https://mrabout.vercel.app/") }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MR মোস্তাফিজুর রহমান",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MR TechLab Studio",
                        color = CyanAccent,
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

            // Websites
            Text(
                text = "Websites",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            AboutLinkRow(
                icon = Icons.Default.Language,
                iconColor = CyanAccent,
                title = "Website",
                url = "https://mr-techlabstudio.vercel.app",
                onClick = { openUrl(context, "https://mr-techlabstudio.vercel.app") }
            )

            AboutLinkRow(
                icon = Icons.Default.Person,
                iconColor = NeonGreen,
                title = "Developer Info",
                url = "https://mrabout.vercel.app/",
                onClick = { openUrl(context, "https://mrabout.vercel.app/") }
            )

            // Social Media
            Text(
                text = "Social Media",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            AboutLinkRow(
                icon = Icons.Default.VideoLibrary,
                iconColor = Color(0xFFEF4444),
                title = "YouTube",
                url = "https://youtube.com/@mrtechlabstudio",
                onClick = { openUrl(context, "https://youtube.com/@mrtechlabstudio") }
            )

            AboutLinkRow(
                icon = Icons.Default.ThumbUp,
                iconColor = Color(0xFF3B82F6),
                title = "Facebook",
                url = "https://www.facebook.com/mrtechlabstudio",
                onClick = { openUrl(context, "https://www.facebook.com/mrtechlabstudio") }
            )

            AboutLinkRow(
                icon = Icons.Default.PlayArrow,
                iconColor = Color(0xFFE1306C),
                title = "Instagram",
                url = "https://www.instagram.com/mrtechlabstudio/",
                onClick = { openUrl(context, "https://www.instagram.com/mrtechlabstudio/") }
            )

            AboutLinkRow(
                icon = Icons.Default.PlayArrow,
                iconColor = Color(0xFF00E5FF),
                title = "TikTok",
                url = "https://www.tiktok.com/@mr.techlab.studio",
                onClick = { openUrl(context, "https://www.tiktok.com/@mr.techlab.studio") }
            )

            // Support & Contact
            Text(
                text = "Support & Contact",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            AboutLinkRow(
                icon = Icons.Default.Email,
                iconColor = NeonGreen,
                title = "Email",
                url = "mrmostafizur198@gmail.com",
                onClick = { openEmail(context, "mrmostafizur198@gmail.com") }
            )

            AboutLinkRow(
                icon = Icons.Default.Send,
                iconColor = Color(0xFF38BDF8),
                title = "Telegram Channel",
                url = "https://t.me/mrtechlabstudio19",
                onClick = { openUrl(context, "https://t.me/mrtechlabstudio19") }
            )

            AboutLinkRow(
                icon = Icons.Default.Send,
                iconColor = Color(0xFF38BDF8),
                title = "Telegram Support",
                url = "https://t.me/mrtechlabstudiosupport",
                onClick = { openUrl(context, "https://t.me/mrtechlabstudiosupport") }
            )

            AboutLinkRow(
                icon = Icons.Default.Send,
                iconColor = Color(0xFF10B981),
                title = "WhatsApp Support Group",
                url = "https://chat.whatsapp.com/HAScEiuNmCn0TW3pZGtGnK",
                onClick = { openUrl(context, "https://chat.whatsapp.com/HAScEiuNmCn0TW3pZGtGnK") }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun openEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "MR AI Assistant Support Inquiry")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    url: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = url,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(13.dp)
        )
    }
}
