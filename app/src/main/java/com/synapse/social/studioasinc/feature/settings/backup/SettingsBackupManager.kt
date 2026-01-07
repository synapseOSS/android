package com.synapse.social.studioasinc.feature.settings.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SettingsBackup(
    val version: Int = 1,
    val timestamp: Long,
    val settings: Map<String, String>
)

/**
 * Settings backup and restore manager for cloud sync and local backup.
 */
@Singleton
class SettingsBackupManager @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun createBackup(): SettingsBackup = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("synapse_settings", Context.MODE_PRIVATE)
        val settings = prefs.all.mapValues { it.value.toString() }
        
        SettingsBackup(
            timestamp = System.currentTimeMillis(),
            settings = settings
        )
    }
    
    suspend fun exportToFile(backup: SettingsBackup): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups")
        backupDir.mkdirs()
        
        val backupFile = File(backupDir, "settings_backup_${backup.timestamp}.json")
        val jsonString = json.encodeToString(SettingsBackup.serializer(), backup)
        backupFile.writeText(jsonString)
        
        backupFile
    }
    
    suspend fun importFromFile(file: File): SettingsBackup = withContext(Dispatchers.IO) {
        val jsonString = file.readText()
        json.decodeFromString(SettingsBackup.serializer(), jsonString)
    }
    
    suspend fun restoreBackup(backup: SettingsBackup) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("synapse_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        backup.settings.forEach { (key, value) ->
            when {
                value == "true" || value == "false" -> editor.putBoolean(key, value.toBoolean())
                value.toIntOrNull() != null -> editor.putInt(key, value.toInt())
                value.toLongOrNull() != null -> editor.putLong(key, value.toLong())
                value.toFloatOrNull() != null -> editor.putFloat(key, value.toFloat())
                else -> editor.putString(key, value)
            }
        }
        
        editor.apply()
    }
    
    suspend fun getAvailableBackups(): List<File> = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return@withContext emptyList()
        
        backupDir.listFiles { file ->
            file.name.startsWith("settings_backup_") && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
