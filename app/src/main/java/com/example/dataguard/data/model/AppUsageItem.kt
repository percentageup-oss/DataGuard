package com.example.dataguard.data.model

import android.graphics.drawable.Drawable

data class AppUsageItem(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val foregroundBytes: Long = 0L,
    val backgroundBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val wifiBytes: Long = 0L,
    val isSystemApp: Boolean = false,
    val icon: Drawable? = null
) {
    val totalMobileBytes: Long
        get() = foregroundBytes + backgroundBytes

    val totalBytes: Long
        get() = mobileBytes + wifiBytes
}
