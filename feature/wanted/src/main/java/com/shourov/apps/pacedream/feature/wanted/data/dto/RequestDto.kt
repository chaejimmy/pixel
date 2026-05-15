package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.google.gson.annotations.SerializedName
import com.shourov.apps.pacedream.feature.wanted.model.ModerationStatus
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
    val date: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null,
    @SerializedName("dateTime")
    val dateTime: String? = null,
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
    return WantedRequest(
        id = id.orEmpty(),
        title = title.orEmpty(),
        description = description.orEmpty(),
        type = type ?: category ?: "Request",
        category = category ?: type ?: "Other",
        location = resolvedLocation,
        budget = budget,
        budgetCurrency = currency ?: "USD",
        dateTime = date ?: dateTime,
        endDate = endDate,
        imageUrl = coverImageUrl ?: imageUrl ?: image,
        authorId = resolvedAuthorId,
        authorName = authorName ?: author?.name,
        authorAvatarUrl = authorAvatar ?: author?.avatar,
        offerCount = offerCount ?: 0,
        moderationStatus = ModerationStatus.fromKey(moderationStatus ?: status),
        moderationReason = (moderationReason ?: rejectionReason)
            ?.trim()
            ?.takeIf { it.isNotBlank() },
    )
}

// ============================================================================
// Create-request body
//
// Web parity: POST /v1/requests expects
//   { type, category, title, description, location: {…}, date, budget,
//     coverImageUrl, imageSource, tags }
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
    val budget: Double? = null,
    val coverImageUrl: String? = null,
    val imageSource: String? = null,
    val tags: List<String>? = null,
)
