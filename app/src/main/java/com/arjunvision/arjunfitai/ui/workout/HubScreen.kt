package com.arjunvision.arjunfitai.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.workout.Exercise
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.PlanItemEntity
import com.arjunvision.arjunfitai.data.workout.PlanKind
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfEyebrow
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.LocalDate
import java.time.ZoneId

class HubActions(
    val onStart: (PlanEntity) -> Unit,
    val onResume: () -> Unit,
    val onLibrary: () -> Unit,
    val onPlans: () -> Unit,
    val onHistory: () -> Unit,
    val onPrs: () -> Unit,
    val onNewPlan: () -> Unit,
)

@Composable
fun HubScreen(
    plans: List<PlanEntity>,
    items: List<PlanItemEntity>,
    sessions: List<SessionEntity>,
    active: SessionEntity?,
    library: Map<String, Exercise>,
    actions: HubActions,
    contentPadding: PaddingValues,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    var mode by rememberSaveable { mutableStateOf(PlanKind.GYM) }
    val pick = todaysPlan(mode, plans, sessions, today)
    val pickItems = pick.plan?.let { p -> items.filter { it.planId == p.id } }.orEmpty()

    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("WINTER ARC · STRENGTH", style = AfEyebrow.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = AfColors.TextMuted)
            Text("WORKOUTS", style = MaterialTheme.typography.headlineLarge, color = AfColors.Text)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(PlanKind.GYM, PlanKind.HOME, PlanKind.CUSTOM).forEach { kind ->
                val on = kind == mode
                Box(
                    Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill)
                        .background(if (on) AfColors.Accent else Color.Transparent)
                        .border(1.dp, if (on) AfColors.Accent else AfColors.Border, AfShapes.Pill)
                        .clickable(role = Role.Tab) { mode = kind }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(kind.label, style = MaterialTheme.typography.labelLarge, color = if (on) AfColors.OnAccent else AfColors.Text) }
            }
        }

        if (active != null) {
            AfCard(Modifier.fillMaxWidth().riseIn(0), padding = PaddingValues(16.dp), color = AfColors.Inner) {
                Text("WORKOUT IN PROGRESS", style = AfEyebrow, color = AfColors.AccentSoft)
                Text(active.planName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = AfColors.Text)
                AfPrimaryButton("Resume workout", actions.onResume, icon = AfIcons.Workout, height = 48.dp, modifier = Modifier.padding(top = 10.dp))
            }
        }

        AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(0.dp)) {
            val cover = pickItems.firstNotNullOfOrNull { it.exerciseId?.let(library::get)?.images?.firstOrNull() }
            ExercisePhoto(cover, contentDescription = null, modifier = Modifier.fillMaxWidth().height(130.dp), label = "COVER PHOTO")
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pick.label, style = AfEyebrow, color = AfColors.Accent)
                Text(
                    pick.plan?.name ?: if (mode == PlanKind.CUSTOM) "Build your own" else "No plans yet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = AfColors.Text,
                )
                Text(
                    pick.plan?.let { (it.focus.ifBlank { pickItems.joinToString(" · ") { i -> i.name } }) + " · " + planMeta(pickItems) }
                        ?: "Pick exercises from the library",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted,
                )
                val plan = pick.plan
                if (plan != null) {
                    AfPrimaryButton(if (active?.planId == plan.id) "Resume workout" else "Start workout", { if (active?.planId == plan.id) actions.onResume() else actions.onStart(plan) },
                        icon = AfIcons.Play, height = 48.dp, enabled = pickItems.isNotEmpty(), modifier = Modifier.padding(top = 10.dp))
                } else if (mode == PlanKind.CUSTOM) {
                    AfPrimaryButton("New plan", actions.onNewPlan, icon = AfIcons.Plus, height = 48.dp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        Row(Modifier.riseIn(2), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HubTile(AfIcons.Book, "Library", actions.onLibrary, Modifier.weight(1f))
            HubTile(AfIcons.Calendar, "Plan", actions.onPlans, Modifier.weight(1f))
            HubTile(AfIcons.History, "History", actions.onHistory, Modifier.weight(1f))
            HubTile(AfIcons.Trophy, "PRs", actions.onPrs, Modifier.weight(1f))
        }

        AfCard(Modifier.fillMaxWidth().riseIn(3), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("CURRENT PLAN", style = AfEyebrow, color = AfColors.TextMuted)
                    Text("Winter Arc ${today.year}", style = MaterialTheme.typography.titleMedium, color = AfColors.Text)
                }
                Text("Oct 12 – Dec 31", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                weekStrip(today, sessions, zone).forEach { DayDot(it) }
            }
            Text("Strength Mon · Wed · Fri + cardio · Sunday recovery", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun HubTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 76.dp).clip(RoundedSixteen).background(AfColors.Card).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = AfColors.AccentSoft, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = AfColors.Text)
    }
}

private val RoundedSixteen = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

@Composable
private fun DayDot(cell: DayCell) {
    val bg = when (cell.kind) {
        DayKind.STRENGTH -> AfColors.Accent
        DayKind.CARDIO -> AfColors.Inner
        DayKind.REST -> Color.Transparent
    }
    val fg = if (cell.kind == DayKind.STRENGTH) AfColors.OnAccent else Color.White.copy(alpha = 0.75f)
    val border = when {
        cell.isToday -> AfColors.Text
        cell.kind == DayKind.REST -> AfColors.Border
        else -> Color.Transparent
    }
    val name = when (cell.kind) { DayKind.STRENGTH -> "strength"; DayKind.CARDIO -> "cardio"; DayKind.REST -> "recovery" }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${cell.date.dayOfWeek.name.lowercase()}: $name" + (if (cell.worked) ", workout done" else "") + (if (cell.isToday) ", today" else "")
        },
    ) {
        Text(cell.label, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
        Box(
            Modifier.size(34.dp).clip(AfShapes.Pill).background(bg).border(if (cell.isToday) 2.dp else 1.dp, border, AfShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (cell.worked) "✓" else cell.kind.tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (cell.worked && cell.kind != DayKind.STRENGTH) AfColors.Success else fg)
        }
    }
}
