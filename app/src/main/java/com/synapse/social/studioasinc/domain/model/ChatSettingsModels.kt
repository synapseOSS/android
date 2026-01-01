package com.synapse.social.studioasinc.domain.model

/**
 * Chat theme presets.
 */
enum class ChatThemePreset {
    DEFAULT,
    OCEAN,
    FOREST,
    SUNSET,
    MONOCHROME;

    fun displayName(): String = when (this) {
        DEFAULT -> "Default Purple"
        OCEAN -> "Ocean Blue"
        FOREST -> "Forest Green"
        SUNSET -> "Sunset Orange"
        MONOCHROME -> "Monochrome"
    }
}

/**
 * Wallpaper types.
 */
enum class WallpaperType {
    DEFAULT,
    SOLID_COLOR,
    IMAGE_URI
}

/**
 * Chat wallpaper configuration.
 */
data class ChatWallpaper(
    val type: WallpaperType = WallpaperType.DEFAULT,
    val value: String? = null // Color Int string or URI string
)
