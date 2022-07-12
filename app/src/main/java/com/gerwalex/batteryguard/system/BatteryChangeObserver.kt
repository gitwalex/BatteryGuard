package com.gerwalex.batteryguard.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class BatteryChangeObserver(private val service: BatteryWorkerService) : BroadcastReceiver() {

    private val intentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }
    private var isRegistered = false
    fun register(context: Context) {
        if (!isRegistered) {
            context.registerReceiver(this, intentFilter)
            isRegistered = true
            Log.d("gerwalex", "BatteryChangeObserver registered ")
        }
    }

    fun unregister(context: Context) {
        if (isRegistered) {
            context.unregisterReceiver(this)
            isRegistered = false
            Log.d("gerwalex", "BatteryChangeObserver unregistered ")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("gerwalex", "Broadcast received: ${intent.action}")
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            service.broadcastIntent.postValue(intent)
        }
    }
}