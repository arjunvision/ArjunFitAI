package com.arjunvision.arjunfitai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.arjunfitai.R

// ArjunFit design system — the single source for colours, type and shape.
// Values come from the "ArjunFit App Design" canvas (Arjun Vision orange).

object AfColors {
    val Background = Color(0xFF121820)
    val Card = Color(0xFF181E26)
    val Inner = Color(0xFF252C36)
    val Border = Color(0xFF2C3440)

    val Accent = Color(0xFFF57C00)
    val AccentSoft = Color(0xFFFF914C)
    val OnAccent = Color(0xFF252525)

    val Success = Color(0xFF00B894)
    /** Water and REM sleep. */
    val Water = Color(0xFF4AA8FF)

    val Text = Color.White
    val TextMuted = Color.White.copy(alpha = 0.65f)
}

object AfFonts {
    val Poppins = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
        Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
    )

    /** Script accent — use for ONE word or phrase per screen. */
    val Satisfy = FontFamily(Font(R.font.satisfy_regular, FontWeight.Normal))
}

object AfShapes {
    val Card = RoundedCornerShape(20.dp)
    val Tile = RoundedCornerShape(18.dp)
    val Inner = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Minimum touch target for anything tappable. */
val AfTouchTarget = 48.dp

private val Colors = darkColorScheme(
    primary = AfColors.Accent,
    onPrimary = AfColors.OnAccent,
    secondary = AfColors.AccentSoft,
    onSecondary = AfColors.OnAccent,
    tertiary = AfColors.Success,
    background = AfColors.Background,
    onBackground = AfColors.Text,
    surface = AfColors.Background,
    onSurface = AfColors.Text,
    surfaceVariant = AfColors.Card,
    onSurfaceVariant = AfColors.TextMuted,
    surfaceContainerLowest = AfColors.Background,
    surfaceContainerLow = AfColors.Card,
    surfaceContainer = AfColors.Card,
    surfaceContainerHigh = AfColors.Card,
    surfaceContainerHighest = AfColors.Inner,
    outline = AfColors.Border,
    outlineVariant = AfColors.Inner,
)

private fun poppins(size: Int, weight: FontWeight, lineHeight: Float = 1.3f, tracking: Float = 0f) = TextStyle(
    fontFamily = AfFonts.Poppins,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = (size * tracking).sp,
)

private val Type = Typography(
    displayLarge = poppins(44, FontWeight.ExtraBold, 1.0f, -0.02f),
    headlineLarge = poppins(28, FontWeight.ExtraBold, 1.15f, -0.01f),
    headlineMedium = poppins(24, FontWeight.Bold, 1.25f),
    headlineSmall = poppins(22, FontWeight.ExtraBold, 1.2f, -0.01f),
    titleLarge = poppins(22, FontWeight.ExtraBold, 1.2f, -0.01f),
    titleMedium = poppins(16, FontWeight.Bold),
    titleSmall = poppins(15, FontWeight.SemiBold),
    bodyLarge = poppins(15, FontWeight.Normal),
    bodyMedium = poppins(13, FontWeight.Normal),
    bodySmall = poppins(11, FontWeight.Normal),
    labelLarge = poppins(13, FontWeight.SemiBold),
    labelMedium = poppins(12, FontWeight.SemiBold),
    labelSmall = poppins(11, FontWeight.Medium),
)

/** All-caps eyebrow labels ("WINTER ARC WEIGHT"). */
val AfEyebrow = poppins(11, FontWeight.Bold, 1.3f, 0.08f)

/** The single script word per screen ("Let's lift"). */
val AfScript = TextStyle(fontFamily = AfFonts.Satisfy, fontSize = 28.sp, lineHeight = 31.sp, color = AfColors.Accent)

@Composable
fun ArjunFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = Type,
        shapes = Shapes(medium = AfShapes.Card, large = AfShapes.Card),
        content = content,
    )
}
