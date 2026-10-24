package com.example.dataguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dataguard.DataGuardApp
import com.example.dataguard.service.SpeedMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? DataGuardApp
            app?.schedulePeriodicMonitoring()

            val plan = app?.planPreferences?.loadPlan()
            if (plan?.liveSpeedEnabled == true) {
                SpeedMonitorService.start(context)
            }
        }
    }
}
