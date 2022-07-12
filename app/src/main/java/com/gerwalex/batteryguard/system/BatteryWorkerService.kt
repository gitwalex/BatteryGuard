package com.gerwalex.batteryguard.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.work.*
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import kotlinx.coroutines.*

class BatteryWorkerService(val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private var lastEvent: Event? = null
    private var isScreenOn: Boolean = true // Beim Start ist der Bidschirm immer eingeschaltet
    private var isCharging: Boolean = false
    private val notificationManager: NotificationManager by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val batteryManager: BatteryManager? by lazy {
        applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    private val channelID by lazy { applicationContext.getString(R.string.notification_channel_id) }
    private val appWidgetUpdater = BatteryWidgetUpdater(context)
    private val batteryChangedObserver = BatteryChangeObserver(this)
    private val batteryReceiver = BatteryBroadcastReceiver(this)
    val broadcastIntent = MutableLiveData<Intent>()
    private val observeBroadcastIntent = Observer<Intent> { intent ->
        MainScope().launch {
            withContext(Dispatchers.IO) {
                val batteryIntent = if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    intent
                } else {
                    getBatteryIntent()
                }
                var newEvent: Event? = null
                batteryIntent?.let { it ->
                    when (intent.action) {
                        Intent.ACTION_SCREEN_OFF -> {
//                            newEvent = Event(BatteryEvent.ScreenOff, it)
                            isScreenOn = false
//                            if (!isCharging) {
//                                batteryChangedObserver.unregister(context)
//                            }
                        }
                        Intent.ACTION_SCREEN_ON -> {
//                            newEvent = Event(BatteryEvent.ScreenOn, it)
                            isScreenOn = true
//                            batteryChangedObserver.register(context)
                        }
                        Intent.ACTION_POWER_CONNECTED -> {
//                            newEvent = Event(BatteryEvent.Plugged_AC, it)
                            isCharging = true
//                            batteryChangedObserver.register(context)
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
//                            newEvent = Event(BatteryEvent.UnPlugged, it)
                            isCharging = false
//                            if (!isScreenOn) {
//                                batteryChangedObserver.unregister(context)
//                            }
                        }
                        Intent.ACTION_BATTERY_LOW -> {
//                            newEvent = Event(BatteryEvent.Battery_Low, it)
                        }
                        Intent.ACTION_BATTERY_OKAY -> {
//                            newEvent = Event(BatteryEvent.Battery_Ok, it)
                        }
                        Intent.ACTION_BATTERY_CHANGED -> {
                            newEvent = Event(BatteryEvent.Battery_Changed, it)
                        }
                        else -> {
                            throw IllegalArgumentException("Fatal! Action ${intent.action} not expected")
                        }
                    }
                    newEvent?.let { event ->
                        batteryManager?.let { bm ->
                            event.remaining = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                            event.capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                            event.avg_current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                            event.now_current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                            event.remaining_nanowatt =
                                bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
                            event.chargeTimeRemaining = bm.computeChargeTimeRemaining()
                        }
                        event.insert()
                        lastEvent = event
                    }
                    lastEvent?.let {
                        if (isScreenOn) {
                            updateNotification(it)
                            appWidgetUpdater.updateWidget(it.level, it.isCharging)
                        }
                    }
                }
            }
        }
    }

    private fun getBatteryIntent(): Intent? {
        return IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
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

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        try {
            withContext(Dispatchers.Main) {
//                batteryChangedObserver.register(context)
                batteryReceiver.register(context)
                broadcastIntent.observeForever(observeBroadcastIntent)
            }
            broadcastIntent.postValue(getBatteryIntent())
            awaitCancellation()
        } catch (e: CancellationException) {
            batteryChangedObserver.unregister(context)
            batteryReceiver.unregister(context)
            e.printStackTrace()
        }
        return Result.success()
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
