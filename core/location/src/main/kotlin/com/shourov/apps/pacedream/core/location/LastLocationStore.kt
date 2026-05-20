package com.shourov.apps.pacedream.core.location

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's most recently selected search location (current
 * location or autocomplete pick) so the next session can pre-populate
 * the WHERE field instead of opening to "Anywhere".
 *
 * Backed by plain [SharedPreferences] — the payload is just a display
 * label + lat/lng + address components, none of which are sensitive.
 */
@Singleton
class LastLocationStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(location: SavedLocation) {
        prefs.edit().apply {
            putString(KEY_LABEL, location.label)
            putString(KEY_CITY, location.city)
            putString(KEY_REGION, location.region)
            putString(KEY_COUNTRY, location.country)
            if (location.lat != null) {
                putString(KEY_LAT, location.lat.toString())
            } else {
                remove(KEY_LAT)
            }
            if (location.lng != null) {
                putString(KEY_LNG, location.lng.toString())
            } else {
                remove(KEY_LNG)
            }
            putLong(KEY_SAVED_AT, System.currentTimeMillis())
        }.apply()
    }

    fun load(): SavedLocation? {
        val label = prefs.getString(KEY_LABEL, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return SavedLocation(
            label = label,
            city = prefs.getString(KEY_CITY, "").orEmpty(),
            region = prefs.getString(KEY_REGION, "").orEmpty(),
            country = prefs.getString(KEY_COUNTRY, "").orEmpty(),
            lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull(),
            lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull(),
            savedAtMillis = prefs.getLong(KEY_SAVED_AT, 0L).takeIf { it > 0L },
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "pacedream_last_location"
        private const val KEY_LABEL = "label"
        private const val KEY_CITY = "city"
        private const val KEY_REGION = "region"
        private const val KEY_COUNTRY = "country"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_SAVED_AT = "saved_at"
    }
}

/**
 * Snapshot of a previously chosen search location.  Coordinates are
 * nullable because some picks (device-Geocoder fallback predictions)
 * only carry an address label without lat/lng.
 */
data class SavedLocation(
    val label: String,
    val city: String,
    val region: String,
    val country: String,
    val lat: Double?,
    val lng: Double?,
    val savedAtMillis: Long? = null,
)
