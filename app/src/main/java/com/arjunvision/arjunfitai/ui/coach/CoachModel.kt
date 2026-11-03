package com.arjunvision.arjunfitai.ui.coach

import com.arjunvision.arjunfitai.data.coach.ChatKind
import com.arjunvision.arjunfitai.data.coach.ChatMessage
import com.arjunvision.arjunfitai.data.coach.ChatRole
import com.arjunvision.arjunfitai.ui.home.WeightProgress
import com.arjunvision.arjunfitai.ui.home.WinterArc
import com.arjunvision.arjunfitai.ui.home.formatSteps
import java.time.Duration
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Pure AI Coach logic (unit-tested). Nothing here touches the network.

/** Today's numbers that may go into the coach summary. Null = not available. */
data class CoachInputs(
    val today: LocalDate,
    val steps: Long?,
    val stepGoal: Int,
    val activeKcal: Float?,
    val hrLow: Int?,
    val hrHigh: Int?,
    val sleep: Duration?,
    val spo2: Int?,
    val weight: WeightProgress?,
    val targetKg: Float,
    val plannedWorkout: String?,
    val workoutDoneToday: Boolean,
    val setsDoneToday: Int,
    val eatenKcal: Float,
    val calorieGoal: Int,
    val proteinG: Float,
    val proteinMin: Int,
    val proteinMax: Int,
    val waterMl: Int,
    val waterGoalMl: Int,
)

/**
 * The only health data sent to Claude: a short summary — no name, no raw records, no exact times.
 * The consent screen and "What the coach sees" show exactly this text.
 */
fun coachSummary(i: CoachInputs): String {
    val lines = mutableListOf<String>()
    lines += when (val s = WinterArc.status(i.today)) {
        is WinterArc.Status.Active -> "Winter Arc day ${s.day} of ${s.totalDays} (Oct 12 – Dec 31)."
        is WinterArc.Status.Upcoming -> "Winter Arc starts in ${s.daysUntil} days."
        is WinterArc.Status.Complete -> "Winter Arc finished."
    }
    lines += "Steps today: ${i.steps?.let(::formatSteps) ?: "not available"} (goal ${formatSteps(i.stepGoal.toLong())})."
    i.activeKcal?.let { lines += "Active calories today: ${it.roundToInt()} kcal." }
    if (i.hrLow != null && i.hrHigh != null) lines += "Heart rate today: ${i.hrLow}–${i.hrHigh} bpm."
    lines += "Sleep last night: ${i.sleep?.let { "${it.toHours()}h ${it.toMinutesPart()}m" } ?: "not recorded"}."
    i.spo2?.let { lines += "Latest SpO2: $it%." }
    lines += when {
        i.weight == null -> "Weight: not logged yet; target ${kg(i.targetKg)} kg."
        i.weight.changeKg == null -> "Weight: ${kg(i.weight.currentKg)} kg; target ${kg(i.targetKg)} kg."
        else -> "Weight: ${kg(i.weight.currentKg)} kg (${sign(i.weight.changeKg)}${kg(abs(i.weight.changeKg))} kg since the Winter Arc started); target ${kg(i.targetKg)} kg."
    }
    lines += when {
        i.workoutDoneToday -> "Workout today: done (${i.setsDoneToday} sets)."
        i.plannedWorkout != null -> "Workout planned today: ${i.plannedWorkout} (not done yet)."
        else -> "No workout planned today."
    }
    lines += "Eaten today: ${i.eatenKcal.roundToInt()} of ${i.calorieGoal} kcal; protein ${i.proteinG.roundToInt()} g (goal ${i.proteinMin}–${i.proteinMax} g)."
    lines += "Water today: ${i.waterMl} of ${i.waterGoalMl} ml."
    return lines.joinToString("\n")
}

data class PlanTask(val label: String, val done: Boolean)

