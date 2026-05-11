package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.google.gson.annotations.SerializedName
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
    @SerializedName("dateTime")
    val dateTime: String? = null,
    @SerializedName("coverImageUrl")
    val coverImageUrl: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    val image: String? = null,
    @SerializedName("authorName")
    val authorName: String? = null,
    @SerializedName("authorAvatar")
    val authorAvatar: String? = null,
    @SerializedName("offerCount")
    val offerCount: Int? = null,
)

data class LocationDto(
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
)

fun LocationDto.displayLine(): String =
    listOfNotNull(city, state, country)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

fun RequestDto.toDomain(): WantedRequest {
    val resolvedLocation = location?.displayLine()
        ?.takeIf { it.isNotBlank() }
        ?: locationString
        ?: city
        ?: ""
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
        imageUrl = coverImageUrl ?: imageUrl ?: image,
        authorName = authorName,
        authorAvatarUrl = authorAvatar,
        offerCount = offerCount ?: 0,
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
    val budget: Double? = null,
    val coverImageUrl: String? = null,
    val imageSource: String? = null,
    val tags: List<String>? = null,
)
