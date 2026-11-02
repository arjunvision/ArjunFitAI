package com.arjunvision.arjunfitai.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons copied path-for-path from the design canvas (24×24, 2px round strokes). Tint with Icon(tint). */
object AfIcons {
    val Coach = stroke("coach", "M12 3l1.8 4.6L18.5 9l-4.7 1.4L12 15l-1.8-4.6L5.5 9l4.7-1.4zM19 15l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z")
    val Profile = stroke("profile", "M8 8a4 4 0 1 0 8 0a4 4 0 1 0 -8 0", "M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7")
    val Steps = stroke("steps", "M4 16c0-3 1-5 3-5s2 2 4 2 3-1 4 0 5 1 5 4v1H4z", "M9 11V7M12 12V6")
    val Heart = stroke("heart", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z")
    val Flame = stroke("flame", "M12 3c1 3.5 5 5.5 5 10a5 5 0 0 1-10 0c0-2.4 1.2-3.9 2.5-5 .3 1.6 1 2.6 2 3 0-3.2-.4-5.5.5-8z")
    val Moon = stroke("moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    val Drop = stroke("drop", "M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z")
    val Home = stroke("home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
    val Workout = stroke("workout", "M6.5 6.5v11M17.5 6.5v11M3.5 9v6M20.5 9v6M6.5 12h11")
    val Nutrition = stroke("nutrition", "M7 3v8M5 3v5a2 2 0 0 0 4 0V3M7 11v10M17 3c-1.7 0-3 2-3 5s1.3 4 3 4v9")
    val Progress = stroke("progress", "M4 20V11M10 20V5M16 20v-6M2 21h20")
    val Back = stroke("back", "M15 5 8 12l7 7", width = 2.2f)

    private fun stroke(name: String, vararg paths: String, width: Float = 2f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
