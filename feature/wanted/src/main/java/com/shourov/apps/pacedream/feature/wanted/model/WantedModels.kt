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
    /**
     * Start of the requested window. Newly created requests send ISO-8601
     * (`yyyy-MM-dd`); legacy records may carry free-text strings like
     * "Sat 10:00 AM" — both are rendered via [RequestDateFormatter].
     */
    val dateTime: String?,
    val endDate: String? = null,
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

// ============================================================================
// Web-parity taxonomy
//
// The backend at POST /v1/requests requires `type` to be one of the keys
// below and `category` to be from the per-type list. Display labels are
// surfaced in the UI; keys are what hit the wire.
// ============================================================================

enum class WantedType(val key: String, val label: String, val subtitle: String) {
    Space("space", "A space",
        "A room, parking spot, desk, studio, etc."),
    Item("item", "An item",
        "A camera, tool, gear, or anything you can borrow."),
    Service("service", "A service",
        "Help, delivery, tutoring, moving, and more."),
    ;

    companion object {
        fun fromKey(key: String?): WantedType =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Space
    }
}

data class WantedCategoryOption(val key: String, val label: String)

/**
 * Built-in starter set of categories per type. Keys match the most common
 * web values; the backend is the source of truth and may extend this list
 * over time. A future `GET /v1/categories` endpoint should drive this
 * dynamically — see the API assumptions in the PR description.
 */
val WantedCategoriesByType: Map<WantedType, List<WantedCategoryOption>> = mapOf(
    WantedType.Space to listOf(
        WantedCategoryOption("parking", "Parking"),
        WantedCategoryOption("workspace", "Workspace"),
        WantedCategoryOption("meeting_room", "Meeting room"),
        WantedCategoryOption("study_room", "Study room"),
        WantedCategoryOption("short_stay", "Short stay"),
        WantedCategoryOption("apartment", "Apartment"),
        WantedCategoryOption("storage", "Storage"),
        WantedCategoryOption("gym", "Gym"),
        WantedCategoryOption("other", "Other"),
    ),
    WantedType.Item to listOf(
        WantedCategoryOption("camera", "Camera"),
        WantedCategoryOption("gear", "Outdoor gear"),
        WantedCategoryOption("electronics", "Electronics"),
        WantedCategoryOption("sports", "Sports equipment"),
        WantedCategoryOption("tools", "Tools"),
        WantedCategoryOption("other", "Other"),
    ),
    WantedType.Service to listOf(
        WantedCategoryOption("home_help", "Home help"),
        WantedCategoryOption("moving", "Moving"),
        WantedCategoryOption("delivery", "Delivery"),
        WantedCategoryOption("tutoring", "Tutoring"),
        WantedCategoryOption("other", "Other"),
    ),
)

/**
 * @deprecated Kept temporarily so other call sites compile during migration.
 * Use [WantedType] for the top-level taxonomy.
 */
val WantedCategories: List<String> = listOf(
    WantedType.Space.label,
    WantedType.Item.label,
    WantedType.Service.label,
)

sealed interface RequestsListUiState {
    data object Loading : RequestsListUiState
    data class Error(val message: String) : RequestsListUiState
    data class Content(val requests: List<WantedRequest>) : RequestsListUiState
}

enum class RequestSort(val key: String, val label: String) {
    Newest("newest", "Newest"),
    HighestBudget("highest_budget", "Highest budget"),
    Nearest("nearest", "Nearest"),
    ;

    companion object {
        fun fromKey(key: String?): RequestSort =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Newest
    }
}

/**
 * Client-side filter + sort applied on top of the full list returned by
 * `GET /v1/requests`. When the backend grows query params for these, the
 * fields here will move onto the wire — keep the names aligned.
 */
@Immutable
data class FilterState(
    val type: WantedType? = null,
    val category: String? = null,
    val sort: RequestSort = RequestSort.Newest,
) {
    val isActive: Boolean
        get() = type != null || category != null || sort != RequestSort.Newest
}

sealed interface RequestDetailUiState {
    data object Loading : RequestDetailUiState
    data class Error(val message: String) : RequestDetailUiState
    data class Content(val request: WantedRequest) : RequestDetailUiState
}

data class CreateRequestForm(
    val type: WantedType = WantedType.Space,
    val category: String = "parking",
    val title: String = "",
    val description: String = "",
    val locationCity: String = "",
    val locationState: String = "",
    val locationCountry: String = "",
    /** Epoch millis at UTC midnight for the start of the requested window. */
    val startDate: Long? = null,
    /** Epoch millis at UTC midnight for the end of the requested window. */
    val endDate: Long? = null,
    val budget: String = "",
    val imageUrl: String? = null,
)

data class CreateRequestUiState(
    val form: CreateRequestForm = CreateRequestForm(),
    val submitting: Boolean = false,
    val uploading: Boolean = false,
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
