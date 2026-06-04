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
     * Start of the requested window (ISO-8601 `yyyy-MM-dd` for new payloads).
     * Legacy records may carry free-text strings like "Sat 10:00 AM" — both
     * are rendered via
     * [com.shourov.apps.pacedream.feature.wanted.presentation.util.RequestDateFormatter].
     */
    val requestStartDate: String? = null,
    /** End of the requested window (ISO-8601 `yyyy-MM-dd`). */
    val requestEndDate: String? = null,
    /**
     * Explicit expiration timestamp (ISO-8601). When the server omits this,
     * the effective expiry is derived from [requestEndDate] (or
     * [requestStartDate] if there is no range). A null expiry means the
     * request never auto-expires — only the requester can close it.
     */
    val expiresAt: String? = null,
    val imageUrl: String?,
    val authorId: String? = null,
    val authorName: String? = null,
    val authorAvatarUrl: String? = null,
    val offerCount: Int = 0,
    /**
     * Web-parity moderation gate. New requests come back from
     * `POST /v1/requests` as [ModerationStatus.PendingReview]; the browse
     * feed must only surface [ModerationStatus.Approved]. Legacy feeds
     * that don't carry the field decode to [ModerationStatus.Approved]
     * so historic data stays visible.
     */
    val moderationStatus: ModerationStatus = ModerationStatus.Approved,
    /** Reviewer note shown when [moderationStatus] is [ModerationStatus.Rejected]. */
    val moderationReason: String? = null,
    /**
     * Lifecycle status. Defaults to [RequestStatus.Active] when the server
     * omits the column so legacy payloads keep behaving like active
     * requests. Time-based expiry is computed via
     * [com.shourov.apps.pacedream.feature.wanted.presentation.util.RequestExpiryResolver]
     * when [status] is [RequestStatus.Active] but [expiresAt] has passed.
     */
    val status: RequestStatus = RequestStatus.Active,
)

/**
 * Lifecycle of the moderation review applied to a [WantedRequest].
 *
 * Web parity: the platform marks a freshly-posted request as
 * `pending_review`, surfaces it on the requester's own list only, and
 * promotes it to `approved` once a moderator (or the auto-pipeline)
 * clears it. Rejected requests stay invisible to the public feed and
 * receive a reviewer note for the requester.
 *
 * Unknown / missing values from the server map to [Approved] so we
 * never accidentally hide legacy records that pre-date the moderation
 * column.
 */
enum class ModerationStatus(val key: String, val label: String) {
    PendingReview("pending_review", "Pending review"),
    Approved("approved", "Approved"),
    Rejected("rejected", "Rejected"),
    ;

    /** True when the request can appear on the public browse feed. */
    val isPublic: Boolean get() = this == Approved

    companion object {
        fun fromKey(key: String?): ModerationStatus {
            if (key.isNullOrBlank()) return Approved
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
                ?: Approved
        }
    }
}

/**
 * Lifecycle of a request beyond moderation.
 *
 *  - [Active]: open for offers. The public feed only ever shows active
 *    (and approved) requests.
 *  - [Expired]: the window passed — auto-closed by the server based on
 *    [WantedRequest.expiresAt], or computed client-side as a fallback.
 *  - [Fulfilled]: the requester accepted an offer and confirmed the
 *    request is satisfied.
 *  - [Cancelled]: the requester closed the request before it expired.
 *
 * Unknown values from the server fall through to [Active] so legacy
 * records that pre-date the column keep showing up on the feed.
 */
enum class RequestStatus(val key: String, val label: String) {
    Active("active", "Active"),
    Expired("expired", "Expired"),
    Fulfilled("fulfilled", "Fulfilled"),
    Cancelled("cancelled", "Cancelled"),
    ;

    /** True when the request is currently open for offers. */
    val isOpenForOffers: Boolean get() = this == Active

    /**
     * Whether the request is "closed" (no longer surfaceable on the public
     * feed). Both expired and explicitly-closed (fulfilled/cancelled) states
     * count.
     */
    val isClosed: Boolean get() = this != Active

    companion object {
        fun fromKey(key: String?): RequestStatus {
            if (key.isNullOrBlank()) return Active
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
                ?: Active
        }
    }
}

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
 * Inner tabs of the "Mine" screen — splits the user's posted requests by
 * lifecycle so the expired/closed history doesn't clutter the active list
 * but is still reachable.
 */
enum class MyRequestsTab(val key: String, val label: String, val status: RequestStatus) {
    Active("active", "Active", RequestStatus.Active),
    Expired("expired", "Expired", RequestStatus.Expired),
    Fulfilled("fulfilled", "Fulfilled", RequestStatus.Fulfilled),
    Cancelled("cancelled", "Cancelled", RequestStatus.Cancelled),
    ;

    companion object {
        fun fromKey(key: String?): MyRequestsTab =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Active
    }
}

/**
 * Combined Mine-tab state: the full list of the requester's posts plus
 * which inner tab they're viewing. The current request slice ([visible])
 * is derived in the ViewModel so tab switches don't re-filter on the UI
 * thread mid-recomposition.
 */
