package com.arjunvision.arjunfitai.ui.nutrition

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arjunvision.arjunfitai.data.local.FoodEntry
import com.arjunvision.arjunfitai.data.local.LocalLogDatabase
import com.arjunvision.arjunfitai.data.local.WaterEntry
import com.arjunvision.arjunfitai.data.health.HealthMetric
import com.arjunvision.arjunfitai.data.health.model.ActivitySummary
import com.arjunvision.arjunfitai.data.health.model.NutritionRecord
import com.arjunvision.arjunfitai.data.health.model.WaterRecord
import com.arjunvision.arjunfitai.data.settings.UserSettings
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.components.AfRing
import com.arjunvision.arjunfitai.ui.components.AfSourcePill
import com.arjunvision.arjunfitai.ui.components.AfTextField
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.rememberDrawIn
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.detail.rememberLoaded
import com.arjunvision.arjunfitai.ui.detail.Loaded
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.home.formatKcal
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfEyebrow
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val number = NumberFormat.getIntegerInstance(Locale.ENGLISH)
private val CupShape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionScreen(
    state: DashboardUiState,
    settings: UserSettings,
    db: LocalLogDatabase,
    loadNutrition: suspend (LocalDate) -> Result<List<NutritionRecord>>,
    loadWater: suspend (LocalDate) -> Result<List<WaterRecord>>,
    loadActivity: suspend (LocalDate) -> Result<ActivitySummary>,
    contentPadding: PaddingValues,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    var epochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val date = LocalDate.ofEpochDay(epochDay)
    val isToday = date == today
    val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val scope = rememberCoroutineScope()

    val localFood by remember(from) { db.food().between(from, to) }.collectAsStateWithLifecycle(emptyList())
    val localWater by remember(from) { db.water().between(from, to) }.collectAsStateWithLifecycle(emptyList())
    val granted = state.permissions?.granted.orEmpty()
    val samsungFood = rememberLoaded(date, state.lastUpdated, HealthMetric.NUTRITION in granted) {
        if (HealthMetric.NUTRITION in granted) loadNutrition(date) else Result.success(emptyList())
    }
    val samsungWater = rememberLoaded(date, state.lastUpdated, HealthMetric.WATER_INTAKE in granted) {
        if (HealthMetric.WATER_INTAKE in granted) loadWater(date) else Result.success(emptyList())
    }
    val activity = rememberLoaded(date, state.lastUpdated) { loadActivity(date) }

    val day = dayNutrition(
        localFood, (samsungFood as? Loaded.Ready)?.value.orEmpty(),
        localWater, (samsungWater as? Loaded.Ready)?.value.orEmpty(),
    )

    var logForm by remember { mutableStateOf<QuickFood?>(null) }
    var showLog by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<FoodEntry?>(null) }
    var lastWater by remember { mutableStateOf<WaterEntry?>(null) }
    LaunchedEffect(lastWater) { if (lastWater != null) { delay(6_000); lastWater = null } }

    /** Today: now. Earlier days: noon, so the entry lands on the day being viewed. */
    fun entryTime(): Long = if (isToday) System.currentTimeMillis() else date.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()

    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("NUTRITION", style = MaterialTheme.typography.headlineLarge, color = AfColors.Text, modifier = Modifier.weight(1f))
            DaySwitcher(date, today, onPrev = { epochDay-- }, onNext = { if (!isToday) epochDay++ })
        }

        CaloriesCard(day.kcal, settings.calorieGoalKcal, (activity as? Loaded.Ready)?.value?.activeCaloriesKcal, isToday, Modifier.riseIn(1))
        MacrosCard(day, settings, Modifier.riseIn(2))
        WaterCard(
            waterMl = day.waterMl,
            goalMl = settings.waterGoalMl,
            canUndo = lastWater != null,
            onAdd = {
                scope.launch {
                    val entry = WaterEntry(time = entryTime(), ml = 250)
                    lastWater = entry.copy(id = db.water().insert(entry))
                }
            },
            onUndo = { lastWater?.let { e -> scope.launch { db.water().delete(e) }; lastWater = null } },
            modifier = Modifier.riseIn(3),
        )

        AfPrimaryButton("Log food", onClick = { logForm = null; showLog = true }, icon = AfIcons.Plus, height = 52.dp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MY USUAL FOODS · QUICK ADD", style = AfEyebrow, color = AfColors.TextMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickFoods.forEach { food ->
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).border(1.dp, AfColors.Border, AfShapes.Pill)
                            .clickable(role = Role.Button, onClickLabel = "Add ${food.name}") { logForm = food; showLog = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(food.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = AfColors.Text) }
                }
            }
        }

        LogList(
            day = day,
            samsungShared = HealthMetric.NUTRITION in granted,
            failed = (samsungFood as? Loaded.Failed)?.message,
            onDelete = { deleting = it },
        )
    }

    if (showLog) {
        LogFoodDialog(
            preset = logForm,
            onDismiss = { showLog = false },
            onSave = { entry ->
                scope.launch { db.food().insert(entry.copy(time = entryTime())) }
                showLog = false
            },
        )
    }
    deleting?.let { entry ->
        AfDialog(
            title = "Delete ${entry.name}?",
            onDismiss = { deleting = null },
            confirm = "Delete" to { scope.launch { db.food().delete(entry) }; deleting = null },
        ) { Text("This removes it from ArjunFit's log. Samsung Health is not changed.") }
    }
}

