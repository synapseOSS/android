package com.synapse.social.studioasinc.chat

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.synapse.social.studioasinc.ChatActivity
import com.synapse.social.studioasinc.ChatAdapter
import com.synapse.social.studioasinc.R

/**
 * Manages multi-select mode for messages in chat
 * Handles selection state tracking and action toolbar lifecycle
 */
class MultiSelectManager(
    private val activity: ChatActivity,
    private val adapter: ChatAdapter
) {
    // Selection state
    private val selectedMessageIds = mutableSetOf<String>()
    var isMultiSelectMode = false
        private set
    
    // Toolbar references
    private var actionToolbar: MaterialToolbar? = null
    private var standardToolbar: MaterialToolbar? = null
    private var selectionCountText: TextView? = null
    
    // Scroll position tracking for stability
    private var savedScrollPosition: Int = -1
    private var savedScrollOffset: Int = 0
    
    // Message queue for incoming messages during multi-select mode
    private val queuedMessages = mutableListOf<HashMap<String, Any?>>()
    
    // Callback for queued messages
    var onQueuedMessagesReady: ((List<HashMap<String, Any?>>) -> Unit)? = null
    
    // Debouncing for UI updates
    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private var pendingUiUpdate: Runnable? = null
    private val uiUpdateDebounceMs = 100L // 100ms debounce delay
    
    /**
     * Enter multi-select mode with an initial message selected
     * @param initialMessageId The ID of the first message to select
     */
    fun enterMultiSelectMode(initialMessageId: String) {
        if (isMultiSelectMode) return
        
        isMultiSelectMode = true
        selectedMessageIds.clear()
        selectedMessageIds.add(initialMessageId)
        
        // Provide haptic feedback when entering multi-select mode
        provideHapticFeedback()
        
        // Save scroll position before entering multi-select mode
        saveScrollPosition()
        
        // Show action toolbar
        showActionToolbar()
        
        // Update adapter to show selection indicators
        adapter.isMultiSelectMode = true
        adapter.notifyDataSetChanged()
    }
    
    /**
     * Save current scroll position for restoration later
     * Requirements: 8.4
     */
    private fun saveScrollPosition() {
        val recyclerView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.ChatMessagesListRecycler)
        val layoutManager = recyclerView?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        
        if (layoutManager != null) {
            savedScrollPosition = layoutManager.findFirstVisibleItemPosition()
            val firstVisibleView = layoutManager.findViewByPosition(savedScrollPosition)
            savedScrollOffset = firstVisibleView?.top ?: 0
        }
    }
    
    /**
     * Restore previously saved scroll position
     * Requirements: 8.4
     */
    private fun restoreScrollPosition() {
        if (savedScrollPosition == -1) return
        
        val recyclerView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.ChatMessagesListRecycler)
        val layoutManager = recyclerView?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        
        layoutManager?.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset)
        
        // Reset saved position
        savedScrollPosition = -1
        savedScrollOffset = 0
    }
    
    /**
     * Exit multi-select mode and clear all selections
     */
    fun exitMultiSelectMode() {
        if (!isMultiSelectMode) return
        
        isMultiSelectMode = false
        selectedMessageIds.clear()
        
        // Cancel any pending debounced updates
        pendingUiUpdate?.let { uiUpdateHandler.removeCallbacks(it) }
        pendingUiUpdate = null
        
        // Provide haptic feedback when exiting multi-select mode
        provideHapticFeedback()
        
        // Hide action toolbar
        hideActionToolbar()
        
        // Update adapter to hide selection indicators
        adapter.isMultiSelectMode = false
        adapter.notifyDataSetChanged()
        
        // Restore scroll position after exiting multi-select mode
        restoreScrollPosition()
        
        // Process queued messages
        processQueuedMessages()
    }
    
    /**
     * Queue a message for display after exiting multi-select mode
     * Requirements: 8.3
     * 
     * @param message The message to queue
     */
    fun queueMessage(message: HashMap<String, Any?>) {
        if (isMultiSelectMode) {
            queuedMessages.add(message)
        }
    }
    
    /**
     * Process all queued messages after exiting multi-select mode
     * Requirements: 8.3
     */
    private fun processQueuedMessages() {
        if (queuedMessages.isNotEmpty()) {
            // Notify callback with queued messages
            onQueuedMessagesReady?.invoke(queuedMessages.toList())
            queuedMessages.clear()
        }
    }
    
    /**
     * Check if messages should be queued (i.e., in multi-select mode)
     * Requirements: 8.1, 8.3
     * 
     * @return true if messages should be queued, false otherwise
     */
    fun shouldQueueMessages(): Boolean {
        return isMultiSelectMode
    }
    
    /**
     * Toggle selection state of a message
     * Uses debouncing to reduce UI update frequency
     * Requirements: 7.2
     * @param messageId The ID of the message to toggle
     */
    fun toggleMessageSelection(messageId: String) {
        if (!isMultiSelectMode) return
        
        if (selectedMessageIds.contains(messageId)) {
            selectedMessageIds.remove(messageId)
            
            // Exit multi-select mode if no messages are selected
            if (selectedMessageIds.isEmpty()) {
                exitMultiSelectMode()
                return
            }
        } else {
            selectedMessageIds.add(messageId)
        }
        
        // Provide haptic feedback on selection change
        provideHapticFeedback()
        
        // Update action toolbar title immediately (lightweight operation)
        updateActionToolbarTitle()
        
        // Debounce adapter updates to reduce frequency
        debouncedAdapterUpdate()
    }
    
    /**
     * Debounce adapter updates to reduce UI update frequency
     * Cancels pending updates and schedules a new one
     * Requirements: 7.2
     */
    private fun debouncedAdapterUpdate() {
        // Cancel any pending update
        pendingUiUpdate?.let { uiUpdateHandler.removeCallbacks(it) }
        
        // Schedule new update
        pendingUiUpdate = Runnable {
            adapter.notifyDataSetChanged()
        }
        
        uiUpdateHandler.postDelayed(pendingUiUpdate!!, uiUpdateDebounceMs)
    }
    
    /**
     * Check if a message is currently selected
     * @param messageId The ID of the message to check
     * @return true if the message is selected, false otherwise
     */
    fun isMessageSelected(messageId: String): Boolean {
        return selectedMessageIds.contains(messageId)
    }
    
    /**
     * Get list of all selected message IDs
     * @return List of selected message IDs
     */
    fun getSelectedMessages(): List<String> {
        return selectedMessageIds.toList()
    }
    
    /**
     * Get the count of selected messages
     * @return Number of selected messages
     */
    fun getSelectionCount(): Int {
        return selectedMessageIds.size
    }
    
    /**
     * Show the action toolbar and hide the standard toolbar
     */
    private fun showActionToolbar() {
        // Find toolbars if not already cached
        if (standardToolbar == null) {
            standardToolbar = activity.findViewById(R.id.toolbar)
        }
        if (actionToolbar == null) {
            actionToolbar = activity.findViewById(R.id.action_toolbar)
            
            // Setup navigation icon click listener (close button)
            actionToolbar?.setNavigationOnClickListener {
                exitMultiSelectMode()
            }
            
            // Setup menu item click listeners
            actionToolbar?.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        onDeleteActionClicked()
                        true
                    }
                    else -> false
                }
            }
        }
        if (selectionCountText == null) {
            selectionCountText = activity.findViewById(R.id.selection_count_text)
        }
        
        // Hide standard toolbar and show action toolbar
        standardToolbar?.visibility = View.GONE
        actionToolbar?.visibility = View.VISIBLE
        
        // Update title with initial count
        updateActionToolbarTitle()
    }
    
    /**
     * Callback for when delete action is clicked
     * This will be overridden by the activity to handle deletion
     */
    var onDeleteActionClicked: () -> Unit = {}
    
    /**
     * Hide the action toolbar and show the standard toolbar
     */
    private fun hideActionToolbar() {
        // Show standard toolbar and hide action toolbar
        standardToolbar?.visibility = View.VISIBLE
        actionToolbar?.visibility = View.GONE
    }
    
    /**
     * Update the action toolbar title to show selection count
     */
    private fun updateActionToolbarTitle() {
        val count = selectedMessageIds.size
        selectionCountText?.text = count.toString()
    }
    
    /**
     * Provide haptic feedback to the user
     * Used for selection changes and mode transitions
     * 
     * Requirements: 5.6
     */
    private fun provideHapticFeedback() {
        try {
            activity.window.decorView.performHapticFeedback(
                android.view.HapticFeedbackConstants.CONTEXT_CLICK,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (e: Exception) {
            // Haptic feedback not available on this device
            android.util.Log.d("MultiSelectManager", "Haptic feedback not available: ${e.message}")
        }
    }
}
