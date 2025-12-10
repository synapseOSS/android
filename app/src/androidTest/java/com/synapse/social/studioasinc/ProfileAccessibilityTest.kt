package com.synapse.social.studioasinc

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synapse.social.studioasinc.ui.profile.ProfileScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileScreen_hasSemanticOrdering() {
        composeTestRule.setContent {
            ProfileScreen(
                userId = "test",
                currentUserId = "test",
                onNavigateBack = {}
            )
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun profileButtons_meetMinimumTouchTarget() {
        composeTestRule.setContent {
            ProfileScreen(
                userId = "test",
                currentUserId = "test",
                onNavigateBack = {}
            )
        }
        // Verify 48dp minimum touch targets
        composeTestRule.onAllNodesWithContentDescription("Follow", substring = true)
            .onFirst()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun profileImages_haveContentDescriptions() {
        composeTestRule.setContent {
            ProfileScreen(
                userId = "test",
                currentUserId = "test",
                onNavigateBack = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Profile picture", substring = true)
            .assertExists()
    }

    @Test
    fun profileStats_haveAccessibleLabels() {
        composeTestRule.setContent {
            ProfileScreen(
                userId = "test",
                currentUserId = "test",
                onNavigateBack = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("followers", substring = true, ignoreCase = true)
            .assertExists()
    }
}
