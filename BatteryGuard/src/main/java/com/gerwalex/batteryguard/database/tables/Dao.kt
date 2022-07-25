package com.gerwalex.batteryguard.database.tables

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gerwalex.batteryguard.database.DB

@Dao
abstract class Dao(val db: DB) {

    @Insert
    abstract fun insert(event: Event): Long

    @Query("select level from event group by ts having max(ts)")
    abstract fun getBatteryLevel(): Long

    @Query("Select * from event order by ts")
    abstract fun getEventList(): Cursor

    @Query("Select * from event where ts > :from order by ts")
    abstract fun getEventList(from: Long): Cursor

    @Query("Select * from event order by ts desc")
    abstract fun getEventListDesc(): Cursor

    @Query("select * from event group by ts having max(ts)")
    abstract fun getLastEvent(): Event?
}