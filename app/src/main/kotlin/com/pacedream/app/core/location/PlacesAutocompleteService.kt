package com.pacedream.app.core.location

import android.content.Context
import com.shourov.apps.pacedream.core.network.BuildConfig
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
        try {
            context.getString(
                context.resources.getIdentifier("google_maps_key", "string", context.packageName)
            )
        } catch (e: Exception) {
            Timber.w("google_maps_key string resource not found")
            ""
        }
    }

    /**
     * Backend base URL for the Maps proxy (public, no auth required).
     * Uses the same SERVICE_URL as the rest of the API.
     */
    private val backendMapsBaseUrl: String by lazy {
        val serviceUrl = BuildConfig.SERVICE_URL.trimEnd('/')
        "$serviceUrl/maps"
    }

    /**
     * Primary autocomplete: tries the backend proxy first (reliable, server-side API key),
     * then falls back to direct Google Places API (client-side key, may not work).
     */
    suspend fun getAutocompletePredictions(
        query: String,
        types: String = "(cities)"
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()

        // Try backend proxy first (more reliable — server has its own API key)
        val backendResults = getAutocompleteFromBackend(query, types)
        if (backendResults.isNotEmpty()) return@withContext backendResults

        // Fallback: direct Google Places API
        if (apiKey.isBlank()) return@withContext emptyList()
        getAutocompleteFromGoogle(query, types)
    }

    /**
     * Address autocomplete with multi-layer fallback:
     * 1. Backend proxy (server-side Google API key)
     * 2. Direct Google Places API (client-side key)
     * 3. Android device Geocoder (always works, no API key needed)
     */
    suspend fun getAddressAutocompletePredictions(
        query: String
    ): List<PlacePrediction> {
        if (query.length < 3) return emptyList()

        // Try backend proxy
        val backendResults = getAutocompleteFromBackend(query, "address")
        if (backendResults.isNotEmpty()) return backendResults
        val backendGeo = getAutocompleteFromBackend(query, "geocode")
        if (backendGeo.isNotEmpty()) return backendGeo

        // Try direct Google Places API
        if (apiKey.isNotBlank()) {
            val googleResults = getAutocompleteFromGoogle(query, "address")
            if (googleResults.isNotEmpty()) return googleResults
            val googleGeo = getAutocompleteFromGoogle(query, "geocode")
            if (googleGeo.isNotEmpty()) return googleGeo
        }

        // Fallback: Android device Geocoder (always works, no API key needed)
        return getAddressFromDeviceGeocoder(query)
    }

    /**
     * Use Android's built-in Geocoder to find addresses.
     * Works without any API key — uses the device's location services.
     */
    private suspend fun getAddressFromDeviceGeocoder(
        query: String
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 5) ?: return@withContext emptyList()
            addresses.mapNotNull { addr ->
                val lines = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }
                val fullAddress = lines.joinToString(", ")
                if (fullAddress.isBlank()) return@mapNotNull null
                val mainText = addr.getAddressLine(0) ?: fullAddress
                val city = addr.locality ?: addr.subAdminArea ?: ""
                val state = addr.adminArea ?: ""
                val secondary = listOfNotNull(
                    city.takeIf { it.isNotBlank() },
                    state.takeIf { it.isNotBlank() },
                    addr.countryName?.takeIf { it.isNotBlank() }
                ).joinToString(", ")
                PlacePrediction(
                    placeId = "", // Device geocoder doesn't have placeIds
                    description = fullAddress,
                    mainText = mainText,
                    secondaryText = secondary
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Device Geocoder failed for query: $query")
            emptyList()
        }
    }

    /**
     * Fetch autocomplete predictions from the backend proxy.
     * Endpoint: GET /v1/maps/autocomplete?input=...&types=...
     * This is public (no auth required) and uses the server's Google Maps API key.
     */
    private suspend fun getAutocompleteFromBackend(
        query: String,
        types: String
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val typesParam = URLEncoder.encode(types, "UTF-8")
            val url = URL("$backendMapsBaseUrl/autocomplete?input=$encoded&types=$typesParam")

            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/json")

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Timber.w("Backend autocomplete returned HTTP $responseCode")
                conn.disconnect()
                return@withContext emptyList()
            }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                Timber.w("Backend autocomplete error: ${json.optString("error")}")
                return@withContext emptyList()
            }

            val data = json.optJSONArray("data") ?: return@withContext emptyList()
            (0 until data.length()).map { i ->
                val p = data.getJSONObject(i)
                PlacePrediction(
                    placeId = p.optString("placeId", ""),
                    description = p.optString("description", ""),
                    mainText = p.optString("mainText", ""),
                    secondaryText = p.optString("secondaryText", "")
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Backend autocomplete failed")
            emptyList()
        }
    }

    /**
     * Fallback: direct Google Places Autocomplete API call.
     */
    private suspend fun getAutocompleteFromGoogle(
        query: String,
        types: String
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
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
                Timber.w("Google Places Autocomplete status: ${json.optString("status")}")
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
            Timber.e(e, "Google Places Autocomplete failed")
            emptyList()
        }
    }

    /**
     * Fetch place details (lat/lng and address components) for a selected prediction.
     * Tries backend proxy first, then direct Google API, then device geocoder.
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails? = withContext(Dispatchers.IO) {
        if (placeId.isBlank()) return@withContext null // Device geocoder results have no placeId

        // Try backend proxy first
        val backendResult = getPlaceDetailsFromBackend(placeId)
        if (backendResult != null) return@withContext backendResult

        // Fallback: direct Google Places API
        if (apiKey.isBlank()) return@withContext null
        getPlaceDetailsFromGoogle(placeId)
    }

    /**
     * Fetch place details from the backend proxy.
     * Endpoint: GET /v1/maps/place/{placeId}
     */
    private suspend fun getPlaceDetailsFromBackend(placeId: String): PlaceDetails? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "$backendMapsBaseUrl/place/${URLEncoder.encode(placeId, "UTF-8")}" +
                        "?fields=geometry,formatted_address,address_components"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Accept", "application/json")

                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    conn.disconnect()
                    return@withContext null
                }

                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(response)
                if (!json.optBoolean("success", false)) return@withContext null

                val data = json.optJSONObject("data") ?: return@withContext null
                val geometry = data.optJSONObject("geometry")
                val location = geometry?.optJSONObject("location")

                val lat = location?.optDouble("lat", 0.0) ?: data.optDouble("lat", 0.0)
                val lng = location?.optDouble("lng", 0.0) ?: data.optDouble("lng", 0.0)
                val formattedAddress = data.optString("formattedAddress", data.optString("formatted_address", ""))

                // Parse address components
                var city = ""
                var state = ""
                var country = ""
                val components = data.optJSONObject("components")
                if (components != null) {
                    city = components.optString("city", "")
                    state = components.optString("state", "")
                    country = components.optString("country", "")
                }

                if (lat == 0.0 && lng == 0.0) return@withContext null

                PlaceDetails(lat = lat, lng = lng, formattedAddress = formattedAddress,
                    city = city, state = state, country = country)
            } catch (e: Exception) {
                Timber.e(e, "Backend place details failed for placeId=$placeId")
                null
            }
        }

    /**
     * Fallback: fetch place details from direct Google Places API.
     */
    private suspend fun getPlaceDetailsFromGoogle(placeId: String): PlaceDetails? =
        withContext(Dispatchers.IO) {
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

                PlaceDetails(lat = lat, lng = lng, formattedAddress = formattedAddress,
                    city = city, state = state, country = country)
            } catch (e: Exception) {
                Timber.e(e, "Google Place Details failed for placeId=$placeId")
                null
            }
        }
}
