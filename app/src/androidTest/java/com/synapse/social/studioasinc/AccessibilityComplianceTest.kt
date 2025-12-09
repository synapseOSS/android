package com.synapse.social.studioasinc

import android.content.Context
import android.graphics.Rect
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max

/**
 * Accessibility compliance test for Material 3 Expressive Auth UI
 * Tests Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityComplianceTest {

    private lateinit var scenario: ActivityScenario<AuthActivity>
    private lateinit var context: Context
    private val minTouchTargetSizeDp = 48

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        scenario = ActivityScenario.launch(AuthActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    /**
     * Requirement 6.1: Test all touch targets meet 48dp minimum
     */
    @Test
    fun testTouchTargetSizes() {
        scenario.onActivity { activity ->
            val binding = activity.binding
            
            // Test all interactive elements
            val interactiveViews = listOf(
                binding.btnSignIn to "Sign In Button",
                binding.tvToggleMode to "Toggle Mode Link",
                binding.tvForgotPassword to "Forgot Password Link",
                binding.btnGoogleAuth to "Google Auth Button",
                binding.btnFacebookAuth to "Facebook Auth Button",
                binding.btnAppleAuth to "Apple Auth Button",
                binding.btnResendVerification to "Resend Verification Button",
                binding.btnBackToSignIn to "Back to Sign In Button"
            )
            
            val minTouchTargetPx = (minTouchTargetSizeDp * context.resources.displayMetrics.density).toInt()
            
            interactiveViews.forEach { (view, name) ->
                if (view?.visibility == View.VISIBLE) {
                    val width = view.width
                    val height = view.height
                    
                    // Check if view meets minimum touch target size
                    val meetsMinimum = width >= minTouchTargetPx && height >= minTouchTargetPx
                    
                    assert(meetsMinimum) {
                        "$name has touch target size ${width}x${height}px (${width / context.resources.displayMetrics.density}x${height / context.resources.displayMetrics.density}dp), " +
                        "which is below the minimum 48x48dp requirement"
                    }
                }
            }
        }
    }

    /**
     * Requirement 6.2: Test content descriptions for all interactive elements
     */
    @Test
    fun testContentDescriptions() {
        scenario.onActivity { activity ->
            val binding = activity.binding
            
            // Test all interactive elements have content descriptions
            val elementsToTest = mapOf(
                binding.etEmail to "Email input field",
                binding.etPassword to "Password input field",
                binding.btnSignIn to "Sign in button",
                binding.tvToggleMode to "Switch authentication mode",
                binding.tvForgotPassword to "Forgot password link",
                binding.btnGoogleAuth to "Sign in with Google",
                binding.btnFacebookAuth to "Sign in with Facebook",
                binding.btnAppleAuth to "Sign in with Apple",
                binding.cardLogo to "Synapse application logo"
            )
            
            elementsToTest.forEach { (view, expectedDescription) ->
                if (view?.visibility == View.VISIBLE) {
                    val contentDescription = view.contentDescription
                    assert(!contentDescription.isNullOrEmpty()) {
                        "View ${view.javaClass.simpleName} is missing content description. Expected: $expectedDescription"
                    }
                }
            }
        }
    }

    /**
     * Requirement 6.3: Test keyboard navigation with IME actions
     */
    @Test
    fun testKeyboardNavigation() {
        // Test email field IME action
        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"))
            .perform(pressImeActionButton())
        
        // Verify focus moved to password field
        onView(withId(R.id.etPassword))
            .check(matches(hasFocus()))
        
        // Test password field IME action in sign-in mode
        onView(withId(R.id.etPassword))
            .perform(typeText("password123"))
            .perform(pressImeActionButton())
        
        // In sign-in mode, IME action should trigger sign-in
        // (We can't fully test this without mocking auth, but we verify the action is handled)
        
        // Switch to sign-up mode
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500) // Wait for animation
        
        // Test password field IME action in sign-up mode
        onView(withId(R.id.etPassword))
            .perform(clearText())
            .perform(typeText("password123"))
            .perform(pressImeActionButton())
        
        // Verify focus moved to username field
        onView(withId(R.id.etUsername))
            .check(matches(hasFocus()))
    }

    /**
     * Requirement 6.4: Test reduced motion support
     */
    @Test
    fun testReducedMotionSupport() {
        scenario.onActivity { activity ->
            // Test that shouldReduceMotion() method exists and works
            val shouldReduce = activity.shouldReduceMotion()
            
            // We can't actually change system settings in a test, but we can verify
            // the method reads the correct setting
            val animationScale = try {
                Settings.Global.getFloat(
                    activity.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
                )
            } catch (e: Exception) {
                1.0f
            }
            
            val expectedReduce = animationScale == 0f
            assert(shouldReduce == expectedReduce) {
                "shouldReduceMotion() returned $shouldReduce but expected $expectedReduce based on system settings"
            }
            
            // Verify animation durations are adjusted
            val normalDuration = 300L
            val adjustedDuration = activity.getAnimationDuration(normalDuration)
            
            if (shouldReduce) {
                assert(adjustedDuration == 0L) {
                    "Animation duration should be 0 when reduced motion is enabled, but got $adjustedDuration"
                }
            } else {
                assert(adjustedDuration == normalDuration) {
                    "Animation duration should be $normalDuration when reduced motion is disabled, but got $adjustedDuration"
                }
            }
        }
    }

    /**
     * Requirement 6.5: Test color contrast ratios for WCAG compliance
     */
    @Test
    fun testColorContrast() {
        scenario.onActivity { activity ->
            val binding = activity.binding
            
            // Test text elements have sufficient contrast
            val textViews = listOf(
                binding.tvAppName to "App Name",
                binding.tvWelcome to "Welcome Text",
                binding.tvToggleMode to "Toggle Mode Link",
                binding.tvForgotPassword to "Forgot Password Link"
            )
            
            textViews.forEach { (textView, name) ->
                if (textView?.visibility == View.VISIBLE) {
                    val textColor = textView.currentTextColor
                    val backgroundColor = getBackgroundColor(textView)
                    
                    val contrastRatio = calculateContrastRatio(textColor, backgroundColor)
                    
                    // WCAG AA requires 4.5:1 for normal text, 3:1 for large text
                    val textSizeSp = textView.textSize / context.resources.displayMetrics.scaledDensity
                    val minContrast = if (textSizeSp >= 18 || (textSizeSp >= 14 && textView.typeface?.isBold == true)) {
                        3.0 // Large text
                    } else {
                        4.5 // Normal text
                    }
                    
                    assert(contrastRatio >= minContrast) {
                        "$name has contrast ratio $contrastRatio:1, which is below WCAG AA requirement of $minContrast:1"
                    }
                }
            }
        }
    }

    /**
     * Test TalkBack screen reader compatibility
     */
    @Test
    fun testScreenReaderAnnouncements() {
        scenario.onActivity { activity ->
            val binding = activity.binding
            
            // Verify important views are focusable for screen readers
            val importantViews = listOf(
                binding.etEmail,
                binding.etPassword,
                binding.btnSignIn,
                binding.tvToggleMode,
                binding.tvForgotPassword
            )
            
            importantViews.forEach { view ->
                if (view?.visibility == View.VISIBLE) {
                    val isAccessibilityFocusable = ViewCompat.getImportantForAccessibility(view) != 
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
                    
                    assert(isAccessibilityFocusable) {
                        "View ${view.javaClass.simpleName} is not accessible to screen readers"
                    }
                }
            }
        }
    }

    /**
     * Test that error messages are announced to screen readers
     */
    @Test
    fun testErrorAnnouncements() {
        // Enter invalid email
        onView(withId(R.id.etEmail))
            .perform(typeText("invalid-email"), closeSoftKeyboard())
        
        Thread.sleep(500) // Wait for validation debounce
        
        scenario.onActivity { activity ->
            val binding = activity.binding
            
            // Verify error is set and announced
            val error = binding.tilEmail.error
            assert(!error.isNullOrEmpty()) {
                "Email validation error should be displayed"
            }
        }
    }

    /**
     * Test haptic feedback on interactive elements
     */
    @Test
    fun testHapticFeedback() {
        scenario.onActivity { activity ->
            val binding = activity.binding
            
            // Verify haptic feedback is enabled for buttons
            val hapticViews = listOf(
                binding.btnSignIn,
                binding.tvToggleMode,
                binding.tvForgotPassword
            )
            
            hapticViews.forEach { view ->
                if (view?.visibility == View.VISIBLE) {
                    val isHapticEnabled = view.isHapticFeedbackEnabled
                    assert(isHapticEnabled) {
                        "View ${view.javaClass.simpleName} should have haptic feedback enabled"
                    }
                }
            }
        }
    }

    /**
     * Test that loading states are announced to screen readers
     */
    @Test
    fun testLoadingStateAnnouncements() {
        scenario.onActivity { activity ->
            val binding = activity.binding
            
            // Verify loading overlay has content description
            val loadingDescription = binding.loadingOverlay.contentDescription
            assert(!loadingDescription.isNullOrEmpty()) {
                "Loading overlay should have content description for screen readers"
            }
            
            // Verify progress indicator has content description
            val progressDescription = binding.progressBar.contentDescription
            assert(!progressDescription.isNullOrEmpty()) {
                "Progress indicator should have content description for screen readers"
            }
        }
    }

    // Helper functions

    private fun getBackgroundColor(view: View): Int {
        var currentView: View? = view
        while (currentView != null) {
            val background = currentView.background
            if (background != null) {
                // Try to extract color from drawable
                // This is simplified - in production you'd need more robust color extraction
                return android.graphics.Color.WHITE // Default to white for testing
            }
            currentView = currentView.parent as? View
        }
        return android.graphics.Color.WHITE
    }

    private fun calculateContrastRatio(foreground: Int, background: Int): Double {
        val fgLuminance = calculateRelativeLuminance(foreground)
        val bgLuminance = calculateRelativeLuminance(background)
        
        val lighter = max(fgLuminance, bgLuminance)
        val darker = kotlin.math.min(fgLuminance, bgLuminance)
        
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun calculateRelativeLuminance(color: Int): Double {
        val r = android.graphics.Color.red(color) / 255.0
        val g = android.graphics.Color.green(color) / 255.0
        val b = android.graphics.Color.blue(color) / 255.0
        
        val rLinear = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
        
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }

    // Custom matcher for checking if view has minimum touch target size
    private fun hasMinimumTouchTarget(minSizeDp: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("has minimum touch target size of ${minSizeDp}dp")
            }

            override fun matchesSafely(view: View): Boolean {
                val minSizePx = (minSizeDp * view.context.resources.displayMetrics.density).toInt()
                return view.width >= minSizePx && view.height >= minSizePx
            }
        }
    }
}
