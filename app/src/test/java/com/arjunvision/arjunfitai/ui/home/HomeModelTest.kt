package com.arjunvision.arjunfitai.ui.home

import com.arjunvision.arjunfitai.data.samsung.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class HomeModelTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun winterArcIs81DaysAndCountsFromOct12() {
        assertEquals("WINTER ARC DAY 1 OF 81", WinterArc.label(LocalDate.of(2026, 10, 12)))
        assertEquals("WINTER ARC DAY 22 OF 81", WinterArc.label(LocalDate.of(2026, 11, 2)))
        assertEquals("WINTER ARC DAY 81 OF 81", WinterArc.label(LocalDate.of(2026, 12, 31)))
        assertEquals("WINTER ARC STARTS TOMORROW", WinterArc.label(LocalDate.of(2026, 10, 11)))
        assertEquals("WINTER ARC STARTS IN 11 DAYS", WinterArc.label(LocalDate.of(2026, 10, 1)))
    }

    @Test fun greetingDependsOnTimeAndName() {
        assertEquals("Good morning, Sri", greeting(LocalTime.of(7, 0), " Sri "))
        assertEquals("Good afternoon", greeting(LocalTime.of(13, 0), ""))
        assertEquals("Good evening, Sri", greeting(LocalTime.of(21, 30), "Sri"))
    }

    @Test fun dateLabelMatchesCanvas() {
        assertEquals("MON 2 NOV", dateLabel(LocalDate.of(2026, 11, 2)))
    }

    @Test fun weightProgressUsesLastWeighInBeforeArc() {
        val arcStart = LocalDate.of(2026, 10, 12)
        val history = listOf(
            weight(LocalDate.of(2026, 10, 1), 110f),
            weight(LocalDate.of(2026, 10, 10), 110f),
            weight(LocalDate.of(2026, 11, 2), 107f),
        )
        val p = weightProgress(history, targetKg = 95f, arcStart = arcStart, zone = zone)!!
        assertEquals(107f, p.currentKg)
        assertEquals(-3f, p.changeKg!!, 0.001f)
        assertEquals(20, p.percentToGoal)
        assertEquals("−3.0 kg since Oct 12 · 20% to goal", weightNote(p, arcStart))
    }

    @Test fun singleWeighInShowsCanvasEmptyHint() {
        val arcStart = LocalDate.of(2026, 10, 12)
        val p = weightProgress(listOf(weight(LocalDate.of(2026, 11, 2), 110f)), 95f, arcStart, zone)!!
        assertNull(p.changeKg)
        assertEquals("Log today's weight to see progress", weightNote(p, arcStart))
        assertNull(weightProgress(emptyList(), 95f, arcStart, zone))
    }

    @Test fun formattingMatchesCanvas() {
        assertEquals("6,740", formatSteps(6740))
        assertEquals("2,140", formatKcal(2139.6f))
        assertEquals("4.9 km", formatKm(4_900f))
        assertEquals("0.08 km", formatKm(80f))
        assertEquals("48 min", formatActiveTime(Duration.ofMinutes(48)))
        assertEquals("1h 29m", formatActiveTime(Duration.ofMinutes(89)))
        assertEquals("7h 05m", formatSleep(Duration.ofMinutes(425)))
    }

    private fun weight(date: LocalDate, kg: Float) =
        WeightRecord(date.atTime(8, 0).atZone(zone).toInstant(), kg, null, null, null)
}