/** The canvas's "plan for today" — ticked from real data, not by hand. */
fun todayChecklist(i: CoachInputs): List<PlanTask> = listOfNotNull(
    PlanTask("Walk ${formatSteps(i.stepGoal.toLong())}+ steps", (i.steps ?: 0L) >= i.stepGoal),
    i.plannedWorkout?.let { PlanTask("Strength: $it", i.workoutDoneToday) } ?: if (i.workoutDoneToday) PlanTask("Workout", true) else null,
    PlanTask("Keep calories around ${"%,d".format(Locale.ENGLISH, i.calorieGoal)} kcal", i.eatenKcal >= i.calorieGoal * 0.9f && i.eatenKcal <= i.calorieGoal * 1.1f),
    PlanTask("Protein ${i.proteinMin}–${i.proteinMax} g", i.proteinG >= i.proteinMin),
)

/** One line of encouragement from your own numbers (no AI call, so it costs nothing). */
fun motivation(i: CoachInputs): String {
    val w = i.weight
    return when {
        w?.changeKg != null && w.changeKg < 0f && w.percentToGoal != null ->
            "You're ${kg(abs(w.changeKg))} kg down — ${w.percentToGoal}% of the way to ${kg(i.targetKg)} kg. Keep going!"
        w == null || w.changeKg == null -> "Log your weight today so I can track your progress to ${kg(i.targetKg)} kg."
        (i.steps ?: 0L) >= i.stepGoal -> "Step goal done today — that's how a Winter Arc is won."
        else -> "Small wins add up. One workout, good food, and enough sleep today."
    }
}

/**
 * Conversation sent to Claude: the last [maxTurns] OK messages, starting with a user message and
 * alternating user/coach (merging any repeats), so the request is always valid.
 */
fun historyForRequest(messages: List<ChatMessage>, maxTurns: Int = 20): List<Pair<ChatRole, String>> {
    val ok = messages.filter { it.kind == ChatKind.OK && it.text.isNotBlank() }
    val merged = mutableListOf<Pair<ChatRole, String>>()
    ok.forEach { m ->
        if (merged.isNotEmpty() && merged.last().first == m.role) merged[merged.lastIndex] = m.role to (merged.last().second + "\n\n" + m.text)
        else merged += m.role to m.text
    }
    var recent = merged.takeLast(maxTurns)
    while (recent.isNotEmpty() && recent.first().first != ChatRole.USER) recent = recent.drop(1)
    return recent
}

/** The newest user message as sent: today's summary first, then what you typed. */
fun withSummary(summary: String, text: String): String = "Today's summary from ArjunFit:\n$summary\n\nMy message:\n$text"

/** The fixed coaching instructions, identical on every request so they can be prompt-cached. */
val COACH_SYSTEM_PROMPT = """
You are the ArjunFit AI Coach, a friendly, practical fitness coach inside the ArjunFit app.

The person you coach is on a "Winter Arc": a training and fat-loss block from Oct 12 to Dec 31. They train full-body strength in the gym on Monday, Wednesday and Friday, do cardio on Tuesday, Thursday and Saturday, and recover on Sunday. They eat mostly South Indian home food (idli, dosa, dal, sambar, rice, eggs, chicken, fish), so suggest realistic meals from that cuisine.

Each newest message starts with "Today's summary from ArjunFit": numbers the app read from their Galaxy Fit3 (via Samsung Health) and their own logs. Use those numbers; never invent data that is not there, and say so when something isn't recorded.

How to reply:
- Be warm and direct. Keep it short: usually 2–5 sentences or a short list. Use plain language and kg/km/kcal.
- Give one or two concrete next steps the person can do today.
- Swapping a workout: suggest exercises with similar muscle groups and give sets × reps.
- Nutrition: practical portions and protein-rich options; don't prescribe extreme diets or very low calories.
- Weight loss pace: steady progress is healthy; don't push for faster than about 1% of body weight per week.

Safety:
- You give general fitness guidance, not medical advice. Don't diagnose conditions, interpret symptoms, or advise on medicines.
- If they mention chest pain, fainting, shortness of breath, injury, an eating disorder, or any medical concern, tell them kindly to stop and speak with a doctor, and keep the rest of the reply general.
- Heart-rate, SpO2 and sleep numbers from a fitness band are estimates; don't treat them as clinical readings.
""".trimIndent()

private fun kg(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(Locale.ENGLISH, v)
private fun sign(v: Float): String = if (v < 0f) "−" else "+"
