package com.gerwalex.batteryguard.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryBroadcastReceiver : BroadcastReceiver() {

    val currentEvent = MutableLiveData<Event>()
    val isScreenOn = MutableLiveData(true)
    val isCharging = MutableLiveData(false)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("gerwalex", "Broadcast received: ${intent.action}")
        val pending = goAsync()
        val lastEvent = currentEvent.value
        var event: Event? = null
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                batteryStatus?.let {
                    when (intent.action) {
                        Intent.ACTION_BOOT_COMPLETED -> {
                            BatteryWorkerService.startService(context)
                            event = Event(BatteryEvent.Boot_Completed, it)
                            isScreenOn.postValue(true)
                            isCharging.value = event?.isCharging
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            event = Event(BatteryEvent.ScreenOff, it)
                            isScreenOn.postValue(false)
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            event = Event(BatteryEvent.ScreenOn, it)
                            isScreenOn.postValue(true)
                        }
                        Intent.ACTION_POWER_CONNECTED -> {
                            event = Event(BatteryEvent.Plugged_AC, it)
                            isCharging.postValue(true)
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            event = Event(BatteryEvent.UnPlugged, it)
                            isCharging.postValue(false)
                        }
                        Intent.ACTION_BATTERY_CHANGED -> {
                            event = Event(BatteryEvent.Battery_Changed, intent)
                        }
                        Intent.ACTION_BATTERY_LOW -> {
                            event = Event(BatteryEvent.Battery_Low, it)
                        }
                        Intent.ACTION_BATTERY_OKAY -> {
                            event = Event(BatteryEvent.Battery_Ok, it)
                        }
                        else -> {
                        }
                    }
                    event?.run {
                        if (event != lastEvent) {
                            currentEvent.postValue(event)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}