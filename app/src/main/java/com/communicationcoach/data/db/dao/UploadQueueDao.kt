package com.communicationcoach.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.communicationcoach.data.db.entity.UploadQueueEntity

@Dao
interface UploadQueueDao {

    @Insert
    suspend fun insert(item: UploadQueueEntity): Long

    @Query("SELECT * FROM upload_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPending(): List<UploadQueueEntity>

    @Query("UPDATE upload_queue SET status = :status, retry_count = :retryCount WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, retryCount: Int)
}
