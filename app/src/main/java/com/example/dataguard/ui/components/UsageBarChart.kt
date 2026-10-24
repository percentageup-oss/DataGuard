package com.example.dataguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dataguard.data.model.DailySnapshot
import com.example.dataguard.data.repository.DataGuardRepository
import com.example.dataguard.ui.theme.MetricMobileCyan
import com.example.dataguard.ui.theme.MetricWifiBlue

@Composable
fun UsageBarChart(
    snapshots: List<DailySnapshot>,
    modifier: Modifier = Modifier,
    title: String = "Daily Data Consumption"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("usage_bar_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MetricMobileCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mobile",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MetricWifiBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Wi-Fi",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (snapshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded history yet.\nDaily snapshots will appear here as you use data.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Show last 7 to 14 entries
                val displayList = snapshots.takeLast(14)
                val maxBytes = (displayList.maxOfOrNull { it.totalMobileBytes.coerceAtLeast(it.totalWifiBytes) } ?: 1L).coerceAtLeast(1024L * 1024L)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val width = size.width
                    val height = size.height - 30f // room for label
                    val barGroupCount = displayList.size
                    val groupWidth = width / barGroupCount
                    val barWidth = (groupWidth * 0.35f).coerceAtMost(16.dp.toPx())

                    for (i in displayList.indices) {
                        val item = displayList[i]
                        val xGroup = i * groupWidth + (groupWidth - (barWidth * 2 + 4f)) / 2f

                        // Mobile bar
                        val mobileRatio = (item.totalMobileBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
                        val mobileBarHeight = (height * mobileRatio).coerceAtLeast(4f)
                        val mobileY = height - mobileBarHeight

                        drawRoundRect(
                            color = MetricMobileCyan,
                            topLeft = Offset(xGroup, mobileY),
                            size = Size(barWidth, mobileBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Wi-Fi bar
                        val wifiRatio = (item.totalWifiBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
                        val wifiBarHeight = (height * wifiRatio).coerceAtLeast(4f)
                        val wifiY = height - wifiBarHeight

                        drawRoundRect(
                            color = MetricWifiBlue,
                            topLeft = Offset(xGroup + barWidth + 4f, wifiY),
                            size = Size(barWidth, wifiBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val firstDate = displayList.firstOrNull()?.date?.substring(5) ?: ""
                    val lastDate = displayList.lastOrNull()?.date?.substring(5) ?: ""
                    Text(
                        text = firstDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "Max: ${DataGuardRepository.formatBytes(maxBytes)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = lastDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
