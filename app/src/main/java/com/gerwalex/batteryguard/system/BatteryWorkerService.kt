package com.gerwalex.batteryguard.system

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.main.MainActivity
import kotlinx.coroutines.*

class BatteryWorkerService(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    var broadcastIntent = MutableLiveData<Intent>()
    private var pendingUpdateEvent: Event? = null
    private var isScreenOn: Boolean = true // Beim Start ist der Bildschirm immer eingeschaltet
    private var isCharging: Boolean = false
    private val channelID by lazy { applicationContext.getString(R.string.notification_channel_id) }
    private val appWidgetUpdater = BatteryWidgetUpdater(applicationContext)
    private val batteryReceiver = BatteryBroadcastReceiver(this)
    private val observeBroadcastIntent = Observer<Intent> { intent ->
        MainScope().launch {
            withContext(Dispatchers.IO) {
                val batteryIntent = if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    intent
                } else {
                    getBatteryIntent()
                }
                batteryIntent?.let { it ->
                    when (intent.action) {
                        Intent.ACTION_SCREEN_OFF -> {
                            isScreenOn = false
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            isScreenOn = true
                            pendingUpdateEvent?.let {
                                doUpdate(it)
                                pendingUpdateEvent = null
                            }
                        }
                        Intent.ACTION_POWER_CONNECTED -> {
                            isCharging = true
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            isCharging = false
                        }
                        Intent.ACTION_BATTERY_LOW -> {
                        }
                        Intent.ACTION_BATTERY_OKAY -> {
                        }
                        Intent.ACTION_BATTERY_CHANGED -> {
                            val batteryManager: BatteryManager =
                                context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                            val event = Event(it, batteryManager)
                            event.insert()
                            pendingUpdateEvent = if (isScreenOn) {
                                doUpdate(event)
                                null
                            } else {
                                event
                            }
                        }
                        else -> {
                            throw IllegalArgumentException("Fatal! Action ${intent.action} not expected")
                        }
                    }
                }
            }
        }
    }

    private fun doUpdate(event: Event) {
        updateNotification(event)
        appWidgetUpdater.updateWidget(event.level, event.isCharging)
    }

    private fun getBatteryIntent(): Intent? {
        return IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
    }

    private fun updateNotification(event: Event) {
        val progress = applicationContext.getString(R.string.observeBatteryProgress, event.status.name, event.level)
        val title = applicationContext.getString(R.string.notification_title)
        val notification = NotificationCompat
            .Builder(applicationContext, channelID)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(progress)
            .setSmallIcon(event.icon)
            .build()
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(R.id.observeBatteryService, notification)
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        try {
            withContext(Dispatchers.Main) {
                batteryReceiver.register(applicationContext)
                broadcastIntent.observeForever(observeBroadcastIntent)
            }
            broadcastIntent.postValue(getBatteryIntent())
            awaitCancellation()
        } catch (e: CancellationException) {
            batteryReceiver.unregister(applicationContext)
            e.printStackTrace()
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val progress = applicationContext.getString(R.string.observeBattery)
        val title = applicationContext.getString(R.string.notification_title)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            /* context = */ applicationContext,
            /* requestCode = */  0,
            /* intent = */ Intent(applicationContext, MainActivity::class.java),
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat
            .Builder(applicationContext, channelID)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
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
            val request = OneTimeWorkRequest
                .Builder(BatteryWorkerService::class.java)
                .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(SERVICENAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
