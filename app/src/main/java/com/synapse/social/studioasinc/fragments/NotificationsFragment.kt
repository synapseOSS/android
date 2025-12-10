package com.synapse.social.studioasinc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.adapters.NotificationAdapter
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.Notification
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var noNotificationsText: TextView
    
    private lateinit var authService: SupabaseAuthenticationService
    private lateinit var databaseService: SupabaseDatabaseService
    private val notificationList = mutableListOf<Notification>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        initializeServices()
        setupRecyclerView()
        fetchNotifications()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.notifications_list)
        progressBar = view.findViewById(R.id.loading_bar)
        noNotificationsText = view.findViewById(R.id.no_notifications_text)
    }

    private fun initializeServices() {
        authService = SupabaseAuthenticationService()
        databaseService = SupabaseDatabaseService()
    }

    private fun setupRecyclerView() {
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        notificationAdapter = NotificationAdapter(requireContext(), notificationList)
        recyclerView.adapter = notificationAdapter
    }

    private fun fetchNotifications() {
        progressBar.visibility = View.VISIBLE
        
        val currentUserId = authService.getCurrentUserId()
        if (currentUserId == null) {
            showNoNotifications("Please sign in to view notifications")
            return
        }

        lifecycleScope.launch {
            try {
                // Fetch notifications from Supabase
                val result = databaseService.selectWhere(
                    "notifications", 
                    "*", 
                    "user_id", 
                    currentUserId
                )
                
                result.onSuccess { notifications ->
                    notificationList.clear()
                    
                    // Convert to Notification objects and sort by date
                    val convertedNotifications = notifications.mapNotNull { notificationData ->
                        try {
                            Notification(
                                notificationData["user_id"] as? String ?: "",
                                notificationData["message"] as? String ?: "",
                                notificationData["type"] as? String ?: "general",
                                notificationData["related_id"] as? String ?: "",
                                notificationData["id"] as? String ?: "",
                                (notificationData["created_at"] as? String)?.toLongOrNull() ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.timestamp }
                    
                    notificationList.addAll(convertedNotifications)
                    
                    progressBar.visibility = View.GONE
                    
                    if (notificationList.isEmpty()) {
                        showNoNotifications("No notifications yet")
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        noNotificationsText.visibility = View.GONE
                        notificationAdapter.notifyDataSetChanged()
                    }
                    
                }.onFailure { error ->
                    progressBar.visibility = View.GONE
                    showNoNotifications("Failed to load notifications: ${error.message}")
                }
                
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                showNoNotifications("Error loading notifications")
            }
        }
    }

    private fun showNoNotifications(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        noNotificationsText.visibility = View.VISIBLE
        noNotificationsText.text = message
    }

    /**
     * Refresh notifications when fragment becomes visible
     */
    override fun onResume() {
        super.onResume()
        fetchNotifications()
    }
}
