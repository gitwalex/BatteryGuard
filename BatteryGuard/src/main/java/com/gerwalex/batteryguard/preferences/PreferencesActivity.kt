package com.gerwalex.batteryguard.preferences

import android.os.Bundle
import com.gerwalex.batteryguard.databinding.PreferencesActivityBinding
import com.gerwalex.lib.main.BasicActivity

class PreferencesActivity : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(PreferencesActivityBinding.inflate(layoutInflater).root)
        setResult(RESULT_OK)
    }
}