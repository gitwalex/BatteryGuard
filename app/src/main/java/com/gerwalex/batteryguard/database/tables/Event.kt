package com.gerwalex.batteryguard.database.tables

import android.database.Cursor
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

    constructor(c: Cursor) : super(c) {
        id = getAsLongOrNull("_id")
        event = BatteryEvent.values()[getAsInt(::event.name)]
        status = BatteryStatus.values()[getAsInt(::status.name)]
        level = getAsFloat(::level.name)
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
        id: Long, event: BatteryEvent, status: BatteryStatus, level: Float, time: Long,
    ) : super() {
        this.id = id
        this.event = event
        this.status = status
        this.level = level
        this.time = time
    }

    fun insert() {
        dao.insert(this)
        Log.d("gerwalex", "Event inserted: $this");
    }

    override fun toString(): String {
        return "Event(id=$id, event=$event, status=$status, level=$level, time=$time)"
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
}