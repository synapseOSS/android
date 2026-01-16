package com.synapse.social.studioasinc.data.local.database

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

        // Storage Configuration Keys - Single provider per media type
        private val KEY_PHOTO_PROVIDER = stringPreferencesKey("photo_provider")
        private val KEY_VIDEO_PROVIDER = stringPreferencesKey("video_provider")
        private val KEY_OTHER_PROVIDER = stringPreferencesKey("other_provider")
        
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

        // Supabase Keys
        private val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        private val KEY_SUPABASE_KEY = stringPreferencesKey("supabase_key")
        private val KEY_SUPABASE_BUCKET = stringPreferencesKey("supabase_bucket")
    }

    private val dataStore: DataStore<Preferences>
        get() = context.appSettingsDataStore

    // Storage Config
    val storageConfigFlow: Flow<StorageConfig> = dataStore.data.map { preferences ->
        StorageConfig(
            photoProvider = preferences[KEY_PHOTO_PROVIDER]?.takeIf { it.isNotBlank() },
            videoProvider = preferences[KEY_VIDEO_PROVIDER]?.takeIf { it.isNotBlank() },
            otherProvider = preferences[KEY_OTHER_PROVIDER]?.takeIf { it.isNotBlank() },
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
            ),
            supabaseConfig = SupabaseConfig(
                url = preferences[KEY_SUPABASE_URL] ?: "",
                apiKey = preferences[KEY_SUPABASE_KEY] ?: "",
                bucketName = preferences[KEY_SUPABASE_BUCKET] ?: ""
            )
        )
    }

    suspend fun updatePhotoProvider(provider: String?) {
        dataStore.edit { preferences ->
            preferences[KEY_PHOTO_PROVIDER] = provider ?: ""
        }
    }

    suspend fun updateVideoProvider(provider: String?) {
        dataStore.edit { preferences ->
            preferences[KEY_VIDEO_PROVIDER] = provider ?: ""
        }
    }

    suspend fun updateOtherProvider(provider: String?) {
        dataStore.edit { preferences ->
            preferences[KEY_OTHER_PROVIDER] = provider ?: ""
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

    suspend fun updateSupabaseConfig(config: SupabaseConfig) {
        dataStore.edit { preferences ->
            preferences[KEY_SUPABASE_URL] = config.url
            preferences[KEY_SUPABASE_KEY] = config.apiKey
            preferences[KEY_SUPABASE_BUCKET] = config.bucketName
        }
    }
}

data class StorageConfig(
    val photoProvider: String?,  // Selected provider for photos (e.g., ImgBB, Cloudinary)
    val videoProvider: String?,  // Selected provider for videos (e.g., Cloudinary, Supabase)
    val otherProvider: String?,  // Selected provider for other files (e.g., Supabase, Cloudflare R2)
    val imgBBConfig: ImgBBConfig,
    val cloudinaryConfig: CloudinaryConfig,
    val r2Config: CloudflareR2Config,
    val supabaseConfig: SupabaseConfig
) {
    // Helper functions to check if a provider is properly configured
    fun isImgBBConfigured(): Boolean = imgBBConfig.apiKey.isNotBlank()
    
    fun isCloudinaryConfigured(): Boolean = 
        cloudinaryConfig.cloudName.isNotBlank() && 
        cloudinaryConfig.apiKey.isNotBlank() && 
        cloudinaryConfig.apiSecret.isNotBlank()
    
    fun isR2Configured(): Boolean = 
        r2Config.accountId.isNotBlank() &&
        r2Config.accessKeyId.isNotBlank() &&
        r2Config.secretAccessKey.isNotBlank() &&
        r2Config.bucketName.isNotBlank()
    
    fun isSupabaseConfigured(): Boolean = 
        supabaseConfig.url.isNotBlank() &&
        supabaseConfig.apiKey.isNotBlank() &&
        supabaseConfig.bucketName.isNotBlank()
    
    fun isProviderConfigured(providerName: String): Boolean = when (providerName) {
        "Default" -> true  // Default is always configured (uses app credentials)
        "ImgBB" -> isImgBBConfigured()
        "Cloudinary" -> isCloudinaryConfigured()
        "Cloudflare R2" -> isR2Configured()
        "Supabase" -> isSupabaseConfigured()
        else -> false
    }
}

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

data class SupabaseConfig(
    val url: String,
    val apiKey: String,
    val bucketName: String
)

