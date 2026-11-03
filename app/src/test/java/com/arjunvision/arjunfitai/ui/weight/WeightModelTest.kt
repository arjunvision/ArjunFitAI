package com.arjunvision.arjunfitai.ui.weight

import com.arjunvision.arjunfitai.data.local.WeightEntry
import com.arjunvision.arjunfitai.data.health.model.WeightRecord
import com.arjunvision.arjunfitai.ui.nutrition.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeightModelTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(date: LocalDate) = date.atTime(8, 0).atZone(zone).toInstant()

    @Test fun mergesSourcesNewestFirst() {
        val samsung = listOf(WeightRecord(at(LocalDate.of(2026, 11, 2)), 110f, null, null, 38.1f))
        val local = listOf(WeightEntry(1, at(LocalDate.of(2026, 11, 3)).toEpochMilli(), 109.6f))
        val merged = mergeWeights(samsung, local)
        assertEquals(listOf(Source.ARJUNFIT, Source.SAMSUNG), merged.map { it.source })
        assertEquals(local.first(), merged.first().local)
        assertEquals(listOf(109.6f, 110f), merged.asRecords().map { it.weightKg })
    }

    @Test fun chartCoversWinterArcAndNeedsTwoPoints() {
        val today = LocalDate.of(2026, 11, 2)
        val one = mergeWeights(listOf(WeightRecord(at(today), 110f, null, null, null)), emptyList())
        assertNull(weightChart(one, today, zone))

        val two = mergeWeights(
            listOf(WeightRecord(at(LocalDate.of(2026, 10, 1)), 112f, null, null, null), WeightRecord(at(today), 110f, null, null, null)),
            listOf(WeightEntry(1, at(LocalDate.of(2026, 10, 20)).toEpochMilli(), 111f)),
        )
        val chart = weightChart(two, today, zone)!!
        assertEquals("Since Oct 12", chart.title)
        assertEquals(LocalDate.of(2026, 10, 12), chart.from)
        assertEquals(2, chart.points.size) // Oct 1 is before the arc
        assertEquals(109f, chart.yMin)
        assertEquals(112f, chart.yMax)
    }

    @Test fun outsideTheArcShowsLastThreeWeeks() {
        val today = LocalDate.of(2027, 1, 20)
        val ws = mergeWeights(emptyList(), listOf(
            WeightEntry(1, at(LocalDate.of(2027, 1, 5)).toEpochMilli(), 100f),
            WeightEntry(2, at(today).toEpochMilli(), 99f),
        ))
        assertEquals("Last 3 weeks", weightChart(ws, today, zone)!!.title)
    }

    @Test fun parsesWeights() {
        assertEquals(107.4f, parseWeightKg(" 107,4 "))
        assertNull(parseWeightKg("12"))
        assertNull(parseWeightKg("abc"))
    }
}
