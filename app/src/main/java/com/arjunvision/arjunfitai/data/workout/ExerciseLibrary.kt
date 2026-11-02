package com.arjunvision.arjunfitai.data.workout

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

/**
 * One exercise from free-exercise-db (github.com/yuhonas/free-exercise-db, public domain / Unlicense),
 * bundled in assets/exercises with WebP photos.
 */
data class Exercise(
    val id: String,
    val name: String,
    val level: String?,
    val equipment: String?,
    val category: String?,
    val force: String?,
    val mechanic: String?,
    val primary: List<String> = emptyList(),
    val secondary: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    /** Asset paths under assets/exercises, e.g. "img/Plank_0.webp". */
    val images: List<String> = emptyList(),
) {
    val group: MuscleGroup? get() = muscleGroup(primary)
    val isCardio: Boolean get() = category == "cardio"
    val isStretch: Boolean get() = category == "stretching"
}

/** The canvas's six filter chips. */
enum class MuscleGroup(val label: String) { CHEST("Chest"), BACK("Back"), LEGS("Legs"), SHOULDERS("Shoulders"), ARMS("Arms"), CORE("Core") }

fun muscleGroup(primary: List<String>): MuscleGroup? = when (primary.firstOrNull()) {
    "chest" -> MuscleGroup.CHEST
    "lats", "middle back", "lower back", "traps" -> MuscleGroup.BACK
    "quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors" -> MuscleGroup.LEGS
    "shoulders", "neck" -> MuscleGroup.SHOULDERS
    "biceps", "triceps", "forearms" -> MuscleGroup.ARMS
    "abdominals" -> MuscleGroup.CORE
    else -> null
}

fun parseExercises(json: String): List<Exercise> =
    Gson().fromJson<List<Exercise>>(json, object : TypeToken<List<Exercise>>() {}.type)
        // Gson leaves Kotlin defaults unset; normalise nullable JSON arrays.
        .map { it.copy(primary = it.primary.orEmptyList(), secondary = it.secondary.orEmptyList(), instructions = it.instructions.orEmptyList(), images = it.images.orEmptyList()) }
        .sortedBy { it.name.lowercase(Locale.ENGLISH) }

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
private fun List<String>?.orEmptyList(): List<String> = this ?: emptyList()

fun String.titleCase(): String = split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.titlecase(Locale.ENGLISH) } }

data class LibraryFilter(
    val query: String = "",
    val group: MuscleGroup? = null,
    val equipment: Set<String> = emptySet(),
    val levels: Set<String> = emptySet(),
)

/** Name search (all words must match) + muscle chip + equipment/level filters. */
fun filterExercises(all: List<Exercise>, f: LibraryFilter): List<Exercise> {
    val words = f.query.lowercase(Locale.ENGLISH).split(' ').filter { it.isNotBlank() }
    return all.filter { e ->
        val name = e.name.lowercase(Locale.ENGLISH)
        words.all { it in name } &&
            (f.group == null || e.group == f.group) &&
            (f.equipment.isEmpty() || (e.equipment ?: "none") in f.equipment) &&
            (f.levels.isEmpty() || e.level in f.levels)
    }
}

/** Loads the bundled library once per process. */
object ExerciseLibrary {
    @Volatile private var cache: List<Exercise>? = null

    fun load(context: Context): List<Exercise> = cache ?: synchronized(this) {
        cache ?: context.assets.open("exercises/exercises.json").bufferedReader().use { parseExercises(it.readText()) }.also { cache = it }
    }
}
