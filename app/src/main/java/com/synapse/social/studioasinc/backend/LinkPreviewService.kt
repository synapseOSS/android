package com.synapse.social.studioasinc.backend

import android.util.Log
import com.synapse.social.studioasinc.ui.chat.LinkPreviewData
import com.synapse.social.studioasinc.util.LinkDetectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for fetching OpenGraph metadata from URLs
 * Used to generate link previews in chat messages
 */
class LinkPreviewService {
    
    companion object {
        private const val TAG = "LinkPreviewService"
        private const val TIMEOUT_MS = 5000
        private const val MAX_CACHE_SIZE = 50
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
    
    // Simple in-memory cache for link previews
    private val cache = ConcurrentHashMap<String, LinkPreviewData>()
    
    /**
     * Fetch link preview data for a URL
     * @param url The URL to fetch preview for
     * @return Result containing LinkPreviewData or failure
     */
    suspend fun fetchLinkPreview(url: String): Result<LinkPreviewData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                cache[url]?.let { 
                    Log.d(TAG, "Cache hit for: $url")
                    return@withContext Result.success(it) 
                }
                
                Log.d(TAG, "Fetching preview for: $url")
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "text/html,application/xhtml+xml")
                    instanceFollowRedirects = true
                }
                
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    return@withContext Result.failure(Exception("HTTP $responseCode"))
                }
                
                val html = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    var linesRead = 0
                    // Only read first 100 lines to find meta tags (they should be in <head>)
                    while (reader.readLine().also { line = it } != null && linesRead < 100) {
                        sb.append(line)
                        linesRead++
                        // Stop if we've passed the head section
                        if (line?.contains("</head>", ignoreCase = true) == true) break
                    }
                    sb.toString()
                }
                
                connection.disconnect()
                
                val preview = parseOpenGraphTags(html, url)
                
                // Cache the result
                if (cache.size >= MAX_CACHE_SIZE) {
                    cache.keys.firstOrNull()?.let { cache.remove(it) }
                }
                cache[url] = preview
                
                Result.success(preview)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch preview for $url", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Parse OpenGraph meta tags from HTML
     */
    private fun parseOpenGraphTags(html: String, url: String): LinkPreviewData {
        val title = extractMetaContent(html, "og:title")
            ?: extractMetaContent(html, "twitter:title")
            ?: extractHtmlTitle(html)
        
        val description = extractMetaContent(html, "og:description")
            ?: extractMetaContent(html, "twitter:description")
            ?: extractMetaContent(html, "description")
        
        val imageUrl = extractMetaContent(html, "og:image")
            ?: extractMetaContent(html, "twitter:image")
        
        val domain = LinkDetectionService.extractDomain(url)
        
        return LinkPreviewData(
            url = url,
            title = title,
            description = description,
            imageUrl = normalizeImageUrl(imageUrl, url),
            domain = domain
        )
    }
    
    /**
     * Extract meta tag content by property or name
     */
    private fun extractMetaContent(html: String, property: String): String? {
        // Try property attribute first (OpenGraph style)
        val propertyPattern = """<meta[^>]*property\s*=\s*["']$property["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""".toRegex(RegexOption.IGNORE_CASE)
        propertyPattern.find(html)?.groupValues?.getOrNull(1)?.let { return it.trim() }
        
        // Also try with content before property
        val contentFirstPattern = """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]*property\s*=\s*["']$property["'][^>]*>""".toRegex(RegexOption.IGNORE_CASE)
        contentFirstPattern.find(html)?.groupValues?.getOrNull(1)?.let { return it.trim() }
        
        // Try name attribute (standard meta tags)
        val namePattern = """<meta[^>]*name\s*=\s*["']$property["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""".toRegex(RegexOption.IGNORE_CASE)
        namePattern.find(html)?.groupValues?.getOrNull(1)?.let { return it.trim() }
        
        // Content before name
        val nameContentFirstPattern = """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]*name\s*=\s*["']$property["'][^>]*>""".toRegex(RegexOption.IGNORE_CASE)
        nameContentFirstPattern.find(html)?.groupValues?.getOrNull(1)?.let { return it.trim() }
        
        return null
    }
    
    /**
     * Extract page title from <title> tag
     */
    private fun extractHtmlTitle(html: String): String? {
        val pattern = """<title[^>]*>([^<]+)</title>""".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(html)?.groupValues?.getOrNull(1)?.trim()
    }
    
    /**
     * Normalize relative image URLs to absolute URLs
     */
    private fun normalizeImageUrl(imageUrl: String?, baseUrl: String): String? {
        if (imageUrl == null) return null
        
        return when {
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            imageUrl.startsWith("//") -> "https:$imageUrl"
            imageUrl.startsWith("/") -> {
                try {
                    val url = URL(baseUrl)
                    "${url.protocol}://${url.host}$imageUrl"
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * Clear the preview cache
     */
    fun clearCache() {
        cache.clear()
    }
}
