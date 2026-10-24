package com.example.dataguard.data.network

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import com.example.dataguard.data.model.AppUsageItem
import java.util.Calendar

object NetworkStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getNetworkUsageSummary(
        context: Context,
        networkType: Int,
        startTime: Long,
        endTime: Long
    ): Pair<Long, Long> {
        if (!hasUsageStatsPermission(context)) return Pair(0L, 0L)
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return Pair(0L, 0L)

        var totalRx = 0L
        var totalTx = 0L

        try {
            val stats = nsm.querySummary(networkType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                totalRx += bucket.rxBytes
                totalTx += bucket.txBytes
            }
            stats.close()
        } catch (e: Exception) {
            // Log or fallback
        }

        return Pair(totalRx, totalTx)
    }

    /**
     * Query tethering / hotspot usage using UID_TETHERING (-5).
     * Returns Pair(bytes, isSupported).
     */
    fun getHotspotUsage(
        context: Context,
        startTime: Long,
        endTime: Long
    ): Pair<Long, Boolean> {
        if (!hasUsageStatsPermission(context)) return Pair(0L, false)
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return Pair(0L, false)

        var totalTetherBytes = 0L
        var supported = false

        try {
            val stats = nsm.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime,
                NetworkStats.Bucket.UID_TETHERING
            )
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                totalTetherBytes += (bucket.rxBytes + bucket.txBytes)
                supported = true
            }
            stats.close()
        } catch (e: SecurityException) {
            supported = false
        } catch (e: Exception) {
            // Some OEM ROMs do not record UID_TETHERING in NetworkStatsService
            supported = false
        }

        return Pair(totalTetherBytes, supported)
    }

    /**
     * Query per-app mobile usage, split into foreground vs background.
     */
    fun getAppUsageBreakdown(
        context: Context,
        startTime: Long,
        endTime: Long
    ): List<AppUsageItem> {
        if (!hasUsageStatsPermission(context)) return emptyList()
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return emptyList()
        val pm = context.packageManager

        class UidStat(
            var foregroundMobile: Long = 0L,
            var backgroundMobile: Long = 0L,
            var wifiTotal: Long = 0L
        )

        val statsMap = mutableMapOf<Int, UidStat>()

        // 1. Mobile details (foreground vs background)
        try {
            val mobileStats = nsm.queryDetails(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            val bucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                val uid = bucket.uid
                if (uid < 0) continue // Skip non-app or system internal non-UIDs

                val stat = statsMap.getOrPut(uid) { UidStat() }
                val bytes = bucket.rxBytes + bucket.txBytes
                if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                    stat.foregroundMobile += bytes
                } else {
                    stat.backgroundMobile += bytes
                }
            }
            mobileStats.close()
        } catch (e: Exception) {
            // ignore
        }

        // 2. Wi-Fi details
        try {
            val wifiStats = nsm.queryDetails(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                val uid = bucket.uid
                if (uid < 0) continue

                val stat = statsMap.getOrPut(uid) { UidStat() }
                stat.wifiTotal += (bucket.rxBytes + bucket.txBytes)
            }
            wifiStats.close()
        } catch (e: Exception) {
            // ignore
        }

        // 3. Resolve app names and packages
        val result = mutableListOf<AppUsageItem>()
        for ((uid, stat) in statsMap) {
            val packages = pm.getPackagesForUid(uid)
            val packageName = packages?.firstOrNull() ?: "UID: $uid"
            var appName = packageName
            var isSystem = false
            var icon: android.graphics.drawable.Drawable? = null

            if (packages != null && packages.isNotEmpty()) {
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    appName = pm.getApplicationLabel(appInfo).toString()
                    icon = pm.getApplicationIcon(appInfo)
                    isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: PackageManager.NameNotFoundException) {
                    appName = packageName
                }
            } else if (uid == 1000) {
                appName = "Android OS"
                isSystem = true
            } else if (uid == NetworkStats.Bucket.UID_TETHERING) {
                appName = "Tethering & Hotspot"
                isSystem = true
            }

            val totalMobile = stat.foregroundMobile + stat.backgroundMobile
            if (totalMobile > 0 || stat.wifiTotal > 0) {
                result.add(
                    AppUsageItem(
                        uid = uid,
                        packageName = packageName,
                        appName = appName,
                        foregroundBytes = stat.foregroundMobile,
                        backgroundBytes = stat.backgroundMobile,
                        mobileBytes = totalMobile,
                        wifiBytes = stat.wifiTotal,
                        isSystemApp = isSystem,
                        icon = icon
                    )
                )
            }
        }

        return result.sortedByDescending { it.mobileBytes }
    }

    /**
     * Calculates the exact start and end millisecond timestamps for the current billing cycle,
     * resetting exactly on [resetDay].
     */
    fun calculateBillingCycleRange(resetDay: Int): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)

        val cycleStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cycleEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        if (currentDay >= resetDay) {
            // Cycle started this month on resetDay
            val maxDaysThisMonth = cycleStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            val safeDay = resetDay.coerceAtMost(maxDaysThisMonth)
            cycleStart.set(Calendar.DAY_OF_MONTH, safeDay)

            // Cycle ends next month on (resetDay - 1)
            cycleEnd.add(Calendar.MONTH, 1)
            val maxDaysNextMonth = cycleEnd.getActualMaximum(Calendar.DAY_OF_MONTH)
            val safeEndDay = (resetDay - 1).coerceIn(1, maxDaysNextMonth)
            cycleEnd.set(Calendar.DAY_OF_MONTH, safeEndDay)
        } else {
            // Cycle started last month on resetDay
            cycleStart.add(Calendar.MONTH, -1)
            val maxDaysLastMonth = cycleStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            val safeDay = resetDay.coerceAtMost(maxDaysLastMonth)
            cycleStart.set(Calendar.DAY_OF_MONTH, safeDay)

            // Cycle ends this month on (resetDay - 1)
            val maxDaysThisMonth = cycleEnd.getActualMaximum(Calendar.DAY_OF_MONTH)
            val safeEndDay = (resetDay - 1).coerceIn(1, maxDaysThisMonth)
            cycleEnd.set(Calendar.DAY_OF_MONTH, safeEndDay)
        }

        return Pair(cycleStart.timeInMillis, cycleEnd.timeInMillis)
    }

    /**
     * Start of today (00:00:00.000) to current time.
     */
    fun getTodayTimeRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, now)
    }
}
