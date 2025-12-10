package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Window
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
// import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.launch
import java.util.*

class EditPostActivity : BaseActivity() {

    companion object {
        private const val REQ_CD_IMAGE_PICKER = 101
    }

    private lateinit var authService: SupabaseAuthenticationService
    private lateinit var databaseService: SupabaseDatabaseService
    private var synapseLoadingDialog: ProgressDialog? = null
    
    private var postKey = ""
    private var selectedImagePath = ""
    private var hasImage = false
    private var imageChanged = false
    
    // UI Components
    private lateinit var main: LinearLayout
    private lateinit var top: LinearLayout
    private lateinit var topSpace: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var back: ImageView
    private lateinit var topSpc: LinearLayout
    private lateinit var updateButton: Button
    private lateinit var title: TextView
    private lateinit var scrollBody: LinearLayout
    private lateinit var postInfoTop1: LinearLayout
    private lateinit var topSpace2: LinearLayout
    private lateinit var spc2: LinearLayout
    private lateinit var imageCard: CardView
    private lateinit var postDescription: FadeEditText
    private lateinit var postImageView: ImageView
    private lateinit var imagePlaceholder: LinearLayout
    private lateinit var settingsButton: LinearLayout
    
    // Post data from intent
    private var originalPostText = ""
    private var originalPostImage = ""
    private var originalPostType = ""
    
