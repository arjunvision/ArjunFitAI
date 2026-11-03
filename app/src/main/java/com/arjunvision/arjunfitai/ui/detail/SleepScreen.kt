package com.arjunvision.arjunfitai.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.data.health.model.SleepStage
import com.arjunvision.arjunfitai.ui.components.AfCard
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfMedicalFooter
import com.arjunvision.arjunfitai.ui.components.AfRing
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.components.AfSegmentedTabs
import com.arjunvision.arjunfitai.ui.components.AfSleepTimeline
import com.arjunvision.arjunfitai.ui.components.AfStatTile
import com.arjunvision.arjunfitai.ui.components.rememberFloatOffsetDp
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.dashboard.DashboardUiState
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.home.lastNightSleep
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.Instant
import java.time.ZoneId

private fun stageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.DEEP -> AfColors.Accent
    SleepStage.LIGHT -> AfColors.AccentSoft
    SleepStage.REM -> AfColors.Water
    SleepStage.AWAKE -> Color.White
    SleepStage.UNDEFINED -> AfColors.Border
}

private fun stageHeight(stage: SleepStage): Float = when (stage) {
    SleepStage.DEEP -> 1f
    SleepStage.REM -> 0.75f
    SleepStage.LIGHT -> 0.55f
    SleepStage.AWAKE -> 0.3f
    SleepStage.UNDEFINED -> 0.4f
}

/** Sleep & oxygen. [startOnOxygen] opens the Blood oxygen tab (from Home's SpO₂ tile). */
@Composable
fun SleepScreen(state: DashboardUiState, startOnOxygen: Boolean, contentPadding: PaddingValues, onBack: () -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    var tab by rememberSaveable { mutableIntStateOf(if (startOnOxygen) 1 else 0) }

    DetailColumn(contentPadding) {
        AfScreenHeader("Sleep & oxygen", onBack)
        AfSegmentedTabs(listOf("Sleep", "Blood oxygen"), tab, { tab = it })
        if (tab == 0) SleepTab(state, zone) else OxygenTab(state)
        AfMedicalFooter()
    }
}

@Composable
private fun SleepTab(state: DashboardUiState, zone: ZoneId) {
    val week = state.data.sleep
    val lastNight = lastNightSleep(week, Instant.now())
    val night = lastNight ?: week.maxByOrNull { it.start }
    val summary = sleepSummary(night, isLastNight = lastNight != null, week = week, zone = zone)
    val bob by rememberFloatOffsetDp(5f, 3000)

    Column(Modifier.fillMaxWidth().riseIn(0), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            AfRing(summary?.ringFraction ?: 0f, 180.dp, 14.dp, color = AfColors.AccentSoft, minSweepDegrees = 0f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AfIcons.Moon, contentDescription = null, tint = AfColors.AccentSoft, modifier = Modifier.size(30.dp).offset(y = bob.dp))
                Text(summary?.total ?: EMPTY, style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp), color = AfColors.Text)
                Text("Goal 7–9 hrs", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = Color.White.copy(alpha = 0.7f))
            }
        }
        Text(summary?.note ?: "No sleep recorded in the last 7 days", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
    }

    AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        AfSleepTimeline(
            summary?.timeline.orEmpty().map { stageColor(it.stage) to (it.weight to stageHeight(it.stage)) },
            Modifier.semantics { contentDescription = summary?.let { "Sleep stages from ${it.bed} to ${it.wake}" } ?: "No sleep stages" },
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(summary?.bed ?: "Bedtime $EMPTY", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
            Text(summary?.wake ?: "Wake $EMPTY", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
        }
    }

    AfCard(Modifier.fillMaxWidth().riseIn(1), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        val rows = summary?.stages ?: listOf(
            StageRow(SleepStage.DEEP, "Deep sleep", EMPTY), StageRow(SleepStage.LIGHT, "Light sleep", EMPTY),
            StageRow(SleepStage.REM, "REM", EMPTY), StageRow(SleepStage.AWAKE, "Awake", EMPTY),
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().heightIn(min = 46.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(10.dp).clip(AfShapes.Pill).background(stageColor(row.stage)))
                Text(row.name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp), color = AfColors.Text, modifier = Modifier.weight(1f))
                Text(row.value, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Color.White.copy(alpha = 0.85f))
            }
            HorizontalDivider(color = AfColors.Inner)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sleep score", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text, modifier = Modifier.weight(1f))
            val score = summary?.score
            Box(
                Modifier.clip(AfShapes.Pill).background(if (score != null) AfColors.Success else AfColors.Inner).padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    "${score ?: EMPTY} / 100",
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (score != null) AfColors.Background else Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }

    TipCard(AfIcons.Band, summary?.tip ?: NO_SLEEP_TIP, Modifier.riseIn(2))
}

@Composable
private fun OxygenTab(state: DashboardUiState) {
    val stats = oxygenStats(state.data.bloodOxygen)
    val bob by rememberFloatOffsetDp(5f, 3000)

    Column(Modifier.fillMaxWidth().riseIn(0).padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(AfIcons.Drop, contentDescription = null, tint = AfColors.Water, modifier = Modifier.size(34.dp).offset(y = bob.dp))
        Text(
            buildAnnotatedString {
                append(stats?.latest?.toString() ?: EMPTY)
                if (stats != null) withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))) { append("%") }
            },
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 62.sp),
            color = AfColors.Text,
        )
        Text(
            if (stats != null) "Latest blood oxygen (SpO₂)" else "No blood oxygen readings in the last 7 days",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f),
        )
    }

    Row(Modifier.riseIn(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AfStatTile("Latest", stats?.let { "${it.latest}%" } ?: EMPTY, Modifier.weight(1f))
        AfStatTile("Average", stats?.let { "${it.average}%" } ?: EMPTY, Modifier.weight(1f))
        AfStatTile("Lowest", stats?.let { "${it.lowest}%" } ?: EMPTY, Modifier.weight(1f))
    }

    TipCard(AfIcons.Info, "For overnight readings, turn on blood oxygen during sleep in the Galaxy Wearable app.", Modifier.riseIn(2))
}

@Composable
private fun TipCard(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(AfShapes.Card).background(AfColors.Inner).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AfColors.Accent, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = AfColors.Text)
    }
}
