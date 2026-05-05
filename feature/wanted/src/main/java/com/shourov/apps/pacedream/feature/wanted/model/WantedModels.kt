package com.shourov.apps.pacedream.feature.wanted.model

import androidx.compose.runtime.Immutable

@Immutable
data class WantedRequest(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val category: String,
    val location: String,
    val budget: Double?,
    val budgetCurrency: String = "USD",
    val dateTime: String?,
    val imageUrl: String?,
    val authorName: String? = null,
    val authorAvatarUrl: String? = null,
    val offerCount: Int = 0,
)

@Immutable
data class WantedOffer(
    val id: String,
    val requestId: String,
    val price: Double,
    val currency: String = "USD",
    val message: String,
    val authorName: String? = null,
    val createdAt: String? = null,
)

/** Request categories shown in the create screen. */
val WantedCategories: List<String> = listOf(
    "Stay",
    "Service",
    "Gear",
    "Ride",
    "Help",
    "Other",
)

sealed interface RequestsListUiState {
    data object Loading : RequestsListUiState
    data class Error(val message: String) : RequestsListUiState
    data class Content(val requests: List<WantedRequest>) : RequestsListUiState
}

sealed interface RequestDetailUiState {
    data object Loading : RequestDetailUiState
    data class Error(val message: String) : RequestDetailUiState
    data class Content(val request: WantedRequest) : RequestDetailUiState
}

data class CreateRequestForm(
    val type: String = "Stay",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val dateTime: String = "",
    val budget: String = "",
    val imageUrl: String? = null,
)

data class CreateRequestUiState(
    val form: CreateRequestForm = CreateRequestForm(),
    val submitting: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
)

data class OfferFormState(
    val price: String = "",
    val message: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false,
)
