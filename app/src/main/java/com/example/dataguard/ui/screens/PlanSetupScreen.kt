package com.example.dataguard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.dataguard.data.model.DataPlan
import com.example.dataguard.ui.theme.PrimaryBlue
import com.example.dataguard.ui.viewmodel.DataGuardViewModel
import java.util.Locale

@Composable
fun PlanSetupScreen(
    viewModel: DataGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPlan by viewModel.planFlow.collectAsState()

    var isGbUnit by remember { mutableStateOf(currentPlan.monthlyAllowanceMb % 1024L == 0L) }
    var allowanceText by remember(currentPlan) {
        val displayVal = if (isGbUnit) currentPlan.monthlyAllowanceMb / 1024L else currentPlan.monthlyAllowanceMb
        mutableStateOf(displayVal.toString())
    }

    var resetDay by remember(currentPlan) { mutableIntStateOf(currentPlan.billingResetDay) }

    var dailyBudgetText by remember(currentPlan) {
        mutableStateOf(if (currentPlan.dailyBudgetMb > 0) currentPlan.dailyBudgetMb.toString() else "")
    }

    var hasHotspotCap by remember(currentPlan) { mutableStateOf(currentPlan.hotspotCapMb > 0) }
    var hotspotCapText by remember(currentPlan) {
        mutableStateOf(if (currentPlan.hotspotCapMb > 0) currentPlan.hotspotCapMb.toString() else "2048")
    }

    var spikeAlertText by remember(currentPlan) {
        mutableStateOf(currentPlan.spikeAlertThresholdMb.toString())
    }

    // Notification Permission for Android 13+
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            Toast.makeText(context, "Notifications enabled for data alerts", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("plan_setup_screen")
    ) {
        Text(
            text = "Data Plan & Alerts",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = "Configure allowances, reset day, budgets, and alert thresholds",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Permission Card if missing
        if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notification Permission Needed",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To receive threshold alerts (50%, 75%, 90%, 100%) and 15-minute usage spike notifications, please enable notifications.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.testTag("grant_notification_permission_button")
                    ) {
                        Text("Enable Notifications")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Monthly Allowance
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Monthly Data Allowance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = allowanceText,
                        onValueChange = { allowanceText = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Allowance") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_allowance_input")
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = isGbUnit,
                            onClick = {
                                if (!isGbUnit) {
                                    val mbVal = allowanceText.toLongOrNull() ?: 10240L
                                    allowanceText = (mbVal / 1024L).coerceAtLeast(1L).toString()
                                    isGbUnit = true
                                }
                            },
                            label = { Text("GB") }
                        )
                        FilterChip(
                            selected = !isGbUnit,
                            onClick = {
                                if (isGbUnit) {
                                    val gbVal = allowanceText.toLongOrNull() ?: 10L
                                    allowanceText = (gbVal * 1024L).toString()
                                    isGbUnit = false
                                }
                            },
                            label = { Text("MB") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Billing Reset Day
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Billing Reset Day",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${resetDay}th of month",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Text(
                    text = "Usage resets exactly at 00:00:00 on this day each month.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = resetDay.toFloat(),
                    onValueChange = { resetDay = it.toInt().coerceIn(1, 31) },
                    valueRange = 1f..31f,
                    steps = 29,
                    modifier = Modifier.testTag("reset_day_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Daily Budget
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Budget (Optional)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Set a daily target or leave empty for automatic calculation based on remaining days.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dailyBudgetText,
                    onValueChange = { dailyBudgetText = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Daily Target (MB)") },
                    placeholder = { Text("Auto-calculated if blank") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_budget_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Hotspot Cap
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Separate Hotspot Cap",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Enforce a separate cap for tethered devices",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Switch(
                        checked = hasHotspotCap,
                        onCheckedChange = { hasHotspotCap = it },
                        modifier = Modifier.testTag("hotspot_cap_switch")
                    )
                }

                if (hasHotspotCap) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = hotspotCapText,
                        onValueChange = { hotspotCapText = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Hotspot Cap (MB)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hotspot_cap_input")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Spike Alert Threshold
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Spike Alert Threshold",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Trigger immediate alert if mobile usage increases by this amount within 15 minutes, naming the top culprit app.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = spikeAlertText,
                    onValueChange = { spikeAlertText = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Spike Threshold (MB in 15 min)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spike_alert_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save Button
        Button(
            onClick = {
                val inputAllowance = allowanceText.toLongOrNull() ?: 10L
                val allowanceMb = if (isGbUnit) inputAllowance * 1024L else inputAllowance
                val dailyBudgetMb = dailyBudgetText.toLongOrNull() ?: 0L
                val hotspotMb = if (hasHotspotCap) hotspotCapText.toLongOrNull() ?: 0L else 0L
                val spikeMb = spikeAlertText.toLongOrNull() ?: 100L

                val updatedPlan = currentPlan.copy(
                    monthlyAllowanceMb = allowanceMb,
                    billingResetDay = resetDay,
                    dailyBudgetMb = dailyBudgetMb,
                    hotspotCapMb = hotspotMb,
                    spikeAlertThresholdMb = spikeMb
                )

                viewModel.savePlan(updatedPlan)
                Toast.makeText(context, "Data plan settings saved!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_plan_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save Plan")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Plan Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
