package com.arjunvision.arjunfitai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import kotlin.math.roundToInt

/** Pill segmented control (Day / Week / Month). */
@Composable
fun AfSegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(AfShapes.Pill).background(AfColors.Card).padding(4.dp).selectableGroup(),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(AfShapes.Pill)
                    .background(if (on) AfColors.Accent else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) AfColors.OnAccent else Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

/** Dashed empty-state box with a one-line explanation (canvas "demo off"). */
@Composable
fun AfEmptyChart(text: String, modifier: Modifier = Modifier, height: Dp = 140.dp) {
    Box(
        modifier.fillMaxWidth().height(height).clip(AfShapes.Inner)
            .border(1.dp, AfColors.Border, AfShapes.Inner).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AfColors.TextMuted, textAlign = TextAlign.Center)
    }
}

/** Hour-by-hour line with a soft area under it; gaps where an hour had no data. Reveals left to right. */
@Composable
fun AfHourlyLine(values: List<Float?>, yMin: Float, yMax: Float, description: String, modifier: Modifier = Modifier) {
    val reveal by rememberDrawIn()
    Canvas(modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        listOf(0.2f, 0.5f, 0.8f).forEach { f -> drawLine(AfColors.Inner, Offset(0f, h * f), Offset(w, h * f), 1.dp.toPx()) }
        fun x(i: Int) = if (values.size <= 1) 0f else w * i / (values.size - 1)
        fun y(v: Float) = h - ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f) * h

        // Runs of consecutive hours with data.
        val runs = mutableListOf<List<Pair<Int, Float>>>()
        var run = mutableListOf<Pair<Int, Float>>()
        values.forEachIndexed { i, v ->
            if (v != null) run.add(i to v) else if (run.isNotEmpty()) { runs.add(run); run = mutableListOf() }
        }
        if (run.isNotEmpty()) runs.add(run)

        clipRect(right = w * reveal) {
            runs.forEach { r ->
                if (r.size == 1) {
                    val (i, v) = r.first()
                    drawCircle(AfColors.Accent, 3.dp.toPx(), Offset(x(i), y(v)))
                    return@forEach
                }
                val line = Path().apply { r.forEachIndexed { k, (i, v) -> if (k == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) } }
                val area = Path().apply {
                    addPath(line)
                    lineTo(x(r.last().first), h); lineTo(x(r.first().first), h); close()
                }
                drawPath(area, AfColors.Accent.copy(alpha = 0.18f))
                drawPath(line, AfColors.Accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** One low–high capsule per day (null = no data). Grows from the middle. */
@Composable
fun AfRangeColumns(ranges: List<Pair<Float, Float>?>, yMin: Float, yMax: Float, description: String, modifier: Modifier = Modifier) {
    val grow by rememberDrawIn(1000, 200)
    Canvas(modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        listOf(0.2f, 0.5f, 0.8f).forEach { f -> drawLine(AfColors.Inner, Offset(0f, h * f), Offset(w, h * f), 1.dp.toPx()) }
        if (ranges.isEmpty()) return@Canvas
        val slot = w / ranges.size
        val barW = (slot * 0.5f).coerceIn(3.dp.toPx(), 14.dp.toPx())
        fun y(v: Float) = h - ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f) * h
        ranges.forEachIndexed { i, r ->
            r ?: return@forEachIndexed
            val cx = slot * i + slot / 2
            val top = y(r.second)
            val bottom = y(r.first)
            val mid = (top + bottom) / 2
            val half = ((bottom - top) / 2 * grow).coerceAtLeast(barW / 2)
            drawRoundRect(
                AfColors.AccentSoft,
                topLeft = Offset(cx - barW / 2, mid - half),
                size = Size(barW, half * 2),
                cornerRadius = CornerRadius(barW / 2),
            )
        }
    }
}

/** Vertical bars with labels; [colorFor] picks each bar's colour. Bars grow from zero. */
@Composable
fun <T> AfBars(
    items: List<T>,
    value: (T) -> Float,
    label: (T) -> String,
    colorFor: (T) -> Color,
    emphasized: (T) -> Boolean,
    scaleMax: Float,
    description: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
) {
    val grow by rememberDrawIn(1000, 200)
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(Modifier.fillMaxWidth().height(chartHeight), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            items.forEach { item ->
                val f = if (scaleMax > 0f) (value(item) / scaleMax).coerceIn(0f, 1f) else 0f
                Box(
                    Modifier.weight(1f).height(maxOf(6.dp, chartHeight * f * grow))
                        .clip(AfShapes.Inner).background(colorFor(item)),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Text(
                    label(item),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (emphasized(item)) FontWeight.Bold else FontWeight.Medium),
                    color = if (emphasized(item)) AfColors.Text else AfColors.TextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Horizontal track [min..max] with a soft range bar [low..high] and a white marker at [marker]. */
@Composable
fun AfRangeBar(low: Float, high: Float, marker: Float?, min: Float, max: Float, modifier: Modifier = Modifier) {
    val grow by rememberDrawIn(900, 150)
    Canvas(modifier.fillMaxWidth().height(24.dp)) {
        val w = size.width
        val trackTop = (size.height - 14.dp.toPx()) / 2
        val r = CornerRadius(7.dp.toPx())
        fun x(v: Float) = ((v - min) / (max - min)).coerceIn(0f, 1f) * w
        drawRoundRect(AfColors.Inner, Offset(0f, trackTop), Size(w, 14.dp.toPx()), r)
        val l = x(low)
        val rr = x(high)
        val width = ((rr - l) * grow).coerceAtLeast(14.dp.toPx())
        drawRoundRect(AfColors.AccentSoft, Offset(l, trackTop), Size(width, 14.dp.toPx()), r)
        marker?.let {
            val mx = x(it) - 2.dp.toPx()
            drawRoundRect(Color.White, Offset(mx, 0f), Size(4.dp.toPx(), size.height), CornerRadius(2.dp.toPx()))
        }
    }
}

/** Sleep stages as one bar: segment width = time, height = depth (deep tallest). */
@Composable
fun AfSleepTimeline(segments: List<Pair<Color, Pair<Float, Float>>>, modifier: Modifier = Modifier) {
    // Each segment: colour to (weight, heightFraction).
    val grow by rememberDrawIn(900, 150)
    Box(modifier.fillMaxWidth().height(26.dp).clip(RoundedEight).background(AfColors.Inner).clipToBounds()) {
        Layout(content = {
            segments.forEach { (color, wh) ->
                Box(Modifier.fillMaxHeight(wh.second * grow).background(color))
            }
        }) { measurables, constraints ->
            val gap = 2.dp.roundToPx()
            val total = segments.sumOf { it.second.first.toDouble() }.toFloat().coerceAtLeast(1f)
            val usable = (constraints.maxWidth - gap * (segments.size - 1).coerceAtLeast(0)).coerceAtLeast(0)
            val widths = segments.map { (usable * it.second.first / total).roundToInt().coerceAtLeast(1) }
            val placeables = measurables.mapIndexed { i, m ->
                m.measure(constraints.copy(minWidth = widths[i], maxWidth = widths[i], minHeight = 0))
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                var x = 0
                placeables.forEach { p ->
                    p.place(x, constraints.maxHeight - p.height)
                    x += p.width + gap
                }
            }
        }
    }
}

private val RoundedEight = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)

/** Small stat tile: muted label over a bold value (Heart rate / SpO₂ grids). */
@Composable
fun AfStatTile(label: String, value: String, modifier: Modifier = Modifier, unit: String? = null) {
    Column(modifier.clip(AfShapes.Tile).background(AfColors.Card).padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
        Text(
            buildAnnotatedString {
                append(value)
                if (unit != null) withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)) { append(" $unit") }
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
            color = AfColors.Text,
        )
    }
}

/** "For fitness tracking, not medical diagnosis". */
@Composable
fun AfMedicalFooter(modifier: Modifier = Modifier) {
    Text(
        "For fitness tracking, not medical diagnosis",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Line + soft area over time. [points] are (x 0..1, value). Draws in left to right.
 */
@Composable
fun AfTimeLine(points: List<Pair<Float, Float>>, yMin: Float, yMax: Float, description: String, modifier: Modifier = Modifier) {
    val reveal by rememberDrawIn(1400, 200)
    Canvas(modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        listOf(0f, 1f / 3, 2f / 3, 1f).forEach { f ->
            val y = (h * f).coerceIn(0.5f, h - 0.5f)
            drawLine(AfColors.Inner, Offset(0f, y), Offset(w, y), 1.dp.toPx())
        }
        if (points.isEmpty()) return@Canvas
        val pad = 3.dp.toPx()
        fun x(f: Float) = pad + f.coerceIn(0f, 1f) * (w - 2 * pad)
        fun y(v: Float) = h - ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f) * h
        val line = Path().apply { points.forEachIndexed { i, (fx, v) -> if (i == 0) moveTo(x(fx), y(v)) else lineTo(x(fx), y(v)) } }
        val area = Path().apply {
            addPath(line)
            lineTo(x(points.last().first), h); lineTo(x(points.first().first), h); close()
        }
        clipRect(right = w * reveal) {
            drawPath(area, AfColors.Accent.copy(alpha = 0.16f))
            drawPath(line, AfColors.Accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.forEach { (fx, v) -> drawCircle(AfColors.Accent, 3.dp.toPx(), Offset(x(fx), y(v))) }
        }
    }
}

/** "ArjunFit" / "Samsung Health" label, so every entry shows where it came from. */
@Composable
fun AfSourcePill(label: String, fromSamsung: Boolean, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        color = if (fromSamsung) AfColors.Water else AfColors.AccentSoft,
        modifier = modifier.clip(AfShapes.Pill).background(AfColors.Inner).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
