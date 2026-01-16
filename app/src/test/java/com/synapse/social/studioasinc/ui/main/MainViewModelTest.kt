package com.synapse.social.studioasinc.ui.main

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var authRepository: AuthRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkUserAuthentication does not have delay`() = runTest {
        // Arrange
        whenever(authRepository.restoreSession()).thenReturn(true)
        whenever(authRepository.getCurrentUserId()).thenReturn("user123")
        whenever(authRepository.getCurrentUserEmail()).thenReturn("user@example.com")

        // Mock user result
        val mockUser = User(uid = "user123", banned = false)
        whenever(userRepository.getUserById(any())).thenReturn(Result.success(mockUser))


        // Create the ViewModel
        viewModel = MainViewModel(application, authRepository, userRepository)

        runCurrent()
        // Now it should be called immediately
        Mockito.verify(authRepository).restoreSession()
    }
}
