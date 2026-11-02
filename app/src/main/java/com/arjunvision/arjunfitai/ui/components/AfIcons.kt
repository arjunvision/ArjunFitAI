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
    val Chevron = stroke("chevron", "m9 5 7 7-7 7")
    val Band = stroke("band", "M11 4h2a3 3 0 0 1 3 3v10a3 3 0 0 1 -3 3h-2a3 3 0 0 1 -3 -3V7a3 3 0 0 1 3 -3z", "M10 4V2h4v2M10 20v2h4v-2")
    val HeartPulse = stroke(
        "heartPulse",
        "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z",
        "M7 11h2.5l1.2-2 2 4 1.3-2H17",
        width = 2.2f,
    )
    val Refresh = stroke("refresh", "M20 12a8 8 0 1 1-2.3-5.7L20 8", "M20 3v5h-5", width = 2.4f)
    val Goals = stroke(
        "goals",
        "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
        "M7 12a5 5 0 1 0 10 0a5 5 0 1 0 -10 0",
        "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
    )
    val Bell = stroke("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10 21h4")
    val Sliders = stroke(
        "sliders",
        "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12",
        "M14 6a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M8 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M16 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    )
    val Lock = stroke("lock", "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2H7a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2z", "M8 11V7a4 4 0 0 1 8 0v4")

    val Book = stroke("book", "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z", "M4 19V5M9 7h6")
    val Calendar = stroke("calendar", "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V7a2 2 0 0 1 2 -2z", "M3 10h18M8 3v4M16 3v4")
    val History = stroke("history", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5M12 7v5l3 2")
    val Trophy = stroke("trophy", "M8 4h8v5a4 4 0 0 1-8 0zM8 6H5v1a3 3 0 0 0 3 3M16 6h3v1a3 3 0 0 1-3 3M12 13v4M8 20h8")
    val Search = stroke("search", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0", "m20 20-3.5-3.5")
    val Filter = stroke("filter", "M4 6h16M7 12h10M10 18h4")
    val Check = stroke("check", "m5 12 5 5 9-10", width = 2.6f)
    val Up = stroke("up", "M6 15l6-6 6 6", width = 2.2f)
    val Down = stroke("down", "M6 9l6 6 6-6", width = 2.2f)
    val Minus = stroke("minus", "M5 12h14", width = 2.6f)
    val Play: ImageVector = ImageVector.Builder("play", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(addPathNodes("M7 4.5v15l12-7.5z"), fill = SolidColor(Color.Black))
    }.build()
    val Plus = stroke("plus", "M12 5v14M5 12h14", width = 2.6f)
    val Trash = stroke("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val ChevronLeft = stroke("chevronLeft", "M15 5 8 12l7 7", width = 2.2f)
    val Pin = stroke("pin", "M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z", "M10 10a2 2 0 1 0 4 0a2 2 0 1 0 -4 0")
    val Timer = stroke("timer", "M4 13a8 8 0 1 0 16 0a8 8 0 1 0 -16 0", "M12 9v4l2 2M9 2h6")
    val Info = stroke("info", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M12 8h.01M11 12h1v4h1")
    val HeartFilled: ImageVector = ImageVector.Builder("heartFilled", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(addPathNodes("M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"), fill = SolidColor(Color.Black))
    }.build()

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
