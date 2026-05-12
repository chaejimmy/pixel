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
    val authorId: String? = null,
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
    val authorAvatarUrl: String? = null,
    val createdAt: String? = null,
    val status: OfferStatus = OfferStatus.Pending,
    /** Snapshot of the parent request's title, when the backend embeds it. */
    val requestTitle: String? = null,
)

/**
 * Lifecycle of a [WantedOffer]. Mirrors the web backend's `status` field
 * on `/v1/offers`. Unknown values fall back to [Pending] so a future
 * status doesn't crash the screen.
 */
enum class OfferStatus(val key: String, val label: String) {
    Pending("pending", "Pending"),
    Accepted("accepted", "Accepted"),
    Declined("declined", "Declined"),
    ;

    companion object {
        fun fromKey(key: String?): OfferStatus =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Pending
    }
}

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

sealed interface MyOffersUiState {
    data object Loading : MyOffersUiState
    data class Error(val message: String) : MyOffersUiState
    data class Content(val offers: List<WantedOffer>) : MyOffersUiState
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
    data class Content(
        val request: WantedRequest,
        /** True when the viewer is the request's author. */
        val isOwner: Boolean = false,
        val isSignedIn: Boolean = false,
        /**
         * Offers visible to the request's owner. Empty when the viewer
         * isn't the owner (providers see the "Make an Offer" CTA) or
         * when no offers have been received yet.
         */
        val offers: List<WantedOffer> = emptyList(),
    ) : RequestDetailUiState
}

/**
 * Which slot of the requests screen tabs is selected. Persisted via
 * `SavedStateHandle` so a process restart restores the user's place
 * (Browse vs Mine), per the spec's acceptance criteria.
 */
enum class RequestsTab(val key: String) {
    Browse("browse"),
    Mine("mine"),
    ;

    companion object {
        fun fromKey(key: String?): RequestsTab =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Browse
    }
}

/**
 * Place selected by the user from autocomplete (or current-location reverse
 * geocoding). `lat`/`lng` are nullable to support device-Geocoder fallback
 * results that don't always carry coordinates.
 */
@Immutable
data class SelectedPlace(
    val city: String,
    val region: String,
    val country: String,
    val lat: Double?,
    val lng: Double?,
) {
    /** "City, Region, Country" for the read-only summary field. */
    val displayLine: String
        get() = listOf(city, region, country)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
}

data class CreateRequestForm(
    val type: WantedType = WantedType.Space,
    val category: String = "parking",
    val title: String = "",
    val description: String = "",
    /** Structured location chosen via Places autocomplete or current location. */
    val location: SelectedPlace? = null,
    /** Epoch millis at UTC midnight for the start of the requested window. */
    val startDate: Long? = null,
    /** Epoch millis at UTC midnight for the end of the requested window. */
    val endDate: Long? = null,
    val budget: String = "",
    val imageUrl: String? = null,
)

data class FieldErrors(
    val titleError: String? = null,
    val descriptionError: String? = null,
    val budgetError: String? = null,
) {
    fun isEmpty(): Boolean =
        titleError == null && descriptionError == null && budgetError == null
}

data class CreateRequestUiState(
    val form: CreateRequestForm = CreateRequestForm(),
    val fieldErrors: FieldErrors = FieldErrors(),
    val submitting: Boolean = false,
    val uploading: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
) {
    /**
     * Required fields are tracked separately from [fieldErrors] so that the
     * submit button can stay disabled for an empty form without showing a
     * red border under every field the user hasn't reached yet.
     */
    val requiredFieldsPresent: Boolean
        get() = form.title.trim().length >= TITLE_MIN_LENGTH &&
            form.description.trim().length >= DESCRIPTION_MIN_LENGTH &&
            form.category.isNotBlank()

    companion object {
        const val TITLE_MIN_LENGTH = 3
        const val TITLE_MAX_LENGTH = 200
        const val DESCRIPTION_MIN_LENGTH = 10
        const val DESCRIPTION_MAX_LENGTH = 2000
    }
}

/**
 * Lightweight summary used by the offer composer's "link a listing"
 * picker — full host-listing payloads carry pricing/calendar/etc. that
 * aren't needed for selection.
 */
@Immutable
data class HostListingSummary(
    val id: String,
    val title: String,
)

/**
 * Selectable expiry windows shown as chips on the offer composer. The
 * `hours` value is what hits the wire as `expiresInHours`.
 */
enum class OfferExpiry(val hours: Int, val label: String) {
    OneDay(24, "24h"),
    TwoDays(48, "48h"),
    OneWeek(24 * 7, "7d"),
    ;

    companion object {
        fun fromHours(hours: Int): OfferExpiry =
            entries.firstOrNull { it.hours == hours } ?: TwoDays
    }
}

const val OFFER_MESSAGE_MAX_LENGTH = 500

data class OfferFormState(
    val price: String = "",
    val message: String = "",
    val expiresInHours: Int = OfferExpiry.TwoDays.hours,
    val linkedListingId: String? = null,
    /**
     * Populated for hosts who have at least one published listing.
     * Stays empty for guests and for hosts with no listings — the
     * composer hides the entire link-a-listing row in that case so
     * users never see an empty picker.
     */
    val hostListings: List<HostListingSummary> = emptyList(),
    val submitting: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false,
)
