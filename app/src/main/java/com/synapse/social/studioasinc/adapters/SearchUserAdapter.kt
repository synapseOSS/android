package com.synapse.social.studioasinc.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.User

class SearchUserAdapter(
    private val context: Context,
    private val userList: List<User>,
    private val onUserClickListener: OnUserClickListener
) : RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_mention, parent, false)
        return SearchUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchUserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class SearchUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: ImageView = itemView.findViewById(R.id.profile_image)
        private val usernameTextView: TextView = itemView.findViewById(R.id.username_text)
        private val nicknameTextView: TextView = itemView.findViewById(R.id.nickname_text)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClickListener.onUserClick(userList[position])
                }
            }
        }

        fun bind(user: User) {
            usernameTextView.text = user.username
            nicknameTextView.text = user.nickname

            if (user.avatar != null && user.avatar != "null") {
                Glide.with(context).load(Uri.parse(user.avatar)).into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.avatar)
            }
        }
    }

    interface OnUserClickListener {
        fun onUserClick(user: User)
    }
}
