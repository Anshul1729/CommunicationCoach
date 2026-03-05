package com.communicationcoach.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.communicationcoach.data.db.dao.ConversationDao
import com.communicationcoach.data.db.dao.DailyDigestDao
import com.communicationcoach.data.db.dao.InsightDao
import com.communicationcoach.data.db.dao.TranscriptChunkDao
import com.communicationcoach.data.db.dao.UploadQueueDao
import com.communicationcoach.data.db.dao.UserProfileDao
import com.communicationcoach.data.db.entity.ConversationEntity
import com.communicationcoach.data.db.entity.DailyDigestEntity
import com.communicationcoach.data.db.entity.InsightEntity
import com.communicationcoach.data.db.entity.TranscriptChunkEntity
import com.communicationcoach.data.db.entity.UploadQueueEntity
import com.communicationcoach.data.db.entity.UserProfileEntity

@Database(
    entities = [
        ConversationEntity::class,
        TranscriptChunkEntity::class,
        InsightEntity::class,
        DailyDigestEntity::class,
        UserProfileEntity::class,
        UploadQueueEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun transcriptChunkDao(): TranscriptChunkDao
    abstract fun insightDao(): InsightDao
    abstract fun dailyDigestDao(): DailyDigestDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun uploadQueueDao(): UploadQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "communication_coach.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
