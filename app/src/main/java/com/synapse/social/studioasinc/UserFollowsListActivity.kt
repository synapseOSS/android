package com.synapse.social.studioasinc

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
// import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.*

class UserFollowsListActivity : BaseActivity() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()

    private val userInfoCacheMap = mutableMapOf<String, String>()
    private val followersList = mutableListOf<Map<String, Any?>>()
    private val followingList = mutableListOf<Map<String, Any?>>()

    // UI Components
    private lateinit var body: LinearLayout
    private lateinit var top: LinearLayout
    private lateinit var topSpace: LinearLayout
    private lateinit var coordinatorLayout: LinearLayout
    private lateinit var loadingLayout: LinearLayout
    private lateinit var back: ImageView
    private lateinit var topProfileLayout: LinearLayout
    private lateinit var topProfileLayoutSpace: LinearLayout
    private lateinit var more: ImageView
    private lateinit var topProfileCard: CardView
    private lateinit var topProfileLayoutRight: LinearLayout
    private lateinit var topProfileLayoutProfileImage: ImageView
    private lateinit var topProfileLayoutRightTop: LinearLayout
    private lateinit var topProfileLayoutUsername2: TextView
    private lateinit var topProfileLayoutUsername: TextView
    private lateinit var topProfileLayoutGenderBadge: ImageView
    private lateinit var topProfileLayoutVerifiedBadge: ImageView
    private lateinit var coordinatorLayoutAppbar: LinearLayout
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var coordinatorLayoutCollapsingToolbar: LinearLayout
    private lateinit var coordinatorLayoutCollapsingToolbarBody: LinearLayout
    private lateinit var tabFollowers: TextView
    private lateinit var tabFollowings: TextView
    private lateinit var swipeLayoutBody: LinearLayout
    private lateinit var followersLayout: LinearLayout
    private lateinit var followingLayout: LinearLayout
    private lateinit var followersLayoutList: RecyclerView
    private lateinit var followersLayoutNoFollowers: TextView
    private lateinit var followersLayoutLoading: LinearLayout
    private lateinit var followersLayoutLoadingBar: ProgressBar
    private lateinit var followingLayoutList: RecyclerView
    private lateinit var followingLayoutNoFollow: TextView
    private lateinit var followingLayoutLoading: LinearLayout
    private lateinit var followingLayoutLoadingBar: ProgressBar
    private lateinit var loadingLayoutBar: ProgressBar

    private var targetUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_follows_list)
        initialize(savedInstanceState)
        initializeLogic()
    }

    private fun initialize(savedInstanceState: Bundle?) {
        // Get target user ID from intent
        targetUserId = intent.getStringExtra("uid")

        // Initialize UI components
        body = findViewById(R.id.body)
        top = findViewById(R.id.top)
        topSpace = findViewById(R.id.topSpace)
        coordinatorLayout = findViewById(R.id.m_coordinator_layout)
        loadingLayout = findViewById(R.id.mLoadingLayout)
        back = findViewById(R.id.back)
        topProfileLayout = findViewById(R.id.topProfileLayout)
        topProfileLayoutSpace = findViewById(R.id.topProfileLayoutSpace)
        more = findViewById(R.id.more)
        topProfileCard = findViewById(R.id.topProfileCard)
        topProfileLayoutRight = findViewById(R.id.topProfileLayoutRight)
        topProfileLayoutProfileImage = findViewById(R.id.topProfileLayoutProfileImage)
        topProfileLayoutRightTop = findViewById(R.id.topProfileLayoutRightTop)
        topProfileLayoutUsername2 = findViewById(R.id.topProfileLayoutUsername2)
        topProfileLayoutUsername = findViewById(R.id.topProfileLayoutUsername)
        topProfileLayoutGenderBadge = findViewById(R.id.topProfileLayoutGenderBadge)
        topProfileLayoutVerifiedBadge = findViewById(R.id.topProfileLayoutVerifiedBadge)
        coordinatorLayoutAppbar = findViewById(R.id.m_coordinator_layout_appbar)
        swipeLayout = findViewById(R.id.swipe_layout)
        coordinatorLayoutCollapsingToolbar = findViewById(R.id.m_coordinator_layout_collapsing_toolbar)
        coordinatorLayoutCollapsingToolbarBody = findViewById(R.id.m_coordinator_layout_collapsing_toolbar_body)
        tabFollowers = findViewById(R.id.tab_followers)
        tabFollowings = findViewById(R.id.tab_followings)
        swipeLayoutBody = findViewById(R.id.swipe_layout_body)
        followersLayout = findViewById(R.id.followers_layout)
        followingLayout = findViewById(R.id.following_layout)
        followersLayoutList = findViewById(R.id.followers_layout_list)
        followersLayoutNoFollowers = findViewById(R.id.followers_layout_no_followers)
        followersLayoutLoading = findViewById(R.id.followers_layout_loading)
        followersLayoutLoadingBar = findViewById(R.id.followers_layout_loading_bar)
        followingLayoutList = findViewById(R.id.following_layout_list)
        followingLayoutNoFollow = findViewById(R.id.following_layout_no_follow)
        followingLayoutLoading = findViewById(R.id.following_layout_loading)
        followingLayoutLoadingBar = findViewById(R.id.following_layout_loading_bar)
        loadingLayoutBar = findViewById(R.id.mLoadingLayoutBar)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        back.setOnClickListener { onBackPressed() }

        swipeLayout.setOnRefreshListener {
            getFollowersReference()
            getFollowingReference()
            swipeLayout.isRefreshing = false
        }

        tabFollowers.setOnClickListener { setTab(0) }
        tabFollowings.setOnClickListener { setTab(1) }
    }

    private fun initializeLogic() {
        stateColor(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        viewGraphics(back, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 0.0, Color.TRANSPARENT)
        topProfileCard.background = createGradientDrawable(300, Color.TRANSPARENT)
        
        followersLayoutList.layoutManager = LinearLayoutManager(this)
        followingLayoutList.layoutManager = LinearLayoutManager(this)
        followersLayoutList.adapter = FollowersLayoutListAdapter(followersList)
        followingLayoutList.adapter = FollowingLayoutListAdapter(followingList)
        
        setTab(0)
        getUserReference()
    }

    private fun getUserReference() {
        if (targetUserId == null) {
            finish()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                coordinatorLayout.visibility = View.GONE
                topProfileLayout.visibility = View.GONE
                loadingLayout.visibility = View.VISIBLE

                val result = databaseService.selectWhere("users", "*", "uid", targetUserId!!)
                
                result.fold(
                    onSuccess = { users ->
                        val user = users.firstOrNull()
                        if (user != null) {
                            getFollowersReference()
                            getFollowingReference()
                            
                            // Set user profile data
                            val banned = user["banned"]?.toString() == "true"
                            val avatar = user["avatar"]?.toString()
                            
                            if (banned) {
                                topProfileLayoutProfileImage.setImageResource(R.drawable.banned_avatar)
                            } else if (avatar == "null" || avatar.isNullOrEmpty()) {
                                topProfileLayoutProfileImage.setImageResource(R.drawable.avatar)
                            } else {
                                Glide.with(applicationContext).load(Uri.parse(avatar)).into(topProfileLayoutProfileImage)
                            }
                            
                            val username = user["username"]?.toString() ?: ""
                            topProfileLayoutUsername2.text = "@$username"
                            
                            val nickname = user["nickname"]?.toString()
                            topProfileLayoutUsername.text = if (nickname == "null" || nickname.isNullOrEmpty()) {
                                "@$username"
                            } else {
                                nickname
                            }
                            
                            // Set gender badge
                            val gender = user["gender"]?.toString()
                            when (gender) {
                                "hidden" -> topProfileLayoutGenderBadge.visibility = View.GONE
                                "male" -> {
                                    topProfileLayoutGenderBadge.setImageResource(R.drawable.male_badge)
                                    topProfileLayoutGenderBadge.visibility = View.VISIBLE
                                }
                                "female" -> {
                                    topProfileLayoutGenderBadge.setImageResource(R.drawable.female_badge)
                                    topProfileLayoutGenderBadge.visibility = View.VISIBLE
                                }
                                else -> topProfileLayoutGenderBadge.visibility = View.GONE
                            }
                            
                            // Set account badge
                            val accountType = user["account_type"]?.toString()
                            when (accountType) {
                                "admin" -> {
                                    topProfileLayoutVerifiedBadge.setImageResource(R.drawable.admin_badge)
                                    topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
                                }
                                "moderator" -> {
                                    topProfileLayoutVerifiedBadge.setImageResource(R.drawable.moderator_badge)
                                    topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
                                }
                                "support" -> {
                                    topProfileLayoutVerifiedBadge.setImageResource(R.drawable.support_badge)
                                    topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
                                }
                                else -> {
                                    val isPremium = user["account_premium"]?.toString() == "true"
                                    val isVerified = user["verify"]?.toString() == "true"
                                    
                                    when {
                                        isPremium -> {
                                            topProfileLayoutVerifiedBadge.setImageResource(R.drawable.premium_badge)
                                            topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
                                        }
                                        isVerified -> {
                                            topProfileLayoutVerifiedBadge.setImageResource(R.drawable.verified_badge)
                                            topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
                                        }
                                        else -> topProfileLayoutVerifiedBadge.visibility = View.GONE
                                    }
                                }
                            }
                            
                            coordinatorLayout.visibility = View.VISIBLE
                            topProfileLayout.visibility = View.VISIBLE
                            loadingLayout.visibility = View.GONE
                        } else {
                            finish()
                        }
                    },
                    onFailure = { error ->
                        SketchwareUtil.showMessage(applicationContext, "Error loading user: ${error.message}")
                        finish()
                    }
                )
            } catch (e: Exception) {
                SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
                finish()
            }
        }
    }

    private fun getFollowersReference() {
        if (targetUserId == null) return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                followersLayoutList.visibility = View.GONE
                followersLayoutNoFollowers.visibility = View.GONE
                followersLayoutLoading.visibility = View.VISIBLE

                val result = databaseService.selectWhere("followers", "*", "following_id", targetUserId!!)
                
                result.fold(
                    onSuccess = { followers ->
                        if (followers.isNotEmpty()) {
                            followersLayoutList.visibility = View.VISIBLE
                            followersLayoutNoFollowers.visibility = View.GONE
                            followersLayoutLoading.visibility = View.GONE
                            
                            followersList.clear()
                            followers.forEach { follower ->
                                val uid = follower["follower_id"]?.toString()
                                if (uid != null) {
                                    followersList.add(mapOf("uid" to uid))
                                }
                            }
                            
                            followersLayoutList.adapter?.notifyDataSetChanged()
                        } else {
                            followersLayoutList.visibility = View.GONE
                            followersLayoutNoFollowers.visibility = View.VISIBLE
                            followersLayoutLoading.visibility = View.GONE
                        }
                    },
                    onFailure = { error ->
                        followersLayoutList.visibility = View.GONE
                        followersLayoutNoFollowers.visibility = View.VISIBLE
                        followersLayoutLoading.visibility = View.GONE
                        SketchwareUtil.showMessage(applicationContext, "Error loading followers: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                followersLayoutList.visibility = View.GONE
                followersLayoutNoFollowers.visibility = View.VISIBLE
                followersLayoutLoading.visibility = View.GONE
            }
        }
    }

    private fun getFollowingReference() {
        if (targetUserId == null) return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                followingLayoutList.visibility = View.GONE
                followingLayoutNoFollow.visibility = View.GONE
                followingLayoutLoading.visibility = View.VISIBLE

                val result = databaseService.selectWhere("followers", "*", "follower_id", targetUserId!!)
                
                result.fold(
                    onSuccess = { following ->
                        if (following.isNotEmpty()) {
                            followingLayoutList.visibility = View.VISIBLE
                            followingLayoutNoFollow.visibility = View.GONE
                            followingLayoutLoading.visibility = View.GONE
                            
                            followingList.clear()
                            following.forEach { follow ->
                                val uid = follow["following_id"]?.toString()
                                if (uid != null) {
                                    followingList.add(mapOf("uid" to uid))
                                }
                            }
                            
                            followingLayoutList.adapter?.notifyDataSetChanged()
                        } else {
                            followingLayoutList.visibility = View.GONE
                            followingLayoutNoFollow.visibility = View.VISIBLE
                            followingLayoutLoading.visibility = View.GONE
                        }
                    },
                    onFailure = { error ->
                        followingLayoutList.visibility = View.GONE
                        followingLayoutNoFollow.visibility = View.VISIBLE
                        followingLayoutLoading.visibility = View.GONE
                        SketchwareUtil.showMessage(applicationContext, "Error loading following: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                followingLayoutList.visibility = View.GONE
                followingLayoutNoFollow.visibility = View.VISIBLE
                followingLayoutLoading.visibility = View.GONE
            }
        }
    }

    private fun setTab(id: Int) {
        when (id) {
            0 -> {
                viewGraphics(tabFollowers, androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary), 0xFF3F51B5.toInt(), 300.0, 0.0, Color.TRANSPARENT)
                viewGraphics(tabFollowings, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 2.0, 0xFFEEEEEE.toInt())
                tabFollowers.setTextColor(0xFFFFFFFF.toInt())
                tabFollowings.setTextColor(0xFF616161.toInt())
                followersLayout.visibility = View.VISIBLE
                followingLayout.visibility = View.GONE
            }
            1 -> {
                viewGraphics(tabFollowers, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 300.0, 2.0, 0xFFEEEEEE.toInt())
                viewGraphics(tabFollowings, androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary), 0xFF3949AB.toInt(), 300.0, 0.0, Color.TRANSPARENT)
                tabFollowers.setTextColor(0xFF616161.toInt())
                tabFollowings.setTextColor(0xFFFFFFFF.toInt())
                followersLayout.visibility = View.GONE
                followingLayout.visibility = View.VISIBLE
            }
        }
    }

    // Utility functions
    private fun imageColor(image: ImageView, color: Int) {
        image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    private fun viewGraphics(view: View, onFocus: Int, onRipple: Int, radius: Double, stroke: Double, strokeColor: Int) {
        val gradientDrawable = GradientDrawable().apply {
            setColor(onFocus)
            cornerRadius = radius.toFloat()
            setStroke(stroke.toInt(), strokeColor)
        }
        view.background = gradientDrawable
    }

    private fun stateColor(statusColor: Int, navigationColor: Int) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
            }
        }
        window.statusBarColor = statusColor
        window.navigationBarColor = navigationColor
    }

    private fun createGradientDrawable(radius: Int, color: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = radius.toFloat()
            setColor(color)
        }
    }

    // RecyclerView Adapters
    inner class FollowersLayoutListAdapter(
        private val data: List<Map<String, Any?>>
    ) : RecyclerView.Adapter<FollowersLayoutListAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.user_followers_list, null)
            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            val uid = item["uid"]?.toString() ?: return

            // Set layout params
            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            holder.itemView.layoutParams = layoutParams

            // Style components
            viewGraphics(holder.body, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 0.0, 0.0, Color.TRANSPARENT)
            holder.profileCard.background = createGradientDrawable(300, Color.TRANSPARENT)
            holder.userStatusCircleBG.background = createGradientDrawable(300, 0xFFFFFFFF.toInt())
            holder.userStatusCircleIN.background = createGradientDrawable(300, 0xFF2196F3.toInt())

            holder.body.visibility = View.GONE

            // Check cache first
            if (userInfoCacheMap.containsKey("uid-$uid")) {
                displayUserInfo(holder, uid)
            } else {
                loadUserInfo(holder, uid, position)
            }

            holder.body.setOnClickListener {
                val intent = Intent(applicationContext, ProfileComposeActivity::class.java)
                intent.putExtra("uid", uid)
                startActivity(intent)
            }
        }

        private fun displayUserInfo(holder: ViewHolder, uid: String) {
            val banned = userInfoCacheMap["banned-$uid"] == "true"
            val avatar = userInfoCacheMap["avatar-$uid"]
            
            if (banned) {
                holder.profileAvatar.setImageResource(R.drawable.banned_avatar)
            } else if (avatar == "null" || avatar.isNullOrEmpty()) {
                holder.profileAvatar.setImageResource(R.drawable.avatar)
            } else {
                Glide.with(applicationContext).load(Uri.parse(avatar)).into(holder.profileAvatar)
            }
            
            val status = userInfoCacheMap["status-$uid"]
            holder.userStatusCircleBG.visibility = if (status == "online") View.VISIBLE else View.GONE
            
            val nickname = userInfoCacheMap["nickname-$uid"]
            val username = userInfoCacheMap["username-$uid"]
            
            holder.username.text = if (nickname == "null" || nickname.isNullOrEmpty()) {
                "@$username"
            } else {
                nickname
            }
            holder.name.text = "@$username"
            
            // Set gender badge
            val gender = userInfoCacheMap["gender-$uid"]
            when (gender) {
                "hidden" -> holder.genderBadge.visibility = View.GONE
                "male" -> {
                    holder.genderBadge.setImageResource(R.drawable.male_badge)
                    holder.genderBadge.visibility = View.VISIBLE
                }
                "female" -> {
                    holder.genderBadge.setImageResource(R.drawable.female_badge)
                    holder.genderBadge.visibility = View.VISIBLE
                }
                else -> holder.genderBadge.visibility = View.GONE
            }
            
            // Set account badge
            val accountType = userInfoCacheMap["acc_type-$uid"]
            when (accountType) {
                "admin" -> {
                    holder.badge.setImageResource(R.drawable.admin_badge)
                    holder.badge.visibility = View.VISIBLE
                }
                "moderator" -> {
                    holder.badge.setImageResource(R.drawable.moderator_badge)
                    holder.badge.visibility = View.VISIBLE
                }
                "support" -> {
                    holder.badge.setImageResource(R.drawable.support_badge)
                    holder.badge.visibility = View.VISIBLE
                }
                "user" -> {
                    val isVerified = userInfoCacheMap["verify-$uid"] == "true"
                    holder.badge.visibility = if (isVerified) View.VISIBLE else View.GONE
                }
                else -> holder.badge.visibility = View.GONE
            }
            
            holder.body.visibility = View.VISIBLE
        }

        private fun loadUserInfo(holder: ViewHolder, uid: String, position: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = databaseService.selectWhere("users", "*", "uid", uid)
                    
                    result.fold(
                        onSuccess = { users ->
                            val user = users.firstOrNull()
                            if (user != null) {
                                // Cache user info
                                userInfoCacheMap["uid-$uid"] = uid
                                userInfoCacheMap["banned-$uid"] = user["banned"]?.toString() ?: "false"
                                userInfoCacheMap["nickname-$uid"] = user["nickname"]?.toString() ?: "null"
                                userInfoCacheMap["username-$uid"] = user["username"]?.toString() ?: ""
                                userInfoCacheMap["status-$uid"] = user["status"]?.toString() ?: "offline"
                                userInfoCacheMap["avatar-$uid"] = user["avatar"]?.toString() ?: "null"
                                userInfoCacheMap["gender-$uid"] = user["gender"]?.toString() ?: "hidden"
                                userInfoCacheMap["verify-$uid"] = user["verify"]?.toString() ?: "false"
                                userInfoCacheMap["acc_type-$uid"] = user["account_type"]?.toString() ?: "user"
                                
                                withContext(Dispatchers.Main) {
                                    displayUserInfo(holder, uid)
                                }
                            }
                        },
                        onFailure = {
                            // Handle error silently
                        }
                    )
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }

        override fun getItemCount(): Int = data.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val body: LinearLayout = itemView.findViewById(R.id.body)
            val profileCardRelative: RelativeLayout = itemView.findViewById(R.id.profileCardRelative)
            val lin: LinearLayout = itemView.findViewById(R.id.lin)
            val profileCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.profileCard)
            val profileRelativeUp: LinearLayout = itemView.findViewById(R.id.ProfileRelativeUp)
            val profileAvatar: ImageView = itemView.findViewById(R.id.profileAvatar)
            val userStatusCircleBG: LinearLayout = itemView.findViewById(R.id.userStatusCircleBG)
            val userStatusCircleIN: LinearLayout = itemView.findViewById(R.id.userStatusCircleIN)
            val usr: LinearLayout = itemView.findViewById(R.id.usr)
            val name: TextView = itemView.findViewById(R.id.name)
            val username: TextView = itemView.findViewById(R.id.username)
            val genderBadge: ImageView = itemView.findViewById(R.id.genderBadge)
            val badge: ImageView = itemView.findViewById(R.id.badge)
        }
    }

    inner class FollowingLayoutListAdapter(
        private val data: List<Map<String, Any?>>
    ) : RecyclerView.Adapter<FollowingLayoutListAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.user_followers_list, null)
            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            val uid = item["uid"]?.toString() ?: return

            // Set layout params
            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            holder.itemView.layoutParams = layoutParams

            // Style components
            viewGraphics(holder.body, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 0.0, 0.0, Color.TRANSPARENT)
            holder.profileCard.background = createGradientDrawable(300, Color.TRANSPARENT)
            holder.userStatusCircleBG.background = createGradientDrawable(300, 0xFFFFFFFF.toInt())
            holder.userStatusCircleIN.background = createGradientDrawable(300, 0xFF2196F3.toInt())

            // Check cache first
            if (userInfoCacheMap.containsKey("uid-$uid")) {
                displayUserInfo(holder, uid)
            } else {
                loadUserInfo(holder, uid, position)
            }

            holder.body.setOnClickListener {
                val intent = Intent(applicationContext, ProfileComposeActivity::class.java)
                intent.putExtra("uid", uid)
                startActivity(intent)
            }
        }

        private fun displayUserInfo(holder: ViewHolder, uid: String) {
            val banned = userInfoCacheMap["banned-$uid"] == "true"
            val avatar = userInfoCacheMap["avatar-$uid"]
            
            if (banned) {
                holder.profileAvatar.setImageResource(R.drawable.banned_avatar)
            } else if (avatar == "null" || avatar.isNullOrEmpty()) {
                holder.profileAvatar.setImageResource(R.drawable.avatar)
            } else {
                Glide.with(applicationContext).load(Uri.parse(avatar)).into(holder.profileAvatar)
            }
            
            val status = userInfoCacheMap["status-$uid"]
            holder.userStatusCircleBG.visibility = if (status == "online") View.VISIBLE else View.GONE
            
            val nickname = userInfoCacheMap["nickname-$uid"]
            val username = userInfoCacheMap["username-$uid"]
            
            holder.username.text = if (nickname == "null" || nickname.isNullOrEmpty()) {
                "@$username"
            } else {
                nickname
            }
            holder.name.text = "@$username"
            
            // Set gender badge
            val gender = userInfoCacheMap["gender-$uid"]
            when (gender) {
                "hidden" -> holder.genderBadge.visibility = View.GONE
                "male" -> {
                    holder.genderBadge.setImageResource(R.drawable.male_badge)
                    holder.genderBadge.visibility = View.VISIBLE
                }
                "female" -> {
                    holder.genderBadge.setImageResource(R.drawable.female_badge)
                    holder.genderBadge.visibility = View.VISIBLE
                }
                else -> holder.genderBadge.visibility = View.GONE
            }
            
            // Set account badge
            val accountType = userInfoCacheMap["acc_type-$uid"]
            when (accountType) {
                "admin" -> {
                    holder.badge.setImageResource(R.drawable.admin_badge)
                    holder.badge.visibility = View.VISIBLE
                }
                "moderator" -> {
                    holder.badge.setImageResource(R.drawable.moderator_badge)
                    holder.badge.visibility = View.VISIBLE
                }
                "support" -> {
                    holder.badge.setImageResource(R.drawable.support_badge)
                    holder.badge.visibility = View.VISIBLE
                }
                "user" -> {
                    val isVerified = userInfoCacheMap["verify-$uid"] == "true"
                    holder.badge.visibility = if (isVerified) View.VISIBLE else View.GONE
                }
                else -> holder.badge.visibility = View.GONE
            }
            
            holder.body.visibility = View.VISIBLE
        }

        private fun loadUserInfo(holder: ViewHolder, uid: String, position: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = databaseService.selectWhere("users", "*", "uid", uid)
                    
                    result.fold(
                        onSuccess = { users ->
                            val user = users.firstOrNull()
                            if (user != null) {
                                // Cache user info
                                userInfoCacheMap["uid-$uid"] = uid
                                userInfoCacheMap["banned-$uid"] = user["banned"]?.toString() ?: "false"
                                userInfoCacheMap["nickname-$uid"] = user["nickname"]?.toString() ?: "null"
                                userInfoCacheMap["username-$uid"] = user["username"]?.toString() ?: ""
                                userInfoCacheMap["status-$uid"] = user["status"]?.toString() ?: "offline"
                                userInfoCacheMap["avatar-$uid"] = user["avatar"]?.toString() ?: "null"
                                userInfoCacheMap["gender-$uid"] = user["gender"]?.toString() ?: "hidden"
                                userInfoCacheMap["verify-$uid"] = user["verify"]?.toString() ?: "false"
                                userInfoCacheMap["acc_type-$uid"] = user["account_type"]?.toString() ?: "user"
                                
                                withContext(Dispatchers.Main) {
                                    displayUserInfo(holder, uid)
                                }
                            }
                        },
                        onFailure = {
                            // Handle error silently
                        }
                    )
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }

        override fun getItemCount(): Int = data.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val body: LinearLayout = itemView.findViewById(R.id.body)
            val profileCardRelative: RelativeLayout = itemView.findViewById(R.id.profileCardRelative)
            val lin: LinearLayout = itemView.findViewById(R.id.lin)
            val profileCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.profileCard)
            val profileRelativeUp: LinearLayout = itemView.findViewById(R.id.ProfileRelativeUp)
            val profileAvatar: ImageView = itemView.findViewById(R.id.profileAvatar)
            val userStatusCircleBG: LinearLayout = itemView.findViewById(R.id.userStatusCircleBG)
            val userStatusCircleIN: LinearLayout = itemView.findViewById(R.id.userStatusCircleIN)
            val usr: LinearLayout = itemView.findViewById(R.id.usr)
            val name: TextView = itemView.findViewById(R.id.name)
            val username: TextView = itemView.findViewById(R.id.username)
            val genderBadge: ImageView = itemView.findViewById(R.id.genderBadge)
            val badge: ImageView = itemView.findViewById(R.id.badge)
        }
    }
}

