package com.synapse.social.studioasinc.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator

/**
 * Configuration for post card animations
 */
data class AnimationConfig(
    val entranceDuration: Long = 300L,
    val interactionDuration: Long = 100L,
    val exitDuration: Long = 250L,
    val staggerDelay: Long = 50L,
    val enableEntranceAnimations: Boolean = true,
    val enableInteractionAnimations: Boolean = true,
    val respectMotionPreferences: Boolean = true
) {
    companion object {
        val DEFAULT = AnimationConfig()
    }
}

/**
 * Utility object for post card animations following Material Design 3 motion guidelines
 */
object PostCardAnimations {
    
    private const val TAG = "PostCardAnimations"
    
    // Animation property constants
    private const val ENTRANCE_SCALE_FROM = 0.95f
    private const val ENTRANCE_SCALE_TO = 1.0f
    private const val PRESS_SCALE = 0.98f
    private const val BUTTON_BOUNCE_SCALE = 1.2f
    private const val EXIT_SCALE = 0.9f
    
    /**
     * Check if animations should be enabled based on system motion preferences
     */
    fun shouldAnimate(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale > 0f
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check motion preferences", e)
            true // Default to enabled if check fails
        }
    }
    
    /**
     * Safely execute an animation with error handling and view attachment checks
     */
    private fun safeAnimate(view: View, config: AnimationConfig, animation: () -> Unit) {
        try {
            if (!view.isAttachedToWindow) {
                Log.w(TAG, "View not attached to window, skipping animation")
                return
            }
            
            if (config.respectMotionPreferences && !shouldAnimate(view.context)) {
                Log.d(TAG, "Motion preferences disabled, skipping animation")
                return
            }
            
            animation()
        } catch (e: Exception) {
            Log.e(TAG, "Animation failed", e)
        }
    }
    
    /**
     * Animate card entrance with fade-in and scale-up
     * @param view The card view to animate
     * @param position The position in the list for stagger calculation
     * @param config Animation configuration
     */
    fun animateEntrance(view: View, position: Int, config: AnimationConfig) {
        if (!config.enableEntranceAnimations) return
        
        safeAnimate(view, config) {
            // Cancel any existing animations
            view.animate().cancel()
            
            // Set initial state
            view.alpha = 0f
            view.scaleX = ENTRANCE_SCALE_FROM
            view.scaleY = ENTRANCE_SCALE_FROM
            
            // Calculate stagger delay (cap at 10 items to prevent excessive delay)
            val staggerDelay = (position.coerceAtMost(10) * config.staggerDelay)
            
            // Enable hardware layer for better performance
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // Animate to final state
            view.animate()
                .alpha(1f)
                .scaleX(ENTRANCE_SCALE_TO)
                .scaleY(ENTRANCE_SCALE_TO)
                .setDuration(config.entranceDuration)
                .setStartDelay(staggerDelay)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                }
                .start()
        }
    }
    
    /**
     * Animate card press interaction
     * @param view The card view to animate
     * @param config Animation configuration
     */
    fun animatePress(view: View, config: AnimationConfig) {
        if (!config.enableInteractionAnimations) return
        
        safeAnimate(view, config) {
            view.animate().cancel()
            
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            view.animate()
                .scaleX(PRESS_SCALE)
                .scaleY(PRESS_SCALE)
                .setDuration(config.interactionDuration)
                .setInterpolator(LinearOutSlowInInterpolator())
                .withEndAction {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                }
                .start()
        }
    }
    
    /**
     * Animate card release interaction
     * @param view The card view to animate
     * @param config Animation configuration
     */
    fun animateRelease(view: View, config: AnimationConfig) {
        if (!config.enableInteractionAnimations) return
        
        safeAnimate(view, config) {
            view.animate().cancel()
            
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            view.animate()
                .scaleX(ENTRANCE_SCALE_TO)
                .scaleY(ENTRANCE_SCALE_TO)
                .setDuration(config.interactionDuration)
                .setInterpolator(FastOutLinearInInterpolator())
                .withEndAction {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                }
                .start()
        }
    }
    
    /**
     * Animate button click with bounce effect
     * @param view The button view to animate
     * @param config Animation configuration
     */
    fun animateButtonClick(view: View, config: AnimationConfig) {
        if (!config.enableInteractionAnimations) return
        
        safeAnimate(view, config) {
            view.animate().cancel()
            
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // Create bounce animation: 1.0 -> 1.2 -> 1.0
            val scaleUpDuration = 200L
            val scaleDownDuration = 200L
            
            view.animate()
                .scaleX(BUTTON_BOUNCE_SCALE)
                .scaleY(BUTTON_BOUNCE_SCALE)
                .setDuration(scaleUpDuration)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    view.animate()
                        .scaleX(ENTRANCE_SCALE_TO)
                        .scaleY(ENTRANCE_SCALE_TO)
                        .setDuration(scaleDownDuration)
                        .setInterpolator(OvershootInterpolator())
                        .withEndAction {
                            view.setLayerType(View.LAYER_TYPE_NONE, null)
                        }
                        .start()
                }
                .start()
        }
    }
    
    /**
     * Animate card exit with fade-out and scale-down
     * @param view The card view to animate
     * @param config Animation configuration
     * @param onComplete Callback when animation completes
     */
    fun animateExit(view: View, config: AnimationConfig, onComplete: () -> Unit) {
        safeAnimate(view, config) {
            view.animate().cancel()
            
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            view.animate()
                .alpha(0f)
                .scaleX(EXIT_SCALE)
                .scaleY(EXIT_SCALE)
                .setDuration(config.exitDuration)
                .setInterpolator(FastOutLinearInInterpolator())
                .withEndAction {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    onComplete()
                }
                .start()
        }
    }
    
    /**
     * Animate content update with cross-fade
     * @param view The view to animate
     * @param config Animation configuration
     * @param onMidpoint Callback at the midpoint of animation (when alpha is 0) to update content
     */
    fun animateContentUpdate(view: View, config: AnimationConfig, onMidpoint: (() -> Unit)? = null) {
        safeAnimate(view, config) {
            view.animate().cancel()
            
            val fadeDuration = 200L
            
            view.animate()
                .alpha(0f)
                .setDuration(fadeDuration)
                .setInterpolator(LinearInterpolator())
                .withEndAction {
                    // Update content at midpoint when view is invisible
                    onMidpoint?.invoke()
                    
                    view.animate()
                        .alpha(1f)
                        .setDuration(fadeDuration)
                        .setInterpolator(LinearInterpolator())
                        .start()
                }
                .start()
        }
    }
    
    /**
     * Animate count change with scale pulse effect
     * @param view The view displaying the count (typically a TextView or Button)
     * @param config Animation configuration
     * @param onMidpoint Callback at the midpoint to update the count value
     */
    fun animateCountChange(view: View, config: AnimationConfig, onMidpoint: (() -> Unit)? = null) {
        safeAnimate(view, config) {
            view.animate().cancel()
            
            val pulseDuration = 150L
            val pulseScale = 1.15f
            
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // Scale up
            view.animate()
                .scaleX(pulseScale)
                .scaleY(pulseScale)
                .setDuration(pulseDuration)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    // Update count at peak of animation
                    onMidpoint?.invoke()
                    
                    // Scale back down
                    view.animate()
                        .scaleX(ENTRANCE_SCALE_TO)
                        .scaleY(ENTRANCE_SCALE_TO)
                        .setDuration(pulseDuration)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction {
                            view.setLayerType(View.LAYER_TYPE_NONE, null)
                        }
                        .start()
                }
                .start()
        }
    }
    
    /**
     * Animate like state change with heart bounce and color transition
     * @param view The like button view
     * @param isLiked Whether the post is now liked
     * @param config Animation configuration
     */
    fun animateLikeStateChange(view: View, isLiked: Boolean, config: AnimationConfig) {
        if (!config.enableInteractionAnimations) return
        
        safeAnimate(view, config) {
            view.animate().cancel()
            
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            if (isLiked) {
                // Liked: Bigger bounce animation
                val scaleUpDuration = 150L
                val scaleDownDuration = 200L
                val bounceScale = 1.3f
                
                view.animate()
                    .scaleX(bounceScale)
                    .scaleY(bounceScale)
                    .setDuration(scaleUpDuration)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        view.animate()
                            .scaleX(ENTRANCE_SCALE_TO)
                            .scaleY(ENTRANCE_SCALE_TO)
                            .setDuration(scaleDownDuration)
                            .setInterpolator(OvershootInterpolator())
                            .withEndAction {
                                view.setLayerType(View.LAYER_TYPE_NONE, null)
                            }
                            .start()
                    }
                    .start()
            } else {
                // Unliked: Subtle scale down and back
                val duration = 150L
                val scaleDown = 0.85f
                
                view.animate()
                    .scaleX(scaleDown)
                    .scaleY(scaleDown)
                    .setDuration(duration)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        view.animate()
                            .scaleX(ENTRANCE_SCALE_TO)
                            .scaleY(ENTRANCE_SCALE_TO)
                            .setDuration(duration)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction {
                                view.setLayerType(View.LAYER_TYPE_NONE, null)
                            }
                            .start()
                    }
                    .start()
            }
        }
    }
    
    /**
     * Animate image load with fade-in
     * @param imageView The image view to animate
     * @param config Animation configuration
     */
    fun animateImageLoad(imageView: ImageView, config: AnimationConfig) {
        safeAnimate(imageView, config) {
            imageView.alpha = 0f
            imageView.animate()
                .alpha(1f)
                .setDuration(config.entranceDuration)
                .setInterpolator(LinearInterpolator())
                .start()
        }
    }
    
    /**
     * Create a shimmer animation for image placeholders
     * @param view The view to apply shimmer to
     * @return ValueAnimator for the shimmer effect
     */
    fun createShimmerAnimation(view: View): ValueAnimator {
        val shimmerAnimator = ValueAnimator.ofFloat(-1.5f, 1.5f)
        shimmerAnimator.duration = 1200L
        shimmerAnimator.repeatMode = ValueAnimator.RESTART
        shimmerAnimator.repeatCount = ValueAnimator.INFINITE
        shimmerAnimator.interpolator = LinearInterpolator()
        
        shimmerAnimator.addUpdateListener {
            view.invalidate()
        }
        
        return shimmerAnimator
    }
    
    /**
     * Cancel all animations on a view
     * @param view The view to cancel animations on
     */
    fun cancelAnimations(view: View) {
        try {
            view.animate().cancel()
            view.clearAnimation()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel animations", e)
        }
    }
}

