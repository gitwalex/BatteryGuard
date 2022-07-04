package com.gerwalex.batteryguard.system

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableBoolean
import androidx.preference.PreferenceManager
import androidx.work.*
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import com.gerwalex.batteryguard.enums.BatteryStatus
import com.gerwalex.batteryguard.widget.BatteryGuardWidgetProvider
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class BatteryObserverWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    //Callback fur Properties, die vom GueardReceiver gesetzt werden
    private val callback = object : OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (IS_AC_PLUGGED.get() || IS_SCREEN_ON.get()) {
                if (job == null) {
                    job = startJob()
                }
            } else {
                job?.let {
                    it.cancel()
                    job = null
                }
            }
        }
    }
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val widgetProvider = ComponentName(context, BatteryGuardWidgetProvider::class.java)
    private var job: Job? = null
    private var lastEvent: Event? = null

    init {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(SERVICE_REQUIRED, true)
            .apply()
        IS_SCREEN_ON.set(true)// Screen ist eingeschaltet, wenn Service startet, da Interaktion mit User oder Reboot
        IS_SCREEN_ON.addOnPropertyChangedCallback(callback)
        IS_AC_PLUGGED.addOnPropertyChangedCallback(callback)
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

    private fun startJob(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(TimeUnit.MINUTES.toMillis(SCREEN_ON_DELAY_IN_MINUTES.toLong()))
                val event = newEvent(BatteryEvent.ServiceStatus, applicationContext)
                event?.let {
                    if (it != lastEvent) {
                        it.insert()
                        lastEvent = event
                        updateWidgets(applicationContext)
                    }
                }
            }
        }
    }

    private fun updateWidgets(context: Context) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)
        if (appWidgetIds.isNotEmpty()) {
            val views = RemoteViews(context.packageName, R.layout.appwidget_provider_layout)
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())

        try {
            register(applicationContext)
            job = startJob()
            newEvent(BatteryEvent.ServiceStarted, applicationContext)?.insert()
            updateWidgets(applicationContext)
            awaitCancellation()
        } catch (e: CancellationException) {
            newEvent(BatteryEvent.ServiceCancelled, applicationContext)?.insert()
            e.printStackTrace()
        }
        return Result.success()
    }

    fun register(context: Context) {
        val inf = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(BatteryGuardWidgetProvider(), inf)
        Log.d("gerwalex", "BatteryGuardReceiver registered!")
    }

    companion object {

        const val SERVICE_REQUIRED = "SERVICE_REQUIRED"
        const val SCREEN_ON_DELAY_IN_MINUTES = 1
        val IS_SCREEN_ON = ObservableBoolean(true)
        val IS_AC_PLUGGED = ObservableBoolean(false)

        @JvmStatic
        fun startService(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getBoolean(SERVICE_REQUIRED, false)) {
                // Service nur starten, wenn benötigt
                val request = OneTimeWorkRequest
                    .Builder(BatteryObserverWorker::class.java)
                    .build()
                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork("BatteryObserveWorker", ExistingWorkPolicy.REPLACE, request)
            }
        }

        @JvmStatic
        fun newEvent(event: BatteryEvent, context: Context): Event? {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            batteryStatus?.let {
                val status: BatteryStatus = when (it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.Status_Charging
                    else -> BatteryStatus.Status_Discharging
                }
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val chargeLevel = level * 100 / scale.toFloat()
                return Event(event, status, chargeLevel)
            }
            return null
        }
    }
}
