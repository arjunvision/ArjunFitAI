package com.arjunvision.arjunfitai.ui.dashboard

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjunvision.arjunfitai.data.samsung.HealthMetric
import com.arjunvision.arjunfitai.data.samsung.PermissionStatus
import com.arjunvision.arjunfitai.data.samsung.SamsungHealthError
import com.arjunvision.arjunfitai.data.samsung.SamsungHealthRepository
import com.arjunvision.arjunfitai.data.samsung.model.ActivitySummary
import com.arjunvision.arjunfitai.data.samsung.model.BloodOxygenSample
import com.arjunvision.arjunfitai.data.samsung.model.ExerciseRecord
import com.arjunvision.arjunfitai.data.samsung.model.HeartRateSample
import com.arjunvision.arjunfitai.data.samsung.model.SleepRecord
import com.arjunvision.arjunfitai.data.samsung.model.WeightRecord
import com.arjunvision.arjunfitai.ui.home.WinterArc
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
    /** Per-metric read failures, so one bad data type doesn't blank the whole dashboard. */
    val errors: Map<HealthMetric, String> = emptyMap(),
)

data class DashboardUiState(
    val status: Status = Status.CONNECTING,
    val permissions: PermissionStatus? = null,
    val error: SamsungHealthError? = null,
    val isRefreshing: Boolean = false,
    val data: HealthSnapshot = HealthSnapshot(),
) {
    enum class Status { CONNECTING, NEEDS_PERMISSION, READY, ERROR }
}

class DashboardViewModel(private val repository: SamsungHealthRepository) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        start()
    }

    /** Connects, checks which READ permissions are granted, and loads what we're allowed to read. */
    fun start() {
        viewModelScope.launch {
            _state.update { it.copy(status = DashboardUiState.Status.CONNECTING, error = null) }
            repository.connectToSamsungHealth().onFailure {
                fail(it)
                return@launch
            }
            repository.checkPermissions().fold(onSuccess = { onPermissions(it) }, onFailure = ::fail)
        }
    }

    fun requestPermissions(activity: Activity) {
        viewModelScope.launch {
            repository.requestReadPermissions(activity).fold(onSuccess = { onPermissions(it) }, onFailure = ::fail)
        }
    }

    /** Opens Samsung Health's fix-up flow; [onResume] re-checks when the user comes back. */
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

    private suspend fun onPermissions(permissions: PermissionStatus) {
        _state.update {
            it.copy(
                permissions = permissions,
                error = null,
                status = if (permissions.noneGranted) DashboardUiState.Status.NEEDS_PERMISSION else DashboardUiState.Status.READY,
            )
        }
        if (!permissions.noneGranted) load(permissions)
    }

    private suspend fun load(permissions: PermissionStatus) {
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

        val snapshot = coroutineScope {
            val steps = async { if (HealthMetric.STEPS in granted) repository.getTodaySteps() else null }
            val activity = async { if (HealthMetric.ACTIVITY_SUMMARY in granted) repository.getTodayActivitySummary() else null }
            val heartRate = async { if (HealthMetric.HEART_RATE in granted) repository.getHeartRateHistory() else null }
            val exercise = async { if (HealthMetric.EXERCISE in granted) repository.getExerciseHistory() else null }
            val sleep = async { if (HealthMetric.SLEEP in granted) repository.getSleepHistory() else null }
            val oxygen = async { if (HealthMetric.BLOOD_OXYGEN in granted) repository.getBloodOxygenHistory() else null }
            val weight = async { if (HealthMetric.BODY_COMPOSITION in granted) repository.getWeightHistory(weightHistoryDays()) else null }
            HealthSnapshot(
                steps = steps.await().valueOr(HealthMetric.STEPS, null),
                activity = activity.await().valueOr(HealthMetric.ACTIVITY_SUMMARY, null),
                heartRate = heartRate.await().valueOr(HealthMetric.HEART_RATE, emptyList()),
                exercise = exercise.await().valueOr(HealthMetric.EXERCISE, emptyList()),
                sleep = sleep.await().valueOr(HealthMetric.SLEEP, emptyList()),
                bloodOxygen = oxygen.await().valueOr(HealthMetric.BLOOD_OXYGEN, emptyList()),
                weight = weight.await().valueOr(HealthMetric.BODY_COMPOSITION, emptyList()),
                errors = errors,
            )
        }
        _state.update { it.copy(isRefreshing = false, data = snapshot) }
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
                error = SamsungHealthError.from(throwable),
                isRefreshing = false,
            )
        }
    }
}
