package com.arjunvision.arjunfitai.ui.detail

import com.arjunvision.arjunfitai.data.health.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.health.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.health.model.HeartRatePoint
import com.arjunvision.arjunfitai.data.health.model.SleepRecord
import com.arjunvision.arjunfitai.data.health.model.SleepStage
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.home.formatSleep
import com.arjunvision.arjunfitai.ui.home.formatSteps
import com.arjunvision.arjunfitai.ui.home.sleepDuration
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// Pure logic for the Heart rate, Steps and Sleep & oxygen screens (unit-tested).

enum class Period(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month") }

// ---------- Heart rate ----------

/** Average bpm per hour of [day] (24 slots; null where there were no measurements). */
fun hourlyAverages(points: List<HeartRatePoint>, day: LocalDate, zone: ZoneId): List<Float?> {
    val byHour = points
        .map { it.time.atZone(zone) to it.bpm }
        .filter { (t, _) -> t.toLocalDate() == day }
        .groupBy({ (t, _) -> t.hour }, { (_, bpm) -> bpm })
    return (0 until 24).map { h -> byHour[h]?.average()?.toFloat() }
}

data class BpmRange(val low: Int, val high: Int)

fun bpmRange(points: List<HeartRatePoint>): BpmRange? =
    if (points.isEmpty()) null else BpmRange(points.minOf { it.bpm }.roundToInt(), points.maxOf { it.bpm }.roundToInt())

/** "96" for one workout today, "70–76" across several; null when none recorded an average. */
fun workoutHeartRate(exercise: List<ExerciseRecord>, day: LocalDate, zone: ZoneId): String? {
    val means = exercise.filter { it.start.atZone(zone).toLocalDate() == day }.mapNotNull { it.meanHeartRate?.roundToInt() }
    return when {
        means.isEmpty() -> null
        means.min() == means.max() -> "${means.first()}"
        else -> "${means.min()}–${means.max()}"
    }
}

// ---------- Steps ----------

data class StepBar(val label: String, val steps: Long, val isCurrent: Boolean, val metGoal: Boolean)
data class StepsChart(val title: String, val bars: List<StepBar>, val note: String)

/**
 * Day: last 7 days. Week: last 6 weeks (Monday start). Month: last 6 months.
 * Week/month bars meet the goal when their daily average does.
 */
fun stepsChart(daily: Map<LocalDate, Long>, period: Period, today: LocalDate, goal: Int): StepsChart = when (period) {
    Period.DAY -> {
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val bars = days.map { d ->
            val steps = daily[d] ?: 0L
            StepBar(d.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)), steps, d == today, steps >= goal)
        }
        StepsChart("Last 7 days", bars, "Goal hit ${bars.count { it.metGoal }} of 7 days")
    }
    Period.WEEK -> {
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val starts = (5 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }
        val bars = starts.map { start ->
            val end = minOf(start.plusDays(6), today)
            val dayCount = daysBetween(start, end).size.toLong()
            val total = daysBetween(start, end).sumOf { daily[it] ?: 0L }
            StepBar(start.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)), total, start == thisWeek, total >= goal * dayCount)
        }
        StepsChart("Weekly totals", bars, averageNote(daily, starts.first(), today))
    }
    Period.MONTH -> {
        val thisMonth = today.withDayOfMonth(1)
        val starts = (5 downTo 0).map { thisMonth.minusMonths(it.toLong()) }
        val bars = starts.map { start ->
            val end = minOf(start.plusMonths(1).minusDays(1), today)
            val dayCount = daysBetween(start, end).size.toLong()
            val total = daysBetween(start, end).sumOf { daily[it] ?: 0L }
            StepBar(start.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)), total, start == thisMonth, total >= goal * dayCount)
        }
        StepsChart("Monthly totals", bars, averageNote(daily, starts.first(), today))
    }
}

/** Every date from [from] to [to], inclusive. (LocalDate.datesUntil isn't available on Android 10–11.) */
internal fun daysBetween(from: LocalDate, to: LocalDate): List<LocalDate> =
    generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()

private fun averageNote(daily: Map<LocalDate, Long>, from: LocalDate, today: LocalDate): String {
    val days = daysBetween(from, today)
    val withSteps = days.filter { (daily[it] ?: 0L) > 0L }
    if (withSteps.isEmpty()) return "No steps recorded in this period"
    val avg = withSteps.sumOf { daily.getValue(it) }.toDouble() / withSteps.size
    return "Daily average ${formatSteps(avg.roundToLong())} on ${withSteps.size} days with steps"
}

