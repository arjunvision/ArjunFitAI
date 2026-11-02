package com.arjunvision.arjunfitai.data.workout

/**
 * Starting plans, matching the design canvas (Strength plans). Sets and reps are a sensible
 * beginner–intermediate starting point; every plan is editable in the app.
 */
internal data class SeedItem(val exerciseId: String?, val name: String, val sets: Int, val target: Int, val unit: Measure = Measure.REPS)
internal data class SeedPlan(val kind: PlanKind, val name: String, val focus: String, val day: String, val items: List<SeedItem>)

internal val SeedPlans = listOf(
    SeedPlan(
        PlanKind.GYM, "Full Body A", "Squat · Bench press · Row · Plank", "MON",
        listOf(
            SeedItem("Barbell_Squat", "Barbell Squat", 4, 8),
            SeedItem("Barbell_Bench_Press_-_Medium_Grip", "Barbell Bench Press - Medium Grip", 4, 8),
            SeedItem("Bent_Over_Barbell_Row", "Bent Over Barbell Row", 4, 10),
            SeedItem("Dumbbell_Shoulder_Press", "Dumbbell Shoulder Press", 3, 10),
            SeedItem("Plank", "Plank", 3, 45, Measure.SECONDS),
        ),
    ),
    SeedPlan(
        PlanKind.GYM, "Full Body B", "Deadlift · Shoulder press · Lat pulldown", "WED",
        listOf(
            SeedItem("Barbell_Deadlift", "Barbell Deadlift", 3, 5),
            SeedItem("Dumbbell_Shoulder_Press", "Dumbbell Shoulder Press", 3, 10),
            SeedItem("Wide-Grip_Lat_Pulldown", "Wide-Grip Lat Pulldown", 3, 10),
            SeedItem("Dumbbell_Lunges", "Dumbbell Lunges", 3, 10),
            SeedItem("Hanging_Leg_Raise", "Hanging Leg Raise", 3, 12),
        ),
    ),
    SeedPlan(
        PlanKind.GYM, "Full Body C", "Leg press · Incline press · Cable row", "FRI",
        listOf(
            SeedItem("Leg_Press", "Leg Press", 4, 10),
            SeedItem("Incline_Dumbbell_Press", "Incline Dumbbell Press", 4, 10),
            SeedItem("Seated_Cable_Rows", "Seated Cable Rows", 4, 10),
            SeedItem("Side_Lateral_Raise", "Side Lateral Raise", 3, 12),
            SeedItem("Cable_Crunch", "Cable Crunch", 3, 15),
        ),
    ),
    SeedPlan(
        PlanKind.GYM, "Arms & Core", "Curls · Pushdowns · Leg raises", "ANY",
        listOf(
            SeedItem("Dumbbell_Bicep_Curl", "Dumbbell Bicep Curl", 3, 12),
            SeedItem("Triceps_Pushdown", "Triceps Pushdown", 3, 12),
            SeedItem("Hanging_Leg_Raise", "Hanging Leg Raise", 3, 12),
            SeedItem("Plank", "Plank", 3, 45, Measure.SECONDS),
        ),
    ),
    SeedPlan(
        PlanKind.HOME, "Bodyweight Full Body", "Squats · Pushups · Lunges · Plank", "A",
        listOf(
            SeedItem("Bodyweight_Squat", "Bodyweight Squat", 4, 15),
            SeedItem("Pushups", "Pushups", 4, 12),
            SeedItem("Bodyweight_Walking_Lunge", "Bodyweight Walking Lunge", 4, 12),
            SeedItem("Plank", "Plank", 4, 45, Measure.SECONDS),
        ),
    ),
    SeedPlan(
        PlanKind.HOME, "Core Blast", "Crunches · Leg raises · Mountain climbers", "B",
        listOf(
            SeedItem("Crunches", "Crunches", 3, 20),
            SeedItem("Flat_Bench_Lying_Leg_Raise", "Flat Bench Lying Leg Raise", 3, 15),
            SeedItem("Mountain_Climbers", "Mountain Climbers", 3, 30, Measure.SECONDS),
        ),
    ),
    SeedPlan(
        PlanKind.HOME, "Mobility & Stretch", "Hips · Back · Shoulders", "C",
        listOf(
            SeedItem("Hip_Circles_prone", "Hip Circles (prone)", 2, 30, Measure.SECONDS),
            SeedItem("Cat_Stretch", "Cat Stretch", 2, 30, Measure.SECONDS),
            SeedItem("Childs_Pose", "Child's Pose", 2, 30, Measure.SECONDS),
            SeedItem("Shoulder_Stretch", "Shoulder Stretch", 2, 30, Measure.SECONDS),
        ),
    ),
    SeedPlan(PlanKind.CARDIO, "Treadmill Walk", "Incline walk, steady pace", "TUE", listOf(SeedItem("Walking_Treadmill", "Walking, Treadmill", 1, 30, Measure.MINUTES))),
    SeedPlan(PlanKind.CARDIO, "Cycling", "Intervals: 1 min hard, 2 min easy", "THU", listOf(SeedItem("Bicycling_Stationary", "Bicycling, Stationary", 1, 25, Measure.MINUTES))),
    SeedPlan(PlanKind.CARDIO, "Swim or Badminton", "Pick what you enjoy", "SAT", listOf(SeedItem(null, "Swim or badminton", 1, 45, Measure.MINUTES))),
)
