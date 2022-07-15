package com.gerwalex.batteryguard.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.ext.IntExt.dpToPx
import com.gerwalex.batteryguard.main.MainActivity
import com.gerwalex.batteryguard.widget.BatteryGuardWidgetProvider
import kotlin.math.min

class BatteryWidgetUpdater(val context: Context) {

    var widgetSize: Int = 48.dpToPx()
    private val textSize: Float = 30f
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val widgetProvider = ComponentName(context, BatteryGuardWidgetProvider::class.java)
    val mainPaint = Paint()
    val backgroundPaint = Paint()
    val margin: Int

    init {
        mainPaint.isAntiAlias = true
        mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_green_light)
        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeWidth = 2
            .dpToPx()
            .dpToPx()
            .toFloat()
        backgroundPaint.isAntiAlias = true
        backgroundPaint.color = ContextCompat.getColor(context, android.R.color.darker_gray)
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 3
            .dpToPx()
            .dpToPx()
            .toFloat()
        margin = 2.dpToPx() // margin should be >= strokeWidth / 2 (otherwise the arc is cut)
    }

    fun updateWidget(level: Float, isCharging: Boolean) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)
        if (isCharging) {
            mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        } else {
            when (level.toInt()) {
                in 0..30 -> mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_red_light)
                in 31..50 -> mainPaint.color =
                    ContextCompat.getColor(context, android.R.color.holo_orange_light)
                else -> mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_green_light)
            }
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
            val bitmap = getBitmap(widgetSize, level / 100)
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.appwidget_provider_layout
            ).apply {
                setImageViewBitmap(R.id.circle, bitmap)
                setTextViewText(R.id.levelText,
                    level
                        .toInt()
                        .toString())
                setOnClickPendingIntent(R.id.levelText, pendingIntent)
                setTextViewTextSize(R.id.levelText, TypedValue.COMPLEX_UNIT_SP, textSize)
            }
            // Tell the AppWidgetManager to perform an update on the current
            // widget.p
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    /**
     *
     * @return appWidgetSize in dp
     */
    private fun getSize(appWidgetId: Int): Int {
        val newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
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
        return if (size == 0) 48 else size
    }

    private fun getBitmap(size: Int, angle: Float): Bitmap {
        val conf = Bitmap.Config.ARGB_8888 // see other conf types
        val bmp = Bitmap.createBitmap(size, size, conf) // this creates a MUTABLE bitmap
        val canvas = Canvas(bmp)
        val rectangle = RectF(0f + margin, 0f + margin, size.toFloat() - margin, size.toFloat() - margin)
        canvas.drawArc(rectangle, -90f, angle * 360, false, mainPaint)
        // This 2nd arc completes the circle. Remove it if you don't want it
        canvas.drawArc(rectangle, -90f + angle * 360, (1 - angle) * 360, false, backgroundPaint)
        return bmp
    }
}