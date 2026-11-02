package com.arjunvision.arjunfitai.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arjunvision.arjunfitai.data.workout.Exercise
import com.arjunvision.arjunfitai.data.workout.Measure
import com.arjunvision.arjunfitai.data.workout.PlanItemEntity
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import com.arjunvision.arjunfitai.data.workout.titleCase
import com.arjunvision.arjunfitai.ui.components.AfCircleButton
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfRing
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.rememberPulseAlpha
import com.arjunvision.arjunfitai.ui.detail.DetailColumn
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import kotlinx.coroutines.launch
import java.util.Locale

class LiveActions(
    val onBack: () -> Unit,
    val onUpdate: suspend (SetEntity) -> Unit,
    val onAddSet: suspend (SetEntity) -> Unit,
    val onFinish: suspend (anyDone: Boolean) -> Unit,
    val onHowTo: (Exercise) -> Unit,
)

@Composable
fun LiveWorkoutScreen(
    session: SessionEntity,
    sessionSets: List<SetEntity>,
    planItems: List<PlanItemEntity>,
    allSets: List<SetEntity>,
    allSessions: List<SessionEntity>,
    library: Map<String, Exercise>,
    timer: RestTimer,
    contentPadding: PaddingValues,
    actions: LiveActions,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    // Keep the screen on during a workout so the rest timer and buzz stay on time.
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    var notificationsAllowed by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsAllowed = it }
    LaunchedEffect(Unit) {
        RestTimer.ensureChannel(context)
        if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val positions = sessionSets.map { it.position }.distinct().sorted()
    var index by rememberSaveable(session.id) { mutableIntStateOf(0) }
    val position = positions.getOrNull(index.coerceIn(0, (positions.size - 1).coerceAtLeast(0)))
    val current = sessionSets.filter { it.position == position }.sortedBy { it.setIndex }
    val first = current.firstOrNull()
    val exercise = first?.exerciseId?.let(library::get)
    val plannedTarget = planItems.firstOrNull { it.position == position && it.name == first?.exerciseName }?.target
    val doneCount = sessionSets.count { it.done }
    var finishing by rememberSaveable { mutableStateOf(false) }

    DetailColumn(contentPadding) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AfCircleButton(AfIcons.Back, "Back to workouts (keeps this workout open)", AfColors.Text, actions.onBack)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${session.planName.uppercase(Locale.ENGLISH)} · ${index + 1} OF ${positions.size}", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                Text("$doneCount of ${sessionSets.size} sets done", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.Text)
            }
            Box(
                Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Card).clickable(role = Role.Button) { finishing = true }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Finish", style = MaterialTheme.typography.labelLarge, color = AfColors.Text) }
        }

        if (first == null) {
            Text("This workout has no exercises.", style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted)
            return@DetailColumn
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("START POSITION", "END POSITION").forEachIndexed { i, label ->
                ExercisePhoto(exercise?.images?.getOrNull(i), "${first.exerciseName}, ${label.lowercase(Locale.ENGLISH)}", Modifier.weight(1f).height(120.dp).clip(AfShapes.Card), label = label)
            }
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(first.exerciseName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = AfColors.Text)
                exercise?.let { e ->
                    Text((e.primary + e.secondary).distinct().take(3).joinToString(" · ") { it.titleCase() }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = Color.White.copy(alpha = 0.6f))
                }
            }
            exercise?.let { e ->
                Text("How to do it", style = MaterialTheme.typography.labelLarge, color = AfColors.AccentSoft,
                    modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { actions.onHowTo(e) }.padding(vertical = 14.dp))
            }
        }

        Column(Modifier.fillMaxWidth().clip(AfShapes.Card).background(AfColors.Card).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val unit = first.unit
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Header("SET", Modifier.width(34.dp))
                Header("PREVIOUS", Modifier.weight(1f))
                Header(if (unit == Measure.REPS) "KG" else "", Modifier.width(64.dp))
                Header(unit.short.uppercase(Locale.ENGLISH), Modifier.width(64.dp))
                Box(Modifier.width(48.dp))
            }
            current.forEach { set ->
                SetRow(
                    set = set,
                    previous = previousSet(allSets, allSessions, set.exerciseName, set.setIndex, session.id),
                    target = plannedTarget,
                    onChange = { scope.launch { actions.onUpdate(it) } },
                    onToggle = { updated ->
                        scope.launch { actions.onUpdate(updated) }
                        if (updated.done) {
                            val nextSet = current.firstOrNull { it.setIndex > set.setIndex && !it.done }
                            val nextLabel = when {
                                nextSet != null -> "Set ${nextSet.setIndex} of ${set.exerciseName}"
                                index + 1 < positions.size -> "Next: " + (sessionSets.firstOrNull { it.position == positions[index + 1] }?.exerciseName ?: "next exercise")
                                else -> "Last set done — tap Finish"
                            }
                            timer.start(RestTimer.DEFAULT_SECONDS, nextLabel)
                        }
                    },
                )
            }
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(AfShapes.Inner).border(1.dp, AfColors.Border, AfShapes.Inner)
                    .clickable(role = Role.Button) { current.lastOrNull()?.let { last -> scope.launch { actions.onAddSet(last) } } },
                contentAlignment = Alignment.Center,
            ) { Text("+ Add set", style = MaterialTheme.typography.labelLarge, color = AfColors.AccentSoft) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavPill("‹ Previous", enabled = index > 0, Modifier.weight(1f)) { index-- }
            NavPill("Next exercise ›", enabled = index + 1 < positions.size, Modifier.weight(1f)) { index++ }
        }

        if (timer.running || timer.finished) RestCard(timer, notificationsAllowed)
    }

    if (finishing) {
        AfDialog(
            title = "Finish workout?",
            onDismiss = { finishing = false },
            confirm = "Finish" to {
                finishing = false
                timer.stop()
                scope.launch { actions.onFinish(doneCount > 0) }
            },
        ) {
            Text(if (doneCount > 0) "$doneCount of ${sessionSets.size} sets done. It's saved to History." else "No sets are ticked, so nothing will be saved.")
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp), color = Color.White.copy(alpha = 0.6f), modifier = modifier)
}