/** The earliest day the Steps screen needs (start of the month 5 months ago). */
fun stepsHistoryStart(today: LocalDate): LocalDate = today.withDayOfMonth(1).minusMonths(5)

// ---------- Sleep ----------

data class StageRow(val stage: SleepStage, val name: String, val value: String)
data class TimelineSegment(val stage: SleepStage, val weight: Float)

data class SleepSummary(
    val total: String,
    /** Of the 8-hour ring. */
    val ringFraction: Float,
    val note: String,
    val bed: String,
    val wake: String,
    val stages: List<StageRow>,
    val timeline: List<TimelineSegment>,
    val score: Int?,
    val tip: String,
)

const val NO_SLEEP_TIP = "Wear your Fit3 to bed tonight. Your stages and score show here after it syncs in the morning."

private val stageOrder = listOf(
    SleepStage.DEEP to "Deep sleep",
    SleepStage.LIGHT to "Light sleep",
    SleepStage.REM to "REM",
    SleepStage.AWAKE to "Awake",
)

/** The night to show: [lastNight] if any, else the newest in [week]. Null → empty state. */
fun sleepSummary(night: SleepRecord?, isLastNight: Boolean, week: List<SleepRecord>, zone: ZoneId): SleepSummary? {
    night ?: return null
    val total = sleepDuration(night) ?: Duration.ZERO
    val clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    val bed = night.start.atZone(zone)
    val wake = night.end?.atZone(zone)
    val whenText = if (isLastNight) "Last night" else DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).format(bed)
    val span = if (wake != null) "${clock.format(bed)} – ${clock.format(wake)}" else clock.format(bed)

    val asleep = listOf(SleepStage.DEEP, SleepStage.LIGHT, SleepStage.REM).sumOf { night.stageDurations[it]?.toMinutes() ?: 0L }
    val rows = stageOrder.map { (stage, name) ->
        val d = night.stageDurations[stage]
        val value = when {
            d == null -> EMPTY
            stage == SleepStage.AWAKE -> shortDuration(d)
            asleep > 0 -> "${shortDuration(d)} · ${(d.toMinutes() * 100.0 / asleep).roundToInt()}%"
            else -> shortDuration(d)
        }
        StageRow(stage, name, value)
    }
    val timeline = night.stages.map { TimelineSegment(it.stage, Duration.between(it.start, it.end).seconds.toFloat().coerceAtLeast(1f)) }

    return SleepSummary(
        total = formatSleep(total),
        ringFraction = (total.toMinutes() / 480f).coerceIn(0f, 1f),
        note = "$whenText · $span",
        bed = "Bedtime ${clock.format(bed)}",
        wake = "Wake ${wake?.let(clock::format) ?: EMPTY}",
        stages = rows,
        timeline = timeline,
        score = night.score,
        tip = sleepTip(night, total, week),
    )
}

/** Factual, data-based tips only. */
private fun sleepTip(night: SleepRecord, total: Duration, week: List<SleepRecord>): String {
    val deepShare = deepShare(night)
    val others = week.filter { it !== night }.mapNotNull(::deepShare)
    return when {
        total < Duration.ofHours(7) -> "Under 7 hours of sleep. An earlier bedtime helps you recover for training."
        deepShare != null && others.isNotEmpty() && deepShare > others.average() -> "Great night! Deep sleep was above your weekly average."
        total > Duration.ofHours(9) -> "Over 9 hours of sleep. Keep your bedtime and wake time steady."
        else -> "Within your 7–9 hour goal. Keep the same bedtime tonight."
    }
}

private fun deepShare(r: SleepRecord): Double? {
    val asleep = listOf(SleepStage.DEEP, SleepStage.LIGHT, SleepStage.REM).sumOf { r.stageDurations[it]?.toMinutes() ?: 0L }
    val deep = r.stageDurations[SleepStage.DEEP]?.toMinutes() ?: return null
    return if (asleep > 0) deep.toDouble() / asleep else null
}

/** "1h 42m", "47m". */
fun shortDuration(d: Duration): String = if (d.toHours() > 0) "${d.toHours()}h %02dm".format(Locale.ENGLISH, d.toMinutes() % 60) else "${d.toMinutes()}m"

// ---------- Blood oxygen ----------

data class OxygenStats(val latest: Int, val average: Int, val lowest: Int)

fun oxygenStats(samples: List<BloodOxygenSample>): OxygenStats? {
    if (samples.isEmpty()) return null
    val latest = samples.maxBy { it.time }
    return OxygenStats(
        latest = latest.percent.roundToInt(),
        average = samples.map { it.percent }.average().roundToInt(),
        lowest = samples.minOf { it.minPercent ?: it.percent }.roundToInt(),
    )
}
