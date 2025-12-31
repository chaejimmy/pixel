package com.pacedream.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject

data class CheckoutUiState(
    val draft: BookingDraft? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    sealed class Effect {
        data class NavigateToConfirmation(val bookingId: String) : Effect()
    }

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun setDraft(draft: BookingDraft) {
        _uiState.update { it.copy(draft = draft, errorMessage = null) }
    }

    fun submitBooking() {
        val draft = _uiState.value.draft ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val body = buildBookingBody(draft).toString()

            val primaryUrl = appConfig.buildApiUrl("bookings")
            val primary = apiClient.post(primaryUrl, body, includeAuth = true)

            val finalResult = when (primary) {
                is ApiResult.Success -> primary
                is ApiResult.Failure -> {
                    if (primary.error is ApiError.NotFound) {
                        val legacyUrl = appConfig.buildApiUrl("bookings", "rooms", "add")
                        apiClient.post(legacyUrl, body, includeAuth = true)
                    } else primary
                }
            }

            when (finalResult) {
                is ApiResult.Success -> {
                    val bookingId = parseBookingId(finalResult.data)
                    if (bookingId.isNullOrBlank()) {
                        _uiState.update { it.copy(isSubmitting = false, errorMessage = "Failed to create booking.") }
                    } else {
                        _uiState.update { it.copy(isSubmitting = false, errorMessage = null) }
                        _effects.send(Effect.NavigateToConfirmation(bookingId))
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = finalResult.error.message) }
                }
            }
        }
    }

    private fun buildBookingBody(draft: BookingDraft) = buildJsonObject {
        // Listing identifier (tolerant)
        put("listingId", draft.listingId)
        put("listing_id", draft.listingId)
        put("property_id", draft.listingId)

        // Time window
        put("date", draft.date)
        put("startTimeISO", draft.startTimeISO)
        put("endTimeISO", draft.endTimeISO)
        put("startTime", draft.startTimeISO)
        put("endTime", draft.endTimeISO)
        put("start_time", draft.startTimeISO)
        put("end_time", draft.endTimeISO)

        // Guests
        put("guests", draft.guests)

        // Estimate (optional)
        draft.totalAmountEstimate?.let {
            put("totalAmountEstimate", it)
            put("total_amount_estimate", it)
        }
    }

    private fun parseBookingId(responseBody: String): String? {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val data = (root["data"] as? JsonObject) ?: root
            val booking = (data["booking"] as? JsonObject) ?: data

            booking["bookingId"]?.jsonPrimitive?.content
                ?: booking["id"]?.jsonPrimitive?.content
                ?: booking["_id"]?.jsonPrimitive?.content
                ?: data["bookingId"]?.jsonPrimitive?.content
                ?: data["id"]?.jsonPrimitive?.content
                ?: data["_id"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse booking id")
            null
        }
    }
}

