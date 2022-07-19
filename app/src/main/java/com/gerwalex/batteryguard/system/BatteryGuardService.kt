package com.gerwalex.batteryguard.system

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.main.MainActivity
import kotlinx.coroutines.*

class BatteryGuardService : Service() {

    private val startAppIntent by lazy {
        PendingIntent.getActivity(
            /* context = */ applicationContext,
            /* requestCode = */  0,
            /* intent = */ Intent(applicationContext, MainActivity::class.java),
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    private var job: Job? = null
    private var broadcastIntent = MutableLiveData<Intent>()
    private var pendingUpdateEvent: Event? = null
    private var isScreenOn: Boolean = true // Beim Start ist der Bildschirm immer eingeschaltet
    private var isCharging: Boolean = false
    private val channelID by lazy { applicationContext.getString(R.string.notification_channel_id) }
    private val appWidgetUpdater by lazy {
        BatteryWidgetUpdater(applicationContext)
    }
    val lastEvent = MutableLiveData<Event>()
    private val batteryReceiver = BatteryBroadcastReceiver(broadcastIntent)
    private val observeBroadcastIntent = Observer<Intent> { intent ->
        MainScope().launch {
            Log.d("gerwalex", "JobService: Action received ${intent.action} ")
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
                                applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
                            val event = Event(it, batteryManager)
                            event.insert()
                            lastEvent.postValue(event)
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

    override fun onCreate() {
        super.onCreate()
        Log.d("gerwalex", "BatteryService created:$this")
        broadcastIntent.observeForever(observeBroadcastIntent)
        job = MainScope().launch {
            startForeground(R.id.observeBatteryService, createForegroundInfo())
            batteryReceiver.register(applicationContext)
            withContext(Dispatchers.IO) {
                try {
                    broadcastIntent.postValue(getBatteryIntent())
                    awaitCancellation()
                } catch (e: CancellationException) {
                    batteryReceiver.unregister(applicationContext)
                    broadcastIntent.removeObserver(observeBroadcastIntent)
                    e.printStackTrace()
                    job = null
                }
            }
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
            .setContentIntent(startAppIntent)
            .build()
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(R.id.observeBatteryService, notification)
    }

    private fun createForegroundInfo(): Notification {
        val progress = applicationContext.getString(R.string.observeBattery)
        val title = applicationContext.getString(R.string.notification_title)
        return NotificationCompat
            .Builder(applicationContext, channelID)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(startAppIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return BatteryServiceBinder()
    }

    inner class BatteryServiceBinder : Binder() {

        val service: BatteryGuardService
            get() = this@BatteryGuardService
    }
}