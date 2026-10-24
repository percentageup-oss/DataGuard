package com.example.dataguard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.dataguard.DataGuardApp
import com.example.dataguard.MainActivity
import com.example.dataguard.R
import com.example.dataguard.data.repository.DataGuardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataGuardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as? DataGuardApp ?: return
        val repo = app.repository
        val prefs = app.planPreferences

        CoroutineScope(Dispatchers.IO).launch {
            val plan = prefs.loadPlan()
            val stats = repo.fetchBillingCycleStats(plan)

            for (widgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId, stats)
            }
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            stats: com.example.dataguard.data.repository.BillingCycleStats
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_dataguard)

            val remainingFormatted = "${DataGuardRepository.formatBytes(stats.remainingBytes)} left"
            val todayFormatted = "Used today: ${DataGuardRepository.formatBytes(stats.todayMobileBytes)}"
            val daysLeftFormatted = "${stats.daysLeftInCycle}d until reset"

            views.setTextViewText(R.id.widget_data_remaining, remainingFormatted)
            views.setTextViewText(R.id.widget_used_today, todayFormatted)
            views.setTextViewText(R.id.widget_days_left, daysLeftFormatted)
            views.setProgressBar(R.id.widget_progress, 100, stats.percentUsed.toInt(), false)

            // Open app on click
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, DataGuardWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, DataGuardWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
