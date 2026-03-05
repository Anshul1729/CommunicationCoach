package com.communicationcoach.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.communicationcoach.data.db.entity.TranscriptChunkEntity

@Dao
interface TranscriptChunkDao {

    @Insert
    suspend fun insertChunk(chunk: TranscriptChunkEntity)

    @Query("SELECT * FROM transcript_chunks WHERE conversation_id = :conversationId ORDER BY chunk_number ASC")
    suspend fun getChunksForConversation(conversationId: Long): List<TranscriptChunkEntity>

    @Query("SELECT GROUP_CONCAT(transcript, ' ') FROM transcript_chunks WHERE conversation_id = :conversationId ORDER BY chunk_number ASC")
    suspend fun getFullTranscriptForConversation(conversationId: Long): String?
}
