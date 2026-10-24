package com.example.dataguard.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dataguard.data.model.AppUsageItem
import com.example.dataguard.data.repository.DataGuardRepository
import com.example.dataguard.ui.theme.MetricMobileCyan
import com.example.dataguard.ui.theme.MetricWifiBlue
import com.example.dataguard.ui.theme.StatusWarningOrange
import com.example.dataguard.ui.viewmodel.AppFilter
import com.example.dataguard.ui.viewmodel.AppSort
import com.example.dataguard.ui.viewmodel.DataGuardViewModel

@Composable
fun TopAppsScreen(
    viewModel: DataGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apps by viewModel.appList.collectAsState()
    val filter by viewModel.appFilter.collectAsState()
    val sort by viewModel.appSort.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredApps = remember(apps, filter, sort, searchQuery) {
        var list = apps

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Filter network
        list = when (filter) {
            AppFilter.MOBILE -> list.filter { it.totalMobileBytes > 0 }
            AppFilter.WIFI -> list.filter { it.wifiBytes > 0 }
            AppFilter.BOTH -> list.filter { it.totalBytes > 0 }
        }

        // Sort
        when (sort) {
            AppSort.TOTAL -> when (filter) {
                AppFilter.MOBILE -> list.sortedByDescending { it.totalMobileBytes }
                AppFilter.WIFI -> list.sortedByDescending { it.wifiBytes }
                AppFilter.BOTH -> list.sortedByDescending { it.totalBytes }
            }
            AppSort.FOREGROUND -> list.sortedByDescending { it.foregroundBytes }
            AppSort.BACKGROUND -> list.sortedByDescending { it.backgroundBytes }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("top_apps_screen")
    ) {
        Text(
            text = "App Usage Breakdown",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = "Cycle usage with foreground vs background split",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search apps...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_apps_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips (Mobile / Wi-Fi / Both)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == AppFilter.MOBILE,
                onClick = { viewModel.setAppFilter(AppFilter.MOBILE) },
                label = { Text("Mobile") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MetricMobileCyan.copy(alpha = 0.2f),
                    selectedLabelColor = MetricMobileCyan
                ),
                modifier = Modifier.testTag("filter_mobile_chip")
            )
            FilterChip(
                selected = filter == AppFilter.WIFI,
                onClick = { viewModel.setAppFilter(AppFilter.WIFI) },
                label = { Text("Wi-Fi") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MetricWifiBlue.copy(alpha = 0.2f),
                    selectedLabelColor = MetricWifiBlue
                ),
                modifier = Modifier.testTag("filter_wifi_chip")
            )
            FilterChip(
                selected = filter == AppFilter.BOTH,
                onClick = { viewModel.setAppFilter(AppFilter.BOTH) },
                label = { Text("Combined") },
                modifier = Modifier.testTag("filter_both_chip")
            )
        }

        // Sort Chips (Total / Foreground / Background)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sort == AppSort.TOTAL,
                onClick = { viewModel.setAppSort(AppSort.TOTAL) },
                label = { Text("Sort: Total") }
            )
            FilterChip(
                selected = sort == AppSort.FOREGROUND,
                onClick = { viewModel.setAppSort(AppSort.FOREGROUND) },
                label = { Text("Sort: Foreground") }
            )
            FilterChip(
                selected = sort == AppSort.BACKGROUND,
                onClick = { viewModel.setAppSort(AppSort.BACKGROUND) },
                label = { Text("Sort: Background") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps found for current filter.\nEnsure Usage Access permission is granted.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName + "_" + it.uid }) { item ->
                    AppUsageCard(
                        item = item,
                        currentFilter = filter,
                        onOpenSettings = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${item.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun AppUsageCard(
    item: AppUsageItem,
    currentFilter: AppFilter,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_usage_item_${item.packageName}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon or Avatar
                AppIconImage(
                    drawable = item.icon,
                    fallbackChar = item.appName.firstOrNull()?.uppercaseChar() ?: 'A'
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    val primaryBytes = when (currentFilter) {
                        AppFilter.MOBILE -> item.totalMobileBytes
                        AppFilter.WIFI -> item.wifiBytes
                        AppFilter.BOTH -> item.totalBytes
                    }
                    Text(
                        text = DataGuardRepository.formatBytes(primaryBytes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("open_settings_${item.packageName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open App Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Foreground vs Background breakdown bar for Mobile
            val totalMobile = item.totalMobileBytes
            if (totalMobile > 0) {
                val fgRatio = (item.foregroundBytes.toFloat() / totalMobile.toFloat()).coerceIn(0f, 1f)
                val bgRatio = 1f - fgRatio

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    if (fgRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(fgRatio.coerceAtLeast(0.01f))
                                .height(6.dp)
                                .background(MetricMobileCyan)
                        )
                    }
                    if (bgRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(bgRatio.coerceAtLeast(0.01f))
                                .height(6.dp)
                                .background(StatusWarningOrange)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Foreground: ${DataGuardRepository.formatBytes(item.foregroundBytes)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MetricMobileCyan
                        )
                    )
                    Text(
                        text = "Background: ${DataGuardRepository.formatBytes(item.backgroundBytes)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StatusWarningOrange
                        )
                    )
                }
            } else if (item.wifiBytes > 0) {
                Text(
                    text = "Wi-Fi: ${DataGuardRepository.formatBytes(item.wifiBytes)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MetricWifiBlue
                    )
                )
            }
        }
    }
}

@Composable
fun AppIconImage(
    drawable: Drawable?,
    fallbackChar: Char,
    modifier: Modifier = Modifier
) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val w = drawable.intrinsicWidth.coerceAtLeast(48)
                val h = drawable.intrinsicHeight.coerceAtLeast(48)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackChar.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
