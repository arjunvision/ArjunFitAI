package com.arjunvision.arjunfitai.data.health

import androidx.activity.ComponentActivity
import com.arjunvision.arjunfitai.data.health.model.ActivitySummary
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthSourcesTest {
    /** A source that only answers availability and permission checks, and counts disconnects. */
    private class FakeSource(
        override val kind: SourceKind,
        private val availability: Availability,
        private val granted: Set<HealthMetric>,
    ) : HealthDataSource {
        var disconnects = 0
        override val supportedMetrics = HealthMetric.entries.toSet()
        override suspend fun isAvailable() = availability
        override fun connect() = Result.success(Unit)
        override fun disconnect() { disconnects++ }
        override suspend fun checkPermissions() = Result.success(PermissionStatus(granted))
        override suspend fun requestReadPermissions(activity: ComponentActivity) = checkPermissions()
        override suspend fun getActivitySummary(date: LocalDate) =
            Result.success(ActivitySummary(activeCaloriesKcal = 100f, totalCaloriesKcal = null, activeTime = null, distanceMeters = null))
        override suspend fun getTodaySteps() = TODO()
        override suspend fun getStepsHistory(from: LocalDate, to: LocalDate) = TODO()
        override suspend fun getHeartRateHistory(days: Long) = TODO()
        override suspend fun getHeartRateSeries(from: LocalDateTime, to: LocalDateTime) = TODO()
        override suspend fun getDailyHeartRateRange(from: LocalDate, to: LocalDate) = TODO()
        override suspend fun getExerciseHistory(days: Long) = TODO()
        override suspend fun getSleepHistory(days: Long) = TODO()
        override suspend fun getBloodOxygenHistory(days: Long) = TODO()
        override suspend fun getWeightHistory(days: Long) = TODO()
        override suspend fun getNutrition(date: LocalDate) = TODO()
        override suspend fun getWaterIntake(date: LocalDate) = TODO()
    }

    @Test fun choosesAndReadsThroughTheCurrentSource() = runBlocking {
        val samsung = FakeSource(SourceKind.SAMSUNG, Availability.AVAILABLE, setOf(HealthMetric.STEPS))
        val sources = HealthSources(listOf(samsung))
        val choice = sources.choose(SourcePreference.AUTO)
        assertEquals(SourceKind.SAMSUNG, choice.kind)
        assertSame(samsung, sources.current.value)
        assertEquals(100f, sources.getActivitySummary(LocalDate.of(2026, 11, 3)).getOrThrow().activeCaloriesKcal)
    }

    @Test fun noSourceGivesAClearFailureNotACrash() = runBlocking {
        val sources = HealthSources(emptyList())
        assertNull(sources.choose(SourcePreference.AUTO).kind)
        val error = sources.getNutrition(LocalDate.of(2026, 11, 3)).exceptionOrNull()
        assertTrue(error is HealthDataError && error.reason == HealthDataError.Reason.UNSUPPORTED)
    }

    @Test fun switchingSourcesDisconnectsTheOldOne() = runBlocking {
        val samsung = FakeSource(SourceKind.SAMSUNG, Availability.AVAILABLE, setOf(HealthMetric.STEPS))
        val connect = FakeSource(SourceKind.HEALTH_CONNECT, Availability.AVAILABLE, setOf(HealthMetric.STEPS))
        val sources = HealthSources(listOf(samsung, connect))
        sources.choose(SourcePreference.AUTO)
        sources.choose(SourcePreference.HEALTH_CONNECT)
        assertSame(connect, sources.current.value)
        assertEquals(1, samsung.disconnects)
    }
}
