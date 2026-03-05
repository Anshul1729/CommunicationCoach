package com.communicationcoach.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

object QueueStatus {
    const val PENDING = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
}

@Entity(tableName = "upload_queue")
data class UploadQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "conversation_id")
    val conversationId: Long,

    @ColumnInfo(name = "status")
    val status: String = QueueStatus.PENDING,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
