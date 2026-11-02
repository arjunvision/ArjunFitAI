package com.arjunvision.arjunfitai.data.workout

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

data class Plan(val plan: PlanEntity, val items: List<PlanItemEntity>)
data class Session(val session: SessionEntity, val sets: List<SetEntity>)

/** Plans, workout sessions and sets, all stored in ArjunFit (never in Samsung Health). */
class WorkoutRepository(private val db: WorkoutDatabase) {
    val plans: Flow<List<PlanEntity>> = db.plans().plans()
    val planItems: Flow<List<PlanItemEntity>> = db.plans().items()
    val sessions: Flow<List<SessionEntity>> = db.sessions().sessions()
    val sets: Flow<List<SetEntity>> = db.sessions().sets()
    val activeSession: Flow<SessionEntity?> = db.sessions().active()

    /** First launch: insert the canvas's starting plans. */
    suspend fun ensureSeeded() {
        if (db.plans().count() > 0) return
        db.withTransaction {
            SeedPlans.forEachIndexed { order, seed ->
                val id = db.plans().insert(PlanEntity(kind = seed.kind, name = seed.name, focus = seed.focus, dayLabel = seed.day, sortOrder = order))
                db.plans().insertItems(seed.items.mapIndexed { i, it -> PlanItemEntity(planId = id, position = i, exerciseId = it.exerciseId, name = it.name, sets = it.sets, target = it.target, unit = it.unit) })
            }
        }
    }

    /** Starts a session with one empty row per planned set. Returns the session id. */
    suspend fun startSession(plan: PlanEntity): Long = db.withTransaction {
        val items = db.plans().itemsOf(plan.id)
        val id = db.sessions().insert(SessionEntity(planId = plan.id, planName = plan.name, start = System.currentTimeMillis()))
        db.sessions().insertSets(items.flatMap { item ->
            (1..item.sets).map { n ->
                SetEntity(sessionId = id, position = item.position, exerciseId = item.exerciseId, exerciseName = item.name, setIndex = n, unit = item.unit)
            }
        })
        id
    }

    suspend fun updateSet(set: SetEntity) = db.sessions().updateSet(set)
    suspend fun addSet(after: SetEntity) = db.sessions().insertSets(listOf(after.copy(id = 0, setIndex = after.setIndex + 1, kg = after.kg, count = after.count, done = false, doneAt = null)))
    suspend fun deleteSet(set: SetEntity) = db.sessions().deleteSet(set)

    /** Ends the session; one with no completed sets is removed rather than kept as an empty workout. */
    suspend fun finish(session: SessionEntity, anyDone: Boolean) {
        if (anyDone) db.sessions().update(session.copy(end = System.currentTimeMillis()))
        else db.withTransaction { db.sessions().deleteSetsOf(session.id); db.sessions().delete(session) }
    }

    suspend fun deleteSession(session: SessionEntity) = db.withTransaction {
        db.sessions().deleteSetsOf(session.id)
        db.sessions().delete(session)
    }

    // ----- Plan editing -----

    suspend fun createPlan(name: String, kind: PlanKind = PlanKind.CUSTOM): Long =
        db.plans().insert(PlanEntity(kind = kind, name = name, focus = "", dayLabel = "ANY", sortOrder = 100))

    suspend fun renamePlan(plan: PlanEntity, name: String) = db.plans().update(plan.copy(name = name))

    suspend fun deletePlan(plan: PlanEntity) = db.withTransaction {
        db.plans().deleteItemsOf(plan.id)
        db.plans().delete(plan)
    }

    suspend fun addToPlan(planId: Long, exercise: Exercise) {
        val items = db.plans().itemsOf(planId)
        val unit = when {
            exercise.isCardio -> Measure.MINUTES
            exercise.isStretch -> Measure.SECONDS
            else -> Measure.REPS
        }
        val (sets, target) = when (unit) {
            Measure.MINUTES -> 1 to 20
            Measure.SECONDS -> 2 to 30
            Measure.REPS -> 3 to 10
        }
        db.plans().insertItems(listOf(PlanItemEntity(planId = planId, position = (items.maxOfOrNull { it.position } ?: -1) + 1, exerciseId = exercise.id, name = exercise.name, sets = sets, target = target, unit = unit)))
    }

    suspend fun updateItem(item: PlanItemEntity) = db.plans().updateItems(listOf(item))

    suspend fun removeItem(item: PlanItemEntity) = db.withTransaction {
        db.plans().deleteItem(item)
        db.plans().updateItems(db.plans().itemsOf(item.planId).mapIndexed { i, it -> it.copy(position = i) })
    }

    /** Moves [item] one place up (-1) or down (+1). */
    suspend fun moveItem(item: PlanItemEntity, by: Int) = db.withTransaction {
        val items = db.plans().itemsOf(item.planId).toMutableList()
        val from = items.indexOfFirst { it.id == item.id }
        val to = (from + by).coerceIn(0, items.lastIndex)
        if (from < 0 || from == to) return@withTransaction
        items.add(to, items.removeAt(from))
        db.plans().updateItems(items.mapIndexed { i, it -> it.copy(position = i) })
    }
}
