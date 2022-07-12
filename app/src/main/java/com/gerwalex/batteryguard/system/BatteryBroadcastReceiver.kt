package com.gerwalex.batteryguard.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class BatteryBroadcastReceiver() : BroadcastReceiver() {

    private var service: BatteryWorkerService? = null

    constructor(service: BatteryWorkerService) : this() {
        this.service = service
    }

    private var isRegistered = false
    private val intentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_BATTERY_OKAY)
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
    }

    fun register(context: Context) {
        if (!isRegistered) {
            context.registerReceiver(this, intentFilter)
            isRegistered = true
        }
    }

    fun unregister(context: Context) {
        if (isRegistered) {
            context.unregisterReceiver(this)
            isRegistered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("gerwalex", "Broadcast received: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                BatteryWorkerService.startService(context)
            }
            Intent.ACTION_SCREEN_OFF,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_BATTERY_CHANGED,
            -> {
                service?.broadcastIntent?.postValue(intent)
            }
            else -> {
                throw IllegalArgumentException("Fatal! Action ${intent.action} not expected")
            }
        }
    }
}