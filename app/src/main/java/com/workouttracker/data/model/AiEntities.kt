package com.workouttracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 0,
    val experience: String = "Intermediate", // Beginner / Intermediate / Advanced
    val primaryGoal: String = "",            // Build muscle / Lose fat / Increase strength / etc.
    val injuries: String = "",
    val preferredSplit: String = "",         // PPL / Upper-Lower / Full Body / etc.
    val trainingDaysPerWeek: Int = 4,
    val additionalNotes: String = ""
)

@Entity(tableName = "ai_chat_messages")
data class AiChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,                        // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
