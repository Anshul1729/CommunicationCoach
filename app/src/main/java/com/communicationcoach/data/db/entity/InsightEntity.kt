package com.communicationcoach.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "insights",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversation_id")]
)
data class InsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "conversation_id")
    val conversationId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    // JSON array of coaching tip strings, e.g. ["Watch your tone", "Pause more"]
    @ColumnInfo(name = "tips_json")
    val tipsJson: String,

    // JSON object with detected issues and details
    @ColumnInfo(name = "issues_json")
    val issuesJson: String,

    @ColumnInfo(name = "summary")
    val summary: String
)
