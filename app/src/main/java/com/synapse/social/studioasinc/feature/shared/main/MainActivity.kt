package com.synapse.social.studioasinc.ui.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Image
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.navigation.AppNavigation
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.network.SupabaseClient
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
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
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.isCheckingAuth.value
        }
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(this@MainActivity)
            val appearanceSettings by settingsRepository.appearanceSettings.collectAsState(
                initial = com.synapse.social.studioasinc.ui.settings.AppearanceSettings()
            )
            
            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM -> 
                    androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            val dynamicColor = appearanceSettings.dynamicColorEnabled && 
                               android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            
            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                val navController = rememberNavController()
                val updateState by viewModel.updateState.observeAsState()
                val startDestination by viewModel.startDestination.collectAsState()
                val isCheckingAuth by viewModel.isCheckingAuth.collectAsState()
                
                if (!isCheckingAuth) {
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }

        createNotificationChannels()
        
        // Check for updates
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onOpenUpdateLink: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onSignOut: () -> Unit,
    onFinishApp: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.observeAsState()
    val authState by viewModel.authState.observeAsState()

    // Handle state changes
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.NoUpdate -> viewModel.checkUserAuthentication()
            is UpdateState.Error -> {
                // Error will be shown in dialog
            }
            else -> { /* UpdateAvailable will be shown in dialog */ }
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> onNavigateToHome()
            is AuthState.Unauthenticated -> onNavigateToAuth()
            is AuthState.Banned -> {
                onShowToast("You are banned & Signed Out.")
                onSignOut()
                onFinish()
            }
            else -> { /* Error will be shown in dialog */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacer
            Spacer(modifier = Modifier.weight(1f))
            
            // App logo
            Image(
                painter = painterResource(id = R.drawable.appicon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(85.dp)
                    .clickable(
                        onClick = onFinish,
                        onClickLabel = "Long press to exit"
                    ),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.weight(3f))
            
            // Company trademark
            Image(
                painter = painterResource(id = R.drawable.bycompany_mtrl),
                contentDescription = "Company Trademark",
                modifier = Modifier
                    .height(100.dp)
                    .padding(bottom = 20.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }

        // Show dialogs based on state
        updateState?.let { state ->
            when (state) {
                is UpdateState.UpdateAvailable -> {
                    UpdateDialog(
                        title = state.title,
                        versionName = state.versionName,
                        changelog = state.changelog,
                        updateLink = state.updateLink,
                        isCancelable = state.isCancelable,
                        onUpdate = { onOpenUpdateLink(state.updateLink) },
                        onLater = {
                            if (state.isCancelable) {
                                viewModel.checkUserAuthentication()
                            }
                        }
                    )
                }
                is UpdateState.Error -> {
                    ErrorDialog(
                        message = state.message,
                        onDismiss = {
                            if (!SupabaseClient.isConfigured()) {
                                onFinishApp()
                            } else {
                                viewModel.checkUserAuthentication()
                            }
                        }
                    )
                }
                else -> { /* No dialog needed */ }
            }
        }

        authState?.let { state ->
            if (state is AuthState.Error) {
                ErrorDialog(
                    message = state.message,
                    onDismiss = {
                        if (!SupabaseClient.isConfigured()) {
                            onFinishApp()
                        } else {
                            viewModel.checkUserAuthentication()
                        }
                    }
                )
            }
        }

        // Check Supabase configuration
        if (!SupabaseClient.isConfigured()) {
            ErrorDialog(
                message = "Supabase Configuration Missing\n\nThe app is not properly configured. Please contact the developer to set up the backend services.\n\nConfiguration needed:\n• Supabase URL\n• Supabase API Key\n\nThis is a development/deployment issue that needs to be resolved by the app developer.",
                onDismiss = onFinishApp
            )
        }
    }
}

@Composable
fun UpdateDialog(
    title: String,
    versionName: String,
    changelog: String,
    updateLink: String,
    isCancelable: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    Dialog(onDismissRequest = { if (isCancelable) onLater() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Version $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = changelog.replace("\\\\n", "\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCancelable) Arrangement.SpaceBetween else Arrangement.Center
                ) {
                    if (isCancelable) {
                        TextButton(onClick = onLater) {
                            Text("Later")
                        }
                    }
                    
                    Button(onClick = onUpdate) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    }
}
