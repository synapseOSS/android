/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.util.regex.Pattern

object LinkPreviewUtil {

    data class LinkData(
        val url: String,
        val title: String? = null,
        val description: String? = null,
        val imageUrl: String? = null,
        val domain: String? = null
    )

    fun extractUrl(text: String?): String? {
        if (text == null) return null
        val pattern = Pattern.compile("https?://\\S+")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group()
        } else null
    }
    
    /**
     * Extract domain from URL
     */
    fun extractDomain(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        
        return try {
            val uri = java.net.URI(url)
            uri.host
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchPreview(url: String): Result<LinkData> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                .get()

            val title = getMetaTag(doc, "og:title")?.takeIf { it.isNotEmpty() } ?: doc.title()
            val description = getMetaTag(doc, "og:description")?.takeIf { it.isNotEmpty() }
                ?: getMetaTag(doc, "description")
            val imageUrl = getMetaTag(doc, "og:image")
            val domain = URL(url).host

            val linkData = LinkData(
                url = url,
                title = title,
                description = description,
                imageUrl = imageUrl,
                domain = domain
            )

            Result.success(linkData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getMetaTag(document: Document, attr: String): String? {
        var element = document.select("meta[property=$attr]").first()
        if (element != null) {
            return element.attr("content")
        }
        element = document.select("meta[name=$attr]").first()
        if (element != null) {
            return element.attr("content")
        }
        return null
    }
}
