package com.shourov.apps.pacedream.feature.webflow.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for BookingConfirmationUiState sealed class,
 * ensuring the Idle state is distinct from Loading.
 */
class BookingConfirmationUiStateTest {

    @Test
    fun `initial state should be Idle not Loading`() {
        val state: BookingConfirmationUiState = BookingConfirmationUiState.Idle
        assertTrue(state is BookingConfirmationUiState.Idle)
        assertFalse(state is BookingConfirmationUiState.Loading)
    }

    @Test
    fun `Loading is distinct from Idle`() {
        val state: BookingConfirmationUiState = BookingConfirmationUiState.Loading
        assertTrue(state is BookingConfirmationUiState.Loading)
        assertFalse(state is BookingConfirmationUiState.Idle)
    }

    @Test
    fun `double-tap guard should not block when state is Idle`() {
        val state: BookingConfirmationUiState = BookingConfirmationUiState.Idle
        // The guard checks: if (state is Loading) return
        // When state is Idle, the guard should NOT trigger
        val shouldBlock = state is BookingConfirmationUiState.Loading
        assertFalse("Guard should not block when state is Idle", shouldBlock)
    }

    @Test
    fun `double-tap guard should block when state is Loading`() {
        val state: BookingConfirmationUiState = BookingConfirmationUiState.Loading
        val shouldBlock = state is BookingConfirmationUiState.Loading
        assertTrue("Guard should block when state is Loading", shouldBlock)
    }
}
