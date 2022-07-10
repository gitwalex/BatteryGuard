package com.gerwalex.batteryguard.system

import android.app.NotificationManager
import android.content.Context
import android.os.BatteryManager
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

    private val notificationManager: NotificationManager by lazy {
        applicationContext.getSystemService(Context
            .NOTIFICATION_SERVICE) as NotificationManager
    }
    private val batteryManager: BatteryManager? by lazy {
        applicationContext.getSystemService(Context
            .BATTERY_SERVICE) as BatteryManager
    }
    private val channelID by lazy { applicationContext.getString(R.string.notification_channel_id) }
    private val appWidgetUpdater = BatteryWidgetUpdater(context)
    private val batteryReceiver = BatteryBroadcastReceiver()
    private val observeStatus = Observer<Boolean> {
        if (batteryReceiver.isScreenOn.value == true || batteryReceiver.isCharging.value == true) {
            batteryChangedObserver.register(context)
        } else {
            batteryChangedObserver.unregister(context)
        }
    }
    private val observeEvent = Observer<Event> { event ->
        MainScope().launch {
            withContext(Dispatchers.IO) {
                batteryManager?.let {
                    event.remaining = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    event.capacity = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                    event.avg_current = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                    event.now_current = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    event.remaining_nanowatt = it.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
                    event.chargeTimeRemaining = it.computeChargeTimeRemaining()
                }
                event.insert()
                updateNotification(event)
            }
            if (batteryReceiver.isScreenOn.value == true) {
                appWidgetUpdater.updateWidget(event.level, event.isCharging)
            }
        }
    }

    private fun updateNotification(event: Event) {
        val progress = applicationContext.getString(R.string.observeBatteryProgress, event.status.name, event.level)
        val title = applicationContext.getString(R.string.notification_title)
        // This PendingIntent can be used to cancel the worker
        val notification = NotificationCompat
            .Builder(applicationContext, channelID)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(progress)
            .setSmallIcon(event.icon)
            .build()
        notificationManager.notify(R.id.observeBatteryService, notification)
    }

    private val batteryChangedObserver = BatteryChangeObserver()

    init {
        MainScope().launch {
            batteryChangedObserver.register(context)
            batteryReceiver.register(context)
            batteryReceiver.isCharging.observeForever(observeStatus)
            batteryReceiver.isScreenOn.observeForever(observeStatus)
            batteryReceiver.currentEvent.observeForever(observeEvent)
            batteryChangedObserver.currentEvent.observeForever(observeEvent)
            Log.d("gerwalex", "BatteryBroadcastReceiver registered!")
        }
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        try {
            awaitCancellation()
        } catch (e: CancellationException) {
            batteryChangedObserver.unregister(context)
            batteryReceiver.unregister(context)
            e.printStackTrace()
        }
        return Result.success()
    }

    private fun createBasicNotification(title: String, progress: String): NotificationCompat.Builder {
        return NotificationCompat
            .Builder(applicationContext, channelID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val progress = applicationContext.getString(R.string.observeBattery)
        val title = applicationContext.getString(R.string.notification_title)
        // This PendingIntent can be used to cancel the worker
        val notification = NotificationCompat
            .Builder(applicationContext, channelID)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return ForegroundInfo(R.id.observeBatteryService, notification)
    }

    companion object {

        private const val SERVICENAME = "BatteryWorkerService"
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
