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
 * The draft is keyed per-user. An unscoped "anon" draft exists only for the
 * rare path where the wizard is opened before the SessionManager emits the
 * current user; it is cleared whenever a real user id is supplied so that it
 * cannot leak to the next signed-in user on a shared device.
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

    init {
        // As soon as we have a real user id, nuke any orphaned anon draft from
        // a previous (possibly different) signed-out session so that the next
        // user does not see someone else's work when the "Continue draft"
        // banner loads.
        if (!userId.isNullOrBlank()) {
            try {
                prefs.edit().remove(ANON_KEY).apply()
            } catch (e: Exception) {
                Timber.w(e, "Failed to clear anon draft (best-effort)")
            }
        }
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

    /** Called when a user signs out to guarantee no draft bleeds into the next session. */
    fun clearForSignOut() {
        try {
            prefs.edit().remove(key).remove(ANON_KEY).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to clear drafts on sign-out (best-effort)")
        }
    }

    companion object {
        private const val PREFS_NAME = "pd_listing_draft_prefs"
        private const val ANON_KEY = "pd_create_listing_draft_v2_anon"
    }
}

/**
 * Serializable snapshot of in-progress listing form data.
 * Matches the fields in CreateListingScreen's wizard state. All new fields
 * default to their wizard defaults so older drafts still decode cleanly.
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
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val basePrice: String = "",
    val totalCost: String = "",
    val pricingUnit: String = "hour",
    val amenities: List<String> = emptyList(),
    val deadlineAt: String = "",
    val requirements: String = "",
    /** Cloudinary URLs (or data URLs) for photos the host already uploaded in this draft. */
    val uploadedImageUrls: List<String> = emptyList(),
    // Schedule / availability
    val selectedDurations: List<Int> = emptyList(),
    val selectedDays: List<Int> = emptyList(),
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val timezone: String = "",
    val minStay: Int = 1,
    val maxStay: Int = 7,
    val checkinTime: String = "15:00",
    val checkoutTime: String = "11:00",
    val minMonths: Int = 1,
    val availableFrom: String = "",
    // Capacity — only meaningful for accommodation-style space listings.
    // Defaults match EditListingUiState bounds (max guests 1, rooms 0)
    // so older drafts decode cleanly and the backend receives sensible
    // values even when the wizard never surfaces the field.
    val maxGuests: Int = 1,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    // Parking-specific — only meaningful for parking / EV parking schemas.
    val vehicleCapacity: Int = 1,
    val parkingCovered: Boolean = false,
    val parkingEvCharging: Boolean = false,
    val parkingAccess247: Boolean = false,
    val parkingSizeLimit: String = "",
    val parkingSecurityFeatures: List<String> = emptyList(),
    // Item-rental specific — gear / camera / tech / etc.
    val deposit: String = "",
    val condition: String = "",
    val pickupDeliveryOptions: List<String> = emptyList(),
    // Service specific — session duration in minutes.
    val serviceDurationMinutes: Int = 60,
)
