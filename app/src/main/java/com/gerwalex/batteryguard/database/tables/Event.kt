package com.gerwalex.batteryguard.database.tables

import android.content.Intent
import android.database.Cursor
import android.os.BatteryManager
import android.util.Log
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.gerwalex.batteryguard.database.DB.dao
import com.gerwalex.batteryguard.enums.BatteryEvent
import com.gerwalex.batteryguard.enums.BatteryStatus
import com.gerwalex.lib.database.ObservableTableRow

@Entity
class Event : ObservableTableRow {

    val isCharging: Boolean
        get() {
            return when (status) {
                BatteryStatus.Status_Charging -> true
                else -> false
            }
        }

    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
        set(value) {
            field = value
            value?.let { put("_id", value) } ?: putNull("_id")
        }
    val event: BatteryEvent
    val status: BatteryStatus
    val level: Float
    val time: Long

    //   Remaining battery capacity as an integer percentage of total capacity (with no fractional part).
    var capacity: Int = 0
    var chargeTimeRemaining: Long = 0

    //Instantaneous battery current in microamperes, as an integer.
    // Positive values indicate net current entering the
    // battery from a charge source, negative values indicate net current discharging from the battery.
    var instant: Int = 0

    //Average battery current in microamperes, as an integer.
// Positive values indicate net current entering the battery from a charge source,
// negative values indicate net current discharging from the battery.
// The time period over which the average is computed may depend on the fuel gauge hardware and its configuration.
    var micro_avg: Int = 0

    //Battery capacity in microampere-hours, as an integer.
    var microampere: Int = 0

    //Battery remaining energy in nanowatt-hours, as a long integer.
    var remaining: Int = 0
    var temperature: Int = 0
    var voltage: Int = 0

    constructor(event: BatteryEvent, batteryStatus: Intent) {
        this.event = event
        status = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.Status_Charging
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.Status_Full
            else -> BatteryStatus.Status_Discharging
        }
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        this.level = level * 100 / scale.toFloat()
        time = System.currentTimeMillis()
    }

    constructor(c: Cursor) : super(c) {
        id = getAsLongOrNull("_id")
        event = BatteryEvent.values()[getAsInt(::event.name)]
        status = BatteryStatus.values()[getAsInt(::status.name)]
        level = getAsFloat(::level.name)
        this.capacity = getAsInt(::capacity.name)
        this.microampere = getAsInt(::microampere.name)
        this.micro_avg = getAsInt(::micro_avg.name)
        this.instant = getAsInt(::instant.name)
        this.remaining = getAsInt(::remaining.name)
        this.temperature = getAsInt(::temperature.name)
        this.voltage = getAsInt(::voltage.name)
        this.chargeTimeRemaining = getAsLong(::chargeTimeRemaining.name)
        time = getAsLong(::time.name)
    }

    @Ignore
    constructor(
        event: BatteryEvent, status: BatteryStatus, level: Float,
    ) : super() {
        this.event = event
        this.status = status
        this.level = level
        this.time = System.currentTimeMillis()
        Log.d("gerwalex", "new Event: $this");
    }

    constructor(
        id: Long,
        event: BatteryEvent,
        status: BatteryStatus,
        level: Float,
        time: Long,
        capacity: Int,
        microampere: Int,
        micro_avg: Int,
        instant: Int,
        remaining: Int,
        temperature: Int,
        voltage: Int,
        chargeTimeRemaining: Long,
    ) : super() {
        this.id = id
        this.event = event
        this.status = status
        this.level = level
        this.capacity = capacity
        this.microampere = microampere
        this.micro_avg = micro_avg
        this.instant = instant
        this.remaining = remaining
        this.temperature = temperature
        this.voltage = voltage
        this.chargeTimeRemaining = chargeTimeRemaining
        this.time = time
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Event

        if (event != other.event) return false
        if (status != other.status) return false
        if (level != other.level) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + level.hashCode()
        return result
    }

    override fun toString(): String {
        return "Event(id=$id, event=$event, status=$status, level=$level, time=$time)"
    }

    fun insert() {
        dao.insert(this)
        Log.d("gerwalex", "Event inserted: $this");
    }
}