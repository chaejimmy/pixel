package com.pacedream.app.core.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class PlacePrediction(
    val placeId: String,
    val description: String,
    val mainText: String,
    val secondaryText: String
)

/**
 * Place details returned by the Place Details API, including lat/lng
 * and parsed address components.
 */
data class PlaceDetails(
    val lat: Double,
    val lng: Double,
    val formattedAddress: String,
    val city: String,
    val state: String,
    val country: String,
)

@Singleton
class PlacesAutocompleteService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apiKey: String by lazy {
        context.getString(
            context.resources.getIdentifier("google_maps_key", "string", context.packageName)
        )
    }

    suspend fun getAutocompletePredictions(
        query: String,
        types: String = "(cities)"
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
        if (query.length < 2 || apiKey.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val typesParam = URLEncoder.encode(types, "UTF-8")
            val url = URL(
                "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                    "?input=$encoded&types=$typesParam&key=$apiKey"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            if (json.optString("status") != "OK") {
                Timber.w("Places Autocomplete status: ${json.optString("status")}")
                return@withContext emptyList()
            }

            val predictions = json.getJSONArray("predictions")
            (0 until predictions.length()).map { i ->
                val p = predictions.getJSONObject(i)
                val structured = p.optJSONObject("structured_formatting")
                PlacePrediction(
                    placeId = p.optString("place_id", ""),
                    description = p.optString("description", ""),
                    mainText = structured?.optString("main_text", "") ?: "",
                    secondaryText = structured?.optString("secondary_text", "") ?: ""
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Places Autocomplete failed")
            emptyList()
        }
    }

    /**
     * Address autocomplete using the `address` type for street-level results.
     * Falls back to `geocode` type if no results found, giving users
     * suggestions as soon as they start typing.
     */
    suspend fun getAddressAutocompletePredictions(
        query: String
    ): List<PlacePrediction> {
        // Try address type first for precise street-level results
        val addressResults = getAutocompletePredictions(query, types = "address")
        if (addressResults.isNotEmpty()) return addressResults
        // Fall back to geocode type for broader results (cities, neighborhoods)
        return getAutocompletePredictions(query, types = "geocode")
    }

    /**
     * Fetch place details (lat/lng and address components) for a selected prediction.
     * Uses the Google Place Details API with the placeId from autocomplete.
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails? = withContext(Dispatchers.IO) {
        if (placeId.isBlank() || apiKey.isBlank()) return@withContext null

        try {
            val url = URL(
                "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=${URLEncoder.encode(placeId, "UTF-8")}" +
                    "&fields=geometry,formatted_address,address_components" +
                    "&key=$apiKey"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            if (json.optString("status") != "OK") {
                Timber.w("Place Details status: ${json.optString("status")}")
                return@withContext null
            }

            val result = json.getJSONObject("result")
            val location = result.getJSONObject("geometry").getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            val formattedAddress = result.optString("formatted_address", "")

            // Parse address components for city, state, country
            var city = ""
            var state = ""
            var country = ""
            val components = result.optJSONArray("address_components")
            if (components != null) {
                for (i in 0 until components.length()) {
                    val comp = components.getJSONObject(i)
                    val types = comp.getJSONArray("types")
                    val typeList = (0 until types.length()).map { types.getString(it) }
                    when {
                        "locality" in typeList -> city = comp.optString("long_name", "")
                        "sublocality_level_1" in typeList && city.isEmpty() ->
                            city = comp.optString("long_name", "")
                        "administrative_area_level_1" in typeList ->
                            state = comp.optString("short_name", "")
                        "country" in typeList -> country = comp.optString("short_name", "")
                    }
                }
            }

            PlaceDetails(
                lat = lat,
                lng = lng,
                formattedAddress = formattedAddress,
                city = city,
                state = state,
                country = country,
            )
        } catch (e: Exception) {
            Timber.e(e, "Place Details fetch failed for placeId=$placeId")
            null
        }
    }
}