/**
 * Shimmer drawable for image loading placeholders
 */
class ShimmerDrawable(
    private val baseColor: Int = Color.parseColor("#E0E0E0"),
    private val shimmerColor: Int = Color.parseColor("#F5F5F5")
) : Drawable() {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private var shimmerAnimator: ValueAnimator? = null
    private var shader: LinearGradient? = null
    
    init {
        paint.style = Paint.Style.FILL
    }
    
    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        
        // Create shader if needed
        if (shader == null) {
            shader = LinearGradient(
                0f, 0f, bounds.width().toFloat(), 0f,
                intArrayOf(baseColor, shimmerColor, baseColor),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
        }
        
        canvas.drawRect(bounds, paint)
    }
    
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }
    
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }
    
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    
    /**
     * Start shimmer animation
     */
    fun startShimmer() {
        if (shimmerAnimator?.isRunning == true) return
        
        shimmerAnimator = ValueAnimator.ofFloat(-1.5f, 1.5f).apply {
            duration = 1200L
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                val width = bounds.width()
                val translate = width * animatedValue
                matrix.setTranslate(translate, 0f)
                shader?.setLocalMatrix(matrix)
                invalidateSelf()
            }
            
            start()
        }
    }
    
    /**
     * Stop shimmer animation
     */
    fun stopShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
    }
    
    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        // Recreate shader when bounds change
        shader = null
    }
}

