package com.gerwalex.batteryguard.preferences

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.gerwalex.batteryguard.R

class RingTonePreference : Preference {

    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    private var st: TextView? = null
    var uri: String? = null
        set(value) {
            field = value
            val editor = prefs.edit()
            val text = getTitle(uri)
            text?.run {
                editor.putString(key, field)
                st?.text = text
            } ?: run {
                editor.remove(key)
                st?.text = context.getString(xyz.aprildown.ultimateringtonepicker.R.string.urp_silent_ringtone_title)
            }
            editor.apply()
        }

    private fun getTitle(uri: String?): String? {
        uri?.let {
            val mUri = Uri.parse(uri)
            context.contentResolver
                .query(mUri, null, null, null, null)
                ?.let { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndex("title"))
                    }
                }
        }
        return null
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs,
        defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        widgetLayoutResource = R.layout.text_view_preference
        uri = prefs.getString(key, null)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        st = (holder.findViewById(R.id.soundTitle) as TextView).also {
            it.text = getTitle(uri)
        }
    }
}