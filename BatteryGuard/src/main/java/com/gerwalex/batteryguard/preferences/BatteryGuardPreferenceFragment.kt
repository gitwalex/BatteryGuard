package com.gerwalex.batteryguard.preferences

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.core.app.NotificationManagerCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.ext.ContextExt.areNotificationsEnabled
import com.gerwalex.lib.main.ActivityResultWrapper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryGuardPreferenceFragment : PreferenceFragmentCompat() {

    val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    protected var activityLauncher = ActivityResultWrapper
        .registerActivityForResult(this)
        .apply {
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (savedInstanceState == null) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            if (!requireContext().areNotificationsEnabled()) {
                prefs
                    .edit()
                    .putBoolean(getString(R.string.pkNotificationsOn), false)
                    .apply()
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference is SwitchPreference) {
            if (preference.isChecked)
                when (preference.key) {
                    getString(R.string.pkNotificationsOn) -> {
                        preference.let { sw ->
                            if (!requireContext().areNotificationsEnabled()) {
                                Snackbar
                                    .make(requireView(), R.string.notificationsNotAllowed, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.change) {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", it.context.packageName, null)
                                        intent.data = uri
                                        startActivity(intent)
                                    }
                                    .show()
                                sw.isChecked = false
                            }
                        }
                    }
                    getString(R.string.pkPlugInSound) ->
                        selectSound(getString(R.string.plugInSound), RingtoneManager.TYPE_NOTIFICATION)
                    getString(R.string.pkPlugOutSound) ->
                        selectSound(getString(R.string.plugOutSound), RingtoneManager.TYPE_NOTIFICATION)
                    getString(R.string.pkHighChargeSound) ->
                        selectSound(getString(R.string.highChargeSound), RingtoneManager.TYPE_ALARM)
                    getString(R.string.pkHighTemperature) ->
                        selectSound(getString(R.string.highTemperatureSound), RingtoneManager.TYPE_ALARM)
                    getString(R.string.pkLowChargeSound) ->
                        selectSound(getString(R.string.lowChargeSound), RingtoneManager.TYPE_ALARM)
                }
        }
        return true
    }

    private fun areNotificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!manager.areNotificationsEnabled()) {
                return false
            }
            val channels = manager.notificationChannels
            channels.forEach {
                if (it.importance == NotificationManager.IMPORTANCE_NONE) {
                    return false
                }
            }
            return true
        } else {
            return NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled()
        }
    }

    private fun selectSound(title: String, ringtoneType: Int) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType)
        prefs
            .getString(title, null)
            ?.let {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
            }
        activityLauncher.setOnActivityResult(object : ActivityResultWrapper.OnActivityResult<ActivityResult> {
            override fun onActivityResult(result: ActivityResult) {
                result.data?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        it
                            .getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                            ?.let { uri ->
                                Log.d("gerwalex", "ringToneSound = $uri")
                                prefs
                                    .edit()
                                    .putString(title, uri.toString())
                                    .apply()
                            }
                    }
                }
            }
        })
        activityLauncher.launch(intent)
    }
}