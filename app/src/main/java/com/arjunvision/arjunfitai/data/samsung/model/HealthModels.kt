package com.arjunvision.arjunfitai.data.samsung.model

import java.time.Duration
import java.time.Instant

// App-side models. They carry no Samsung SDK types so the UI and the rest of the app
// never depend on the SDK directly.

data class HeartRateSample(
    val time: Instant,
    val bpm: Float,
    val minBpm: Float?,
    val maxBpm: Float?,
    /** Individual measurements in this record (Samsung Health batches them into series). */
    val measurementCount: Int,
)

data class ExerciseRecord(
    val start: Instant,
    val end: Instant,
    /** SDK exercise type name, e.g. "RUNNING". */
    val type: String,
    val title: String?,
    val duration: Duration,
    val caloriesKcal: Float,
    val distanceMeters: Float?,
    val meanHeartRate: Float?,
    val maxHeartRate: Float?,
)

enum class SleepStage { AWAKE, LIGHT, DEEP, REM, UNDEFINED }

data class SleepRecord(
    val start: Instant,
    val end: Instant?,
    val duration: Duration?,
    val score: Int?,
    val stageDurations: Map<SleepStage, Duration>,
)

data class BloodOxygenSample(
    val time: Instant,
    val percent: Float,
    val minPercent: Float?,
    val maxPercent: Float?,
)

data class WeightRecord(
    val time: Instant,
    val weightKg: Float,
    val bodyFatPercent: Float?,
    val skeletalMuscleMassKg: Float?,
    val bmi: Float?,
)

data class ActivitySummary(
    val activeCaloriesKcal: Float?,
    val totalCaloriesKcal: Float?,
    val activeTime: Duration?,
    val distanceMeters: Float?,
)
