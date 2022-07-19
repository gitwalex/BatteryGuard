package com.gerwalex.batteryguard.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.DB

class App : com.gerwalex.lib.main.App() {

    val CHANNELID by lazy {
        getString(R.string.notification_channel_id)
    }

    override fun onCreate() {
        super.onCreate()
        DB.createInstance(this)

        createChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        // Create a Notification channel
        val name = applicationContext.getString(R.string.channel_name)
        val descriptionText = applicationContext.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                    NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNELID, name, importance).apply {
                description = descriptionText
                // Register the channel with the system
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}