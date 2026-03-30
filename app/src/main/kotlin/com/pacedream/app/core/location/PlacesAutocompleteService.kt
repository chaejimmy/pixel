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

    suspend fun getAddressAutocompletePredictions(
        query: String
    ): List<PlacePrediction> = getAutocompletePredictions(query, types = "address")
}
