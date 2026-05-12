package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.google.gson.annotations.SerializedName
import com.shourov.apps.pacedream.feature.wanted.model.OfferStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer

data class OfferEnvelope(
    val status: Boolean = false,
    val ok: Boolean = false,
    val message: String? = null,
    val data: OfferDto? = null,
    val offer: OfferDto? = null,
) {
    val payload: OfferDto?
        get() = data ?: offer
}

/**
 * Envelope returned by `GET /v1/offers?mine=true` and
 * `GET /v1/requests/{id}/offers`. Same shape-tolerance as the requests
 * list — multiple keys are accepted to survive backend drift.
 */
data class OffersResponse(
    val status: Boolean = false,
    val ok: Boolean = false,
    val message: String? = null,
    val data: List<OfferDto>? = null,
    val results: List<OfferDto>? = null,
    val offers: List<OfferDto>? = null,
) {
    val all: List<OfferDto>
        get() = data ?: results ?: offers ?: emptyList()
}

data class OfferDto(
    @SerializedName("_id")
    val id: String? = null,
    @SerializedName("requestId")
    val requestId: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val message: String? = null,
    @SerializedName("authorName")
    val authorName: String? = null,
    @SerializedName("authorAvatar")
    val authorAvatarUrl: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    val status: String? = null,
    /**
     * Optional snapshot of the parent request when the backend embeds it
     * (e.g. `GET /offers?mine=true` returns each offer with the request
     * title/category so we don't have to fan out to per-request fetches).
     */
    val request: RequestDto? = null,
    @SerializedName("requestTitle")
    val requestTitle: String? = null,
)

fun OfferDto.toDomain(fallbackRequestId: String): WantedOffer = WantedOffer(
    id = id.orEmpty(),
    requestId = requestId ?: request?.id ?: fallbackRequestId,
    price = price ?: 0.0,
    currency = currency ?: "USD",
    message = message.orEmpty(),
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    createdAt = createdAt,
    status = OfferStatus.fromKey(status),
    requestTitle = requestTitle ?: request?.title,
)

data class CreateOfferBody(
    val price: Double,
    val message: String,
)
