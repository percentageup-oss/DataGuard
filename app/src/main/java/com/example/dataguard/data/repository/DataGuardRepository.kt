package com.example.dataguard.data.repository

import android.content.Context
import android.net.ConnectivityManager
import com.example.dataguard.data.local.DailySnapshotDao
import com.example.dataguard.data.local.PlanPreferences
import com.example.dataguard.data.model.AppUsageItem
import com.example.dataguard.data.model.DailySnapshot
import com.example.dataguard.data.model.DataPlan
import com.example.dataguard.data.network.NetworkStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DataGuardRepository(
    private val context: Context,
    private val dao: DailySnapshotDao,
    private val planPrefs: PlanPreferences
) {
    val planFlow: StateFlow<DataPlan> = planPrefs.planFlow

    val allSnapshots: Flow<List<DailySnapshot>> = dao.getAllSnapshots()

    fun getSnapshotsBetween(startDate: String, endDate: String): Flow<List<DailySnapshot>> {
        return dao.getSnapshotsBetween(startDate, endDate)
    }

    suspend fun savePlan(plan: DataPlan) {
        planPrefs.savePlan(plan)
    }

    suspend fun setLiveSpeedEnabled(enabled: Boolean) {
        planPrefs.setLiveSpeedEnabled(enabled)
    }

    fun hasUsagePermission(): Boolean = NetworkStatsHelper.hasUsageStatsPermission(context)

    /**
     * Records or updates today's snapshot in Room database.
     */
    suspend fun recordTodaySnapshot(): DailySnapshot = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(Date())

        val (startToday, endToday) = NetworkStatsHelper.getTodayTimeRange()

        val (mobileRx, mobileTx) = NetworkStatsHelper.getNetworkUsageSummary(
            context,
            ConnectivityManager.TYPE_MOBILE,
            startToday,
            endToday
        )
        val (wifiRx, wifiTx) = NetworkStatsHelper.getNetworkUsageSummary(
            context,
            ConnectivityManager.TYPE_WIFI,
            startToday,
            endToday
        )
        val (hotspotBytes, _) = NetworkStatsHelper.getHotspotUsage(
            context,
            startToday,
            endToday
        )

        // Find top app today
        val apps = NetworkStatsHelper.getAppUsageBreakdown(context, startToday, endToday)
        val topApp = apps.maxByOrNull { it.totalMobileBytes }

        val snapshot = DailySnapshot(
            date = todayStr,
            mobileRxBytes = mobileRx,
            mobileTxBytes = mobileTx,
            wifiRxBytes = wifiRx,
            wifiTxBytes = wifiTx,
            hotspotBytes = hotspotBytes,
            topAppPackage = topApp?.packageName ?: "",
            topAppName = topApp?.appName ?: "",
            topAppBytes = topApp?.totalMobileBytes ?: 0L,
            timestamp = System.currentTimeMillis()
        )

        dao.insertOrUpdate(snapshot)
        snapshot
    }

    /**
     * Fetches current billing cycle stats.
     */
    suspend fun fetchBillingCycleStats(plan: DataPlan): BillingCycleStats = withContext(Dispatchers.IO) {
        val (cycleStart, cycleEnd) = NetworkStatsHelper.calculateBillingCycleRange(plan.billingResetDay)
        val now = System.currentTimeMillis()

        val (mobileRx, mobileTx) = NetworkStatsHelper.getNetworkUsageSummary(
            context,
            ConnectivityManager.TYPE_MOBILE,
            cycleStart,
            now
        )
        val (wifiRx, wifiTx) = NetworkStatsHelper.getNetworkUsageSummary(
            context,
            ConnectivityManager.TYPE_WIFI,
            cycleStart,
            now
        )
        val (hotspotBytes, hotspotSupported) = NetworkStatsHelper.getHotspotUsage(
            context,
            cycleStart,
            now
        )

        val (todayStart, todayEnd) = NetworkStatsHelper.getTodayTimeRange()
        val (todayRx, todayTx) = NetworkStatsHelper.getNetworkUsageSummary(
            context,
            ConnectivityManager.TYPE_MOBILE,
            todayStart,
            todayEnd
        )
        val (todayWifiRx, todayWifiTx) = NetworkStatsHelper.getNetworkUsageSummary(
            context,
            ConnectivityManager.TYPE_WIFI,
            todayStart,
            todayEnd
        )
        val (todayHotspot, _) = NetworkStatsHelper.getHotspotUsage(
            context,
            todayStart,
            todayEnd
        )

        val totalMobileBytes = mobileRx + mobileTx
        val totalWifiBytes = wifiRx + wifiTx
        val todayMobileBytes = todayRx + todayTx
        val todayWifiTotal = todayWifiRx + todayWifiTx

        val allowanceBytes = plan.monthlyAllowanceBytes
        val remainingBytes = (allowanceBytes - totalMobileBytes).coerceAtLeast(0L)
        val percentUsed = if (allowanceBytes > 0) {
            ((totalMobileBytes.toDouble() / allowanceBytes.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        // Calculate days left in cycle
        val diffMillis = (cycleEnd - now).coerceAtLeast(0L)
        val daysLeftInCycle = ((diffMillis / (1000L * 60 * 60 * 24)) + 1).toInt().coerceAtLeast(1)

        // Elapsed days in cycle
        val elapsedMillis = (now - cycleStart).coerceAtLeast(1L)
        val elapsedDays = ((elapsedMillis / (1000L * 60 * 60 * 24)) + 1).toInt().coerceAtLeast(1)

        // Average daily pace
        val avgPaceBytesPerDay = totalMobileBytes / elapsedDays

        // Forecast run out date
        val forecastRunOutDate: String = if (avgPaceBytesPerDay > 0 && remainingBytes > 0) {
            val daysUntilDepleted = remainingBytes / avgPaceBytesPerDay
            if (daysUntilDepleted < daysLeftInCycle) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, daysUntilDepleted.toInt())
                val format = SimpleDateFormat("MMM d", Locale.getDefault())
                format.format(cal.time)
            } else {
                "Safe (covers full cycle)"
            }
        } else if (remainingBytes == 0L) {
            "Run out (Allowance depleted)"
        } else {
            "Safe (no usage pace)"
        }

        // Daily recommended budget
        val calculatedDailyBudget = if (plan.dailyBudgetBytes > 0) {
            plan.dailyBudgetBytes
        } else if (daysLeftInCycle > 0) {
            remainingBytes / daysLeftInCycle
        } else 0L

        BillingCycleStats(
            cycleStartMillis = cycleStart,
            cycleEndMillis = cycleEnd,
            daysLeftInCycle = daysLeftInCycle,
            elapsedDays = elapsedDays,
            totalMobileBytes = totalMobileBytes,
            totalWifiBytes = totalWifiBytes,
            hotspotBytes = hotspotBytes,
            hotspotSupported = hotspotSupported,
            todayMobileBytes = todayMobileBytes,
            todayWifiBytes = todayWifiTotal,
            todayHotspotBytes = todayHotspot,
            remainingBytes = remainingBytes,
            percentUsed = percentUsed,
            avgPaceBytesPerDay = avgPaceBytesPerDay,
            forecastRunOutDate = forecastRunOutDate,
            dailyBudgetBytes = calculatedDailyBudget
        )
    }

    /**
     * Exports Room snapshots to CSV format in cache directory for sharing.
     */
    suspend fun exportUsageReportCsv(snapshots: List<DailySnapshot>): File = withContext(Dispatchers.IO) {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val csvFile = File(reportsDir, "DataGuard_Report_$timestamp.csv")

        csvFile.bufferedWriter().use { writer ->
            writer.write("Date,Mobile Usage (MB),Wi-Fi Usage (MB),Hotspot Usage (MB),Top App Name,Top App Package,Top App MB\n")
            for (snap in snapshots) {
                val mobileMb = String.format(Locale.US, "%.2f", snap.totalMobileBytes / (1024.0 * 1024.0))
                val wifiMb = String.format(Locale.US, "%.2f", snap.totalWifiBytes / (1024.0 * 1024.0))
                val hotspotMb = String.format(Locale.US, "%.2f", snap.hotspotBytes / (1024.0 * 1024.0))
                val topMb = String.format(Locale.US, "%.2f", snap.topAppBytes / (1024.0 * 1024.0))
                val cleanAppName = snap.topAppName.replace(",", " ")
                val cleanPkg = snap.topAppPackage.replace(",", " ")
                writer.write("${snap.date},$mobileMb,$wifiMb,$hotspotMb,$cleanAppName,$cleanPkg,$topMb\n")
            }
        }

        csvFile
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            val tb = gb / 1024.0

            return when {
                tb >= 1.0 -> String.format(Locale.getDefault(), "%.2f TB", tb)
                gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

data class BillingCycleStats(
    val cycleStartMillis: Long = 0L,
    val cycleEndMillis: Long = 0L,
    val daysLeftInCycle: Int = 1,
    val elapsedDays: Int = 1,
    val totalMobileBytes: Long = 0L,
    val totalWifiBytes: Long = 0L,
    val hotspotBytes: Long = 0L,
    val hotspotSupported: Boolean = true,
    val todayMobileBytes: Long = 0L,
    val todayWifiBytes: Long = 0L,
    val todayHotspotBytes: Long = 0L,
    val remainingBytes: Long = 0L,
    val percentUsed: Double = 0.0,
    val avgPaceBytesPerDay: Long = 0L,
    val forecastRunOutDate: String = "",
    val dailyBudgetBytes: Long = 0L
)
