package com.synapse.social.studioasinc.ui.auth

import android.content.SharedPreferences
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.ui.auth.models.PasswordStrength
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
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
 * Property-based tests for password strength calculation in AuthViewModel.
 *
 * **Feature: auth-ui-redesign, Property 5: Password strength is calculated correctly**
 * **Validates: Requirements 2.2**
 *
 * Tests that password strength is calculated correctly based on length and complexity.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelPasswordPropertyTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "Property 5: Password with length < 8 is always Weak" {
        checkAll(Arb.string().filter { it.length < 8 }) { password ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            val strength = viewModel.calculatePasswordStrength(password)
            strength shouldBe PasswordStrength.Weak
        }
    }

    // Helper to calculate expected score for verification
    fun calculateExpectedStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.Weak

        var score = 0
        if (password.length >= 12) score += 2
        else if (password.length >= 10) score += 1

        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score >= 5 -> PasswordStrength.Strong
            score >= 3 -> PasswordStrength.Fair
            else -> PasswordStrength.Weak
        }
    }

    "Property 5: Password strength matches expected calculation logic" {
        checkAll(Arb.string(minSize = 8, maxSize = 30)) { password ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            val strength = viewModel.calculatePasswordStrength(password)
            strength shouldBe calculateExpectedStrength(password)
        }
    }
})
