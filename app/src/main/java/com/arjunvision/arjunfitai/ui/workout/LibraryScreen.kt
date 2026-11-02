package com.arjunvision.arjunfitai.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.workout.Exercise
import com.arjunvision.arjunfitai.data.workout.LibraryFilter
import com.arjunvision.arjunfitai.data.workout.MuscleGroup
import com.arjunvision.arjunfitai.data.workout.filterExercises
import com.arjunvision.arjunfitai.data.workout.titleCase
import com.arjunvision.arjunfitai.ui.components.AfCircleButton
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfEyebrow
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.util.Locale

/** "Chest · Barbell". */
fun exerciseMeta(e: Exercise): String =
    listOfNotNull(e.group?.label ?: e.primary.firstOrNull()?.titleCase(), (e.equipment ?: "body only").titleCase()).joinToString(" · ")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    library: List<Exercise>,
    /** Set when choosing an exercise for a plan ("Add to Full Body A"). */
    pickingFor: String?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (Exercise) -> Unit,
    onAdd: (Exercise) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    var equipment by rememberSaveable { mutableStateOf(setOf<String>()) }
    var levels by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val shown = remember(library, query, group, equipment, levels) { filterExercises(library, LibraryFilter(query, group, equipment, levels)) }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AfCircleButton(AfIcons.Back, "Back", AfColors.Text, onBack)
                Column {
                    Text(pickingFor?.let { "ADD TO ${it.uppercase(Locale.ENGLISH)}" } ?: "${library.size} EXERCISES · WITH PHOTOS", style = AfEyebrow.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = AfColors.TextMuted)
                    Text("EXERCISES", style = MaterialTheme.typography.headlineLarge, color = AfColors.Text)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Card).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(AfIcons.Search, contentDescription = null, tint = AfColors.TextMuted, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search bench press, squat…", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp), color = AfColors.TextMuted)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = AfColors.Text),
                        cursorBrush = SolidColor(AfColors.Accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium, color = AfColors.AccentSoft, modifier = Modifier.clickable(role = Role.Button) { query = "" }.padding(8.dp))
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("All", group == null) { group = null }
                MuscleGroup.entries.forEach { g -> Chip(g.label, group == g) { group = if (group == g) null else g } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${group?.label ?: "All"} · ${shown.size} exercises", style = MaterialTheme.typography.labelLarge, color = AfColors.Text, modifier = Modifier.weight(1f))
                Row(
                    Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { showFilters = true }.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(AfIcons.Filter, contentDescription = null, tint = AfColors.AccentSoft, modifier = Modifier.size(16.dp))
                    val n = equipment.size + levels.size
                    Text("Equipment · Level" + if (n > 0) " ($n)" else "", style = MaterialTheme.typography.labelLarge, color = AfColors.AccentSoft)
                }
            }
        }
        if (shown.isEmpty()) {
            item { Text("No exercises match. Clear a filter or try another word.", style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted) }
        }
        items(shown, key = { it.id }) { e -> ExerciseRow(e, onOpen = { onOpen(e) }, onAdd = { onAdd(e) }) }
    }

    if (showFilters) {
        val allEquipment = remember(library) { library.map { it.equipment ?: "none" }.distinct().sorted() }
        AfDialog(title = "Equipment · Level", onDismiss = { showFilters = false }, confirm = "Done" to { showFilters = false }, showCancel = false) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LEVEL", style = AfEyebrow, color = AfColors.TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("beginner", "intermediate", "expert").forEach { l -> Chip(l.titleCase(), l in levels) { levels = levels.toggle(l) } }
                }
                Text("EQUIPMENT", style = AfEyebrow, color = AfColors.TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allEquipment.forEach { eq -> Chip(if (eq == "none") "No equipment listed" else eq.titleCase(), eq in equipment) { equipment = equipment.toggle(eq) } }
                }
                if (equipment.isNotEmpty() || levels.isNotEmpty()) {
                    Text("Clear filters", style = MaterialTheme.typography.labelLarge, color = AfColors.AccentSoft,
                        modifier = Modifier.clickable(role = Role.Button) { equipment = emptySet(); levels = emptySet() }.padding(vertical = 8.dp))
                }
            }
        }
    }
}

private fun Set<String>.toggle(v: String) = if (v in this) this - v else this + v

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 40.dp).clip(AfShapes.Pill)
            .background(if (on) AfColors.Accent else Color.Transparent)
            .border(1.dp, if (on) AfColors.Accent else AfColors.Border, AfShapes.Pill)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) AfColors.OnAccent else AfColors.Text) }
}

@Composable
private fun ExerciseRow(e: Exercise, onOpen: () -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(AfShapes.Card).background(AfColors.Card).clickable(role = Role.Button, onClickLabel = "Open ${e.name}", onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ExercisePhoto(e.images.firstOrNull(), contentDescription = null, modifier = Modifier.size(84.dp).clip(RoundedFourteen), thumb = true, label = "PHOTO")
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(e.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = AfColors.Text)
            Text(exerciseMeta(e), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
            e.level?.let {
                Text(it.uppercase(Locale.ENGLISH), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = AfColors.AccentSoft,
                    modifier = Modifier.clip(AfShapes.Pill).background(AfColors.Inner).padding(horizontal = 10.dp, vertical = 3.dp))
            }
        }
        Box(
            Modifier.size(48.dp).clip(AfShapes.Pill).background(AfColors.Accent).clickable(role = Role.Button, onClickLabel = "Add ${e.name} to a plan", onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Icon(AfIcons.Plus, contentDescription = "Add ${e.name}", tint = AfColors.OnAccent, modifier = Modifier.size(20.dp)) }
    }
}

private val RoundedFourteen = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(e: Exercise, contentPadding: PaddingValues, onBack: () -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AfCircleButton(AfIcons.Back, "Back", AfColors.Text, onBack)
            Text(e.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = AfColors.Text, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("START POSITION", "END POSITION").forEachIndexed { i, label ->
                ExercisePhoto(e.images.getOrNull(i), "${e.name}, ${label.lowercase(Locale.ENGLISH)}", Modifier.weight(1f).height(160.dp).clip(AfShapes.Card), label = label)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOfNotNull(e.group?.label, e.equipment?.titleCase(), e.level?.titleCase(), e.mechanic?.titleCase(), e.category?.titleCase()).forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = AfColors.AccentSoft,
                    modifier = Modifier.clip(AfShapes.Pill).background(AfColors.Inner).padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Text(
            "Primary: " + e.primary.joinToString { it.titleCase() } + if (e.secondary.isNotEmpty()) "\nSecondary: " + e.secondary.joinToString { it.titleCase() } else "",
            style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted,
        )
        Text("HOW TO DO IT", style = AfEyebrow, color = AfColors.TextMuted)
        e.instructions.forEachIndexed { i, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = AfColors.Accent)
                Text(step, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = AfColors.Text)
            }
        }
        AfPrimaryButton("Add to plan", onAdd, icon = AfIcons.Plus)
        Text("Exercise data and photos: free-exercise-db (public domain)", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
    }
}
