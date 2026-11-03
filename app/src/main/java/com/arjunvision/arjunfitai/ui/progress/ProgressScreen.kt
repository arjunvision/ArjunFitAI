package com.arjunvision.arjunfitai.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.health.HealthMetric
import com.arjunvision.arjunfitai.data.settings.UserSettings
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import com.arjunvision.arjunfitai.ui.components.AfBars
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfEmptyChart
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfSegmentedTabs
import com.arjunvision.arjunfitai.ui.components.AfTimeLine
import com.arjunvision.arjunfitai.ui.components.rememberDrawIn
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.detail.Loaded
import com.arjunvision.arjunfitai.ui.detail.Period
import com.arjunvision.arjunfitai.ui.detail.rememberLoaded
import com.arjunvision.arjunfitai.ui.detail.stepsChart
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.home.WinterArc
import com.arjunvision.arjunfitai.ui.home.formatSteps
import com.arjunvision.arjunfitai.ui.home.weightProgress
import com.arjunvision.arjunfitai.ui.profile.formatTarget
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfEyebrow
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import com.arjunvision.arjunfitai.ui.weight.WeighIn
import com.arjunvision.arjunfitai.ui.weight.asRecords
import com.arjunvision.arjunfitai.ui.weight.weightChart
import com.arjunvision.arjunfitai.ui.workout.personalRecords
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val BarIdle = Color(0xFF3A4350)

class ProgressActions(val onLogWeight: () -> Unit, val onWorkouts: () -> Unit, val onPrs: () -> Unit)

@Composable
fun ProgressScreen(
    state: DashboardUiState,
    settings: UserSettings,
    weighIns: List<WeighIn>,
    plans: List<PlanEntity>,
    sessions: List<SessionEntity>,
    sets: List<SetEntity>,
    loadDailySteps: suspend (LocalDate, LocalDate) -> Result<Map<LocalDate, Long>>,
    actions: ProgressActions,
    contentPadding: PaddingValues,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val timeline = arcTimeline(today)

    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("WINTER ARC ${today.year}", style = AfEyebrow.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = AfColors.TextMuted)
            Text("PROGRESS", style = MaterialTheme.typography.headlineLarge, color = AfColors.Text)
        }
        AfSegmentedTabs(listOf("Weight", "Activity", "Workout"), tab, { tab = it })

        AfCard(Modifier.fillMaxWidth().riseIn(0), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(timeline.title, style = MaterialTheme.typography.labelLarge, color = AfColors.Text, modifier = Modifier.weight(1f))
                Text("Oct 12 – Dec 31", style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted)
            }
            GrowBar(timeline.fraction, AfColors.Accent, Modifier.padding(vertical = 8.dp), "${timeline.title}, ${timeline.note}")
            Text(timeline.note, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
        }

        when (tab) {
            0 -> WeightTab(weighIns, settings, today, zone, actions)
            1 -> ActivityTab(state, settings, today, loadDailySteps)
            else -> WorkoutTab(plans, sessions, sets, today, zone, actions)
        }
    }
}

@Composable
private fun WeightTab(weighIns: List<WeighIn>, settings: UserSettings, today: LocalDate, zone: ZoneId, actions: ProgressActions) {
    val arcStart = WinterArc.start(today.year)
    val progress = weightProgress(weighIns.asRecords(), settings.targetWeightKg, arcStart, zone)
    val chart = weightChart(weighIns, today, zone)
    val baseline = progress?.let { p -> p.changeKg?.let { p.currentKg - it } }
    val goal = goalProgress(baseline, progress?.baselineDate, progress?.currentKg, settings.targetWeightKg, today)

    AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Weight trend", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.Text, modifier = Modifier.weight(1f))
            Box(
                Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Accent)
                    .clickable(role = Role.Button, onClick = actions.onLogWeight).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("+ Log weight", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = AfColors.OnAccent) }
        }
        Column(Modifier.padding(top = 12.dp)) {
            if (chart == null) {
                AfEmptyChart("Log two weigh-ins and your trend line appears here", height = 120.dp)
            } else {
                val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                val change = chart.points.last().kg - chart.points.first().kg
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text("${chart.from.format(fmt)} – ${chart.to.format(fmt)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted, modifier = Modifier.weight(1f))
                    Text(
                        (if (change < 0f) "−" else "+") + "%.1f kg".format(Locale.ENGLISH, kotlin.math.abs(change)),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (change < 0f) AfColors.Success else AfColors.Text,
                    )
                }
                val start = chart.from.atStartOfDay(zone).toInstant()
                val span = Duration.between(start, chart.to.plusDays(1).atStartOfDay(zone).toInstant()).seconds.toFloat()
                AfTimeLine(
                    chart.points.map { Duration.between(start, it.time).seconds / span to it.kg },
                    chart.yMin, chart.yMax,
                    "Weight from %.1f to %.1f kg".format(Locale.ENGLISH, chart.points.first().kg, chart.points.last().kg),
                    Modifier.padding(top = 6.dp),
                    chartHeight = 110.dp,
                )
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InnerStat("Start", baseline?.let { "%.1f kg".format(Locale.ENGLISH, it) } ?: EMPTY, AfColors.Text, Modifier.weight(1f))
            InnerStat("Current", progress?.let { "%.1f kg".format(Locale.ENGLISH, it.currentKg) } ?: EMPTY, AfColors.Text, Modifier.weight(1f))
            InnerStat("Target", formatTarget(settings.targetWeightKg), AfColors.Accent, Modifier.weight(1f))
        }
    }

    AfCard(Modifier.fillMaxWidth().riseIn(2), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Goal progress", style = MaterialTheme.typography.labelLarge, color = AfColors.Text, modifier = Modifier.weight(1f))
            Text(goal.title, style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted)
        }
        GrowBar(goal.fraction, AfColors.Success, Modifier.padding(vertical = 8.dp), "Goal progress ${goal.title}")
        Text(goal.note, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
    }
}

