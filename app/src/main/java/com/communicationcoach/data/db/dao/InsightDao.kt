package com.communicationcoach.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.communicationcoach.data.db.entity.InsightEntity

@Dao
interface InsightDao {

    @Insert
    suspend fun insert(insight: InsightEntity): Long

    @Query("SELECT * FROM insights WHERE conversation_id = :conversationId LIMIT 1")
    suspend fun getForConversation(conversationId: Long): InsightEntity?

    @Query("SELECT * FROM insights ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<InsightEntity>
}
