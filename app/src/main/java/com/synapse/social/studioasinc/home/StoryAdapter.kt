package com.synapse.social.studioasinc.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.Story
import kotlinx.coroutines.launch

class StoryAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val authService: SupabaseAuthenticationService = SupabaseAuthenticationService(),
    private val databaseService: SupabaseDatabaseService = SupabaseDatabaseService()
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private var stories = mutableListOf<Story>()

    fun updateStories(newStories: List<Story>) {
        stories.clear()
        stories.addAll(newStories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.bind(story)
    }

    override fun getItemCount(): Int = stories.size

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val storyImage: ImageView = itemView.findViewById(R.id.storyImage)
        private val userNameText: TextView = itemView.findViewById(R.id.storyUsername)

        fun bind(story: Story) {
            // Load story image
            story.imageUrl?.let { imageUrl ->
                Glide.with(context)
                    .load(imageUrl)
                    .circleCrop()
                    .into(storyImage)
            }

            // Load user information
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val result = databaseService.getSingle("users", "uid", story.userId).getOrNull()?.let { Result.success(it) } ?: Result.failure(Exception("User not found"))
                    
                    result.onSuccess { userData ->
                        run {
                            val displayName = userData["display_name"] as? String
                            val username = userData["username"] as? String
                            val nickname = userData["nickname"] as? String
                            
                            userNameText.text = displayName ?: username ?: nickname ?: "Unknown"
                        }
                    }.onFailure {
                        userNameText.text = "Unknown"
                    }
                } catch (e: Exception) {
                    userNameText.text = "Unknown"
                }
            }
        }
    }
}
