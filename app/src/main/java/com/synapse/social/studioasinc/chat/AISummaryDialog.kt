package com.synapse.social.studioasinc.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import com.synapse.social.studioasinc.databinding.DialogAiSummaryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dialog for displaying AI-generated message summaries
 * Shows loading state, summary text, character count, read time, and original message
 * Supports copy to clipboard and error handling with retry
 */
class AISummaryDialog : DialogFragment() {

    companion object {
        private const val TAG = "AISummaryDialog"
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_MESSAGE_TEXT = "message_text"
        private const val MIN_MESSAGE_LENGTH = 100

        /**
         * Create a new instance of AISummaryDialog
         * 
         * @param messageId The ID of the message to summarize
         * @param messageText The text content of the message
         * @return New instance of AISummaryDialog
         */
        fun newInstance(messageId: String, messageText: String): AISummaryDialog {
            val dialog = AISummaryDialog()
            val args = Bundle().apply {
                putString(ARG_MESSAGE_ID, messageId)
                putString(ARG_MESSAGE_TEXT, messageText)
            }
            dialog.arguments = args
            return dialog
        }
    }

    private var _binding: DialogAiSummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MessageActionsViewModel

    private var messageId: String = ""
    private var messageText: String = ""
    private var isOriginalMessageExpanded = false
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)

        // Get arguments
        arguments?.let {
            messageId = it.getString(ARG_MESSAGE_ID, "")
            messageText = it.getString(ARG_MESSAGE_TEXT, "")
        }

        // Initialize ViewModel
        viewModel = MessageActionsViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Validate message length
        if (messageText.length < MIN_MESSAGE_LENGTH) {
            showError("Message is too short to summarize (minimum $MIN_MESSAGE_LENGTH characters)", false)
            return
        }

        setupOriginalMessageSection()
        setupButtons()
        generateSummary()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Set up the collapsible original message section
     */
    private fun setupOriginalMessageSection() {
        binding.tvOriginalMessage.text = messageText

        binding.layoutOriginalMessageHeader.setOnClickListener {
            toggleOriginalMessage()
        }
    }

    /**
     * Toggle the visibility of the original message
     */
    private fun toggleOriginalMessage() {
        isOriginalMessageExpanded = !isOriginalMessageExpanded

        if (isOriginalMessageExpanded) {
            binding.tvOriginalMessage.visibility = View.VISIBLE
            binding.ivExpandCollapse.setImageResource(android.R.drawable.arrow_up_float)
        } else {
            binding.tvOriginalMessage.visibility = View.GONE
            binding.ivExpandCollapse.setImageResource(android.R.drawable.arrow_down_float)
        }
    }

    /**
     * Set up button click listeners
     */
    private fun setupButtons() {
        binding.btnCopy.setOnClickListener {
            copyToClipboard()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnRetry.setOnClickListener {
            generateSummary()
        }
    }

    /**
     * Generate AI summary for the message
     */
    private fun generateSummary() {
        Log.d(TAG, "Generating AI summary for message: $messageId")

        // Hide error and retry button
        binding.tvErrorMessage.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE

        lifecycleScope.launch {
            viewModel.generateAISummary(messageId, messageText)
                .collectLatest { state ->
                    when {
                        state.isGenerating -> {
                            showLoading()
                        }
                        state.summary != null -> {
                            showSummary(
                                summary = state.summary,
                                characterCount = state.characterCount,
                                readTime = state.estimatedReadTime
                            )
                        }
                        state.error != null -> {
                            val isRateLimitError = state.rateLimitResetTime > 0
                            showError(state.error, !isRateLimitError)
                            
                            if (isRateLimitError) {
                                startRateLimitCountdown(state.rateLimitResetTime)
                            }
                        }
                    }
                }
        }
    }

    /**
     * Show loading state with shimmer effect
     */
    private fun showLoading() {
        Log.d(TAG, "Showing loading state")
        
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutSummaryContent.visibility = View.GONE
        binding.layoutButtons.visibility = View.GONE
        binding.tvErrorMessage.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
        
        // Apply pulse animation to progress bar
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
        binding.progressBar.startAnimation(pulseAnimation)
    }

    /**
     * Show summary content with smooth transition
     * 
     * @param summary The generated summary text
     * @param characterCount The character count of the original message
     * @param readTime The estimated reading time in minutes
     */
    private fun showSummary(summary: String, characterCount: Int, readTime: Int) {
        Log.d(TAG, "Showing summary")
        
        // Clear animation from progress bar
        binding.progressBar.clearAnimation()
        
        binding.progressBar.visibility = View.GONE
        binding.layoutSummaryContent.visibility = View.VISIBLE
        binding.layoutButtons.visibility = View.VISIBLE
        binding.tvErrorMessage.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE

        // Set summary text
        binding.tvSummaryText.text = summary

        // Set character count
        binding.tvCharacterCount.text = "$characterCount characters"

        // Set estimated read time
        binding.tvReadTime.text = "~$readTime min read"
        
        // Apply fade-in animation to summary content
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_message)
        binding.layoutSummaryContent.startAnimation(fadeInAnimation)
    }

    /**
     * Show error message
     * 
     * @param error The error message to display
     * @param showRetry Whether to show the retry button
     */
    private fun showError(error: String, showRetry: Boolean) {
        Log.e(TAG, "Showing error: $error")
        
        binding.progressBar.visibility = View.GONE
        binding.layoutSummaryContent.visibility = View.GONE
        binding.layoutButtons.visibility = View.VISIBLE
        binding.tvErrorMessage.visibility = View.VISIBLE
        binding.tvErrorMessage.text = error
        binding.btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    /**
     * Start countdown timer for rate limit
     * 
     * @param resetTime The timestamp when the rate limit will reset
     */
    private fun startRateLimitCountdown(resetTime: Long) {
        // Cancel existing timer if any
        countDownTimer?.cancel()

        val currentTime = System.currentTimeMillis()
        val remainingTime = resetTime - currentTime

        if (remainingTime <= 0) {
            return
        }

        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60

                val countdownText = if (minutes > 0) {
                    "Rate limit reached. Try again in ${minutes}m ${remainingSeconds}s"
                } else {
                    "Rate limit reached. Try again in ${remainingSeconds}s"
                }

                binding.tvErrorMessage.text = countdownText
            }

            override fun onFinish() {
                binding.tvErrorMessage.text = "Rate limit expired. You can try again now."
                binding.btnRetry.visibility = View.VISIBLE
            }
        }.start()
    }

    /**
     * Copy summary text to clipboard
     */
    private fun copyToClipboard() {
        val summaryText = binding.tvSummaryText.text?.toString() ?: return

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Summary", summaryText)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), "Summary copied", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Summary copied to clipboard")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}
