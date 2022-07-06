package com.gerwalex.batteryguard.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("gerwalex", "Broadcast received: ${intent.action}")
        val pending = goAsync()
        var event: Event? = null
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                batteryStatus?.let { intent ->
                    when (intent.action) {
                        Intent.ACTION_BOOT_COMPLETED -> {
                            BatteryWorkerService.startService(context)
                            event = Event(BatteryEvent.Boot_Completed, batteryStatus)
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            event = Event(BatteryEvent.ScreenOff, batteryStatus)
                            BatteryWorkerService.IS_SCREEN_ON.set(false)
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            event = Event(BatteryEvent.ScreenOn, batteryStatus)
                            BatteryWorkerService.IS_SCREEN_ON.set(true)
                        }
                        Intent.ACTION_POWER_CONNECTED -> {
                            event = Event(BatteryEvent.Plugged_AC, batteryStatus)
                            BatteryWorkerService.IS_AC_PLUGGED.set(true)
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            event = Event(BatteryEvent.UnPlugged, batteryStatus)
                            BatteryWorkerService.IS_AC_PLUGGED.set(false)
                        }
                        Intent.ACTION_BATTERY_LOW -> {
                            event = Event(BatteryEvent.Battery_Low, batteryStatus)
                        }
                        Intent.ACTION_BATTERY_OKAY -> {
                            event = Event(BatteryEvent.Battery_Ok, batteryStatus)
                        }
                        else -> {
                        }
                    }
                    event?.run {
                        insert()
                        BatteryWidgetUpdateWorker.startUpdateWidget(context, this)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}