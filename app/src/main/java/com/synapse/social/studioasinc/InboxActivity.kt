package com.synapse.social.studioasinc

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.PopupMenu
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService

class InboxActivity : BaseActivity() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()

    // UI Components
    private lateinit var headerTitle: TextView
    private lateinit var btnOptions: ImageView
    private lateinit var viewpager1: ViewPager
    private lateinit var bottomnavigation1: BottomNavigationView
    private lateinit var fabNewChat: FloatingActionButton

    private lateinit var fg: FgFragmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)
        initialize(savedInstanceState)
        initializeLogic()
    }

    private fun initialize(savedInstanceState: Bundle?) {
        // Initialize UI components
        headerTitle = findViewById(R.id.headerTitle)
        btnOptions = findViewById(R.id.btn_options)
        viewpager1 = findViewById(R.id.viewpager1)
        bottomnavigation1 = findViewById(R.id.bottomnavigation1)
        fabNewChat = findViewById(R.id.fab_new_chat)
        
        fg = FgFragmentAdapter(applicationContext, supportFragmentManager)

        setupViewPager()
        setupBottomNavigation()
        setupClickListeners()
    }

    private fun setupViewPager() {
        viewpager1.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                // Check if menu has items before accessing
                if (bottomnavigation1.menu.size() > position) {
                    bottomnavigation1.menu.getItem(position).isChecked = true
                }
            }

            override fun onPageScrollStateChanged(scrollState: Int) {}
        })
        
        viewpager1.adapter = fg
    }

    private fun setupBottomNavigation() {
        bottomnavigation1.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_chats -> viewpager1.currentItem = 0
                R.id.navigation_calls -> viewpager1.currentItem = 1
                R.id.navigation_contacts -> viewpager1.currentItem = 2
            }
            true
        }
    }

    private fun setupClickListeners() {
        fabNewChat.setOnClickListener {
            startNewChat()
        }

        btnOptions.setOnClickListener {
            showMoreOptions(it)
        }
    }

    private fun initializeLogic() {
        stateColor(
            ContextCompat.getColor(this, R.color.white),
            ContextCompat.getColor(this, R.color.white)
        )
    }

    private fun showMoreOptions(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_inbox_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_mark_read -> {
                    SketchwareUtil.showMessage(applicationContext, "Coming soon")
                    true
                }
                R.id.action_chat_settings -> {
                    val intent = Intent(applicationContext, ChatPrivacySettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_settings -> {
                    val intent = Intent(applicationContext, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun startNewChat() {
        // Navigate to search activity to find users to chat with
        val intent = Intent(applicationContext, SearchActivity::class.java)
        intent.putExtra("mode", "chat")
        startActivity(intent)
    }

    override fun onBackPressed() {
        val intent = Intent(applicationContext, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        finish()
    }

    // Utility functions
    private fun stateColor(statusColor: Int, navigationColor: Int) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = statusColor
        window.navigationBarColor = navigationColor
    }

    /**
     * Fragment Adapter for ViewPager
     */
    inner class FgFragmentAdapter(
        private val context: android.content.Context,
        fm: FragmentManager
    ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> com.synapse.social.studioasinc.fragments.ChatListFragment() // Chat list fragment
                1 -> InboxCallsFragment() // Calls fragment (placeholder)
                2 -> InboxContactsFragment() // Contacts fragment (placeholder)
                else -> com.synapse.social.studioasinc.fragments.ChatListFragment()
            }
        }

        override fun getCount(): Int = 3

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> "Chats"
                1 -> "Calls"
                2 -> "Contacts"
                else -> null
            }
        }
    }
}

/**
 * Placeholder fragments for the inbox tabs
 * These would be implemented separately with their own functionality
 */
class InboxChatsFragmentSimple : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = TextView(context)
        view.text = "Chat list coming soon\nTap on a user in Search to start chatting"
        view.gravity = android.view.Gravity.CENTER
        view.setTextColor(Color.GRAY)
        view.setPadding(32, 32, 32, 32)
        return view
    }
}

class InboxCallsFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = TextView(context)
        view.text = "Calls feature coming soon"
        view.gravity = android.view.Gravity.CENTER
        view.setTextColor(Color.GRAY)
        return view
    }
}

class InboxContactsFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = TextView(context)
        view.text = "Contacts feature coming soon"
        view.gravity = android.view.Gravity.CENTER
        view.setTextColor(Color.GRAY)
        return view
    }
}
