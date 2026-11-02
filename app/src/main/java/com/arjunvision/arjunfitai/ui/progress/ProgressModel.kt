package com.arjunvision.arjunfitai.ui.progress

import com.arjunvision.arjunfitai.data.workout.Measure
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.PlanKind
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import com.arjunvision.arjunfitai.ui.home.WinterArc
import com.arjunvision.arjunfitai.ui.workout.PersonalRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

// Pure Progress logic (unit-tested).

data class ArcTimeline(val title: String, val fraction: Float, val note: String)

/** "Day 22 of 81" + bar fraction + "59 days to go · stay consistent". */
fun arcTimeline(today: LocalDate): ArcTimeline = when (val s = WinterArc.status(today)) {
    is WinterArc.Status.Active -> {
        val left = s.totalDays - s.day
        ArcTimeline(
            "Day ${s.day} of ${s.totalDays}",
            s.day / s.totalDays.toFloat(),
            if (left == 0L) "Last day · finish strong" else "$left ${if (left == 1L) "day" else "days"} to go · stay consistent",
        )
    }
    is WinterArc.Status.Upcoming -> ArcTimeline("Starts in ${s.daysUntil} ${if (s.daysUntil == 1L) "day" else "days"}", 0f, "Winter Arc runs Oct 12 – Dec 31")
    is WinterArc.Status.Complete -> ArcTimeline("Complete · ${s.totalDays} days", 1f, "Winter Arc finished on Dec 31")
}

data class GoalProgress(val title: String, val fraction: Float, val note: String)

/**
 * "3 of 15 kg" towards the target. "On track" compares your average weekly loss so far with the
 * loss per week still needed to reach the target by Dec 31.
 */
fun goalProgress(baselineKg: Float?, baselineDate: LocalDate?, currentKg: Float?, targetKg: Float, today: LocalDate): GoalProgress {
    if (baselineKg == null || currentKg == null) return GoalProgress("— of ${kg(abs(targetKg - (baselineKg ?: targetKg)))} kg", 0f, "Fills in after your first weigh-in")
    val total = baselineKg - targetKg
    if (total <= 0f) return GoalProgress("Target reached", 1f, "You started at or below your ${kg(targetKg)} kg target")
    val lost = baselineKg - currentKg
    val fraction = (lost / total).coerceIn(0f, 1f)
    val toGo = (currentKg - targetKg).coerceAtLeast(0f)
    if (toGo == 0f) return GoalProgress("${kg(total)} of ${kg(total)} kg", 1f, "Target reached — well done")
    val end = WinterArc.end(today.year)
    val weeksLeft = ChronoUnit.DAYS.between(today, end).coerceAtLeast(0) / 7.0
    val weeksSoFar = baselineDate?.let { ChronoUnit.DAYS.between(it, today) / 7.0 } ?: 0.0
    val note = when {
        weeksLeft <= 0.0 -> "${kg(toGo)} kg to go"
        weeksSoFar < 1.0 || lost <= 0f -> "${kg(toGo)} kg to go · about ${kg((toGo / weeksLeft).toFloat())} kg a week until Dec 31"
        lost / weeksSoFar >= toGo / weeksLeft -> "${kg(toGo)} kg to go · on track for Dec 31"
        else -> "${kg(toGo)} kg to go · ${kg((toGo / weeksLeft).toFloat())} kg a week needed for Dec 31"
    }
    return GoalProgress("${kg(lost.coerceAtLeast(0f))} of ${kg(total)} kg", fraction, note)
}

/** Distinct days this week (Mon–Sun) with a finished Gym or Custom workout. */
fun strengthDaysThisWeek(sessions: List<SessionEntity>, plans: List<PlanEntity>, today: LocalDate, zone: ZoneId): Int {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val strengthPlans = plans.filter { it.kind == PlanKind.GYM || it.kind == PlanKind.CUSTOM }.map { it.id }.toSet()
    return sessions
        .filter { it.end != null && it.planId in strengthPlans }
        .map { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }
        .filter { !it.isBefore(monday) && !it.isAfter(today) }
        .distinct().size
}

/** kg × reps over finished workouts since [from]. */
fun volumeSince(sessions: List<SessionEntity>, sets: List<SetEntity>, from: LocalDate, zone: ZoneId): Float {
    val ids = sessions.filter { it.end != null && !Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate().isBefore(from) }.map { it.id }.toSet()
    return sets.filter { it.done && it.sessionId in ids && it.unit == Measure.REPS }.sumOf { ((it.kg ?: 0f) * (it.count ?: 0)).toDouble() }.toFloat()
}

/** "Bench press 62.5 kg × 10 · Squat 80 kg × 8" (two most recent PRs). */
fun prSummary(prs: List<PersonalRecord>): String =
    if (prs.isEmpty()) "Finish a workout to set your first PR"
    else prs.take(2).joinToString(" · ") { "${it.exerciseName} ${kg(it.kg)} kg × ${it.reps}" }

private fun kg(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(Locale.ENGLISH, v)
