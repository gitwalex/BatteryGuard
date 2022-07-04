package com.gerwalex.batteryguard.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.system.BatteryObserverWorker
import com.gerwalex.lib.main.BasicActivity

class AppWidgetConfigurationActivity : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (configureWidget() != AppWidgetManager.INVALID_APPWIDGET_ID) {
            prefs
                .edit()
                .putBoolean(BatteryObserverWorker.SERVICE_REQUIRED, true)
                .commit()
            BatteryObserverWorker.startService(this)
        }
        finish()
    }

    private fun configureWidget(): Int {
        val appWidgetId =
            intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val views = RemoteViews(packageName, R.layout.appwidget_provider_layout)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
        }
        return appWidgetId
    }
}