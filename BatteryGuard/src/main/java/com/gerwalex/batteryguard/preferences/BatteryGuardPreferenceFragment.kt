package com.gerwalex.batteryguard.preferences

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.gerwalex.batteryguard.R
import com.gerwalex.lib.main.ActivityResultWrapper

class BatteryGuardPreferenceFragment : PreferenceFragmentCompat() {

    protected var activityLauncher = ActivityResultWrapper
        .registerActivityForResult(this)
        .apply {
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (savedInstanceState == null) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.pkPlugInSound) ->
                selectSound(R.string.plugInSound, RingtoneManager.TYPE_NOTIFICATION)
            getString(R.string.pkPlugOutSound) ->
                selectSound(R.string.plugOutSound, RingtoneManager.TYPE_NOTIFICATION)
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun selectSound(@StringRes titleResId: Int, ringtoneType: Int) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(titleResId))
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType)
        activityLauncher.setOnActivityResult(object : ActivityResultWrapper.OnActivityResult<ActivityResult> {
            override fun onActivityResult(result: ActivityResult) {
                result.data?.let {
                    it
                        .getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                        ?.let { uri ->
                            Log.d("gerwalex", "ringToneSound = $uri")
                            when (titleResId) {
                                R.string.plugInSound -> {
                                    Log.d("gerwalex", "plugInSound: $uri")
                                }
                                else -> {
                                    throw IllegalArgumentException("id nicht bekannt")
                                }
                            }
                        }
                }
            }
        })
        activityLauncher.launch(intent)
    }
}