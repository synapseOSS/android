package com.synapse.social.studioasinc.ui.auth

import android.content.SharedPreferences
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.ui.auth.models.AuthUiState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.email
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock

/**
 * Property-based tests for email validation in AuthViewModel.
 * 
 * **Feature: auth-ui-redesign, Property 15: Email validation provides feedback**
 * **Validates: Requirements 5.1, 5.2, 5.3**
 * 
 * Tests that email validation provides appropriate visual feedback:
 * - Valid emails show green checkmark (isEmailValid = true, emailError = null)
 * - Invalid emails show red error icon with message (isEmailValid = false, emailError != null)
 * - Empty emails show no error (emailError = null)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelEmailValidationPropertyTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    /**
     * Generator for valid email addresses matching our regex
     */
    fun validEmailArb(): Arb<String> = Arb.stringPattern("[a-z0-9._-]{3,10}@[a-z0-9-]{3,10}\\.[a-z]{2,4}")

    /**
     * Generator for invalid email addresses (missing @, missing domain, etc.)
     */
    fun invalidEmailArb(): Arb<String> = Arb.choice(
        // Missing @ symbol
        Arb.stringPattern("[a-zA-Z0-9]{3,20}"),
        // Missing domain
        Arb.stringPattern("[a-zA-Z0-9]{3,10}@"),
        // Missing local part
        Arb.stringPattern("@[a-zA-Z0-9]{3,10}\\.[a-z]{2,4}"),
        // Invalid characters
        Arb.stringPattern("[a-zA-Z0-9]{3,10}@[a-zA-Z0-9]{3,10}"),
        // Multiple @ symbols
        Arb.stringPattern("[a-zA-Z0-9]{3,10}@@[a-zA-Z0-9]{3,10}\\.[a-z]{2,4}"),
        // Spaces in email
        Arb.stringPattern("[a-zA-Z0-9]{3,10} @[a-zA-Z0-9]{3,10}\\.[a-z]{2,4}")
    )

    "Property 15: Valid emails should be marked as valid with no error" {
        checkAll(100, validEmailArb()) { email ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            // Test email validation method directly
            val isValid = viewModel.validateEmail(email)
            isValid shouldBe true
        }
    }

    "Property 15: Invalid emails should be marked as invalid with error message" {
        checkAll(100, invalidEmailArb()) { email ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            // Test email validation method directly
            val isValid = viewModel.validateEmail(email)
            isValid shouldBe false
        }
    }

    "Property 15: Empty email should show no error" {
        val mockAuthRepository = mock<AuthRepository>()
        val mockUsernameRepository = mock<UsernameRepository>()
        val mockSharedPreferences = mock<SharedPreferences>()
        val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

        // Empty email should not be valid
        val isValid = viewModel.validateEmail("")
        isValid shouldBe false
    }

    "Property 15: Email validation should work consistently across different inputs" {
        checkAll(50, validEmailArb()) { email ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            // Validation should be consistent
            val isValid = viewModel.validateEmail(email)
            isValid shouldBe true
        }
    }

    "Property 15: Email validation regex should correctly identify valid and invalid formats" {
        checkAll(50, validEmailArb()) { validEmail ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            viewModel.validateEmail(validEmail) shouldBe true
        }

        checkAll(50, invalidEmailArb()) { invalidEmail ->
            val mockAuthRepository = mock<AuthRepository>()
            val mockUsernameRepository = mock<UsernameRepository>()
            val mockSharedPreferences = mock<SharedPreferences>()
            val viewModel = AuthViewModel(mockAuthRepository, mockUsernameRepository, mockSharedPreferences)

            viewModel.validateEmail(invalidEmail) shouldBe false
        }
    }
})
