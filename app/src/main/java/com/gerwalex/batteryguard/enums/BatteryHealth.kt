package com.gerwalex.batteryguard.enums

enum class BatteryHealth(private val health: Int) {
    UnKnown(1), Good(2), Overheat(3), Dead(4), OverVoltage(5), UnspecifiedFailure(6), Cold(7);

    fun getValue(health: Int): BatteryHealth? {
        values()
            .forEach {
                if (it.health == health) {
                    return it
                }
            }
        return null
    }
}