package com.gerwalex.batteryguard.preferences

import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.ext.ContextExt.areNotificationsEnabled
import com.google.android.material.snackbar.Snackbar
import xyz.aprildown.ultimateringtonepicker.RingtonePickerDialog
import xyz.aprildown.ultimateringtonepicker.UltimateRingtonePicker

class BatteryGuardPreferenceFragment : PreferenceFragmentCompat() {

    private val soundPreferenceList = listOf(
        R.string.pkPlugInSoundFile,
        R.string.pkPlugOutSoundFile,
        R.string.pkHighChargeSoundFile,
        R.string.pkHighTemperatureSoundFile,
        R.string.pkLowChargeSoundFile
    )
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (savedInstanceState == null) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            if (!requireContext().areNotificationsEnabled()) {
                prefs
                    .edit()
                    .putBoolean(getString(R.string.pkNotificationsOn), false)
                    .apply()
            }
            soundPreferenceList.forEach {
                val key = getString(it)
                findPreference<Preference>(key)?.let { preference ->
                    prefs
                        .getString(key, null)
                        ?.let {
                            preference.summary = it
                        }
                }
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.pkNotificationsOn) -> {
                (preference as SwitchPreference).let { sw ->
                    if (preference.isChecked)
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
            else -> {
                openRingtoneDialog(preference)
            }
        }
        return true
    }

    private fun openRingtoneDialog(preference: Preference) {
        val ringtoneTypes = ArrayList<Int>()
        var title: String? = null
        when (preference.key) {
            getString(R.string.pkPlugInSoundFile) -> {
                title = getString(R.string.plugInSound)
                ringtoneTypes.add(RingtoneManager.TYPE_NOTIFICATION)
            }
            getString(R.string.pkPlugOutSoundFile) -> {
                title = getString(R.string.plugOutSound)
                ringtoneTypes.add(RingtoneManager.TYPE_NOTIFICATION)
            }
            getString(R.string.pkHighChargeSoundFile) -> {
                title = getString(R.string.highChargeSound)
                ringtoneTypes.add(RingtoneManager.TYPE_ALARM)
            }
            getString(R.string.pkHighTemperatureSoundFile) -> {
                title = getString(R.string.highTemperatureSound)
                ringtoneTypes.add(RingtoneManager.TYPE_ALARM)
            }
            getString(R.string.pkLowChargeSoundFile) -> {
                title = getString(R.string.lowChargeSound)
                ringtoneTypes.add(RingtoneManager.TYPE_ALARM)
            }
            else -> {
                //
            }
        }
        title?.let {
            preference as RingTonePreference
            val preSelectedUris = ArrayList<Uri>()
            preference.uri?.let {
                preSelectedUris.add(Uri.parse(it))
            }
            val settings = UltimateRingtonePicker.Settings(
                preSelectUris = preSelectedUris,
                systemRingtonePicker = UltimateRingtonePicker.SystemRingtonePicker(
                    defaultSection = UltimateRingtonePicker.SystemRingtonePicker.DefaultSection(),
                    ringtoneTypes = ringtoneTypes
                )
            )
            RingtonePickerDialog
                .createEphemeralInstance(
                    settings = settings,
                    dialogTitle = title,
                    listener = object : UltimateRingtonePicker.RingtonePickerListener {
                        override fun onRingtonePicked(ringtones: List<UltimateRingtonePicker.RingtoneEntry>) {
                            if (ringtones.isNotEmpty()) {
                                ringtones[0].run {
                                    preference.uri = uri.toString()
                                }
                            }
                        }
                    }
                )
                .show(childFragmentManager, null)
        }
    }
}