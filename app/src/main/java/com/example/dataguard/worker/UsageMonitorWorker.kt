package com.example.dataguard.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dataguard.DataGuardApp
import com.example.dataguard.MainActivity
import com.example.dataguard.R
import com.example.dataguard.data.network.NetworkStatsHelper
import com.example.dataguard.data.repository.DataGuardRepository
import com.example.dataguard.widget.DataGuardWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageMonitorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DataGuardApp ?: return Result.failure()
        val repo = app.repository
        val prefs = app.planPreferences

        if (!repo.hasUsagePermission()) {
            return Result.success()
        }

        try {
            val plan = prefs.loadPlan()
            val cycleKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            prefs.resetCycleWarningsIfNew(cycleKey)

            // 1. Record snapshot to Room
            repo.recordTodaySnapshot()

            // 2. Fetch current cycle stats
            val stats = repo.fetchBillingCycleStats(plan)

            // 3. Check Threshold Alerts (50%, 75%, 90%, 100%)
            val percent = stats.percentUsed
            val totalMobile = stats.totalMobileBytes
            val allowance = plan.monthlyAllowanceBytes

            if (percent >= 100.0 && !plan.warning100Sent && allowance > 0) {
                sendThresholdNotification(
                    id = NOTIF_ID_100,
                    title = "Data Cap Exceeded (100%)",
                    message = "You have used ${DataGuardRepository.formatBytes(totalMobile)} of your ${DataGuardRepository.formatBytes(allowance)} plan allowance."
                )
                prefs.markWarningSent(100)
            } else if (percent >= 90.0 && !plan.warning90Sent && allowance > 0) {
                sendThresholdNotification(
                    id = NOTIF_ID_90,
                    title = "Data Warning: 90% Used",
                    message = "You have used ${DataGuardRepository.formatBytes(totalMobile)} (${String.format(Locale.getDefault(), "%.1f", percent)}%). Only ${DataGuardRepository.formatBytes(stats.remainingBytes)} left!"
                )
                prefs.markWarningSent(90)
            } else if (percent >= 75.0 && !plan.warning75Sent && allowance > 0) {
                sendThresholdNotification(
                    id = NOTIF_ID_75,
                    title = "Data Warning: 75% Used",
                    message = "You have reached 75% of your mobile data plan (${DataGuardRepository.formatBytes(totalMobile)} used)."
                )
                prefs.markWarningSent(75)
            } else if (percent >= 50.0 && !plan.warning50Sent && allowance > 0) {
                sendThresholdNotification(
                    id = NOTIF_ID_50,
                    title = "Data Notice: 50% Used",
                    message = "You are halfway through your monthly data plan (${DataGuardRepository.formatBytes(totalMobile)} used)."
                )
                prefs.markWarningSent(50)
            }

            // 4. Spike Alert Check (e.g. 100 MB spike in last interval)
            val nowTime = System.currentTimeMillis()
            val lastCheckTime = prefs.lastCheckedTimestamp
            val lastMobile = prefs.lastCheckedMobileBytes

            if (lastCheckTime > 0 && lastMobile > 0 && nowTime > lastCheckTime) {
                val deltaBytes = totalMobile - lastMobile
                val spikeThresholdBytes = plan.spikeAlertThresholdMb * 1024L * 1024L

                if (deltaBytes >= spikeThresholdBytes) {
                    // Identify top app during this window
                    val appsInWindow = NetworkStatsHelper.getAppUsageBreakdown(
                        applicationContext,
                        lastCheckTime,
                        nowTime
                    )
                    val topApp = appsInWindow.maxByOrNull { it.totalMobileBytes }
                    val topAppName = topApp?.appName ?: "An app"
                    val topAppBytesFormatted = topApp?.let { DataGuardRepository.formatBytes(it.totalMobileBytes) } ?: ""

                    sendSpikeNotification(
                        deltaFormatted = DataGuardRepository.formatBytes(deltaBytes),
                        topAppName = topAppName,
                        topAppBytesFormatted = topAppBytesFormatted
                    )
                }
            }

            // 5. Hotspot Reminder Check
            val lastHotspot = prefs.lastCheckedHotspotBytes
            val currentHotspot = stats.hotspotBytes
            if (lastHotspot > 0 && currentHotspot > lastHotspot + (5L * 1024L * 1024L)) {
                // Hotspot data increased by more than 5MB
                sendHotspotNotification(
                    currentHotspotFormatted = DataGuardRepository.formatBytes(currentHotspot)
                )
            }

            // Update cached readings
            prefs.lastCheckedTimestamp = nowTime
            prefs.lastCheckedMobileBytes = totalMobile
            prefs.lastCheckedHotspotBytes = currentHotspot

            // 6. Update App Widget
            DataGuardWidgetProvider.updateAllWidgets(applicationContext)

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendThresholdNotification(id: Int, title: String, message: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, DataGuardApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.dataguard_icon_1792840943111)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(id, notification)
    }

    private fun sendSpikeNotification(
        deltaFormatted: String,
        topAppName: String,
        topAppBytesFormatted: String
    ) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIF_ID_SPIKE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = "Mobile data jumped by $deltaFormatted! Top consumer: $topAppName ($topAppBytesFormatted)."
        val notification = NotificationCompat.Builder(applicationContext, DataGuardApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.dataguard_icon_1792840943111)
            .setContentTitle("Data Spike Detected!")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(NOTIF_ID_SPIKE, notification)
    }

    private fun sendHotspotNotification(currentHotspotFormatted: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val hotspotSettingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIF_ID_HOTSPOT,
            hotspotSettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = "Hotspot data is actively being used ($currentHotspotFormatted this cycle)."
        val notification = NotificationCompat.Builder(applicationContext, DataGuardApp.CHANNEL_HOTSPOT)
            .setSmallIcon(R.drawable.dataguard_icon_1792840943111)
            .setContentTitle("Hotspot Data Active")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "Hotspot Settings", settingsPendingIntent)
            .setContentIntent(settingsPendingIntent)
            .build()

        nm.notify(NOTIF_ID_HOTSPOT, notification)
    }

    companion object {
        const val NOTIF_ID_50 = 1050
        const val NOTIF_ID_75 = 1075
        const val NOTIF_ID_90 = 1090
        const val NOTIF_ID_100 = 1100
        const val NOTIF_ID_SPIKE = 2001
        const val NOTIF_ID_HOTSPOT = 3001
    }
}
