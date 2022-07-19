package com.gerwalex.batteryguard.system

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gerwalex.batteryguard.widget.BatteryGuardWidgetProvider

class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("gerwalex", "Broadcast received: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetProvider = ComponentName(context, BatteryGuardWidgetProvider::class.java)
                if (appWidgetManager
                        .getAppWidgetIds(widgetProvider)
                        .isNotEmpty()
                ) {
                    context.startService(Intent(context, BatteryGuardService::class.java))
                }
            }
        }
    }
}