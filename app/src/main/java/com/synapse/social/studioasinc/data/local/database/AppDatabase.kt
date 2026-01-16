package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PostEntity::class, 
        CommentEntity::class, 
        UserEntity::class
    ], 
    version = 5, 
    exportSchema = true
)
@TypeConverters(
    MediaItemConverter::class, 
    PollOptionConverter::class, 
    ReactionTypeConverter::class,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao

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
                // Add proper migrations to preserve user data
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true) // Allow destructive migration as fallback
                .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // Check if column already exists
                    val cursor = db.query("PRAGMA table_info(comments)")
                    var hasParentCommentId = false
                    
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        if (columnName == "parent_comment_id") {
                            hasParentCommentId = true
                            break
                        }
                    }
                    cursor.close()
                    
                    // Add parent_comment_id column to comments table if it doesn't exist
                    if (!hasParentCommentId) {
                        db.execSQL("ALTER TABLE comments ADD COLUMN parent_comment_id TEXT")
                    }
                } catch (e: Exception) {
                    // If migration fails, let Room handle it with destructive migration
                    throw e
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // Create deletion_operations table for tracking deletion history
                    // Requirements: 4.2, 5.4
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS deletion_operations (
                            id TEXT PRIMARY KEY NOT NULL,
                            userId TEXT NOT NULL,
                            deletionType TEXT NOT NULL,
                            chatIds TEXT,
                            status TEXT NOT NULL,
                            storageType TEXT NOT NULL,
                            messagesAffected INTEGER NOT NULL DEFAULT 0,
                            createdAt INTEGER NOT NULL,
                            completedAt INTEGER,
                            retryCount INTEGER NOT NULL DEFAULT 0,
                            errorMessage TEXT
                        )
                    """.trimIndent())

                    // Create deletion_retry_queue table for failed operation retries
                    // Requirements: 4.2, 4.5
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS deletion_retry_queue (
                            id TEXT PRIMARY KEY NOT NULL,
                            operationId TEXT NOT NULL,
                            scheduledRetryTime INTEGER NOT NULL,
                            maxRetries INTEGER NOT NULL DEFAULT 3,
                            currentRetry INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    // Create indexes for better query performance
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_operations_userId ON deletion_operations(userId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_operations_status ON deletion_operations(status)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_retry_queue_scheduledRetryTime ON deletion_retry_queue(scheduledRetryTime)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_retry_queue_operationId ON deletion_retry_queue(operationId)")
                    
                } catch (e: Exception) {
                    // If migration fails, let Room handle it with destructive migration
                    throw e
                }
            }
        }
    }
}
