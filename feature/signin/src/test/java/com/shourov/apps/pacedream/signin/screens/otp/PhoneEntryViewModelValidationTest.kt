package com.shourov.apps.pacedream.signin.screens.otp

import com.shourov.apps.pacedream.core.network.repository.OtpRepository
import com.shourov.apps.pacedream.core.network.services.OtpService
import com.shourov.apps.pacedream.model.request.OtpCheckRequest
import com.shourov.apps.pacedream.model.request.OtpLoginRequest
import com.shourov.apps.pacedream.model.request.OtpSendRequest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Locks the audit's C-03 invariant (`INTERACTION_AUDIT_REPORT.md`): the
 * Continue button on `PhoneEntryScreen` is gated on
 * `uiState.isValidPhone`. Previously the validation was commented out
 * and the screen navigated past phone entry without an OTP — an
 * authentication-security bypass. This test exercises the ViewModel
 * gating directly so a future regression of the bypass fails CI.
 *
 * The screen wires `ProcessButton.isEnabled =
 * uiState.isValidPhone && !uiState.isLoading && !uiState.isBlocked`,
 * so a green `isValidPhone` test is sufficient to prove the button
 * cannot be tapped with an invalid number.
 */
class PhoneEntryViewModelValidationTest {

    private lateinit var viewModel: PhoneEntryViewModel

    @Before
    fun setUp() {
        // The validation under test does not touch the repository, but
        // the ctor requires one. A throwing OtpService catches accidental
        // I/O — if the test ever ends up firing a real network call, it
        // surfaces as a clear test failure instead of a flaky DNS lookup.
        val throwingService = object : OtpService {
            override suspend fun sendOTP(request: OtpSendRequest) =
                error("OTP service must not be hit from validation tests")

            override suspend fun verifyOTP(request: OtpCheckRequest) =
                error("OTP service must not be hit from validation tests")

            override suspend fun login(request: OtpLoginRequest) =
                error("OTP service must not be hit from validation tests")

            override suspend fun sendOTPLegacy(request: OtpSendRequest) =
                error("OTP service must not be hit from validation tests")

            override suspend fun verifyOTPLegacy(request: OtpCheckRequest) =
                error("OTP service must not be hit from validation tests")

            override suspend fun loginLegacy(request: OtpLoginRequest) =
                error("OTP service must not be hit from validation tests")
        }
        viewModel = PhoneEntryViewModel(otpRepository = OtpRepository(throwingService))
    }

    @After
    fun tearDown() {
        // ViewModel does not own resources beyond its CoroutineScope, which
        // is JUnit-friendly without explicit cleanup.
    }

    @Test
    fun `empty phone leaves isValidPhone false and produces no error`() {
        viewModel.updatePhoneNumber("")
        val state = viewModel.uiState.value
        assertFalse("empty phone must not be valid", state.isValidPhone)
        // We don't show an error on empty input — only after the user
        // types something invalid. Otherwise the field is red on focus.
        assertNull("empty phone must not show an error message", state.phoneError)
    }

    @Test
    fun `partial US number is not valid yet`() {
        viewModel.updatePhoneNumber("+1415")
        val state = viewModel.uiState.value
        assertFalse(state.isValidPhone)
        assertNotNull("partial US number must surface an error so the user knows why Continue is disabled", state.phoneError)
    }

    @Test
    fun `well-formed US number is valid`() {
        viewModel.updatePhoneNumber("+14155551234")
        val state = viewModel.uiState.value
        assertTrue("well-formed +1 phone (10 digits) must be valid", state.isValidPhone)
        assertNull(state.phoneError)
    }

    @Test
    fun `well-formed Canadian number is valid`() {
        viewModel.updatePhoneNumber("+16475559876")
        val state = viewModel.uiState.value
        assertTrue("+1 followed by 10 digits covers Canada too", state.isValidPhone)
        assertNull(state.phoneError)
    }

    @Test
    fun `non-US prefix is rejected with the unsupported-country message`() {
        viewModel.updatePhoneNumber("+447911123456")
        val state = viewModel.uiState.value
        assertFalse(state.isValidPhone)
        val err = state.phoneError ?: error("expected an error for unsupported country")
        assertTrue(
            "non-US prefix should mention US/Canada — got: '$err'",
            err.contains("United States", ignoreCase = true) || err.contains("Canada", ignoreCase = true)
        )
    }

    @Test
    fun `bare 1 prefix without plus is rejected`() {
        viewModel.updatePhoneNumber("14155551234")
        val state = viewModel.uiState.value
        assertFalse("US number without + prefix must not be considered valid", state.isValidPhone)
    }

    @Test
    fun `extra digits after a US number are rejected`() {
        viewModel.updatePhoneNumber("+141555512345")
        val state = viewModel.uiState.value
        assertFalse("11-digit suffix must fail", state.isValidPhone)
    }

    @Test
    fun `updating from valid back to invalid clears isValidPhone`() {
        viewModel.updatePhoneNumber("+14155551234")
        assertTrue(viewModel.uiState.value.isValidPhone)

        // User deletes a digit.
        viewModel.updatePhoneNumber("+1415555123")
        assertFalse(
            "isValidPhone must drop back to false when the user deletes a digit — otherwise the Continue button stays enabled with a stale-valid state",
            viewModel.uiState.value.isValidPhone
        )
    }
}
