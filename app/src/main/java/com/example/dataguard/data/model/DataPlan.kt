package com.example.dataguard.data.model

data class DataPlan(
    val monthlyAllowanceMb: Long = 10240L, // 10 GB default
    val billingResetDay: Int = 1, // 1 to 31
    val dailyBudgetMb: Long = 0L, // 0 = automatic
    val hotspotCapMb: Long = 0L, // 0 = no separate cap
    val spikeAlertThresholdMb: Long = 100L, // default 100 MB spike
    val liveSpeedEnabled: Boolean = false,
    val warning50Sent: Boolean = false,
    val warning75Sent: Boolean = false,
    val warning90Sent: Boolean = false,
    val warning100Sent: Boolean = false,
    val lastNotifiedCycle: String = "" // e.g. "2026-10"
) {
    val monthlyAllowanceBytes: Long
        get() = monthlyAllowanceMb * 1024L * 1024L

    val dailyBudgetBytes: Long
        get() = dailyBudgetMb * 1024L * 1024L

    val hotspotCapBytes: Long
        get() = hotspotCapMb * 1024L * 1024L
}
