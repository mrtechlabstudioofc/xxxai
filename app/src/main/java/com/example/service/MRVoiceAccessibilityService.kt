package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MRVoiceAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MRVoiceAccessibility"
        var instance: MRVoiceAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expectedComponentName = "${context.packageName}/${MRVoiceAccessibilityService::class.java.canonicalName}"
            return enabledServices.contains(expectedComponentName)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.d(TAG, "MRVoiceAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events can be logged or tracked for active app detection
    }

    override fun onInterrupt() {
        Log.w(TAG, "MRVoiceAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceConnected.value = false
        }
    }

    // GLOBAL SYSTEM NAVIGATION
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotificationPanel(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    // GESTURE 1: Scroll Down (Swipe up from bottom to top)
    fun scrollDown(callback: ((Boolean) -> Unit)? = null) {
        val dm = resources.displayMetrics
        val width = dm.widthPixels.toFloat()
        val height = dm.heightPixels.toFloat()

        val startX = width / 2f
        val startY = height * 0.75f
        val endX = width / 2f
        val endY = height * 0.25f

        dispatchSwipe(startX, startY, endX, endY, 350L, callback)
    }

    // GESTURE 2: Scroll Up (Swipe down from top to bottom)
    fun scrollUp(callback: ((Boolean) -> Unit)? = null) {
        val dm = resources.displayMetrics
        val width = dm.widthPixels.toFloat()
        val height = dm.heightPixels.toFloat()

        val startX = width / 2f
        val startY = height * 0.25f
        val endX = width / 2f
        val endY = height * 0.75f

        dispatchSwipe(startX, startY, endX, endY, 350L, callback)
    }

    // GESTURE 3: Click First Post in feed
    fun clickFirstPost(callback: ((Boolean) -> Unit)? = null): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val clickables = mutableListOf<AccessibilityNodeInfo>()
            findClickableNodes(root, clickables, maxNodes = 15)
            // Filter out system bars / headers, pick first content node
            val target = clickables.firstOrNull { node ->
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                bounds.top > 150 && bounds.height() > 80
            }
            if (target != null) {
                val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    callback?.invoke(true)
                    return true
                }
            }
        }

        // Fallback coordinate click in upper feed area
        val dm = resources.displayMetrics
        return dispatchTap(dm.widthPixels * 0.5f, dm.heightPixels * 0.35f, callback)
    }

    // GESTURE 4: Click Second Post in feed
    fun clickSecondPost(callback: ((Boolean) -> Unit)? = null): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val clickables = mutableListOf<AccessibilityNodeInfo>()
            findClickableNodes(root, clickables, maxNodes = 25)
            val candidates = clickables.filter { node ->
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                bounds.top > 300 && bounds.height() > 80
            }
            if (candidates.size >= 2) {
                val target = candidates[1]
                val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    callback?.invoke(true)
                    return true
                }
            }
        }

        val dm = resources.displayMetrics
        return dispatchTap(dm.widthPixels * 0.5f, dm.heightPixels * 0.65f, callback)
    }

    // GESTURE 5: Like Post or Video (Searches for Like button or double-taps for TikTok/Instagram)
    fun clickLike(callback: ((Boolean) -> Unit)? = null): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val likeNode = findNodeMatching(root) { node ->
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                val viewId = node.viewIdResourceName?.lowercase() ?: ""

                desc.contains("like") || desc.contains("love") || desc.contains("পছন্দ") || desc.contains("লাইক") ||
                        text.contains("like") || text.contains("love") || text.contains("লাইক") ||
                        viewId.contains("like") || viewId.contains("love") || viewId.contains("heart")
            }

            if (likeNode != null) {
                val success = performClickOrParent(likeNode)
                if (success) {
                    callback?.invoke(true)
                    return true
                }
            }
        }

        // Fallback for short-form video apps (TikTok, Reels): Double tap center of screen
        val dm = resources.displayMetrics
        val centerX = dm.widthPixels * 0.5f
        val centerY = dm.heightPixels * 0.5f
        return dispatchDoubleTap(centerX, centerY, callback)
    }

    // GESTURE 6: Click Comment
    fun clickComment(callback: ((Boolean) -> Unit)? = null): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val commentNode = findNodeMatching(root) { node ->
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                val viewId = node.viewIdResourceName?.lowercase() ?: ""

                desc.contains("comment") || desc.contains("মন্তব্য") ||
                        text.contains("comment") || text.contains("মন্তব্য") ||
                        viewId.contains("comment")
            }
            if (commentNode != null) {
                val success = performClickOrParent(commentNode)
                if (success) {
                    callback?.invoke(true)
                    return true
                }
            }
        }
        val dm = resources.displayMetrics
        return dispatchTap(dm.widthPixels * 0.88f, dm.heightPixels * 0.62f, callback)
    }

    // GESTURE 7: Auto-Type and Send Message in Active Chat
    fun typeAndSendMessage(text: String, callback: ((Boolean) -> Unit)? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        // 1. Find editable text field
        val editField = findNodeMatching(root) { node ->
            node.isEditable || node.className?.contains("EditText") == true
        }

        if (editField != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val textSet = editField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (textSet) {
                // Find and click the send button
                val sendNode = findNodeMatching(root) { node ->
                    val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                    val t = node.text?.toString()?.lowercase() ?: ""
                    val vId = node.viewIdResourceName?.lowercase() ?: ""

                    desc.contains("send") || desc.contains("পাঠাও") || desc.contains("भेजें") ||
                            t.contains("send") || t.contains("পাঠাও") ||
                            vId.contains("send") || vId.contains("button_send")
                }

                if (sendNode != null) {
                    performClickOrParent(sendNode)
                } else {
                    // Try pressing enter / clicking right edge of edit field
                    val bounds = Rect()
                    editField.getBoundsInScreen(bounds)
                    dispatchTap(bounds.right - 40f, bounds.centerY().toFloat(), null)
                }

                callback?.invoke(true)
                return true
            }
        }

        callback?.invoke(false)
        return false
    }

    // GESTURE 8: Read Messages and Text currently displayed on screen
    fun readScreenMessages(): String {
        val root = rootInActiveWindow ?: return "স্ক্রিনের কোনো বার্তা খুঁজে পাওয়া যায়নি।"
        val textSnippets = mutableListOf<String>()
        collectScreenText(root, textSnippets)
        return if (textSnippets.isNotEmpty()) {
            textSnippets.takeLast(10).joinToString("\n")
        } else {
            "স্ক্রিনে কোনো পাঠ্য বা মেসেজ দেখা যাচ্ছে না।"
        }
    }

    private fun collectScreenText(node: AccessibilityNodeInfo, list: MutableList<String>) {
        val t = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!t.isNullOrBlank() && t.length > 1 && !list.contains(t)) {
            list.add(t)
        } else if (!desc.isNullOrBlank() && desc.length > 1 && !list.contains(desc)) {
            list.add(desc)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectScreenText(child, list)
        }
    }

    private fun findClickableNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>, maxNodes: Int) {
        if (result.size >= maxNodes) return
        if (node.isClickable) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findClickableNodes(child, result, maxNodes)
        }
    }

    private fun findNodeMatching(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeMatching(child, predicate)
            if (found != null) return found
        }
        return null
    }

    private fun performClickOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            parent = parent.parent
        }
        return false
    }

    private fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long, callback: ((Boolean) -> Unit)?) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                callback?.invoke(false)
            }
        }, null)
    }

    private fun dispatchTap(x: Float, y: Float, callback: ((Boolean) -> Unit)?): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                callback?.invoke(false)
            }
        }, null)
    }

    private fun dispatchDoubleTap(x: Float, y: Float, callback: ((Boolean) -> Unit)?): Boolean {
        val path1 = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path1, 0, 50)
        val stroke2 = GestureDescription.StrokeDescription(path1, 150, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                callback?.invoke(false)
            }
        }, null)
    }
}
