package com.synapse.social.studioasinc.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseFollowService
import kotlinx.coroutines.launch

/**
 * Reusable Follow Button Component
 * Handles follow/unfollow functionality with loading states
 */
class FollowButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val followService = SupabaseFollowService()
    private lateinit var followButton: Button
    private lateinit var progressBar: ProgressBar
    
    private var currentUserId: String? = null
    private var targetUserId: String? = null
    private var isFollowing: Boolean = false
    private var isLoading: Boolean = false
    
    var onFollowStateChanged: ((Boolean) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.component_follow_button, this, true)
        
        followButton = findViewById(R.id.followButtonInternal)
        progressBar = findViewById(R.id.progressBarInternal)
        
        followButton.setOnClickListener {
            toggleFollow()
        }
        
        updateButtonState()
    }

    fun setup(currentUserId: String, targetUserId: String, lifecycleScope: LifecycleCoroutineScope) {
        this.currentUserId = currentUserId
        this.targetUserId = targetUserId
        this.lifecycleScope = lifecycleScope
        
        // Check initial follow status
        lifecycleScope.launch {
            checkFollowStatus()
        }
    }

    private suspend fun checkFollowStatus() {
        if (currentUserId == null || targetUserId == null) return
        
        setLoading(true)
        
        val result = followService.isFollowing(currentUserId!!, targetUserId!!)
        result.fold(
            onSuccess = { following ->
                isFollowing = following
                updateButtonState()
            },
            onFailure = { error ->
                android.util.Log.e("FollowButton", "Failed to check follow status", error)
            }
        )
        
        setLoading(false)
    }

    private var lifecycleScope: LifecycleCoroutineScope? = null
    
    private fun toggleFollow() {
        if (currentUserId == null || targetUserId == null || isLoading) return
        
        setLoading(true)
        
        lifecycleScope?.launch {
            val result = if (isFollowing) {
                followService.unfollowUser(currentUserId!!, targetUserId!!)
            } else {
                followService.followUser(currentUserId!!, targetUserId!!)
            }
            
            result.fold(
                onSuccess = {
                    isFollowing = !isFollowing
                    updateButtonState()
                    onFollowStateChanged?.invoke(isFollowing)
                },
                onFailure = { error ->
                    android.util.Log.e("FollowButton", "Failed to toggle follow", error)
                    // Show error message
                    android.widget.Toast.makeText(
                        context, 
                        "Failed to ${if (isFollowing) "unfollow" else "follow"} user", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
            
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        followButton.isEnabled = !loading
        progressBar.visibility = if (loading) VISIBLE else GONE
        followButton.visibility = if (loading) INVISIBLE else VISIBLE
    }

    private fun updateButtonState() {
        if (isFollowing) {
            followButton.text = "Following"
            followButton.setBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
            followButton.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        } else {
            followButton.text = "Follow"
            followButton.setBackgroundColor(ContextCompat.getColor(context, R.color.primary))
            followButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
    }

    fun setFollowState(following: Boolean) {
        isFollowing = following
        updateButtonState()
    }
}
