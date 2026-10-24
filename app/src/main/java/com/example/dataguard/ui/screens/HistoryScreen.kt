package com.example.dataguard.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.dataguard.data.model.DailySnapshot
import com.example.dataguard.data.repository.DataGuardRepository
import com.example.dataguard.ui.components.UsageBarChart
import com.example.dataguard.ui.theme.MetricHotspotPurple
import com.example.dataguard.ui.theme.MetricMobileCyan
import com.example.dataguard.ui.theme.MetricWifiBlue
import com.example.dataguard.ui.viewmodel.DataGuardViewModel

enum class HistoryViewMode {
    DAILY,
    MONTHLY
}

@Composable
fun HistoryScreen(
    viewModel: DataGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snapshots by viewModel.snapshots.collectAsState()
    var viewMode by remember { mutableStateOf(HistoryViewMode.DAILY) }

    // Aggregate monthly if viewMode == MONTHLY
    val displaySnapshots = remember(snapshots, viewMode) {
        if (viewMode == HistoryViewMode.DAILY) {
            snapshots
        } else {
            // Group by year-month
            snapshots.groupBy { it.date.take(7) }
                .map { (yearMonth, list) ->
                    DailySnapshot(
                        date = yearMonth,
                        mobileRxBytes = list.sumOf { it.mobileRxBytes },
                        mobileTxBytes = list.sumOf { it.mobileTxBytes },
                        wifiRxBytes = list.sumOf { it.wifiRxBytes },
                        wifiTxBytes = list.sumOf { it.wifiTxBytes },
                        hotspotBytes = list.sumOf { it.hotspotBytes },
                        topAppName = list.maxByOrNull { it.topAppBytes }?.topAppName ?: "",
                        topAppBytes = list.sumOf { it.topAppBytes }
                    )
                }.sortedBy { it.date }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("history_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Usage History",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Long-term snapshots stored locally in Room",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Button(
                onClick = {
                    viewModel.exportCsvReport { file ->
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "DataGuard Usage Report")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Report via"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export CSV",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle Daily vs Monthly
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = viewMode == HistoryViewMode.DAILY,
                onClick = { viewMode = HistoryViewMode.DAILY },
                label = { Text("Daily Snapshots") },
                modifier = Modifier.testTag("filter_daily_chip")
            )
            FilterChip(
                selected = viewMode == HistoryViewMode.MONTHLY,
                onClick = { viewMode = HistoryViewMode.MONTHLY },
                label = { Text("Monthly Aggregates") },
                modifier = Modifier.testTag("filter_monthly_chip")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Card
        UsageBarChart(
            snapshots = displaySnapshots,
            title = if (viewMode == HistoryViewMode.DAILY) "Daily Usage (Last 14 Days)" else "Monthly Usage Breakdown"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recorded Snapshots",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (snapshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No snapshots recorded yet.\nDataGuard records daily snapshots at midnight or when refreshed.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snapshots, key = { it.date }) { snapshot ->
                    SnapshotHistoryCard(snapshot)
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SnapshotHistoryCard(
    snapshot: DailySnapshot,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("snapshot_${snapshot.date}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snapshot.date,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = DataGuardRepository.formatBytes(snapshot.totalMobileBytes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MetricMobileCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wi-Fi: ${DataGuardRepository.formatBytes(snapshot.totalWifiBytes)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MetricWifiBlue
                    )
                )

                if (snapshot.hotspotBytes > 0) {
                    Text(
                        text = "Hotspot: ${DataGuardRepository.formatBytes(snapshot.hotspotBytes)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MetricHotspotPurple
                        )
                    )
                }
            }

            if (snapshot.topAppName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Top app: ${snapshot.topAppName} (${DataGuardRepository.formatBytes(snapshot.topAppBytes)})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
