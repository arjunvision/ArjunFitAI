package com.arjunvision.arjunfitai.ui.sync

import com.arjunvision.arjunfitai.data.health.model.ActivitySummary
import com.arjunvision.arjunfitai.data.health.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.health.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.health.model.HeartRateSample
import com.arjunvision.arjunfitai.data.health.model.NutritionRecord
import com.arjunvision.arjunfitai.data.health.model.WaterRecord
import com.arjunvision.arjunfitai.data.health.model.SleepRecord
import com.arjunvision.arjunfitai.data.health.model.WeightRecord
import com.arjunvision.arjunfitai.ui.home.formatSteps
import com.arjunvision.arjunfitai.ui.home.lastNightSleep
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

// Pure Health Sync logic (no Android, no Samsung SDK), so it is unit-tested.

/** The seven Samsung Health types, in the canvas's order and wording. */
enum class SyncType(val label: String) {
    STEPS("Steps"),
    HEART_RATE("Heart rate"),
    EXERCISE("Exercise"),
    SLEEP("Sleep"),
    BLOOD_OXYGEN("Blood oxygen"),
    BODY_COMPOSITION("Body composition"),
    ACTIVITY_SUMMARY("Activity summary"),
    NUTRITION("Nutrition"),
    WATER_INTAKE("Water intake"),
}

/** [hasRecords] drives the ✓ (green) vs – badge. */
data class SyncRow(val type: SyncType, val hasRecords: Boolean, val note: String)

class SyncInputs(
    val granted: Set<SyncType>,
    val failed: Set<SyncType>,
    val steps: Long?,
    val activity: ActivitySummary?,
    val heartRate: List<HeartRateSample>,
    val exercise: List<ExerciseRecord>,
    val sleep: List<SleepRecord>,
    val bloodOxygen: List<BloodOxygenSample>,
    val weight: List<WeightRecord>,
    val nutritionToday: List<NutritionRecord> = emptyList(),
    val waterToday: List<WaterRecord> = emptyList(),
)

fun syncRows(inputs: SyncInputs, now: Instant, zone: ZoneId): List<SyncRow> = SyncType.entries.map { type ->
    when {
        type !in inputs.granted -> SyncRow(type, false, "Not allowed")
        type in inputs.failed -> SyncRow(type, false, "Couldn't read")
        else -> rowFor(type, inputs, now, zone) ?: SyncRow(type, false, "No records yet")
    }
}

private fun rowFor(type: SyncType, i: SyncInputs, now: Instant, zone: ZoneId): SyncRow? = when (type) {
    SyncType.STEPS -> i.steps?.let { SyncRow(type, true, "Today · ${formatSteps(it)}") }
    SyncType.HEART_RATE -> i.heartRate.maxOfOrNull { it.time }?.let { SyncRow(type, true, whenText(it, now, zone)) }
    SyncType.EXERCISE -> exerciseNote(i.exercise, now, zone)?.let { SyncRow(type, true, it) }
    SyncType.SLEEP -> when {
        lastNightSleep(i.sleep, now) != null -> SyncRow(type, true, "Last night")
        else -> i.sleep.maxOfOrNull { it.start }?.let { SyncRow(type, true, whenText(it, now, zone)) }
    }
    SyncType.BLOOD_OXYGEN -> i.bloodOxygen.maxByOrNull { it.time }?.let { SyncRow(type, true, "Latest ${it.percent.roundToInt()}%") }
    SyncType.BODY_COMPOSITION -> i.weight.maxByOrNull { it.time }?.let { SyncRow(type, true, "Weight %.1f kg".format(Locale.ENGLISH, it.weightKg)) }
    SyncType.ACTIVITY_SUMMARY -> i.activity
        ?.takeIf { it.activeCaloriesKcal != null || it.totalCaloriesKcal != null || it.activeTime != null || it.distanceMeters != null }
        ?.let { SyncRow(type, true, "Today") }
    SyncType.NUTRITION -> i.nutritionToday.size.takeIf { it > 0 }?.let { SyncRow(type, true, if (it == 1) "1 entry today" else "$it entries today") }
    SyncType.WATER_INTAKE -> i.waterToday.sumOf { it.ml.toDouble() }.takeIf { it > 0 }?.let { SyncRow(type, true, "Today ${it.roundToInt()} ml") }
}

/** "2 today", "3 this week" (Monday start), else the last session's date. */
private fun exerciseNote(sessions: List<ExerciseRecord>, now: Instant, zone: ZoneId): String? {
    if (sessions.isEmpty()) return null
    val today = now.atZone(zone).toLocalDate()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val todayCount = sessions.count { it.start.atZone(zone).toLocalDate() == today }
    val weekCount = sessions.count { !it.start.atZone(zone).toLocalDate().isBefore(weekStart) }
    return when {
        todayCount > 0 -> "$todayCount today"
        weekCount > 0 -> "$weekCount this week"
        else -> "Last " + whenText(sessions.maxOf { it.start }, now, zone)
    }
}

/** "Today 17:15", otherwise "Sun 1 Nov". */
fun whenText(instant: Instant, now: Instant, zone: ZoneId): String {
    val time = instant.atZone(zone)
    return if (time.toLocalDate() == now.atZone(zone).toLocalDate()) {
        "Today " + DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).format(time)
    } else {
        DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).format(time)
    }
}

/** "5 of 7 data types have records · updated just now". */
fun syncSummary(rows: List<SyncRow>, lastUpdated: Instant?, now: Instant, zone: ZoneId): String {
    val withRecords = rows.count { it.hasRecords }
    val base = if (withRecords == rows.size) "All ${rows.size} data types up to date" else "$withRecords of ${rows.size} data types have records"
    val updated = lastUpdated?.let {
        if (Duration.between(it, now) < Duration.ofMinutes(1)) "updated just now"
        else "updated " + DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).format(it.atZone(zone))
    }
    return listOfNotNull(base, updated).joinToString(" · ")
}