@Composable
private fun DaySwitcher(date: LocalDate, today: LocalDate, onPrev: () -> Unit, onNext: () -> Unit) {
    val label = when (date) {
        today -> "Today, " + date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
    }
    Row(Modifier.clip(AfShapes.Pill).background(AfColors.Card).padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(AfShapes.Pill).clickable(role = Role.Button, onClickLabel = "Previous day", onClick = onPrev), contentAlignment = Alignment.Center) {
            Icon(AfIcons.ChevronLeft, contentDescription = "Previous day", tint = AfColors.Text, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = AfColors.Text, modifier = Modifier.padding(horizontal = 2.dp))
        val canNext = date < today
        Box(
            Modifier.size(48.dp).clip(AfShapes.Pill).clickable(enabled = canNext, role = Role.Button, onClickLabel = "Next day", onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AfIcons.Chevron, contentDescription = "Next day", tint = if (canNext) AfColors.Text else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CaloriesCard(eaten: Float, goal: Int, activeKcal: Float?, isToday: Boolean, modifier: Modifier) {
    AfCard(modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(132.dp).semantics { contentDescription = "${eaten.roundToInt()} of $goal kcal eaten" },
                contentAlignment = Alignment.Center,
            ) {
                AfRing(if (goal > 0) eaten / goal else 0f, 132.dp, 12.dp, minSweepDegrees = 0f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatKcal(eaten), style = MaterialTheme.typography.headlineLarge, color = AfColors.Text)
                    Text("of ${number.format(goal)} kcal", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStat(number.format((goal - eaten).roundToInt().coerceAtLeast(0)), if (isToday) "kcal left today" else "kcal left that day")
                SmallStat(activeKcal?.let(::formatKcal) ?: EMPTY, "Active kcal burned (Fit3)")
            }
        }
    }
}

@Composable
private fun SmallStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = AfColors.Text)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
    }
}

@Composable
private fun MacrosCard(day: DayNutrition, s: UserSettings, modifier: Modifier) {
    AfCard(modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroBar("Protein", "${day.proteinG.roundToInt()} g / ${s.proteinMinG}–${s.proteinMaxG} g", day.proteinG / s.proteinMaxG, AfColors.Accent)
            MacroBar("Carbs", "${day.carbsG.roundToInt()} g / ${s.carbsTargetG} g", day.carbsG / s.carbsTargetG, AfColors.AccentSoft)
            MacroBar("Fat", "${day.fatG.roundToInt()} g / ${s.fatTargetG} g", day.fatG / s.fatTargetG, Color.White)
        }
    }
}

@Composable
private fun MacroBar(name: String, value: String, fraction: Float, color: Color) {
    val grow by rememberDrawIn(900, 150)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(name, style = MaterialTheme.typography.labelLarge, color = AfColors.Text, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(AfShapes.Pill).background(AfColors.Inner)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f) * grow).clip(AfShapes.Pill).background(color))
        }
    }
}

