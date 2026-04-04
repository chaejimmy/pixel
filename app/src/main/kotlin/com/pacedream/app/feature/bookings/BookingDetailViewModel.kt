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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
            val normalized = raw.trim().lowercase()
            // Match iOS BookingStatusHelper logic
            if (normalized in setOf("cancelled", "canceled", "refunded", "failed", "expired", "declined")) {
                return CANCELLED
            }
            if (normalized in setOf("completed", "finished")) return COMPLETED
            if (normalized in setOf("confirmed", "upcoming", "active", "ongoing", "booked", "paid", "succeeded", "accepted")) {
                return CONFIRMED
            }
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
    val listingId: String? = null,
    val checkInDate: String,
    val checkInTime: String?,
    val checkOutDate: String,
    val checkOutTime: String?,
    val guestCount: Int,
    val nightsCount: Int,
    val perNightPrice: String?,
    val totalPrice: String,
    val totalAmount: Double?,
    val hostName: String,
    val hostAvatarUrl: String?,
    val hostId: String?,
    val cancellationPolicy: String,
    val verificationPin: String?,
    val pinStatus: String?,
    val locationCity: String?,
    val locationState: String?,
    val statusLabel: String // Resolved via iOS-matching logic
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
// ViewModel — now accepts cached data from list (matching iOS pattern)
// ============================================================================
@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val bookingId: String = savedStateHandle.get<String>("bookingId").orEmpty()

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init {
        if (bookingId.isNotBlank()) {
            // Check if we have cached data passed via savedStateHandle (iOS pattern)
            val cachedTitle = savedStateHandle.get<String>("cached_title")
            if (cachedTitle != null) {
                val cached = buildCachedBookingDetail(savedStateHandle)
                _uiState.update { it.copy(isLoading = false, booking = cached) }
            }
            loadBookingDetail()
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Missing booking ID") }
        }
    }

    /**
     * Build a BookingDetail from cached list data passed via savedStateHandle.
     * This matches the iOS pattern of passing the existing BookingSummary to
     * BookingDetailView for immediate display while fresh data loads.
     */
    private fun buildCachedBookingDetail(handle: SavedStateHandle): BookingDetail {
        val totalAmount = handle.get<String>("cached_amount")?.toDoubleOrNull()
        val nightsCount = handle.get<String>("cached_nightsCount")?.toIntOrNull() ?: 0
        val perNight = if (nightsCount > 0 && totalAmount != null) {
            BookingsViewModel.formatUsd(totalAmount / nightsCount)
        } else null

        return BookingDetail(
            id = bookingId,
            referenceId = handle.get<String>("cached_referenceId") ?: bookingId.takeLast(8).uppercase(),
            status = BookingStatus.from(handle.get<String>("cached_status")),
            propertyName = handle.get<String>("cached_title") ?: "Booking",
            propertyLocation = handle.get<String>("cached_location") ?: "",
            propertyImageUrl = handle.get<String>("cached_imageUrl")?.takeIf { it.isNotBlank() },
            listingId = handle.get<String>("cached_listingId"),
            checkInDate = handle.get<String>("cached_checkInDate") ?: "",
            checkInTime = handle.get<String>("cached_checkInTime"),
            checkOutDate = handle.get<String>("cached_checkOutDate") ?: "",
            checkOutTime = handle.get<String>("cached_checkOutTime"),
            guestCount = handle.get<String>("cached_guestCount")?.toIntOrNull() ?: 1,
            nightsCount = nightsCount,
            perNightPrice = perNight,
            totalPrice = totalAmount?.let { BookingsViewModel.formatUsd(it) } ?: "—",
            totalAmount = totalAmount,
            hostName = handle.get<String>("cached_hostName") ?: "Host",
            hostAvatarUrl = handle.get<String>("cached_hostAvatarUrl"),
            hostId = handle.get<String>("cached_hostId"),
            cancellationPolicy = "Free cancellation up to 24 hours before check-in. After that, the first night is non-refundable.",
            verificationPin = handle.get<String>("cached_verificationPin"),
            pinStatus = handle.get<String>("cached_pinStatus"),
            locationCity = null,
            locationState = null,
            statusLabel = resolveStatusLabel(
                handle.get<String>("cached_status") ?: "",
                handle.get<String>("cached_checkOutDate")
            )
        )
    }

    fun loadBookingDetail() {
        if (bookingId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Missing booking ID") }
            return
        }
        viewModelScope.launch {
            try {
                // Only show loading spinner if we have no cached data (matching iOS)
                if (_uiState.value.booking == null) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }

                val url = appConfig.buildApiUrl("booking", bookingId)

                when (val res = apiClient.get(url, includeAuth = true)) {
                    is ApiResult.Success -> {
                        val detail = parseBookingDetail(res.data)
                        if (detail != null) {
                            _uiState.update { it.copy(isLoading = false, booking = detail, error = null) }
                        } else {
                            // Parse failed, but if we have cached data, keep it (matching iOS)
                            if (_uiState.value.booking != null) {
                                _uiState.update { it.copy(isLoading = false, error = null) }
                            } else {
                                _uiState.update { it.copy(isLoading = false, error = "Failed to parse booking details.") }
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        // If we already have cached data, don't overwrite with error (matching iOS 404 handling)
                        if (_uiState.value.booking != null) {
                            _uiState.update { it.copy(isLoading = false, error = null) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = res.error.message) }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load booking detail")
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load booking details")
                }
            }
        }
    }

    fun cancelBooking() {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCancelling = true, cancelError = null) }

                // Try POST first (matching iOS primary endpoint)
                val cancelUrl = appConfig.buildApiUrl("poc", "bookings", bookingId, "cancel")
                val result = apiClient.post(cancelUrl, body = "{}", includeAuth = true)

                when (result) {
                    is ApiResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isCancelling = false,
                                cancelSuccess = true,
                                booking = it.booking?.copy(
                                    status = BookingStatus.CANCELLED,
                                    statusLabel = "Cancelled"
                                )
                            )
                        }
                    }
                    is ApiResult.Failure -> {
                        // Fallback endpoint (matching iOS)
                        val fallbackUrl = appConfig.buildApiUrl("bookings", bookingId, "cancel")
                        when (val fallback = apiClient.put(fallbackUrl, body = "{}", includeAuth = true)) {
                            is ApiResult.Success -> {
                                _uiState.update {
                                    it.copy(
                                        isCancelling = false,
                                        cancelSuccess = true,
                                        booking = it.booking?.copy(
                                            status = BookingStatus.CANCELLED,
                                            statusLabel = "Cancelled"
                                        )
                                    )
                                }
                            }
                            is ApiResult.Failure -> {
                                _uiState.update {
                                    it.copy(isCancelling = false, cancelError = fallback.error.message)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to cancel booking")
                _uiState.update {
                    it.copy(isCancelling = false, cancelError = e.message ?: "Failed to cancel booking")
                }
            }
        }
    }

    fun dismissCancelError() {
        _uiState.update { it.copy(cancelError = null) }
    }

    // ============================================================================
    // Status label resolution — matching iOS BookingStatusHelper.resolveLabel
    // ============================================================================
    private fun resolveStatusLabel(rawStatus: String, checkOutRaw: String?): String {
        val s = rawStatus.trim().lowercase()

        if (s in setOf("issue_reported", "under_review")) return "Issue Reported"
        if (s in setOf("completed", "finished")) return "Completed"
        if (s in setOf("cancelled", "canceled", "refunded", "failed", "expired", "declined")) return "Cancelled"
        if (s.isEmpty() || s.contains("pending") || s.contains("created") || s.contains("processing")) return "Pending"

        val activeStatuses = setOf("confirmed", "upcoming", "active", "ongoing", "booked", "paid", "succeeded", "accepted")
        if (s in activeStatuses) {
            val endDate = BookingsViewModel.parseIsoDate(checkOutRaw)
            if (endDate != null && endDate.before(Date())) return "Completed"
            if (s == "ongoing") return "In Progress"
            return "Upcoming"
        }

        return s.replaceFirstChar { it.uppercase() }
    }

    // ============================================================================
    // JSON Parsing — matching iOS BookingSummary flexible decoding
    // ============================================================================
    private fun parseBookingDetail(body: String): BookingDetail? {
        return try {
            val root = json.parseToJsonElement(body) as? JsonObject ?: return null
            val obj = root["data"]?.asObjOrNull()
                ?: root["booking"]?.asObjOrNull()
                ?: root

            val id = obj.str("_id", "id", "bookingId") ?: bookingId
            val rawStatus = obj.str("status") ?: ""

            val listing = obj["listing"]?.asObjOrNull()
            val host = obj["host"]?.asObjOrNull() ?: listing?.get("host")?.asObjOrNull()
            val location = listing?.get("location")?.asObjOrNull()

            val parsedListingId = listing?.str("_id", "id")
                ?: obj.str("listingId", "listing_id", "propertyId", "property_id")

            val pricing = obj["pricing"]?.asObjOrNull()

            val title = obj.str("title", "listingTitle", "listing_title")
                ?: listing?.str("name", "title")
                ?: obj.str("listingName", "propertyName")
                ?: "Property"

            // Image (comprehensive matching iOS)
            val imageUrl = (listing?.str("imageUrl", "image", "coverImage", "thumbnail")
                ?: obj.str("coverUrl", "cover_url", "imageUrl", "propertyImage", "coverImage", "image"))
                ?.takeIf { it.isNotBlank() }

            // Location
            val city = location?.str("city") ?: obj.str("city", "location")
            val state = location?.str("state") ?: obj.str("state")
            val displayLocation = buildString {
                if (!city.isNullOrBlank()) append(city)
                if (!city.isNullOrBlank() && !state.isNullOrBlank()) append(", ")
                if (!state.isNullOrBlank()) append(state)
            }

            // Dates
            val checkInRaw = obj.str("checkIn", "check_in", "checkInDate", "startDate", "date")
            val checkOutRaw = obj.str("checkOut", "check_out", "checkOutDate", "endDate")

            val checkIn = BookingsViewModel.parseIsoDate(checkInRaw)
            val checkOut = BookingsViewModel.parseIsoDate(checkOutRaw)
            val nightsCount = if (checkIn != null && checkOut != null) {
                ((checkOut.time - checkIn.time) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            } else 0

            // Amount
            val totalAmount = pricing?.double("total", "totalPrice")
                ?: obj.double("priceTotal", "price_total", "totalPrice", "total", "amount")

            val perNight = if (nightsCount > 0 && totalAmount != null) {
                BookingsViewModel.formatUsd(totalAmount / nightsCount)
            } else null

            // Format dates for display
            val dateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
            val timeFormatter = SimpleDateFormat("h:mm a", Locale.US)

            val checkInDisplay = checkIn?.let { dateFormatter.format(it) } ?: checkInRaw ?: ""
            val checkOutDisplay = checkOut?.let { dateFormatter.format(it) } ?: checkOutRaw ?: ""

            val checkInTimeDisplay = checkIn?.let { d ->
                val cal = java.util.Calendar.getInstance().apply { time = d }
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val min = cal.get(java.util.Calendar.MINUTE)
                if (hour == 0 && min == 0) null else timeFormatter.format(d)
            }
            val checkOutTimeDisplay = checkOut?.let { d ->
                val cal = java.util.Calendar.getInstance().apply { time = d }
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val min = cal.get(java.util.Calendar.MINUTE)
                if (hour == 0 && min == 0) null else timeFormatter.format(d)
            }

            BookingDetail(
                id = id,
                referenceId = obj.str("referenceId", "reference", "bookingRef", "confirmationCode")
                    ?: id.takeLast(10),
                status = BookingStatus.from(rawStatus),
                statusLabel = resolveStatusLabel(rawStatus, checkOutRaw),
                propertyName = title,
                propertyLocation = displayLocation,
                propertyImageUrl = imageUrl,
                listingId = parsedListingId,
                checkInDate = checkInDisplay,
                checkInTime = checkInTimeDisplay ?: obj.str("checkInTime", "startTime"),
                checkOutDate = checkOutDisplay,
                checkOutTime = checkOutTimeDisplay ?: obj.str("checkOutTime", "endTime"),
                guestCount = obj.int("guestsCount", "guestCount", "guests", "numberOfGuests") ?: 1,
                nightsCount = nightsCount,
                perNightPrice = perNight,
                totalPrice = totalAmount?.let { BookingsViewModel.formatUsd(it) }
                    ?: pricing?.str("total", "totalPrice")
                    ?: obj.str("totalPrice", "total", "amount")
                    ?: "—",
                totalAmount = totalAmount,
                hostName = host?.str("name", "displayName", "firstName")
                    ?: obj.str("hostName") ?: "Host",
                hostAvatarUrl = host?.str("avatarUrl", "avatar", "profileImage", "imageUrl"),
                hostId = host?.str("_id", "id", "userId") ?: obj.str("hostId"),
                cancellationPolicy = obj.str("cancellationPolicy", "cancelPolicy")
                    ?: listing?.str("cancellationPolicy")
                    ?: "Free cancellation up to 24 hours before check-in. After that, the first night is non-refundable.",
                verificationPin = obj.str("verificationPin", "verification_pin", "verificationCode"),
                pinStatus = obj.str("pinStatus", "pin_status", "verificationStatus"),
                locationCity = city,
                locationState = state
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse booking detail")
            null
        }
    }
}

// ============================================================================
// JSON Helpers
// ============================================================================
private fun JsonElement.asObjOrNull(): JsonObject? = this as? JsonObject

private fun JsonObject.str(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } }

private fun JsonObject.double(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { k ->
        val el = this[k] ?: return@firstNotNullOfOrNull null
        val prim = el.jsonPrimitive
        prim.doubleOrNull ?: prim.intOrNull?.toDouble() ?: prim.contentOrNull?.toDoubleOrNull()
    }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k ->
        val el = this[k] ?: return@firstNotNullOfOrNull null
        val prim = el.jsonPrimitive
        prim.intOrNull ?: prim.contentOrNull?.trim()?.toIntOrNull() ?: prim.doubleOrNull?.toInt()
    }
