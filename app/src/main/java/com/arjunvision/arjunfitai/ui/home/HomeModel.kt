package com.arjunvision.arjunfitai.ui.home

import com.arjunvision.arjunfitai.data.health.model.SleepRecord
import com.arjunvision.arjunfitai.data.health.model.WeightRecord
import com.arjunvision.arjunfitai.ui.dashboard.HealthSnapshot
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Pure Home-screen logic (no Android or Compose), so it is unit-tested.

/** Winter Arc: 12 Oct – 31 Dec, 81 days, every year. */
object WinterArc {
    sealed interface Status {
        data class Upcoming(val daysUntil: Long) : Status
        data class Active(val day: Long, val totalDays: Long) : Status
        data class Complete(val totalDays: Long) : Status
    }

    fun start(year: Int): LocalDate = LocalDate.of(year, 10, 12)
    fun end(year: Int): LocalDate = LocalDate.of(year, 12, 31)

    fun status(today: LocalDate): Status {
        val start = start(today.year)
        val end = end(today.year)
        val total = ChronoUnit.DAYS.between(start, end) + 1
        return when {
            today.isBefore(start) -> Status.Upcoming(ChronoUnit.DAYS.between(today, start))
            today.isAfter(end) -> Status.Complete(total)
            else -> Status.Active(ChronoUnit.DAYS.between(start, today) + 1, total)
        }
    }

    /** "WINTER ARC DAY 22 OF 81". */
    fun label(today: LocalDate): String = when (val s = status(today)) {
        is Status.Active -> "WINTER ARC DAY ${s.day} OF ${s.totalDays}"
        is Status.Upcoming -> if (s.daysUntil == 1L) "WINTER ARC STARTS TOMORROW" else "WINTER ARC STARTS IN ${s.daysUntil} DAYS"
        is Status.Complete -> "WINTER ARC COMPLETE"
    }
}

fun greeting(time: LocalTime, name: String): String {
    val part = when (time.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isBlank()) part else "$part, ${name.trim()}"
}

/** "MON 2 NOV". */
fun dateLabel(today: LocalDate): String =
    DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).format(today).uppercase(Locale.ENGLISH)

data class WeightProgress(
    val currentKg: Float,
    val targetKg: Float,
    /** Change since the baseline; null with only one weigh-in. */
    val changeKg: Float?,
    val baselineDate: LocalDate?,
    /** 0–100 of the way from baseline to target; null when it can't be computed. */
    val percentToGoal: Int?,
)

/**
 * Baseline = the last weigh-in before the Winter Arc started, else the first one since.
 * [history] may be in any order.
 */
fun weightProgress(history: List<WeightRecord>, targetKg: Float, arcStart: LocalDate, zone: ZoneId): WeightProgress? {
    if (history.isEmpty()) return null
    val sorted = history.sortedBy { it.time }
    val current = sorted.last()
    val arcStartInstant = arcStart.atStartOfDay(zone).toInstant()
    val baseline = sorted.lastOrNull { it.time < arcStartInstant } ?: sorted.first()
    if (baseline === current) {
        return WeightProgress(current.weightKg, targetKg, null, null, null)
    }
    val change = current.weightKg - baseline.weightKg
    val toLose = baseline.weightKg - targetKg
    val percent = if (toLose > 0f) ((baseline.weightKg - current.weightKg) / toLose * 100f).roundToInt().coerceIn(0, 100) else null
    return WeightProgress(current.weightKg, targetKg, change, baseline.time.atZone(zone).toLocalDate(), percent)
}

/** "−3.0 kg since Oct 12 · 20% to goal", or the canvas's empty-state hint. */
fun weightNote(progress: WeightProgress?, arcStart: LocalDate): String {
    if (progress?.changeKg == null) return "Log today's weight to see progress"
    val sign = if (progress.changeKg < 0f) "−" else "+"
    val since = progress.baselineDate?.let { if (it.isBefore(arcStart)) arcStart else it } ?: arcStart
    val sinceText = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH).format(since)
    val change = "$sign${"%.1f".format(Locale.ENGLISH, abs(progress.changeKg))} kg since $sinceText"
    return progress.percentToGoal?.let { "$change · $it% to goal" } ?: change
}

/** Newest record across everything we read — what the "latest reading" chip shows (not Samsung's sync time). */
fun latestReading(snapshot: HealthSnapshot): Instant? = listOfNotNull(
    snapshot.heartRate.maxOfOrNull { it.time },
    snapshot.bloodOxygen.maxOfOrNull { it.time },
    snapshot.exercise.maxOfOrNull { it.end },
    snapshot.sleep.mapNotNull { it.end }.maxOrNull(),
    snapshot.weight.maxOfOrNull { it.time },
).maxOrNull()

/** "17:28" today, otherwise "Sun 1 Nov, 17:28". */
fun formatReadingTime(instant: Instant, today: LocalDate, zone: ZoneId): String {
    val time = instant.atZone(zone)
    val clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).format(time)
    return if (time.toLocalDate() == today) clock else DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).format(time) + ", " + clock
}

/** The most recent sleep that ended within the last 24 hours ("last night"), else null. */
fun lastNightSleep(sleep: List<SleepRecord>, now: Instant): SleepRecord? =
    sleep.filter { record ->
        val end = record.end ?: record.duration?.let { record.start.plus(it) } ?: record.start
        !end.isBefore(now.minus(Duration.ofHours(24)))
    }.maxByOrNull { it.start }

fun sleepDuration(record: SleepRecord): Duration? = record.duration ?: record.end?.let { Duration.between(record.start, it) }

// Formatting shared by the Home screen. "—" is the canvas's empty value.

const val EMPTY = "—"

fun formatSteps(steps: Long): String = NumberFormat.getIntegerInstance(Locale.ENGLISH).format(steps)

fun formatKcal(kcal: Float): String = NumberFormat.getIntegerInstance(Locale.ENGLISH).format(kcal.roundToInt())

/** "4.9 km", or "0.08 km" under 1 km. */
fun formatKm(meters: Float): String {
    val km = meters / 1000f
    return if (km < 1f) "%.2f km".format(Locale.ENGLISH, km) else "%.1f km".format(Locale.ENGLISH, km)
}

/** "48 min" under an hour, otherwise "1h 05m". */
fun formatActiveTime(duration: Duration): String {
    val hours = duration.toHours()
    return if (hours > 0) "${hours}h %02dm".format(Locale.ENGLISH, duration.toMinutes() % 60) else "${duration.toMinutes()} min"
}

/** "7h 05m". */
fun formatSleep(duration: Duration): String = "${duration.toHours()}h %02dm".format(Locale.ENGLISH, duration.toMinutes() % 60)
