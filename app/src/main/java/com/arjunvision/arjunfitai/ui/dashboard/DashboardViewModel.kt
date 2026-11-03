package com.arjunvision.arjunfitai.ui.dashboard

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjunvision.arjunfitai.data.health.HealthDataError
import com.arjunvision.arjunfitai.data.health.HealthMetric
import com.arjunvision.arjunfitai.data.health.HealthSources
import com.arjunvision.arjunfitai.data.health.PermissionStatus
import com.arjunvision.arjunfitai.data.health.SourceKind
import com.arjunvision.arjunfitai.data.health.SourcePreference
import com.arjunvision.arjunfitai.data.health.model.ActivitySummary
import com.arjunvision.arjunfitai.data.health.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.health.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.health.model.HeartRateSample
import com.arjunvision.arjunfitai.data.health.model.NutritionRecord
import com.arjunvision.arjunfitai.data.health.model.WaterRecord
import com.arjunvision.arjunfitai.data.health.model.SleepRecord
import com.arjunvision.arjunfitai.data.health.model.WeightRecord
import com.arjunvision.arjunfitai.ui.home.WinterArc
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthSnapshot(
    val steps: Long? = null,
    val activity: ActivitySummary? = null,
    val heartRate: List<HeartRateSample> = emptyList(),
    val exercise: List<ExerciseRecord> = emptyList(),
    val sleep: List<SleepRecord> = emptyList(),
    val bloodOxygen: List<BloodOxygenSample> = emptyList(),
    val weight: List<WeightRecord> = emptyList(),
    val nutritionToday: List<NutritionRecord> = emptyList(),
    val waterToday: List<WaterRecord> = emptyList(),
    /** Per-metric read failures, so one bad data type doesn't blank the whole dashboard. */
    val errors: Map<HealthMetric, String> = emptyMap(),
)

data class DashboardUiState(
    val status: Status = Status.CONNECTING,
    /** The source being read; null until one is chosen. */
    val source: SourceKind? = null,
    val permissions: PermissionStatus? = null,
    val error: HealthDataError? = null,
    val isRefreshing: Boolean = false,
    val data: HealthSnapshot = HealthSnapshot(),
    /** When the last read from the source finished. */
    val lastUpdated: Instant? = null,
) {
    enum class Status { CONNECTING, NEEDS_PERMISSION, READY, ERROR }
}

class DashboardViewModel(private val sources: HealthSources) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        start()
    }

    /** Picks the source, checks which READ permissions are granted, and loads what we're allowed to read. */
    fun start() {
        viewModelScope.launch {
            _state.update { it.copy(status = DashboardUiState.Status.CONNECTING, error = null) }
            sources.choose(SourcePreference.AUTO, previous = _state.value.source)
            val source = sources.current.value ?: return@launch fail(HealthSources.noSource())
            _state.update { it.copy(source = source.kind) }
            source.connect().onFailure {
                fail(it)
                return@launch
            }
            source.checkPermissions().fold(onSuccess = { onPermissions(it) }, onFailure = ::fail)
        }
    }

    /** Shows the source's consent screen; [onDone] runs once the answer is known, before data loads. */
    fun requestPermissions(activity: Activity, onDone: () -> Unit = {}) {
        val source = sources.current.value
        val host = activity as? ComponentActivity
        if (source == null || host == null) {
            fail(HealthSources.noSource())
            onDone()
            return
        }
        viewModelScope.launch {
            source.requestReadPermissions(host).fold(
                onSuccess = { onPermissions(it, onDone) },
                onFailure = {
                    fail(it)
                    onDone()
                },
            )
        }
    }

    /** Opens the source's own fix-up flow; [onResume] re-checks when the user comes back. */
    fun resolveError(activity: Activity) {
        _state.value.error?.resolve(activity)
    }

    fun onResume() {
        if (_state.value.status == DashboardUiState.Status.ERROR) start()
    }

    fun refresh() {
        val permissions = _state.value.permissions ?: return start()
        viewModelScope.launch { load(permissions) }
    }

    private suspend fun onPermissions(permissions: PermissionStatus, onUpdated: () -> Unit = {}) {
        _state.update {
            it.copy(
                permissions = permissions,
                error = null,
                status = if (permissions.noneGranted) DashboardUiState.Status.NEEDS_PERMISSION else DashboardUiState.Status.READY,
            )
        }
        onUpdated()
        if (!permissions.noneGranted) load(permissions)
    }

    private suspend fun load(permissions: PermissionStatus) {
        val source = sources.current.value ?: return fail(HealthSources.noSource())
        _state.update { it.copy(isRefreshing = true) }
        val granted = permissions.granted
        val errors = mutableMapOf<HealthMetric, String>()

        fun <T> Result<T>?.valueOr(metric: HealthMetric, default: T): T {
            if (this == null) return default
            return getOrElse { e ->
                errors[metric] = e.message ?: "Could not read ${metric.label.lowercase()}"
                default
            }
        }

        val today = LocalDate.now()
        val snapshot = coroutineScope {
            val steps = async { if (HealthMetric.STEPS in granted) source.getTodaySteps() else null }
            val activity = async { if (HealthMetric.ACTIVITY_SUMMARY in granted) source.getActivitySummary(today) else null }
            val heartRate = async { if (HealthMetric.HEART_RATE in granted) source.getHeartRateHistory() else null }
            val exercise = async { if (HealthMetric.EXERCISE in granted) source.getExerciseHistory() else null }
            val sleep = async { if (HealthMetric.SLEEP in granted) source.getSleepHistory() else null }
            val oxygen = async { if (HealthMetric.BLOOD_OXYGEN in granted) source.getBloodOxygenHistory() else null }
            val weight = async { if (HealthMetric.BODY_COMPOSITION in granted) source.getWeightHistory(weightHistoryDays()) else null }
            val nutrition = async { if (HealthMetric.NUTRITION in granted) source.getNutrition(today) else null }
            val water = async { if (HealthMetric.WATER_INTAKE in granted) source.getWaterIntake(today) else null }
            HealthSnapshot(
                steps = steps.await().valueOr(HealthMetric.STEPS, null),
                activity = activity.await().valueOr(HealthMetric.ACTIVITY_SUMMARY, null),
                heartRate = heartRate.await().valueOr(HealthMetric.HEART_RATE, emptyList()),
                exercise = exercise.await().valueOr(HealthMetric.EXERCISE, emptyList()),
                sleep = sleep.await().valueOr(HealthMetric.SLEEP, emptyList()),
                bloodOxygen = oxygen.await().valueOr(HealthMetric.BLOOD_OXYGEN, emptyList()),
                weight = weight.await().valueOr(HealthMetric.BODY_COMPOSITION, emptyList()),
                nutritionToday = nutrition.await().valueOr(HealthMetric.NUTRITION, emptyList()),
                waterToday = water.await().valueOr(HealthMetric.WATER_INTAKE, emptyList()),
                errors = errors,
            )
        }
        _state.update { it.copy(isRefreshing = false, data = snapshot, lastUpdated = Instant.now()) }
    }

    /** Back to 60 days before this year's Winter Arc start, so the weight card has a baseline; at least 90 days. */
    private fun weightHistoryDays(): Long {
        val today = LocalDate.now()
        val from = WinterArc.start(today.year).minusDays(60)
        return maxOf(90L, ChronoUnit.DAYS.between(from, today))
    }

    private fun fail(throwable: Throwable) {
        _state.update {
            it.copy(
                status = DashboardUiState.Status.ERROR,
                error = HealthDataError.from(throwable, it.source),
                isRefreshing = false,
            )
        }
    }
}
