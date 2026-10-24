package com.example.dataguard.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dataguard.DataGuardApp
import com.example.dataguard.MainActivity
import com.example.dataguard.R
import com.example.dataguard.data.repository.DataGuardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class SpeedMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastCheckTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotif = buildSpeedNotification("Calculating network speed...", "DataGuard Active")
        startForeground(NOTIF_ID, initialNotif)

        startSpeedSampling()

        return START_STICKY
    }

    private fun startSpeedSampling() {
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastCheckTime = System.currentTimeMillis()

        serviceScope.launch {
            while (isActive) {
                delay(1500)
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val timeDeltaSec = (currentTime - lastCheckTime) / 1000.0
                if (timeDeltaSec > 0 && lastRxBytes >= 0 && lastTxBytes >= 0) {
                    val rxSpeedBytes = ((currentRx - lastRxBytes) / timeDeltaSec).toLong().coerceAtLeast(0L)
                    val txSpeedBytes = ((currentTx - lastTxBytes) / timeDeltaSec).toLong().coerceAtLeast(0L)

                    val rxFormatted = formatSpeed(rxSpeedBytes)
                    val txFormatted = formatSpeed(txSpeedBytes)

                    val title = "↓ $rxFormatted  ↑ $txFormatted"
                    val subtitle = "Real-time network speed monitor"

                    val updatedNotif = buildSpeedNotification(title, subtitle)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.notify(NOTIF_ID, updatedNotif)
                }

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastCheckTime = currentTime
            }
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB/s", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB/s", kb)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun buildSpeedNotification(title: String, subtitle: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SpeedMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, DataGuardApp.CHANNEL_SPEED)
            .setSmallIcon(R.drawable.dataguard_icon_1792840943111)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Turn Off", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        val app = application as? DataGuardApp
        app?.planPreferences?.setLiveSpeedEnabled(false)
    }

    companion object {
        const val NOTIF_ID = 5001
        const val ACTION_START = "ACTION_START_SPEED"
        const val ACTION_STOP = "ACTION_STOP_SPEED"

        fun start(context: Context) {
            val intent = Intent(context, SpeedMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SpeedMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }
}
