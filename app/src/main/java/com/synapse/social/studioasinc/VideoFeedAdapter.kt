package com.synapse.social.studioasinc

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.util.ImageLoader
import java.util.ArrayList
import java.util.HashMap

class VideoFeedAdapter(
    private val data: ArrayList<HashMap<String, Any>>,
    private val context: Context
) : RecyclerView.Adapter<VideoFeedAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.synapse_reels_cv, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val postData = data[position]

        // Bind data
        val videoUrl = postData["video_url"] as? String ?: ""
        val postText = postData["post_text"] as? String ?: ""
        val authorId = postData["author_id"] as? String ?: ""

        // Post Description
        holder.postDescription.text = postText

        // Username (Placeholder for now as we don't have it in the map)
        holder.username.text = "@User" // TODO: Fetch username

        // Video
        if (videoUrl.isNotEmpty()) {
            val uri = Uri.parse(videoUrl)
            holder.videoView.setVideoURI(uri)

            holder.videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
                val screenRatio = holder.videoView.width / holder.videoView.height.toFloat()

                val scaleX = videoRatio / screenRatio
                if (scaleX >= 1f) {
                    holder.videoView.scaleX = scaleX
                } else {
                    holder.videoView.scaleY = 1f / scaleX
                }

                holder.playPauseButton.visibility = View.GONE
                holder.videoView.start()
            }

            holder.videoView.setOnCompletionListener {
                holder.videoView.start()
            }
        }

        // Play/Pause toggle
        holder.itemView.setOnClickListener {
            if (holder.videoView.isPlaying) {
                holder.videoView.pause()
                holder.playPauseButton.visibility = View.VISIBLE
            } else {
                holder.videoView.start()
                holder.playPauseButton.visibility = View.GONE
            }
        }

        // Setup other views (stubs for now)
        holder.likeCount.text = "0"
        holder.commentCount.text = "0"
        holder.shareCount.text = "0"

        // Basic image loading for profile if available, otherwise placeholder
        // Since we don't have profile URL in the map, we rely on defaults or fetch logic
        // For now, let's leave the default avatar
    }

    override fun getItemCount(): Int {
        return data.size
    }

    // Stop video when view is recycled
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.videoView.stopPlayback()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoView: VideoView = itemView.findViewById(R.id.videoView)
        val playPauseButton: ImageView = itemView.findViewById(R.id.playPauseButton)
        val postDescription: TextView = itemView.findViewById(R.id.postDescription)
        val username: TextView = itemView.findViewById(R.id.buttonsRelativeBottomUserInfUsername)
        val profileImage: ImageView = itemView.findViewById(R.id.profileCardImage)

        val likeCount: TextView = itemView.findViewById(R.id.likeButtonCount)
        val commentCount: TextView = itemView.findViewById(R.id.commentsButtonCount)
        val shareCount: TextView = itemView.findViewById(R.id.shareButtonCount)
        val favoriteCount: TextView = itemView.findViewById(R.id.addFavoriteButtonCount)

        val likeButton: View = itemView.findViewById(R.id.likeButton)
        val commentButton: View = itemView.findViewById(R.id.commentsButton)
        val shareButton: View = itemView.findViewById(R.id.shareButton)
    }
}
