package com.arjunvision.arjunfitai.ui.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.BuildConfig
import com.arjunvision.arjunfitai.R
import com.arjunvision.arjunfitai.data.settings.UserSettings
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfTextField
import com.arjunvision.arjunfitai.ui.components.AfIconWell
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.home.formatKcal
import com.arjunvision.arjunfitai.ui.home.formatSteps
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import kotlinx.coroutines.launch
import java.util.Locale

private enum class ProfileDialog { NAME, GOALS, APP_SETTINGS, PRIVACY }

@Composable
fun ProfileScreen(
    settings: UserSettings,
    samsungConnected: Boolean,
    onUpdate: suspend ((UserSettings) -> UserSettings) -> Unit,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onConnectedDevices: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }
    val save: ((UserSettings) -> UserSettings) -> Unit = { transform -> scope.launch { onUpdate(transform) } }

    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AfScreenHeader("Profile", onBack)

        Column(
            Modifier
                .fillMaxWidth()
                .riseIn(0)
                .clip(AfShapes.Card)
                .clickable(role = Role.Button, onClickLabel = "Edit name") { dialog = ProfileDialog.NAME }
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(88.dp).clip(AfShapes.Pill).background(AfColors.Accent), contentAlignment = Alignment.Center) {
                val initials = initials(settings.name)
                if (initials.isNotEmpty()) {
                    Text(initials, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = AfColors.OnAccent)
                } else {
                    Icon(AfIcons.Profile, contentDescription = null, tint = AfColors.OnAccent, modifier = Modifier.size(40.dp))
                }
            }
            if (settings.name.isNotBlank()) {
                Text(settings.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = AfColors.Text)
            } else {
                Text("Add your name", style = MaterialTheme.typography.titleMedium, color = AfColors.AccentSoft)
            }
            if (settings.tagline.isNotBlank()) {
                Text(settings.tagline, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = Color.White.copy(alpha = 0.7f))
            }
        }

        AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            SettingsRow(
                AfIcons.Goals, "My goals",
                "${formatTarget(settings.targetWeightKg)} · ${formatSteps(settings.stepGoal.toLong())} steps · ${formatKcal(settings.calorieGoalKcal.toFloat())} kcal",
                onClick = { dialog = ProfileDialog.GOALS },
            )
            HorizontalDivider(color = AfColors.Inner)
            SettingsRow(
                AfIcons.Band, "Connected devices",
                if (samsungConnected) "Galaxy Fit3 · Samsung Health" else "Samsung Health not connected",
                onClick = onConnectedDevices,
            )
            HorizontalDivider(color = AfColors.Inner)
            SwitchRow(
                AfIcons.Bell, "Milestone alerts", "Buzz on Fit3 · quiet 22:00–07:00 · coming soon",
                checked = settings.milestoneAlerts,
                onCheckedChange = { on -> save { it.copy(milestoneAlerts = on) } },
            )
            HorizontalDivider(color = AfColors.Inner)
            SettingsRow(AfIcons.Sliders, "App settings", "Units kg · accent colour · animations", onClick = { dialog = ProfileDialog.APP_SETTINGS })
            HorizontalDivider(color = AfColors.Inner)
            SettingsRow(AfIcons.Lock, "Data & privacy", "Read-only · health data stays on this phone", onClick = { dialog = ProfileDialog.PRIVACY })
        }

        AfCard(Modifier.fillMaxWidth().riseIn(2), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painterResource(R.drawable.arjunfit_logo),
                    contentDescription = "ArjunFit logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 40.dp, height = 36.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("About ArjunFit", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} · Arjun Vision Tech Solutions",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = AfColors.TextMuted,
                    )
                }
            }
        }
    }

    when (dialog) {
        ProfileDialog.NAME -> NameDialog(settings, onDismiss = { dialog = null }) { name, tagline ->
            save { it.copy(name = name, tagline = tagline) }
            dialog = null
        }
        ProfileDialog.GOALS -> GoalsDialog(settings, onDismiss = { dialog = null }) { kg, steps, kcal, targets ->
            save {
                it.copy(
                    targetWeightKg = kg, stepGoal = steps, calorieGoalKcal = kcal,
                    waterGoalMl = targets.waterMl, carbsTargetG = targets.carbsG, fatTargetG = targets.fatG,
                )
            }
            dialog = null
        }
        ProfileDialog.APP_SETTINGS -> InfoDialog(
            "App settings",
            "Units: kilograms and kilometres.\n\nAccent colour: Arjun Vision orange.\n\n" +
                "Animations follow Android's “Remove animations” setting (Settings › Accessibility). " +
                "Turn it on and ArjunFit stops all motion.",
        ) { dialog = null }
        ProfileDialog.PRIVACY -> InfoDialog(
            "Data & privacy",
            "ArjunFit only reads from Samsung Health — it never writes to it.\n\n" +
                "Your health data is read on this phone and is not sent anywhere. Your name and goals are " +
                "stored in ArjunFit on this phone.\n\nChange what ArjunFit may read any time in Health sync › " +
                "Review permissions.\n\nWorkouts, plans and sets are stored in ArjunFit on this phone.\n\n" +
                "Exercises and photos: free-exercise-db by yuhonas (public domain, Unlicense). " +
                "Fonts: Poppins (SIL Open Font License 1.1), Satisfy (Apache License 2.0).",
        ) { dialog = null }
        null -> Unit
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AfIconWell(icon)
        RowText(title, subtitle, Modifier.weight(1f))
        Icon(AfIcons.Chevron, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AfIconWell(icon)
        RowText(title, subtitle, Modifier.weight(1f))
        AfSwitch(checked)
    }
}

