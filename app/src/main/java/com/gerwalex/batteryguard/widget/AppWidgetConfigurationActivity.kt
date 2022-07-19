package com.gerwalex.batteryguard.widget

import android.os.Bundle
import com.gerwalex.batteryguard.databinding.WidgetConfActivityBinding
import com.gerwalex.lib.main.BasicActivity

class AppWidgetConfigurationActivity : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(WidgetConfActivityBinding.inflate(layoutInflater).root)
        setResult(RESULT_OK)
//        MainScope().launch {
//            delay(2000)
//            finish()
//        }
        finish()
    }
}