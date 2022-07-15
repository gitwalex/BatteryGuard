package com.gerwalex.batteryguard.database;

import androidx.room.TypeConverter;

import com.gerwalex.batteryguard.enums.BatteryStatus;
import com.gerwalex.lib.database.MyConverter;

import java.text.DateFormat;
import java.util.Date;

public class BatteryGuardConverter extends MyConverter {

    @TypeConverter
    public static int convertBatteryStatus(BatteryStatus status) {
        return status.ordinal();
    }

    @TypeConverter
    public static BatteryStatus convertBatteryStatus(int status) {
        return BatteryStatus.values()[status];
    }

    public static String convertDate(long date) {
        return DateFormat
                .getDateTimeInstance()
                .format(new Date(date));
    }
}
