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
    val milestoneAlerts: Boolean = false,
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
            prefs[MILESTONE_ALERTS] = next.milestoneAlerts
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
            milestoneAlerts = prefs[MILESTONE_ALERTS] ?: defaults.milestoneAlerts,
            onboarded = prefs[ONBOARDED] ?: defaults.onboarded,
        )
    }

    private companion object {
        val NAME = stringPreferencesKey("name")
        val TAGLINE = stringPreferencesKey("tagline")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val TARGET_WEIGHT = floatPreferencesKey("target_weight_kg")
        val CALORIE_GOAL = intPreferencesKey("calorie_goal_kcal")
        val MILESTONE_ALERTS = booleanPreferencesKey("milestone_alerts")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
