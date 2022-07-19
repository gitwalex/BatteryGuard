package com.gerwalex.batteryguard.database;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import com.gerwalex.batteryguard.R;
import com.gerwalex.batteryguard.enums.BatteryHealth;
import com.gerwalex.batteryguard.enums.BatteryPlugged;
import com.gerwalex.batteryguard.enums.BatteryStatus;
import com.gerwalex.lib.database.MyConverter;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

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
        return ((Integer) (value / 1000)).toString();
    }

    public static String convertDate(long date) {
        return DateFormat
                .getDateTimeInstance()
                .format(new Date(date));
    }

    public static int convertHealthToString(BatteryHealth health) {
        return health == null ? R.string.unknown : health.getTextResID();
    }

    public static String convertNanowattHours(long nanowattHours) {
        return ((Long) (nanowattHours / 1000)).toString();
    }

    public static String convertRemainingChargeTime(long value) {
        if (value == -1) {
            return null;
        }
        int time = (int) (value / 1000);
        int minutes = (int) (time / 60);
        int seconds = (int) (time % 60);
        return String.format(Locale.getDefault(), "%1d min,%2d sec", minutes, seconds);
    }

    public static int convertStatusToString(BatteryStatus status) {
        return status == null ? R.string.unknown : status.getTextResID();
    }

    public static String convertTemperatur(int value) {
        return ((Float) (value / 10f)).toString();
    }

    public static String convertVolt(@NonNull Integer value) {
        return value.toString();
    }
}
