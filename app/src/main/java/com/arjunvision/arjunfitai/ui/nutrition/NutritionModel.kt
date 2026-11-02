package com.arjunvision.arjunfitai.ui.nutrition

import com.arjunvision.arjunfitai.data.local.FoodEntry
import com.arjunvision.arjunfitai.data.local.WaterEntry
import com.arjunvision.arjunfitai.data.samsung.model.NutritionRecord
import com.arjunvision.arjunfitai.data.samsung.model.WaterRecord
import java.time.Instant
import kotlin.math.roundToInt

// Pure Nutrition logic (unit-tested). Entries come from two places and are always labelled.

enum class Source(val label: String) { ARJUNFIT("ArjunFit"), SAMSUNG("Samsung Health") }

data class FoodItem(
    val time: Instant,
    val name: String,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val source: Source,
    /** Set for ArjunFit entries, which can be deleted; Samsung Health entries are read-only. */
    val local: FoodEntry? = null,
)

data class DayNutrition(
    val items: List<FoodItem>,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val waterMl: Int,
)

fun dayNutrition(local: List<FoodEntry>, samsung: List<NutritionRecord>, localWater: List<WaterEntry>, samsungWater: List<WaterRecord>): DayNutrition {
    val items = local.map {
        FoodItem(Instant.ofEpochMilli(it.time), it.name, it.kcal, it.proteinG, it.carbsG, it.fatG, Source.ARJUNFIT, it)
    } + samsung.map {
        FoodItem(it.time, it.title?.takeIf(String::isNotBlank) ?: "Food", it.kcal ?: 0f, it.proteinG ?: 0f, it.carbsG ?: 0f, it.fatG ?: 0f, Source.SAMSUNG)
    }
    return DayNutrition(
        items = items.sortedByDescending { it.time },
        kcal = items.sumOf { it.kcal.toDouble() }.toFloat(),
        proteinG = items.sumOf { it.proteinG.toDouble() }.toFloat(),
        carbsG = items.sumOf { it.carbsG.toDouble() }.toFloat(),
        fatG = items.sumOf { it.fatG.toDouble() }.toFloat(),
        waterMl = localWater.sumOf { it.ml } + samsungWater.sumOf { it.ml.toDouble() }.roundToInt(),
    )
}

/** Of the 8 water cups, how many are full. */
fun filledCups(waterMl: Int, goalMl: Int, cups: Int = 8): Int =
    if (goalMl <= 0) 0 else ((waterMl.toFloat() / goalMl).coerceIn(0f, 1f) * cups).roundToInt()

/**
 * Quick-add foods (one typical serving). Approximate values, shown in the log-food form so you can
 * correct them before saving. Eggs, chicken, fish and rice: USDA FoodData Central figures;
 * idli, dal and sambar: typical home-recipe estimates.
 */
data class QuickFood(val name: String, val serving: String, val kcal: Float, val proteinG: Float, val carbsG: Float, val fatG: Float)

val QuickFoods = listOf(
    QuickFood("Idli", "2 idli (≈80 g)", 116f, 4f, 24f, 0.4f),
    QuickFood("Dal", "1 katori (≈150 g)", 150f, 9f, 20f, 4f),
    QuickFood("Eggs", "2 large, boiled", 156f, 12.6f, 1.1f, 10.6f),
    QuickFood("Chicken", "100 g breast, cooked", 165f, 31f, 0f, 3.6f),
    QuickFood("Fish", "100 g, cooked", 128f, 26f, 0f, 2.7f),
    QuickFood("Sambar", "1 katori (≈150 g)", 115f, 5f, 15f, 4f),
    QuickFood("Rice", "1 cup cooked (≈158 g)", 205f, 4.3f, 45f, 0.4f),
)
