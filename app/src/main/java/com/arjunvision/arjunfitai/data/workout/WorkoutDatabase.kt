package com.arjunvision.arjunfitai.data.workout

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Workouts are a separate database from the food/water/weight log, so that log's schema never changes.

enum class PlanKind(val label: String) { GYM("Gym"), HOME("Home"), CARDIO("Cardio"), CUSTOM("Custom") }

/** How an item's target is measured. */
enum class Measure(val short: String) { REPS("reps"), SECONDS("sec"), MINUTES("min") }

@Entity(tableName = "plan")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: PlanKind,
    val name: String,
    val focus: String,
    /** Badge on the plan card: "MON", "A", "ANY". */
    val dayLabel: String,
    val sortOrder: Int,
)

@Entity(tableName = "plan_item", indices = [Index("planId")])
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val position: Int,
    /** Null for activities not in the library (e.g. swimming). */
    val exerciseId: String?,
    val name: String,
    val sets: Int,
    val target: Int,
    val unit: Measure,
)

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long?,
    val planName: String,
    val start: Long,
    val end: Long? = null,
)

@Entity(tableName = "workout_set", indices = [Index("sessionId"), Index("exerciseName")])
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val position: Int,
    val exerciseId: String?,
    val exerciseName: String,
    val setIndex: Int,
    val kg: Float? = null,
    /** Reps, or seconds/minutes for timed items. */
    val count: Int? = null,
    val unit: Measure = Measure.REPS,
    val done: Boolean = false,
    val doneAt: Long? = null,
)

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan ORDER BY sortOrder, id") fun plans(): Flow<List<PlanEntity>>
    @Query("SELECT * FROM plan_item ORDER BY planId, position") fun items(): Flow<List<PlanItemEntity>>
    @Query("SELECT COUNT(*) FROM plan") suspend fun count(): Int
    @Query("SELECT * FROM plan_item WHERE planId = :planId ORDER BY position") suspend fun itemsOf(planId: Long): List<PlanItemEntity>
    @Insert suspend fun insert(plan: PlanEntity): Long
    @Insert suspend fun insertItems(items: List<PlanItemEntity>)
    @Update suspend fun update(plan: PlanEntity)
    @Update suspend fun updateItems(items: List<PlanItemEntity>)
    @Delete suspend fun deleteItem(item: PlanItemEntity)
    @Query("DELETE FROM plan_item WHERE planId = :planId") suspend fun deleteItemsOf(planId: Long)
    @Delete suspend fun delete(plan: PlanEntity)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM session ORDER BY start DESC") fun sessions(): Flow<List<SessionEntity>>
    @Query("SELECT * FROM workout_set ORDER BY sessionId, position, setIndex") fun sets(): Flow<List<SetEntity>>
    @Query("SELECT * FROM session WHERE `end` IS NULL ORDER BY start DESC LIMIT 1") fun active(): Flow<SessionEntity?>
    @Insert suspend fun insert(session: SessionEntity): Long
    @Update suspend fun update(session: SessionEntity)
    @Insert suspend fun insertSets(sets: List<SetEntity>): List<Long>
    @Update suspend fun updateSet(set: SetEntity)
    @Delete suspend fun deleteSet(set: SetEntity)
    @Query("DELETE FROM workout_set WHERE sessionId = :sessionId") suspend fun deleteSetsOf(sessionId: Long)
    @Delete suspend fun delete(session: SessionEntity)
}

@Database(entities = [PlanEntity::class, PlanItemEntity::class, SessionEntity::class, SetEntity::class], version = 1, exportSchema = false)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun plans(): PlanDao
    abstract fun sessions(): SessionDao

    companion object {
        @Volatile private var instance: WorkoutDatabase? = null

        fun get(context: Context): WorkoutDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, WorkoutDatabase::class.java, "arjunfit-workouts.db")
                .build()
                .also { instance = it }
        }
    }
}
