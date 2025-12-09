package com.synapse.social.studioasinc.ui.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.domain.usecase.profile.*
import com.synapse.social.studioasinc.domain.usecase.post.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class ProfileViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var getProfileUseCase: GetProfileUseCase
    @Mock private lateinit var getProfileContentUseCase: GetProfileContentUseCase
    @Mock private lateinit var followUserUseCase: FollowUserUseCase
    @Mock private lateinit var unfollowUserUseCase: UnfollowUserUseCase
    @Mock private lateinit var likePostUseCase: LikePostUseCase
    @Mock private lateinit var unlikePostUseCase: UnlikePostUseCase
    @Mock private lateinit var savePostUseCase: SavePostUseCase
    @Mock private lateinit var unsavePostUseCase: UnsavePostUseCase
    @Mock private lateinit var deletePostUseCase: DeletePostUseCase
    @Mock private lateinit var reportPostUseCase: ReportPostUseCase
    @Mock private lateinit var lockProfileUseCase: LockProfileUseCase
    @Mock private lateinit var archiveProfileUseCase: ArchiveProfileUseCase
    @Mock private lateinit var blockUserUseCase: BlockUserUseCase
    @Mock private lateinit var reportUserUseCase: ReportUserUseCase
    @Mock private lateinit var muteUserUseCase: MuteUserUseCase

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProfileViewModel(
            getProfileUseCase, getProfileContentUseCase, followUserUseCase,
            unfollowUserUseCase, likePostUseCase, unlikePostUseCase,
            savePostUseCase, unsavePostUseCase, deletePostUseCase,
            reportPostUseCase, lockProfileUseCase, archiveProfileUseCase,
            blockUserUseCase, reportUserUseCase, muteUserUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProfile sets loading state initially`() = runTest {
        val state = viewModel.state.value
        Assert.assertEquals(ProfileUiState.Loading, state.profileState)
    }

    @Test
    fun `loadProfile success updates state with profile`() = runTest {
        val profile = UserProfile(
            id = "1", username = "test", name = "Test User",
            nickname = null, bio = null, profileImageUrl = null,
            coverImageUrl = null, isVerified = false, isPrivate = false,
            postCount = 0, followerCount = 0, followingCount = 0,
            joinedDate = 0L, location = null, relationshipStatus = null,
            birthday = null, work = null, education = null,
            currentCity = null, hometown = null, website = null,
            gender = null, pronouns = null, linkedAccounts = emptyList(),
            privacySettings = emptyMap()
        )
        whenever(getProfileUseCase("1")).thenReturn(flowOf(Result.success(profile)))

        viewModel.loadProfile("1", "current")
        advanceUntilIdle()

        val state = viewModel.state.value
        Assert.assertTrue(state.profileState is ProfileUiState.Success)
        Assert.assertEquals(profile, (state.profileState as ProfileUiState.Success).profile)
    }

    @Test
    fun `switchContentFilter updates filter state`() {
        viewModel.switchContentFilter(ProfileContentFilter.PHOTOS)
        Assert.assertEquals(ProfileContentFilter.PHOTOS, viewModel.state.value.contentFilter)
    }

    @Test
    fun `toggleMoreMenu toggles menu visibility`() {
        val initial = viewModel.state.value.showMoreMenu
        viewModel.toggleMoreMenu()
        Assert.assertEquals(!initial, viewModel.state.value.showMoreMenu)
    }
}
