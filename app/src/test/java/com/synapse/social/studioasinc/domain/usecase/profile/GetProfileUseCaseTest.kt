package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.data.repository.ProfileRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class GetProfileUseCaseTest {

    @Mock
    private lateinit var repository: ProfileRepository

    private lateinit var useCase: GetProfileUseCase

    @Before
    fun setup() {
        useCase = GetProfileUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        val profile = UserProfile(
            id = "1", username = "test", name = "Test",
            nickname = null, bio = null, profileImageUrl = null,
            coverImageUrl = null, isVerified = false, isPrivate = false,
            postCount = 0, followerCount = 0, followingCount = 0,
            joinedDate = 0L, location = null, relationshipStatus = null,
            birthday = null, work = null, education = null,
            currentCity = null, hometown = null, website = null,
            gender = null, pronouns = null, linkedAccounts = emptyList(),
            privacySettings = emptyMap()
        )
        whenever(repository.getProfile("1")).thenReturn(flowOf(Result.success(profile)))

        val result = useCase("1").first()

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(profile, result.getOrNull())
        verify(repository).getProfile("1")
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        val exception = Exception("Network error")
        whenever(repository.getProfile("1")).thenReturn(flowOf(Result.failure(exception)))

        val result = useCase("1").first()

        Assert.assertTrue(result.isFailure)
        verify(repository).getProfile("1")
    }

    @Test
    fun `invoke validates userId is not empty`() = runTest {
        try {
            useCase("")
            Assert.fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message?.contains("User ID") == true)
        }
        verify(repository, never()).getProfile(any())
    }
}
