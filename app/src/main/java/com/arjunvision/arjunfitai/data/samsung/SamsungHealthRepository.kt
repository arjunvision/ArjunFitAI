package com.arjunvision.arjunfitai.data.samsung

import android.app.Activity
import android.content.Context
import com.arjunvision.arjunfitai.data.samsung.model.ActivitySummary
import com.arjunvision.arjunfitai.data.samsung.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.samsung.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.samsung.model.HeartRateSample
import com.arjunvision.arjunfitai.data.samsung.model.SleepRecord
import com.arjunvision.arjunfitai.data.samsung.model.SleepStage
import com.arjunvision.arjunfitai.data.samsung.model.WeightRecord
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Read-only access to Samsung Health through the Samsung Health Data SDK.
 *
 * Every call returns a [Result]; failures are always a [SamsungHealthError]. Nothing here
 * requests or uses WRITE access.
 */
class SamsungHealthRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val appContext = context.applicationContext

    @Volatile
    private var store: HealthDataStore? = null

    /**
     * Gets the [HealthDataStore]. This does not prove Samsung Health is installed or ready:
     * those problems surface from the first permission check or read.
     */
    fun connectToSamsungHealth(): Result<Unit> = try {
        requireStore()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SamsungHealthError.from(e))
    }

    suspend fun checkPermissions(): Result<PermissionStatus> = call {
        PermissionStatus.from(requireStore().getGrantedPermissions(HealthMetric.ALL_READ_PERMISSIONS))
    }

    /**
     * Shows Samsung Health's consent screen for any READ permissions not yet granted.
     * Runs on the caller's dispatcher because the SDK shows UI on [activity].
     */
    suspend fun requestReadPermissions(activity: Activity): Result<PermissionStatus> = call(dispatcher = null) {
        val store = requireStore()
        val wanted = HealthMetric.ALL_READ_PERMISSIONS
        val granted = store.getGrantedPermissions(wanted)
        PermissionStatus.from(if (granted.containsAll(wanted)) granted else store.requestPermissions(wanted, activity))
    }

    /** Steps since local midnight. Steps are aggregate-only in the SDK. */
    suspend fun getTodaySteps(): Result<Long> = call {
        val request = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(today())
            .build()
        requireStore().aggregateData(request).dataList.sumOf { it.value ?: 0L }
    }

    /** Active/total calories, active time and distance since local midnight. */
    suspend fun getTodayActivitySummary(): Result<ActivitySummary> = call {
        val store = requireStore()
        val filter = today()
        val activeCalories = store.aggregateData(
            DataType.ActivitySummaryType.TOTAL_ACTIVE_CALORIES_BURNED.requestBuilder.setLocalTimeFilter(filter).build()
        ).dataList.mapNotNull { it.value }
        val totalCalories = store.aggregateData(
            DataType.ActivitySummaryType.TOTAL_CALORIES_BURNED.requestBuilder.setLocalTimeFilter(filter).build()
        ).dataList.mapNotNull { it.value }
        val activeTime = store.aggregateData(
            DataType.ActivitySummaryType.TOTAL_ACTIVE_TIME.requestBuilder.setLocalTimeFilter(filter).build()
        ).dataList.mapNotNull { it.value }
        val distance = store.aggregateData(
            DataType.ActivitySummaryType.TOTAL_DISTANCE.requestBuilder.setLocalTimeFilter(filter).build()
        ).dataList.mapNotNull { it.value }
        ActivitySummary(
            activeCaloriesKcal = activeCalories.takeIf { it.isNotEmpty() }?.sum(),
            totalCaloriesKcal = totalCalories.takeIf { it.isNotEmpty() }?.sum(),
            activeTime = activeTime.takeIf { it.isNotEmpty() }?.fold(Duration.ZERO, Duration::plus),
            distanceMeters = distance.takeIf { it.isNotEmpty() }?.sum(),
        )
    }

    /** Heart-rate readings, newest first. */
    suspend fun getHeartRateHistory(days: Long = 7): Result<List<HeartRateSample>> = call {
        val request = DataTypes.HEART_RATE.readDataRequestBuilder
            .setLocalTimeFilter(lastDays(days))
            .setOrdering(Ordering.DESC)
            .build()
        requireStore().readData(request).dataList.mapNotNull { point ->
            val bpm = point.getValue(DataType.HeartRateType.HEART_RATE) ?: return@mapNotNull null
            HeartRateSample(
                time = point.startTime,
                bpm = bpm,
                minBpm = point.getValue(DataType.HeartRateType.MIN_HEART_RATE),
                maxBpm = point.getValue(DataType.HeartRateType.MAX_HEART_RATE),
            )
        }
    }

    /** Exercise sessions, newest first. One Samsung Health record can hold several sessions. */
    suspend fun getExerciseHistory(days: Long = 30): Result<List<ExerciseRecord>> = call {
        val request = DataTypes.EXERCISE.readDataRequestBuilder
            .setLocalTimeFilter(lastDays(days))
            .setOrdering(Ordering.DESC)
            .build()
        requireStore().readData(request).dataList.flatMap { point ->
            val recordTitle = point.getValue(DataType.ExerciseType.CUSTOM_TITLE)
            point.getValue(DataType.ExerciseType.SESSIONS).orEmpty().map { session ->
                ExerciseRecord(
                    start = session.startTime,
                    end = session.endTime,
                    type = session.exerciseType.name,
                    title = session.customTitle ?: recordTitle,
                    duration = session.duration,
                    caloriesKcal = session.calories,
                    distanceMeters = session.distance,
                    meanHeartRate = session.meanHeartRate,
                    maxHeartRate = session.maxHeartRate,
                )
            }
        }.sortedByDescending { it.start }
    }

    /** Sleep records with time spent in each stage, newest first. */
    suspend fun getSleepHistory(days: Long = 7): Result<List<SleepRecord>> = call {
        val request = DataTypes.SLEEP.readDataRequestBuilder
            .setLocalTimeFilter(lastDays(days))
            .setOrdering(Ordering.DESC)
            .build()
        requireStore().readData(request).dataList.map { point ->
            val stages = point.getValue(DataType.SleepType.SESSIONS).orEmpty().flatMap { it.stages.orEmpty() }
            SleepRecord(
                start = point.startTime,
                end = point.endTime,
                duration = point.getValue(DataType.SleepType.DURATION),
                score = point.getValue(DataType.SleepType.SLEEP_SCORE),
                stageDurations = stages
                    .groupBy { stage -> SleepStage.entries.firstOrNull { it.name == stage.stage.name } ?: SleepStage.UNDEFINED }
                    .mapValues { (_, list) ->
                        list.fold(Duration.ZERO) { total, s -> total + Duration.between(s.startTime, s.endTime) }
                    },
            )
        }
    }

    /** SpO2 readings, newest first. */
    suspend fun getBloodOxygenHistory(days: Long = 7): Result<List<BloodOxygenSample>> = call {
        val request = DataTypes.BLOOD_OXYGEN.readDataRequestBuilder
            .setLocalTimeFilter(lastDays(days))
            .setOrdering(Ordering.DESC)
            .build()
        requireStore().readData(request).dataList.mapNotNull { point ->
            val percent = point.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION) ?: return@mapNotNull null
            BloodOxygenSample(
                time = point.startTime,
                percent = percent,
                minPercent = point.getValue(DataType.BloodOxygenType.MIN_OXYGEN_SATURATION),
                maxPercent = point.getValue(DataType.BloodOxygenType.MAX_OXYGEN_SATURATION),
            )
        }
    }

    /** Body-composition records that include a weight, newest first. */
    suspend fun getWeightHistory(days: Long = 90): Result<List<WeightRecord>> = call {
        val request = DataTypes.BODY_COMPOSITION.readDataRequestBuilder
            .setLocalTimeFilter(lastDays(days))
            .setOrdering(Ordering.DESC)
            .build()
        requireStore().readData(request).dataList.mapNotNull { point ->
            val weight = point.getValue(DataType.BodyCompositionType.WEIGHT) ?: return@mapNotNull null
            WeightRecord(
                time = point.startTime,
                weightKg = weight,
                bodyFatPercent = point.getValue(DataType.BodyCompositionType.BODY_FAT),
                skeletalMuscleMassKg = point.getValue(DataType.BodyCompositionType.SKELETAL_MUSCLE_MASS),
                bmi = point.getValue(DataType.BodyCompositionType.BODY_MASS_INDEX),
            )
        }
    }

    private fun requireStore(): HealthDataStore =
        store ?: synchronized(this) { store ?: HealthDataService.getStore(appContext).also { store = it } }

    private fun today(): LocalTimeFilter {
        val date = LocalDate.now(clock)
        return LocalTimeFilter.of(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
    }

    /** From local midnight [days] days ago up to the end of today. */
    private fun lastDays(days: Long): LocalTimeFilter {
        val date = LocalDate.now(clock)
        return LocalTimeFilter.of(date.minusDays(days).atStartOfDay(), date.plusDays(1).atStartOfDay())
    }

    /** Runs [block] on [dispatcher] (or the caller's context when null), mapping failures to [SamsungHealthError]. */
    private suspend fun <T> call(
        dispatcher: CoroutineDispatcher? = ioDispatcher,
        block: suspend () -> T,
    ): Result<T> = try {
        Result.success(if (dispatcher == null) block() else withContext(dispatcher) { block() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(SamsungHealthError.from(e))
    }
}
