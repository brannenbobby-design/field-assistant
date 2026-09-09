package com.brannenservices.fieldassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var handsFreeButton: Button
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var handsFreeRunning = false
    private val assistant = AssistantClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        requestMic()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 64, 48, 64)
            setBackgroundColor(0xFF0B0D10.toInt())
        }
        val title = TextView(this).apply {
            text = "FIELD ASSISTANT"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Ready"
            textSize = 18f
            setTextColor(0xFF9CA3AF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        transcript = TextView(this).apply {
            text = "Tap TALK and ask anything."
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }
        val talk = Button(this).apply {
            text = "TALK"
            textSize = 22f
            setOnClickListener { listenOnce() }
        }
        handsFreeButton = Button(this).apply {
            text = "START HANDS-FREE MODE"
            setOnClickListener { toggleHandsFree() }
        }
        root.addView(title)
        root.addView(status)
        root.addView(transcript)
        root.addView(talk)
        root.addView(handsFreeButton)
        setContentView(root)
    }

    private fun toggleHandsFree() {
        if (!handsFreeRunning) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestMic()
                status.text = "Microphone permission is required"
                return
            }
            ContextCompat.startForegroundService(this, Intent(this, WakeWordService::class.java))
            handsFreeRunning = true
            handsFreeButton.text = "STOP HANDS-FREE MODE"
            status.text = "Hands-free mode active — safe to lock screen"
        } else {
            stopService(Intent(this, WakeWordService::class.java))
            handsFreeRunning = false
            handsFreeButton.text = "START HANDS-FREE MODE"
            status.text = "Hands-free mode stopped"
        }
    }

    private fun requestMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun listenOnce() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition unavailable"
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { status.text = "Thinking…" }
                override fun onError(error: Int) { status.text = "Didn't catch that. Tap TALK and try again." }
                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    transcript.text = words.ifBlank { "No speech detected" }
                    if (words.isNotBlank()) askAssistant(words)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            })
        }
    }

    private fun askAssistant(question: String) {
        status.text = "Asking Field Assistant…"
        assistant.ask(question) { result ->
            runOnUiThread {
                result.onSuccess { reply ->
                    transcript.text = reply
                    status.text = "Ready"
                    speak(reply)
                }.onFailure { error -> status.text = error.message ?: "Assistant connection failed" }
            }
        }
    }

    private fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "field_reply") }
    override fun onInit(result: Int) { if (result == TextToSpeech.SUCCESS) tts?.language = Locale.US }
    override fun onDestroy() { recognizer?.destroy(); tts?.shutdown(); super.onDestroy() }
}
