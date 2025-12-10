package com.synapse.social.studioasinc.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.synapse.social.studioasinc.data.model.UserProfile
import org.junit.*

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockProfile = UserProfile(
        id = "1",
        username = "testuser",
        name = "Test User",
        nickname = "Tester",
        bio = "Test bio",
        profileImageUrl = null,
        coverImageUrl = null,
        isVerified = true,
        isPrivate = false,
        postCount = 10,
        followerCount = 100,
        followingCount = 50,
        joinedDate = System.currentTimeMillis(),
        location = "Test City",
        relationshipStatus = null,
        birthday = null,
        work = null,
        education = null,
        currentCity = null,
        hometown = null,
        website = null,
        gender = null,
        pronouns = null,
        linkedAccounts = emptyList(),
        privacySettings = emptyMap()
    )

    @Test
    fun profileScreen_displaysUsername() {
        composeTestRule.setContent {
            ProfileHeader(
                profileImageUrl = mockProfile.profileImageUrl,
                name = mockProfile.name,
                username = mockProfile.username,
                nickname = mockProfile.nickname,
                bio = mockProfile.bio,
                isVerified = mockProfile.isVerified,
                hasStory = false,
                postsCount = mockProfile.postCount,
                followersCount = mockProfile.followerCount,
                followingCount = mockProfile.followingCount,
                isOwnProfile = true,
                onProfileImageClick = {},
                onEditProfileClick = {},
                onAddStoryClick = {},
                onMoreClick = {},
                onStatsClick = {}
            )
        }

        composeTestRule.onNodeWithText("@${mockProfile.username}").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysName() {
        composeTestRule.setContent {
            ProfileHeader(
                profileImageUrl = mockProfile.profileImageUrl,
                name = mockProfile.name,
                username = mockProfile.username,
                nickname = mockProfile.nickname,
                bio = mockProfile.bio,
                isVerified = mockProfile.isVerified,
                hasStory = false,
                postsCount = mockProfile.postCount,
                followersCount = mockProfile.followerCount,
                followingCount = mockProfile.followingCount,
                isOwnProfile = true,
                onProfileImageClick = {},
                onEditProfileClick = {},
                onAddStoryClick = {},
                onMoreClick = {},
                onStatsClick = {}
            )
        }

        composeTestRule.onNodeWithText(mockProfile.name).assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysStats() {
        composeTestRule.setContent {
            ProfileHeader(
                profileImageUrl = mockProfile.profileImageUrl,
                name = mockProfile.name,
                username = mockProfile.username,
                nickname = mockProfile.nickname,
                bio = mockProfile.bio,
                isVerified = mockProfile.isVerified,
                hasStory = false,
                postsCount = mockProfile.postCount,
                followersCount = mockProfile.followerCount,
                followingCount = mockProfile.followingCount,
                isOwnProfile = true,
                onProfileImageClick = {},
                onEditProfileClick = {},
                onAddStoryClick = {},
                onMoreClick = {},
                onStatsClick = {}
            )
        }

        composeTestRule.onNodeWithText("${mockProfile.postCount} Posts").assertIsDisplayed()
    }

    @Test
    fun contentFilterBar_switchesFilters() {
        var selectedFilter = ProfileContentFilter.POSTS

        composeTestRule.setContent {
            ContentFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )
        }

        composeTestRule.onNodeWithText("Photos").performClick()
        Assert.assertEquals(ProfileContentFilter.PHOTOS, selectedFilter)
    }
}
