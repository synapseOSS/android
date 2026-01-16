package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseMigrationHelper {
    
    suspend fun fixDatabaseSchema(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the database path
            val dbPath = context.getDatabasePath("synapse_database")
            
            if (!dbPath.exists()) {
                // Database doesn't exist, no need to fix
                return@withContext true
            }
            
            // Create a temporary database connection to check and fix schema
            val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("synapse_database")
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Not needed for existing database
                    }
                    
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // Apply migration if needed
                        if (oldVersion < 2) {
                            try {
                                db.execSQL("ALTER TABLE comments ADD COLUMN parent_comment_id TEXT")
                            } catch (e: Exception) {
                                // Column might already exist, ignore
                            }
                        }
                    }
                })
                .build()
            
            val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
            val db = helper.writableDatabase
            
            // Check if parent_comment_id column exists in comments table
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
            
            // Add the column if it doesn't exist
            if (!hasParentCommentId) {
                db.execSQL("ALTER TABLE comments ADD COLUMN parent_comment_id TEXT")
            }
            
            db.close()
            helper.close()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun clearDatabaseIfCorrupted(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbPath = context.getDatabasePath("synapse_database")
            if (dbPath.exists()) {
                dbPath.delete()
                // Also delete WAL and SHM files
                context.getDatabasePath("synapse_database-wal").delete()
                context.getDatabasePath("synapse_database-shm").delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