@Composable
private fun SetRow(set: SetEntity, previous: String?, target: Int?, onChange: (SetEntity) -> Unit, onToggle: (SetEntity) -> Unit) {
    var kgText by remember(set.id) { mutableStateOf(set.kg?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "") }
    var countText by remember(set.id) { mutableStateOf(set.count?.toString() ?: "") }
    val reduce = LocalReduceMotion.current
    val pop = remember(set.id) { Animatable(1f) }
    LaunchedEffect(set.done) {
        if (set.done && !reduce) { pop.snapTo(0.8f); pop.animateTo(1.15f, tween(190)); pop.animateTo(1f, tween(130)) }
    }
    Row(
        Modifier.fillMaxWidth().clip(AfShapes.Inner).background(if (set.done) AfColors.Success.copy(alpha = 0.12f) else Color.Transparent).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("${set.setIndex}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.Text, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
        Text(previous ?: "—", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.weight(1f), maxLines = 1)
        if (set.unit == Measure.REPS) {
            NumberField(kgText, "kg", "Set ${set.setIndex} weight in kg", KeyboardType.Decimal) {
                kgText = it
                onChange(set.copy(kg = it.replace(',', '.').toFloatOrNull()))
            }
        } else Box(Modifier.width(64.dp))
        NumberField(countText, target?.toString() ?: set.unit.short, "Set ${set.setIndex} ${set.unit.short}", KeyboardType.Number) {
            countText = it.filter(Char::isDigit).take(4)
            onChange(set.copy(count = countText.toIntOrNull()))
        }
        Box(
            Modifier.size(48.dp).scale(pop.value).clip(AfShapes.Inner).background(if (set.done) AfColors.Success else AfColors.Inner)
                .clickable(role = Role.Checkbox, onClickLabel = if (set.done) "Mark set ${set.setIndex} not done" else "Mark set ${set.setIndex} done") {
                    val done = !set.done
                    val count = countText.toIntOrNull() ?: if (done) target else null
                    if (done && countText.isEmpty() && count != null) countText = count.toString()
                    onToggle(set.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null, count = count, kg = kgText.replace(',', '.').toFloatOrNull()))
                }
                .semantics { stateDescription = if (set.done) "done" else "not done" },
            contentAlignment = Alignment.Center,
        ) { Icon(AfIcons.Check, contentDescription = null, tint = if (set.done) AfColors.Background else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun NumberField(value: String, placeholder: String, description: String, keyboard: KeyboardType, onValueChange: (String) -> Unit) {
    Box(
        Modifier.size(width = 64.dp, height = 44.dp).clip(RoundedTen).background(AfColors.Inner).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.45f))
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = MaterialTheme.typography.titleSmall.copy(color = AfColors.Text, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            cursorBrush = SolidColor(AfColors.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val RoundedTen = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)

@Composable
private fun NavPill(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).border(1.dp, AfColors.Border, AfShapes.Pill).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (enabled) AfColors.Text else Color.White.copy(alpha = 0.3f)) }
}

@Composable
private fun RestCard(timer: RestTimer, notificationsAllowed: Boolean) {
    val pulse by rememberPulseAlpha()
    val go = timer.finished
    Row(
        Modifier.fillMaxWidth().clip(AfShapes.Card).background(AfColors.Card).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
            AfRing(
                fraction = if (go) 1f else timer.remaining / timer.total.toFloat().coerceAtLeast(1f),
                size = 108.dp, strokeWidth = 9.dp,
                color = if (go) AfColors.Success else AfColors.Accent,
                minSweepDegrees = 3f,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    clock(timer.remaining),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AfColors.Text,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(if (go) "GO!" else "REST", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp,
                    color = Color.White.copy(alpha = if (go) pulse else 0.65f))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AfIcons.Band, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(14.dp))
                Text(
                    if (notificationsAllowed) "Fit3 buzzes when rest ends" else "Allow notifications so your Fit3 can buzz",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = Color.White.copy(alpha = 0.75f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimerButton("−15", "Remove 15 seconds", AfColors.Inner, AfColors.Text, Modifier.weight(1f), enabled = !go) { timer.adjust(-15) }
                TimerButton("+15", "Add 15 seconds", AfColors.Inner, AfColors.Text, Modifier.weight(1f), enabled = !go) { timer.adjust(15) }
                TimerButton(if (go) "Next set" else "Skip", null, AfColors.Accent, AfColors.OnAccent, Modifier.weight(1f)) { timer.stop() }
            }
        }
    }
}

@Composable
private fun TimerButton(label: String, description: String?, bg: Color, fg: Color, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (enabled) fg else fg.copy(alpha = 0.4f)) }
}
