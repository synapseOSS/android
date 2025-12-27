package com.synapse.social.studioasinc

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter
import com.synapse.social.studioasinc.util.ChatMessageManager
import com.synapse.social.studioasinc.ui.chat.ChatActivity

/**
 * Handles attachment functionality in chat with Supabase integration.
 * Manages file selection, upload, and display of attachments.
 */
class AttachmentHandler(
    private val activity: ChatActivity,
    private val attachmentLayoutListHolder: View,
    private val rv_attacmentList: RecyclerView,
    private var attactmentmap: ArrayList<HashMap<String, Any>>,
    private val close_attachments_btn: View,
    private val galleryBtn: View,
    private val authRepository: AuthRepository
) {

    companion object {
        private const val TAG = "AttachmentHandler"
    }

    fun setup() {
        setupGalleryButton()
        setupCloseButton()
        setupRecyclerView()
    }

    private fun setupGalleryButton() {
        galleryBtn.setOnClickListener {
            // Open file picker for multiple file types
            StorageUtils.pickMultipleFiles(activity, "*/*", 1001) // REQ_CD_IMAGE_PICKER constant
        }
    }

    private fun setupCloseButton() {
        close_attachments_btn.setOnClickListener {
            clearAttachments()
        }
    }

    private fun setupRecyclerView() {
        val attachmentAdapter = Rv_attacmentListAdapter(activity, attactmentmap, attachmentLayoutListHolder)
        rv_attacmentList.adapter = attachmentAdapter
        rv_attacmentList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        // Add spacing between attachment items
        val attachmentSpacing = activity.resources.getDimension(R.dimen.spacing_small).toInt()
        rv_attacmentList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == 0) {
                    outRect.left = attachmentSpacing
                }
                outRect.right = attachmentSpacing
            }
        })
    }

    private fun clearAttachments() {
        // Hide attachment layout
        attachmentLayoutListHolder.visibility = View.GONE
        
        // Clear attachment list
        val oldSize = attactmentmap.size
        if (oldSize > 0) {
            attactmentmap.clear()
            rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
        }

        // Clear draft attachments from SharedPreferences
        clearDraftAttachments()
        
        // Update user presence
        updateUserPresence()
    }

    private fun clearDraftAttachments() {
        try {
            val drafts: SharedPreferences = activity.getSharedPreferences("chat_drafts", Context.MODE_PRIVATE)
            val currentUserId = authRepository.getCurrentUserId()
            val recipientUid = activity.intent.getStringExtra("uid")
            
            if (currentUserId != null && recipientUid != null) {
                val chatId = ChatMessageManager.getChatId(currentUserId, recipientUid)
                drafts.edit().remove("${chatId}_attachments").apply()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error clearing draft attachments: ${e.message}")
        }
    }

    private fun updateUserPresence() {
        try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                ChatPresenceManager.setActivity(currentUserId, "Idle")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating user presence: ${e.message}")
        }
    }

    fun startUploadForItem(position: Int) {
        try {
            // Upload functionality would need to be implemented based on actual ChatActivity structure
            android.util.Log.d(TAG, "Starting upload for item at position: $position")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error starting upload for item at position $position: ${e.message}")
        }
    }

    fun resetAttachmentState() {
        try {
            attachmentLayoutListHolder.visibility = View.GONE
            
            val oldSize = attactmentmap.size
            if (oldSize > 0) {
                attactmentmap.clear()
                rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error resetting attachment state: ${e.message}")
        }
    }

    /**
     * Add a new attachment to the list
     */
    fun addAttachment(filePath: String, fileType: String = "image") {
        try {
            val itemMap = HashMap<String, Any>().apply {
                put("localPath", filePath)
                put("uploadState", "pending")
                put("fileType", fileType)
                put("width", 100)
                put("height", 100)
            }

            val position = attactmentmap.size
            attactmentmap.add(itemMap)
            
            rv_attacmentList.adapter?.notifyItemInserted(position)
            
            // Show attachment layout if hidden
            if (attachmentLayoutListHolder.visibility != View.VISIBLE) {
                attachmentLayoutListHolder.visibility = View.VISIBLE
            }
            
            // Start upload
            startUploadForItem(position)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error adding attachment: ${e.message}")
        }
    }

    /**
     * Remove an attachment from the list
     */
    fun removeAttachment(position: Int) {
        try {
            if (position >= 0 && position < attactmentmap.size) {
                attactmentmap.removeAt(position)
                rv_attacmentList.adapter?.notifyItemRemoved(position)
                
                // Hide layout if no attachments left
                if (attactmentmap.isEmpty()) {
                    attachmentLayoutListHolder.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error removing attachment at position $position: ${e.message}")
        }
    }

    /**
     * Get the current attachment count
     */
    fun getAttachmentCount(): Int = attactmentmap.size

    /**
     * Check if there are any attachments
     */
    fun hasAttachments(): Boolean = attactmentmap.isNotEmpty()
}
