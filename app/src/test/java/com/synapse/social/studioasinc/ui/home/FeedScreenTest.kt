package com.synapse.social.studioasinc.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysCorrectMessage() {
        composeTestRule.setContent {
            FeedEmpty()
        }

        composeTestRule.onNodeWithText("No posts yet. Follow people to see their posts!")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_displaysMessageAndRetryButton() {
        composeTestRule.setContent {
            FeedError(message = "Network Error", onRetry = {})
        }

        composeTestRule.onNodeWithText("Network Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
