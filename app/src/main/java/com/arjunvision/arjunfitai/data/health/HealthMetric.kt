package com.arjunvision.arjunfitai.data.health

/** The kinds of health data ArjunFit reads, whatever the source. READ access only — the app never writes. */
enum class HealthMetric(val label: String) {
    STEPS("Steps"),
    HEART_RATE("Heart rate"),
    EXERCISE("Exercise"),
    SLEEP("Sleep"),
    BLOOD_OXYGEN("Blood oxygen"),
    BODY_COMPOSITION("Weight & body composition"),

    /** Active calories, total calories, active time and distance. */
    ACTIVITY_SUMMARY("Activity summary"),
    NUTRITION("Nutrition"),
    WATER_INTAKE("Water intake"),
}

/**
 * Which metrics a source lets ArjunFit read. [supported] is what that source can offer at all,
 * so a metric the source never has doesn't count as "missing".
 */
data class PermissionStatus(
    val granted: Set<HealthMetric>,
    val supported: Set<HealthMetric> = HealthMetric.entries.toSet(),
) {
    val missing: Set<HealthMetric> get() = supported - granted
    val allGranted: Boolean get() = missing.isEmpty()
    val noneGranted: Boolean get() = granted.isEmpty()
}
