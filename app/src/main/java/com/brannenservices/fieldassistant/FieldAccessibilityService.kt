package com.brannenservices.fieldassistant

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FieldAccessibilityService : AccessibilityService() {
    companion object {
        const val CHATGPT_PACKAGE = "com.openai.chatgpt"
        const val PREFS = "field_assistant_prefs"
        const val KEY_PENDING_TEXT = "pending_chatgpt_text"
        @Volatile var connected = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != CHATGPT_PACKAGE) return

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val pending = prefs.getString(KEY_PENDING_TEXT, null)?.trim().orEmpty()
        if (pending.isBlank()) return

        val root = rootInActiveWindow ?: return
        val editor = findEditableNode(root) ?: return
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pending)
        }

        if (!editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return

        val send = findClickableByText(root, "send", "send message") ?: return
        if (send.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            prefs.edit().remove(KEY_PENDING_TEXT).apply()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        connected = false
        super.onDestroy()
    }

    private fun findEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isEditable && node.isEnabled) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
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
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }
}
