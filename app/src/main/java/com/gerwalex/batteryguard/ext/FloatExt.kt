package com.gerwalex.batteryguard.ext

import android.content.res.Resources

object FloatExt {

    fun Float.pxToDP(): Float = (this / Resources.getSystem().displayMetrics.density)
    fun Float.dpToPx(): Float = (this * Resources.getSystem().displayMetrics.density)
    fun Float.pxToSp(): Float = (this / Resources.getSystem().displayMetrics.scaledDensity)
    fun Float.spToPx(): Float = (this * Resources.getSystem().displayMetrics.scaledDensity)
}