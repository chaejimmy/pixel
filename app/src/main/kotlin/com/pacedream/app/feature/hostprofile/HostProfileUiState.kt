package com.pacedream.app.feature.hostprofile

/**
 * Sealed UI state for the host profile screen.
 * Loading / Content / NotFound / Error keep the screen logic explicit
 * and matches the documented MVP states.
 */
sealed interface HostProfileUiState {
    data object Loading : HostProfileUiState
    data object NotFound : HostProfileUiState
    data class Error(val message: String) : HostProfileUiState
    data class Content(
        val host: HostProfileModel,
        val isContactingHost: Boolean = false,
    ) : HostProfileUiState
}
