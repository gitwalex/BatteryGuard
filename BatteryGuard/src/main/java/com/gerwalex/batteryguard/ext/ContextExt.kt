package com.gerwalex.batteryguard.ext

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object ContextExt {

    /**
     * Prüft, ob es zu einem Intent auch eine Activity gibt.
     *
     * @param intent Intent zum Start
     * @return true, wenn eine activity für den Intent gefunden wurde
     */
    fun Context.checkForActivity(intent: Intent): Boolean {
        return intent.resolveActivity(this.packageManager) != null
    }

    /**
     * Prüft, ob es zu einem Intent auch eine Activity gibt. Wenn ja, startet die Activity und gibt true zurück
     *
     * @param intent Intent zum Start
     * @return true, wenn eine activity gefunden und gestartet werden konnte
     */
    fun Context.startActivityWithCheck(intent: Intent): Boolean {
        if (checkForActivity(intent)) {
            startActivity(intent)
            return true
        }
        return false
    }

    /**
     * Prüft, ob Notification für diesen Context erlaubt sind
     *
     * @return true, wenn erlaubt
     */
    fun Context.areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!manager.areNotificationsEnabled()) {
                return false
            }
            val channels = manager.notificationChannels
            channels.forEach {
                if (it.importance == NotificationManager.IMPORTANCE_NONE) {
                    return false
                }
            }
            return true
        } else {
            return NotificationManagerCompat
                .from(this)
                .areNotificationsEnabled()
        }
    }
}