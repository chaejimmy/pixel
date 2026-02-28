package com.pacedream.app.feature.checkout

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BookingDraft(
    val listingId: String,
    val listingType: String = "", // "time-based", "gear", "split-stay"
    val date: String, // yyyy-MM-dd
    val startTimeISO: String, // yyyy-MM-ddTHH:mm:ss
    val endTimeISO: String, // yyyy-MM-ddTHH:mm:ss
    val guests: Int,
    val totalAmountEstimate: Double? = null
)

object BookingDraftCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(draft: BookingDraft): String = json.encodeToString(BookingDraft.serializer(), draft)
    fun decode(raw: String): BookingDraft = json.decodeFromString(BookingDraft.serializer(), raw)
}

