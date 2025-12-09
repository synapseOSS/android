package com.synapse.social.studioasinc

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
// import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Debug Activity
 * Displays error information and provides debugging utilities.
 * Follows MVVM pattern and Android best practices.
 */
class DebugActivity : BaseActivity() {

    // View Binding alternative (manual for legacy layout)
    private lateinit var body: LinearLayout
    private lateinit var icBug: ImageView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var cardview1: CardView
    private lateinit var clearDataBtn: Button
    private lateinit var scroll: ScrollView
    private lateinit var errorText: TextView

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        
        initializeViews()
        setupListeners()
        initializeLogic()
    }

    private fun initializeViews() {
        body = findViewById(R.id.body)
        icBug = findViewById(R.id.ic_bug)
        title = findViewById(R.id.title)
        subtitle = findViewById(R.id.subtitle)
        cardview1 = findViewById(R.id.cardview1)
        clearDataBtn = findViewById(R.id.clearData_btn)
        scroll = findViewById(R.id.scroll)
        errorText = findViewById(R.id.error_text)
    }

    private fun setupListeners() {
        clearDataBtn.setOnClickListener {
            showClearDataConfirmation()
        }

        errorText.setOnLongClickListener {
            copyErrorToClipboard()
            true
        }
    }

    private fun initializeLogic() {
        intent.getStringExtra(EXTRA_ERROR)?.let { error ->
            errorText.text = error
        }
    }

    private fun showClearDataConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear App Data")
            .setMessage("This will clear all app data and close the application. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                clearData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyErrorToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("error_log", errorText.text.toString())
        clipboard.setPrimaryClip(clip)
        
        vibrateDevice(48)
        SketchwareUtil.showMessage(applicationContext, "Error log copied to clipboard")
    }

    private fun vibrateDevice(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    /**
     * Set status bar and navigation bar colors
     */
    private fun setStateColor(statusColor: Int, navigationColor: Int) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = statusColor
        window.navigationBarColor = navigationColor
    }

    /**
     * Apply graphics styling to a view with ripple effect
     */
    private fun applyViewGraphics(
        view: View,
        backgroundColor: Int,
        rippleColor: Int,
        radius: Float,
        strokeWidth: Float,
        strokeColor: Int
    ) {
        val gradientDrawable = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = radius
            setStroke(strokeWidth.toInt(), strokeColor)
        }

        val rippleDrawable = RippleDrawable(
            android.content.res.ColorStateList.valueOf(rippleColor),
            gradientDrawable,
            null
        )

        view.background = rippleDrawable
    }

    /**
     * Clear all application data
     * This will reset the app to its initial state
     */
    private fun clearData() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.clearApplicationUserData()
            } else {
                @Suppress("DEPRECATION")
                Runtime.getRuntime().exec("pm clear ${applicationContext.packageName}")
            }
            finishAffinity()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to clear app data", e)
            SketchwareUtil.showMessage(applicationContext, "Failed to clear data: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "DebugActivity"
        const val EXTRA_ERROR = "error"

        /**
         * Launch DebugActivity with error message
         */
        fun launch(context: Context, errorMessage: String) {
            val intent = android.content.Intent(context, DebugActivity::class.java).apply {
                putExtra(EXTRA_ERROR, errorMessage)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
