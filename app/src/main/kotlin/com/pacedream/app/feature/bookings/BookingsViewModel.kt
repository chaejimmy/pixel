package com.pacedream.app.feature.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

data class BookingListItem(
    val id: String,
    val title: String,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val status: String? = null
)

data class BookingsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val bookings: List<BookingListItem> = emptyList()
)

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.bookings.isEmpty(), isRefreshing = true, error = null) }
            val url = appConfig.buildApiUrl("bookings", "mine")

            when (val res = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val list = parseBookings(res.data)
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, bookings = list, error = null) }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = res.error.message) }
                }
            }
        }
    }

    private fun parseBookings(body: String): List<BookingListItem> {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]

            val arr = when (data) {
                is JsonArray -> data
                is JsonObject -> data["items"]?.asArrayOrNull()
                    ?: data["bookings"]?.asArrayOrNull()
                else -> null
            } ?: root["bookings"]?.asArrayOrNull()
                ?: root["items"]?.asArrayOrNull()
                ?: return emptyList()

            arr.mapNotNull { el ->
                val obj = el.asObjectOrNull() ?: return@mapNotNull null
                val id = obj.string("_id", "id", "bookingId") ?: return@mapNotNull null
                val listing = obj["listing"]?.asObjectOrNull()
                val title = listing?.string("name", "title") ?: obj.string("listingName", "title") ?: "Booking"

                BookingListItem(
                    id = id,
                    title = title,
                    date = obj.string("date"),
                    startTime = obj.string("startTime", "start_time", "startTimeISO"),
                    endTime = obj.string("endTime", "end_time", "endTimeISO"),
                    status = obj.string("status")
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse bookings")
            emptyList()
        }
    }
}

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray
private fun JsonObject.string(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } }

