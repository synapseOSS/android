package com.synapse.social.studioasinc.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore singleton
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "synapse_app_settings"
)

class AppSettingsManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AppSettingsManager? = null

        fun getInstance(context: Context): AppSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // AI Configuration Keys
        private val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_AI_API_KEY = stringPreferencesKey("ai_api_key")
        private val KEY_AI_ENDPOINT = stringPreferencesKey("ai_endpoint")

        // Storage Configuration Keys
        private val KEY_STORAGE_PROVIDER = stringPreferencesKey("storage_provider")
        
        // ImgBB Keys
        private val KEY_IMGBB_API_KEY = stringPreferencesKey("imgbb_api_key")
        
        // Cloudinary Keys
        private val KEY_CLOUDINARY_CLOUD_NAME = stringPreferencesKey("cloudinary_cloud_name")
        private val KEY_CLOUDINARY_API_KEY = stringPreferencesKey("cloudinary_api_key")
        private val KEY_CLOUDINARY_API_SECRET = stringPreferencesKey("cloudinary_api_secret")
        
        // Cloudflare R2 Keys
        private val KEY_R2_ACCOUNT_ID = stringPreferencesKey("r2_account_id")
        private val KEY_R2_ACCESS_KEY_ID = stringPreferencesKey("r2_access_key_id")
        private val KEY_R2_SECRET_ACCESS_KEY = stringPreferencesKey("r2_secret_access_key")
        private val KEY_R2_BUCKET_NAME = stringPreferencesKey("r2_bucket_name")
    }

    private val dataStore: DataStore<Preferences>
        get() = context.appSettingsDataStore

    // AI Config
    val aiConfigFlow: Flow<AIConfig> = dataStore.data.map { preferences ->
        AIConfig(
            provider = preferences[KEY_AI_PROVIDER] ?: "Gemini",
            apiKey = preferences[KEY_AI_API_KEY] ?: "",
            endpoint = preferences[KEY_AI_ENDPOINT] ?: ""
        )
    }

    suspend fun updateAIConfig(config: AIConfig) {
        dataStore.edit { preferences ->
            preferences[KEY_AI_PROVIDER] = config.provider
            preferences[KEY_AI_API_KEY] = config.apiKey
            preferences[KEY_AI_ENDPOINT] = config.endpoint
        }
    }

    // Storage Config
    val storageConfigFlow: Flow<StorageConfig> = dataStore.data.map { preferences ->
        StorageConfig(
            provider = preferences[KEY_STORAGE_PROVIDER] ?: "ImgBB",
            imgBBConfig = ImgBBConfig(
                apiKey = preferences[KEY_IMGBB_API_KEY] ?: ""
            ),
            cloudinaryConfig = CloudinaryConfig(
                cloudName = preferences[KEY_CLOUDINARY_CLOUD_NAME] ?: "",
                apiKey = preferences[KEY_CLOUDINARY_API_KEY] ?: "",
                apiSecret = preferences[KEY_CLOUDINARY_API_SECRET] ?: ""
            ),
            r2Config = CloudflareR2Config(
                accountId = preferences[KEY_R2_ACCOUNT_ID] ?: "",
                accessKeyId = preferences[KEY_R2_ACCESS_KEY_ID] ?: "",
                secretAccessKey = preferences[KEY_R2_SECRET_ACCESS_KEY] ?: "",
                bucketName = preferences[KEY_R2_BUCKET_NAME] ?: ""
            )
        )
    }

    suspend fun updateStorageProvider(provider: String) {
        dataStore.edit { preferences ->
            preferences[KEY_STORAGE_PROVIDER] = provider
        }
    }

    suspend fun updateImgBBConfig(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[KEY_IMGBB_API_KEY] = apiKey
        }
    }

    suspend fun updateCloudinaryConfig(config: CloudinaryConfig) {
        dataStore.edit { preferences ->
            preferences[KEY_CLOUDINARY_CLOUD_NAME] = config.cloudName
            preferences[KEY_CLOUDINARY_API_KEY] = config.apiKey
            preferences[KEY_CLOUDINARY_API_SECRET] = config.apiSecret
        }
    }

    suspend fun updateR2Config(config: CloudflareR2Config) {
        dataStore.edit { preferences ->
            preferences[KEY_R2_ACCOUNT_ID] = config.accountId
            preferences[KEY_R2_ACCESS_KEY_ID] = config.accessKeyId
            preferences[KEY_R2_SECRET_ACCESS_KEY] = config.secretAccessKey
            preferences[KEY_R2_BUCKET_NAME] = config.bucketName
        }
    }
}

data class AIConfig(
    val provider: String,
    val apiKey: String,
    val endpoint: String
)

data class StorageConfig(
    val provider: String,
    val imgBBConfig: ImgBBConfig,
    val cloudinaryConfig: CloudinaryConfig,
    val r2Config: CloudflareR2Config
)

data class ImgBBConfig(
    val apiKey: String
)

data class CloudinaryConfig(
    val cloudName: String,
    val apiKey: String,
    val apiSecret: String
)

data class CloudflareR2Config(
    val accountId: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String
)
