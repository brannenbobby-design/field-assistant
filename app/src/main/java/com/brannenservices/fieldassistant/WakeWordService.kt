package com.brannenservices.fieldassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class WakeWordService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channelId = "field_assistant_listening"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channelId, "Field Assistant", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Field Assistant")
            .setContentText("Hands-free mode is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(42, notification)
        // V1 scaffold: persistent service is in place. A dedicated on-device wake-word
        // engine will be attached here so wake detection does not burn cloud/API usage.
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
