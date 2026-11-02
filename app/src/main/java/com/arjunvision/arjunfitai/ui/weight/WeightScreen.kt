package com.arjunvision.arjunfitai.ui.weight

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.local.LocalLogDatabase
import com.arjunvision.arjunfitai.data.local.WeightEntry
import com.arjunvision.arjunfitai.data.settings.UserSettings
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfEmptyChart
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.components.AfSourcePill
import com.arjunvision.arjunfitai.ui.components.AfStatTile
import com.arjunvision.arjunfitai.ui.components.AfTextField
import com.arjunvision.arjunfitai.ui.components.AfTimeLine
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.detail.DetailColumn
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.home.WinterArc
import com.arjunvision.arjunfitai.ui.home.weightNote
import com.arjunvision.arjunfitai.ui.home.weightProgress
import com.arjunvision.arjunfitai.ui.nutrition.Source
import com.arjunvision.arjunfitai.ui.profile.formatTarget
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfEyebrow
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun WeightScreen(
    weighIns: List<WeighIn>,
    settings: UserSettings,
    db: LocalLogDatabase,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val scope = rememberCoroutineScope()
    var showLog by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WeightEntry?>(null) }

    val arcStart = WinterArc.start(today.year)
    val progress = weightProgress(weighIns.asRecords(), settings.targetWeightKg, arcStart, zone)
    val chart = weightChart(weighIns, today, zone)

    DetailColumn(contentPadding) {
        AfScreenHeader("Weight tracker", onBack)

        Row(Modifier.riseIn(0), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AfCard(Modifier.weight(1f), padding = PaddingValues(16.dp)) {
                Text("Current weight", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
                BigKg(progress?.currentKg?.let { "%.1f".format(Locale.ENGLISH, it) } ?: EMPTY, AfColors.Text)
                val change = progress?.changeKg
                Text(
                    when {
                        progress == null -> "Log your first weigh-in"
                        change == null -> weightNote(progress, arcStart)
                        else -> weightNote(progress, arcStart).substringBefore(" · ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (change != null && change < 0f) AfColors.Success else AfColors.TextMuted,
                )
            }
            AfCard(Modifier.weight(1f), padding = PaddingValues(16.dp)) {
                Text("Target", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
                BigKg(formatTarget(settings.targetWeightKg).removeSuffix(" kg"), AfColors.Accent)
                Text("By Dec 31 · Winter Arc", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
            }
        }

        AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(chart?.title ?: "Trend", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text, modifier = Modifier.weight(1f))
                Text("kg", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted)
            }
            Column(Modifier.padding(top = 8.dp)) {
                if (chart == null) {
                    AfEmptyChart("Log two weigh-ins and your trend line appears here", height = 210.dp)
                } else {
                    val span = Duration.between(chart.from.atStartOfDay(zone), chart.to.plusDays(1).atStartOfDay(zone)).seconds.toFloat()
                    val start = chart.from.atStartOfDay(zone).toInstant()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.width(30.dp).height(190.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                            (0..3).map { chart.yMax - (chart.yMax - chart.yMin) * it / 3f }.forEach {
                                Text("%.0f".format(Locale.ENGLISH, it), style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                            }
                        }
                        AfTimeLine(
                            chart.points.map { Duration.between(start, it.time).seconds / span to it.kg },
                            chart.yMin, chart.yMax,
                            "Weight from %.1f to %.1f kg".format(Locale.ENGLISH, chart.points.first().kg, chart.points.last().kg),
                            Modifier.weight(1f),
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 38.dp, top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        val days = java.time.temporal.ChronoUnit.DAYS.between(chart.from, chart.to)
                        (0..3).map { chart.from.plusDays(days * it / 3) }.forEach {
                            Text(it.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)), style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                        }
                    }
                }
            }
        }

        Row(Modifier.riseIn(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val start = progress?.let { p -> p.changeKg?.let { p.currentKg - it } }
            AfStatTile("Start", start?.let(::kg) ?: EMPTY, Modifier.weight(1f))
            AfStatTile("Current", progress?.currentKg?.let(::kg) ?: EMPTY, Modifier.weight(1f))
            AfStatTile("To go", progress?.let { kg((it.currentKg - settings.targetWeightKg).coerceAtLeast(0f)) } ?: EMPTY, Modifier.weight(1f))
        }

        if (weighIns.isNotEmpty()) {
            Text("RECENT WEIGH-INS", style = AfEyebrow, color = AfColors.TextMuted)
            AfCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                weighIns.take(6).forEachIndexed { i, w ->
                    if (i > 0) HorizontalDivider(color = AfColors.Inner)
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(kg(w.kg), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
                            Text(
                                w.time.atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH)),
                                style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted,
                            )
                        }
                        AfSourcePill(w.source.label, fromSamsung = w.source == Source.SAMSUNG)
                        w.local?.let { entry ->
                            Box(
                                Modifier.size(48.dp).clip(AfShapes.Pill).clickable(role = Role.Button, onClickLabel = "Delete weigh-in") { deleting = entry },
                                contentAlignment = Alignment.Center,
                            ) { Icon(AfIcons.Trash, contentDescription = "Delete weigh-in", tint = AfColors.TextMuted, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }

        AfPrimaryButton("Log weight", onClick = { showLog = true }, icon = AfIcons.Plus)
        Text(
            "Weigh in the morning, same time, for a fair trend",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showLog) {
        var text by rememberSaveable { mutableStateOf(progress?.currentKg?.let { "%.1f".format(Locale.ENGLISH, it) } ?: "") }
        val value = parseWeightKg(text)
        AfDialog(
            title = "Log weight",
            onDismiss = { showLog = false },
            confirm = "Save" to {
                value?.let { kgValue -> scope.launch { db.weight().insert(WeightEntry(time = System.currentTimeMillis(), kg = kgValue)) } }
                showLog = false
            },
            confirmEnabled = value != null,
        ) {
            Text("Saved in ArjunFit on this phone. Samsung Health is not changed.", style = MaterialTheme.typography.bodySmall)
            AfTextField(text, { text = it }, "Weight (kg)", KeyboardOptions(keyboardType = KeyboardType.Decimal), if (text.isNotEmpty() && value == null) "Enter 30–300 kg" else null)
        }
    }
    deleting?.let { entry ->
        AfDialog(
            title = "Delete this weigh-in?",
            onDismiss = { deleting = null },
            confirm = "Delete" to { scope.launch { db.weight().delete(entry) }; deleting = null },
        ) { Text("${kg(entry.kg)} logged in ArjunFit. Samsung Health is not changed.") }
    }
}

@Composable
private fun BigKg(value: String, color: Color) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append(value) }
            if (value != EMPTY) withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f))) { append(" kg") }
        },
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 34.sp),
    )
}

private fun kg(v: Float): String = "%.1f kg".format(Locale.ENGLISH, abs(v))
