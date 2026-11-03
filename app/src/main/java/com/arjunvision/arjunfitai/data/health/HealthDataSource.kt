package com.arjunvision.arjunfitai.data.health

import androidx.activity.ComponentActivity
import com.arjunvision.arjunfitai.data.health.model.ActivitySummary
import com.arjunvision.arjunfitai.data.health.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.health.model.DailyRange
import com.arjunvision.arjunfitai.data.health.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.health.model.HeartRatePoint
import com.arjunvision.arjunfitai.data.health.model.HeartRateSample
import com.arjunvision.arjunfitai.data.health.model.NutritionRecord
import com.arjunvision.arjunfitai.data.health.model.SleepRecord
import com.arjunvision.arjunfitai.data.health.model.WaterRecord
import com.arjunvision.arjunfitai.data.health.model.WeightRecord
import java.time.LocalDate
import java.time.LocalDateTime

/** Where health data comes from. Order = priority for automatic selection. */
enum class SourceKind(val label: String) {
    SAMSUNG("Samsung Health"),
    HEALTH_CONNECT("Health Connect"),
    DEMO("Demo mode"),
}

/** Whether a source can be used on this phone, in this build. */
enum class Availability {
    AVAILABLE,
    NOT_INSTALLED,
    UPDATE_REQUIRED,
    /** Not possible on this phone or not included in this build. */
    UNSUPPORTED,
}

/**
 * One place health data can be read from: Samsung Health, Health Connect or Demo mode.
 *
 * Every source returns the same app models (`data.health.model`), so screens never know which
 * source they are showing. READ-only: no source requests or uses WRITE access. Failures are
 * always a [HealthDataError].
 */
interface HealthDataSource {
    val kind: SourceKind

    /** The metrics this source can offer at all; permissions are requested only for these. */
    val supportedMetrics: Set<HealthMetric>

    suspend fun isAvailable(): Availability

    /** Prepares the source. Doesn't prove it's ready: problems surface from the first check or read. */
    fun connect(): Result<Unit>

    /** Forgets the connection. Nothing is revoked; permissions stay as the user set them. */
    fun disconnect()

    suspend fun checkPermissions(): Result<PermissionStatus>

    /** Shows the source's own consent screen for any READ permissions not yet granted. */
    suspend fun requestReadPermissions(activity: ComponentActivity): Result<PermissionStatus>

    /** Steps since local midnight. */
    suspend fun getTodaySteps(): Result<Long>

    /** Step totals per local day for [from]..[to] inclusive. Days without steps are absent. */
    suspend fun getStepsHistory(from: LocalDate, to: LocalDate): Result<Map<LocalDate, Long>>

    /** Heart-rate readings, newest first. */
    suspend fun getHeartRateHistory(days: Long = 7): Result<List<HeartRateSample>>

    /** Every heart-rate measurement between [from] and [to] (local time), oldest first. */
    suspend fun getHeartRateSeries(from: LocalDateTime, to: LocalDateTime): Result<List<HeartRatePoint>>

    /** Daily lowest/highest heart rate for [from]..[to] inclusive. */
    suspend fun getDailyHeartRateRange(from: LocalDate, to: LocalDate): Result<List<DailyRange>>

    /** Exercise sessions, newest first. */
    suspend fun getExerciseHistory(days: Long = 30): Result<List<ExerciseRecord>>

    /** Sleep records with time spent in each stage, newest first. */
    suspend fun getSleepHistory(days: Long = 7): Result<List<SleepRecord>>

    /** SpO2 readings, newest first. */
    suspend fun getBloodOxygenHistory(days: Long = 7): Result<List<BloodOxygenSample>>

    /** Weigh-ins (with body composition where available), newest first. */
    suspend fun getWeightHistory(days: Long = 90): Result<List<WeightRecord>>

    /** Active/total calories, active time and distance for one local day. */
    suspend fun getActivitySummary(date: LocalDate): Result<ActivitySummary>

    /** Food and meals logged in the source on [date], oldest first. */
    suspend fun getNutrition(date: LocalDate): Result<List<NutritionRecord>>

    /** Water logged in the source on [date], oldest first (ml). */
    suspend fun getWaterIntake(date: LocalDate): Result<List<WaterRecord>>
}
