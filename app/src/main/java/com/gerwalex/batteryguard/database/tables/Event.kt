package com.gerwalex.batteryguard.database.tables

import android.content.Intent
import android.database.Cursor
import android.os.BatteryManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.gerwalex.batteryguard.database.DB.dao
import com.gerwalex.batteryguard.enums.BatteryHealth
import com.gerwalex.batteryguard.enums.BatteryPlugged
import com.gerwalex.batteryguard.enums.BatteryStatus
import com.gerwalex.lib.database.ObservableTableRow

@Entity
class Event : ObservableTableRow {


    @ColumnInfo(name = "_id")
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
        set(value) {
            field = value
            value?.let { put("_id", value) } ?: putNull("_id")
        }
    val status: BatteryStatus
    val isCharging: Boolean
        get() {
            return when (status) {
                BatteryStatus.Status_Full,
                BatteryStatus.Status_Charging,
                -> true
                else -> false
            }
        }
    var plugged: BatteryPlugged = BatteryPlugged.None

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
    val ts: Long

    /**
     * Remaining battery capacity as an integer percentage of total capacity (with no fractional part).
     */
    var remaining: Int = 0

    /**
     * Battery capacity in microampere-hours, as an integer.
     */
    var capacity: Int = 0

    /**
     * Battery capacity in microampere-hours
     */
    var chargeCounter: Int = -1

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
    var health: BatteryHealth = BatteryHealth.UnKnown

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

    constructor(batteryStatus: Intent, batteryManager: BatteryManager?) {
        status = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.Status_Charging
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.Status_Full
            else -> BatteryStatus.Status_Discharging
        }
        plugged = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> BatteryPlugged.AC
            BatteryManager.BATTERY_PLUGGED_USB -> BatteryPlugged.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPlugged.Wireless
            else -> BatteryPlugged.None
        }
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        this.level = level * 100 / scale.toFloat()
        ts = System.currentTimeMillis()
        this.icon =
            batteryStatus.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, android.R.drawable.ic_lock_idle_low_battery)
        temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        health = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.Cold
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.Dead
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.Good
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.Overheat
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OverVoltage
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UnspecifiedFailure
            else -> BatteryHealth.UnKnown
        }
        battery_low = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
        batteryManager?.let { bm ->
            remaining = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            avg_current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            now_current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            remaining_nanowatt = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            chargeTimeRemaining = bm.computeChargeTimeRemaining()
        }
    }

    constructor(c: Cursor) : super(c) {
        id = getAsLongOrNull("_id")
        status = BatteryStatus.values()[getAsInt(::status.name)]
        level = getAsFloat(::level.name)
        scale = getAsInt(::scale.name)
        health = BatteryHealth.values()[getAsInt(::health.name)]
        plugged = BatteryPlugged.values()[getAsInt(::plugged.name)]
        battery_low = getAsBoolean(::battery_low.name)
        technology = getAsString(::technology.name)
        capacity = getAsInt(::capacity.name)
        remaining = getAsInt(::remaining.name)
        temperature = getAsInt(::temperature.name)
        voltage = getAsInt(::voltage.name)
        chargeTimeRemaining = getAsLong(::chargeTimeRemaining.name)
        ts = getAsLong(::ts.name)
        avg_current = getAsInt(::avg_current.name)
        now_current = getAsInt(::now_current.name)
        remaining_nanowatt = getAsLong(::remaining_nanowatt.name)
    }

    constructor(
        id: Long,
        status: BatteryStatus,
        level: Float,
        scale: Int,
        ts: Long,
        plugged: BatteryPlugged,
        remaining: Int,
        capacity: Int,
        avg_current: Int,
        now_current: Int,
        chargeTimeRemaining: Long,
        temperature: Int,
        voltage: Int,
        technology: String?,
        health: BatteryHealth,
        battery_low: Boolean,
        remaining_nanowatt: Long,
    ) : super() {
        this.id = id
        this.status = status
        this.level = level
        this.scale = scale
        this.ts = ts
        this.plugged = plugged
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
        return "Event(id=$id,  status=$status, isCharging=$isCharging, plugged=$plugged, level=$level, " +
                "scale=$scale, time=$ts, remaining=$remaining, capacity=$capacity, avg_current=$avg_current, " +
                "now_current=$now_current, chargeTimeRemaining=$chargeTimeRemaining, temperature=$temperature, " +
                "voltage=$voltage, technology=$technology, health=$health, battery_low=$battery_low, remaining_nanowatt=$remaining_nanowatt)"
    }
}