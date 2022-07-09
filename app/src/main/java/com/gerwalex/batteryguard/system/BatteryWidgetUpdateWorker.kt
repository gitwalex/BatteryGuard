package com.gerwalex.batteryguard.system

import android.content.Context
import android.util.Log
import androidx.work.*
import com.gerwalex.batteryguard.database.tables.Event

class BatteryWidgetUpdateWorker(val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val appWidgetUpdater = BatteryWidgetUpdater(context)
    private val isCharging = parameters.inputData.getBoolean(CHARGING, false)
    private val level = parameters.inputData.getFloat(LEVEL, 0f)
    override suspend fun doWork(): Result {
        Log.d("gerwalex", "WidgetUpdateWorker: level: $level, charging: $isCharging")
        appWidgetUpdater.updateWidget(level, isCharging)
        return Result.success()
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