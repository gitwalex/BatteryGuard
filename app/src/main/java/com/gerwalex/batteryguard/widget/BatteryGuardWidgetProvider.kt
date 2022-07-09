package com.gerwalex.batteryguard.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.preference.PreferenceManager
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import com.gerwalex.batteryguard.system.BatteryWidgetUpdater
import com.gerwalex.batteryguard.system.BatteryWorkerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryGuardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        try {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("gerwalex", "GuardWidgetProvider")
                val appWidgetUpdater = BatteryWidgetUpdater(context)
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                batteryStatus?.let { intent ->
                    val event = Event(BatteryEvent.UpdateWidget, intent)
                    appWidgetUpdater.updateWidget(event.level, event.isCharging)
                }
            }
        } finally {
            pending.finish()
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        BatteryWorkerService.stopService(context)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(BatteryWorkerService
                .SERVICE_REQUIRED, false)
            .apply()
        Log.d("gerwalex", "GuardWidgetProvider disabled")
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(BatteryWorkerService
                .SERVICE_REQUIRED, true)
            .apply()
        Log.d("gerwalex", "GuardWidgetProvider enabled")
    }
}