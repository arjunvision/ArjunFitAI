package com.arjunvision.arjunfitai.data.health

import com.arjunvision.arjunfitai.data.health.model.ActivitySummary
import com.arjunvision.arjunfitai.data.health.model.DailyRange
import com.arjunvision.arjunfitai.data.health.model.HeartRatePoint
import com.arjunvision.arjunfitai.data.health.model.NutritionRecord
import com.arjunvision.arjunfitai.data.health.model.WaterRecord
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * All health sources in this build, and the one currently in use. Screens read through the
 * functions below, so they always follow the current source.
 */
class HealthSources(val all: List<HealthDataSource>) {
    private val _current = MutableStateFlow<HealthDataSource?>(null)
    val current: StateFlow<HealthDataSource?> = _current.asStateFlow()

    fun byKind(kind: SourceKind): HealthDataSource? = all.firstOrNull { it.kind == kind }

    /** Checks every source, picks one with [chooseSource] and makes it current. */
    suspend fun choose(preference: SourcePreference, previous: SourceKind? = null): SourceChoice {
        val states = all.map { source ->
            val availability = source.isAvailable()
            val consent = availability == Availability.AVAILABLE &&
                source.connect().isSuccess &&
                source.checkPermissions().getOrNull()?.noneGranted == false
            SourceState(source.kind, availability, consent)
        }
        val choice = chooseSource(preference, states, previous)
        val next = choice.kind?.let(::byKind)
        if (next !== _current.value) _current.value?.disconnect()
        _current.value = next
        return choice
    }

    suspend fun getStepsHistory(from: LocalDate, to: LocalDate): Result<Map<LocalDate, Long>> =
        read { getStepsHistory(from, to) }

    suspend fun getHeartRateSeries(from: LocalDateTime, to: LocalDateTime): Result<List<HeartRatePoint>> =
        read { getHeartRateSeries(from, to) }

    suspend fun getDailyHeartRateRange(from: LocalDate, to: LocalDate): Result<List<DailyRange>> =
        read { getDailyHeartRateRange(from, to) }

    suspend fun getActivitySummary(date: LocalDate): Result<ActivitySummary> = read { getActivitySummary(date) }

    suspend fun getNutrition(date: LocalDate): Result<List<NutritionRecord>> = read { getNutrition(date) }

    suspend fun getWaterIntake(date: LocalDate): Result<List<WaterRecord>> = read { getWaterIntake(date) }

    private suspend fun <T> read(block: suspend HealthDataSource.() -> Result<T>): Result<T> =
        _current.value?.block() ?: Result.failure(noSource())

    companion object {
        fun noSource() = HealthDataError(null, HealthDataError.Reason.UNSUPPORTED, "No health data source is connected.")
    }
}
