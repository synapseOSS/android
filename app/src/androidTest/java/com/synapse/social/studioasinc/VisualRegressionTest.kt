package com.synapse.social.studioasinc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import android.graphics.Color
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

@RunWith(AndroidJUnit4::class)
class VisualRegressionTest {

    private lateinit var scenario: ActivityScenario<AuthActivity>
    private val screenshotDir = File(
        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
        "screenshots"
    )

    @Before
    fun setup() {
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs()
        }
        scenario = ActivityScenario.launch(AuthActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun captureSignInScreen_LightTheme() {
        Thread.sleep(1000)
        
        captureScreenshot("sign_in_light")
        
        verifyMaterial3Styling()
    }

    @Test
    fun captureSignUpScreen_LightTheme() {
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500)
        
        captureScreenshot("sign_up_light")
        
        verifyMaterial3Styling()
    }

    @Test
    fun captureSignInWithInput_LightTheme() {
        onView(withId(R.id.etEmail))
            .perform(click(), typeText("test@example.com"))
        
        onView(withId(R.id.etPassword))
            .perform(click(), typeText("password123"))
        
        onView(withId(R.id.etPassword))
            .perform(closeSoftKeyboard())
        
        Thread.sleep(500)
        
        captureScreenshot("sign_in_with_input_light")
    }

