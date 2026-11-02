package com.arjunvision.arjunfitai.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.samsung.HealthMetric
import com.arjunvision.arjunfitai.ui.components.AfBars
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfEmptyChart
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfMedicalFooter
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.components.AfSegmentedTabs
import com.arjunvision.arjunfitai.ui.components.rememberDrawIn
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.home.formatActiveTime
import com.arjunvision.arjunfitai.ui.home.formatKcal
import com.arjunvision.arjunfitai.ui.home.formatKm
import com.arjunvision.arjunfitai.ui.home.formatSteps
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.LocalDate
import java.time.ZoneId

private val BarIdle = Color(0xFF3A4350)

@Composable
fun StepsScreen(
    state: DashboardUiState,
    stepGoal: Int,
    loadDaily: suspend (LocalDate, LocalDate) -> Result<Map<LocalDate, Long>>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val period = Period.entries[tab]
    val allowed = state.permissions?.granted?.contains(HealthMetric.STEPS) == true
    val steps = state.data.steps
    val activity = state.data.activity

    val daily = rememberLoaded(state.lastUpdated) { loadDaily(stepsHistoryStart(today), today) }

    DetailColumn(contentPadding) {
        AfScreenHeader("Steps", onBack)
        AfSegmentedTabs(Period.entries.map { it.label }, tab, { tab = it })

        Column(Modifier.riseIn(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                buildAnnotatedString {
                    append(steps?.let(::formatSteps) ?: EMPTY)
                    withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))) {
                        append(" / ${formatSteps(stepGoal.toLong())}")
                    }
                },
                style = MaterialTheme.typography.displayLarge,
                color = AfColors.Text,
            )
            val pct = if (steps != null && stepGoal > 0) steps * 100 / stepGoal else null
            Text(pct?.let { "$it% of today's goal" } ?: "No steps read yet today", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            GoalBar(((steps ?: 0L).toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f), pct)
        }

        AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(16.dp)) {
            when {
                !allowed -> AfEmptyChart("Allow steps in Health sync to see your history")
                daily is Loaded.Loading -> AfEmptyChart("Reading Samsung Health…")
                daily is Loaded.Failed -> AfEmptyChart(daily.message)
                daily is Loaded.Ready -> {
                    // Today's live total wins over the grouped read.
                    val values = if (steps != null) daily.value + (today to steps) else daily.value
                    val chart = stepsChart(values, period, today, stepGoal)
                    Text(chart.title, style = MaterialTheme.typography.labelLarge, color = AfColors.Text)
                    val scaleMax = maxOf(chart.bars.maxOf { it.steps }.toFloat(), goalFor(period, stepGoal)) * 1.1f
                    AfBars(
                        items = chart.bars,
                        value = { it.steps.toFloat() },
                        label = { it.label },
                        colorFor = { if (it.isCurrent) AfColors.Accent else if (it.metGoal) AfColors.AccentSoft else BarIdle },
                        emphasized = { it.isCurrent },
                        scaleMax = scaleMax,
                        description = chart.title + ": " + chart.bars.joinToString { "${it.label} ${formatSteps(it.steps)}" },
                        modifier = Modifier.padding(top = 10.dp),
                        chartHeight = 170.dp,
                    )
                    Text(chart.note, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        AfCard(Modifier.fillMaxWidth().riseIn(2), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            DetailRow(AfIcons.Pin, "Distance", activity?.distanceMeters?.let(::formatKm))
            HorizontalDivider(color = AfColors.Inner)
            DetailRow(AfIcons.Flame, "Active calories", activity?.activeCaloriesKcal?.let { "${formatKcal(it)} kcal" })
            HorizontalDivider(color = AfColors.Inner)
            DetailRow(AfIcons.Timer, "Active time", activity?.activeTime?.let(::formatActiveTime))
        }

        AfMedicalFooter()
    }
}

/** Week/month bars are compared with the goal for the whole period, so scale against that. */
private fun goalFor(period: Period, goal: Int): Float = when (period) {
    Period.DAY -> goal.toFloat()
    Period.WEEK -> goal * 7f
    Period.MONTH -> goal * 30f
}

@Composable
private fun GoalBar(fraction: Float, pct: Long?) {
    val grow by rememberDrawIn(1000, 200)
    Box(
        Modifier.fillMaxWidth().height(12.dp).clip(AfShapes.Pill).background(AfColors.Inner)
            .semantics { contentDescription = pct?.let { "$it% of today's step goal" } ?: "No steps yet" },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction * grow).widthIn(min = 8.dp).clip(AfShapes.Pill).background(AfColors.Accent))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = AfColors.AccentSoft, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp), color = AfColors.Text, modifier = Modifier.weight(1f))
        Text(value ?: EMPTY, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.Text)
    }
}
