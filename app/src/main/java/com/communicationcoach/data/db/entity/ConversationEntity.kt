package com.communicationcoach.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Possible values for the status column
object ConversationStatus {
    const val RECORDING = "RECORDING"
    const val READY = "READY"
    const val ANALYZING = "ANALYZING"
    const val ANALYZED = "ANALYZED"
    const val FAILED = "FAILED"
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    @ColumnInfo(name = "status")
    val status: String = ConversationStatus.RECORDING,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int = 0
)
