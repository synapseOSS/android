package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class FollowUserUseCaseTest {

    @Mock
    private lateinit var repository: ProfileRepository

    private lateinit var useCase: FollowUserUseCase

    @Before
    fun setup() {
        useCase = FollowUserUseCase(repository)
    }

    @Test
    fun `invoke calls repository followUser`() = runTest {
        whenever(repository.followUser("user1", "user2")).thenReturn(Result.success(Unit))

        val result = useCase("user1", "user2")

        Assert.assertTrue(result.isSuccess)
        verify(repository).followUser("user1", "user2")
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        val exception = Exception("Follow failed")
        whenever(repository.followUser("user1", "user2")).thenReturn(Result.failure(exception))

        val result = useCase("user1", "user2")

        Assert.assertTrue(result.isFailure)
    }

    @Test
    fun `invoke validates userId is not empty`() = runTest {
        try {
            useCase("", "user2")
            Assert.fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message?.contains("User ID") == true)
        }
        verify(repository, never()).followUser(any(), any())
    }
}
