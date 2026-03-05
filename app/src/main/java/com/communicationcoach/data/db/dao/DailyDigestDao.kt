package com.communicationcoach.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.communicationcoach.data.db.entity.DailyDigestEntity

@Dao
interface DailyDigestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(digest: DailyDigestEntity): Long

    @Query("SELECT * FROM daily_digests WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): DailyDigestEntity?

    @Query("SELECT * FROM daily_digests ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 7): List<DailyDigestEntity>
}
