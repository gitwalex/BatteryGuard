package com.gerwalex.batteryguard.ext

import android.app.NotificationManager
import android.os.Build

object NotificationManagerCompatExt {

    fun NotificationManager.areNotificationsEnabled() = when {
        areNotificationsEnabled().not() -> false
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        -> {
            notificationChannels.firstOrNull { channel -> channel.importance == NotificationManager.IMPORTANCE_NONE } == null
        }
        else -> true
    }
}