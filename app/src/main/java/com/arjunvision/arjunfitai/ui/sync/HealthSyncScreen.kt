package com.arjunvision.arjunfitai.ui.sync

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfIconWell
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfOutlineButton
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.dashboard.DashboardViewModel
import com.arjunvision.arjunfitai.ui.home.formatReadingTime
import com.arjunvision.arjunfitai.ui.home.latestReading
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HealthSyncScreen(
    viewModel: DashboardViewModel,
    state: DashboardUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val activity = LocalActivity.current
    val zone = remember { ZoneId.systemDefault() }
    val now = Instant.now()
    val data = state.data
    val granted = state.permissions?.granted.orEmpty().mapNotNull { m -> SyncType.entries.firstOrNull { it.name == m.name } }.toSet()
    val failed = data.errors.keys.mapNotNull { m -> SyncType.entries.firstOrNull { it.name == m.name } }.toSet()
    val rows = syncRows(
        SyncInputs(granted, failed, data.steps, data.activity, data.heartRate, data.exercise, data.sleep, data.bloodOxygen, data.weight),
        now, zone,
    )
    val latest = latestReading(data)

    BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AfScreenHeader("Health sync", onBack)

            AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                ConnectionRow(
                    icon = AfIcons.HeartPulse,
                    title = "Samsung Health",
                    status = when (state.status) {
                        DashboardUiState.Status.READY -> "Connected · read only"
                        DashboardUiState.Status.CONNECTING -> "Connecting…"
                        DashboardUiState.Status.NEEDS_PERMISSION -> "Not allowed yet · tap Review permissions"
                        DashboardUiState.Status.ERROR -> state.error?.message ?: "Not available"
                    },
                    statusColor = when (state.status) {
                        DashboardUiState.Status.READY -> AfColors.Success
                        DashboardUiState.Status.CONNECTING -> AfColors.TextMuted
                        else -> AfColors.AccentSoft
                    },
                    action = if (state.status == DashboardUiState.Status.ERROR && state.error?.canResolve == true) {
                        "Fix" to { activity?.let(viewModel::resolveError); Unit }
                    } else null,
                )
                HorizontalDivider(color = AfColors.Inner)
                ConnectionRow(
                    icon = AfIcons.Band,
                    title = "Galaxy Fit3",
                    status = latest?.let { "Latest reading " + readingText(it, zone) } ?: "No readings yet",
                    statusColor = AfColors.TextMuted,
                )
            }

            AfCard(Modifier.fillMaxWidth().riseIn(2), padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = AfColors.Inner)
                    TypeRow(row)
                }
            }

            Text(
                syncSummary(rows, state.lastUpdated, now, zone),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = AfColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val busy = state.isRefreshing || state.status == DashboardUiState.Status.CONNECTING
                AfPrimaryButton(
                    text = if (busy) "Reading Samsung Health…" else "Refresh now",
                    onClick = { if (!busy) viewModel.refresh() },
                    icon = AfIcons.Refresh,
                    iconModifier = Modifier.rotate(spinDegrees(busy)),
                    height = 52.dp,
                )
                AfOutlineButton("Review permissions", onClick = { activity?.let { viewModel.requestPermissions(it) } })
                Text(
                    "Your health data stays on this phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = AfColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** "today, 17:15" or "Sun 1 Nov, 17:15". */
private fun readingText(instant: Instant, zone: ZoneId): String {
    val today = LocalDate.now(zone)
    val text = formatReadingTime(instant, today, zone)
    return if (instant.atZone(zone).toLocalDate() == today) "today, $text" else text
}

@Composable
private fun ConnectionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    statusColor: Color,
    action: Pair<String, () -> Unit>? = null,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AfIconWell(icon, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AfColors.Text)
            Text(status, style = MaterialTheme.typography.labelMedium, color = statusColor)
        }
        action?.let { (label, onClick) ->
            Box(
                Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Accent)
                    .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelLarge, color = AfColors.OnAccent) }
        }
    }
}

@Composable
private fun TypeRow(row: SyncRow) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${row.type.label}: ${row.note}" + if (row.hasRecords) ", has records" else ""
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(24.dp).clip(AfShapes.Pill).background(if (row.hasRecords) AfColors.Success else AfColors.Inner)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (row.hasRecords) "✓" else "–",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (row.hasRecords) AfColors.Background else Color.White.copy(alpha = 0.7f),
            )
        }
        Text(row.type.label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = AfColors.Text, modifier = Modifier.weight(1f))
        Text(row.note, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
    }
}

/** Refresh icon spin while reading (0.9s per turn); still when motion is reduced. */
@Composable
private fun spinDegrees(spinning: Boolean): Float {
    if (!spinning || LocalReduceMotion.current) return 0f
    return rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spinDeg",
    ).value
}
