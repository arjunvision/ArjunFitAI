package com.arjunvision.arjunfitai.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.R
import com.arjunvision.arjunfitai.ui.components.AfEasing
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.Wordmark
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfFonts
import com.arjunvision.arjunfitai.ui.theme.AfShapes

/** Logo pops in, loading bar runs while Samsung Health is read. Tap anywhere to skip. */
@Composable
fun SplashScreen(onOpen: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) pop.snapTo(1f) else pop.animateTo(1f, tween(700, easing = AfEasing))
    }
    // .6 → 1.06 (at 70%) → 1, fading in over the first 70%.
    val p = pop.value
    val scale = if (p < 0.7f) 0.6f + (1.06f - 0.6f) * (p / 0.7f) else 1.06f - 0.06f * ((p - 0.7f) / 0.3f)
    val alpha = (p / 0.7f).coerceAtMost(1f)

    Box(
        Modifier
            .fillMaxSize()
            .background(AfColors.Background)
            .clickable(role = Role.Button, onClickLabel = "Open ArjunFit", onClick = onOpen)
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 40.dp),
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.arjunfit_logo),
                contentDescription = "ArjunFit logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 150.dp, height = 133.dp).graphicsLayer {
                    scaleX = scale; scaleY = scale; this.alpha = alpha
                },
            )
            Spacer(Modifier.height(28.dp))
            Wordmark(style = MaterialTheme.typography.displayLarge, modifier = Modifier.riseIn(4))
            Text(
                "TRACK · TRAIN · EAT · IMPROVE",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = (13 * 0.12).sp),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 10.dp).riseIn(4),
            )
            Text(
                "Winter Arc",
                fontFamily = AfFonts.Satisfy,
                fontSize = 30.sp,
                color = AfColors.Accent,
                modifier = Modifier.padding(top = 18.dp).riseIn(7),
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoadingBar()
            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Reading your Fit3 data… tap to open",
                    style = MaterialTheme.typography.labelLarge,
                    color = AfColors.AccentSoft,
                )
            }
            Text("by Arjun Vision Tech Solutions", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

/** 120×4 track with a 40% segment sliding across (static when motion is reduced). */
@Composable
private fun LoadingBar() {
    val reduce = LocalReduceMotion.current
    val shift = if (reduce) null else rememberInfiniteTransition(label = "load").animateFloat(
        initialValue = -1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "loadShift",
    )
    Box(Modifier.width(120.dp).height(4.dp).clip(AfShapes.Pill).background(AfColors.Inner)) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .graphicsLayer { translationX = (shift?.value ?: 0f) * size.width }
                .clip(AfShapes.Pill)
                .background(AfColors.Accent),
        )
    }
}
