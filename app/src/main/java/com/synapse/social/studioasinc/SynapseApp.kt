package com.synapse.social.studioasinc

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.local.SyncWorker
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.data.repository.CommentRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SynapseApp : Application(), DefaultLifecycleObserver {
    
    private lateinit var exceptionHandler: Thread.UncaughtExceptionHandler
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val postRepository: PostRepository by lazy { PostRepository(database.postDao()) }
    val commentRepository: CommentRepository by lazy { CommentRepository(database.commentDao()) }
    val userRepository: UserRepository by lazy { UserRepository(database.userDao()) }
    val chatRepository: ChatRepository by lazy { ChatRepository(database.chatDao()) }
    
    companion object {
        private lateinit var context: Context
        
        @JvmStatic
        lateinit var mAuth: SupabaseAuthenticationService
            private set
        
        @JvmStatic
        val mCalendar: Calendar = Calendar.getInstance()
        
        @JvmStatic
        fun getContext(): Context = context
    }
    
    override fun onCreate() {
        super<Application>.onCreate()
        context = this
        exceptionHandler = Thread.getDefaultUncaughtExceptionHandler() 
            ?: Thread.UncaughtExceptionHandler { _, _ -> }
        
        createNotificationChannels()
        
        mAuth = SupabaseAuthenticationService()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val intent = Intent(context, DebugActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("error", Log.getStackTraceString(throwable))
            }
            context.startActivity(intent)
            exceptionHandler.uncaughtException(thread, throwable)
        }
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        initializeOneSignal()

        setupBackgroundSync()
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
    
    private fun initializeOneSignal() {
        val oneSignalAppId = "044e1911-6911-4871-95f9-d60003002fe2"
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, oneSignalAppId)
        
        applicationScope.launch {
            try {
                val granted = OneSignal.Notifications.requestPermission(true)
                Log.i("OneSignal", "Notification permission granted: $granted")
            } catch (e: Exception) {
                Log.e("OneSignal", "Error requesting notification permission", e)
            }
        }
        
        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                if (state.current.optedIn) {
                    val playerId = state.current.id
                    val userUid = mAuth.getCurrentUserId()
                    if (userUid != null && playerId != null) {
                        OneSignalManager.savePlayerIdToSupabase(userUid, playerId)
                    }
                }
            }
        })
    }
    
    override fun onStart(owner: LifecycleOwner) {
        mAuth.getCurrentUserId()?.let { userUid ->
            PresenceManager.goOnline(userUid)
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        mAuth.getCurrentUserId()?.let { userUid ->
            PresenceManager.goOffline(userUid)
        }
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
            
            notificationManager?.createNotificationChannel(messagesChannel)
            notificationManager?.createNotificationChannel(generalChannel)
        }
    }
}
