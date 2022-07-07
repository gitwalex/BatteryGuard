package com.gerwalex.batteryguard.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableBoolean
import androidx.preference.PreferenceManager
import androidx.work.*
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation

class BatteryWorkerService(val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val batteryReceiver = BatteryBroadcastReceiver()
    private var plugReceiverRegistered: Boolean = false

    //Callback fur Properties, die vom GuardReceiver gesetzt werden
    private val callback = object : OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (IS_AC_PLUGGED.get() || IS_SCREEN_ON.get()) {
                if (!plugReceiverRegistered) {
                    plugReceiverRegistered = true
                    registerPlugInReceiver(context, plugReceiverRegistered)
                }
            } else {
                plugReceiverRegistered = false
                registerPlugInReceiver(context, plugReceiverRegistered)
            }
        }
    }
    private val plugReceiver = BatteryBroadcastReceiver()
    private val plugFilter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }

    init {
        IS_SCREEN_ON.set(true)// Screen ist eingeschaltet, wenn Service startet, da Interaktion mit User oder Reboot
        IS_SCREEN_ON.addOnPropertyChangedCallback(callback)
        IS_AC_PLUGGED.addOnPropertyChangedCallback(callback)
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())

        try {
            register(applicationContext)
            val event = getEvent(context, BatteryEvent.ServiceStarted)
            event?.let {
                it.insert()
                IS_AC_PLUGGED.set(it.isCharging)
                BatteryWidgetUpdateWorker.startUpdateWidget(context, event)
            }
            awaitCancellation()
        } catch (e: CancellationException) {
            getEvent(context, BatteryEvent.ServiceCancelled)?.insert()
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

        private val SERVICENAME = "BatteryWorkerService"
        const val SERVICE_REQUIRED = "SERVICE_REQUIRED"
        val IS_SCREEN_ON = ObservableBoolean(true)
        val IS_AC_PLUGGED = ObservableBoolean(false)

        @JvmStatic
        fun getEvent(context: Context, event: BatteryEvent): Event? {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            batteryStatus?.let { intent ->
                return Event(event, intent)
            }
            return null
        }

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
