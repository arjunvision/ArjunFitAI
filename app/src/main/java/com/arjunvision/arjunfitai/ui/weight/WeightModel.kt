package com.arjunvision.arjunfitai.ui.weight

import com.arjunvision.arjunfitai.data.local.WeightEntry
import com.arjunvision.arjunfitai.data.health.model.WeightRecord
import com.arjunvision.arjunfitai.ui.home.WinterArc
import com.arjunvision.arjunfitai.ui.nutrition.Source
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

// Pure Weight Tracker logic (unit-tested).

data class WeighIn(val time: Instant, val kg: Float, val source: Source, val local: WeightEntry? = null)

/** Samsung Health body-composition weights + weights logged in ArjunFit, newest first. */
fun mergeWeights(samsung: List<WeightRecord>, local: List<WeightEntry>): List<WeighIn> =
    (samsung.map { WeighIn(it.time, it.weightKg, Source.SAMSUNG) } +
        local.map { WeighIn(Instant.ofEpochMilli(it.time), it.kg, Source.ARJUNFIT, it) })
        .sortedByDescending { it.time }

/** As [WeightRecord]s, so Home and the tracker share [com.arjunvision.arjunfitai.ui.home.weightProgress]. */
fun List<WeighIn>.asRecords(): List<WeightRecord> = map { WeightRecord(it.time, it.kg, null, null, null) }

data class WeightChart(val title: String, val from: LocalDate, val to: LocalDate, val points: List<WeighIn>, val yMin: Float, val yMax: Float)

/** Winter Arc start → today while the arc runs; otherwise the last 3 weeks. Null with fewer than 2 weigh-ins. */
fun weightChart(all: List<WeighIn>, today: LocalDate, zone: ZoneId): WeightChart? {
    val arc = WinterArc.status(today)
    val (from, title) = if (arc is WinterArc.Status.Active) {
        WinterArc.start(today.year) to "Since Oct 12"
    } else {
        today.minusDays(20) to "Last 3 weeks"
    }
    val fromInstant = from.atStartOfDay(zone).toInstant()
    val points = all.filter { !it.time.isBefore(fromInstant) }.sortedBy { it.time }
    if (points.size < 2) return null
    val lo = floor(points.minOf { it.kg } - 1f)
    val hi = ceil(points.maxOf { it.kg } + 1f)
    return WeightChart(title, from, today, points, lo, maxOf(hi, lo + 3f))
}

/** Parses "107.4" or "107,4"; 30–300 kg. */
fun parseWeightKg(text: String): Float? = text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it in 30f..300f }
