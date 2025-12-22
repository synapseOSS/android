package com.synapse.social.studioasinc.data.model

/**
 * Data model representing an open source library license.
 *
 * @property library The name of the library (e.g. "Kotlin Standard Library")
 * @property developer The developer or organization (e.g. "JetBrains")
 * @property year The copyright year (e.g. "2023")
 * @property licenseType The type of license (e.g. "Apache 2.0", "MIT")
 * @property licenseUrl The URL to the license text
 * @property licenseContent The full text of the license
 */
data class License(
    val library: String,
    val developer: String,
    val year: String,
    val licenseType: String,
    val licenseUrl: String,
    val licenseContent: String
)
