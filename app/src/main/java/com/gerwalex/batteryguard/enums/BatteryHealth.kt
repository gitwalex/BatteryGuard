package com.gerwalex.batteryguard.enums

import com.gerwalex.batteryguard.R

enum class BatteryHealth(private val health: Int) {
    UnKnown(1) {

        override fun getTextResID(): Int {
            return R.string.unknown
        }
    },
    Good(2) {

        override fun getTextResID(): Int {
            return R.string.good
        }
    },
    Overheat(3) {

        override fun getTextResID(): Int {
            return R.string.overheat
        }
    },
    Dead(4) {

        override fun getTextResID(): Int {
            return R.string.dead
        }
    },
    OverVoltage(5) {

        override fun getTextResID(): Int {
            return R.string.overvoltage
        }
    },
    UnspecifiedFailure(6) {

        override fun getTextResID(): Int {
            return R.string.unspecifiedFailure
        }
    },
    Cold(7) {

        override fun getTextResID(): Int {
            return R.string.cold
        }
    };

    fun getValue(health: Int): BatteryHealth? {
        values()
            .forEach {
                if (it.health == health) {
                    return it
                }
            }
        return null
    }

    abstract fun getTextResID(): Int
}