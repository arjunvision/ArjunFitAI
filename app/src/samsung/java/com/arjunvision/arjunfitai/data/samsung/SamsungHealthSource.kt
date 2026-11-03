package com.arjunvision.arjunfitai.data.samsung

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import com.arjunvision.arjunfitai.data.health.Availability
import com.arjunvision.arjunfitai.data.health.HealthDataSource
import com.arjunvision.arjunfitai.data.health.HealthMetric
import com.arjunvision.arjunfitai.data.health.PermissionStatus
import com.arjunvision.arjunfitai.data.health.SourceKind
import com.arjunvision.arjunfitai.data.health.model.ActivitySummary
import com.arjunvision.arjunfitai.data.health.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.health.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.health.model.HeartRateSample
import com.arjunvision.arjunfitai.data.health.model.DailyRange
import com.arjunvision.arjunfitai.data.health.model.HeartRatePoint
import com.arjunvision.arjunfitai.data.health.model.NutritionRecord
import com.arjunvision.arjunfitai.data.health.model.WaterRecord
import com.arjunvision.arjunfitai.data.health.model.SleepRecord
import com.arjunvision.arjunfitai.data.health.model.SleepStageSegment
import com.arjunvision.arjunfitai.data.health.model.SleepStage
import com.arjunvision.arjunfitai.data.health.model.WeightRecord
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalDateGroup
import com.samsung.android.sdk.health.data.request.LocalDateGroupUnit
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.android.sdk.health.data.request.Ordering
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Read-only access to Samsung Health through the Samsung Health Data SDK. Only in the "samsung"
 * build, because the SDK can't be redistributed.
 *
 * Every call returns a [Result]; failures are always a HealthDataError. Nothing here
 * requests or uses WRITE access.
 */
