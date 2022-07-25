package com.gerwalex.batteryguard.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.text.TextPaint
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.ext.FloatExt.dpToPx
import com.gerwalex.batteryguard.ext.IntExt.dpToPx
import com.gerwalex.batteryguard.main.MainActivity
import com.gerwalex.batteryguard.widget.BatteryGuardWidgetProvider
import kotlin.math.min

class BatteryWidgetUpdater(val context: Context) {

    private val pendingIntent: PendingIntent = PendingIntent.getActivity(
        /* context = */ context,
        /* requestCode = */  0,
        /* intent = */ Intent(context, MainActivity::class.java),
        /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    private val widgetBackground = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    val mTextPaint = TextPaint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        isAntiAlias = true
        textSize = 30f.dpToPx()
    }
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val widgetProvider = ComponentName(context, BatteryGuardWidgetProvider::class.java)
    private val mainPaint = Paint()
    private val backgroundPaint = Paint()

    init {
        mainPaint.isAntiAlias = true
        mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_green_light)
        mainPaint.style = Paint.Style.STROKE
        backgroundPaint.isAntiAlias = true
        backgroundPaint.color = ContextCompat.getColor(context, android.R.color.darker_gray)
        backgroundPaint.style = Paint.Style.STROKE
    }

    fun updateWidget(level: Float, isCharging: Boolean) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)
        appWidgetIds.forEach { appWidgetId ->
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            updateWidget(appWidgetId, options, level, isCharging)
        }
    }

    fun updateWidget(appWidgetId: Int, options: Bundle, level: Float, isCharging: Boolean) {
        if (isCharging) {
            mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_blue_dark)
        } else {
            when (level.toInt()) {
                in 0..30 -> mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_red_light)
                in 31..50 -> mainPaint.color =
                    ContextCompat.getColor(context, android.R.color.holo_orange_light)
                else -> mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_green_light)
            }
        }
        var size: Int = if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            min(minH, minW).dpToPx()
        } else {
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            min(maxH, maxW).dpToPx()
        }
        size = if (size == 0) 48.dpToPx() else size
        val bitmap = getBitmap(size)
        val canvas = Canvas(bitmap)
        drawBackground(canvas, size)
        drawArcs(canvas, size, level.toInt())
        drawText(canvas, size, level.toString())
        val views: RemoteViews = RemoteViews(
            context.packageName,
            R.layout.appwidget_provider_layout
        ).apply {
            setOnClickPendingIntent(R.id.widget, pendingIntent)
            setImageViewBitmap(R.id.circle, bitmap)
            setTextViewText(R.id.levelText,
                level
                    .toInt()
                    .toString())
            setTextViewTextSize(R.id.levelText, TypedValue.COMPLEX_UNIT_PX, (size / 2f))
        }
        // Tell the AppWidgetManager to perform an update on the current
        // widget.p
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getBitmap(size: Int): Bitmap {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    }

    private fun drawText(canvas: Canvas, size: Int, text: String) {
    }

    private fun drawBackground(canvas: Canvas, size: Int) {
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, widgetBackground)
    }

    private fun drawArcs(canvas: Canvas, size: Int, level: Int) {
        val angle = level / 100f
        val arcWidth = size / 9f
        val margin = arcWidth / 2 + 1
        mainPaint.strokeWidth = arcWidth
        backgroundPaint.strokeWidth = arcWidth
        val rectangle = RectF(0f + margin, 0f + margin, size.toFloat() - margin, size.toFloat() - margin)
        canvas.drawArc(rectangle, -90f, (1 - angle) * 360, false, backgroundPaint)
        // This 2nd arc completes the circle. Remove it if you don't want it
        canvas.drawArc(rectangle, -90f + (1 - angle) * 360, angle * 360, false, mainPaint)
    }
}