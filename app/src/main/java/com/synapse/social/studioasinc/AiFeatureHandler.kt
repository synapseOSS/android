package com.synapse.social.studioasinc

import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.SendMessageUseCase
import com.synapse.social.studioasinc.AI.Gemini
import com.synapse.social.studioasinc.ChatConstants.KEY_KEY
import com.synapse.social.studioasinc.ChatConstants.MESSAGE_TEXT_KEY
import com.synapse.social.studioasinc.ChatConstants.UID_KEY
import kotlinx.coroutines.launch
import java.util.HashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Handles AI features in chat using Gemini AI with Supabase integration.
 * Provides text correction, reply suggestions, and message analysis.
 */
class AiFeatureHandler(
    private val activity: AppCompatActivity,
    private val gemini: Gemini,
    private val message_et: EditText,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val authRepository: AuthRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private var secondUserName: String,
    private val mMessageReplyLayoutBodyRightUsername: TextView,
    private val mMessageReplyLayoutBodyRightMessage: TextView
) {

    companion object {
        private const val TAG = "AiFeatureHandler"
    }

    fun setSecondUserName(name: String) {
        this.secondUserName = name
    }

    private data class AiFeatureParams(
        val prompt: String,
        val systemInstruction: String,
        val model: String,
        val bottomSheetTitle: String,
        val logTag: String,
        val errorMessage: String,
        val viewHolder: BaseMessageViewHolder,
        val maxTokens: Int?
    )

    /**
     * Handle long click on send button for AI assistance
     */
    fun handleSendButtonLongClick(replyMessageID: String): Boolean {
        return try {
            if (message_et.text.toString().isNotEmpty()) {
                // Text correction mode
                val prompt = buildTextCorrectionPrompt(message_et.text.toString())
                callGeminiForSend(prompt, true)
            } else {
                // Reply suggestion mode
                if (replyMessageID.isNotEmpty() && replyMessageID != "null") {
                    generateReplyFromContext(replyMessageID)
                } else {
                    generateGenericGreeting()
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error handling send button long click: ${e.message}")
            false
        }
    }

    private fun buildTextCorrectionPrompt(text: String): String {
        return "Fix grammar, punctuation, and clarity without changing meaning. " +
                "Preserve original formatting (line breaks, lists, markdown). " +
                "Censor profanity by replacing letters with asterisks. " +
                "Return ONLY the corrected RAW text.\n```$text```"
    }

    private fun generateReplyFromContext(replyMessageID: String) {
        try {
            val repliedMessageIndex = chatMessagesList.indexOfFirst { 
                it[KEY_KEY]?.toString() == replyMessageID 
            }

            if (repliedMessageIndex != -1) {
                val contextBuilder = buildConversationContext(repliedMessageIndex)
                val prompt = contextBuilder.toString()
                callGeminiForSend(prompt, false)
            } else {
                generateGenericGreeting()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error generating reply from context: ${e.message}")
            generateGenericGreeting()
        }
    }

    private fun buildConversationContext(repliedMessageIndex: Int): StringBuilder {
        val contextBuilder = StringBuilder()
        contextBuilder.append("You are helping 'Me' to write a reply in a conversation with '$secondUserName'.\n")
        contextBuilder.append("Here is the recent chat history:\n---\n")

        val startIndex = max(0, repliedMessageIndex - 10)
        val endIndex = min(chatMessagesList.size - 1, repliedMessageIndex + 10)

        for (i in startIndex..endIndex) {
            val message = chatMessagesList[i]
            val currentUserId = authRepository.getCurrentUserId()
            val sender = if (message[UID_KEY].toString() == currentUserId) "Me" else secondUserName
            contextBuilder.append("$sender: ${message[MESSAGE_TEXT_KEY]}\n")
        }

        contextBuilder.append("---\n")

        val repliedMessageSender = mMessageReplyLayoutBodyRightUsername.text.toString()
        val repliedMessageText = mMessageReplyLayoutBodyRightMessage.text.toString()

        contextBuilder.append("I need to reply to this message from '$repliedMessageSender': \"$repliedMessageText\"\n")
        contextBuilder.append("Based on the conversation history, please suggest a short, relevant reply from 'Me'.")

        return contextBuilder
    }

    private fun generateGenericGreeting() {
        val prompt = "Suggest a generic, friendly greeting."
        callGeminiForSend(prompt, false)
    }

    private fun callGeminiForSend(prompt: String, showThinking: Boolean) {
        try {
            val geminiInstance = Gemini.Builder(activity)
                .model("gemini-2.5-flash-lite")
                .showThinking(showThinking)
                .systemInstruction(
                    "You are a concise text assistant. Always return ONLY the transformed text (no explanation, no labels). " +
                            "Preserve original formatting. Censor profanity by replacing letters with asterisks (e.g., s***t). " +
                            "Keep the language and tone of the input unless asked to change it."
                )
                .build()
            
            geminiInstance.sendPrompt(prompt, object : Gemini.GeminiCallback {
                override fun onSuccess(response: String) {
                    activity.runOnUiThread { 
                        message_et.setText(response)
                    }
                }

                override fun onError(error: String) {
                    activity.runOnUiThread { 
                        Toast.makeText(activity, "AI Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onThinking() {
                    if (showThinking) {
                        activity.runOnUiThread { 
                            message_et.setText("Thinking...")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error calling Gemini for send: ${e.message}")
            activity.runOnUiThread {
                Toast.makeText(activity, "AI service unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Generate message summary using AI
     */
    internal fun callGeminiForSummary(prompt: String, viewHolder: BaseMessageViewHolder) {
        val params = AiFeatureParams(
            prompt,
            activity.getString(R.string.gemini_system_instruction_summary),
            "gemini-2.5-flash-lite",
            activity.getString(R.string.gemini_summary_title),
            "GeminiSummary",
            activity.getString(R.string.gemini_error_summary),
            viewHolder,
            null
        )
        callGeminiForAiFeature(params)
    }

    /**
     * Generate message explanation using AI
     */
    internal fun callGeminiForExplanation(prompt: String, viewHolder: BaseMessageViewHolder) {
        val params = AiFeatureParams(
            prompt,
            activity.getString(R.string.gemini_system_instruction_explanation),
            "gemini-2.5-flash",
            activity.getString(R.string.gemini_explanation_title),
            "GeminiExplanation",
            activity.getString(R.string.gemini_error_explanation),
            viewHolder,
            null
        )
        callGeminiForAiFeature(params)
    }

    private fun callGeminiForAiFeature(params: AiFeatureParams) {
        try {
            val builder = Gemini.Builder(activity)
                .model(params.model)
                .showThinking(true)
                .systemInstruction(params.systemInstruction)

            params.maxTokens?.let { builder.maxTokens(it) }

            val gemini = builder.build()

            gemini.sendPrompt(params.prompt, object : Gemini.GeminiCallback {
                override fun onSuccess(response: String) {
                    activity.runOnUiThread {
                        params.viewHolder.stopShimmer()
                        val bottomSheet = ContentDisplayBottomSheetDialogFragment.newInstance(
                            response, 
                            params.bottomSheetTitle
                        )
                        bottomSheet.show(activity.supportFragmentManager, bottomSheet.tag)
                    }
                }

                override fun onError(error: String) {
                    activity.runOnUiThread {
                        params.viewHolder.stopShimmer()
                        Toast.makeText(activity, "${params.errorMessage}$error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onThinking() {
                    activity.runOnUiThread {
                        params.viewHolder.startShimmer()
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error calling Gemini for AI feature: ${e.message}")
            activity.runOnUiThread {
                params.viewHolder.stopShimmer()
                Toast.makeText(activity, "AI feature unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Check if AI features are available
     */
    fun isAiAvailable(): Boolean {
        return try {
            gemini != null
        } catch (e: Exception) {
            false
        }
    }
}
