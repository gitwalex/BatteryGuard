package com.gerwalex.batteryguard.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.system.BatteryWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryGuardWidgetProvider : AppWidgetProvider() {

    var context: Context? = null
    val batteryManager: BatteryManager? by lazy {
        context?.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    val appWidgetUpdater by lazy { context?.let { BatteryWidgetUpdater(it) } }
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        this.context = context
        val pending = goAsync()
        try {
            CoroutineScope(Dispatchers.IO).launch {
                getEvent(context)?.let { e ->
                    appWidgetUpdater?.updateWidget(e.level, e.isCharging)
                }
            }
        } finally {
            pending.finish()
        }
    }

    private suspend fun getEvent(context: Context): Event? {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        batteryStatus?.let { intent ->
            val event = Event(intent, batteryManager)
            event.insert()
            return event
        }
        return null
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        this.context = context
        CoroutineScope(Dispatchers.IO).launch {
            getEvent(context)?.let {
                appWidgetUpdater?.updateWidget(appWidgetId, newOptions, it.level, it.isCharging)
            }
        }
    }
}