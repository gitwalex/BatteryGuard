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
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.*
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.ext.IntExt.dpToPx
import com.gerwalex.batteryguard.main.MainActivity
import com.gerwalex.batteryguard.widget.BatteryGuardWidgetProvider

class BatteryWidgetUpdateWorker(val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    val mainPaint = Paint()
    val backgroundPaint = Paint()
    val margin: Int
    val charging = parameters.inputData.getBoolean(CHARGING, false)
    val level = parameters.inputData.getFloat(LEVEL, 0f)

    init {
        mainPaint.isAntiAlias = true
        mainPaint.color = ContextCompat.getColor(context, android.R.color.holo_green_light)
        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeWidth = 5
            .dpToPx()
            .toFloat()
        backgroundPaint.isAntiAlias = true
        backgroundPaint.color = ContextCompat.getColor(context, R.color.white)
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 5
            .dpToPx()
            .toFloat()
        margin = 3.dpToPx() // margin should be >= strokeWidth / 2 (otherwise the arc is cut)
    }

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

    private fun getWidgetSize(): Pair<Int, Int> {
        return Pair(40.dpToPx(), 40.dpToPx())
    }

    private fun getBitmap(angle: Float): Bitmap? {
        val size = getWidgetSize()
        val conf = Bitmap.Config.ARGB_8888 // see other conf types
        val bmp = Bitmap.createBitmap(size.first, size.second, conf) // this creates a MUTABLE
        // bitmap
        val canvas = Canvas(bmp)
        val rectangle = RectF(0f + margin, 0f + margin, size.first.toFloat() - margin, size.second.toFloat() - margin)
        onDraw(angle, canvas, rectangle)
        return bmp
    }

    fun onDraw(angle: Float, canvas: Canvas, rectangle: RectF) {
        canvas.drawArc(rectangle, -90f, angle * 360, false, mainPaint)
        // This 2nd arc completes the circle. Remove it if you don't want it
        canvas.drawArc(rectangle, -90f + angle * 360, (1 - angle) * 360, false, backgroundPaint)
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