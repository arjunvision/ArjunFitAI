package com.arjunvision.arjunfitai.ui.progress

import com.arjunvision.arjunfitai.data.workout.Measure
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.PlanKind
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import com.arjunvision.arjunfitai.ui.workout.PersonalRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ProgressModelTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 2) // Monday, day 22
    private fun ms(d: LocalDate) = d.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun timelineMatchesCanvas() {
        val t = arcTimeline(today)
        assertEquals("Day 22 of 81", t.title)
        assertEquals("59 days to go · stay consistent", t.note)
        assertEquals(22 / 81f, t.fraction, 0.0001f)
        assertEquals("Starts in 11 days", arcTimeline(LocalDate.of(2026, 10, 1)).title)
        assertEquals(1f, arcTimeline(LocalDate.of(2027, 1, 1).withYear(2026).withDayOfYear(365)).fraction)
    }

    @Test fun goalProgressAndPace() {
        val start = LocalDate.of(2026, 10, 12)
        // 3 kg in 3 weeks (1 kg/wk) vs 12 kg in ~8.4 weeks needed (≈1.4 kg/wk) → not yet on track.
        val g = goalProgress(110f, start, 107f, 95f, today)
        assertEquals("3 of 15 kg", g.title)
        assertEquals(0.2f, g.fraction, 0.001f)
        assertEquals("12 kg to go · 1.4 kg a week needed for Dec 31", g.note)
        // 6 kg in 3 weeks → on track.
        assertEquals("9 kg to go · on track for Dec 31", goalProgress(110f, start, 104f, 95f, today).note)
        assertEquals("Fills in after your first weigh-in", goalProgress(null, null, null, 95f, today).note)
    }

    @Test fun workoutCounts() {
        val plans = listOf(PlanEntity(1, PlanKind.GYM, "A", "", "MON", 0), PlanEntity(2, PlanKind.CARDIO, "Walk", "", "TUE", 1))
        val sessions = listOf(
            SessionEntity(1, 1, "A", ms(today), ms(today) + 3_600_000),
            SessionEntity(2, 2, "Walk", ms(today), ms(today) + 1_800_000),
            SessionEntity(3, 1, "A", ms(today.minusDays(3)), ms(today.minusDays(3)) + 3_600_000), // last week
        )
        assertEquals(1, strengthDaysThisWeek(sessions, plans, today, zone))
        val sets = listOf(
            SetEntity(1, 1, 0, "x", "Squat", 1, 80f, 8, Measure.REPS, true, ms(today)),
            SetEntity(2, 3, 0, "x", "Squat", 1, 60f, 10, Measure.REPS, true, ms(today)),
        )
        assertEquals(1240f, volumeSince(sessions, sets, LocalDate.of(2026, 10, 12), zone))
        assertEquals(640f, volumeSince(sessions, sets, today, zone))
    }

    @Test fun prSummaryText() {
        val prs = listOf(PersonalRecord("Bench press", 62.5f, 10, Instant.EPOCH, 0f), PersonalRecord("Squat", 80f, 8, Instant.EPOCH, 0f))
        assertEquals("Bench press 62.5 kg × 10 · Squat 80 kg × 8", prSummary(prs))
        assertEquals("Finish a workout to set your first PR", prSummary(emptyList()))
    }
}
