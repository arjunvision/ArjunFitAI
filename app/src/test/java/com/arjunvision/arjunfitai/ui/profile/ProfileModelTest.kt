package com.arjunvision.arjunfitai.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileModelTest {
    @Test fun initialsFromName() {
        assertEquals("SB", initials("Sri Balaji Aravind"))
        assertEquals("S", initials("  sri "))
        assertEquals("", initials(""))
    }

    @Test fun goalsValidateRanges() {
        val ok = parseGoals("95", "8000", "2200")
        assertEquals(Triple(95f, 8000, 2200), ok.valid)
        assertEquals(94.5f, parseGoals("94,5", "8000", "2200").valid!!.first)

        val bad = parseGoals("20", "500", "")
        assertNull(bad.valid)
        assertNotNull(bad.weightError)
        assertNotNull(bad.stepsError)
        assertNotNull(bad.kcalError)
    }

    @Test fun nutritionTargetsValidate() {
        assertEquals(NutritionTargets(3000, 250, 70), parseNutritionTargets("3000", "250", "70"))
        assertNull(parseNutritionTargets("100", "250", "70"))
        assertNull(parseNutritionTargets("3000", "", "70"))
    }

    @Test fun targetFormatting() {
        assertEquals("95 kg", formatTarget(95f))
        assertEquals("94.5 kg", formatTarget(94.5f))
    }
}
