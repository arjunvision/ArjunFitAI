package com.arjunvision.arjunfitai.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfEmptyChart
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.detail.DetailColumn
import com.arjunvision.arjunfitai.ui.theme.AfColors
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(sessions: List<SessionEntity>, sets: List<SetEntity>, contentPadding: PaddingValues, onBack: () -> Unit, onDelete: (SessionEntity) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val finished = sessions.filter { it.end != null }
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<SessionEntity?>(null) }

    DetailColumn(contentPadding) {
        AfScreenHeader("History", onBack)
        if (finished.isEmpty()) AfEmptyChart("Finish a workout and it shows here")
        finished.forEach { s ->
            val summary = sessionSummary(s, sets)
            val expanded = open == s.id
            AfCard(Modifier.fillMaxWidth(), onClick = { open = if (expanded) null else s.id }, onClickLabel = if (expanded) "Hide sets" else "Show sets", padding = PaddingValues(16.dp)) {
                Text(
                    Instant.ofEpochMilli(s.start).atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.ENGLISH)),
                    style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted,
                )
                Text(s.planName, style = MaterialTheme.typography.titleMedium, color = AfColors.Text)
                Text(
                    "${summary.minutes} min · ${summary.setsDone} sets" + if (summary.volumeKg > 0f) " · ${NumberFormat.getIntegerInstance(Locale.ENGLISH).format(summary.volumeKg.roundToInt())} kg volume" else "",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted,
                )
                if (expanded) {
                    val done = sets.filter { it.sessionId == s.id && it.done }.groupBy { it.exerciseName }
                    Column(Modifier.padding(top = 10.dp)) {
                        done.entries.forEachIndexed { i, (name, list) ->
                            if (i > 0) HorizontalDivider(color = AfColors.Inner)
                            Column(Modifier.padding(vertical = 8.dp)) {
                                Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
                                Text(list.joinToString("  ·  ") { setText(it) }, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                            }
                        }
                        Text("Delete workout", style = MaterialTheme.typography.labelLarge, color = AfColors.TextMuted,
                            modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp).clickable(role = Role.Button) { deleting = s }.padding(12.dp))
                    }
                }
            }
        }
    }
    deleting?.let { s ->
        AfDialog("Delete this workout?", { deleting = null }, "Delete" to { onDelete(s); deleting = null }) {
            Text("${s.planName} and its sets are removed from ArjunFit.")
        }
    }
}

@Composable
fun PrsScreen(sessions: List<SessionEntity>, sets: List<SetEntity>, contentPadding: PaddingValues, onBack: () -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val prs = personalRecords(sessions, sets)
    DetailColumn(contentPadding) {
        AfScreenHeader("Personal records", onBack)
        if (prs.isEmpty()) AfEmptyChart("Finish a workout with weights to set your first PR")
        else Text("Heaviest set per exercise · est. 1RM by the Epley formula", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
        if (prs.isNotEmpty()) {
            AfCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                prs.forEachIndexed { i, pr ->
                    if (i > 0) HorizontalDivider(color = AfColors.Inner)
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(pr.exerciseName, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
                            Text(pr.date.atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) + " · est. 1RM ${"%.1f".format(Locale.ENGLISH, pr.estimated1Rm)} kg",
                                style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
                        }
                        Text("${"%.1f".format(Locale.ENGLISH, pr.kg).removeSuffix(".0")} kg × ${pr.reps}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.Accent)
                    }
                }
            }
        }
    }
}
