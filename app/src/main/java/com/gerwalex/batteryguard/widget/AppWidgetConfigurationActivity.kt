package com.gerwalex.batteryguard.widget

import android.os.Bundle
import com.gerwalex.batteryguard.enums.BatteryEvent
import com.gerwalex.batteryguard.system.BatteryWidgetUpdateWorker
import com.gerwalex.batteryguard.system.BatteryWorkerService
import com.gerwalex.lib.main.BasicActivity

class AppWidgetConfigurationActivity : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prefs.getBoolean(BatteryWorkerService.SERVICE_REQUIRED, false)) {
            prefs
                .edit()
                .putBoolean(BatteryWorkerService.SERVICE_REQUIRED, true)
                .commit()
            BatteryWorkerService.startService(this)
        }
        BatteryWidgetUpdateWorker.startUpdateWidget(this,
            BatteryWorkerService.getEvent(this, BatteryEvent.UpdateWidget))
        setResult(RESULT_OK)
        finish()
    }
}