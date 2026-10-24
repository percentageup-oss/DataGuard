package com.example.dataguard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_snapshots")
data class DailySnapshot(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val hotspotBytes: Long = 0L,
    val topAppPackage: String = "",
    val topAppName: String = "",
    val topAppBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalMobileBytes: Long
        get() = mobileRxBytes + mobileTxBytes

    val totalWifiBytes: Long
        get() = wifiRxBytes + wifiTxBytes

    val totalCombinedBytes: Long
        get() = totalMobileBytes + totalWifiBytes
}
