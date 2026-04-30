package com.pacedream.app.feature.hostprofile

data class HostProfileUiState(
    val isLoading: Boolean = false,
    val host: HostProfileModel? = null,
    val errorMessage: String? = null,
    val isContactingHost: Boolean = false,
)
