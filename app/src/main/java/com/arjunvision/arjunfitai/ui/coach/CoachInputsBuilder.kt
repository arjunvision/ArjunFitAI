package com.arjunvision.arjunfitai.ui.coach

import com.arjunvision.arjunfitai.data.local.FoodEntry
import com.arjunvision.arjunfitai.data.local.WaterEntry
import com.arjunvision.arjunfitai.data.settings.UserSettings
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.PlanKind
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import com.arjunvision.arjunfitai.ui.dashboard.HealthSnapshot
import com.arjunvision.arjunfitai.ui.home.WinterArc
import com.arjunvision.arjunfitai.ui.home.lastNightSleep
import com.arjunvision.arjunfitai.ui.home.sleepDuration
import com.arjunvision.arjunfitai.ui.home.weightProgress
import com.arjunvision.arjunfitai.ui.nutrition.dayNutrition
import com.arjunvision.arjunfitai.ui.weight.WeighIn
import com.arjunvision.arjunfitai.ui.weight.asRecords
import com.arjunvision.arjunfitai.ui.workout.todaysPlan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Gathers today's numbers from Samsung Health and ArjunFit's own logs for the coach. */
fun buildCoachInputs(
    today: LocalDate,
    now: Instant,
    zone: ZoneId,
    data: HealthSnapshot,
    settings: UserSettings,
    weighIns: List<WeighIn>,
    plans: List<PlanEntity>,
    sessions: List<SessionEntity>,
    sets: List<SetEntity>,
    foodToday: List<FoodEntry>,
    waterToday: List<WaterEntry>,
): CoachInputs {
    val hrToday = data.heartRate.filter { it.time.atZone(zone).toLocalDate() == today }
    val finishedToday = sessions.filter { it.end != null && Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() == today }
    val gym = todaysPlan(PlanKind.GYM, plans, sessions, today).takeIf { it.label.startsWith("TODAY") }?.plan
    val nutrition = dayNutrition(foodToday, data.nutritionToday, waterToday, data.waterToday)
    return CoachInputs(
        today = today,
        steps = data.steps,
        stepGoal = settings.stepGoal,
        activeKcal = data.activity?.activeCaloriesKcal,
        hrLow = hrToday.minOfOrNull { it.minBpm ?: it.bpm }?.roundToInt(),
        hrHigh = hrToday.maxOfOrNull { it.maxBpm ?: it.bpm }?.roundToInt(),
        sleep = lastNightSleep(data.sleep, now)?.let(::sleepDuration),
        spo2 = data.bloodOxygen.maxByOrNull { it.time }?.percent?.roundToInt(),
        weight = weightProgress(weighIns.asRecords(), settings.targetWeightKg, WinterArc.start(today.year), zone),
        targetKg = settings.targetWeightKg,
        plannedWorkout = gym?.name,
        workoutDoneToday = finishedToday.isNotEmpty(),
        setsDoneToday = sets.count { s -> s.done && finishedToday.any { it.id == s.sessionId } },
        eatenKcal = nutrition.kcal,
        calorieGoal = settings.calorieGoalKcal,
        proteinG = nutrition.proteinG,
        proteinMin = settings.proteinMinG,
        proteinMax = settings.proteinMaxG,
        waterMl = nutrition.waterMl,
        waterGoalMl = settings.waterGoalMl,
    )
}
