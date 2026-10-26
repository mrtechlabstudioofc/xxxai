package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OngoingCallService : Service() {

    companion object {
        const val CHANNEL_ID = "mr_ai_ongoing_call_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_CALL = "com.example.action.START_CALL"
        const val ACTION_END_CALL = "com.example.action.END_CALL"
        const val ACTION_TOGGLE_MUTE = "com.example.action.TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.example.action.TOGGLE_SPEAKER"

        private val _isCallActive = MutableStateFlow(false)
        val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _isSpeakerActive = MutableStateFlow(false)
        val isSpeakerActive: StateFlow<Boolean> = _isSpeakerActive.asStateFlow()

        private val _isCameraVisionActive = MutableStateFlow(false)
        val isCameraVisionActive: StateFlow<Boolean> = _isCameraVisionActive.asStateFlow()

        private val _callDurationSeconds = MutableStateFlow(0L)
        val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

        private val _callDurationFormatted = MutableStateFlow("00:00")
        val callDurationFormatted: StateFlow<String> = _callDurationFormatted.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, OngoingCallService::class.java).apply {
                action = ACTION_START_CALL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OngoingCallService::class.java).apply {
                action = ACTION_END_CALL
            }
            context.startService(intent)
        }

        fun toggleMute() {
            _isMuted.value = !_isMuted.value
        }

        fun toggleSpeaker(context: Context? = null) {
            _isSpeakerActive.value = !_isSpeakerActive.value
            context?.let { applyAudioRouting(it, _isSpeakerActive.value) }
        }

        fun applyAudioRouting(context: Context, isLoudspeakerOn: Boolean) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                audioManager?.let { am ->
                    am.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
                    am.isSpeakerphoneOn = isLoudspeakerOn
                }
            } catch (e: Exception) {
                android.util.Log.e("OngoingCallService", "Failed to apply audio routing: ${e.message}")
            }
        }

        fun toggleCameraVision(active: Boolean? = null) {
            _isCameraVisionActive.value = active ?: !_isCameraVisionActive.value
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var startTimeMillis = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                handleStartCall()
            }
            ACTION_END_CALL -> {
                handleEndCall()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MUTE -> {
                toggleMute()
                updateNotification()
            }
            ACTION_TOGGLE_SPEAKER -> {
                toggleSpeaker(this)
                updateNotification()
            }
        }
        return START_STICKY
    }

    private fun handleStartCall() {
        _isCallActive.value = true
        _isMuted.value = false
        _isSpeakerActive.value = false // Default OFF as requested
        applyAudioRouting(this, false)
        _callDurationSeconds.value = 0L
        _callDurationFormatted.value = "00:00"
        startTimeMillis = System.currentTimeMillis()

        startForeground(NOTIFICATION_ID, buildNotification())

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _isCallActive.value) {
                delay(1000)
                val elapsedSec = (System.currentTimeMillis() - startTimeMillis) / 1000
                _callDurationSeconds.value = elapsedSec

                val minutes = elapsedSec / 60
                val seconds = elapsedSec % 60
                val formatted = String.format("%02d:%02d", minutes, seconds)
                _callDurationFormatted.value = formatted

                // Update notification text every 5 seconds to conserve battery
                if (elapsedSec % 5L == 0L) {
                    updateNotification()
                }
            }
        }
    }

    private fun handleEndCall() {
        _isCallActive.value = false
        _isCameraVisionActive.value = false
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endCallIntent = Intent(this, OngoingCallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            this,
            1,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, OngoingCallService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            2,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteTitle = if (_isMuted.value) "Unmute (আনমিউট)" else "Mute (মিউট)"
        val statusSubText = if (_isMuted.value) "Microphone Muted" else "Microphone Live"
        val duration = _callDurationFormatted.value

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MR AI Ongoing Voice Call")
            .setContentText("$statusSubText • $duration")
            .setSubText(duration)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF00E5FF.toInt())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call (কল শেষ)",
                endCallPendingIntent
            )
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                muteTitle,
                mutePendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MR AI Ongoing Call",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status and controls for active MR AI voice session"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isCallActive.value = false
        _isCameraVisionActive.value = false
        timerJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
