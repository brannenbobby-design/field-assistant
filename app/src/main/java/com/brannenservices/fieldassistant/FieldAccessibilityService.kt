package com.brannenservices.fieldassistant

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class FieldAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {
    companion object {
        const val CHATGPT_PACKAGE = "com.openai.chatgpt"
        const val PREFS = "field_assistant_prefs"
        const val KEY_PENDING_TEXT = "pending_chatgpt_text"
        private const val KEY_LAST_PROMPT = "last_prompt"
        private const val KEY_AWAITING_REPLY = "awaiting_reply"
        @Volatile var connected = false
    }

    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var candidateReply = ""

    private val speakWhenStable = Runnable {
        val reply = candidateReply.trim()
        if (reply.isBlank()) return@Runnable
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_AWAITING_REPLY, false)) return@Runnable
        prefs.edit().putBoolean(KEY_AWAITING_REPLY, false).apply()
        tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "chatgpt_reply")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
        tts = TextToSpeech(this, this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != CHATGPT_PACKAGE) return
        val root = rootInActiveWindow ?: return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        val pending = prefs.getString(KEY_PENDING_TEXT, null)?.trim().orEmpty()
        if (pending.isNotBlank()) {
            val editor = findEditableNode(root) ?: return
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pending)
            }
            if (!editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return

            val send = findClickableByText(root, "send", "send message") ?: return
            if (send.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                prefs.edit()
                    .remove(KEY_PENDING_TEXT)
                    .putString(KEY_LAST_PROMPT, pending)
                    .putBoolean(KEY_AWAITING_REPLY, true)
                    .apply()
                candidateReply = ""
            }
            return
        }

        if (!prefs.getBoolean(KEY_AWAITING_REPLY, false)) return
        val prompt = prefs.getString(KEY_LAST_PROMPT, "").orEmpty()
        val reply = findLikelyReply(root, prompt)
        if (reply.isBlank()) return

        candidateReply = reply
        handler.removeCallbacks(speakWhenStable)
        handler.postDelayed(speakWhenStable, 1400)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        connected = false
        handler.removeCallbacksAndMessages(null)
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    private fun findLikelyReply(root: AccessibilityNodeInfo?, prompt: String): String {
        if (root == null) return ""
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val value = node.text?.toString()?.trim().orEmpty()
            if (value.length >= 3 && value != prompt && !isUiLabel(value)) texts.add(value)
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }

        return texts.lastOrNull { it.length >= 12 }.orEmpty()
    }

    private fun isUiLabel(text: String): Boolean {
        val value = text.lowercase(Locale.US)
        return value in setOf(
            "send", "attach", "voice", "stop", "copy", "edit", "share", "regenerate",
            "read aloud", "good response", "bad response", "chatgpt", "new chat"
        )
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
