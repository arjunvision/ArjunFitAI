package com.arjunvision.arjunfitai.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.R
import com.arjunvision.arjunfitai.data.settings.UserSettings
import com.arjunvision.arjunfitai.ui.components.AfAccentPill
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfCircleButton
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfRing
import com.arjunvision.arjunfitai.ui.components.Wordmark
import com.arjunvision.arjunfitai.ui.components.rememberHeartBeatScale
import com.arjunvision.arjunfitai.ui.components.rememberPulseAlpha
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.dashboard.DashboardViewModel
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfEyebrow
import com.arjunvision.arjunfitai.ui.theme.AfScript
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/** Where Home's taps go. */
class HomeActions(
    val onSteps: () -> Unit,
    val onHeartRate: () -> Unit,
    val onSleep: () -> Unit,
    val onOxygen: () -> Unit,
    val onWeight: () -> Unit,
    val onCoach: () -> Unit,
    val onProfile: () -> Unit,
    val onHealthSync: () -> Unit,
)

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    state: DashboardUiState,
    settings: UserSettings,
    /** Samsung Health + ArjunFit weigh-ins. */
    weights: List<com.arjunvision.arjunfitai.data.samsung.model.WeightRecord>,
    actions: HomeActions,
    contentPadding: PaddingValues,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val data = state.data
    val activity = LocalActivity.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(onCoach = actions.onCoach, onProfile = actions.onProfile)

        Column {
            Text(
                "${dateLabel(today)} · ${WinterArc.label(today)}",
                style = AfEyebrow.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = AfColors.TextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    greeting(LocalTime.now(zone), settings.name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AfColors.Text,
                    modifier = Modifier.alignByBaseline().weight(1f, fill = false),
                )
                Text("Let's lift", style = AfScript, modifier = Modifier.alignByBaseline())
            }
        }

        SourceChip(state = state, latest = latestReading(data), today = today, zone = zone, onClick = actions.onHealthSync)

        when (state.status) {
            DashboardUiState.Status.NEEDS_PERMISSION -> NoticeCard(
                title = "Connect Samsung Health",
                body = "Allow ArjunFit to read your steps, heart rate, sleep, blood oxygen, weight, activity, food and water from Samsung Health.",
                primary = "Allow read access" to { activity?.let(viewModel::requestPermissions) },
            )
            DashboardUiState.Status.ERROR -> NoticeCard(
                title = "Samsung Health unavailable",
                body = state.error?.message ?: "Something went wrong.",
                primary = if (state.error?.canResolve == true) "Fix in Samsung Health" to { activity?.let(viewModel::resolveError) } else null,
                secondary = "Retry" to viewModel::start,
            )
            else -> Unit
        }

        StepsCard(
            steps = data.steps,
            goal = settings.stepGoal,
            distance = data.activity?.distanceMeters?.let(::formatKm),
            totalKcal = data.activity?.totalCaloriesKcal?.let(::formatKcal),
            activeTime = data.activity?.activeTime?.let(::formatActiveTime),
            onClick = actions.onSteps,
            modifier = Modifier.riseIn(1),
        )

        VitalsGrid(
            heartRate = data.heartRate.firstOrNull()?.bpm?.roundToInt()?.toString(),
            activeKcal = data.activity?.activeCaloriesKcal?.let(::formatKcal),
            sleep = lastNightSleep(data.sleep, Instant.now())?.let(::sleepDuration)?.let(::formatSleep),
            spo2 = data.bloodOxygen.firstOrNull()?.percent?.let { "${it.roundToInt()}%" },
            actions = actions,
            modifier = Modifier.riseIn(2),
        )

        val arcStart = WinterArc.start(today.year)
        WeightCard(
            progress = weightProgress(weights, settings.targetWeightKg, arcStart, zone),
            targetKg = settings.targetWeightKg,
            arcStart = arcStart,
            onClick = actions.onWeight,
            modifier = Modifier.riseIn(3),
        )
    }
}

@Composable
private fun Header(onCoach: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                painterResource(R.drawable.arjunfit_logo),
                contentDescription = "ArjunFit logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 42.dp, height = 38.dp),
            )
            Wordmark(style = MaterialTheme.typography.headlineSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AfCircleButton(AfIcons.Coach, "AI coach", AfColors.AccentSoft, onCoach)
            AfCircleButton(AfIcons.Profile, "Profile and settings", AfColors.Text, onProfile)
        }
    }
}

