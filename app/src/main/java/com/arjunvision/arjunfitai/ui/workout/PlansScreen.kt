package com.arjunvision.arjunfitai.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.workout.Exercise
import com.arjunvision.arjunfitai.data.workout.Measure
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.PlanItemEntity
import com.arjunvision.arjunfitai.data.workout.PlanKind
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfOutlineButton
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.components.AfSegmentedTabs
import com.arjunvision.arjunfitai.ui.components.AfTextField
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.detail.DetailColumn
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.LocalDate

private val tabKinds = listOf(PlanKind.GYM, PlanKind.HOME, PlanKind.CARDIO, PlanKind.CUSTOM)

@Composable
fun PlansScreen(
    plans: List<PlanEntity>,
    items: List<PlanItemEntity>,
    sessions: List<SessionEntity>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (PlanEntity) -> Unit,
    onStart: (PlanEntity) -> Unit,
    onNewPlan: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val kind = tabKinds[tab]
    val today = LocalDate.now()
    val pick = todaysPlan(PlanKind.GYM, plans, sessions, today)
    val shown = plans.filter { it.kind == kind }

    DetailColumn(contentPadding) {
        AfScreenHeader("Strength plans", onBack)
        AfSegmentedTabs(tabKinds.map { it.label }, tab, { tab = it })
        Text(
            when (kind) {
                PlanKind.GYM -> "Winter Arc · full-body strength 3× a week"
                PlanKind.HOME -> "No equipment — for busy days"
                PlanKind.CARDIO -> "Stamina days · Tue / Thu / Sat"
                PlanKind.CUSTOM -> "Your own plans, built from the library"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = Color.White.copy(alpha = 0.7f),
        )
        shown.forEachIndexed { i, plan ->
            val planItems = items.filter { it.planId == plan.id }
            val isToday = plan.id == pick.plan?.id && pick.label.startsWith("TODAY")
            Row(
                Modifier.fillMaxWidth().heightIn(min = 76.dp).riseIn(i).clip(AfShapes.Card).background(AfColors.Card)
                    .clickable(role = Role.Button, onClickLabel = "Open ${plan.name}") { onOpen(plan) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    Modifier.size(56.dp).clip(RoundedSixteen).background(if (isToday) AfColors.Accent else AfColors.Inner),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    val c = if (isToday) AfColors.OnAccent else AfColors.AccentSoft
                    Text(plan.dayLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c)
                    Icon(AfIcons.Workout, contentDescription = null, tint = c, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, color = AfColors.Text)
                    Text(plan.focus.ifBlank { planItems.joinToString(" · ") { it.name }.ifBlank { "No exercises yet" } },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = Color.White.copy(alpha = 0.7f), maxLines = 2)
                    Text(planMeta(planItems), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
                Icon(AfIcons.Chevron, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
        if (kind == PlanKind.CUSTOM) AfOutlineButton("+ New plan", onNewPlan)
        pick.plan?.let { p -> AfPrimaryButton("Start today's workout", { onStart(p) }, icon = AfIcons.Play) }
    }
}

private val RoundedSixteen = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

class PlanEditActions(
    val onBack: () -> Unit,
    val onStart: () -> Unit,
    val onAddExercise: () -> Unit,
    val onUpdate: (PlanItemEntity) -> Unit,
    val onMove: (PlanItemEntity, Int) -> Unit,
    val onRemove: (PlanItemEntity) -> Unit,
    val onRename: (String) -> Unit,
    val onDelete: () -> Unit,
)

@Composable
fun PlanEditScreen(plan: PlanEntity, items: List<PlanItemEntity>, library: Map<String, Exercise>, contentPadding: PaddingValues, actions: PlanEditActions) {
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    DetailColumn(contentPadding) {
        AfScreenHeader(plan.name, actions.onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${plan.kind.label} · ${planMeta(items)}", style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted, modifier = Modifier.weight(1f))
            Text("Rename", style = MaterialTheme.typography.labelLarge, color = AfColors.AccentSoft,
                modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { renaming = true }.padding(horizontal = 8.dp, vertical = 14.dp))
        }
        if (items.isEmpty()) {
            Text("No exercises yet. Add some from the library.", style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted)
        } else {
            AfCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                items.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(color = AfColors.Inner)
                    ItemRow(item, item.exerciseId?.let(library::get), first = i == 0, last = i == items.lastIndex, actions)
                }
            }
        }
        AfOutlineButton("+ Add exercise from library", actions.onAddExercise)
        AfPrimaryButton("Start this workout", actions.onStart, icon = AfIcons.Play, enabled = items.isNotEmpty())
        Text("Delete plan", style = MaterialTheme.typography.labelLarge, color = AfColors.TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp).clickable(role = Role.Button) { deleting = true }.padding(14.dp))
    }

    if (renaming) {
        var name by rememberSaveable { mutableStateOf(plan.name) }
        AfDialog("Rename plan", { renaming = false }, "Save" to { actions.onRename(name.trim()); renaming = false }, confirmEnabled = name.isNotBlank()) {
            AfTextField(name, { name = it.take(40) }, "Plan name", KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        }
    }
    if (deleting) {
        AfDialog("Delete ${plan.name}?", { deleting = false }, "Delete" to { deleting = false; actions.onDelete() }) {
            Text("The plan is removed. Workouts you already logged with it stay in History.")
        }
    }
}

@Composable
private fun ItemRow(item: PlanItemEntity, exercise: Exercise?, first: Boolean, last: Boolean, actions: PlanEditActions) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExercisePhoto(exercise?.images?.firstOrNull(), null, Modifier.size(56.dp).clip(AfShapes.Inner), thumb = true)
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
                Text(targetText(item), style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
            }
            SmallIcon(AfIcons.Up, "Move up", enabled = !first) { actions.onMove(item, -1) }
            SmallIcon(AfIcons.Down, "Move down", enabled = !last) { actions.onMove(item, 1) }
            SmallIcon(AfIcons.Trash, "Remove ${item.name}") { actions.onRemove(item) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stepper("Sets", item.sets, 1..10, Modifier.weight(1f)) { actions.onUpdate(item.copy(sets = it)) }
            val (label, range, step) = when (item.unit) {
                Measure.REPS -> Triple("Reps", 1..50, 1)
                Measure.SECONDS -> Triple("Seconds", 10..300, 5)
                Measure.MINUTES -> Triple("Minutes", 5..180, 5)
            }
            Stepper(label, item.target, range, Modifier.weight(1f), step) { actions.onUpdate(item.copy(target = it)) }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, modifier: Modifier, step: Int = 1, onChange: (Int) -> Unit) {
    Row(modifier.clip(AfShapes.Pill).background(AfColors.Inner), verticalAlignment = Alignment.CenterVertically) {
        SmallIcon(AfIcons.Minus, "Fewer ${label.lowercase()}", enabled = value - step >= range.first) { onChange((value - step).coerceIn(range)) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.Text)
            Text(label, fontSize = 10.sp, color = AfColors.TextMuted)
        }
        SmallIcon(AfIcons.Plus, "More ${label.lowercase()}", enabled = value + step <= range.last) { onChange((value + step).coerceIn(range)) }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(AfShapes.Pill).clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = if (enabled) AfColors.Text else Color.White.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
    }
}

/** Dialog listing plans to add [exercise] to, plus "New plan". */
@Composable
fun AddToPlanDialog(exercise: Exercise, plans: List<PlanEntity>, onDismiss: () -> Unit, onPick: (PlanEntity) -> Unit, onNewPlan: (String) -> Unit) {
    var newName by remember { mutableStateOf("") }
    AfDialog("Add ${exercise.name}", onDismiss, "Create plan" to { if (newName.isNotBlank()) onNewPlan(newName.trim()) }, confirmEnabled = newName.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            plans.filter { it.kind != PlanKind.CARDIO || exercise.isCardio }.forEach { p ->
                Text(p.name, style = MaterialTheme.typography.bodyLarge, color = AfColors.Text,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(AfShapes.Inner).clickable(role = Role.Button) { onPick(p) }.padding(horizontal = 8.dp, vertical = 12.dp))
            }
        }
        AfTextField(newName, { newName = it.take(40) }, "…or a new plan", KeyboardOptions(capitalization = KeyboardCapitalization.Words))
    }
}
