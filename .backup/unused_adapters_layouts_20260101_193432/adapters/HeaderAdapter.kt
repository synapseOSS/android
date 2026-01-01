package com.synapse.social.studioasinc.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import kotlinx.coroutines.launch

class HeaderAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(AppDatabase.getDatabase(context).userDao())
) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    private var headerData: String = ""

    fun updateHeader(data: String) {
        headerData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(headerData)
    }

    override fun getItemCount(): Int = if (headerData.isNotEmpty()) 1 else 0

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(data: String) {
            headerText.text = data
            
            // Load current user info if needed
            lifecycleOwner.lifecycleScope.launch {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    userRepository.getUserById(userId)
                        .onSuccess { user ->
                            // Update UI with user info if needed
                        }
                        .onFailure {
                            // Handle error
                        }
                }
            }
        }
    }
}