    // Post settings variables
    private var hideViewsCount = false
    private var hideLikesCount = false
    private var hideCommentsCount = false
    private var hidePostFromEveryone = false
    private var disableSaveToFavorites = false
    private var disableComments = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_post)
        initialize()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
        } else {
            initializeLogic()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            initializeLogic()
        }
    }

    private fun initialize() {
        authService = SupabaseAuthenticationService()
        databaseService = SupabaseDatabaseService()
        
        main = findViewById(R.id.main)
        top = findViewById(R.id.top)
        topSpace = findViewById(R.id.topSpace)
        scroll = findViewById(R.id.scroll)
        back = findViewById(R.id.back)
        topSpc = findViewById(R.id.topSpc)
        updateButton = findViewById(R.id.updateButton)
        title = findViewById(R.id.title)
        scrollBody = findViewById(R.id.scrollBody)
        postInfoTop1 = findViewById(R.id.PostInfoTop1)
        topSpace2 = findViewById(R.id.topSpace2)
        spc2 = findViewById(R.id.spc2)
        imageCard = findViewById(R.id.imageCard)
        postDescription = findViewById(R.id.postDescription)
        postImageView = findViewById(R.id.postImageView)
        imagePlaceholder = findViewById(R.id.imagePlaceholder)
        settingsButton = findViewById(R.id.settingsButton)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        back.setOnClickListener { onBackPressed() }
        updateButton.setOnClickListener { updatePost() }
        imagePlaceholder.setOnClickListener { openImagePicker() }
        settingsButton.setOnClickListener { showPostSettingsBottomSheet() }
        
        postDescription.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                transitionManager(postInfoTop1, 130)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initializeLogic() {
        setStatusBarColor(true, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        viewGraphics(back, 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 300, 0, Color.TRANSPARENT)
        viewGraphics(settingsButton, 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 300, 0, Color.TRANSPARENT)
        
        imageCard.background = GradientDrawable().apply {
            cornerRadius = 22f
            setStroke(2, 0xFFEEEEEE.toInt())
            setColor(0xFFFFFFFF.toInt())
        }
        
        postDescription.background = GradientDrawable().apply {
            cornerRadius = 28f
            setStroke(3, 0xFFEEEEEE.toInt())
            setColor(0xFFFFFFFF.toInt())
        }
        
        viewGraphics(imagePlaceholder, 0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 0, 0, Color.TRANSPARENT)
        
        loadPostDataFromIntent()
    }

    private fun loadPostDataFromIntent() {
        intent?.let { intent ->
            if (intent.hasExtra("postKey")) {
                postKey = intent.getStringExtra("postKey") ?: ""
            }
            if (intent.hasExtra("postText")) {
                originalPostText = intent.getStringExtra("postText") ?: ""
                postDescription.setText(originalPostText)
            }
            if (intent.hasExtra("postImage")) {
                originalPostImage = intent.getStringExtra("postImage") ?: ""
                if (originalPostImage.isNotEmpty()) {
                    hasImage = true
                    loadOriginalImage()
                }
            }
            if (intent.hasExtra("postType")) {
                originalPostType = intent.getStringExtra("postType") ?: ""
            }
            
            // Load post settings from intent
            hideViewsCount = intent.getBooleanExtra("hideViewsCount", false)
            hideLikesCount = intent.getBooleanExtra("hideLikesCount", false)
            hideCommentsCount = intent.getBooleanExtra("hideCommentsCount", false)
            hidePostFromEveryone = intent.getBooleanExtra("hidePostFromEveryone", false)
            disableSaveToFavorites = intent.getBooleanExtra("disableSaveToFavorites", false)
            disableComments = intent.getBooleanExtra("disableComments", false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQ_CD_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            data?.let { intentData ->
                val filePaths = mutableListOf<String>()
                
                if (intentData.clipData != null) {
                    for (i in 0 until intentData.clipData!!.itemCount) {
                        val item: ClipData.Item = intentData.clipData!!.getItemAt(i)
                        FileUtil.convertUriToFilePath(applicationContext, item.uri)?.let { filePaths.add(it) }
                    }
                } else {
                    intentData.data?.let { uri ->
                        FileUtil.convertUriToFilePath(applicationContext, uri)?.let { filePaths.add(it) }
                    }
                }
                
                if (filePaths.isNotEmpty()) {
                    selectedImagePath = filePaths[0]
                    hasImage = true
                    imageChanged = true
                    loadSelectedImage()
                }
            }
        }
    }

    private fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
            } else {
                val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, REQ_CD_IMAGE_PICKER)
            }
        } else {
            val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQ_CD_IMAGE_PICKER)
        }
    }

    private fun loadOriginalImage() {
        if (hasImage && originalPostImage.isNotEmpty()) {
            imagePlaceholder.visibility = View.GONE
            postImageView.visibility = View.VISIBLE
            Glide.with(applicationContext).load(originalPostImage).into(postImageView)
        }
    }

    private fun loadSelectedImage() {
        if (hasImage && selectedImagePath.isNotEmpty()) {
            imagePlaceholder.visibility = View.GONE
            postImageView.visibility = View.VISIBLE
            Glide.with(applicationContext).load(selectedImagePath).into(postImageView)
        }
    }

    private fun updatePost() {
        if (postDescription.text.toString().trim().isEmpty() && !hasImage) {
            Toast.makeText(applicationContext, "Please add some text or an image to your post", Toast.LENGTH_SHORT).show()
            return
        }
        
        loadingDialog(true)
        
        if (imageChanged && hasImage) {
            // Upload new image first, then update post
            ImageUploader.uploadImage(applicationContext, selectedImagePath, object : ImageUploader.UploadCallback {
                override fun onUploadComplete(imageUrl: String) {
                    updatePostInDatabase(imageUrl)
                }
                
                override fun onUploadError(errorMessage: String) {
                    loadingDialog(false)
                    Toast.makeText(applicationContext, "Failed to upload image: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // Update post without changing image
            updatePostInDatabase(originalPostImage)
        }
    }

    private fun updatePostInDatabase(imageUrl: String) {
        val currentUserId = authService.getCurrentUserId()
        if (currentUserId == null) {
            Toast.makeText(applicationContext, "User not logged in", Toast.LENGTH_SHORT).show()
            loadingDialog(false)
            return
        }
        
        lifecycleScope.launch {
            try {
                val updateMap = mutableMapOf<String, Any?>()
                
                if (hasImage) {
                    updateMap["post_type"] = "IMAGE"
                    updateMap["image_url"] = imageUrl
                    updateMap["video_url"] = null
                } else {
                    updateMap["post_type"] = "TEXT"
                    updateMap["image_url"] = null
                    updateMap["video_url"] = null
                }
                
                updateMap["content"] = postDescription.text.toString().trim()
                
                // Apply post settings
                updateMap["post_hide_views_count"] = hideViewsCount
                updateMap["post_hide_like_count"] = hideLikesCount
                updateMap["post_hide_comments_count"] = hideCommentsCount
                updateMap["post_visibility"] = if (hidePostFromEveryone) "private" else "public"
                updateMap["post_disable_favorite"] = disableSaveToFavorites
                updateMap["post_disable_comments"] = disableComments
                updateMap["updated_at"] = System.currentTimeMillis().toString()
                
                val result = databaseService.update("posts", updateMap, "id", postKey)
                
                result.onSuccess {
                    Toast.makeText(applicationContext, "Post updated successfully", Toast.LENGTH_SHORT).show()
                    loadingDialog(false)
                    finish()
                }.onFailure { error ->
                    Toast.makeText(applicationContext, "Failed to update post: ${error.message}", Toast.LENGTH_SHORT).show()
                    loadingDialog(false)
                }
                
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error updating post: ${e.message}", Toast.LENGTH_SHORT).show()
                loadingDialog(false)
            }
        }
    }

    private fun showPostSettingsBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.create_post_settings_bottom_sheet, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Initialize switches
        val hideViewsSwitch = bottomSheetView.findViewById<SwitchCompat>(R.id.hideViewsSwitch)
        val hideLikesSwitch = bottomSheetView.findViewById<SwitchCompat>(R.id.hideLikesSwitch)
        val hideCommentsSwitch = bottomSheetView.findViewById<SwitchCompat>(R.id.hideCommentsSwitch)
        val hidePostSwitch = bottomSheetView.findViewById<SwitchCompat>(R.id.hidePostSwitch)
        val disableSaveSwitch = bottomSheetView.findViewById<SwitchCompat>(R.id.disableSaveSwitch)
        val disableCommentsSwitch = bottomSheetView.findViewById<SwitchCompat>(R.id.disableCommentsSwitch)
        
        // Set current values
        hideViewsSwitch.isChecked = hideViewsCount
        hideLikesSwitch.isChecked = hideLikesCount
        hideCommentsSwitch.isChecked = hideCommentsCount
        hidePostSwitch.isChecked = hidePostFromEveryone
        disableSaveSwitch.isChecked = disableSaveToFavorites
        disableCommentsSwitch.isChecked = disableComments
        
        // Set listeners
        hideViewsSwitch.setOnCheckedChangeListener { _, isChecked -> hideViewsCount = isChecked }
        hideLikesSwitch.setOnCheckedChangeListener { _, isChecked -> hideLikesCount = isChecked }
        hideCommentsSwitch.setOnCheckedChangeListener { _, isChecked -> hideCommentsCount = isChecked }
        
        hidePostSwitch.setOnCheckedChangeListener { _, isChecked ->
            hidePostFromEveryone = isChecked
            if (isChecked) {
                disableCommentsSwitch.isChecked = true
                disableCommentsSwitch.isEnabled = false
            } else {
                disableCommentsSwitch.isChecked = false
                disableCommentsSwitch.isEnabled = true
            }
        }
        
        disableSaveSwitch.setOnCheckedChangeListener { _, isChecked -> disableSaveToFavorites = isChecked }
        disableCommentsSwitch.setOnCheckedChangeListener { _, isChecked -> disableComments = isChecked }
        
        bottomSheetDialog.show()
    }

    private fun setStatusBarColor(isLight: Boolean, stateColor: Int, navigationColor: Int) {
        if (isLight) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = stateColor
        window.navigationBarColor = navigationColor
    }

    private fun viewGraphics(view: View, onFocus: Int, onRipple: Int, radius: Int, stroke: Int, strokeColor: Int) {
        val gradientDrawable = GradientDrawable().apply {
            setColor(onFocus)
            cornerRadius = radius.toFloat()
            setStroke(stroke, strokeColor)
        }
        val rippleDrawable = RippleDrawable(
            android.content.res.ColorStateList(arrayOf(intArrayOf()), intArrayOf(onRipple)),
            gradientDrawable,
            null
        )
        view.background = rippleDrawable
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

    private fun transitionManager(view: View, duration: Int) {
        val viewGroup = view as LinearLayout
        val autoTransition = android.transition.AutoTransition().apply {
            setDuration(duration.toLong())
        }
        android.transition.TransitionManager.beginDelayedTransition(viewGroup, autoTransition)
    }

    override fun onBackPressed() {
        finish()
    }
}
