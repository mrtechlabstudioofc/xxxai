package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.AssistantNotificationListenerService
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.InactiveRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class AppPermissionItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val checkActive: (Context) -> Boolean,
    val openSetting: (Context) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onRequestPermission: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionItems = remember {
        listOf(
            AppPermissionItem(
                id = Manifest.permission.RECORD_AUDIO,
                name = "Microphone",
                description = "For voice commands",
                icon = Icons.Default.Mic,
                checkActive = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                },
                openSetting = { onRequestPermission(Manifest.permission.RECORD_AUDIO) }
            ),
            AppPermissionItem(
                id = Manifest.permission.CAMERA,
                name = "Camera",
                description = "For image/video analysis & torch",
                icon = Icons.Default.CameraAlt,
                checkActive = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                },
                openSetting = { onRequestPermission(Manifest.permission.CAMERA) }
            ),
            AppPermissionItem(
                id = Manifest.permission.ACCESS_FINE_LOCATION,
                name = "Location",
                description = "For location based services",
                icon = Icons.Default.LocationOn,
                checkActive = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                },
                openSetting = { onRequestPermission(Manifest.permission.ACCESS_FINE_LOCATION) }
            ),
            AppPermissionItem(
                id = "notification_listener",
                name = "Notification Access",
                description = "For reading notifications",
                icon = Icons.Default.Notifications,
                checkActive = { ctx ->
                    AssistantNotificationListenerService.hasNotificationAccess(ctx)
                },
                openSetting = { ctx ->
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                }
            ),
            AppPermissionItem(
                id = "storage",
                name = "Storage",
                description = "For file & media access",
                icon = Icons.Default.Folder,
                checkActive = { true },
                openSetting = { ctx -> openAppSettings(ctx) }
            ),
            AppPermissionItem(
                id = Manifest.permission.READ_CONTACTS,
                name = "Contacts",
                description = "For calls & messaging",
                icon = Icons.Default.Contacts,
                checkActive = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                },
                openSetting = { onRequestPermission(Manifest.permission.READ_CONTACTS) }
            ),
            AppPermissionItem(
                id = Manifest.permission.CALL_PHONE,
                name = "Phone",
                description = "For call management",
                icon = Icons.Default.Call,
                checkActive = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                },
                openSetting = { onRequestPermission(Manifest.permission.CALL_PHONE) }
            ),
            AppPermissionItem(
                id = "overlay",
                name = "Display Over Other Apps",
                description = "For Floating AI Orb",
                icon = Icons.Default.Layers,
                checkActive = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(ctx)
                    } else true
                },
                openSetting = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                    }
                }
            )
        )
    }

    val statusMap = remember { mutableStateMapOf<String, Boolean>() }

    fun refreshStatus() {
        permissionItems.forEach { item ->
            statusMap[item.id] = item.checkActive(context)
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Permissions",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("permissions_back_button")
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
            Text(
                text = "Required permissions for full functionality",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            permissionItems.forEach { item ->
                val isActive = statusMap[item.id] ?: false
                PermissionRow(
                    item = item,
                    isActive = isActive,
                    onClick = {
                        item.openSetting(context)
                        refreshStatus()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { openAppSettings(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_system_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = NeonGreen
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
private fun PermissionRow(
    item: AppPermissionItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isActive) ActiveGreen.copy(alpha = 0.15f) else DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = if (isActive) ActiveGreen else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.description,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Active / Inactive Badge matching Image 3 Screen 6
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isActive) ActiveGreen.copy(alpha = 0.15f) else InactiveRed.copy(alpha = 0.15f))
                .border(
                    1.dp,
                    if (isActive) ActiveGreen.copy(alpha = 0.4f) else InactiveRed.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isActive) "Active" else "Inactive",
                color = if (isActive) ActiveGreen else InactiveRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
