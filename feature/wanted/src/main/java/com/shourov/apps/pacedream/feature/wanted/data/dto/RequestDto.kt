package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.google.gson.annotations.SerializedName
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest

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

data class RequestDto(
    @SerializedName("_id")
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    val location: String? = null,
    val city: String? = null,
    val budget: Double? = null,
    val currency: String? = null,
    @SerializedName("dateTime")
    val dateTime: String? = null,
    val date: String? = null,
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

fun RequestDto.toDomain(): WantedRequest = WantedRequest(
    id = id.orEmpty(),
    title = title.orEmpty(),
    description = description.orEmpty(),
    type = type ?: category ?: "Request",
    category = category ?: type ?: "Other",
    location = location ?: city ?: "",
    budget = budget,
    budgetCurrency = currency ?: "USD",
    dateTime = dateTime ?: date,
    imageUrl = imageUrl ?: image,
    authorName = authorName,
    authorAvatarUrl = authorAvatar,
    offerCount = offerCount ?: 0,
)

data class CreateRequestBody(
    val type: String,
    val title: String,
    val description: String,
    val location: String,
    val dateTime: String?,
    val budget: Double?,
    val imageUrl: String?,
)
