package com.brannenservices.fieldassistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FieldAccessibilityService : AccessibilityService() {
    companion object {
        const val CHATGPT_PACKAGE = "com.openai.chatgpt"
        const val PREFS = "field_assistant_prefs"
        const val KEY_START_VOICE = "start_chatgpt_voice"
        @Volatile var connected = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != CHATGPT_PACKAGE) return

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_START_VOICE, false)) return

        val root = rootInActiveWindow ?: return
        val voice = findClickableByText(
            root,
            "voice",
            "voice mode",
            "start voice",
            "start voice mode",
            "open voice mode"
        ) ?: return

        if (voice.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            prefs.edit().putBoolean(KEY_START_VOICE, false).apply()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        connected = false
        super.onDestroy()
    }

    private fun findClickableByText(root: AccessibilityNodeInfo?, vararg labels: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val wanted = labels.map { it.lowercase() }.toSet()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim()?.lowercase()
            val description = node.contentDescription?.toString()?.trim()?.lowercase()
            if (node.isClickable && (text in wanted || description in wanted)) return node

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::add)
            }
        }
        return null
    }
}
