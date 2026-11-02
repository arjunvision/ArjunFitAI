package com.arjunvision.arjunfitai.ui.connect

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.components.AfScreenHeader
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.home.EMPTY
import com.arjunvision.arjunfitai.ui.home.formatSteps
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * First-run "Connect device". The band's screen shows the real time, latest heart rate and today's
 * steps when we have them (never sample numbers).
 */
@Composable
fun ConnectScreen(
    heartRateBpm: Int?,
    steps: Long?,
    stepGoal: Int,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onConnect: () -> Unit,
) {
    var showHow by rememberSaveable { mutableStateOf(false) }

    // Scrolls on small screens; on tall ones the buttons sit at the bottom (weight uses the min height).
    BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding)) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AfScreenHeader("Connect device", onBack)

        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            PulseRing()
            Box(Modifier.size(230.dp).clip(AfShapes.Pill).background(AfColors.Card))
            FloatingBand(heartRateBpm, steps, stepGoal)
        }

        Column(Modifier.fillMaxWidth().riseIn(0), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Samsung Galaxy Fit3", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AfColors.Text)
            Text(
                "Your band syncs to Samsung Health, ArjunFit reads it",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        Column(Modifier.riseIn(1), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CheckLine("Steps, heart rate, sleep, SpO₂ and workouts")
            CheckLine("Updates each time your band syncs")
            CheckLine("Read-only · data stays on your phone")
        }

        Spacer(Modifier.weight(1f, fill = true))

        Column(Modifier.riseIn(2), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AfPrimaryButton("Connect with Samsung Health", onConnect, icon = AfIcons.HeartPulse)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { showHow = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("How does it work?", style = MaterialTheme.typography.labelLarge, color = AfColors.AccentSoft)
            }
        }
    }
    }

    if (showHow) {
        AlertDialog(
            onDismissRequest = { showHow = false },
            confirmButton = { TextButton(onClick = { showHow = false }) { Text("Got it", color = AfColors.AccentSoft) } },
            title = { Text("How ArjunFit gets your data") },
            text = {
                Text(
                    "Your Galaxy Fit3 syncs to the Samsung Health app on this phone. ArjunFit asks Samsung Health " +
                        "for read-only access and reads it from there — it never writes to Samsung Health and your " +
                        "health data never leaves this phone. New readings appear after each band sync.",
                )
            },
            containerColor = AfColors.Card,
            titleContentColor = AfColors.Text,
            textContentColor = AfColors.TextMuted,
        )
    }
}

@Composable
private fun CheckLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(24.dp).clip(AfShapes.Pill).background(AfColors.Success), contentAlignment = Alignment.Center) {
            Text("✓", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AfColors.Background)
        }
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp), color = AfColors.Text)
    }
}

/** Accent ring that grows and fades behind the band (2.2s). */
@Composable
private fun PulseRing() {
    val reduce = LocalReduceMotion.current
    val t = if (reduce) null else rememberInfiniteTransition(label = "ring").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing)), label = "ringT",
    )
    val p = t?.value ?: 0f
    Box(
        Modifier
            .size(200.dp)
            .graphicsLayer {
                val s = 0.8f + 0.7f * p
                scaleX = s; scaleY = s
                alpha = if (reduce) 0.35f else 0.7f * (1f - p)
            }
            .border(2.dp, AfColors.Accent, AfShapes.Pill),
    )
}

/** Galaxy Fit3 drawing (120×250, from the canvas) bobbing 8dp every 3.2s. */
@Composable
private fun FloatingBand(heartRateBpm: Int?, steps: Long?, stepGoal: Int) {
    val reduce = LocalReduceMotion.current
    val bob = if (reduce) null else rememberInfiniteTransition(label = "float").animateFloat(
        0f, -8f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "floatY",
    )
    val time = remember { DateTimeFormatter.ofPattern("HH:mm").format(LocalTime.now()) }
    val progress = if (steps != null && stepGoal > 0) (steps.toFloat() / stepGoal).coerceIn(0f, 1f) else 0f

    Box(
        Modifier
            .size(width = 120.dp, height = 250.dp)
            .graphicsLayer { translationY = (bob?.value ?: 0f) * density }
            .semantics { contentDescription = "Galaxy Fit3 band" },
    ) {
        val strap = RoundedCornerShape(14.dp)
        Box(Modifier.offset(38.dp, 0.dp).size(44.dp, 80.dp).clip(strap).background(AfColors.Inner))
        Box(Modifier.offset(38.dp, 170.dp).size(44.dp, 80.dp).clip(strap).background(AfColors.Inner))
        Box(
            Modifier.offset(22.dp, 58.dp).size(76.dp, 134.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xFF0B0F14))
                .border(3.dp, Color(0xFF3A4350), RoundedCornerShape(24.dp)),
        )
        Column(
            Modifier.offset(31.dp, 68.dp).size(58.dp, 114.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black)
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(time, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 18.sp)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(AfIcons.Heart, contentDescription = null, tint = AfColors.Accent, modifier = Modifier.size(12.dp))
                Text(heartRateBpm?.toString() ?: EMPTY, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 14.sp)
            }
            Text(
                steps?.let(::formatSteps) ?: EMPTY,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AfColors.AccentSoft, lineHeight = 14.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Box(Modifier.padding(top = 6.dp).size(40.dp, 5.dp).clip(AfShapes.Pill).background(AfColors.Inner)) {
                Box(Modifier.width(40.dp * progress).height(5.dp).clip(AfShapes.Pill).background(AfColors.Accent))
            }
        }
    }
}
