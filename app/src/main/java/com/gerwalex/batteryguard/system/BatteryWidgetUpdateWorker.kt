package com.gerwalex.batteryguard.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.*
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.main.MainActivity
import com.gerwalex.batteryguard.widget.BatteryGuardWidgetProvider

class BatteryWidgetUpdateWorker(val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    val charging = parameters.inputData.getBoolean(CHARGING, false)
    val level = parameters.inputData.getFloat(LEVEL, 0f)
    override suspend fun doWork(): Result {
        Log.d("gerwalex", "WidgetUpdate: level: $level, charging: $charging")
        updateWidget(context)
        return Result.success()
    }

    private fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetProvider = ComponentName(context, BatteryGuardWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)
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
                setOnClickPendingIntent(R.id.levelText, pendingIntent)
            }
            // Tell the AppWidgetManager to perform an update on the current
            // widget.p
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {

        const val LEVEL = "LEVEL"
        const val CHARGING = "CHARGING"

        @JvmStatic
        fun startUpdateWidget(context: Context, event: Event?) {
            val data = Data
                .Builder()
                .putFloat(LEVEL, event?.level ?: -1f)
                .putBoolean(CHARGING, event?.isCharging ?: false)
                .build()
            val request = OneTimeWorkRequest
                .Builder(BatteryWidgetUpdateWorker::class.java)
                .setInputData(data)
                .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork("BatteryWidgetUpdateWorker", ExistingWorkPolicy.APPEND, request)
        }
    }
}