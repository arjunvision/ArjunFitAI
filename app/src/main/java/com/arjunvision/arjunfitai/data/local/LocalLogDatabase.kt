package com.arjunvision.arjunfitai.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// Data the user logs inside ArjunFit. Samsung Health is never written to.
// Times are epoch milliseconds.

@Entity(tableName = "food")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val name: String,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
)

@Entity(tableName = "water")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val ml: Int,
)

@Entity(tableName = "weight")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val kg: Float,
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM food WHERE time >= :from AND time < :to ORDER BY time DESC")
    fun between(from: Long, to: Long): Flow<List<FoodEntry>>

    @Insert suspend fun insert(entry: FoodEntry): Long
    @Delete suspend fun delete(entry: FoodEntry)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water WHERE time >= :from AND time < :to ORDER BY time DESC")
    fun between(from: Long, to: Long): Flow<List<WaterEntry>>

    @Insert suspend fun insert(entry: WaterEntry): Long
    @Delete suspend fun delete(entry: WaterEntry)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight ORDER BY time DESC")
    fun all(): Flow<List<WeightEntry>>

    @Insert suspend fun insert(entry: WeightEntry): Long
    @Delete suspend fun delete(entry: WeightEntry)
}

@Database(entities = [FoodEntry::class, WaterEntry::class, WeightEntry::class], version = 1, exportSchema = false)
abstract class LocalLogDatabase : RoomDatabase() {
    abstract fun food(): FoodDao
    abstract fun water(): WaterDao
    abstract fun weight(): WeightDao

    companion object {
        @Volatile private var instance: LocalLogDatabase? = null

        fun get(context: Context): LocalLogDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LocalLogDatabase::class.java, "arjunfit-log.db")
                .build()
                .also { instance = it }
        }
    }
}