/**
 * Extension functions for integrating shimmer with Glide image loading
 */
object GlideShimmerExtensions {
    
    /**
     * Load an image with Glide and show shimmer placeholder during loading
     * @param imageView The ImageView to load the image into
     * @param imageUrl The URL of the image to load
     * @param errorDrawableRes Resource ID for error placeholder (optional)
     * @param onLoadSuccess Callback when image loads successfully (optional)
     * @param onLoadFailed Callback when image load fails (optional)
     */
    fun loadImageWithShimmer(
        imageView: ImageView,
        imageUrl: String?,
        errorDrawableRes: Int? = null,
        onLoadSuccess: (() -> Unit)? = null,
        onLoadFailed: (() -> Unit)? = null
    ) {
        // Skip if URL is null or empty
        if (imageUrl.isNullOrEmpty()) {
            imageView.visibility = View.GONE
            return
        }
        
        imageView.visibility = View.VISIBLE
        
        // Create and start shimmer drawable
        val shimmerDrawable = ShimmerDrawable()
        imageView.setImageDrawable(shimmerDrawable)
        shimmerDrawable.startShimmer()
        
        // Load image with Glide
        try {
            val glide = com.bumptech.glide.Glide.with(imageView.context)
            val requestBuilder = glide.load(imageUrl)
            
            // Add error placeholder if provided
            if (errorDrawableRes != null) {
                requestBuilder.error(errorDrawableRes)
            }
            
            requestBuilder
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Stop shimmer on failure
                        shimmerDrawable.stopShimmer()
                        
                        // Show error placeholder with fade-in if provided
                        if (errorDrawableRes != null) {
                            imageView.alpha = 0f
                            imageView.animate()
                                .alpha(1f)
                                .setDuration(200L)
                                .setInterpolator(LinearInterpolator())
                                .start()
                        }
                        
                        onLoadFailed?.invoke()
                        return false // Let Glide handle error drawable
                    }
                    
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Stop shimmer on success
                        shimmerDrawable.stopShimmer()
                        
                        // Fade in the loaded image
                        imageView.alpha = 0f
                        imageView.setImageDrawable(resource)
                        imageView.animate()
                            .alpha(1f)
                            .setDuration(300L)
                            .setInterpolator(LinearInterpolator())
                            .start()
                        
                        onLoadSuccess?.invoke()
                        return true // We handle the drawable ourselves
                    }
                })
                .into(imageView)
        } catch (e: Exception) {
            Log.e("GlideShimmer", "Failed to load image", e)
            shimmerDrawable.stopShimmer()
            onLoadFailed?.invoke()
        }
    }
    
    /**
     * Load an image with Glide, shimmer placeholder, and custom animation config
     * @param imageView The ImageView to load the image into
     * @param imageUrl The URL of the image to load
     * @param config Animation configuration
     * @param errorDrawableRes Resource ID for error placeholder (optional)
     */
    fun loadImageWithShimmer(
        imageView: ImageView,
        imageUrl: String?,
        config: AnimationConfig,
        errorDrawableRes: Int? = null
    ) {
        // Skip if URL is null or empty
        if (imageUrl.isNullOrEmpty()) {
            imageView.visibility = View.GONE
            return
        }
        
        // Check motion preferences
        if (config.respectMotionPreferences && !PostCardAnimations.shouldAnimate(imageView.context)) {
            // Load without animations if motion is disabled
            loadImageWithoutAnimation(imageView, imageUrl, errorDrawableRes)
            return
        }
        
        loadImageWithShimmer(imageView, imageUrl, errorDrawableRes)
    }
    
    /**
     * Load image without animations (for accessibility)
     */
    private fun loadImageWithoutAnimation(
        imageView: ImageView,
        imageUrl: String,
        errorDrawableRes: Int?
    ) {
        imageView.visibility = View.VISIBLE
        
        try {
            val glide = com.bumptech.glide.Glide.with(imageView.context)
            val requestBuilder = glide.load(imageUrl)
            
            if (errorDrawableRes != null) {
                requestBuilder.error(errorDrawableRes)
            }
            
            requestBuilder.into(imageView)
        } catch (e: Exception) {
            Log.e("GlideShimmer", "Failed to load image", e)
        }
    }
}
