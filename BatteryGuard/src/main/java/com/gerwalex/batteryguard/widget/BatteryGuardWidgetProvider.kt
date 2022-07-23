package com.gerwalex.batteryguard.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
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
                Log.d("gerwalex", "GuardWidgetProvider")
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                batteryStatus?.let { intent ->
                    val event = Event(intent, batteryManager)
                    appWidgetUpdater?.updateWidget(event.level, event.isCharging)
                }
            }
        } finally {
            pending.finish()
        }
    }
}