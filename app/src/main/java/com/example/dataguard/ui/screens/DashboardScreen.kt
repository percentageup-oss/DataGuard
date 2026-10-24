package com.example.dataguard.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dataguard.data.repository.DataGuardRepository
import com.example.dataguard.ui.components.BatteryOptimizationBanner
import com.example.dataguard.ui.components.ForecastCard
import com.example.dataguard.ui.components.HotspotCard
import com.example.dataguard.ui.components.MetricTile
import com.example.dataguard.ui.components.ProgressRing
import com.example.dataguard.ui.components.UsagePermissionBanner
import com.example.dataguard.ui.theme.MetricHotspotPurple
import com.example.dataguard.ui.theme.MetricMobileCyan
import com.example.dataguard.ui.theme.MetricWifiBlue
import com.example.dataguard.ui.theme.PrimaryLight
import com.example.dataguard.ui.theme.StatusSafeGreen
import com.example.dataguard.ui.viewmodel.DataGuardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DataGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.billingStats.collectAsState()
    val plan by viewModel.planFlow.collectAsState()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refreshAll() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("dashboard_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DataGuard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Device-local data guardian",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.testTag("refresh_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permission Warning Banner
            if (!isPermissionGranted) {
                UsagePermissionBanner(
                    onGrantClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Big Progress Ring
            ProgressRing(
                percent = stats.percentUsed,
                usedFormatted = DataGuardRepository.formatBytes(stats.totalMobileBytes),
                totalFormatted = DataGuardRepository.formatBytes(plan.monthlyAllowanceBytes),
                remainingFormatted = DataGuardRepository.formatBytes(stats.remainingBytes)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Forecast Prediction Card
            ForecastCard(
                forecastDate = stats.forecastRunOutDate,
                avgPaceFormatted = DataGuardRepository.formatBytes(stats.avgPaceBytesPerDay),
                daysLeft = stats.daysLeftInCycle
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 Metric Tiles Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    icon = Icons.Default.PhoneAndroid,
                    iconTint = MetricMobileCyan,
                    title = "Today Mobile",
                    value = DataGuardRepository.formatBytes(stats.todayMobileBytes),
                    subtitle = "Used since midnight",
                    modifier = Modifier.weight(1f),
                    testTag = "tile_today_mobile"
                )

                MetricTile(
                    icon = Icons.Default.Wifi,
                    iconTint = MetricWifiBlue,
                    title = "Today Wi-Fi",
                    value = DataGuardRepository.formatBytes(stats.todayWifiBytes),
                    subtitle = "Unmetered network",
                    modifier = Modifier.weight(1f),
                    testTag = "tile_today_wifi"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val budgetVal = if (stats.dailyBudgetBytes > 0) DataGuardRepository.formatBytes(stats.dailyBudgetBytes) else "None"
                MetricTile(
                    icon = Icons.Default.CalendarToday,
                    iconTint = StatusSafeGreen,
                    title = "Daily Budget",
                    value = budgetVal,
                    subtitle = "${stats.daysLeftInCycle} days left in cycle",
                    modifier = Modifier.weight(1f),
                    testTag = "tile_daily_budget"
                )

                MetricTile(
                    icon = Icons.Default.SimCard,
                    iconTint = PrimaryLight,
                    title = "Dual SIM Status",
                    value = "Active",
                    subtitle = "Tracking all active slots",
                    modifier = Modifier.weight(1f),
                    testTag = "tile_dual_sim"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hotspot Card
            HotspotCard(
                hotspotBytesFormatted = DataGuardRepository.formatBytes(stats.hotspotBytes),
                hotspotCapFormatted = if (plan.hotspotCapBytes > 0) DataGuardRepository.formatBytes(plan.hotspotCapBytes) else "",
                isSupported = stats.hotspotSupported
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Speed Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_speed_toggle_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Live Speed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Speed Notification",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Ongoing status bar download/upload meter",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Switch(
                        checked = plan.liveSpeedEnabled,
                        onCheckedChange = { checked ->
                            viewModel.toggleLiveSpeed(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("live_speed_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // OEM Battery Warning if needed
            BatteryOptimizationBanner()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
