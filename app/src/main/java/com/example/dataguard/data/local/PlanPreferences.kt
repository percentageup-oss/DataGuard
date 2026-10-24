package com.example.dataguard.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.dataguard.data.model.DataPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlanPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dataguard_plan_prefs", Context.MODE_PRIVATE)

    private val _planFlow = MutableStateFlow(loadPlan())
    val planFlow: StateFlow<DataPlan> = _planFlow.asStateFlow()

    fun loadPlan(): DataPlan {
        return DataPlan(
            monthlyAllowanceMb = prefs.getLong(KEY_ALLOWANCE_MB, 10240L),
            billingResetDay = prefs.getInt(KEY_RESET_DAY, 1),
            dailyBudgetMb = prefs.getLong(KEY_DAILY_BUDGET_MB, 0L),
            hotspotCapMb = prefs.getLong(KEY_HOTSPOT_CAP_MB, 0L),
            spikeAlertThresholdMb = prefs.getLong(KEY_SPIKE_THRESHOLD_MB, 100L),
            liveSpeedEnabled = prefs.getBoolean(KEY_LIVE_SPEED, false),
            warning50Sent = prefs.getBoolean(KEY_WARN_50, false),
            warning75Sent = prefs.getBoolean(KEY_WARN_75, false),
            warning90Sent = prefs.getBoolean(KEY_WARN_90, false),
            warning100Sent = prefs.getBoolean(KEY_WARN_100, false),
            lastNotifiedCycle = prefs.getString(KEY_LAST_CYCLE, "") ?: ""
        )
    }

    fun savePlan(plan: DataPlan) {
        prefs.edit().apply {
            putLong(KEY_ALLOWANCE_MB, plan.monthlyAllowanceMb)
            putInt(KEY_RESET_DAY, plan.billingResetDay)
            putLong(KEY_DAILY_BUDGET_MB, plan.dailyBudgetMb)
            putLong(KEY_HOTSPOT_CAP_MB, plan.hotspotCapMb)
            putLong(KEY_SPIKE_THRESHOLD_MB, plan.spikeAlertThresholdMb)
            putBoolean(KEY_LIVE_SPEED, plan.liveSpeedEnabled)
            putBoolean(KEY_WARN_50, plan.warning50Sent)
            putBoolean(KEY_WARN_75, plan.warning75Sent)
            putBoolean(KEY_WARN_90, plan.warning90Sent)
            putBoolean(KEY_WARN_100, plan.warning100Sent)
            putString(KEY_LAST_CYCLE, plan.lastNotifiedCycle)
            apply()
        }
        _planFlow.value = plan
    }

    fun setLiveSpeedEnabled(enabled: Boolean) {
        val current = _planFlow.value
        val updated = current.copy(liveSpeedEnabled = enabled)
        savePlan(updated)
    }

    fun resetCycleWarningsIfNew(currentCycleKey: String) {
        val current = _planFlow.value
        if (current.lastNotifiedCycle != currentCycleKey) {
            val updated = current.copy(
                warning50Sent = false,
                warning75Sent = false,
                warning90Sent = false,
                warning100Sent = false,
                lastNotifiedCycle = currentCycleKey
            )
            savePlan(updated)
        }
    }

    fun markWarningSent(threshold: Int) {
        val current = _planFlow.value
        val updated = when (threshold) {
            50 -> current.copy(warning50Sent = true)
            75 -> current.copy(warning75Sent = true)
            90 -> current.copy(warning90Sent = true)
            100 -> current.copy(warning100Sent = true)
            else -> current
        }
        savePlan(updated)
    }

    // Previous reading for spike detection
    var lastCheckedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK_TIME, value).apply()

    var lastCheckedMobileBytes: Long
        get() = prefs.getLong(KEY_LAST_MOBILE_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_MOBILE_BYTES, value).apply()

    var lastCheckedHotspotBytes: Long
        get() = prefs.getLong(KEY_LAST_HOTSPOT_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_HOTSPOT_BYTES, value).apply()

    companion object {
        private const val KEY_ALLOWANCE_MB = "allowance_mb"
        private const val KEY_RESET_DAY = "reset_day"
        private const val KEY_DAILY_BUDGET_MB = "daily_budget_mb"
        private const val KEY_HOTSPOT_CAP_MB = "hotspot_cap_mb"
        private const val KEY_SPIKE_THRESHOLD_MB = "spike_threshold_mb"
        private const val KEY_LIVE_SPEED = "live_speed"
        private const val KEY_WARN_50 = "warn_50"
        private const val KEY_WARN_75 = "warn_75"
        private const val KEY_WARN_90 = "warn_90"
        private const val KEY_WARN_100 = "warn_100"
        private const val KEY_LAST_CYCLE = "last_cycle"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_LAST_MOBILE_BYTES = "last_mobile_bytes"
        private const val KEY_LAST_HOTSPOT_BYTES = "last_hotspot_bytes"
    }
}
