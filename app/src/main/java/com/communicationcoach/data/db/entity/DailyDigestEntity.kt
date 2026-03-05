package com.communicationcoach.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_digests")
data class DailyDigestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Format: "YYYY-MM-DD"
    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "summary")
    val summary: String,

    // JSON array of recurring pattern strings
    @ColumnInfo(name = "top_patterns_json")
    val topPatternsJson: String,

    @ColumnInfo(name = "progress_notes")
    val progressNotes: String
)
