package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.google.gson.annotations.SerializedName
import com.shourov.apps.pacedream.feature.wanted.model.ModerationStatus
import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest

// ============================================================================
// Response envelopes
//
// The backend at /v1/requests returns one of:
//   { status, data: {…} }      ← canonical
//   { ok,     data: {…} }
//   { request: {…} }           ← legacy fallback
//   { requests: […] }          ← legacy list fallback
//
// The fields below cover all three so we never crash on a slightly-different
// payload shape.
// ============================================================================

data class RequestsResponse(
    val status: Boolean = false,
    val ok: Boolean = false,
    val message: String? = null,
    val data: List<RequestDto>? = null,
    val results: List<RequestDto>? = null,
    val requests: List<RequestDto>? = null,
) {
    val all: List<RequestDto>
        get() = data ?: results ?: requests ?: emptyList()
}

data class RequestEnvelope(
    val status: Boolean = false,
    val ok: Boolean = false,
    val message: String? = null,
    val data: RequestDto? = null,
    val request: RequestDto? = null,
) {
    val payload: RequestDto?
        get() = data ?: request
}

// ============================================================================
// Wire model
//
// Mirrors the web `POST /v1/requests` response. Location may arrive as a
// JSON object (per the web schema) or as a plain string from older
// fixtures — we accept both.
// ============================================================================

data class RequestDto(
    @SerializedName("_id")
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    val location: LocationDto? = null,
    /** Legacy/string location used by some older feeds. */
    @SerializedName("locationString")
    val locationString: String? = null,
    val city: String? = null,
    val budget: Double? = null,
    val currency: String? = null,
    /** Canonical start of the requested window. */
    @SerializedName("requestStartDate")
    val requestStartDate: String? = null,
    /** Canonical end of the requested window. */
    @SerializedName("requestEndDate")
    val requestEndDate: String? = null,
    /**
     * Legacy field names — newer payloads use [requestStartDate] /
     * [requestEndDate], but historic responses may still ship `date` /
     * `dateTime` for the start, so we accept both.
     */
    val date: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null,
    @SerializedName("dateTime")
    val dateTime: String? = null,
    /**
     * Auto-expiration timestamp (ISO-8601). Server-driven; null means "no
     * auto-expiry" (the request stays Active until the requester closes it).
     */
    @SerializedName("expiresAt")
    val expiresAt: String? = null,
    /**
     * Lifecycle column added alongside expiration. Accepts the legacy
     * `requestStatus` name as well so the migration window doesn't break
     * older clients that haven't been redeployed.
     */
    @SerializedName("requestStatus")
    val requestStatus: String? = null,
    @SerializedName("lifecycleStatus")
    val lifecycleStatus: String? = null,
    @SerializedName("coverImageUrl")
    val coverImageUrl: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    val image: String? = null,
    @SerializedName("authorId")
    val authorId: String? = null,
    /** Some feeds nest the author under `author: { _id, name, ... }`. */
    val author: AuthorDto? = null,
    /** Fallback id-only field used by older payloads. */
    val userId: String? = null,
    @SerializedName("authorName")
    val authorName: String? = null,
    @SerializedName("authorAvatar")
    val authorAvatar: String? = null,
    @SerializedName("offerCount")
    val offerCount: Int? = null,
    /**
     * Moderation gate. The web platform writes this as `moderationStatus`
     * for new payloads and `status` for legacy ones; we accept either so
     * the migration window is non-blocking.
     */
    @SerializedName("moderationStatus")
    val moderationStatus: String? = null,
    /**
     * Legacy/alternate moderation column. Only consulted when
     * [moderationStatus] is missing — moderation values that overlap with
     * lifecycle state ("open", "matched") fall through to "approved" via
     * [ModerationStatus.fromKey].
     */
    val status: String? = null,
    @SerializedName("moderationReason")
    val moderationReason: String? = null,
    @SerializedName("rejectionReason")
    val rejectionReason: String? = null,
)

