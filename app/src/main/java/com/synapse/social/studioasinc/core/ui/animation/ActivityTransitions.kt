package com.synapse.social.studioasinc.core.ui.animation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityOptionsCompat
import com.synapse.social.studioasinc.R

/**
 * Utility object for premium activity transition animations.
 * Provides smooth slide, scale, and fade transitions between activities.
 */
object ActivityTransitions {

    /**
     * Start an activity with premium enter transition.
     * The new activity slides in from the right with scale and fade.
     * 
     * @param context The context (usually an Activity)
     * @param intent The intent for the activity to start
     */
    fun startActivityWithTransition(context: Context, intent: Intent) {
        context.startActivity(intent)
        if (context is Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.activity_open_enter,
                    R.anim.activity_open_exit
                )
            } else {
                @Suppress("DEPRECATION")
                context.overridePendingTransition(
                    R.anim.activity_open_enter,
                    R.anim.activity_open_exit
                )
            }
        }
    }

    /**
     * Finish the current activity with premium exit transition.
     * The activity slides out to the right with scale and fade.
     * 
     * @param activity The activity to finish
     */
    fun finishWithTransition(activity: Activity) {
        activity.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.activity_close_enter,
                R.anim.activity_close_exit
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(
                R.anim.activity_close_enter,
                R.anim.activity_close_exit
            )
        }
    }
}

/**
 * Extension function to start an activity with premium transition.
 */
fun Activity.startActivityWithPremiumTransition(intent: Intent) {
    ActivityTransitions.startActivityWithTransition(this, intent)
}

/**
 * Extension function to finish with premium transition.
 */
fun Activity.finishWithPremiumTransition() {
    ActivityTransitions.finishWithTransition(this)
}
