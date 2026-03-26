package com.pacedream.app.feature.webflow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.shourov.apps.pacedream.core.data.repository.BookingRepository as CoreBookingRepository
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

/**
 * BookingConfirmationViewModel - Confirms booking after Stripe checkout
 * 
 * iOS Parity:
 * - Receives session_id from deep link (via SavedStateHandle)
 * - Calls success endpoint based on booking type:
 *   - Timebased: GET /v1/properties/bookings/timebased/success/checkout?session_id=...
 *   - Gear: GET /v1/gear-rentals/success/checkout?session_id=...
 * - Persists last checkout for resume after relaunch
 */
@HiltViewModel
class BookingConfirmationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json,
    private val coreBookingRepository: CoreBookingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BookingConfirmationUiState())
    val uiState: StateFlow<BookingConfirmationUiState> = _uiState.asStateFlow()
    
    // Get session_id from deep link arguments
    private val sessionId: String? = savedStateHandle.get<String>("sessionId")
        ?: savedStateHandle.get<String>("session_id")
    
    init {
        // Check for resumed checkout
        if (sessionId == null) {
            checkResumedCheckout()
        }
    }
    
    fun confirmBooking(bookingType: String) {
        val sid = sessionId ?: tokenStorage.lastCheckoutSessionId
        if (sid == null) {
            _uiState.update { it.copy(isLoading = false, error = "No session found") }
            return
        }
        
        val type = if (bookingType.isNotBlank()) bookingType else tokenStorage.lastCheckoutBookingType ?: "timebased"
        
        // Persist for resume
        tokenStorage.storeCheckoutSession(sid, type)
        
        callSuccessEndpoint(sid, type)
    }
    
    fun retry(bookingType: String) {
        confirmBooking(bookingType)
    }
    
    private fun checkResumedCheckout() {
        val savedSessionId = tokenStorage.lastCheckoutSessionId
        val savedBookingType = tokenStorage.lastCheckoutBookingType
        
        if (savedSessionId != null && savedBookingType != null) {
            Timber.d("Resuming checkout: $savedSessionId, $savedBookingType")
            callSuccessEndpoint(savedSessionId, savedBookingType)
        }
    }
    
    private fun callSuccessEndpoint(sessionId: String, bookingType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val url = when (bookingType) {
                "gear" -> appConfig.buildApiUrl(
                    "gear-rentals", "success", "checkout",
                    queryParams = mapOf("session_id" to sessionId)
                )
                else -> appConfig.buildApiUrl(
                    "properties", "bookings", "timebased", "success", "checkout",
                    queryParams = mapOf("session_id" to sessionId)
                )
            }
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val bookingId = parseBookingId(result.data)

                    // Cache confirmed booking to Room so detail view can find it
                    if (bookingId != null) {
                        cacheConfirmedBooking(bookingId, result.data)
                    }

                    // Clear persisted checkout on success
                    tokenStorage.clearCheckoutSession()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            bookingId = bookingId,
                            error = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to confirm booking: ${result.error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }
    
    private fun parseBookingId(responseBody: String): String? {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject

            val data = obj["data"]?.jsonObject ?: obj

            data["_id"]?.jsonPrimitive?.content
                ?: data["id"]?.jsonPrimitive?.content
                ?: data["bookingId"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse booking ID")
            null
        }
    }

    private suspend fun cacheConfirmedBooking(bookingId: String, responseBody: String) {
        try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            val data = obj["data"]?.jsonObject ?: obj

            val title = data["itemTitle"]?.jsonPrimitive?.content
                ?: data["title"]?.jsonPrimitive?.content
                ?: data["listingTitle"]?.jsonPrimitive?.content
                ?: ""
            val startDate = data["startDate"]?.jsonPrimitive?.content
                ?: data["start_date"]?.jsonPrimitive?.content
                ?: data["startTime"]?.jsonPrimitive?.content
                ?: ""
            val endDate = data["endDate"]?.jsonPrimitive?.content
                ?: data["end_date"]?.jsonPrimitive?.content
                ?: data["endTime"]?.jsonPrimitive?.content
                ?: ""
            val amount = data["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: data["total"]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: 0.0
            val status = data["status"]?.jsonPrimitive?.content ?: "confirmed"

            val booking = BookingModel(
                id = bookingId,
                propertyName = title,
                startDate = startDate,
                endDate = endDate,
                totalPrice = amount,
                price = amount.toString(),
                status = BookingStatus.fromString(status),
                bookingStatus = status,
                checkInTime = startDate,
                checkOutTime = endDate,
                currency = "USD"
            )
            coreBookingRepository.cacheBooking(booking)
            Timber.d("Cached confirmed booking $bookingId to Room")
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache confirmed booking to Room")
        }
    }
}

/**
 * Booking confirmation UI state
 */
data class BookingConfirmationUiState(
    val isLoading: Boolean = true,
    val bookingId: String? = null,
    val error: String? = null
)