sealed interface MyRequestsUiState {
    data object Loading : MyRequestsUiState
    data class Error(val message: String) : MyRequestsUiState
    data class Content(
        val selectedTab: MyRequestsTab,
        /** Per-tab visible slice. Empty list means "no requests in this tab". */
        val visible: List<WantedRequest>,
        /** Per-tab counts, used to badge the tab labels. */
        val counts: Map<MyRequestsTab, Int>,
        /** Per-request action in flight (for spinner / disabled state). */
        val pendingActionId: String? = null,
        /** Inline action error, displayed at the top of the screen. */
        val actionError: String? = null,
    ) : MyRequestsUiState
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

    /**
     * A pick is "verified" when we have either a labelled component (city
     * is the cheapest sentinel) or a real coordinate pair. The browse feed
     * filters on labelled locations, but the requester is still allowed to
     * pin the request with coordinates only — the moderator will see them.
     */
    val hasVerifiedSelection: Boolean
        get() = city.isNotBlank() ||
            region.isNotBlank() ||
            country.isNotBlank() ||
            (lat != null && lng != null)
}

/** Null-safe alias of [SelectedPlace.hasVerifiedSelection] for nullable form state. */
val SelectedPlace?.hasVerifiedSelection: Boolean
    get() = this != null && this.hasVerifiedSelection

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
    /**
     * Explicit auto-expiry chosen by the user — null means "infer from the
     * end of the requested window" (handled by the ViewModel on submit).
     */
    val expiresAt: Long? = null,
    val budget: String = "",
    val imageUrl: String? = null,
)

/**
 * Identifies a single field on the Post-a-Request form. Used to drive
 * focus-to-first-error after a failed submit — the screen maps each value
 * to a [androidx.compose.ui.focus.FocusRequester] / scroll target so the
 * user lands on the exact problem instead of the bottom of the form.
 */
enum class CreateRequestField { Title, Description, Category, Budget, Location }

data class FieldErrors(
    val titleError: String? = null,
    val descriptionError: String? = null,
    /**
     * Category is required and must belong to the selected type's list. In
     * practice the dropdown always carries a default selection, so this is
     * only ever populated by the submit-time guard.
     */
    val categoryError: String? = null,
    val budgetError: String? = null,
    /**
     * Populated only after the user attempts to submit without picking a
     * structured location. We don't shout at the field on every keystroke
     * because the user may not have reached it yet — the submit button
     * stays disabled via [CreateRequestUiState.requiredFieldsPresent].
     */
    val locationError: String? = null,
) {
    fun isEmpty(): Boolean =
        titleError == null &&
            descriptionError == null &&
            categoryError == null &&
            budgetError == null &&
            locationError == null

    /**
     * The first field (in visual top-to-bottom order) that carries an
     * error, or null when the form is clean. Drives focus-to-first-error so
     * the user is taken to the highest problem on the page.
     */
    fun firstInvalidField(): CreateRequestField? = when {
        titleError != null -> CreateRequestField.Title
        descriptionError != null -> CreateRequestField.Description
        categoryError != null -> CreateRequestField.Category
        budgetError != null -> CreateRequestField.Budget
        locationError != null -> CreateRequestField.Location
        else -> null
    }
}

data class CreateRequestUiState(
    val form: CreateRequestForm = CreateRequestForm(),
    val fieldErrors: FieldErrors = FieldErrors(),
    val submitting: Boolean = false,
    val uploading: Boolean = false,
    val error: String? = null,
    /**
     * One-shot signal set by [com.shourov.apps.pacedream.feature.wanted.presentation.CreateRequestViewModel.submit]
     * when a submit attempt fails validation: the screen focuses / scrolls to
     * this field, then calls `consumeFocusTarget()` to clear it so a later
     * recomposition doesn't steal focus again.
     */
    val focusTarget: CreateRequestField? = null,
    val createdId: String? = null,
    /**
     * Moderation state echoed by the server on successful submit. New
     * requests come back as [ModerationStatus.PendingReview] — we mirror
     * that into the success screen so the requester sees "Submitted for
     * review" rather than "Live now". Defaults to [ModerationStatus.PendingReview]
     * so the success screen's review copy is the safe default if the
     * server omits the field.
     */
    val createdModerationStatus: ModerationStatus = ModerationStatus.PendingReview,
    /**
     * Per-type category options shown in the dropdown. Defaults to the
     * hardcoded [WantedCategoriesByType] so the form is never blank on
     * first launch or while offline; replaced with server values when
     * `GET /v1/requests/categories` resolves.
     */
    val categoriesByType: Map<WantedType, List<WantedCategoryOption>> = WantedCategoriesByType,
) {
    /**
     * Required fields are tracked separately from [fieldErrors] so that the
     * submit button can stay disabled for an empty form without showing a
     * red border under every field the user hasn't reached yet.
     *
     * A picked location is treated as present when the autocomplete sheet
     * gave us a labelled place (city) or — for the "Use current location"
     * fallback that may skip the geocoder — at least a coordinate pair.
     */
    val requiredFieldsPresent: Boolean
        get() = form.title.trim().length >= TITLE_MIN_LENGTH &&
            form.description.trim().length >= DESCRIPTION_MIN_LENGTH &&
            form.category.isNotBlank() &&
            form.location.hasVerifiedSelection

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
