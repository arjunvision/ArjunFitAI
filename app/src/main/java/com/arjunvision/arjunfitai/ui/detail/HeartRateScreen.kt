package com.arjunvision.arjunfitai.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.samsung.HealthMetric
import com.arjunvision.arjunfitai.data.samsung.model.DailyRange
import com.arjunvision.arjunfitai.data.samsung.model.HeartRatePoint
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfCircleButton
import com.arjunvision.arjunfitai.ui.components.AfEmptyChart
import com.arjunvision.arjunfitai.ui.components.AfHourlyLine
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfMedicalFooter
import com.arjunvision.arjunfitai.ui.components.AfRangeBar
import com.arjunvision.arjunfitai.ui.components.AfRangeColumns
import com.arjunvision.arjunfitai.ui.components.AfSegmentedTabs
import com.arjunvision.arjunfitai.ui.components.AfStatTile
import com.arjunvision.arjunfitai.ui.components.rememberHeartBeatScale
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.theme.AfColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val HR_MIN = 40f
private const val HR_MAX = 140f

@Composable
fun HeartRateScreen(
    state: DashboardUiState,
    loadSeries: suspend (LocalDateTime, LocalDateTime) -> Result<List<HeartRatePoint>>,
    loadRanges: suspend (LocalDate, LocalDate) -> Result<List<DailyRange>>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val period = Period.entries[tab]
    val allowed = state.permissions?.granted?.contains(HealthMetric.HEART_RATE) == true
    val refreshKey = state.lastUpdated

    val daySeries = rememberLoaded(refreshKey) { loadSeries(today.atStartOfDay(), today.plusDays(1).atStartOfDay()) }
    val ranges = if (period == Period.DAY) null else rememberLoaded(period, refreshKey) {
        loadRanges(today.minusDays(if (period == Period.WEEK) 6 else 29), today)
    }

    val points = (daySeries as? Loaded.Ready)?.value.orEmpty()
    val todayRange = bpmRange(points)
    val latest = state.data.heartRate.firstOrNull()
    val beat by rememberHeartBeatScale()

    DetailColumn(contentPadding) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AfCircleButton(AfIcons.Back, "Back", AfColors.Text, onBack)
            Text(
                "HEART RATE",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AfColors.Text,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            Icon(AfIcons.HeartFilled, contentDescription = null, tint = AfColors.Accent, modifier = Modifier.size(30.dp).scale(if (latest != null) beat else 1f))
        }

        AfSegmentedTabs(Period.entries.map { it.label }, tab, { tab = it })

        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                buildAnnotatedString {
                    append(latest?.bpm?.roundToInt()?.toString() ?: EMPTY)
                    if (latest != null) withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))) { append(" bpm") }
                },
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 58.sp),
                color = AfColors.Text,
            )
            Text(
                latest?.let { "Latest reading " + readingWhen(it.time.atZone(zone).toLocalDateTime(), today) } ?: "No heart-rate readings in the last 7 days",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(16.dp)) {
            Text("Today's range", style = MaterialTheme.typography.labelLarge, color = AfColors.Text)
            if (todayRange != null) {
                val max = maxOf(HR_MAX, todayRange.high + 10f)
                AfRangeBar(todayRange.low.toFloat(), todayRange.high.toFloat(), points.lastOrNull()?.bpm, HR_MIN, max, Modifier.padding(top = 10.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${HR_MIN.toInt()}", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                    Text("${todayRange.low} – ${todayRange.high} bpm today", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                    Text("${max.toInt()}", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                }
            } else {
                Text(
                    if (daySeries is Loaded.Loading) "Reading today's heart rate…" else "No heart-rate readings yet today",
                    style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Column(Modifier.riseIn(2), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AfStatTile("Lowest today", todayRange?.low?.toString() ?: EMPTY, Modifier.weight(1f), unit = todayRange?.let { "bpm" })
                AfStatTile("Highest today", todayRange?.high?.toString() ?: EMPTY, Modifier.weight(1f), unit = todayRange?.let { "bpm" })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Samsung Health Data SDK 1.1.0 has no resting heart rate field.
                AfStatTile("Resting HR", EMPTY, Modifier.weight(1f))
                val workout = workoutHeartRate(state.data.exercise, today, zone)
                AfStatTile("During workouts", workout ?: EMPTY, Modifier.weight(1f), unit = workout?.let { "avg" })
            }
        }

        AfCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
            Text(
                when (period) {
                    Period.DAY -> "Today, hour by hour"
                    Period.WEEK -> "Last 7 days, daily range"
                    Period.MONTH -> "Last 30 days, daily range"
                },
                style = MaterialTheme.typography.labelLarge, color = AfColors.Text,
            )
            Column(Modifier.padding(top = 10.dp)) {
                when {
                    !allowed -> AfEmptyChart("Allow heart rate in Health sync to see this chart")
                    period == Period.DAY -> DayChart(daySeries, today, zone)
                    else -> RangeChart(ranges!!, period, today)
                }
            }
        }

        AfMedicalFooter()
    }
}

@Composable
private fun DayChart(series: Loaded<List<HeartRatePoint>>, today: LocalDate, zone: ZoneId) {
    when (series) {
        Loaded.Loading -> AfEmptyChart("Reading Samsung Health…")
        is Loaded.Failed -> AfEmptyChart(series.message)
        is Loaded.Ready -> {
            val hourly = hourlyAverages(series.value, today, zone)
            if (hourly.count { it != null } < 2) {
                AfEmptyChart("The chart fills in as your Fit3 records more heart-rate data")
            } else {
                val top = maxOf(HR_MAX, (hourly.filterNotNull().maxOrNull() ?: 0f) + 10f)
                AfHourlyLine(hourly, HR_MIN, top, "Heart rate through the day, ${hourly.count { it != null }} hours with data")
                AxisLabels(listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM"))
            }
        }
    }
}

@Composable
private fun RangeChart(ranges: Loaded<List<DailyRange>>, period: Period, today: LocalDate) {
    when (ranges) {
        Loaded.Loading -> AfEmptyChart("Reading Samsung Health…")
        is Loaded.Failed -> AfEmptyChart(ranges.message)
        is Loaded.Ready -> {
            val days = if (period == Period.WEEK) 7 else 30
            val dates = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
            val byDate = ranges.value.associateBy { it.date }
            if (byDate.isEmpty()) {
                AfEmptyChart("The chart fills in as your Fit3 records more heart-rate data")
            } else {
                val top = maxOf(HR_MAX, byDate.values.maxOf { it.max } + 10f)
                val bottom = minOf(HR_MIN, byDate.values.minOf { it.min } - 5f)
                AfRangeColumns(
                    dates.map { d -> byDate[d]?.let { it.min to it.max } },
                    bottom, top,
                    "Daily heart-rate range over the last $days days, ${byDate.size} days with data",
                )
                val labels = if (period == Period.WEEK) {
                    dates.map { it.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) }
                } else {
                    listOf(dates.first(), dates[10], dates[20], dates.last()).map { it.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) }
                }
                AxisLabels(labels)
            }
        }
    }
}

@Composable
private fun AxisLabels(labels: List<String>) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted, textAlign = TextAlign.Center) }
    }
}

/** "today, 21:30" or "Sun 1 Nov, 21:30". */
private fun readingWhen(time: LocalDateTime, today: LocalDate): String {
    val clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).format(time)
    return if (time.toLocalDate() == today) "today, $clock" else DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).format(time) + ", " + clock
}
