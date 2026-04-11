package com.workouttracker.data.db

import androidx.room.*
import com.workouttracker.data.model.AiChatMessage
import com.workouttracker.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDao {

    // ── Chat messages ──────────────────────────────────────────────────────────
    @Insert
    suspend fun insertMessage(msg: AiChatMessage)

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AiChatMessage>>

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<AiChatMessage>

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearAllMessages()

    // ── User profile ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id=1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Query("SELECT * FROM user_profile WHERE id=1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>
}
