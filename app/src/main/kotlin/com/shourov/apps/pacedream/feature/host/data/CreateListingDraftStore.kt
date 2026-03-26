package com.shourov.apps.pacedream.feature.host.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * iOS parity: CreateListingDraftStore.
 *
 * Persists in-progress listing drafts to SharedPreferences (iOS: UserDefaults)
 * so the user can resume after leaving and coming back.
 *
 * The draft is keyed per-user (or "anon" for unauthenticated sessions).
 */
class CreateListingDraftStore(
    context: Context,
    private val userId: String? = null,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val key: String
        get() = "pd_create_listing_draft_v2_${userId ?: "anon"}"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun save(draft: ListingDraftData) {
        try {
            val encoded = json.encodeToString(draft)
            prefs.edit().putString(key, encoded).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to save listing draft (best-effort)")
        }
    }

    fun load(): ListingDraftData? {
        return try {
            val raw = prefs.getString(key, null) ?: return null
            json.decodeFromString<ListingDraftData>(raw)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load listing draft, returning null")
            null
        }
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }

    companion object {
        private const val PREFS_NAME = "pd_listing_draft_prefs"
    }
}

/**
 * Serializable snapshot of in-progress listing form data.
 * Matches the fields in CreateListingScreen's wizard state.
 */
@Serializable
data class ListingDraftData(
    val listingMode: String = "share",
    val resourceKind: String = "spaces",
    val subCategory: String = "",
    val title: String = "",
    val description: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val basePrice: String = "",
    val totalCost: String = "",
    val pricingUnit: String = "hour",
    val amenities: List<String> = emptyList(),
    val deadlineAt: String = "",
    val requirements: String = "",
)
