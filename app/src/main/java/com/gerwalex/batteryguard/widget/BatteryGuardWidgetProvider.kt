package com.gerwalex.batteryguard.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
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
        val batteryManager: BatteryManager? by lazy {
            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }
        val pending = goAsync()
        try {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("gerwalex", "GuardWidgetProvider")
                val appWidgetUpdater = BatteryWidgetUpdater(context)
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                batteryStatus?.let { intent ->
                    val event = Event(BatteryEvent.UpdateWidget, intent, batteryManager)
                    appWidgetUpdater.updateWidget(event.level, event.isCharging)
                }
            }
        } finally {
            pending.finish()
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        newOptions?.let {
            val min_w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val max_w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val min_h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val max_h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            Log.d("gerwalex", "AppWidgetOptions: min_w=$min_w, max_w=$max_w, min_h=$min_h, max_h=$max_h ")
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