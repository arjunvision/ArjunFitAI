package com.arjunvision.arjunfitai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
