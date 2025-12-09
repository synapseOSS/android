package com.synapse.social.studioasinc

import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResponsiveDesignTest {

    private lateinit var scenario: ActivityScenario<AuthActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(AuthActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun testPhonePortrait_360x640() {
        onView(withId(R.id.cardLogo))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvAppName))
            .check(matches(isDisplayed()))
            .check(matches(withText("Synapse")))

        onView(withId(R.id.tvWelcome))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardFormContainer))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tilEmail))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tilPassword))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnSignIn))
            .check(matches(isDisplayed()))
            .check(matches(withText("Sign In")))

        onView(withId(R.id.layoutSocialAuth))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnGoogleAuth))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnFacebookAuth))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnAppleAuth))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPhoneLandscape_640x360() {
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        Thread.sleep(1000)

        onView(withId(R.id.cardLogo))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardFormContainer))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tilEmail))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tilPassword))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnSignIn))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnGoogleAuth))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTabletPortrait_768x1024() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = context.resources.configuration

        val isTablet = configuration.smallestScreenWidthDp >= 600

        if (isTablet) {
            onView(withId(R.id.cardLogo))
                .check(matches(isDisplayed()))

            onView(withId(R.id.cardFormContainer))
                .check(matches(isDisplayed()))

            onView(withId(R.id.tilEmail))
                .check(matches(isDisplayed()))

            onView(withId(R.id.tilPassword))
                .check(matches(isDisplayed()))

            onView(withId(R.id.btnSignIn))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testTabletLandscape_1024x768() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = context.resources.configuration
        val isTablet = configuration.smallestScreenWidthDp >= 600

        if (isTablet) {
            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            Thread.sleep(1000)

            onView(withId(R.id.cardLogo))
                .check(matches(isDisplayed()))

            onView(withId(R.id.cardFormContainer))
                .check(matches(isDisplayed()))

            onView(withId(R.id.tilEmail))
                .check(matches(isDisplayed()))

            onView(withId(R.id.tilPassword))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testFormWidthConstraints() {
        onView(withId(R.id.cardFormContainer))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tilEmail))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }

    @Test
    fun testFormCentering() {
        onView(withId(R.id.cardLogo))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvAppName))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardFormContainer))
            .check(matches(isDisplayed()))

        onView(withId(R.id.layoutSocialAuth))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testKeyboardHandling_EmailField() {
        onView(withId(R.id.etEmail))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.etEmail))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"))

        onView(withId(R.id.etEmail))
            .check(matches(withText("test@example.com")))
    }

    @Test
    fun testKeyboardHandling_PasswordField() {
        onView(withId(R.id.etPassword))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.etPassword))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etPassword))
            .perform(typeText("password123"))

        onView(withId(R.id.etPassword))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testKeyboardHandling_SignUpMode() {
        onView(withId(R.id.tvToggleMode))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.tilUsername))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etUsername))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.etUsername))
            .perform(typeText("testuser"))

        onView(withId(R.id.etUsername))
            .check(matches(withText("testuser")))
    }

    @Test
    fun testKeyboardHandling_Landscape() {
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        Thread.sleep(1000)

        onView(withId(R.id.etEmail))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.etEmail))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"))

        onView(withId(R.id.etEmail))
            .check(matches(withText("test@example.com")))
    }

    @Test
    fun testScrollViewFunctionality() {
        onView(withId(R.id.etEmail))
            .perform(click(), typeText("test@example.com"))

        onView(withId(R.id.etEmail))
            .perform(closeSoftKeyboard())

        onView(withId(R.id.btnGoogleAuth))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardLogo))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testModeToggle_DifferentOrientations() {
        onView(withId(R.id.tvToggleMode))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.btnSignIn))
            .check(matches(withText("Create Account")))

        onView(withId(R.id.tilUsername))
            .check(matches(isDisplayed()))

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        Thread.sleep(1000)

        onView(withId(R.id.tilUsername))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvToggleMode))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.btnSignIn))
            .check(matches(withText("Sign In")))
    }

    @Test
    fun testSocialAuthButtons_AllOrientations() {
        onView(withId(R.id.btnGoogleAuth))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnFacebookAuth))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnAppleAuth))
            .check(matches(isDisplayed()))

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        Thread.sleep(1000)

        onView(withId(R.id.btnGoogleAuth))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnFacebookAuth))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnAppleAuth))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testErrorCard_DifferentSizes() {
        onView(withId(R.id.btnSignIn))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.tilEmail))
            .check(matches(isDisplayed()))

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        Thread.sleep(1000)

        onView(withId(R.id.tilEmail))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTouchTargetSizes() {
        onView(withId(R.id.btnSignIn))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))

        onView(withId(R.id.tvToggleMode))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        onView(withId(R.id.btnGoogleAuth))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        onView(withId(R.id.btnFacebookAuth))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        onView(withId(R.id.btnAppleAuth))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        onView(withId(R.id.tvForgotPassword))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
}
