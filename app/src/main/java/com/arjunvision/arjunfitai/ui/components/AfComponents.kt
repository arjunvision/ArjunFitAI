package com.arjunvision.arjunfitai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import com.arjunvision.arjunfitai.ui.theme.AfTouchTarget

/** A #181E26 card; tappable (as a button) when [onClick] is set. */
@Composable
fun AfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    shape: androidx.compose.ui.graphics.Shape = AfShapes.Card,
    color: Color = AfColors.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Round #181E26 icon button, 48dp. */
@Composable
fun AfCircleButton(icon: ImageVector, contentDescription: String, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AfTouchTarget)
            .clip(AfShapes.Pill)
            .background(AfColors.Card)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Progress ring: #252C36 track, accent arc from 12 o'clock, drawn in by [drawProgress]. */
@Composable
fun AfRing(
    fraction: Float,
    size: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    color: Color = AfColors.Accent,
    track: Color = AfColors.Inner,
    minSweepDegrees: Float = 2f,
) {
    val drawn by rememberDrawIn()
    Canvas(modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        val sweep = (fraction.coerceIn(0f, 1f) * 360f).coerceAtLeast(minSweepDegrees) * drawn
        if (sweep > 0f) {
            drawArc(color, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

/** "Arjun" + accent "Fit". */
@Composable
fun Wordmark(style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append("Arjun")
            withStyle(SpanStyle(color = AfColors.Accent)) { append("Fit") }
        },
        style = style,
        color = AfColors.Text,
        modifier = modifier,
    )
}

/** Back button + all-caps screen title ("HEALTH SYNC"). */
@Composable
fun AfScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AfCircleButton(AfIcons.Back, "Back", AfColors.Text, onBack)
        Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = AfColors.Text)
    }
}

/** Full-width accent button (54dp primary CTA by default). */
@Composable
fun AfPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconModifier: Modifier = Modifier,
    height: Dp = 54.dp,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(AfShapes.Pill)
            .background(AfColors.Accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = AfColors.OnAccent, modifier = iconModifier.size(20.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AfColors.OnAccent)
    }
}

/** Full-width outlined button (48dp). */
@Composable
fun AfOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = AfTouchTarget)
            .clip(AfShapes.Pill)
            .border(1.dp, AfColors.Border, AfShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = AfColors.Text)
    }
}

/** 38dp rounded icon well (#252C36) used in rows and tiles. */
@Composable
fun AfIconWell(icon: ImageVector, tint: Color = AfColors.AccentSoft, size: Dp = 38.dp) {
    Box(Modifier.size(size).clip(AfShapes.Inner).background(AfColors.Inner), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Accent pill used for short calls to action ("Log"). */
@Composable
fun AfAccentPill(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(AfShapes.Pill)
            .background(AfColors.Accent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = style, color = AfColors.OnAccent)
    }
}