@Composable
private fun WaterCard(waterMl: Int, goalMl: Int, canUndo: Boolean, onAdd: () -> Unit, onUndo: () -> Unit, modifier: Modifier) {
    val filled = filledCups(waterMl, goalMl)
    AfCard(modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    buildAnnotatedString {
                        append("Water ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f))) {
                            append("${number.format(waterMl)} / ${number.format(goalMl)} ml")
                        }
                    },
                    style = MaterialTheme.typography.labelLarge, color = AfColors.Text,
                )
                Row(
                    Modifier.semantics { contentDescription = "$waterMl of $goalMl ml water" },
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    repeat(8) { i -> Cup(full = i < filled, modifier = Modifier.weight(1f)) }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Water)
                        .clickable(role = Role.Button, onClick = onAdd).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("+250 ml", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = AfColors.Background) }
                if (canUndo) {
                    Box(Modifier.heightIn(min = 40.dp).clickable(role = Role.Button, onClick = onUndo).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                        Text("Undo", style = MaterialTheme.typography.labelMedium, color = AfColors.AccentSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cup(full: Boolean, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    val color by animateColorAsState(if (full) AfColors.Water else AfColors.Inner, if (reduce) snap() else tween(300), label = "cup")
    Box(modifier.height(16.dp).clip(CupShape).background(color))
}

@Composable
private fun LogList(day: DayNutrition, samsungShared: Boolean, failed: String?, onDelete: (FoodEntry) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("LOGGED · ${day.items.size}", style = AfEyebrow, color = AfColors.TextMuted)
        if (day.items.isEmpty()) {
            Text("Nothing logged for this day yet.", style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted)
        } else {
            AfCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                day.items.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(color = AfColors.Inner)
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
                                AfSourcePill(item.source.label, fromSamsung = item.source == Source.SAMSUNG)
                            }
                            Text(
                                "${formatKcal(item.kcal)} kcal · P ${item.proteinG.roundToInt()} g · C ${item.carbsG.roundToInt()} g · F ${item.fatG.roundToInt()} g",
                                style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted,
                            )
                        }
                        item.local?.let { entry ->
                            Box(
                                Modifier.size(48.dp).clip(AfShapes.Pill).clickable(role = Role.Button, onClickLabel = "Delete ${item.name}") { onDelete(entry) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(AfIcons.Trash, contentDescription = "Delete ${item.name}", tint = AfColors.TextMuted, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
        val note = when {
            failed != null -> "Samsung Health food couldn't be read: $failed"
            !samsungShared -> "Food and water from Samsung Health aren't shared. Allow them in Health sync › Review permissions."
            else -> null
        }
        note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted) }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun LogFoodDialog(preset: QuickFood?, onDismiss: () -> Unit, onSave: (FoodEntry) -> Unit) {
    var name by rememberSaveable { mutableStateOf(preset?.name ?: "") }
    var kcal by rememberSaveable { mutableStateOf(preset?.kcal?.let(::trim) ?: "") }
    var protein by rememberSaveable { mutableStateOf(preset?.proteinG?.let(::trim) ?: "") }
    var carbs by rememberSaveable { mutableStateOf(preset?.carbsG?.let(::trim) ?: "") }
    var fat by rememberSaveable { mutableStateOf(preset?.fatG?.let(::trim) ?: "") }
    val kcalValue = kcal.replace(',', '.').toFloatOrNull()?.takeIf { it in 0f..5000f }
    fun grams(t: String) = if (t.isBlank()) 0f else t.replace(',', '.').toFloatOrNull()?.takeIf { it in 0f..500f }
    val valid = name.isNotBlank() && kcalValue != null && grams(protein) != null && grams(carbs) != null && grams(fat) != null
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AfDialog(
        title = if (preset != null) "Add ${preset.name}" else "Log food",
        onDismiss = onDismiss,
        confirm = "Save" to {
            if (valid) onSave(FoodEntry(time = 0, name = name.trim(), kcal = kcalValue!!, proteinG = grams(protein)!!, carbsG = grams(carbs)!!, fatG = grams(fat)!!))
        },
        confirmEnabled = valid,
    ) {
        preset?.let { Text("${it.serving} · approximate values, edit to match your portion", style = MaterialTheme.typography.bodySmall) }
        AfTextField(name, { name = it.take(40) }, "Food", KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        AfTextField(kcal, { kcal = it }, "Calories (kcal)", decimal, if (kcal.isNotEmpty() && kcalValue == null) "Enter 0–5,000" else null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { AfTextField(protein, { protein = it }, "Protein g", decimal) }
            Box(Modifier.weight(1f)) { AfTextField(carbs, { carbs = it }, "Carbs g", decimal) }
            Box(Modifier.weight(1f)) { AfTextField(fat, { fat = it }, "Fat g", decimal) }
        }
    }
}

private fun trim(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(Locale.ENGLISH, v)
