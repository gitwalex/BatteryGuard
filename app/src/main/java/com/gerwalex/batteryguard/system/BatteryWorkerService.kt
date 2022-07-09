package com.gerwalex.batteryguard.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.work.*
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import kotlinx.coroutines.*

class BatteryWorkerService(val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val appWidgetUpdater = BatteryWidgetUpdater(context)
    private val batteryReceiver = BatteryBroadcastReceiver()
    private var plugReceiverRegistered: Boolean = false
    private val observeStatus = Observer<Boolean> {
        if (batteryReceiver.isScreenOn.value == true || batteryReceiver.isCharging.value == true) {
            if (!plugReceiverRegistered) {
                plugReceiverRegistered = true
                registerPlugInReceiver(context, plugReceiverRegistered)
            }
        } else {
            plugReceiverRegistered = false
            registerPlugInReceiver(context, plugReceiverRegistered)
        }
    }
    private val observeEvent = Observer<Event> { event ->
        MainScope().launch {
            withContext(Dispatchers.IO) {
                event.insert()
            }
            appWidgetUpdater.updateWidget(event.level, event.isCharging)
        }
    }
    private val plugReceiver = BatteryBroadcastReceiver()
    private val plugFilter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }

    init {
        MainScope().launch {
            batteryReceiver.isCharging.observeForever(observeStatus)
            batteryReceiver.isScreenOn.observeForever(observeStatus)
            batteryReceiver.currentEvent.observeForever(observeEvent)
            plugReceiver.currentEvent.observeForever(observeEvent)
        }
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())

        try {
            register(applicationContext)
            awaitCancellation()
        } catch (e: CancellationException) {
            context.unregisterReceiver(batteryReceiver)
            registerPlugInReceiver(context, !plugReceiverRegistered)
            e.printStackTrace()
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val progress = applicationContext.getString(R.string.observeBattery)
        val title = applicationContext.getString(R.string.notification_title)
        val cancel = applicationContext.getString(R.string.cancel_observeBattery)
        val channelID = applicationContext.getString(R.string.notification_channel_id)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager
            .getInstance(applicationContext)
            .createCancelPendingIntent(id)
        val notification = NotificationCompat
            .Builder(applicationContext, channelID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(R.id.observeBatteryService, notification)
    }

    private fun register(context: Context) {
        val inf = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(batteryReceiver, inf)
        Log.d("gerwalex", "BatteryBroadcastReceiver registered!")
    }

    private fun registerPlugInReceiver(context: Context, register: Boolean) {
        if (register) {
            context.registerReceiver(plugReceiver, plugFilter)
        } else {
            context.unregisterReceiver(plugReceiver)
        }
        Log.d("gerwalex", "PlugInReceiver registered: $register")
    }

    companion object {

        const val SERVICENAME = "BatteryWorkerService"
        const val SERVICE_REQUIRED = "SERVICE_REQUIRED"

        @JvmStatic
        fun stopService(context: Context) {
            WorkManager
                .getInstance(context)
                .cancelUniqueWork(SERVICENAME)
        }

        @JvmStatic
        fun startService(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getBoolean(SERVICE_REQUIRED, false)) {
                // Service nur starten, wenn benötigt
                val request = OneTimeWorkRequest
                    .Builder(BatteryWorkerService::class.java)
                    .build()
                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork(SERVICENAME, ExistingWorkPolicy.KEEP, request)
            }
        }
    }
}
