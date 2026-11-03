package com.arjunvision.arjunfitai.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Settings and goals kept inside ArjunFit (never written to Samsung Health). */
data class UserSettings(
    val name: String = "",
    /** Optional line under the name on Profile ("Founder · Arjun Vision Tech Solutions"). */
    val tagline: String = "",
    val stepGoal: Int = 8_000,
    val targetWeightKg: Float = 95f,
    val calorieGoalKcal: Int = 2_200,
    val proteinMinG: Int = 130,
    val proteinMaxG: Int = 150,
    val carbsTargetG: Int = 250,
    val fatTargetG: Int = 70,
    val waterGoalMl: Int = 3_000,
    val milestoneAlerts: Boolean = false,
    /** You agreed to send the AI Coach a short daily summary (never raw records). */
    val coachConsent: Boolean = false,
    /** True once the first-run Connect Device screen has been seen or skipped. */
    val onboarded: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<UserSettings> = store.data.map(::read)

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { prefs ->
            val next = transform(read(prefs))
            prefs[NAME] = next.name
            prefs[TAGLINE] = next.tagline
            prefs[STEP_GOAL] = next.stepGoal
            prefs[TARGET_WEIGHT] = next.targetWeightKg
            prefs[CALORIE_GOAL] = next.calorieGoalKcal
            prefs[PROTEIN_MIN] = next.proteinMinG
            prefs[PROTEIN_MAX] = next.proteinMaxG
            prefs[CARBS_TARGET] = next.carbsTargetG
            prefs[FAT_TARGET] = next.fatTargetG
            prefs[WATER_GOAL] = next.waterGoalMl
            prefs[MILESTONE_ALERTS] = next.milestoneAlerts
            prefs[COACH_CONSENT] = next.coachConsent
            prefs[ONBOARDED] = next.onboarded
        }
    }

    private fun read(prefs: Preferences): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            name = prefs[NAME] ?: defaults.name,
            tagline = prefs[TAGLINE] ?: defaults.tagline,
            stepGoal = prefs[STEP_GOAL] ?: defaults.stepGoal,
            targetWeightKg = prefs[TARGET_WEIGHT] ?: defaults.targetWeightKg,
            calorieGoalKcal = prefs[CALORIE_GOAL] ?: defaults.calorieGoalKcal,
            proteinMinG = prefs[PROTEIN_MIN] ?: defaults.proteinMinG,
            proteinMaxG = prefs[PROTEIN_MAX] ?: defaults.proteinMaxG,
            carbsTargetG = prefs[CARBS_TARGET] ?: defaults.carbsTargetG,
            fatTargetG = prefs[FAT_TARGET] ?: defaults.fatTargetG,
            waterGoalMl = prefs[WATER_GOAL] ?: defaults.waterGoalMl,
            milestoneAlerts = prefs[MILESTONE_ALERTS] ?: defaults.milestoneAlerts,
            coachConsent = prefs[COACH_CONSENT] ?: defaults.coachConsent,
            onboarded = prefs[ONBOARDED] ?: defaults.onboarded,
        )
    }

    private companion object {
        val NAME = stringPreferencesKey("name")
        val TAGLINE = stringPreferencesKey("tagline")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val TARGET_WEIGHT = floatPreferencesKey("target_weight_kg")
        val CALORIE_GOAL = intPreferencesKey("calorie_goal_kcal")
        val PROTEIN_MIN = intPreferencesKey("protein_min_g")
        val PROTEIN_MAX = intPreferencesKey("protein_max_g")
        val CARBS_TARGET = intPreferencesKey("carbs_target_g")
        val FAT_TARGET = intPreferencesKey("fat_target_g")
        val WATER_GOAL = intPreferencesKey("water_goal_ml")
        val MILESTONE_ALERTS = booleanPreferencesKey("milestone_alerts")
        val COACH_CONSENT = booleanPreferencesKey("coach_consent")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
