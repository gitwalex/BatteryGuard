package com.gerwalex.batteryguard.enums

import androidx.annotation.StringRes
import com.gerwalex.batteryguard.R

enum class BatteryStatus {
    Status_Charging {

        override fun getTextResID(): Int {
            return R.string.charging
        }
    },
    Status_Discharging {

        override fun getTextResID(): Int {
            return R.string.discharging
        }
    },
    Status_Full {

        override fun getTextResID(): Int {
            return R.string.full
        }
    },
    Status_Unknown {

        override fun getTextResID(): Int {
            return R.string.unknown
        }
    };

    @StringRes
    abstract fun getTextResID(): Int
}