package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.google.gson.annotations.SerializedName
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
    @SerializedName("createdAt")
    val createdAt: String? = null,
)

fun OfferDto.toDomain(fallbackRequestId: String): WantedOffer = WantedOffer(
    id = id.orEmpty(),
    requestId = requestId ?: fallbackRequestId,
    price = price ?: 0.0,
    currency = currency ?: "USD",
    message = message.orEmpty(),
    authorName = authorName,
    createdAt = createdAt,
)

data class CreateOfferBody(
    val price: Double,
    val message: String,
)