@Composable
private fun ActivityTab(state: DashboardUiState, settings: UserSettings, today: LocalDate, loadDailySteps: suspend (LocalDate, LocalDate) -> Result<Map<LocalDate, Long>>) {
    val allowed = state.permissions?.granted?.contains(HealthMetric.STEPS) == true
    val daily = rememberLoaded(state.lastUpdated) { loadDailySteps(today.minusDays(6), today) }
    AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("Steps · from Fit3", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.Text, modifier = Modifier.weight(1f))
            Text("Goal ${formatSteps(settings.stepGoal.toLong())}/day", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
        }
        Column(Modifier.padding(top = 12.dp)) {
            when {
                !allowed -> AfEmptyChart("Allow steps in Health sync to see your week")
                daily is Loaded.Loading -> AfEmptyChart("Reading Samsung Health…")
                daily is Loaded.Failed -> AfEmptyChart(daily.message)
                daily is Loaded.Ready -> {
                    val values = state.data.steps?.let { daily.value + (today to it) } ?: daily.value
                    val chart = stepsChart(values, Period.DAY, today, settings.stepGoal)
                    AfBars(
                        items = chart.bars,
                        value = { it.steps.toFloat() },
                        label = { it.label.take(1) },
                        colorFor = { if (it.isCurrent) AfColors.Accent else if (it.metGoal) AfColors.AccentSoft else BarIdle },
                        emphasized = { it.isCurrent },
                        scaleMax = maxOf(chart.bars.maxOf { it.steps }.toFloat(), settings.stepGoal.toFloat()) * 1.1f,
                        description = "Steps over the last 7 days: " + chart.bars.joinToString { "${it.label} ${formatSteps(it.steps)}" },
                        chartHeight = 140.dp,
                    )
                    val withSteps = chart.bars.filter { it.steps > 0 }
                    val avg = if (withSteps.isEmpty()) null else withSteps.sumOf { it.steps }.toDouble() / withSteps.size
                    Text(
                        avg?.let { "Avg ${formatSteps(it.roundToLong())} steps/day over ${withSteps.size} days · ${chart.note.lowercase(Locale.ENGLISH)}" } ?: "No steps recorded this week yet",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutTab(plans: List<PlanEntity>, sessions: List<SessionEntity>, sets: List<SetEntity>, today: LocalDate, zone: ZoneId, actions: ProgressActions) {
    val days = strengthDaysThisWeek(sessions, plans, today, zone)
    val volume = volumeSince(sessions, sets, WinterArc.start(today.year), zone)
    val prs = personalRecords(sessions, sets)
    Row(Modifier.riseIn(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigStat("$days / 3", "Strength days this week", Modifier.weight(1f))
        BigStat(NumberFormat.getIntegerInstance(Locale.ENGLISH).format(volume.roundToInt()), "Total volume lifted · Winter Arc", Modifier.weight(1f), unit = "kg")
    }
    Row(
        Modifier.fillMaxWidth().riseIn(2).clip(AfShapes.Card).background(AfColors.Card)
            .clickable(role = Role.Button, onClickLabel = "Open personal records", onClick = actions.onPrs).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(AfShapes.Pill).background(AfColors.Inner), contentAlignment = Alignment.Center) {
            Icon(AfIcons.Trophy, contentDescription = null, tint = AfColors.AccentSoft, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Personal records", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
            Text(prSummary(prs), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
        }
        Text("Start", style = MaterialTheme.typography.labelLarge, color = AfColors.AccentSoft,
            modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = "Go to workouts", onClick = actions.onWorkouts).padding(vertical = 14.dp, horizontal = 4.dp))
    }
}

@Composable
private fun GrowBar(fraction: Float, color: Color, modifier: Modifier, description: String) {
    val grow by rememberDrawIn(1000, 200)
    Box(modifier.fillMaxWidth().height(10.dp).clip(AfShapes.Pill).background(AfColors.Inner).semantics { contentDescription = description }) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f) * grow).clip(AfShapes.Pill).background(color))
    }
}

@Composable
private fun InnerStat(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedFourteen).background(AfColors.Inner).padding(10.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = valueColor)
    }
}

private val RoundedFourteen = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier, unit: String? = null) {
    Column(modifier.clip(AfShapes.Card).background(AfColors.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            buildAnnotatedString {
                append(value)
                if (unit != null) withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)) { append(" $unit") }
            },
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
            color = AfColors.Text,
        )
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
    }
}