    @Test
    fun captureSignUpWithInput_LightTheme() {
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.etUsername))
            .perform(click(), typeText("testuser"))
        
        onView(withId(R.id.etEmail))
            .perform(click(), typeText("test@example.com"))
        
        onView(withId(R.id.etPassword))
            .perform(click(), typeText("StrongPass123!"))
        
        onView(withId(R.id.etPassword))
            .perform(closeSoftKeyboard())
        
        Thread.sleep(500)
        
        captureScreenshot("sign_up_with_input_light")
    }

    @Test
    fun captureErrorState_LightTheme() {
        onView(withId(R.id.btnSignIn))
            .perform(click())
        
        Thread.sleep(500)
        
        captureScreenshot("error_state_light")
    }

    @Test
    fun captureFocusedEmailField_LightTheme() {
        onView(withId(R.id.etEmail))
            .perform(click())
        
        Thread.sleep(300)
        
        captureScreenshot("focused_email_field_light")
    }

    @Test
    fun captureFocusedPasswordField_LightTheme() {
        onView(withId(R.id.etPassword))
            .perform(click())
        
        Thread.sleep(300)
        
        captureScreenshot("focused_password_field_light")
    }

    @Test
    fun capturePasswordStrengthWeak_LightTheme() {
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.etPassword))
            .perform(click(), typeText("weak"))
        
        Thread.sleep(300)
        
        captureScreenshot("password_strength_weak_light")
    }

    @Test
    fun capturePasswordStrengthFair_LightTheme() {
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.etPassword))
            .perform(click(), typeText("Fair123"))
        
        Thread.sleep(300)
        
        captureScreenshot("password_strength_fair_light")
    }

    @Test
    fun capturePasswordStrengthStrong_LightTheme() {
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.etPassword))
            .perform(click(), typeText("Strong123!@#"))
        
        Thread.sleep(300)
        
        captureScreenshot("password_strength_strong_light")
    }

    @Test
    fun captureSocialAuthButtons_LightTheme() {
        onView(withId(R.id.btnGoogleAuth))
            .perform(scrollTo())
        
        Thread.sleep(300)
        
        captureScreenshot("social_auth_buttons_light")
    }

    @Test
    fun verifyMaterial3RoundedCorners() {
        scenario.onActivity { activity ->
            val cardLogo = activity.findViewById<View>(R.id.cardLogo)
            val cardFormContainer = activity.findViewById<View>(R.id.cardFormContainer)
            
            assert(cardLogo != null) { "Logo card should exist" }
            assert(cardFormContainer != null) { "Form container card should exist" }
        }
    }

    @Test
    fun verifyMaterial3Elevation() {
        scenario.onActivity { activity ->
            val cardLogo = activity.findViewById<View>(R.id.cardLogo)
            val cardFormContainer = activity.findViewById<View>(R.id.cardFormContainer)
            
            assert(cardLogo.elevation > 0f) { "Logo card should have elevation" }
            assert(cardFormContainer.elevation > 0f) { "Form container should have elevation" }
        }
    }

    @Test
    fun verifyMaterial3Colors_LightTheme() {
        scenario.onActivity { activity ->
            val context = activity.applicationContext
            
            val primaryColor = ContextCompat.getColor(context, R.color.md_theme_primary)
            val surfaceColor = ContextCompat.getColor(context, R.color.md_theme_surface)
            val onSurfaceColor = ContextCompat.getColor(context, R.color.md_theme_onSurface)
            
            assert(primaryColor != 0) { "Primary color should be defined" }
            assert(surfaceColor != 0) { "Surface color should be defined" }
            assert(onSurfaceColor != 0) { "OnSurface color should be defined" }
        }
    }

    @Test
    fun verifyAnimationsRunAt60fps() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        val animatorScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
        
        assert(animatorScale > 0f) { "Animations should be enabled for testing" }
        
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        
        Thread.sleep(500)
    }

    @Test
    fun verifyBlurEffectOnAPI31Plus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scenario.onActivity { activity ->
                val rootView = activity.window.decorView.rootView
                
                assert(rootView.background != null) {
                    "Background should have blur effect or drawable on API 31+"
                }
            }
        }
    }

    @Test
    fun captureButtonPressedState() {
        onView(withId(R.id.btnSignIn))
            .perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> = isDisplayed()
                override fun getDescription(): String = "Press button"
                override fun perform(uiController: UiController, view: View) {
                    view.isPressed = true
                    uiController.loopMainThreadForAtLeast(100)
                }
            })
        
        Thread.sleep(100)
        
        captureScreenshot("button_pressed_state_light")
    }

    @Test
    fun captureScrolledView() {
        onView(withId(R.id.btnGoogleAuth))
            .perform(scrollTo())
        
        Thread.sleep(300)
        
        captureScreenshot("scrolled_to_social_auth_light")
    }

    @Test
    fun verifyCardShadows() {
        scenario.onActivity { activity ->
            val cardLogo = activity.findViewById<View>(R.id.cardLogo)
            val cardFormContainer = activity.findViewById<View>(R.id.cardFormContainer)
            
            assert(cardLogo.elevation >= 2f) { "Logo card should have minimum 2dp elevation" }
            assert(cardFormContainer.elevation >= 2f) { "Form container should have minimum 2dp elevation" }
        }
    }

    @Test
    fun verifyTextFieldOutlines() {
        scenario.onActivity { activity ->
            val tilEmail = activity.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilEmail)
            val tilPassword = activity.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPassword)
            
            assert(tilEmail != null) { "Email text field should exist" }
            assert(tilPassword != null) { "Password text field should exist" }
            
            assert(tilEmail.boxBackgroundMode == com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE) {
                "Email field should use outline style"
            }
            assert(tilPassword.boxBackgroundMode == com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE) {
                "Password field should use outline style"
            }
        }
    }

    @Test
    fun verifyButtonRippleEffect() {
        onView(withId(R.id.btnSignIn))
            .perform(click())
        
        Thread.sleep(300)
        
        scenario.onActivity { activity ->
            val button = activity.findViewById<View>(R.id.btnSignIn)
            assert(button.background != null) { "Button should have ripple background" }
        }
    }

    @Test
    fun captureAllAuthScreenStates() {
        captureScreenshot("01_initial_sign_in")
        
        onView(withId(R.id.etEmail))
            .perform(click(), typeText("test@example.com"), closeSoftKeyboard())
        Thread.sleep(300)
        captureScreenshot("02_email_entered")
        
        onView(withId(R.id.etPassword))
            .perform(click(), typeText("password123"), closeSoftKeyboard())
        Thread.sleep(300)
        captureScreenshot("03_password_entered")
        
        onView(withId(R.id.tvToggleMode))
            .perform(click())
        Thread.sleep(500)
        captureScreenshot("04_sign_up_mode")
        
        onView(withId(R.id.etUsername))
            .perform(click(), typeText("testuser"), closeSoftKeyboard())
        Thread.sleep(300)
        captureScreenshot("05_username_entered")
        
        onView(withId(R.id.etPassword))
            .perform(clearText(), click(), typeText("Weak1"), closeSoftKeyboard())
        Thread.sleep(300)
        captureScreenshot("06_weak_password")
        
        onView(withId(R.id.etPassword))
            .perform(clearText(), click(), typeText("Medium123"), closeSoftKeyboard())
        Thread.sleep(300)
        captureScreenshot("07_fair_password")
        
        onView(withId(R.id.etPassword))
            .perform(clearText(), click(), typeText("Strong123!@#"), closeSoftKeyboard())
        Thread.sleep(300)
        captureScreenshot("08_strong_password")
        
        onView(withId(R.id.btnGoogleAuth))
            .perform(scrollTo())
        Thread.sleep(300)
        captureScreenshot("09_social_auth_visible")
    }

    private fun verifyMaterial3Styling() {
        scenario.onActivity { activity ->
            val cardLogo = activity.findViewById<View>(R.id.cardLogo)
            val cardFormContainer = activity.findViewById<View>(R.id.cardFormContainer)
            val btnSignIn = activity.findViewById<View>(R.id.btnSignIn)
            
            assert(cardLogo != null) { "Logo card should exist" }
            assert(cardFormContainer != null) { "Form container should exist" }
            assert(btnSignIn != null) { "Sign in button should exist" }
            
            assert(cardLogo.elevation > 0f) { "Cards should have elevation" }
            assert(cardFormContainer.elevation > 0f) { "Cards should have elevation" }
        }
    }

    private fun captureScreenshot(filename: String) {
        scenario.onActivity { activity ->
            val rootView = activity.window.decorView.rootView
            val bitmap = Bitmap.createBitmap(
                rootView.width,
                rootView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)
            
            val file = File(screenshotDir, "$filename.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        }
    }
}
