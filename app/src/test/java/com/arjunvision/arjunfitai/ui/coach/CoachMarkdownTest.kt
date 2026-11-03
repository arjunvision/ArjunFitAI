package com.arjunvision.arjunfitai.ui.coach

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachMarkdownTest {
    @Test fun boldBulletsAndNoRawAsterisks() {
        val out = coachMarkdown("- **Chicken pepper fry:** 200 g raw chicken\n- **1 bowl dal** (~8–10 g protein)\n\n**Vegetarian swap:** paneer bhurji")
        assertEquals("• Chicken pepper fry: 200 g raw chicken\n• 1 bowl dal (~8–10 g protein)\n\nVegetarian swap: paneer bhurji", out.text)
        val bold = out.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }.map { out.text.substring(it.start, it.end) }
        assertEquals(listOf("Chicken pepper fry:", "1 bowl dal", "Vegetarian swap:"), bold)
    }

    @Test fun italicHeadingsNumbersAndLoneAsterisks() {
        val out = coachMarkdown("## Plan\n1. Squat *slowly*\n2 * 3 = 6")
        assertEquals("Plan\n1. Squat slowly\n2 * 3 = 6", out.text)
        assertTrue(out.spanStyles.any { it.item.fontStyle == FontStyle.Italic && out.text.substring(it.start, it.end) == "slowly" })
    }

    @Test fun unmatchedMarkersStay() {
        assertEquals("**not closed", coachMarkdown("**not closed").text)
    }
}
