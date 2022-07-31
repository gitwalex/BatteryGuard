package com.gerwalex.batteryguard.system

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.main.MainActivity
import kotlinx.coroutines.*

class BatteryGuardService : Service() {

    private val LOWIMPORTANCECHANNELID by lazy {
        getString(R.string.notification_low_importance_channel_id)
    }
    private val HIGHIMPORTANCECHANNELID by lazy {
        getString(R.string.notification_high_importance_channel_id)
    }
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
    private val appWidgetUpdater by lazy {
        BatteryWidgetUpdater(applicationContext)
    }
    private val notificationManager by lazy {
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    }
    private val powerManager by lazy {
        (applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
    }
    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
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
                            vibrate()
                            if (prefs.getBoolean(getString(R.string.pkPlugInNotification), false)) {
                                play(prefs.getString(getString(R.string.pkPlugInSoundFile), null))
                            }
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            isCharging = false
                            vibrate()
                            if (prefs.getBoolean(getString(R.string.pkPlugOutNotification), false)) {
                                play(prefs.getString(getString(R.string.pkPlugOutSoundFile), null))
                            }
                        }
                        Intent.ACTION_BATTERY_LOW -> {
                        }
                        Intent.ACTION_BATTERY_OKAY -> {
                        }
                        Intent.ACTION_BATTERY_CHANGED -> {
                            val batteryManager: BatteryManager =
                                applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
                            val event = Event(it, batteryManager)
                            checkAlarms(event)
                            if (!event.isEqual(lastEvent.value)) {
                                event.insert()
                            }
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

    /**
     *
     */
    private fun checkAlarms(event: Event) {
        lastEvent.value?.let {
            if (prefs.getBoolean(getString(R.string.pkLowChargeNotification), false)) {
                val percent = prefs.getInt(getString(R.string.pkLowChargeSoundPercent), 20)
                if (event.remaining <= percent && it.remaining > percent) {
                    Log.d("gerwalex", "Alarm: lowCharge ${event.remaining}")
                    play(prefs.getString(getString(R.string.pkLowChargeSoundFile), null))
                }
            }
            if (prefs.getBoolean(getString(R.string.pkHighChargeNotification), false)) {
                val percent = prefs.getInt(getString(R.string.pkHighChargeSoundPercent), 20)
                if (event.remaining >= percent && it.remaining < percent) {
                    Log.d("gerwalex", "Alarm: highCharge ${event.remaining}")
                    play(prefs.getString(getString(R.string.pkHighChargeSoundFile), null))
                }
            }
            if (prefs.getBoolean(getString(R.string.pkHighTemperatureNotification), false)) {
                val value = prefs.getInt(getString(R.string.pkHighTemperature), 40)
                if (event.remaining >= value && it.remaining < value) {
                    Log.d("gerwalex", "Alarm: highTemperatur ${event.remaining}")
                    play(prefs.getString(getString(R.string.pkHighTemperatureSoundFile), null))
                }
            }
        }
    }

    private fun vibrate() {
        if (prefs.getBoolean(getString(R.string.pkVibrationOn), false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }

    private fun play(uriAsString: String?) {
        uriAsString?.let {
            val uri = Uri.parse(uriAsString)
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, uri)
                prepare()
                start()
            }
        }
    }

    private fun doUpdate(event: Event) {
        val status = applicationContext.getString(event.status.getTextResID())
        val progress = applicationContext.getString(R.string.observeBatteryProgress, status, event.level)
        val title = applicationContext.getString(R.string.notification_title)
        val notification = NotificationCompat
            .Builder(applicationContext, LOWIMPORTANCECHANNELID)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(progress)
            .setSmallIcon(event.icon)
            .setContentIntent(startAppIntent)
            .build()
        notificationManager.notify(R.id.observeBatteryService, notification)
        appWidgetUpdater.updateWidget(event.level, event.isCharging)
    }

    private fun getBatteryIntent(): Intent? {
        return IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // Create the Low Importance Notification channel
        var name = applicationContext.getString(R.string.low_importance_channel_name)
        var descriptionText = applicationContext.getString(R.string.low_importance_channel_description)
        var importance = NotificationManager.IMPORTANCE_LOW
        var channel = NotificationChannel(LOWIMPORTANCECHANNELID, name, importance)
        channel.description = descriptionText
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
        // Create the High Importance Notification channel
        name = applicationContext.getString(R.string.high_importance_channel_name)
        descriptionText = applicationContext.getString(R.string.high_importance_channel_description)
        importance = NotificationManager.IMPORTANCE_HIGH
        channel = NotificationChannel(HIGHIMPORTANCECHANNELID, name, importance)
        channel.description = descriptionText
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("gerwalex", "BatteryService created:$this")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        job = MainScope().launch {
            startForeground(R.id.observeBatteryService, createForegroundInfo())
            batteryReceiver.register(applicationContext)
            broadcastIntent.postValue(getBatteryIntent())
            broadcastIntent.observeForever(observeBroadcastIntent)
            withContext(Dispatchers.IO) {
                try {
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

    private fun createAlarmNotification(@StringRes titleRes: Int, contentText: String, sound: Uri?) {
        val title = applicationContext.getString(titleRes)
        val notification = NotificationCompat
            .Builder(applicationContext, HIGHIMPORTANCECHANNELID)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(startAppIntent)
            .setSound(sound)
            .build()
        notificationManager.notify(titleRes, notification)
    }

    private fun createForegroundInfo(): Notification {
        val title = applicationContext.getString(R.string.notification_title)
        return NotificationCompat
            .Builder(applicationContext, LOWIMPORTANCECHANNELID)
            .setTicker(title)
            .setContentTitle(title)
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
