package com.gerwalex.batteryguard.database;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import com.gerwalex.batteryguard.enums.BatteryHealth;
import com.gerwalex.batteryguard.enums.BatteryPlugged;
import com.gerwalex.batteryguard.enums.BatteryStatus;
import com.gerwalex.lib.database.MyConverter;

import java.text.DateFormat;
import java.util.Date;

public class BatteryGuardConverter extends MyConverter {

    public static String convertAmpere(@NonNull Integer value) {
        return value.toString();
    }

    @TypeConverter
    public static int convertBatteryHealt(BatteryHealth health) {
        return health.ordinal();
    }

    @TypeConverter
    public static BatteryHealth convertBatteryHealth(int health) {
        return BatteryHealth.values()[health];
    }

    @TypeConverter
    public static BatteryPlugged convertBatteryPlugged(int plugged) {
        return BatteryPlugged.values()[plugged];
    }

    @TypeConverter
    public static int convertBatteryPlugged(BatteryPlugged plugged) {
        return plugged.ordinal();
    }

    @TypeConverter
    public static BatteryStatus convertBatteryStatus(int status) {
        return BatteryStatus.values()[status];
    }

    @TypeConverter
    public static int convertBatteryStatus(BatteryStatus status) {
        return status.ordinal();
    }

    public static String convertBoolean(@NonNull Boolean value) {
        return value.toString();
    }

    public static String convertCapacity(@NonNull Integer value) {
        return value.toString();
    }

    public static String convertDate(long date) {
        return DateFormat
                .getDateTimeInstance()
                .format(new Date(date));
    }

    public static String convertRemaining(@NonNull Long value) {
        return value.toString();
    }

    public static String convertTemperatur(@NonNull Integer value) {
        return value.toString();
    }

    public static String convertVolt(@NonNull Integer value) {
        return value.toString();
    }
}
