package com.arjunvision.arjunfitai.ui.coach

import com.arjunvision.arjunfitai.data.coach.ChatKind
import com.arjunvision.arjunfitai.data.coach.ChatMessage
import com.arjunvision.arjunfitai.data.coach.ChatRole
import com.arjunvision.arjunfitai.data.coach.apiKeyProblem
import com.arjunvision.arjunfitai.data.coach.looksLikeApiKey
import com.arjunvision.arjunfitai.ui.home.WeightProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class CoachModelTest {
    private fun inputs(
        steps: Long? = 3377,
        weight: WeightProgress? = WeightProgress(107f, 95f, -3f, LocalDate.of(2026, 10, 10), 20),
        workoutDone: Boolean = false,
        protein: Float = 34f,
    ) = CoachInputs(
        today = LocalDate.of(2026, 11, 2), steps = steps, stepGoal = 8000, activeKcal = 606f,
        hrLow = 65, hrHigh = 163, sleep = null, spo2 = 97, weight = weight, targetKg = 95f,
        plannedWorkout = "Full Body A", workoutDoneToday = workoutDone, setsDoneToday = if (workoutDone) 18 else 0,
        eatenKcal = 716f, calorieGoal = 2200, proteinG = protein, proteinMin = 130, proteinMax = 150,
        waterMl = 1000, waterGoalMl = 3000,
    )

    @Test fun summaryIsShortAndHasNoPersonalDetails() {
        val s = coachSummary(inputs())
        assertEquals(
            listOf(
                "Winter Arc day 22 of 81 (Oct 12 – Dec 31).",
                "Steps today: 3,377 (goal 8,000).",
                "Active calories today: 606 kcal.",
                "Heart rate today: 65–163 bpm.",
                "Sleep last night: not recorded.",
                "Latest SpO2: 97%.",
                "Weight: 107 kg (−3 kg since the Winter Arc started); target 95 kg.",
                "Workout planned today: Full Body A (not done yet).",
                "Eaten today: 716 of 2200 kcal; protein 34 g (goal 130–150 g).",
                "Water today: 1000 of 3000 ml.",
            ),
            s.lines(),
        )
        assertFalse(s.contains("Sri"))
        assertTrue(coachSummary(inputs(steps = null, weight = null)).contains("Steps today: not available"))
        assertTrue(coachSummary(inputs(weight = null)).contains("Weight: not logged yet; target 95 kg."))
    }

    @Test fun checklistTicksFromData() {
        assertEquals(listOf(false, false, false, false), todayChecklist(inputs()).map { it.done })
        val done = todayChecklist(inputs(steps = 9000, workoutDone = true, protein = 140f))
        assertEquals(listOf("Walk 8,000+ steps", "Strength: Full Body A", "Keep calories around 2,200 kcal", "Protein 130–150 g"), done.map { it.label })
        assertEquals(listOf(true, true, false, true), done.map { it.done })
    }

    @Test fun motivationUsesRealProgress() {
        assertEquals("You're 3 kg down — 20% of the way to 95 kg. Keep going!", motivation(inputs()))
        assertEquals("Log your weight today so I can track your progress to 95 kg.", motivation(inputs(weight = null)))
    }

    @Test fun historyStartsWithUserAlternatesAndSkipsNotices() {
        fun m(id: Long, role: ChatRole, text: String, kind: ChatKind = ChatKind.OK) = ChatMessage(id, id, role, text, kind)
        val history = historyForRequest(
            listOf(
                m(1, ChatRole.COACH, "old reply"),
                m(2, ChatRole.USER, "hi"),
                m(3, ChatRole.COACH, "Your API key was rejected.", ChatKind.NOTICE),
                m(4, ChatRole.USER, "hello?"),
                m(5, ChatRole.COACH, "Hey!"),
                m(6, ChatRole.USER, "Swap today's workout"),
            ),
        )
        assertEquals(listOf(ChatRole.USER, ChatRole.COACH, ChatRole.USER), history.map { it.first })
        assertEquals("hi\n\nhello?", history.first().second)
        assertTrue(withSummary("S", "T").startsWith("Today's summary from ArjunFit:\nS"))
    }

    @Test fun apiKeyFormat() {
        assertTrue(looksLikeApiKey("sk-ant-api03-" + "a".repeat(40)))
        assertTrue(looksLikeApiKey("sk-ant-usr-" + "a".repeat(40))) // account-linked Console keys
        assertFalse(looksLikeApiKey("sk-ant-admin01-" + "a".repeat(40)))
        assertFalse(looksLikeApiKey("sk-ant-oat01-" + "a".repeat(40)))
        assertFalse(looksLikeApiKey("hello"))
        assertFalse(looksLikeApiKey("sk-ant-api03-abc def" + "a".repeat(40)))
        assertEquals("Anthropic API keys start with sk-ant- (yours starts with \"hello\").", apiKeyProblem("hello"))
        assertEquals(null, apiKeyProblem("sk-ant-usr-" + "a".repeat(40)))
    }

    @Test fun systemPromptIsFixed() {
        // Same text every request, so it can be prompt-cached; no per-day data in it.
        assertFalse(COACH_SYSTEM_PROMPT.contains("2026"))
        assertTrue(COACH_SYSTEM_PROMPT.contains("not medical advice"))
        assertEquals(Duration.ZERO, Duration.ZERO)
    }
}
