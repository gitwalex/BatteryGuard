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
import com.gerwalex.batteryguard.system.BatteryWidgetUpdateWorker
import com.gerwalex.batteryguard.system.BatteryWorkerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryGuardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
// Perform this loop procedure for each widget that belongs to this
        // provider.
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("gerwalex", "GuardWidgetProvider")
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            batteryStatus?.let { intent ->
                val event = Event(BatteryEvent.UpdateWidget, intent)
                BatteryWidgetUpdateWorker.startUpdateWidget(context, event)
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(BatteryWorkerService
                .SERVICE_REQUIRED, false)
            .apply()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(BatteryWorkerService
                .SERVICE_REQUIRED, true)
            .apply()
    }
}