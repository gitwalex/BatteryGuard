package com.gerwalex.batteryguard.database.tables

import android.content.Intent
import android.database.Cursor
import android.os.BatteryManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.gerwalex.batteryguard.database.DB.dao
import com.gerwalex.batteryguard.enums.BatteryEvent
import com.gerwalex.batteryguard.enums.BatteryStatus
import com.gerwalex.lib.database.ObservableTableRow

@Entity
class Event : ObservableTableRow {

    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
        set(value) {
            field = value
            value?.let { put("_id", value) } ?: putNull("_id")
        }
    val event: BatteryEvent
    val status: BatteryStatus
    val isCharging: Boolean
        get() {
            return when (status) {
                BatteryStatus.Status_Charging -> true
                else -> false
            }
        }

    /**
     * integer field containing the current battery level, from 0 to scale
     */
    val level: Float

    /**
     *  integer containing the maximum battery level.
     */
    val scale: Int

    /**
     * Timestamp of event
     */
    val time: Long

    /**
     * Remaining battery capacity as an integer percentage of total capacity (with no fractional part).
     */
    var remaining: Int = 0

    /**
     *       Battery capacity in microampere-hours, as an integer.
     */
    var capacity: Int = 0

    /**
    Average battery current in microamperes, as an integer.
    Positive values indicate net current entering the battery from a charge source,
    negative values indicate net current discharging from the battery.
    The time period over which the average is computed may depend on the fuel gauge hardware and its configuration.
     */
    var avg_current: Int = 0

    /**
    Instantaneous battery current in microamperes, as an integer.
    Positive values indicate net current entering the battery from a charge source,
    negative values indicate net current discharging from the battery.
     */
    var now_current: Int = 0

    /**
     * an approximation for how much time (in milliseconds) remains until the battery is fully charged. -1 if no time can be computed:
     */
    var chargeTimeRemaining: Long = -1

    /**
     * integer containing the current battery temperature.
     */
    var temperature: Int = 0

    /**
     *  integer containing the current battery voltage level.
     */
    var voltage: Int = 0

    /**
     *  String describing the technology of the current battery.
     */
    var technology: String? = "UNKNOWN"

    /**
     * integer containing the current health constant.
     */
    var health: Int = -1

    /**
     * Boolean field indicating whether the battery is currently considered to be low, that is whether a Intent#ACTION_BATTERY_LOW broadcast has been sent.
     */
    var battery_low: Boolean = false

    /**
     * Battery remaining energy in nanowatt-hours, as a long integer.
     */
    var remaining_nanowatt: Long = 0

    @Ignore
    @DrawableRes
    var icon: Int = android.R.drawable.ic_lock_idle_low_battery

    constructor(event: BatteryEvent, batteryStatus: Intent) {
        this.event = event
        status = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.Status_Charging
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.Status_Full
            else -> BatteryStatus.Status_Discharging
        }
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        this.level = level * 100 / scale.toFloat()
        time = System.currentTimeMillis()
        this.icon =
            batteryStatus.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, android.R.drawable.ic_lock_idle_low_battery)
        temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        battery_low = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
    }

    constructor(c: Cursor) : super(c) {
        id = getAsLongOrNull("_id")
        event = BatteryEvent.values()[getAsInt(::event.name)]
        status = BatteryStatus.values()[getAsInt(::status.name)]
        level = getAsFloat(::level.name)
        scale = getAsInt(::scale.name)
        this.capacity = getAsInt(::capacity.name)
        this.remaining = getAsInt(::remaining.name)
        this.temperature = getAsInt(::temperature.name)
        this.voltage = getAsInt(::voltage.name)
        this.chargeTimeRemaining = getAsLong(::chargeTimeRemaining.name)
        time = getAsLong(::time.name)
    }

    constructor(
        id: Long,
        event: BatteryEvent,
        status: BatteryStatus,
        level: Float,
        scale: Int,
        time: Long,
        remaining: Int,
        capacity: Int,
        avg_current: Int,
        now_current: Int,
        chargeTimeRemaining: Long,
        temperature: Int,
        voltage: Int,
        technology: String?,
        health: Int,
        battery_low: Boolean,
        remaining_nanowatt: Long,
    ) : super() {
        this.id = id
        this.event = event
        this.status = status
        this.level = level
        this.scale = scale
        this.time = time
        this.remaining = remaining
        this.capacity = capacity
        this.avg_current = avg_current
        this.now_current = now_current
        this.chargeTimeRemaining = chargeTimeRemaining
        this.temperature = temperature
        this.voltage = voltage
        this.technology = technology
        this.health = health
        this.battery_low = battery_low
        this.remaining_nanowatt = remaining_nanowatt
    }

    fun insert() {
        id = dao.insert(this)
        Log.d("gerwalex", "Event inserted: $this")
    }

    override fun toString(): String {
        return "Event(id=$id, event=$event, status=$status, isCharging=$isCharging, level=$level, scale=$scale, time=$time, remaining=$remaining, capacity=$capacity, avg_current=$avg_current, now_current=$now_current, chargeTimeRemaining=$chargeTimeRemaining, temperature=$temperature, voltage=$voltage, technology=$technology, health=$health, battery_low=$battery_low, remaining_nanowatt=$remaining_nanowatt)"
    }
}