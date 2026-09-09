package com.brannenservices.fieldassistant

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FieldAccessibilityService : AccessibilityService() {
    companion object {
        const val CHATGPT_PACKAGE = "com.openai.chatgpt"
        @Volatile var connected = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // V1 controller deliberately scopes itself to ChatGPT only.
        // We will identify the current voice-control node from the user's installed
        // ChatGPT build during device testing rather than relying on screen coordinates.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        connected = false
        super.onDestroy()
    }

    fun openChatGpt(): Boolean {
        val launch = packageManager.getLaunchIntentForPackage(CHATGPT_PACKAGE) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
        return true
    }

    fun findClickableByText(root: AccessibilityNodeInfo?, vararg labels: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val wanted = labels.map { it.lowercase() }.toSet()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim()?.lowercase()
            val description = node.contentDescription?.toString()?.trim()?.lowercase()
            if (node.isClickable && (text in wanted || description in wanted)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }
}
