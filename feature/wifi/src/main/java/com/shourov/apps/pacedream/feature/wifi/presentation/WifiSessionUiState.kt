package com.shourov.apps.pacedream.feature.wifi.presentation

import java.time.Instant

/**
 * Visible state for the persistent Wi-Fi session pill and its derived sheets.
 *
 * `expiresAt` is server-anchored. The view layer subtracts `Instant.now()` for
 * display only; it never extends or shortens `expiresAt` on its own.
 */
data class WifiSessionUiState(
    val sessionId: String? = null,
    val bookingId: String? = null,
    val ssid: String? = null,
    val password: String? = null,
    val expiresAt: Instant? = null,
    val secondsRemaining: Long = 0L,
    val canExtend: Boolean = true,
    val graceSeconds: Long = 0L,
    val phase: Phase = Phase.Idle,
    val sheet: Sheet = Sheet.None,
    val extensionInProgress: Boolean = false,
    val errorMessage: String? = null
) {
    enum class Phase {
        /** No active session known. Pill is hidden. */
        Idle,
        /** Plenty of time left (>15 min). Green pill. */
        Active,
        /** ≤15 min: amber pill, inline extend chip. */
        Warning,
        /** ≤3 min: red pill, auto-presents extension sheet. */
        Critical,
        /** Server has ended the session. Expired modal opens. */
        Expired
    }

    enum class Sheet { None, Session, Extend, Expired }

    val isVisible: Boolean get() = phase != Phase.Idle
}
