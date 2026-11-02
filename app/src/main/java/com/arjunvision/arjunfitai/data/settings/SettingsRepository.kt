package com.arjunvision.arjunfitai.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    val stepGoal: Int = 8_000,
    val targetWeightKg: Float = 95f,
    val calorieGoalKcal: Int = 2_200,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<UserSettings> = store.data.map { prefs ->
        val defaults = UserSettings()
        UserSettings(
            name = prefs[NAME] ?: defaults.name,
            stepGoal = prefs[STEP_GOAL] ?: defaults.stepGoal,
            targetWeightKg = prefs[TARGET_WEIGHT] ?: defaults.targetWeightKg,
            calorieGoalKcal = prefs[CALORIE_GOAL] ?: defaults.calorieGoalKcal,
        )
    }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { prefs ->
            val current = UserSettings(
                name = prefs[NAME] ?: "",
                stepGoal = prefs[STEP_GOAL] ?: UserSettings().stepGoal,
                targetWeightKg = prefs[TARGET_WEIGHT] ?: UserSettings().targetWeightKg,
                calorieGoalKcal = prefs[CALORIE_GOAL] ?: UserSettings().calorieGoalKcal,
            )
            val next = transform(current)
            prefs[NAME] = next.name
            prefs[STEP_GOAL] = next.stepGoal
            prefs[TARGET_WEIGHT] = next.targetWeightKg
            prefs[CALORIE_GOAL] = next.calorieGoalKcal
        }
    }

    private companion object {
        val NAME = stringPreferencesKey("name")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val TARGET_WEIGHT = floatPreferencesKey("target_weight_kg")
        val CALORIE_GOAL = intPreferencesKey("calorie_goal_kcal")
    }
}