@Composable
private fun SourceChip(state: DashboardUiState, latest: Instant?, today: LocalDate, zone: ZoneId, onClick: () -> Unit) {
    val pulse by rememberPulseAlpha()
    val (text, dot) = when (state.status) {
        DashboardUiState.Status.CONNECTING -> "Connecting to Samsung Health…" to AfColors.TextMuted
        DashboardUiState.Status.READY -> when {
            latest != null -> "Galaxy Fit3 · latest reading ${formatReadingTime(latest, today, zone)}" to AfColors.Success
            state.isRefreshing -> "Galaxy Fit3 · reading Samsung Health…" to AfColors.Success
            else -> "Galaxy Fit3 · no readings yet" to AfColors.TextMuted
        }
        else -> "Samsung Health not connected" to AfColors.AccentSoft
    }
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(AfShapes.Pill)
            .background(AfColors.Card)
            .clickable(role = Role.Button, onClickLabel = "Health sync", onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).alpha(if (dot == AfColors.Success) pulse else 1f).clip(AfShapes.Pill).background(dot))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun NoticeCard(title: String, body: String, primary: Pair<String, () -> Unit>?, secondary: Pair<String, () -> Unit>? = null) {
    AfCard(Modifier.fillMaxWidth().riseIn(0)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = AfColors.Text)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            primary?.let { (label, onClick) ->
                Box(
                    Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Accent)
                        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = MaterialTheme.typography.labelLarge, color = AfColors.OnAccent) }
            }
            secondary?.let { (label, onClick) ->
                Box(
                    Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Inner)
                        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = MaterialTheme.typography.labelLarge, color = AfColors.Text) }
            }
        }
    }
}

@Composable
private fun StepsCard(
    steps: Long?,
    goal: Int,
    distance: String?,
    totalKcal: String?,
    activeTime: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (steps != null && goal > 0) steps.toFloat() / goal else 0f
    val description = steps?.let { "${formatSteps(it)} of ${formatSteps(goal.toLong())} steps" } ?: "Steps not available yet"
    AfCard(modifier.fillMaxWidth(), onClick = onClick, onClickLabel = "Open steps") {
        Box(Modifier.align(Alignment.CenterHorizontally).size(200.dp).semantics { contentDescription = description }) {
            AfRing(fraction = fraction, size = 200.dp, strokeWidth = 14.dp, minSweepDegrees = if (steps != null) 2f else 0f)
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AfIcons.Steps, contentDescription = null, tint = AfColors.Accent, modifier = Modifier.size(26.dp))
                Text(steps?.let(::formatSteps) ?: EMPTY, style = MaterialTheme.typography.displayLarge, color = AfColors.Text)
                Text("Steps", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                Text("Goal ${formatSteps(goal.toLong())}", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
            }
            if (steps != null && goal > 0) {
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = (-2).dp, y = (-18).dp)
                        .clip(AfShapes.Pill).background(AfColors.Accent).padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("${(steps * 100 / goal)}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = AfColors.OnAccent)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("Distance", distance, Modifier.weight(1f))
            MiniStat("Total kcal", totalKcal, Modifier.weight(1f))
            MiniStat("Active time", activeTime, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value ?: EMPTY, style = MaterialTheme.typography.titleMedium, color = AfColors.Text, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun VitalsGrid(heartRate: String?, activeKcal: String?, sleep: String?, spo2: String?, actions: HomeActions, modifier: Modifier) {
    val beat by rememberHeartBeatScale()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VitalTile(AfIcons.Heart, AfColors.AccentSoft, heartRate, "bpm", "Heart rate", actions.onHeartRate, Modifier.weight(1f), iconScale = if (heartRate != null) beat else 1f)
            VitalTile(AfIcons.Flame, AfColors.AccentSoft, activeKcal, "kcal", "Active calories", actions.onSteps, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VitalTile(AfIcons.Moon, AfColors.AccentSoft, sleep, null, "Sleep", actions.onSleep, Modifier.weight(1f))
            VitalTile(AfIcons.Drop, AfColors.Water, spo2, null, "Blood oxygen", actions.onOxygen, Modifier.weight(1f))
        }
    }
}

@Composable
private fun VitalTile(
    icon: ImageVector,
    iconTint: Color,
    value: String?,
    unit: String?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    iconScale: Float = 1f,
) {
    AfCard(modifier, onClick = onClick, onClickLabel = "Open ${label.lowercase(Locale.ENGLISH)}", padding = PaddingValues(12.dp), shape = AfShapes.Tile) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(AfShapes.Inner).background(AfColors.Inner), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp).scale(iconScale))
            }
            Column {
                Text(
                    buildAnnotatedString {
                        append(value ?: EMPTY)
                        if (value != null && unit != null) {
                            withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)) { append(" $unit") }
                        }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 20.sp),
                    color = AfColors.Text,
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
            }
        }
    }
}

@Composable
private fun WeightCard(progress: WeightProgress?, targetKg: Float, arcStart: LocalDate, onClick: () -> Unit, modifier: Modifier) {
    AfCard(modifier.fillMaxWidth(), onClick = onClick, onClickLabel = "Open weight tracker", padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("WINTER ARC WEIGHT", style = AfEyebrow, color = AfColors.TextMuted)
                Text(
                    buildAnnotatedString {
                        append(progress?.let { "%.1f kg".format(Locale.ENGLISH, it.currentKg) } ?: EMPTY)
                        withStyle(SpanStyle(color = AfColors.TextMuted, fontWeight = FontWeight.Medium)) { append(" → ") }
                        withStyle(SpanStyle(color = AfColors.Accent)) { append(formatTarget(targetKg)) }
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AfColors.Text,
                )
                Text(weightNote(progress, arcStart), style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
            }
            AfAccentPill("Log", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(64.dp))
        }
    }
}

private fun formatTarget(kg: Float): String =
    if (kg % 1f == 0f) "${kg.toInt()} kg" else "%.1f kg".format(Locale.ENGLISH, kg)
