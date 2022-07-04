package com.gerwalex.batteryguard.database.tables

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gerwalex.batteryguard.database.DB

@Dao
abstract class Dao(val db: DB) {

    @Insert
    abstract fun insert(event: Event)

    @Query("select level from event group by time having max(time)")
    abstract fun getBatteryLevel(): Long

    @Query("Select * from event order by time desc")
    abstract fun getEventList(): Cursor
}