package com.example.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.model.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        Log.i("NotificationListener", "Notification listener connected")
        fetchActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processStatusBarNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val current = _notifications.value.toMutableList()
        current.removeAll { it.id == sbn.key }
        _notifications.value = current
    }

    private fun fetchActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            val list = mutableListOf<AppNotification>()
            for (sbn in active) {
                val item = convertToAppNotification(sbn)
                if (item != null) list.add(item)
            }
            if (list.isNotEmpty()) {
                _notifications.value = list
            }
        } catch (e: Exception) {
            Log.e("NotificationListener", "Error fetching active notifications: ${e.message}")
        }
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification) {
        val item = convertToAppNotification(sbn) ?: return
        val current = _notifications.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        // Keep up to 50
        _notifications.value = if (current.size > 50) current.take(50) else current
    }

    private fun convertToAppNotification(sbn: StatusBarNotification): AppNotification? {
        val pkg = sbn.packageName ?: return null
        // Skip system noise
        if (pkg == "android" || pkg == "com.android.systemui") return null

        val extras = sbn.notification?.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        if (title.isBlank() && text.isBlank()) return null

        val appName = try {
            val pm = applicationContext.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }

        return AppNotification(
            id = sbn.key ?: "${pkg}_${sbn.id}",
            packageName = pkg,
            appName = appName,
            title = title,
            text = text,
            timestampMillis = sbn.postTime
        )
    }

    companion object {
        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        // Sample initial notifications matching Image 3 Screen 10
        private val initialSampleNotifications = listOf(
            AppNotification(
                id = "sample_1",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Rahul",
                text = "Hey, are you available?",
                timestampMillis = System.currentTimeMillis() - 2 * 60 * 1000
            ),
            AppNotification(
                id = "sample_2",
                packageName = "com.facebook.orca",
                appName = "Messenger",
                title = "Sadia",
                text = "Call me when you're free.",
                timestampMillis = System.currentTimeMillis() - 5 * 60 * 1000
            ),
            AppNotification(
                id = "sample_3",
                packageName = "com.google.android.gm",
                appName = "Gmail",
                title = "Project Update",
                text = "Meeting at 4 PM tomorrow with the engineering team.",
                timestampMillis = System.currentTimeMillis() - 12 * 60 * 1000
            )
        )

        private val _notifications = MutableStateFlow(initialSampleNotifications)
        val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

        fun clearAll() {
            _notifications.value = emptyList()
        }

        fun hasNotificationAccess(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            val componentName = "${context.packageName}/${AssistantNotificationListenerService::class.java.name}"
            return enabledListeners?.contains(componentName) == true
        }
    }
}
