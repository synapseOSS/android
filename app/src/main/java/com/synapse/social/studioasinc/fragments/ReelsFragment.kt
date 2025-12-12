package com.synapse.social.studioasinc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.synapse.social.studioasinc.LineVideosRecyclerViewAdapter
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.launch

class ReelsFragment : Fragment() {

    private lateinit var databaseService: SupabaseDatabaseService
    private lateinit var lineVideosRecyclerViewAdapter: LineVideosRecyclerViewAdapter
    private val lineVideosListMap = mutableListOf<HashMap<String, Any>>()
    
    private lateinit var middleRelativeTopSwipe: SwipeRefreshLayout
    private lateinit var loadedBody: LinearLayout
    private lateinit var videosRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize(view)
        initializeLogic()
    }

    private fun initialize(view: View) {
        databaseService = SupabaseDatabaseService()
        
        middleRelativeTopSwipe = view.findViewById(R.id.middleRelativeTopSwipe)
        loadedBody = view.findViewById(R.id.loadedBody)
        videosRecyclerView = view.findViewById(R.id.videosRecyclerView)

        middleRelativeTopSwipe.setOnRefreshListener { 
            loadReels() 
        }
    }

    private fun initializeLogic() {
        loadedBody.visibility = View.GONE
        videosRecyclerView.layoutManager = LinearLayoutManager(context)
        
        val lineVideoViewSnapHelper = PagerSnapHelper()
        lineVideoViewSnapHelper.attachToRecyclerView(videosRecyclerView)
        
        loadReels()
    }

    private fun loadReels() {
        lifecycleScope.launch {
            try {
                // Fetch video posts from Supabase
                val result = databaseService.selectWhere(
                    "posts",
                    "*",
                    "post_type",
                    "VIDEO"
                )
                
                result.onSuccess { videoPosts ->
                    lineVideosListMap.clear()
                    
                    // Convert to HashMap format for adapter compatibility
                    val convertedPosts = videoPosts.mapNotNull { postData ->
                        try {
                            hashMapOf<String, Any>(
                                "id" to (postData["id"] ?: ""),
                                "post_id" to (postData["post_id"] ?: postData["id"] ?: ""),
                                "author_id" to (postData["author_id"] ?: ""),
                                "uid" to (postData["author_id"] ?: ""), // For compatibility
                                "post_type" to "LINE_VIDEO",
                                "post_text" to (postData["content"] ?: ""),
                                "post_image" to (postData["video_url"] ?: ""),
                                "video_url" to (postData["video_url"] ?: ""),
                                "publish_date" to (postData["created_at"] ?: ""),
                                "timestamp" to System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.take(50) // Limit to 50 videos
                    
                    lineVideosListMap.addAll(convertedPosts)
                    
                    if (context != null) {
                        lineVideosRecyclerViewAdapter = LineVideosRecyclerViewAdapter(lineVideosListMap as ArrayList<HashMap<String, Any>>, requireContext())
                        videosRecyclerView.adapter = lineVideosRecyclerViewAdapter
                        loadedBody.visibility = View.VISIBLE
                    } else {
                        loadedBody.visibility = View.GONE
                    }
                    
                }.onFailure { error ->
                    // Handle error - show empty state or error message
                    loadedBody.visibility = View.GONE
                }
                
                middleRelativeTopSwipe.isRefreshing = false
                
            } catch (e: Exception) {
                loadedBody.visibility = View.GONE
                middleRelativeTopSwipe.isRefreshing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh reels when fragment becomes visible
        if (::databaseService.isInitialized) {
            loadReels()
        }
    }
}
