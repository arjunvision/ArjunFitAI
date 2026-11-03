package com.arjunvision.arjunfitai.ui.nutrition

import com.arjunvision.arjunfitai.data.local.FoodEntry
import com.arjunvision.arjunfitai.data.local.WaterEntry
import com.arjunvision.arjunfitai.data.health.model.NutritionRecord
import com.arjunvision.arjunfitai.data.health.model.WaterRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class NutritionModelTest {
    @Test fun combinesBothSourcesAndLabelsThem() {
        val local = listOf(FoodEntry(1, 2_000, "Idli", 116f, 4f, 24f, 0.4f))
        val samsung = listOf(NutritionRecord(Instant.ofEpochMilli(3_000), "Lunch", 600f, 30f, 70f, 20f), NutritionRecord(Instant.ofEpochMilli(1_000), null, null, null, null, null))
        val day = dayNutrition(local, samsung, listOf(WaterEntry(1, 1, 250), WaterEntry(2, 2, 250)), listOf(WaterRecord(Instant.EPOCH, 500.4f)))

        assertEquals(listOf("Lunch", "Idli", "Food"), day.items.map { it.name })
        assertEquals(listOf(Source.SAMSUNG, Source.ARJUNFIT, Source.SAMSUNG), day.items.map { it.source })
        assertEquals(716f, day.kcal, 0.01f)
        assertEquals(34f, day.proteinG, 0.01f)
        assertEquals(1000, day.waterMl)
        assertEquals(local.first(), day.items[1].local)
    }

    @Test fun waterCupsRoundAndCap() {
        assertEquals(0, filledCups(0, 3000))
        assertEquals(3, filledCups(1000, 3000))
        assertEquals(8, filledCups(5000, 3000))
    }

    @Test fun quickFoodsMatchCanvasChips() {
        assertEquals(listOf("Idli", "Dal", "Eggs", "Chicken", "Fish", "Sambar", "Rice"), QuickFoods.map { it.name })
    }
}