data class AuthorDto(
    @SerializedName("_id")
    val id: String? = null,
    val name: String? = null,
    val avatar: String? = null,
)

data class LocationDto(
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

/** "City, State" — the cardable label, with country omitted. */
fun LocationDto.cityRegionLine(): String =
    listOfNotNull(city, state)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

fun RequestDto.toDomain(): WantedRequest {
    val resolvedLocation = location?.cityRegionLine()
        ?.takeIf { it.isNotBlank() }
        ?: locationString
        ?: city
        ?: ""
    val resolvedAuthorId = authorId?.takeIf { it.isNotBlank() }
        ?: author?.id?.takeIf { it.isNotBlank() }
        ?: userId?.takeIf { it.isNotBlank() }
    val resolvedStart = requestStartDate ?: date ?: dateTime
    val resolvedEnd = requestEndDate ?: endDate
    // The legacy `status` column has been overloaded for several different
    // meanings over time. We only honour it as a lifecycle value when it
    // matches one of our known keys — otherwise it falls through to the
    // default Active so old "open"/"matched" rows don't get hidden.
    val resolvedStatus = RequestStatus.fromKey(
        requestStatus
            ?: lifecycleStatus
            ?: status?.takeIf { it.matchesLifecycleKey() }
    )
    return WantedRequest(
        id = id.orEmpty(),
        title = title.orEmpty(),
        description = description.orEmpty(),
        type = type ?: category ?: "Request",
        category = category ?: type ?: "Other",
        location = resolvedLocation,
        budget = budget,
        budgetCurrency = currency ?: "USD",
        requestStartDate = resolvedStart,
        requestEndDate = resolvedEnd,
        expiresAt = expiresAt,
        imageUrl = coverImageUrl ?: imageUrl ?: image,
        authorId = resolvedAuthorId,
        authorName = authorName ?: author?.name,
        authorAvatarUrl = authorAvatar ?: author?.avatar,
        offerCount = offerCount ?: 0,
        moderationStatus = ModerationStatus.fromKey(moderationStatus ?: status),
        moderationReason = (moderationReason ?: rejectionReason)
            ?.trim()
            ?.takeIf { it.isNotBlank() },
        status = resolvedStatus,
    )
}

private fun String.matchesLifecycleKey(): Boolean =
    RequestStatus.entries.any { it.key.equals(this, ignoreCase = true) }

// ============================================================================
// Create-request body
//
// Web parity: POST /v1/requests expects
//   { type, category, title, description, location: {…}, date, budget,
//     coverImageUrl, imageSource, tags, expiresAt }
// `null` fields are dropped server-side; Gson serializes them as `null`,
// which the backend tolerates.
// ============================================================================

data class CreateRequestBody(
    val type: String,
    val category: String,
    val title: String,
    val description: String,
    val location: LocationDto? = null,
    val date: String? = null,
    val endDate: String? = null,
    /**
     * Optional explicit auto-expiry (`yyyy-MM-dd`). Omitted when the user
     * doesn't pick a date range — the server falls back to its own default
     * (currently 30 days after creation, but the client doesn't depend on
     * that number).
     */
    val expiresAt: String? = null,
    val budget: Double? = null,
    val coverImageUrl: String? = null,
    val imageSource: String? = null,
    val tags: List<String>? = null,
)

// ============================================================================
// Status update body
//
// PATCH /v1/requests/{id}/status accepts a single `status` field. Used by
// the Mark as Fulfilled / Cancel / Renew actions.
// ============================================================================

data class UpdateRequestStatusBody(
    val status: String,
    /**
     * Optional new expiration (used by the Renew action — omitted by
     * Cancel/Fulfill). When the server receives this it rolls
     * [WantedRequest.expiresAt] forward and flips status back to Active.
     */
    val expiresAt: String? = null,
)
