package com.gerwalex.batteryguard.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.gerwalex.batteryguard.R

class TextViewPreference : Preference {

    private var st: TextView? = null
    private var text: String? = null
        set(value) {
            st?.text = value
            field = value
        }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs,
        defStyleAttr, defStyleRes) {
        attrs?.let { init(it) }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        attrs?.let { init(it) }
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        attrs?.let { init(it) }
    }

    constructor(context: Context) : super(context)

    private fun init(attrs: AttributeSet) {
        widgetLayoutResource = R.layout.text_view_preference
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.TextViewPreference)
        try {
            text = a.getString(R.styleable.TextViewPreference_preferencetext)
        } finally {
            a.recycle()
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        st = (holder.findViewById(R.id.soundTitle) as TextView).also {
            it.text = text
        }
    }
}