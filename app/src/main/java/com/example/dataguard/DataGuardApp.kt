package com.example.dataguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dataguard.data.local.AppDatabase
import com.example.dataguard.data.local.PlanPreferences
import com.example.dataguard.data.repository.DataGuardRepository
import com.example.dataguard.worker.UsageMonitorWorker
import java.util.concurrent.TimeUnit

class DataGuardApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var planPreferences: PlanPreferences
        private set

    lateinit var repository: DataGuardRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        planPreferences = PlanPreferences(this)
        repository = DataGuardRepository(this, database.dailySnapshotDao(), planPreferences)

        createNotificationChannels()
        schedulePeriodicMonitoring()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Channel: Data & Spike Alerts (High priority)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Usage & Spike Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when approaching data plan thresholds or during rapid usage spikes"
                enableVibration(true)
            }

            // Channel: Hotspot Reminders (Default priority)
            val hotspotChannel = NotificationChannel(
                CHANNEL_HOTSPOT,
                "Hotspot Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds when hotspot/tethering is actively consuming mobile data"
            }

            // Channel: Live Speed Monitor (Low priority, silent ongoing)
            val speedChannel = NotificationChannel(
                CHANNEL_SPEED,
                "Real-time Speed Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download and upload network speed in notification shade"
                setShowBadge(false)
            }

            nm.createNotificationChannel(alertsChannel)
            nm.createNotificationChannel(hotspotChannel)
            nm.createNotificationChannel(speedChannel)
        }
    }

    fun schedulePeriodicMonitoring() {
        val workRequest = PeriodicWorkRequestBuilder<UsageMonitorWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_PERIODIC_MONITOR,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        const val CHANNEL_ALERTS = "channel_dataguard_alerts"
        const val CHANNEL_HOTSPOT = "channel_dataguard_hotspot"
        const val CHANNEL_SPEED = "channel_dataguard_speed"

        const val WORK_PERIODIC_MONITOR = "dataguard_periodic_monitor"
    }
}
