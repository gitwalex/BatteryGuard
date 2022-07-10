package com.gerwalex.batteryguard.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.enums.BatteryEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryChangeObserver : BroadcastReceiver() {

    private val intentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }
    val currentEvent = MutableLiveData<Event>()
    private var _isRegistered = false
    val isRegistered: Boolean
        get() {
            return _isRegistered
        }

    fun register(context: Context) {
        if (!isRegistered) {
            context.registerReceiver(this, intentFilter)
            _isRegistered = true
            Log.d("gerwalex", "BatteryChangeObserver registered ")
        }
    }

    fun unregister(context: Context) {
        if (isRegistered) {
            context.unregisterReceiver(this)
            _isRegistered = false
            Log.d("gerwalex", "BatteryChangeObserver unregistered ")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("gerwalex", "Broadcast received: ${intent.action}")
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    if (level.toFloat() != currentEvent.value?.level) {
                        currentEvent.postValue(Event(BatteryEvent.Battery_Changed, intent))
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}