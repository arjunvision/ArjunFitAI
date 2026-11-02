package com.arjunvision.arjunfitai.ui.workout

import com.arjunvision.arjunfitai.data.workout.LibraryFilter
import com.arjunvision.arjunfitai.data.workout.Measure
import com.arjunvision.arjunfitai.data.workout.MuscleGroup
import com.arjunvision.arjunfitai.data.workout.PlanEntity
import com.arjunvision.arjunfitai.data.workout.PlanItemEntity
import com.arjunvision.arjunfitai.data.workout.PlanKind
import com.arjunvision.arjunfitai.data.workout.SeedPlans
import com.arjunvision.arjunfitai.data.workout.SessionEntity
import com.arjunvision.arjunfitai.data.workout.SetEntity
import com.arjunvision.arjunfitai.data.workout.filterExercises
import com.arjunvision.arjunfitai.data.workout.parseExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class WorkoutModelTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val monday = LocalDate.of(2026, 11, 2)
    private fun ms(date: LocalDate, h: Int) = date.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()

    private val library by lazy { parseExercises(File("src/main/assets/exercises/exercises.json").readText()) }

    @Test fun bundledLibraryHas876ExercisesWithPhotosOnDisk() {
        assertEquals(876, library.size)
        val first = library.first { it.id == "Barbell_Squat" }
        assertEquals(MuscleGroup.LEGS, first.group)
        assertEquals(2, first.images.size)
        first.images.forEach { assertTrue(it, File("src/main/assets/exercises/$it").exists()) }
    }

    @Test fun everySeedExerciseExistsInTheLibrary() {
        val ids = library.associateBy { it.id }
        SeedPlans.flatMap { it.items }.mapNotNull { it.exerciseId }.forEach { id ->
            val e = ids[id]
            assertTrue("missing $id", e != null)
            assertEquals("photos for $id", 2, e!!.images.size)
        }
    }

    @Test fun searchAndFilters() {
        val bench = filterExercises(library, LibraryFilter(query = "bench press", group = MuscleGroup.CHEST))
        assertTrue(bench.any { it.id == "Barbell_Bench_Press_-_Medium_Grip" })
        assertTrue(bench.all { it.group == MuscleGroup.CHEST })
        val bodyOnlyBeginner = filterExercises(library, LibraryFilter(equipment = setOf("body only"), levels = setOf("beginner")))
        assertTrue(bodyOnlyBeginner.isNotEmpty() && bodyOnlyBeginner.all { it.equipment == "body only" && it.level == "beginner" })
    }

    private fun plan(id: Long, kind: PlanKind, day: String) = PlanEntity(id, kind, "P$id", "", day, id.toInt())

    @Test fun gymPlanFollowsWeekdayElseNext() {
        val plans = listOf(plan(1, PlanKind.GYM, "MON"), plan(2, PlanKind.GYM, "WED"), plan(3, PlanKind.GYM, "FRI"), plan(4, PlanKind.GYM, "ANY"))
        assertEquals(TodayPick(plans[0], "TODAY · MONDAY"), todaysPlan(PlanKind.GYM, plans, emptyList(), monday))
        assertEquals(TodayPick(plans[1], "NEXT · WEDNESDAY"), todaysPlan(PlanKind.GYM, plans, emptyList(), monday.plusDays(1)))
        assertEquals(plans[0], todaysPlan(PlanKind.GYM, plans, emptyList(), monday.plusDays(6)).plan) // Sunday → next Monday
    }

    @Test fun homePlansRotate() {
        val plans = listOf(plan(5, PlanKind.HOME, "A"), plan(6, PlanKind.HOME, "B"))
        val done = listOf(SessionEntity(1, 5, "P5", ms(monday, 7), ms(monday, 8)))
        assertEquals(plans[1], todaysPlan(PlanKind.HOME, plans, done, monday).plan)
        assertNull(todaysPlan(PlanKind.CUSTOM, plans, done, monday).plan)
    }

    @Test fun weekStripMarksWorkedDays() {
        val strip = weekStrip(monday.plusDays(2), listOf(SessionEntity(1, 1, "A", ms(monday, 7), ms(monday, 8))), zone)
        assertEquals(listOf("S", "C", "S", "C", "S", "C", "R"), strip.map { it.kind.tag })
        assertTrue(strip[0].worked)
        assertTrue(strip[2].isToday)
    }

    @Test fun previousSetsSummariesAndPrs() {
        val sessions = listOf(
            SessionEntity(1, 1, "A", ms(monday.minusDays(7), 7), ms(monday.minusDays(7), 8)),
            SessionEntity(2, 1, "A", ms(monday, 7), ms(monday, 8)),
        )
        val sets = listOf(
            SetEntity(1, 1, 0, "Barbell_Squat", "Barbell Squat", 1, 60f, 8, Measure.REPS, true, ms(monday.minusDays(7), 7)),
            SetEntity(2, 2, 0, "Barbell_Squat", "Barbell Squat", 1, 65f, 6, Measure.REPS, true, ms(monday, 7)),
            SetEntity(3, 2, 1, "Plank", "Plank", 1, null, 45, Measure.SECONDS, true, ms(monday, 7)),
        )
        assertEquals("60 kg × 8", previousSet(sets, sessions, "Barbell Squat", 1, currentSessionId = 2))
        assertEquals("45 sec", setText(sets[2]))

        val summary = sessionSummary(sessions[1], sets)
        assertEquals(60, summary.minutes)
        assertEquals(2, summary.setsDone)
        assertEquals(390f, summary.volumeKg)

        val pr = personalRecords(sessions, sets).single()
        assertEquals(65f, pr.kg)
        assertEquals(6, pr.reps)
        assertEquals(78f, pr.estimated1Rm, 0.01f)
    }

    @Test fun planMetaAndTargets() {
        val items = listOf(
            PlanItemEntity(1, 1, 0, "x", "Squat", 4, 8, Measure.REPS),
            PlanItemEntity(2, 1, 1, "y", "Plank", 3, 45, Measure.SECONDS),
        )
        assertEquals("2 exercises · ~15 min", planMeta(items))
        assertEquals("4 × 8", targetText(items[0]))
        assertEquals("3 × 45 sec", targetText(items[1]))
        assertEquals("1:30", clock(90))
    }
}
