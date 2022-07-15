package com.gerwalex.batteryguard.widget

import android.os.Bundle
import com.gerwalex.batteryguard.databinding.WidgetConfActivityBinding
import com.gerwalex.batteryguard.system.BatteryWorkerService
import com.gerwalex.lib.main.BasicActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppWidgetConfigurationActivity : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(WidgetConfActivityBinding.inflate(layoutInflater).root)
        BatteryWorkerService.startService(this)
        setResult(RESULT_OK)
        MainScope().launch {
            delay(2000)
            finish()
        }
    }
}