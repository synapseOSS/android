package com.synapse.social.studioasinc.ui.auth

import android.content.SharedPreferences
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock

/**
 * Property-based tests for username validation in AuthViewModel.
 *
 * **Feature: auth-ui-redesign, Property 7: Username validation enforces minimum length**
 * **Validates: Requirements 2.5**
 *
 * Tests that username validation enforces minimum length requirement of 3 characters.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelUsernamePropertyTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "Property 7: Username with length < 3 is invalid" {
        checkAll(Arb.string().filter { it.length < 3 }) { username ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            val isValid = viewModel.validateUsername(username)
            isValid shouldBe false
        }
    }

    "Property 7: Username with length >= 3 is valid" {
        checkAll(Arb.string(minSize = 3, maxSize = 20).filter { it.matches(Regex("^[a-zA-Z0-9_]+$")) }) { username ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            val isValid = viewModel.validateUsername(username)
            isValid shouldBe true
        }
    }
})
