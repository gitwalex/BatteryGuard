package com.gerwalex.batteryguard.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.system.BatteryWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

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
                    val event = Event(intent, batteryManager)
                    appWidgetUpdater.updateWidget(event.level, event.isCharging)
                }
            }
        } finally {
            pending.finish()
        }
    }

    /**
     * minWidth and maxHeight are the dimensions of your widget
     * when the device is in portrait orientation,
     * maxWidth and minHeight are the dimensions when the device is in landscape orientation.
     * All in dp.
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
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
            val size: Int = if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                min(min_h, min_w)
            } else {
                min(max_h, max_w)
            }

            Log.d("gerwalex", "AppWidgetOptions: min_w=$min_w, max_w=$max_w, min_h=$min_h, max_h=$max_h, size=$size ")
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d("gerwalex", "GuardWidgetProvider disabled")
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("gerwalex", "GuardWidgetProvider enabled")
    }
}