class SamsungHealthSource(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemDefaultZone(),
) : HealthDataSource {
    private val appContext = context.applicationContext

    @Volatile
    private var store: HealthDataStore? = null

    override val kind = SourceKind.SAMSUNG

    override val supportedMetrics: Set<HealthMetric> = HealthMetric.entries.toSet()

    /** Installed or not; an old version or unfinished setup surfaces from the first check, with Samsung's own fix. */
    override suspend fun isAvailable(): Availability = try {
        if (Build.VERSION.SDK_INT >= 33) {
            appContext.packageManager.getPackageInfo(SAMSUNG_HEALTH_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            appContext.packageManager.getPackageInfo(SAMSUNG_HEALTH_PACKAGE, 0)
        }
        Availability.AVAILABLE
    } catch (e: PackageManager.NameNotFoundException) {
        Availability.NOT_INSTALLED
    }

    /**
     * Gets the [HealthDataStore]. This does not prove Samsung Health is installed or ready:
     * those problems surface from the first permission check or read.
     */
    override fun connect(): Result<Unit> = try {
        requireStore()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(samsungError(e))
    }

    /** The SDK has no disconnect call; dropping the store is enough. */
    override fun disconnect() {
        store = null
    }

    override suspend fun checkPermissions(): Result<PermissionStatus> = call {
        permissionStatus(requireStore().getGrantedPermissions(ALL_READ_PERMISSIONS))
    }

    /**
     * Shows Samsung Health's consent screen for any READ permissions not yet granted.
     * Runs on the caller's dispatcher because the SDK shows UI on [activity].
     */
    override suspend fun requestReadPermissions(activity: ComponentActivity): Result<PermissionStatus> = call(dispatcher = null) {
        val store = requireStore()
        val wanted = ALL_READ_PERMISSIONS
        val granted = store.getGrantedPermissions(wanted)
        permissionStatus(if (granted.containsAll(wanted)) granted else store.requestPermissions(wanted, activity))
    }

    /** Steps since local midnight. Steps are aggregate-only in the SDK. */
    override suspend fun getTodaySteps(): Result<Long> = call {
        val request = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(today())
            .build()
        requireStore().aggregateData(request).dataList.sumOf { it.value ?: 0L }
    }

    /** Active/total calories, active time and distance for one local day. */
    override suspend fun getActivitySummary(date: LocalDate): Result<ActivitySummary> = call {
        val store = requireStore()
        val filter = LocalTimeFilter.of(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
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
    override suspend fun getHeartRateHistory(days: Long): Result<List<HeartRateSample>> = call {
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
                // A record without series data is a single measurement.
                measurementCount = point.getValue(DataType.HeartRateType.SERIES_DATA)?.size?.takeIf { it > 0 } ?: 1,
            )
        }
    }

    /** Exercise sessions, newest first. One Samsung Health record can hold several sessions. */
    override suspend fun getExerciseHistory(days: Long): Result<List<ExerciseRecord>> = call {
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
    override suspend fun getSleepHistory(days: Long): Result<List<SleepRecord>> = call {
        val request = DataTypes.SLEEP.readDataRequestBuilder
            .setLocalTimeFilter(lastDays(days))
            .setOrdering(Ordering.DESC)
            .build()
        requireStore().readData(request).dataList.map { point ->
            val stages = point.getValue(DataType.SleepType.SESSIONS).orEmpty().flatMap { it.stages.orEmpty() }.sortedBy { it.startTime }
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
                stages = stages.map { s ->
                    SleepStageSegment(s.startTime, s.endTime, SleepStage.entries.firstOrNull { it.name == s.stage.name } ?: SleepStage.UNDEFINED)
                },
            )
        }
    }

    /** SpO2 readings, newest first. */
    override suspend fun getBloodOxygenHistory(days: Long): Result<List<BloodOxygenSample>> = call {
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
    override suspend fun getWeightHistory(days: Long): Result<List<WeightRecord>> = call {
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

    /**
     * Every heart-rate measurement between [from] and [to] (local time), oldest first. Samsung Health
     * batches measurements into records with SERIES_DATA; a record without a series counts as one point.
     */
    override suspend fun getHeartRateSeries(from: LocalDateTime, to: LocalDateTime): Result<List<HeartRatePoint>> = call {
        val request = DataTypes.HEART_RATE.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(from, to))
            .setOrdering(Ordering.ASC)
            .build()
        requireStore().readData(request).dataList.flatMap { point ->
            val series = point.getValue(DataType.HeartRateType.SERIES_DATA).orEmpty()
            if (series.isNotEmpty()) {
                series.map { HeartRatePoint(it.startTime, it.heartRate) }
            } else {
                listOfNotNull(point.getValue(DataType.HeartRateType.HEART_RATE)?.let { HeartRatePoint(point.startTime, it) })
            }
        }.sortedBy { it.time }
    }

    /** Daily lowest/highest heart rate for [from]..[to] inclusive (Samsung's own MIN/MAX aggregates). */
    override suspend fun getDailyHeartRateRange(from: LocalDate, to: LocalDate): Result<List<DailyRange>> = call {
        val store = requireStore()
        val filter = LocalDateFilter.of(from, to, true, true)
        val group = LocalDateGroup.of(LocalDateGroupUnit.DAILY, 1)
        val mins = store.aggregateData(
            DataType.HeartRateType.MIN.requestBuilder.setLocalDateFilterWithGroup(filter, group).build()
        ).dataList.mapNotNull { d -> d.value?.let { d.getStartLocalDateTime().toLocalDate() to it } }.toMap()
        val maxs = store.aggregateData(
            DataType.HeartRateType.MAX.requestBuilder.setLocalDateFilterWithGroup(filter, group).build()
        ).dataList.mapNotNull { d -> d.value?.let { d.getStartLocalDateTime().toLocalDate() to it } }.toMap()
        (mins.keys intersect maxs.keys).sorted().map { DailyRange(it, mins.getValue(it), maxs.getValue(it)) }
    }

    /** Step totals per local day for [from]..[to] inclusive. Days without steps are absent. */
    override suspend fun getStepsHistory(from: LocalDate, to: LocalDate): Result<Map<LocalDate, Long>> = call {
        val request = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilterWithGroup(
                LocalTimeFilter.of(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                LocalTimeGroup.of(LocalTimeGroupUnit.DAILY, 1),
            )
            .setOrdering(Ordering.ASC)
            .build()
        requireStore().aggregateData(request).dataList
            .mapNotNull { d -> d.value?.let { d.getStartLocalDateTime().toLocalDate() to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, v) -> v.sum() }
    }

    /** Food and meals logged in Samsung Health on [date], oldest first. */
    override suspend fun getNutrition(date: LocalDate): Result<List<NutritionRecord>> = call {
        val request = DataTypes.NUTRITION.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
            .setOrdering(Ordering.ASC)
            .build()
        requireStore().readData(request).dataList.map { point ->
            NutritionRecord(
                time = point.startTime,
                title = point.getValue(DataType.NutritionType.TITLE),
                kcal = point.getValue(DataType.NutritionType.CALORIES),
                proteinG = point.getValue(DataType.NutritionType.PROTEIN),
                carbsG = point.getValue(DataType.NutritionType.CARBOHYDRATE),
                fatG = point.getValue(DataType.NutritionType.TOTAL_FAT),
            )
        }
    }

    /** Water logged in Samsung Health on [date], oldest first (ml). */
    override suspend fun getWaterIntake(date: LocalDate): Result<List<WaterRecord>> = call {
        val request = DataTypes.WATER_INTAKE.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
            .setOrdering(Ordering.ASC)
            .build()
        requireStore().readData(request).dataList.mapNotNull { point ->
            point.getValue(DataType.WaterIntakeType.AMOUNT)?.let { WaterRecord(point.startTime, it) }
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

    /** Runs [block] on [dispatcher] (or the caller's context when null), mapping failures with [samsungError]. */
    private suspend fun <T> call(
        dispatcher: CoroutineDispatcher? = ioDispatcher,
        block: suspend () -> T,
    ): Result<T> = try {
        Result.success(if (dispatcher == null) block() else withContext(dispatcher) { block() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(samsungError(e))
    }

    private companion object {
        /** Also declared in the manifest's <queries>, so the package check works on Android 11+. */
        const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

        /** The Samsung data type behind each metric. */
        val DATA_TYPES: Map<HealthMetric, DataType> = mapOf(
            HealthMetric.STEPS to DataTypes.STEPS,
            HealthMetric.HEART_RATE to DataTypes.HEART_RATE,
            HealthMetric.EXERCISE to DataTypes.EXERCISE,
            HealthMetric.SLEEP to DataTypes.SLEEP,
            HealthMetric.BLOOD_OXYGEN to DataTypes.BLOOD_OXYGEN,
            HealthMetric.BODY_COMPOSITION to DataTypes.BODY_COMPOSITION,
            HealthMetric.ACTIVITY_SUMMARY to DataTypes.ACTIVITY_SUMMARY,
            HealthMetric.NUTRITION to DataTypes.NUTRITION,
            HealthMetric.WATER_INTAKE to DataTypes.WATER_INTAKE,
        )

        /** READ only — ArjunFit never asks Samsung Health for WRITE access. */
        val READ_PERMISSIONS: Map<HealthMetric, Permission> =
            DATA_TYPES.mapValues { (_, type) -> Permission.of(type, AccessType.READ) }

        val ALL_READ_PERMISSIONS: Set<Permission> = READ_PERMISSIONS.values.toSet()

        fun permissionStatus(granted: Set<Permission>) = PermissionStatus(
            granted = READ_PERMISSIONS.filterValues { it in granted }.keys,
            supported = READ_PERMISSIONS.keys,
        )
    }
}
