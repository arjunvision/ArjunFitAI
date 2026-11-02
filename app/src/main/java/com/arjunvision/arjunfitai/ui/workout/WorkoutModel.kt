package com.arjunvision.arjunfitai.ui.workout

import com.arjunvision.arjunfitai.data.workout.Measure
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.PlanItemEntity
import com.arjunvision.arjunfitai.data.workout.PlanKind
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

// Pure workout logic (unit-tested).

/** Winter Arc week from the canvas: strength Mon/Wed/Fri, cardio Tue/Thu/Sat, Sunday recovery. */
enum class DayKind(val tag: String) { STRENGTH("S"), CARDIO("C"), REST("R") }

fun dayKind(day: DayOfWeek): DayKind = when (day) {
    DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> DayKind.STRENGTH
    DayOfWeek.SUNDAY -> DayKind.REST
    else -> DayKind.CARDIO
}

data class DayCell(val date: LocalDate, val label: String, val kind: DayKind, val isToday: Boolean, val worked: Boolean)

/** Monday–Sunday of [today]'s week; [worked] = a finished session that day. */
fun weekStrip(today: LocalDate, sessions: List<SessionEntity>, zone: ZoneId): List<DayCell> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val doneDays = sessions.filter { it.end != null }.map { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }.toSet()
    return (0..6).map { i ->
        val d = monday.plusDays(i.toLong())
        DayCell(d, d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), dayKind(d.dayOfWeek), d == today, d in doneDays)
    }
}

data class TodayPick(val plan: PlanEntity?, val label: String)

/**
 * Gym: the plan for today's weekday (MON/WED/FRI badge), else the next one this week.
 * Home / Custom: the plan after the last one finished (rotates A → B → C).
 */
fun todaysPlan(kind: PlanKind, plans: List<PlanEntity>, sessions: List<SessionEntity>, today: LocalDate): TodayPick {
    val ofKind = plans.filter { it.kind == kind }
    if (ofKind.isEmpty()) return TodayPick(null, if (kind == PlanKind.CUSTOM) "BUILD YOUR OWN" else "NO PLANS")
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)
    if (kind == PlanKind.GYM) {
        val byDay = ofKind.mapNotNull { p -> dayOf(p.dayLabel)?.let { it to p } }.sortedBy { it.first.value }
        byDay.firstOrNull { it.first == today.dayOfWeek }?.let { return TodayPick(it.second, "TODAY · $dayName") }
        val next = byDay.firstOrNull { it.first > today.dayOfWeek } ?: byDay.firstOrNull()
        if (next != null) {
            return TodayPick(next.second, "NEXT · " + next.first.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH))
        }
    }
    val last = sessions.filter { it.end != null && it.planId in ofKind.map(PlanEntity::id) }.maxByOrNull { it.start }
    val index = last?.let { s -> ofKind.indexOfFirst { it.id == s.planId } } ?: -1
    return TodayPick(ofKind[(index + 1) % ofKind.size], "TODAY · $dayName")
}

private fun dayOf(label: String): DayOfWeek? = DayOfWeek.entries.firstOrNull { it.name.startsWith(label.uppercase(Locale.ENGLISH)) && label.length >= 3 }

/** "5 exercises · ~55 min" (≈2.5 min per working set incl. rest). */
fun planMeta(items: List<PlanItemEntity>): String {
    val minutes = items.sumOf { item ->
        when (item.unit) {
            Measure.REPS -> item.sets * 2.5
            Measure.SECONDS -> item.sets * (item.target + 60) / 60.0
            Measure.MINUTES -> item.target.toDouble()
        }
    }
    val rounded = ((minutes / 5).roundToInt() * 5).coerceAtLeast(5)
    val count = items.size
    return "$count ${if (count == 1) "exercise" else "exercises"} · ~$rounded min"
}

/** "4 × 8", "3 × 45 sec", "30 min". */
fun targetText(item: PlanItemEntity): String = when (item.unit) {
    Measure.MINUTES -> if (item.sets == 1) "${item.target} min" else "${item.sets} × ${item.target} min"
    Measure.SECONDS -> "${item.sets} × ${item.target} sec"
    Measure.REPS -> "${item.sets} × ${item.target}"
}

/** "60 kg × 8" from the most recent finished session's same exercise + set number. */
fun previousSet(history: List<SetEntity>, sessions: List<SessionEntity>, exerciseName: String, setIndex: Int, currentSessionId: Long): String? {
    val finished = sessions.filter { it.end != null && it.id != currentSessionId }.sortedByDescending { it.start }
    for (s in finished) {
        val hit = history.firstOrNull { it.sessionId == s.id && it.exerciseName == exerciseName && it.setIndex == setIndex && it.done } ?: continue
        return setText(hit)
    }
    return null
}

fun setText(set: SetEntity): String {
    val count = set.count?.let { "$it" + if (set.unit == Measure.REPS) "" else " ${set.unit.short}" }
    return when {
        set.kg != null && set.kg > 0f && count != null -> "${kg(set.kg)} kg × $count"
        count != null -> if (set.unit == Measure.REPS) "$count reps" else count
        set.kg != null -> "${kg(set.kg)} kg"
        else -> "done"
    }
}

private fun kg(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(Locale.ENGLISH, v)

data class SessionSummary(val minutes: Int, val setsDone: Int, val volumeKg: Float)

fun sessionSummary(session: SessionEntity, sets: List<SetEntity>): SessionSummary {
    val done = sets.filter { it.sessionId == session.id && it.done }
    val end = session.end ?: done.mapNotNull { it.doneAt }.maxOrNull() ?: session.start
    return SessionSummary(
        minutes = ((end - session.start) / 60_000).toInt(),
        setsDone = done.size,
        volumeKg = done.filter { it.unit == Measure.REPS }.sumOf { ((it.kg ?: 0f) * (it.count ?: 0)).toDouble() }.toFloat(),
    )
}

/** Best set per exercise: heaviest weight (ties → more reps), with Epley estimated 1-rep max. */
data class PersonalRecord(val exerciseName: String, val kg: Float, val reps: Int, val date: Instant, val estimated1Rm: Float)

fun personalRecords(sessions: List<SessionEntity>, sets: List<SetEntity>): List<PersonalRecord> {
    val finished = sessions.filter { it.end != null }.associateBy { it.id }
    return sets
        .filter { it.done && it.sessionId in finished && it.unit == Measure.REPS && (it.kg ?: 0f) > 0f && (it.count ?: 0) > 0 }
        .groupBy { it.exerciseName }
        .map { (name, list) ->
            val best = list.maxWith(compareBy<SetEntity>({ it.kg }, { it.count }))
            val kg = best.kg!!
            val reps = best.count!!
            PersonalRecord(name, kg, reps, Instant.ofEpochMilli(best.doneAt ?: finished.getValue(best.sessionId).start), kg * (1 + reps / 30f))
        }
        .sortedByDescending { it.date }
}

/** "1:30". */
fun clock(seconds: Int): String = "${seconds.coerceAtLeast(0) / 60}:%02d".format(Locale.ENGLISH, seconds.coerceAtLeast(0) % 60)
