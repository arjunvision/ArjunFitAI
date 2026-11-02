package com.arjunvision.arjunfitai.ui.sync

import com.arjunvision.arjunfitai.data.samsung.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.samsung.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.samsung.model.HeartRateSample
import com.arjunvision.arjunfitai.data.samsung.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthSyncModelTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = ZonedDateTime.of(2026, 11, 2, 21, 40, 0, 0, zone).toInstant() // Monday

    private fun at(day: Int, h: Int, m: Int) = ZonedDateTime.of(2026, 11, day, h, m, 0, 0, zone).toInstant()

    private fun inputs(
        granted: Set<SyncType> = SyncType.entries.toSet(),
        failed: Set<SyncType> = emptySet(),
        exercise: List<ExerciseRecord> = emptyList(),
    ) = SyncInputs(
        granted = granted,
        failed = failed,
        steps = 3377,
        activity = null,
        heartRate = listOf(HeartRateSample(at(2, 21, 30), 116f, null, null, 1)),
        exercise = exercise,
        sleep = emptyList(),
        bloodOxygen = listOf(BloodOxygenSample(at(2, 16, 44), 97.2f, null, null)),
        weight = listOf(WeightRecord(at(2, 8, 0), 110f, null, null, 38.1f)),
    )

    private fun session(start: java.time.Instant) =
        ExerciseRecord(start, start.plusSeconds(600), "RUNNING", null, Duration.ofMinutes(10), 50f, null, null, null)

    @Test fun rowsMatchCanvasWordingAndOrder() {
        val rows = syncRows(inputs(exercise = listOf(session(at(2, 10, 0)), session(at(2, 11, 0)))), now, zone)
        assertEquals(SyncType.entries.toList(), rows.map { it.type })
        assertEquals("Today · 3,377", rows[0].note)
        assertEquals("Today 21:30", rows[1].note)
        assertEquals("2 today", rows[2].note)
        assertEquals("No records yet", rows[3].note)
        assertEquals(false, rows[3].hasRecords)
        assertEquals("Latest 97%", rows[4].note)
        assertEquals("Weight 110.0 kg", rows[5].note)
        assertEquals("No records yet", rows[6].note)
        assertEquals("No records yet", rows[7].note)
        assertEquals("No records yet", rows[8].note)
        assertEquals("5 of 9 data types have records", syncSummary(rows, null, now, zone))
        assertEquals("5 of 9 data types have records · updated just now", syncSummary(rows, now.minusSeconds(20), now, zone))
    }

    @Test fun exerciseFallsBackToWeekThenLastDate() {
        // Sunday 1 Nov is in the previous week (weeks start Monday).
        val lastWeek = syncRows(inputs(exercise = listOf(session(at(1, 9, 0)))), now, zone)
        assertEquals("Last Sun 1 Nov", lastWeek[2].note)
    }

    @Test fun notAllowedAndFailedBeatData() {
        val rows = syncRows(inputs(granted = SyncType.entries.toSet() - SyncType.SLEEP, failed = setOf(SyncType.HEART_RATE)), now, zone)
        assertEquals("Couldn't read", rows[1].note)
        assertEquals("Not allowed", rows[3].note)
    }
}
