package com.arjunvision.arjunfitai.ui.detail

import com.arjunvision.arjunfitai.data.samsung.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.samsung.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.samsung.model.HeartRatePoint
import com.arjunvision.arjunfitai.data.samsung.model.SleepRecord
import com.arjunvision.arjunfitai.data.samsung.model.SleepStage
import com.arjunvision.arjunfitai.data.samsung.model.SleepStageSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DetailModelsTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 2) // Monday
    private fun at(day: Int, h: Int, m: Int = 0): Instant = ZonedDateTime.of(2026, 11, day, h, m, 0, 0, zone).toInstant()

    @Test fun hourlyAveragesBucketByLocalHour() {
        val points = listOf(HeartRatePoint(at(2, 9, 5), 70f), HeartRatePoint(at(2, 9, 50), 80f), HeartRatePoint(at(2, 21, 30), 116f), HeartRatePoint(at(1, 9), 200f))
        val hourly = hourlyAverages(points, today, zone)
        assertEquals(24, hourly.size)
        assertEquals(75f, hourly[9])
        assertEquals(116f, hourly[21])
        assertNull(hourly[10])
        assertEquals(BpmRange(70, 200), bpmRange(points))
    }

    @Test fun workoutHeartRateIsSingleOrRange() {
        fun s(h: Int, hr: Float?) = ExerciseRecord(at(2, h), at(2, h, 20), "RUNNING", null, Duration.ofMinutes(20), 50f, null, hr, null)
        assertEquals("96", workoutHeartRate(listOf(s(7, 96f)), today, zone))
        assertEquals("70–76", workoutHeartRate(listOf(s(7, 70f), s(9, 76f), s(10, null)), today, zone))
        assertNull(workoutHeartRate(emptyList(), today, zone))
    }

    @Test fun stepsDayChartIsLast7DaysWithGoalNote() {
        val daily = mapOf(today to 3377L, today.minusDays(1) to 9000L, today.minusDays(3) to 8000L)
        val chart = stepsChart(daily, Period.DAY, today, goal = 8000)
        assertEquals(listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon"), chart.bars.map { it.label })
        assertEquals(true, chart.bars.last().isCurrent)
        assertEquals("Goal hit 2 of 7 days", chart.note)
    }

    @Test fun stepsWeekAndMonthTotals() {
        val daily = mapOf(today to 1000L, today.minusDays(1) to 2000L) // Mon 2 Nov, Sun 1 Nov
        val week = stepsChart(daily, Period.WEEK, today, 8000)
        assertEquals(6, week.bars.size)
        assertEquals(1000L, week.bars.last().steps) // this week = Monday only
        assertEquals(2000L, week.bars[4].steps)
        assertEquals("Daily average 1,500 on 2 days with steps", week.note)
        val month = stepsChart(daily, Period.MONTH, today, 8000)
        assertEquals(listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov"), month.bars.map { it.label })
        assertEquals(3000L, month.bars.last().steps)
        assertEquals(LocalDate.of(2026, 6, 1), stepsHistoryStart(today))
    }

    @Test fun sleepSummaryMatchesCanvasFormat() {
        val stages = listOf(
            SleepStageSegment(at(1, 23, 10), at(2, 0, 52), SleepStage.LIGHT),
            SleepStageSegment(at(2, 0, 52), at(2, 2, 34), SleepStage.DEEP),
            SleepStageSegment(at(2, 2, 34), at(2, 3, 21), SleepStage.REM),
            SleepStageSegment(at(2, 3, 21), at(2, 3, 33), SleepStage.AWAKE),
        )
        val record = SleepRecord(
            at(1, 23, 10), at(2, 6, 27), Duration.ofMinutes(425), 86,
            mapOf(
                SleepStage.DEEP to Duration.ofMinutes(102), SleepStage.LIGHT to Duration.ofMinutes(276),
                SleepStage.REM to Duration.ofMinutes(47), SleepStage.AWAKE to Duration.ofMinutes(12),
            ),
            stages,
        )
        val s = sleepSummary(record, isLastNight = true, week = listOf(record), zone = zone)!!
        assertEquals("7h 05m", s.total)
        assertEquals("Last night · 23:10 – 06:27", s.note)
        assertEquals("1h 42m · 24%", s.stages[0].value)
        assertEquals("4h 36m · 65%", s.stages[1].value)
        assertEquals("47m · 11%", s.stages[2].value)
        assertEquals("12m", s.stages[3].value)
        assertEquals(4, s.timeline.size)
        assertEquals(86, s.score)
        assertEquals("Within your 7–9 hour goal. Keep the same bedtime tonight.", s.tip)
        assertNull(sleepSummary(null, false, emptyList(), zone))
    }

    @Test fun oxygenStatsUseMinFields() {
        val samples = listOf(BloodOxygenSample(at(2, 3), 96f, 93f, 98f), BloodOxygenSample(at(2, 16, 44), 97.2f, null, null))
        assertEquals(OxygenStats(latest = 97, average = 97, lowest = 93), oxygenStats(samples))
        assertNull(oxygenStats(emptyList()))
    }
}
