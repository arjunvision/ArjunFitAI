package com.arjunvision.arjunfitai.data.coach

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// The coach chat is kept only on this phone, in its own database.

enum class ChatRole { USER, COACH }

/** OK = a normal message; NOTICE = an app message (refusal, error) that is never sent to Claude. */
enum class ChatKind { OK, NOTICE }

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val role: ChatRole,
    val text: String,
    val kind: ChatKind = ChatKind.OK,
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_message ORDER BY time, id") fun all(): Flow<List<ChatMessage>>
    @Query("SELECT * FROM chat_message ORDER BY time, id") suspend fun snapshot(): List<ChatMessage>
    @Insert suspend fun insert(message: ChatMessage): Long
    @Update suspend fun update(message: ChatMessage)
    @Query("DELETE FROM chat_message") suspend fun clear()
}

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class CoachDatabase : RoomDatabase() {
    abstract fun chat(): ChatDao

    companion object {
        @Volatile private var instance: CoachDatabase? = null

        fun get(context: Context): CoachDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CoachDatabase::class.java, "arjunfit-coach.db")
                .build()
                .also { instance = it }
        }
    }
}
