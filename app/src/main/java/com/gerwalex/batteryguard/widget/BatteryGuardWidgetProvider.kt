package com.gerwalex.batteryguard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import android.widget.RemoteViews
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.DB.dao
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import com.gerwalex.batteryguard.main.MainActivity
import com.gerwalex.batteryguard.system.BatteryObserverWorker
import kotlinx.coroutines.*

class BatteryGuardWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("gerwalex", "Broadcast received: ${intent.action}")
        var event: Event? = null
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                BatteryObserverWorker.startService(context)
                event = BatteryObserverWorker.newEvent(BatteryEvent.Boot_Completed, context)
                updateWidget(context)
            }
            Intent.ACTION_SCREEN_OFF -> {
                if (BatteryObserverWorker.IS_SCREEN_ON.get()) {
                    event = BatteryObserverWorker.newEvent(BatteryEvent.ScreenOff, context)
                    BatteryObserverWorker.IS_SCREEN_ON.set(false)
                }
            }
            Intent.ACTION_SCREEN_ON -> {
                if (!BatteryObserverWorker.IS_SCREEN_ON.get()) {
                    event = BatteryObserverWorker.newEvent(BatteryEvent.ScreenOn, context)
                    BatteryObserverWorker.IS_SCREEN_ON.set(true)
                    updateWidget(context)
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                if (!BatteryObserverWorker.IS_AC_PLUGGED.get()) {
                    // How are we charging?
                    val chargePlug: Int =
                        intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_STATUS_UNKNOWN)
                    val plugged = when (chargePlug) {
                        BatteryManager.BATTERY_PLUGGED_AC -> BatteryEvent.Plugged_AC
                        BatteryManager.BATTERY_PLUGGED_USB -> BatteryEvent.Plugged_USB
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryEvent.Plugged_Wireless
                        else -> BatteryEvent.Plugged_Unknown
                    }
                    event = BatteryObserverWorker.newEvent(plugged, context)
                    BatteryObserverWorker.IS_AC_PLUGGED.set(true)
                    updateWidget(context)
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                if (BatteryObserverWorker.IS_AC_PLUGGED.get()) {
                    event = BatteryObserverWorker.newEvent(BatteryEvent.UnPlugged, context)
                    BatteryObserverWorker.IS_AC_PLUGGED.set(false)
                    updateWidget(context)
                }
            }
            Intent.ACTION_BATTERY_LOW -> {
                event = BatteryObserverWorker.newEvent(BatteryEvent.Battery_Low, context)
                updateWidget(context)
            }
            Intent.ACTION_BATTERY_OKAY -> {
                event = BatteryObserverWorker.newEvent(BatteryEvent.Battery_Ok, context)
                updateWidget(context)
            }
            else -> super.onReceive(context, intent)
        }
        event?.let {
            CoroutineScope(Dispatchers.IO).launch {
                it.insert()
            }
        }
    }

    private fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetProvider = ComponentName(context, this::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)
        if (appWidgetIds.isNotEmpty()) {
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
// Perform this loop procedure for each widget that belongs to this
        // provider.
        MainScope().launch {
            val level: Long
            withContext(Dispatchers.IO) {
                level = dao.getBatteryLevel()
            }
            appWidgetIds.forEach { appWidgetId ->
                // Create an Intent to launch ExampleActivity.
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */  0,
                    /* intent = */ Intent(context, MainActivity::class.java),
                    /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                // Get the layout for the widget and attach an on-click listener
                // to the button.
                val views: RemoteViews = RemoteViews(
                    context.packageName,
                    R.layout.appwidget_provider_layout
                ).apply {
                    setTextViewText(R.id.levelText, level.toString())
                }
                // Tell the AppWidgetManager to perform an update on the current
                // widget.
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}