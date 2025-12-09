package com.synapse.social.studioasinc

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
// import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class ProfilePhotoHistoryActivity : BaseActivity() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()

    private var synapseLoadingDialog: ProgressDialog? = null
    private var currentAvatarUri = ""
    private val profileHistoryList = mutableListOf<Map<String, Any?>>()

    // UI Components
    private lateinit var fab: FloatingActionButton
    private lateinit var main: LinearLayout
    private lateinit var top: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var back: ImageView
    private lateinit var title: TextView
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var loadingBody: LinearLayout
    private lateinit var loadedBody: LinearLayout
    private lateinit var isDataExistsLayout: LinearLayout
    private lateinit var isDataNotExistsLayout: LinearLayout
    private lateinit var profilePhotosHistoryList: RecyclerView
    private lateinit var isDataNotExistsLayoutTitle: TextView
    private lateinit var isDataNotExistsLayoutSubTitle: TextView
    private lateinit var loadingBar: ProgressBar

    private val cc = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_photo_history)
        initialize(savedInstanceState)
        initializeLogic()
    }

    private fun initialize(savedInstanceState: Bundle?) {
        // Initialize UI components
        fab = findViewById(R.id._fab)
        main = findViewById(R.id.main)
        top = findViewById(R.id.top)
        body = findViewById(R.id.body)
        back = findViewById(R.id.back)
        title = findViewById(R.id.title)
        swipeLayout = findViewById(R.id.mSwipeLayout)
        loadingBody = findViewById(R.id.mLoadingBody)
        loadedBody = findViewById(R.id.mLoadedBody)
        isDataExistsLayout = findViewById(R.id.isDataExistsLayout)
        isDataNotExistsLayout = findViewById(R.id.isDataNotExistsLayout)
        profilePhotosHistoryList = findViewById(R.id.ProfilePhotosHistoryList)
        isDataNotExistsLayoutTitle = findViewById(R.id.isDataNotExistsLayoutTitle)
        isDataNotExistsLayoutSubTitle = findViewById(R.id.isDataNotExistsLayoutSubTitle)
        loadingBar = findViewById(R.id.mLoadingBar)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        back.setOnClickListener { onBackPressed() }

        swipeLayout.setOnRefreshListener {
            getReference()
        }

        fab.setOnClickListener {
            addProfilePhotoUrlDialog()
        }
    }

    private fun initializeLogic() {
        stateColor(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        viewGraphics(back, 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 300.0, 0.0, Color.TRANSPARENT)
        top.elevation = 4f
        
        val profileImagesHistoryListGrid = GridLayoutManager(this, 3)
        profilePhotosHistoryList.layoutManager = profileImagesHistoryListGrid
        profilePhotosHistoryList.adapter = ProfilePhotosHistoryListAdapter(profileHistoryList)
        
        getReference()
    }

    private fun getReference() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                isDataExistsLayout.visibility = View.GONE
                isDataNotExistsLayout.visibility = View.GONE
                swipeLayout.visibility = View.GONE
                loadingBody.visibility = View.VISIBLE

                val currentUserId = authService.getCurrentUserId() ?: return@launch

                // Get current user avatar
                val userResult = databaseService.selectWhere("users", "avatar", "uid", currentUserId)
                userResult.fold(
                    onSuccess = { users ->
                        val user = users.firstOrNull()
                        currentAvatarUri = user?.get("avatar")?.toString() ?: "null"
                    },
                    onFailure = { }
                )

                // Get profile history
                val historyResult = databaseService.selectWhere("profile_history", "*", "user_id", currentUserId)
                
                historyResult.fold(
                    onSuccess = { history ->
                        if (history.isNotEmpty()) {
                            isDataExistsLayout.visibility = View.VISIBLE
                            isDataNotExistsLayout.visibility = View.GONE
                            swipeLayout.visibility = View.VISIBLE
                            loadingBody.visibility = View.GONE
                            
                            profileHistoryList.clear()
                            profileHistoryList.addAll(history.sortedByDescending { 
                                it["upload_date"]?.toString()?.toLongOrNull() ?: 0L 
                            })
                            
                            profilePhotosHistoryList.adapter?.notifyDataSetChanged()
                        } else {
                            isDataExistsLayout.visibility = View.GONE
                            isDataNotExistsLayout.visibility = View.VISIBLE
                            swipeLayout.visibility = View.VISIBLE
                            loadingBody.visibility = View.GONE
                        }
                    },
                    onFailure = { error ->
                        isDataExistsLayout.visibility = View.GONE
                        isDataNotExistsLayout.visibility = View.VISIBLE
                        swipeLayout.visibility = View.VISIBLE
                        loadingBody.visibility = View.GONE
                        SketchwareUtil.showMessage(applicationContext, "Error loading history: ${error.message}")
                    }
                )
                
                swipeLayout.isRefreshing = false
            } catch (e: Exception) {
                isDataExistsLayout.visibility = View.GONE
                isDataNotExistsLayout.visibility = View.VISIBLE
                swipeLayout.visibility = View.VISIBLE
                loadingBody.visibility = View.GONE
                swipeLayout.isRefreshing = false
                SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
            }
        }
    }

    private fun addProfilePhotoUrlDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Add image with link")
        
        val view = LayoutInflater.from(this).inflate(R.layout.single_et, null)
        builder.setView(view)
        
        val editText = view.findViewById<EditText>(R.id.edittext1)
        val textInputLayout = view.findViewById<TextInputLayout>(R.id.textinputlayout1)
        
        editText.isFocusableInTouchMode = true
        
        builder.setPositiveButton("Add") { _, _ ->
            val url = editText.text.toString().trim()
            if (url.isNotEmpty() && checkValidUrl(url)) {
                addProfilePhotoFromUrl(url)
            }
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun addProfilePhotoFromUrl(imageUrl: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUserId = authService.getCurrentUserId() ?: return@launch
                val historyKey = UUID.randomUUID().toString()
                
                val historyData = mapOf(
                    "key" to historyKey,
                    "user_id" to currentUserId,
                    "image_url" to imageUrl.trim(),
                    "upload_date" to cc.timeInMillis.toString(),
                    "type" to "url"
                )
                
                val result = databaseService.insert("profile_history", historyData)
                
                result.fold(
                    onSuccess = {
                        SketchwareUtil.showMessage(applicationContext, "Profile image added")
                        getReference()
                    },
                    onFailure = { error ->
                        SketchwareUtil.showMessage(applicationContext, "Error adding image: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
            }
        }
    }

    private fun deleteProfileImage(key: String, type: String, uri: String) {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_synapse_bg_view, null)
        builder.setView(view)
        
        val dialogTitle = view.findViewById<TextView>(R.id.dialog_title)
        val dialogMessage = view.findViewById<TextView>(R.id.dialog_message)
        val dialogNoButton = view.findViewById<TextView>(R.id.dialog_no_button)
        val dialogYesButton = view.findViewById<TextView>(R.id.dialog_yes_button)
        
        dialogYesButton.setTextColor(0xFFF44336.toInt())
        viewGraphics(dialogYesButton, 0xFFFFFFFF.toInt(), 0xFFFFCDD2.toInt(), 28.0, 0.0, Color.TRANSPARENT)
        dialogNoButton.setTextColor(0xFF2196F3.toInt())
        viewGraphics(dialogNoButton, 0xFFFFFFFF.toInt(), 0xFFBBDEFB.toInt(), 28.0, 0.0, Color.TRANSPARENT)
        
        dialogTitle.text = resources.getString(R.string.info)
        dialogMessage.text = "Are you sure you want to delete this profile photo completely?"
        dialogNoButton.text = resources.getString(R.string.no)
        dialogYesButton.text = resources.getString(R.string.yes)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        dialogNoButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogYesButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val currentUserId = authService.getCurrentUserId() ?: return@launch
                    
                    // If this is the current avatar, reset it
                    if (uri == currentAvatarUri) {
                        val userData = mapOf(
                            "avatar" to "null"
                        )
                        databaseService.update("users", userData, "uid", currentUserId)
                        currentAvatarUri = "null"
                    }
                    
                    // Delete from history
                    val result = databaseService.deleteWhere("profile_history", "key", key)
                    
                    result.fold(
                        onSuccess = {
                            getReference()
                            dialog.dismiss()
                        },
                        onFailure = { error ->
                            SketchwareUtil.showMessage(applicationContext, "Error deleting image: ${error.message}")
                            dialog.dismiss()
                        }
                    )
                } catch (e: Exception) {
                    SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
                    dialog.dismiss()
                }
            }
        }
        
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun checkValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }

    override fun onBackPressed() {
        finish()
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
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = statusColor
        window.navigationBarColor = navigationColor
    }

    private fun loadingDialog(visibility: Boolean) {
        if (visibility) {
            if (synapseLoadingDialog == null) {
                synapseLoadingDialog = ProgressDialog(this).apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }
            synapseLoadingDialog?.show()
            synapseLoadingDialog?.setContentView(R.layout.loading_synapse)
        } else {
            synapseLoadingDialog?.dismiss()
        }
    }

    // RecyclerView Adapter
    inner class ProfilePhotosHistoryListAdapter(
        private val data: List<Map<String, Any?>>
    ) : RecyclerView.Adapter<ProfilePhotosHistoryListAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dp_history_cv, null)
            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            
            holder.checked.setBackgroundColor(0x50000000)
            imageColor(holder.checkedIc, 0xFFFFFFFF.toInt())
            
            val imageUrl = item["image_url"]?.toString() ?: ""
            Glide.with(applicationContext).load(Uri.parse(imageUrl)).into(holder.profile)
            
            // Show checked indicator if this is the current avatar
            if (imageUrl == currentAvatarUri) {
                holder.checked.visibility = View.VISIBLE
            } else {
                holder.checked.visibility = View.GONE
            }
            
            holder.body.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val currentUserId = authService.getCurrentUserId() ?: return@launch
                        
                        if (imageUrl == currentAvatarUri) {
                            // Remove current avatar
                            val userData: Map<String, Any?> = mapOf(
                                "avatar" to "null"
                            )
                            databaseService.update("users", userData, "uid", currentUserId)
                            currentAvatarUri = "null"
                        } else {
                            // Set as current avatar
                            val userData: Map<String, Any?> = mapOf(
                                "avatar" to imageUrl
                            )
                            databaseService.update("users", userData, "uid", currentUserId)
                            currentAvatarUri = imageUrl
                        }
                        
                        notifyDataSetChanged()
                    } catch (e: Exception) {
                        SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
                    }
                }
            }
            
            holder.body.setOnLongClickListener {
                deleteProfileImage(
                    item["key"]?.toString() ?: "",
                    item["type"]?.toString() ?: "",
                    imageUrl
                )
                true
            }
        }

        override fun getItemCount(): Int = data.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val body: LinearLayout = itemView.findViewById(R.id.body)
            val card: androidx.cardview.widget.CardView = itemView.findViewById(R.id.card)
            val relative: RelativeLayout = itemView.findViewById(R.id.relative)
            val profile: ImageView = itemView.findViewById(R.id.profile)
            val checked: LinearLayout = itemView.findViewById(R.id.checked)
            val checkedIc: ImageView = itemView.findViewById(R.id.checked_ic)
        }
    }
}
