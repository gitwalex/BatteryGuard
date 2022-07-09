package com.gerwalex.batteryguard.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.ext.FloatExt.dpToPx
import com.gerwalex.batteryguard.ext.IntExt.dpToPx
import com.gerwalex.batteryguard.main.MainActivity
import com.gerwalex.batteryguard.widget.BatteryGuardWidgetProvider

class BatteryWidgetUpdater(val context: Context) {

    val appWidgetManager = AppWidgetManager.getInstance(context)
    val widgetProvider = ComponentName(context, BatteryGuardWidgetProvider::class.java)
    val mainPaint = Paint()
    val backgroundPaint = Paint()
    val margin: Int

    init {
        mainPaint.isAntiAlias = true
        mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_green_light)
        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeWidth = 5
            .dpToPx()
            .toFloat()
        backgroundPaint.isAntiAlias = true
        backgroundPaint.color = ContextCompat.getColor(context, android.R.color.darker_gray)
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 5
            .dpToPx()
            .toFloat()
        margin = 3.dpToPx() // margin should be >= strokeWidth / 2 (otherwise the arc is cut)
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
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.appwidget_provider_layout
            ).apply {
                setImageViewBitmap(R.id.circle, getBitmap(level / 100))
                setTextViewText(R.id.levelText,
                    level
                        .toInt()
                        .toString())
                setOnClickPendingIntent(R.id.levelText, pendingIntent)
            }
            // Tell the AppWidgetManager to perform an update on the current
            // widget.p
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun getBitmap(angle: Float): Bitmap? {
        val size = context.resources
            .getDimension(R.dimen.widgetSize)
            .dpToPx()
        val conf = Bitmap.Config.ARGB_8888 // see other conf types
        val bmp = Bitmap.createBitmap(size.toInt(), size.toInt(), conf) // this creates a MUTABLE
        // bitmap
        val canvas = Canvas(bmp)
        val rectangle = RectF(0f + margin, 0f + margin, size - margin, size.toFloat() - margin)
        onDraw(angle, canvas, rectangle)
        return bmp
    }

    fun onDraw(angle: Float, canvas: Canvas, rectangle: RectF) {
        canvas.drawArc(rectangle, -90f, angle * 360, false, mainPaint)
        // This 2nd arc completes the circle. Remove it if you don't want it
        canvas.drawArc(rectangle, -90f + angle * 360, (1 - angle) * 360, false, backgroundPaint)
    }
}