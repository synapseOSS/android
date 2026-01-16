package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Alignment

/**
 * Animation specifications for the Settings feature.
 * 
 * Provides consistent motion design across all settings screens following
 * Material Design 3 motion guidelines for smooth, expressive transitions.
 * 
 * Requirements: 1.2
 */
object SettingsAnimations {

    // Animation durations
    private const val SCREEN_TRANSITION_DURATION = 300
    private const val CONTENT_ANIMATION_DURATION = 200
    private const val PRESS_ANIMATION_DURATION = 100

    /**
     * Enter transition for navigating to a settings screen.
     * Combines fade in with horizontal slide from the right.
     */
    val enterTransition: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { fullWidth -> fullWidth / 4 }
    )

    /**
     * Exit transition for leaving a settings screen (going forward).
     * Combines fade out with horizontal slide to the left.
     */
    val exitTransition: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { fullWidth -> -fullWidth / 4 }
    )

    /**
     * Pop enter transition for navigating back to a settings screen.
     * Combines fade in with horizontal slide from the left.
     */
    val popEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { fullWidth -> -fullWidth / 4 }
    )

    /**
     * Pop exit transition for leaving a settings screen (going back).
     * Combines fade out with horizontal slide to the right.
     */
    val popExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { fullWidth -> fullWidth / 4 }
    )

    /**
     * Expand animation for content that expands vertically (e.g., expandable sections).
     */
    val expandAnimation: EnterTransition = expandVertically(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        ),
        expandFrom = Alignment.Top
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

    /**
     * Collapse animation for content that shrinks vertically.
     */
    val collapseAnimation: ExitTransition = shrinkVertically(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        ),
        shrinkTowards = Alignment.Top
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

    /**
     * Scale factor for press feedback animation.
     * Cards and buttons scale down slightly when pressed.
     */
    const val pressScale: Float = 0.98f

    /**
     * Duration for press feedback animation in milliseconds.
     */
    const val pressDuration: Int = PRESS_ANIMATION_DURATION

    /**
     * Animation spec for press scale animation.
     */
    val pressAnimationSpec = tween<Float>(
        durationMillis = PRESS_ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    )
}

/**
 * Extension functions for applying settings animations to navigation.
 */
object SettingsNavigationAnimations {
    
    /**
     * Provides enter transition for AnimatedContentTransitionScope.
     * Use this in NavHost composable for consistent screen transitions.
     */
    fun <T> AnimatedContentTransitionScope<T>.settingsEnterTransition(): EnterTransition {
        return SettingsAnimations.enterTransition
    }

    /**
     * Provides exit transition for AnimatedContentTransitionScope.
     * Use this in NavHost composable for consistent screen transitions.
     */
    fun <T> AnimatedContentTransitionScope<T>.settingsExitTransition(): ExitTransition {
        return SettingsAnimations.exitTransition
    }

    /**
     * Provides pop enter transition for AnimatedContentTransitionScope.
     * Use this in NavHost composable for consistent back navigation transitions.
     */
    fun <T> AnimatedContentTransitionScope<T>.settingsPopEnterTransition(): EnterTransition {
        return SettingsAnimations.popEnterTransition
    }

    /**
     * Provides pop exit transition for AnimatedContentTransitionScope.
     * Use this in NavHost composable for consistent back navigation transitions.
     */
    fun <T> AnimatedContentTransitionScope<T>.settingsPopExitTransition(): ExitTransition {
        return SettingsAnimations.popExitTransition
    }
}
