package com.synapse.social.studioasinc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PostEntity::class, CommentEntity::class, UserEntity::class, ChatEntity::class], 
    version = 2, 
    exportSchema = true
)
@TypeConverters(MediaItemConverter::class, PollOptionConverter::class, ReactionTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "synapse_database"
                )
                // For v1, destructive migration is acceptable as this is initial offline implementation
                // TODO: Add proper migrations before v2 to preserve user data
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
