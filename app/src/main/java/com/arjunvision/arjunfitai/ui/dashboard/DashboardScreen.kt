package com.arjunvision.arjunfitai.ui.dashboard

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arjunvision.arjunfitai.data.samsung.model.SleepStage
import com.arjunvision.arjunfitai.ui.components.AfIcons
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Samsung Health data") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AfIcons.Back, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    ConnectionCard(
                        state = state,
                        onGrant = { activity?.let(viewModel::requestPermissions) },
                        onResolve = { activity?.let(viewModel::resolveError) },
                        onRetry = viewModel::start,
                    )
                }
                if (state.status == DashboardUiState.Status.READY) {
                    val data = state.data
                    item { TodayCard(data) }
                    item { HeartRateCard(data) }
                    item { SleepCard(data) }
                    item { ExerciseCard(data) }
                    item { BloodOxygenCard(data) }
                    item { WeightCard(data) }
                    if (data.errors.isNotEmpty()) {
                        item {
                            SectionCard("Some data couldn't be read") {
                                data.errors.forEach { (metric, message) -> Text("${metric.label}: $message") }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Read-only access to Samsung Health. ArjunFit never writes to it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    state: DashboardUiState,
    onGrant: () -> Unit,
    onResolve: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state.status) {
        DashboardUiState.Status.CONNECTING -> SectionCard("Samsung Health") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(12.dp))
                Text("Connecting…")
            }
        }
        DashboardUiState.Status.ERROR -> SectionCard("Samsung Health unavailable") {
            Text(state.error?.message ?: "Something went wrong.")
            state.error?.errorCode?.let {
                Text("Error code $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.error?.canResolve == true) Button(onClick = onResolve) { Text("Fix in Samsung Health") }
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
        }
        DashboardUiState.Status.NEEDS_PERMISSION -> SectionCard("Connect Samsung Health") {
            Text("Allow ArjunFit to read your steps, heart rate, exercise, sleep, blood oxygen, weight and activity summary.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onGrant) { Text("Allow read access") }
        }
        DashboardUiState.Status.READY -> {
            val missing = state.permissions?.missing.orEmpty()
            if (missing.isNotEmpty()) {
                SectionCard("Some data not shared") {
                    Text("Not allowed: " + missing.joinToString { it.label })
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onGrant) { Text("Review permissions") }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(data: HealthSnapshot) = SectionCard("Today") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("Steps", data.steps?.let { "%,d".format(it) })
        Stat("Active kcal", data.activity?.activeCaloriesKcal?.let { "%.0f".format(it) })
        Stat("Total kcal", data.activity?.totalCaloriesKcal?.let { "%.0f".format(it) })
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("Active time", data.activity?.activeTime?.let(::formatDuration))
        Stat("Distance", data.activity?.distanceMeters?.let { "%.2f km".format(it / 1000f) })
        Spacer(Modifier.size(1.dp))
    }
}

@Composable
private fun HeartRateCard(data: HealthSnapshot) = SectionCard("Heart rate · 7 days") {
    val latest = data.heartRate.firstOrNull()
    if (latest == null) {
        Empty()
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Latest", "%.0f bpm".format(latest.bpm))
            Stat("Lowest", "%.0f".format(data.heartRate.minOf { it.minBpm ?: it.bpm }))
            Stat("Highest", "%.0f".format(data.heartRate.maxOf { it.maxBpm ?: it.bpm }))
        }
        val measurements = data.heartRate.sumOf { it.measurementCount }
        Caption("Latest at ${formatTime(latest.time)} · $measurements ${if (measurements == 1) "measurement" else "measurements"}")
    }
}

@Composable
private fun SleepCard(data: HealthSnapshot) = SectionCard("Sleep · 7 days") {
    if (data.sleep.isEmpty()) Empty()
    data.sleep.take(3).forEachIndexed { index, night ->
        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
        val total = night.duration ?: night.end?.let { Duration.between(night.start, it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDate(night.start), fontWeight = FontWeight.Medium)
            Text(listOfNotNull(total?.let(::formatDuration), night.score?.let { "score $it" }).joinToString(" · "))
        }
        val stages = listOf(SleepStage.DEEP, SleepStage.LIGHT, SleepStage.REM, SleepStage.AWAKE)
            .mapNotNull { stage -> night.stageDurations[stage]?.let { "${stage.name.lowercase()} ${formatDuration(it)}" } }
        if (stages.isNotEmpty()) Caption(stages.joinToString(" · "))
    }
}

@Composable
private fun ExerciseCard(data: HealthSnapshot) = SectionCard("Exercise · 30 days") {
    if (data.exercise.isEmpty()) Empty()
    data.exercise.take(5).forEachIndexed { index, session ->
        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(session.title ?: prettyEnum(session.type), fontWeight = FontWeight.Medium)
            Text(formatDuration(session.duration))
        }
        Caption(
            listOfNotNull(
                formatDate(session.start),
                "%.0f kcal".format(session.caloriesKcal),
                session.distanceMeters?.takeIf { it > 0f }?.let { "%.2f km".format(it / 1000f) },
                session.meanHeartRate?.let { "avg %.0f bpm".format(it) },
            ).joinToString(" · ")
        )
    }
}

@Composable
private fun BloodOxygenCard(data: HealthSnapshot) = SectionCard("Blood oxygen · 7 days") {
    val latest = data.bloodOxygen.firstOrNull()
    if (latest == null) {
        Empty()
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Latest", "%.0f%%".format(latest.percent))
            Stat("Average", "%.0f%%".format(data.bloodOxygen.map { it.percent }.average()))
            Stat("Lowest", "%.0f%%".format(data.bloodOxygen.minOf { it.minPercent ?: it.percent }))
        }
        Caption("Latest at ${formatTime(latest.time)}")
    }
}

@Composable
private fun WeightCard(data: HealthSnapshot) = SectionCard("Weight · since ${formatDate(data.weight.lastOrNull()?.time ?: Instant.now())}") {
    val latest = data.weight.firstOrNull()
    if (latest == null) {
        Empty()
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Weight", "%.1f kg".format(latest.weightKg))
            Stat("Body fat", latest.bodyFatPercent?.let { "%.1f%%".format(it) })
            Stat("BMI", latest.bmi?.let { "%.1f".format(it) })
        }
        val oldest = data.weight.last()
        val change = latest.weightKg - oldest.weightKg
        Caption(
            "Measured ${formatDate(latest.time)}" +
                if (data.weight.size > 1) " · %+.1f kg since %s".format(change, formatDate(oldest.time)) else ""
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Stat(label: String, value: String?) {
    Column {
        Text(value ?: "—", style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun Empty() = Caption("No data in Samsung Health for this period.")

private val timeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

private fun formatTime(instant: Instant) = timeFormatter.format(instant.atZone(ZoneId.systemDefault()))
private fun formatDate(instant: Instant) = dateFormatter.format(instant.atZone(ZoneId.systemDefault()))

/** "45s" under a minute, "12m" under an hour, otherwise "1h 05m". */
private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    return when {
        hours > 0 -> "${hours}h %02dm".format(minutes)
        minutes > 0 -> "${minutes}m"
        else -> "${duration.toSecondsPart()}s"
    }
}

/** "TRACK_RUNNING" → "Track running". */
private fun prettyEnum(name: String) = name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
