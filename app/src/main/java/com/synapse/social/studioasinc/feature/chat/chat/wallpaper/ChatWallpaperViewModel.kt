package com.synapse.social.studioasinc.ui.chat.wallpaper

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.domain.model.ChatWallpaper
import com.synapse.social.studioasinc.domain.model.WallpaperType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ChatWallpaperViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _currentWallpaper = MutableStateFlow(ChatWallpaper())
    val currentWallpaper: StateFlow<ChatWallpaper> = _currentWallpaper.asStateFlow()

    init {
        loadCurrentWallpaper()
    }

    private fun loadCurrentWallpaper() {
        viewModelScope.launch {
            val settings = settingsRepository.chatSettings.first()
            _currentWallpaper.value = settings.wallpaper
        }
    }

    fun setSolidColor(colorHex: String) {
        viewModelScope.launch {
            val wallpaper = ChatWallpaper(
                type = WallpaperType.SOLID_COLOR,
                value = colorHex
            )
            _currentWallpaper.value = wallpaper
            settingsRepository.setChatWallpaper(wallpaper)
        }
    }

    fun setImageWallpaper(uri: Uri) {
        viewModelScope.launch {
            try {
                // Copy image to internal storage for persistence
                val savedUri = copyImageToInternalStorage(uri)

                if (savedUri != null) {
                    // Delete old wallpaper if it exists and is an image
                    val oldWallpaper = _currentWallpaper.value
                    if (oldWallpaper.type == WallpaperType.IMAGE_URI && oldWallpaper.value != null) {
                        deleteFile(oldWallpaper.value)
                    }

                    val wallpaper = ChatWallpaper(
                        type = WallpaperType.IMAGE_URI,
                        value = savedUri.toString()
                    )
                    _currentWallpaper.value = wallpaper
                    settingsRepository.setChatWallpaper(wallpaper)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun copyImageToInternalStorage(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null

            // Create a file in the app's internal storage directory
            val fileName = "chat_wallpaper_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteFile(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val wallpaper = ChatWallpaper(
                type = WallpaperType.DEFAULT,
                value = null
            )
            _currentWallpaper.value = wallpaper
            settingsRepository.setChatWallpaper(wallpaper)
        }
    }
}
