package com.example.dataguard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dataguard.ui.theme.StatusCriticalRed
import com.example.dataguard.ui.theme.StatusSafeGreen
import com.example.dataguard.ui.theme.StatusWarningOrange
import java.util.Locale

@Composable
fun ProgressRing(
    percent: Double,
    usedFormatted: String,
    totalFormatted: String,
    remainingFormatted: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    strokeWidth: Dp = 18.dp
) {
    val targetSweep = ((percent.coerceIn(0.0, 100.0) / 100.0) * 260.0).toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = tween(durationMillis = 900),
        label = "progress_sweep"
    )

    val statusColor = when {
        percent >= 90.0 -> StatusCriticalRed
        percent >= 75.0 -> StatusWarningOrange
        else -> StatusSafeGreen
    }

    val statusGradient = when {
        percent >= 90.0 -> listOf(StatusWarningOrange, StatusCriticalRed)
        percent >= 75.0 -> listOf(StatusSafeGreen, StatusWarningOrange)
        else -> listOf(Color(0xFF06B6D4), StatusSafeGreen)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("plan_progress_ring")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val startAngle = 140f
            val totalAngle = 260f

            // Background Track
            drawArc(
                color = Color.Gray.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = totalAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Foreground Progress
            if (animatedSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(statusGradient),
                    startAngle = startAngle,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", percent),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = statusColor
                )
            )
            Text(
                text = "$usedFormatted / $totalFormatted",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$remainingFormatted left",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
