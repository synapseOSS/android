package com.synapse.social.studioasinc

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
// import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.adapters.FollowListAdapter
import com.synapse.social.studioasinc.backend.SupabaseFollowService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class FollowListActivity : BaseActivity() {

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_LIST_TYPE = "list_type"
        const val TYPE_FOLLOWERS = "followers"
        const val TYPE_FOLLOWING = "following"
    }

    private val followService = SupabaseFollowService()
    
    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    
    private var userId: String? = null
    private var listType: String? = null
    private val usersList = ArrayList<Map<String, Any?>>()
    private var adapter: FollowListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_list)
        
        userId = intent.getStringExtra(EXTRA_USER_ID)
        listType = intent.getStringExtra(EXTRA_LIST_TYPE)
        
        if (userId == null || listType == null) {
            finish()
            return
        }
        
        initialize()
        setupRecyclerView()
        loadData()
    }

    private fun initialize() {
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        
        // Set title based on list type
        titleText.text = when (listType) {
            TYPE_FOLLOWERS -> "Followers"
            TYPE_FOLLOWING -> "Following"
            else -> "Users"
        }
        
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowListAdapter(
            users = usersList,
            onUserClick = { user ->
                // Handle user click - navigate to profile
                val intent = android.content.Intent(this, ProfileComposeActivity::class.java)
                intent.putExtra("uid", user["uid"]?.toString())
                startActivity(intent)
            },
            onMessageClick = { user ->
                // Handle message click - start direct chat
                startDirectChat(user)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadData() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = when (listType) {
                    TYPE_FOLLOWERS -> followService.getFollowers(userId!!)
                    TYPE_FOLLOWING -> followService.getFollowing(userId!!)
                    else -> Result.success(emptyList())
                }
                
                result.fold(
                    onSuccess = { users ->
                        usersList.clear()
                        usersList.addAll(users)
                        adapter?.notifyDataSetChanged()
                        
                        showLoading(false)
                        
                        if (users.isEmpty()) {
                            showEmpty(when (listType) {
                                TYPE_FOLLOWERS -> "No followers yet"
                                TYPE_FOLLOWING -> "Not following anyone yet"
                                else -> "No users found"
                            })
                        }
                    },
                    onFailure = { error ->
                        showLoading(false)
                        showEmpty("Failed to load users: ${error.message}")
                        android.util.Log.e("FollowListActivity", "Failed to load users", error)
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                showEmpty("Error: ${e.message}")
                android.util.Log.e("FollowListActivity", "Error loading data", e)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        emptyView.visibility = View.GONE
    }

    private fun showEmpty(message: String) {
        emptyView.text = message
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun startDirectChat(user: Map<String, Any?>) {
        val targetUserId = user["uid"]?.toString() ?: return

        lifecycleScope.launch {
            try {
                // Get current user UID (not auth UUID)
                val authRepository = com.synapse.social.studioasinc.data.repository.AuthRepository()
                val currentUserId = authRepository.getCurrentUserUid()

                if (currentUserId == null) {
                    android.widget.Toast.makeText(this@FollowListActivity, "Failed to get user info", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (targetUserId == currentUserId) {
                    android.widget.Toast.makeText(this@FollowListActivity, "You cannot message yourself", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Show loading
                val progressDialog = android.app.ProgressDialog(this@FollowListActivity).apply {
                    setMessage("Starting chat...")
                    setCancelable(false)
                    show()
                }

                val chatService = com.synapse.social.studioasinc.backend.SupabaseChatService()
                val result = chatService.getOrCreateDirectChat(currentUserId, targetUserId)
                
                result.fold(
                    onSuccess = { chatId ->
                        progressDialog.dismiss()
                        
                        // Navigate to ChatActivity
                        val intent = android.content.Intent(this@FollowListActivity, ChatActivity::class.java)
                        intent.putExtra("chatId", chatId)
                        intent.putExtra("uid", targetUserId)
                        intent.putExtra("isGroup", false)
                        startActivity(intent)
                    },
                    onFailure = { error ->
                        progressDialog.dismiss()
                        android.util.Log.e("FollowListActivity", "Failed to create chat", error)
                        android.widget.Toast.makeText(
                            this@FollowListActivity, 
                            "Failed to start chat: ${error.message}", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("FollowListActivity", "Error starting chat", e)
                android.widget.Toast.makeText(
                    this@FollowListActivity, 
                    "Error starting chat: ${e.message}", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
