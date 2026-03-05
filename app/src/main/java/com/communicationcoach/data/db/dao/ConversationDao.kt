package com.communicationcoach.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.communicationcoach.data.db.entity.ConversationEntity

@Dao
interface ConversationDao {

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET end_time = :endTime, duration_seconds = :durationSeconds, status = :status WHERE id = :id")
    suspend fun update(id: Long, endTime: Long, durationSeconds: Int, status: String)

    @Query("UPDATE conversations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY start_time DESC")
    suspend fun getAll(): List<ConversationEntity>

    @Query("SELECT * FROM conversations ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ConversationEntity>

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transcript_chunks WHERE conversation_id = :conversationId")
    suspend fun deleteChunksForConversation(conversationId: Long)
}
