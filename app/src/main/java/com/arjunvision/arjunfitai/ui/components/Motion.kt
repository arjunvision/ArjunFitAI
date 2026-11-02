package com.arjunvision.arjunfitai.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The canvas's cubic-bezier(.2,.8,.2,1). */
val AfEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * True when the system "Remove animations" setting is on (animator duration scale 0).
 * [ValueAnimator.areAnimatorsEnabled] is the platform's own check (API 26+).
 */
fun systemReducesMotion(): Boolean = !ValueAnimator.areAnimatorsEnabled()

val LocalReduceMotion = staticCompositionLocalOf { false }

/** Cards rise in: fade + 14dp lift, staggered 80ms by [order]. */
fun Modifier.riseIn(order: Int = 0): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) progress.snapTo(1f) else {
            delay(80L * order)
            progress.animateTo(1f, tween(500, easing = AfEasing))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

/** Draw-in progress for rings and arcs: 0 → 1 over 1.2s after a 250ms delay. */
@Composable
fun rememberDrawIn(durationMillis: Int = 1200, delayMillis: Long = 250): State<Float> {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) progress.snapTo(1f) else {
            delay(delayMillis)
            progress.animateTo(1f, tween(durationMillis, easing = AfEasing))
        }
    }
    return progress.asState()
}

/** Sync-dot pulse: opacity 1 ↔ 0.35 every 1.6s. Static when motion is reduced. */
@Composable
fun rememberPulseAlpha(): State<Float> {
    if (LocalReduceMotion.current) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
}

/** Heart beat: a quick double bump once a second. Static when motion is reduced. */
@Composable
fun rememberHeartBeatScale(): State<Float> {
    if (LocalReduceMotion.current) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "heart").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1000
                1f at 0 using LinearEasing
                1.18f at 120 using LinearEasing
                1f at 240 using LinearEasing
                1.12f at 360 using LinearEasing
                1f at 520
            }
        ),
        label = "heartScale",
    )
}
