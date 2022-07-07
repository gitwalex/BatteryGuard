package com.gerwalex.batteryguard.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.preference.PreferenceManager
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import com.gerwalex.batteryguard.ext.IntExt.dpToPx
import com.gerwalex.batteryguard.system.BatteryWidgetUpdateWorker
import com.gerwalex.batteryguard.system.BatteryWorkerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryGuardWidgetProvider : AppWidgetProvider() {

    val mainPaint = Paint()
    val backgroundPaint = Paint()
    val margin: Int

    init {
        mainPaint.isAntiAlias = true
        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeWidth = 5
            .dpToPx()
            .toFloat()
        backgroundPaint.isAntiAlias = true
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 5
            .dpToPx()
            .toFloat()
        margin = 3.dpToPx() // margin should be >= strokeWidth / 2 (otherwise the arc is cut)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
// Perform this loop procedure for each widget that belongs to this
        // provider.
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("gerwalex", "GuardWidgetProvider")
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            batteryStatus?.let { intent ->
                val event = Event(BatteryEvent.UpdateWidget, intent)
                BatteryWidgetUpdateWorker.startUpdateWidget(context, event)
            }
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
        val rectangle =
            RectF(0f + margin, 0f + margin, size.first.toFloat() - margin, size.second.toFloat() - margin)
        onDraw(angle, canvas, rectangle)
        return bmp
    }

    fun onDraw(angle: Float, canvas: Canvas, rectangle: RectF) {
        canvas.drawArc(rectangle, -90f, angle * 360, false, mainPaint)
        // This 2nd arc completes the circle. Remove it if you don't want it
        canvas.drawArc(rectangle, -90f + angle * 360, (1 - angle) * 360, false, backgroundPaint)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(BatteryWorkerService
                .SERVICE_REQUIRED, false)
            .apply()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(BatteryWorkerService
                .SERVICE_REQUIRED, true)
            .apply()
    }
}