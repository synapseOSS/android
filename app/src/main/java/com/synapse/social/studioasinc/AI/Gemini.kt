package com.synapse.social.studioasinc.AI

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.synapse.social.studioasinc.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Gemini private constructor(
    private val context: Context,
    private var model: String,
    private var responseType: String,
    private var tone: String,
    private var size: String,
    private var maxTokens: Int,
    private var temperature: Double,
    private var showThinking: Boolean,
    private var thinkingText: String,
    private var systemInstruction: String,
    private var responseTextView: TextView?
) {

    interface GeminiCallback {
        fun onSuccess(response: String)
        fun onError(error: String)
        fun onThinking()
    }

    class Builder(private val context: Context) {
        private var model: String = "gemini-1.5-flash"
        private var responseType: String = "text"
        private var tone: String = "normal"
        private var size: String = "normal"
        private var maxTokens: Int = 2500
        private var temperature: Double = 1.0
        private var showThinking: Boolean = false
        private var thinkingText: String = "Thinking..."
        private var systemInstruction: String = 
            "Your name is Synapse AI, you are an AI made for Synapse (social media) assistance"
        private var responseTextView: TextView? = null

        fun model(model: String) = apply { this.model = model }
        fun responseType(responseType: String) = apply { this.responseType = responseType }
        fun tone(tone: String) = apply { this.tone = tone }
        fun size(size: String) = apply { this.size = size }
        fun maxTokens(maxTokens: Int) = apply { this.maxTokens = maxTokens }
        fun temperature(temperature: Double) = apply { this.temperature = temperature }
        fun showThinking(showThinking: Boolean) = apply { this.showThinking = showThinking }
        fun thinkingText(thinkingText: String) = apply { this.thinkingText = thinkingText }
        fun systemInstruction(systemInstruction: String) = apply { this.systemInstruction = systemInstruction }
        fun responseTextView(textView: TextView) = apply { this.responseTextView = textView }

        fun build(): Gemini {
            return Gemini(
                context = context,
                model = model,
                responseType = responseType,
                tone = tone,
                size = size,
                maxTokens = maxTokens,
                temperature = temperature,
                showThinking = showThinking,
                thinkingText = thinkingText,
                systemInstruction = systemInstruction,
                responseTextView = responseTextView
            )
        }
    }

    fun sendPrompt(prompt: String, callback: GeminiCallback? = null) {
        if (prompt.isBlank()) {
            handleError("Prompt is empty!", callback)
            return
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            handleError("No API key available!", callback)
            return
        }

        if (showThinking) {
            callback?.onThinking() ?: responseTextView?.post {
                responseTextView?.text = thinkingText
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    sendGeminiRequest(prompt, apiKey)
                }

                responseTextView?.text = response
                callback?.onSuccess(response)
            } catch (e: Exception) {
                val error = "Error: ${e.message}"
                Log.e(TAG, error, e)
                handleError(error, callback)
            }
        }
    }

    private suspend fun sendGeminiRequest(prompt: String, apiKey: String): String {
        val generativeModel = Firebase.vertexAI.generativeModel(
            modelName = model,
            generationConfig = generationConfig {
                temperature = this@Gemini.temperature.toFloat()
                maxOutputTokens = maxTokens
            },
            systemInstruction = content { text(buildFullSystemInstruction()) }
        )

        val response = generativeModel.generateContent(prompt)
        return response.text ?: "No response generated"
    }

    private fun buildFullSystemInstruction(): String {
        return buildString {
            append(systemInstruction)

            if (tone != "normal") {
                append(" Respond in a $tone tone.")
            }

            if (size != "normal") {
                append(" Make the response $size in length.")
            }

            if (responseType != "text") {
                append(" Format the response as $responseType.")
            }
        }
    }

    private fun handleError(error: String, callback: GeminiCallback?) {
        callback?.onError(error) ?: responseTextView?.post {
            responseTextView?.text = error
        }
    }

    // Getters and setters
    fun setModel(model: String) { this.model = model }
    fun setResponseType(responseType: String) { this.responseType = responseType }
    fun setTone(tone: String) { this.tone = tone }
    fun setSize(size: String) { this.size = size }
    fun setMaxTokens(maxTokens: Int) { this.maxTokens = maxTokens }
    fun setTemperature(temperature: Double) { this.temperature = temperature }
    fun setShowThinking(showThinking: Boolean) { this.showThinking = showThinking }
    fun setThinkingText(thinkingText: String) { this.thinkingText = thinkingText }
    fun setSystemInstruction(systemInstruction: String) { this.systemInstruction = systemInstruction }
    fun setResponseTextView(responseTextView: TextView?) { this.responseTextView = responseTextView }

    fun getModel(): String = model
    fun getResponseType(): String = responseType
    fun getTone(): String = tone
    fun getSize(): String = size
    fun getMaxTokens(): Int = maxTokens
    fun getTemperature(): Double = temperature
    fun isShowThinking(): Boolean = showThinking
    fun getThinkingText(): String = thinkingText
    fun getSystemInstruction(): String = systemInstruction

    companion object {
        private const val TAG = "GeminiAPI"
    }
}
