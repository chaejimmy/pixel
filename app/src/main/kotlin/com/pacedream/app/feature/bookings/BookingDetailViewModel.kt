package com.pacedream.app.feature.bookings

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

// ============================================================================
// Booking Status
// ============================================================================
enum class BookingStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    companion object {
        fun from(raw: String?): BookingStatus {
            if (raw == null) return PENDING
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
        }
    }
}

// ============================================================================
// Booking Detail Data
// ============================================================================
data class BookingDetail(
    val id: String,
    val referenceId: String,
    val status: BookingStatus,
    val propertyName: String,
    val propertyLocation: String,
    val propertyImageUrl: String?,
    val checkInDate: String,
    val checkInTime: String?,
    val checkOutDate: String,
    val checkOutTime: String?,
    val guestCount: Int,
    val basePrice: String,
    val serviceFee: String,
    val taxes: String,
    val totalPrice: String,
    val hostName: String,
    val hostAvatarUrl: String?,
    val hostId: String?,
    val cancellationPolicy: String
)

// ============================================================================
// UI State
// ============================================================================
data class BookingDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val booking: BookingDetail? = null,
    val isCancelling: Boolean = false,
    val cancelSuccess: Boolean = false,
    val cancelError: String? = null
)

// ============================================================================
// ViewModel
// ============================================================================
@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val bookingId: String = checkNotNull(savedStateHandle["bookingId"])

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init {
        loadBookingDetail()
    }

    fun loadBookingDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val url = appConfig.buildApiUrl("bookings", bookingId)

            when (val res = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val detail = parseBookingDetail(res.data)
                    if (detail != null) {
                        _uiState.update { it.copy(isLoading = false, booking = detail) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Failed to parse booking details.") }
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error.message) }
                }
            }
        }
    }

    fun cancelBooking() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, cancelError = null) }
            val url = appConfig.buildApiUrl("bookings", bookingId, "cancel")

            when (val res = apiClient.post(url, body = "{}", includeAuth = true)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancelSuccess = true,
                            booking = it.booking?.copy(status = BookingStatus.CANCELLED)
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isCancelling = false, cancelError = res.error.message) }
                }
            }
        }
    }

    fun dismissCancelError() {
        _uiState.update { it.copy(cancelError = null) }
    }

    // ============================================================================
    // JSON Parsing — defensive, matching iOS field names
    // ============================================================================
    private fun parseBookingDetail(body: String): BookingDetail? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val obj = root["data"]?.asObjOrNull()
                ?: root["booking"]?.asObjOrNull()
                ?: root

            val id = obj.str("_id", "id", "bookingId") ?: bookingId

            val listing = obj["listing"]?.asObjOrNull()
            val host = obj["host"]?.asObjOrNull() ?: listing?.get("host")?.asObjOrNull()

            val pricing = obj["pricing"]?.asObjOrNull()

            BookingDetail(
                id = id,
                referenceId = obj.str("referenceId", "reference", "bookingRef", "confirmationCode") ?: id.takeLast(8).uppercase(),
                status = BookingStatus.from(obj.str("status")),
                propertyName = listing?.str("name", "title")
                    ?: obj.str("listingName", "propertyName", "title")
                    ?: "Property",
                propertyLocation = listing?.str("location", "address", "city")
                    ?: obj.str("location", "address")
                    ?: "",
                propertyImageUrl = listing?.str("imageUrl", "image", "coverImage", "thumbnail")
                    ?: obj.str("imageUrl", "propertyImage"),
                checkInDate = obj.str("checkInDate", "startDate", "date") ?: "",
                checkInTime = obj.str("checkInTime", "startTime", "start_time"),
                checkOutDate = obj.str("checkOutDate", "endDate") ?: "",
                checkOutTime = obj.str("checkOutTime", "endTime", "end_time"),
                guestCount = obj.str("guestCount", "guests", "numberOfGuests")?.toIntOrNull() ?: 1,
                basePrice = pricing?.str("basePrice", "subtotal")
                    ?: obj.str("basePrice", "subtotal", "price")
                    ?: "$0.00",
                serviceFee = pricing?.str("serviceFee", "fee")
                    ?: obj.str("serviceFee", "fee")
                    ?: "$0.00",
                taxes = pricing?.str("taxes", "tax")
                    ?: obj.str("taxes", "tax")
                    ?: "$0.00",
                totalPrice = pricing?.str("total", "totalPrice")
                    ?: obj.str("totalPrice", "total", "amount")
                    ?: "$0.00",
                hostName = host?.str("name", "displayName", "firstName")
                    ?: obj.str("hostName")
                    ?: "Host",
                hostAvatarUrl = host?.str("avatarUrl", "avatar", "profileImage", "imageUrl"),
                hostId = host?.str("_id", "id", "userId") ?: obj.str("hostId"),
                cancellationPolicy = obj.str("cancellationPolicy", "cancelPolicy")
                    ?: listing?.str("cancellationPolicy")
                    ?: "Free cancellation up to 24 hours before check-in. After that, the first night is non-refundable."
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse booking detail")
            null
        }
    }
}

// ============================================================================
// JSON Helpers (package-private, shared with BookingsViewModel pattern)
// ============================================================================
private fun kotlinx.serialization.json.JsonElement.asObjOrNull(): JsonObject? = this as? JsonObject

private fun JsonObject.str(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } }
