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
    val errorMessage: String? = null,
    val paymentCompleted: Boolean = false
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    sealed class Effect {
        data class NavigateToConfirmation(val bookingId: String) : Effect()
        data class LaunchStripeCheckout(val checkoutUrl: String) : Effect()
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
        // Guard: prevent duplicate submissions
        if (_uiState.value.isSubmitting || _uiState.value.paymentCompleted) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val body = buildBookingBody(draft).toString()
            val urls = buildBookingUrls(draft)

            var finalResult: ApiResult<String>? = null
            for (url in urls) {
                val result = apiClient.post(url, body, includeAuth = true)
                if (result is ApiResult.Success) {
                    finalResult = result
                    break
                }
                if (result is ApiResult.Failure && result.error !is ApiError.NotFound) {
                    finalResult = result
                    break
                }
                finalResult = result
            }

            when (finalResult) {
                is ApiResult.Success -> {
                    // iOS parity: backend may return a checkoutUrl (Stripe) or a direct booking ID.
                    val checkoutUrl = parseCheckoutUrl(finalResult.data)
                    val bookingId = parseBookingId(finalResult.data)

                    when {
                        !checkoutUrl.isNullOrBlank() -> {
                            // Stripe checkout flow (matches iOS WebFlow)
                            _uiState.update { it.copy(isSubmitting = false, errorMessage = null, paymentCompleted = true) }
                            _effects.send(Effect.LaunchStripeCheckout(checkoutUrl))
                        }
                        !bookingId.isNullOrBlank() -> {
                            // Direct booking created
                            _uiState.update { it.copy(isSubmitting = false, errorMessage = null, paymentCompleted = true) }
                            _effects.send(Effect.NavigateToConfirmation(bookingId))
                        }
                        else -> {
                            _uiState.update { it.copy(isSubmitting = false, errorMessage = "Failed to create booking.") }
                        }
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = finalResult.error.message ?: "Failed to create booking.") }
                }
                null -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "Failed to create booking.") }
                }
            }
        }
    }

    /**
     * Build the ordered list of booking URLs to try based on listing type.
     *
     * Backend endpoints (matching iOS / CheckoutLauncher):
     *   time-based → POST /v1/properties/bookings/timebased
     *   gear       → POST /v1/gear-rentals/book
     *   fallback   → POST /v1/bookings
     */
    private fun buildBookingUrls(draft: BookingDraft): List<okhttp3.HttpUrl> {
        val typeSpecific = when (draft.listingType) {
            "time-based" -> listOf(
                appConfig.buildApiUrl("properties", "bookings", "timebased")
            )
            "gear" -> listOf(
                appConfig.buildApiUrl("gear-rentals", "book")
            )
            "split-stay" -> listOf(
                appConfig.buildApiUrl("roommate", "book")
            )
            else -> emptyList()
        }
        return typeSpecific + listOf(appConfig.buildApiUrl("bookings"))
    }

    private fun buildBookingBody(draft: BookingDraft) = buildJsonObject {
        // Listing / item identifier – use the key the endpoint expects
        put("itemId", draft.listingId)
        put("listingId", draft.listingId)
        put("listing_id", draft.listingId)
        put("property_id", draft.listingId)
        put("gearId", draft.listingId)

        // Time window
        put("date", draft.date)
        put("startTimeISO", draft.startTimeISO)
        put("endTimeISO", draft.endTimeISO)
        put("startTime", draft.startTimeISO)
        put("endTime", draft.endTimeISO)
        put("start_time", draft.startTimeISO)
        put("end_time", draft.endTimeISO)
        put("startDate", draft.date)
        put("endDate", draft.date)

        // Website parity: send guests as object { adults, children, infants }
        // AND as flat integer for backward compatibility
        put("guests", buildJsonObject {
            put("adults", draft.adults)
            put("children", draft.children)
            put("infants", draft.infants)
        })
        put("guestCount", draft.guests)

        // Amount (required by the backend)
        draft.totalAmountEstimate?.let {
            put("amount", it)
            put("totalAmountEstimate", it)
            put("total_amount_estimate", it)
        }
    }

    /**
     * Parse checkout URL from backend response (iOS parity: Stripe checkout flow).
     */
    private fun parseCheckoutUrl(responseBody: String): String? {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val data = (root["data"] as? JsonObject) ?: root
            data["checkoutUrl"]?.jsonPrimitive?.content
                ?: data["checkout_url"]?.jsonPrimitive?.content
                ?: data["url"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
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

