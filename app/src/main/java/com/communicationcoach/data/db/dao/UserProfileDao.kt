package com.communicationcoach.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.communicationcoach.data.db.entity.UserProfileEntity

@Dao
interface UserProfileDao {

    // INSERT OR REPLACE — keeps it a singleton (id = 1 always)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?
}
