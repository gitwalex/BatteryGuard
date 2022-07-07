package com.gerwalex.batteryguard.ext

import android.content.res.Resources

object IntExt {

    fun Int.pxToDP(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()
    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
    fun Int.pxToSp(): Int = (this / Resources.getSystem().displayMetrics.scaledDensity).toInt()
    fun Int.spToPx(): Int = (this * Resources.getSystem().displayMetrics.scaledDensity).toInt()
}