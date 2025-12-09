package com.synapse.social.studioasinc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
// import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.databinding.ActivityMainBinding
import com.synapse.social.studioasinc.databinding.DialogErrorBinding
import com.synapse.social.studioasinc.databinding.DialogUpdateBinding
import com.synapse.social.studioasinc.ui.auth.util.FeatureFlag

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private val userRepository by lazy { UserRepository(AppDatabase.getDatabase(this).userDao()) }
    
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, authRepository, userRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannels()
        setupListeners()
        setupObservers()
        viewModel.checkForUpdates()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val messagesChannel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat message notifications"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            val generalChannel = NotificationChannel(
                "general",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableLights(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    private fun setupListeners() {
        binding.appLogo.setOnLongClickListener {
            // Developer access check - implement with Supabase user metadata if needed
            // For now, just allow long press to exit
            finish()
            true
        }
    }

    private fun setupObservers() {
        // Check if Supabase is configured first
        if (!com.synapse.social.studioasinc.SupabaseClient.isConfigured()) {
            // Log the configuration issue for debugging
            android.util.Log.e("MainActivity", "Supabase not configured - URL: ${BuildConfig.SUPABASE_URL}, Key length: ${BuildConfig.SUPABASE_ANON_KEY.length}")
            
            showErrorDialog("Supabase Configuration Missing\n\nThe app is not properly configured. Please contact the developer to set up the backend services.\n\nConfiguration needed:\n• Supabase URL\n• Supabase API Key\n\nThis is a development/deployment issue that needs to be resolved by the app developer.")
            return
        }
        
        viewModel.updateState.observe(this) { state ->
            when (state) {
                is UpdateState.UpdateAvailable -> showUpdateDialog(state.title, state.versionName, state.changelog, state.updateLink, state.isCancelable)
                is UpdateState.NoUpdate -> viewModel.checkUserAuthentication()
                is UpdateState.Error -> showErrorDialog(state.message)
            }
        }

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    // Navigate to home screen
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is AuthState.Unauthenticated -> {
                    // Navigate to auth screen
                    val useNewAuth = FeatureFlag.USE_NEW_AUTH_UI
                    val intent = if (useNewAuth) {
                        Intent(this, AuthComposeActivity::class.java)
                    } else {
                        Intent(this, AuthActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
                is AuthState.Banned -> {
                    Toast.makeText(this, "You are banned & Signed Out.", Toast.LENGTH_LONG).show()
                    lifecycleScope.launch {
                        authRepository.signOut()
                    }
                    finish()
                }
                is AuthState.Error -> showErrorDialog(state.message)
            }
        }
    }

    private fun showUpdateDialog(title: String, versionName: String, changelog: String, updateLink: String, isCancelable: Boolean) {
        val dialogBinding = DialogUpdateBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(isCancelable)
            .create()

        dialogBinding.updateTitle.text = title
        dialogBinding.updateVersion.text = "Version $versionName"
        dialogBinding.updateChangelog.text = changelog.replace("\\\\n", "\\n")

        dialogBinding.buttonUpdate.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateLink))
            startActivity(intent)
            dialog.dismiss()
            finish()
        }

        dialogBinding.buttonLater.setOnClickListener {
            dialog.dismiss()
            if (isCancelable) {
                viewModel.checkUserAuthentication()
            }
        }

        if (!isCancelable) {
            dialogBinding.buttonLater.visibility = View.GONE
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showErrorDialog(errorMessage: String) {
        val dialogBinding = DialogErrorBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.errorMessageTextview.text = errorMessage

        dialogBinding.okButton.setOnClickListener {
            dialog.dismiss()
            // If Supabase is not configured, close the app instead of retrying
            if (!com.synapse.social.studioasinc.SupabaseClient.isConfigured()) {
                finishAffinity()
            } else {
                viewModel.checkUserAuthentication()
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}