@Composable
private fun RowText(title: String, subtitle: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
        Text(subtitle, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
    }
}

/** Canvas switch: 52×32 track, 26dp white knob sliding 20dp. */
@Composable
private fun AfSwitch(checked: Boolean) {
    val reduce = LocalReduceMotion.current
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, if (reduce) snap() else tween(250), label = "knob")
    Box(
        Modifier.size(width = 52.dp, height = 32.dp).clip(AfShapes.Pill)
            .background(if (checked) AfColors.Accent else AfColors.Border).padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = x).size(26.dp).clip(AfShapes.Pill).background(Color.White))
    }
}

@Composable
private fun NameDialog(settings: UserSettings, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(settings.name) }
    var tagline by rememberSaveable { mutableStateOf(settings.tagline) }
    AfDialog(
        title = "Your profile",
        onDismiss = onDismiss,
        confirm = "Save" to { onSave(name.trim(), tagline.trim()) },
    ) {
        AfTextField(name, { name = it.take(40) }, "Name", KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        AfTextField(tagline, { tagline = it.take(60) }, "Tagline (optional)", KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
    }
}

@Composable
private fun GoalsDialog(settings: UserSettings, onDismiss: () -> Unit, onSave: (Float, Int, Int, NutritionTargets) -> Unit) {
    var kg by rememberSaveable { mutableStateOf(formatTarget(settings.targetWeightKg).removeSuffix(" kg")) }
    var steps by rememberSaveable { mutableStateOf(settings.stepGoal.toString()) }
    var kcal by rememberSaveable { mutableStateOf(settings.calorieGoalKcal.toString()) }
    var water by rememberSaveable { mutableStateOf(settings.waterGoalMl.toString()) }
    var carbs by rememberSaveable { mutableStateOf(settings.carbsTargetG.toString()) }
    var fat by rememberSaveable { mutableStateOf(settings.fatTargetG.toString()) }
    val goals = parseGoals(kg, steps, kcal)
    val targets = parseNutritionTargets(water, carbs, fat)
    AfDialog(
        title = "My goals",
        onDismiss = onDismiss,
        confirm = "Save" to { goals.valid?.let { (w, s, c) -> targets?.let { onSave(w, s, c, it) } } },
        confirmEnabled = goals.valid != null && targets != null,
    ) {
        AfTextField(kg, { kg = it }, "Target weight (kg)", KeyboardOptions(keyboardType = KeyboardType.Decimal), goals.weightError)
        AfTextField(steps, { steps = it.filter(Char::isDigit) }, "Daily steps", KeyboardOptions(keyboardType = KeyboardType.Number), goals.stepsError)
        AfTextField(kcal, { kcal = it.filter(Char::isDigit) }, "Daily calories (kcal)", KeyboardOptions(keyboardType = KeyboardType.Number), goals.kcalError)
        AfTextField(water, { water = it.filter(Char::isDigit) }, "Daily water (ml)", KeyboardOptions(keyboardType = KeyboardType.Number), if (targets == null && water.toIntOrNull() !in 500..6_000) "Enter 500–6,000" else null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { AfTextField(carbs, { carbs = it.filter(Char::isDigit) }, "Carbs g", KeyboardOptions(keyboardType = KeyboardType.Number)) }
            Box(Modifier.weight(1f)) { AfTextField(fat, { fat = it.filter(Char::isDigit) }, "Fat g", KeyboardOptions(keyboardType = KeyboardType.Number)) }
        }
        Text("Protein target: ${settings.proteinMinG}–${settings.proteinMaxG} g a day", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AfDialog(title = title, onDismiss = onDismiss, confirm = "Close" to onDismiss, showCancel = false) {
        Text(body, style = MaterialTheme.typography.bodyMedium, color = AfColors.TextMuted)
    }
}

/** "SB" for "Sri Balaji Aravind"; empty when there is no name. */
internal fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

internal fun formatTarget(kg: Float): String =
    if (kg % 1f == 0f) "${kg.toInt()} kg" else "%.1f kg".format(Locale.ENGLISH, kg)

internal data class GoalsInput(
    val valid: Triple<Float, Int, Int>?,
    val weightError: String?,
    val stepsError: String?,
    val kcalError: String?,
)

/** Sensible ranges: 30–300 kg, 1,000–30,000 steps (as the canvas), 1,000–6,000 kcal. */
internal fun parseGoals(kg: String, steps: String, kcal: String): GoalsInput {
    val w = kg.replace(',', '.').toFloatOrNull()
    val s = steps.toIntOrNull()
    val c = kcal.toIntOrNull()
    val wErr = if (w == null || w !in 30f..300f) "Enter 30–300 kg" else null
    val sErr = if (s == null || s !in 1_000..30_000) "Enter 1,000–30,000" else null
    val cErr = if (c == null || c !in 1_000..6_000) "Enter 1,000–6,000" else null
    val valid = if (wErr == null && sErr == null && cErr == null) Triple(w!!, s!!, c!!) else null
    return GoalsInput(valid, wErr, sErr, cErr)
}

internal data class NutritionTargets(val waterMl: Int, val carbsG: Int, val fatG: Int)

/** Water 500–6,000 ml, carbs 20–800 g, fat 10–300 g; null if any is out of range. */
internal fun parseNutritionTargets(water: String, carbs: String, fat: String): NutritionTargets? {
    val w = water.toIntOrNull()?.takeIf { it in 500..6_000 } ?: return null
    val c = carbs.toIntOrNull()?.takeIf { it in 20..800 } ?: return null
    val f = fat.toIntOrNull()?.takeIf { it in 10..300 } ?: return null
    return NutritionTargets(w, c, f)
}
