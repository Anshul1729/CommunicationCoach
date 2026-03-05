package com.communicationcoach.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Singleton row — always id = 1.
// profileJson stores the full user profile as a JSON string so it can evolve freely.
// Example structure:
// {
//   "totalConversations": 5,
//   "recurringPatterns": ["defensive language", "fast speech under pressure"],
//   "improvements": ["clearer sentence structure"],
//   "focusAreas": ["emotional tone", "grammar"],
//   "recentInsights": ["..."]
// }
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "profile_json")
    val profileJson: String
)
