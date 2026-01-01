package com.synapse.social.studioasinc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.BottomSheetCommentOptionsBinding
import com.synapse.social.studioasinc.model.CommentAction
import com.synapse.social.studioasinc.model.CommentWithUser

class CommentOptionsBottomSheet(
    private val comment: CommentWithUser,
    private val isOwnComment: Boolean,
    private val isPostAuthor: Boolean,
    private val onActionSelected: (CommentAction) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCommentOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCommentOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // Show owner/author actions
        val showOwnerActions = isOwnComment || isPostAuthor
        binding.dividerOwnerActions.isVisible = showOwnerActions
        binding.btnEdit.isVisible = isOwnComment && !comment.isDeleted
        binding.btnDelete.isVisible = isOwnComment
        binding.btnPin.isVisible = isPostAuthor && !comment.isDeleted
        
        // Update pin button text
        if (comment.isPinned) {
            binding.btnPin.text = getString(R.string.unpin_comment)
        }

        // Reply
        binding.btnReply.setOnClickListener {
            onActionSelected(CommentAction.Reply(comment.id, comment.userId))
            dismiss()
        }

        // Copy
        binding.btnCopy.setOnClickListener {
            copyToClipboard(comment.content)
            onActionSelected(CommentAction.Copy(comment.content))
            dismiss()
        }

        // Share
        binding.btnShare.setOnClickListener {
            shareComment()
            onActionSelected(CommentAction.Share(comment.id, comment.content, comment.postId))
            dismiss()
        }

        // Hide
        binding.btnHide.setOnClickListener {
            onActionSelected(CommentAction.Hide(comment.id))
            dismiss()
        }

        // Report
        binding.btnReport.setOnClickListener {
            showReportDialog()
        }

        // Pin (post author only)
        binding.btnPin.setOnClickListener {
            onActionSelected(CommentAction.Pin(comment.id, comment.postId))
            dismiss()
        }

        // Edit (owner only)
        binding.btnEdit.setOnClickListener {
            onActionSelected(CommentAction.Edit(comment.id, comment.content))
            dismiss()
        }

        // Delete (owner only)
        binding.btnDelete.setOnClickListener {
            onActionSelected(CommentAction.Delete(comment.id))
            dismiss()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("comment", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.comment_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareComment() {
        val shareText = "${comment.content}\n\nShared from Synapse"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun showReportDialog() {
        val reasons = arrayOf(
            getString(R.string.report_reason_spam),
            getString(R.string.report_reason_harassment),
            getString(R.string.report_reason_hate_speech),
            getString(R.string.report_reason_violence),
            getString(R.string.report_reason_misinformation),
            getString(R.string.report_reason_other)
        )
        
        val reasonKeys = arrayOf("spam", "harassment", "hate_speech", "violence", "misinformation", "other")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.report_comment)
            .setItems(reasons) { _, which ->
                onActionSelected(CommentAction.Report(comment.id, reasonKeys[which], null))
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CommentOptionsBottomSheet"
    }
}
