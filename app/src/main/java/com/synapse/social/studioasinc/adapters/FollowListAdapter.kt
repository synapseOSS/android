package com.synapse.social.studioasinc.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.components.FollowButton
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class FollowListAdapter(
    private val users: List<Map<String, Any?>>,
    private val onUserClick: (Map<String, Any?>) -> Unit,
    private val onMessageClick: ((Map<String, Any?>) -> Unit)? = null
) : RecyclerView.Adapter<FollowListAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
        private val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        private val displayNameText: TextView = itemView.findViewById(R.id.displayNameText)
        private val verifyIcon: ImageView = itemView.findViewById(R.id.verifyIcon)
        private val followButton: FollowButton = itemView.findViewById(R.id.followButton)
        private val messageButton: ImageButton = itemView.findViewById(R.id.messageButton)

        fun bind(user: Map<String, Any?>) {
            val username = user["username"]?.toString() ?: "Unknown"
            val displayName = user["display_name"]?.toString()
            val avatarUrl = user["avatar"]?.toString()
            val isVerified = user["verify"]?.toString()?.toBoolean() ?: false
            val userId = user["uid"]?.toString() ?: ""

            usernameText.text = "@$username"
            displayNameText.text = displayName ?: username
            displayNameText.visibility = if (displayName.isNullOrEmpty()) View.GONE else View.VISIBLE
            
            verifyIcon.visibility = if (isVerified) View.VISIBLE else View.GONE

            // Load avatar
            if (!avatarUrl.isNullOrEmpty() && avatarUrl != "null") {
                Glide.with(itemView.context)
                    .load(Uri.parse(avatarUrl))
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(avatarImage)
            } else {
                avatarImage.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // Setup follow button and message button
            if (itemView.context is androidx.lifecycle.LifecycleOwner) {
                val lifecycleOwner = itemView.context as androidx.lifecycle.LifecycleOwner
                
                // Get current user UID asynchronously
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        val authRepository = com.synapse.social.studioasinc.data.repository.AuthRepository()
                        val currentUserUid = authRepository.getCurrentUserUid()
                        
                        if (currentUserUid != null && currentUserUid != userId) {
                            followButton.visibility = View.VISIBLE
                            messageButton.visibility = View.VISIBLE
                            
                            // Setup follow button with correct UIDs
                            followButton.setup(currentUserUid, userId, lifecycleOwner.lifecycleScope)
                            
                            // Setup message button
                            messageButton.setOnClickListener {
                                onMessageClick?.invoke(user)
                            }
                        } else {
                            followButton.visibility = View.GONE
                            messageButton.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FollowListAdapter", "Failed to get current user UID", e)
                        followButton.visibility = View.GONE
                        messageButton.visibility = View.GONE
                    }
                }
            } else {
                followButton.visibility = View.GONE
                messageButton.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onUserClick(user)
            }
        }

        private fun getCurrentUserId(): String? {
            return try {
                // Get the user UID from the users table, not the auth UUID
                val authRepository = com.synapse.social.studioasinc.data.repository.AuthRepository()
                // We need to use a coroutine for this, but since this is called from bind(),
                // we'll use the auth UUID for now and fix the follow button setup
                com.synapse.social.studioasinc.SupabaseClient.client.auth.currentUserOrNull()?.id
            } catch (e: Exception) {
                null
            }
        }
    }
}
