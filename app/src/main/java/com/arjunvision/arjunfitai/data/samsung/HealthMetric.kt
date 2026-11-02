package com.arjunvision.arjunfitai.data.samsung

import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes

/** The Samsung Health data types ArjunFit reads. READ access only — the app never writes. */
enum class HealthMetric(val label: String, internal val dataType: DataType) {
    STEPS("Steps", DataTypes.STEPS),
    HEART_RATE("Heart rate", DataTypes.HEART_RATE),
    EXERCISE("Exercise", DataTypes.EXERCISE),
    SLEEP("Sleep", DataTypes.SLEEP),
    BLOOD_OXYGEN("Blood oxygen", DataTypes.BLOOD_OXYGEN),
    BODY_COMPOSITION("Weight & body composition", DataTypes.BODY_COMPOSITION),

    /** Active calories, total calories, active time and distance. */
    ACTIVITY_SUMMARY("Activity summary", DataTypes.ACTIVITY_SUMMARY),
    NUTRITION("Nutrition", DataTypes.NUTRITION),
    WATER_INTAKE("Water intake", DataTypes.WATER_INTAKE);

    internal val readPermission: Permission get() = Permission.of(dataType, AccessType.READ)

    companion object {
        internal val ALL_READ_PERMISSIONS: Set<Permission> = entries.map { it.readPermission }.toSet()
    }
}

data class PermissionStatus(val granted: Set<HealthMetric>) {
    val missing: Set<HealthMetric> get() = HealthMetric.entries.toSet() - granted
    val allGranted: Boolean get() = missing.isEmpty()
    val noneGranted: Boolean get() = granted.isEmpty()

    internal companion object {
        fun from(grantedPermissions: Set<Permission>) = PermissionStatus(
            HealthMetric.entries.filterTo(mutableSetOf()) { it.readPermission in grantedPermissions }
        )
    }
}
