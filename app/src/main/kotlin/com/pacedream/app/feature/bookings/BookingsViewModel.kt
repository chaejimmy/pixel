package com.pacedream.app.feature.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

// ============================================================================
// Filter Tab — matching iOS BookingTab
// ============================================================================
enum class BookingTab(val label: String, val icon: String) {
    ALL("All", "list"),
    UPCOMING("Upcoming", "calendar_clock"),
    PAST("Past", "check_circle"),
    CANCELLED("Cancelled", "cancel");
}

// ============================================================================
// Booking Role — matching iOS BookingRole
// ============================================================================
enum class BookingRole(val label: String) {
    GUEST("Guest"),
    HOST("Host")
}

// ============================================================================
// Status config — matching iOS BookingStatusConfig
// ============================================================================
enum class BookingFilterCategory { UPCOMING, PAST, CANCELLED }

data class BookingStatusConfig(
    val label: String,
    val filterCategory: BookingFilterCategory,
    val badgeColor: String // "yellow", "blue", "green", "red", "gray"
)

// ============================================================================
// Rich booking list item — matching iOS UnifiedBookingItem
// ============================================================================
data class BookingListItem(
    val id: String,
    val role: BookingRole = BookingRole.GUEST,
    val title: String,
    val imageUrl: String? = null,
    val location: String = "—",
    val dateRange: String = "—",
    val guestsDisplay: String = "—",
    val status: String = "",
    val amount: Double? = null,
    val perNightPrice: Double? = null,
    val nightsCount: Int = 0,
    val isCancelable: Boolean = false,
    val guestName: String? = null,
    // Raw fields for detail screen fallback
    val listingId: String? = null,
    val checkInDate: String? = null,
    val checkInTime: String? = null,
    val checkOutDate: String? = null,
    val checkOutTime: String? = null,
    val guestCount: Int = 1,
    val propertyLocation: String = "",
    val referenceId: String = "",
    val hostName: String = "Host",
    val hostAvatarUrl: String? = null,
    val hostId: String? = null,
    val verificationPin: String? = null,
    val pinStatus: String? = null
)

// ============================================================================
// UI State
// ============================================================================
data class BookingsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val allBookings: List<BookingListItem> = emptyList(),
    val selectedTab: BookingTab = BookingTab.ALL,
    val upcomingBookings: List<BookingListItem> = emptyList(),
    val pastBookings: List<BookingListItem> = emptyList(),
    val cancelledBookings: List<BookingListItem> = emptyList()
) {
    val filteredBookings: List<BookingListItem>
        get() = when (selectedTab) {
            BookingTab.ALL -> allBookings
            BookingTab.UPCOMING -> upcomingBookings
            BookingTab.PAST -> pastBookings
            BookingTab.CANCELLED -> cancelledBookings
        }

    fun count(tab: BookingTab): Int = when (tab) {
        BookingTab.ALL -> allBookings.size
        BookingTab.UPCOMING -> upcomingBookings.size
        BookingTab.PAST -> pastBookings.size
        BookingTab.CANCELLED -> cancelledBookings.size
    }
}

// ============================================================================
// ViewModel
// ============================================================================
@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    // Cached status configs for each booking
    private val statusConfigs = mutableMapOf<String, BookingStatusConfig>()

    init {
        refresh()
    }

    fun selectTab(tab: BookingTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun statusConfig(item: BookingListItem): BookingStatusConfig {
        return statusConfigs[item.id] ?: resolveStatusConfig(item).also {
            statusConfigs[item.id] = it
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = it.allBookings.isEmpty(),
                        isRefreshing = true,
                        error = null
                    )
                }

                // Fetch guest and host bookings in parallel (matching iOS)
                val guestDeferred = async { fetchGuestBookings() }
                val hostDeferred = async { fetchHostBookings() }

                val guestResult = guestDeferred.await()
                val hostResult = hostDeferred.await()

                val allBookings = mutableListOf<BookingListItem>()
                var guestError: String? = null
                var hostError: String? = null

                when (guestResult) {
                    is BookingsResult.Success -> allBookings.addAll(guestResult.items)
                    is BookingsResult.Failure -> guestError = guestResult.message
                }
                when (hostResult) {
                    is BookingsResult.Success -> allBookings.addAll(hostResult.items)
                    is BookingsResult.Failure -> hostError = hostResult.message
                }

                // Rebuild category caches
                statusConfigs.clear()
                val upcoming = mutableListOf<BookingListItem>()
                val past = mutableListOf<BookingListItem>()
                val cancelled = mutableListOf<BookingListItem>()

                for (item in allBookings) {
                    val config = resolveStatusConfig(item)
                    statusConfigs[item.id] = config
                    when (config.filterCategory) {
                        BookingFilterCategory.UPCOMING -> upcoming.add(item)
                        BookingFilterCategory.PAST -> past.add(item)
                        BookingFilterCategory.CANCELLED -> cancelled.add(item)
                    }
                }

                val error = if (guestError != null && hostError != null) {
                    "Couldn't load bookings."
                } else null

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        allBookings = allBookings,
                        upcomingBookings = upcoming,
                        pastBookings = past,
                        cancelledBookings = cancelled,
                        error = error
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh bookings")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load bookings"
                    )
                }
            }
        }
    }

    // ============================================================================
    // Status resolution — matching iOS GuestBookingsViewModel.statusConfig()
    // ============================================================================
    private fun resolveStatusConfig(item: BookingListItem): BookingStatusConfig {
        val status = item.status.lowercase().trim()
        val now = Date()

        // Smart date logic: if checkout/end date has passed and status is still active,
        // auto-promote to "Completed" → past category (matching iOS)
        val endDate = parseIsoDate(item.checkOutDate)
        if (endDate != null && endDate.before(now)) {
            val upcomingStatuses = setOf(
                "confirmed", "upcoming", "active", "ongoing",
                "booked", "accepted", "paid", "succeeded"
            )
            if (status in upcomingStatuses) {
                return BookingStatusConfig("Completed", BookingFilterCategory.PAST, "green")
            }
        }

        // Pending statuses → Upcoming
        val pendingStatuses = setOf(
            "pending", "pending_host", "requires_capture",
            "created", "requires_payment_method", "processing",
            "unverified", "awaiting_confirmation"
        )
        if (status in pendingStatuses || status.contains("pending")) {
            return BookingStatusConfig("Pending", BookingFilterCategory.UPCOMING, "yellow")
        }

        // Confirmed/active statuses → Upcoming
        val confirmedStatuses = setOf(
            "confirmed", "upcoming", "active", "ongoing",
            "booked", "paid", "succeeded", "captured", "accepted"
        )
        if (status in confirmedStatuses) {
            return BookingStatusConfig("Confirmed", BookingFilterCategory.UPCOMING, "blue")
        }

        // Completed statuses → Past
        val completedStatuses = setOf("completed", "finished")
        if (status in completedStatuses) {
            return BookingStatusConfig("Completed", BookingFilterCategory.PAST, "green")
        }

        // Cancelled statuses → Cancelled
        val cancelledStatuses = setOf(
            "canceled", "cancelled", "refunded", "failed", "expired", "void", "declined"
        )
        if (status in cancelledStatuses) {
            return BookingStatusConfig("Cancelled", BookingFilterCategory.CANCELLED, "red")
        }

        if (status.isEmpty()) {
            return BookingStatusConfig("Pending", BookingFilterCategory.UPCOMING, "yellow")
        }

        return BookingStatusConfig(
            status.replaceFirstChar { it.uppercase() },
            BookingFilterCategory.UPCOMING,
            "gray"
        )
    }

    // ============================================================================
    // Network — Guest bookings (with fallback endpoints matching iOS)
    // ============================================================================
    private sealed class BookingsResult {
        data class Success(val items: List<BookingListItem>) : BookingsResult()
        data class Failure(val message: String) : BookingsResult()
    }

    private suspend fun fetchGuestBookings(): BookingsResult {
        // Primary endpoint
        val url = appConfig.buildApiUrl("bookings", "mine")
        return when (val res = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val list = parseGuestBookings(res.data)
                BookingsResult.Success(list)
            }
            is ApiResult.Failure -> {
                // Fallback endpoint
                val fallbackUrl = appConfig.buildApiUrl("guest", "bookings")
                when (val fallback = apiClient.get(fallbackUrl, includeAuth = true)) {
                    is ApiResult.Success -> BookingsResult.Success(parseGuestBookings(fallback.data))
                    is ApiResult.Failure -> BookingsResult.Failure(
                        res.error.message ?: "Failed to load bookings"
                    )
                }
            }
        }
    }

    private suspend fun fetchHostBookings(): BookingsResult {
        val url = appConfig.buildApiUrl("bookings", "host")
        return when (val res = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val list = parseHostBookings(res.data)
                BookingsResult.Success(list)
            }
            is ApiResult.Failure -> {
                // Host bookings failure is non-fatal
                BookingsResult.Success(emptyList())
            }
        }
    }

    // ============================================================================
    // JSON Parsing — Guest bookings (matching iOS BookingSummary decoding)
    // ============================================================================
    private fun parseGuestBookings(body: String): List<BookingListItem> {
        return try {
            val rootElement = json.parseToJsonElement(body)
            val root = rootElement as? JsonObject ?: return emptyList()
            val data = root["data"]

            // Handle multiple response shapes (matching iOS BookingMineResponse)
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
                parseGuestBookingItem(obj)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse guest bookings")
            emptyList()
        }
    }

    private fun parseGuestBookingItem(obj: JsonObject): BookingListItem? {
        val id = obj.str("_id", "id", "bookingId") ?: return null
        val listing = obj["listing"]?.asObjectOrNull()
        val host = obj["host"]?.asObjectOrNull() ?: listing?.get("host")?.asObjectOrNull()
        val location = listing?.get("location")?.asObjectOrNull()

        val parsedListingId = listing?.str("_id", "id")
            ?: obj.str("listingId", "listing_id", "propertyId", "property_id")

        val title = obj.str("title", "listingTitle", "listing_title")
            ?: listing?.str("title", "name")
            ?: "Booking"

        // Image URL extraction (matching iOS comprehensive priority)
        val imageUrl = extractImageUrl(obj, listing)

        // Location
        val city = location?.str("city") ?: obj.str("city", "location")
        val state = location?.str("state") ?: obj.str("state")
        val displayLocation = buildLocation(city, state)

        // Status
        val status = obj.str("status")?.lowercase()?.trim() ?: ""

        // Amount (matching iOS priority: priceTotal, price_total, amount, total, totalAmount)
        val amount = obj.double("priceTotal", "price_total", "price", "amount", "total", "totalAmount", "total_amount")

        // Dates
        val checkInRaw = obj.str("checkIn", "check_in")
        val checkOutRaw = obj.str("checkOut", "check_out")
        val startRaw = obj.str("start", "startDate", "start_date", "startTime", "start_time")
        val endRaw = obj.str("end", "endDate", "end_date", "endTime", "end_time")

        val checkIn = parseIsoDate(checkInRaw) ?: parseIsoDate(startRaw)
        val checkOut = parseIsoDate(checkOutRaw) ?: parseIsoDate(endRaw)

        val nightsCount = if (checkIn != null && checkOut != null) {
            val diff = checkOut.time - checkIn.time
            (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } else 0

        val perNight = if (nightsCount > 0 && amount != null) amount / nightsCount else null

        val guestsCount = obj.int("guestsCount", "guestCount", "guests", "guest_count") ?: 0
        val guestsDisplay = if (guestsCount > 0) "$guestsCount guest${if (guestsCount == 1) "" else "s"}" else "—"

        val dateRange = formatDateRange(checkIn, checkOut, startRaw != null || endRaw != null)

        // Cancelability (matching iOS)
        val nonCancelable = setOf("canceled", "cancelled", "refunded", "failed", "expired", "void", "completed")
        val isCancelable = status.isNotEmpty() && status !in nonCancelable && (checkIn?.after(Date()) ?: true)

        // Verification PIN
        val verificationPin = obj.str("verificationPin", "verification_pin", "verificationCode")
        val pinStatus = obj.str("pinStatus", "pin_status", "verificationStatus")

        return BookingListItem(
            id = id,
            role = BookingRole.GUEST,
            title = title,
            imageUrl = imageUrl,
            location = displayLocation,
            dateRange = dateRange,
            guestsDisplay = guestsDisplay,
            status = status,
            amount = amount,
            perNightPrice = perNight,
            nightsCount = nightsCount,
            isCancelable = isCancelable,
            listingId = parsedListingId,
            checkInDate = checkInRaw ?: startRaw,
            checkInTime = obj.str("checkInTime", "startTime"),
            checkOutDate = checkOutRaw ?: endRaw,
            checkOutTime = obj.str("checkOutTime", "endTime"),
            guestCount = guestsCount.coerceAtLeast(1),
            propertyLocation = displayLocation,
            referenceId = obj.str("referenceId", "reference", "bookingRef", "confirmationCode")
                ?: id.takeLast(8).uppercase(),
            hostName = host?.str("name", "displayName", "firstName") ?: obj.str("hostName") ?: "Host",
            hostAvatarUrl = host?.str("avatarUrl", "avatar", "profileImage", "imageUrl"),
            hostId = host?.str("_id", "id", "userId") ?: obj.str("hostId"),
            verificationPin = verificationPin,
            pinStatus = pinStatus
        )
    }

    // ============================================================================
    // JSON Parsing — Host bookings
    // ============================================================================
    private fun parseHostBookings(body: String): List<BookingListItem> {
        return try {
            val rootElement = json.parseToJsonElement(body)
            val root = rootElement as? JsonObject ?: return emptyList()
            val arr = root["bookings"]?.asArrayOrNull()
                ?: root["data"]?.asArrayOrNull()
                ?: return emptyList()

            arr.mapNotNull { el ->
                val obj = el.asObjectOrNull() ?: return@mapNotNull null
                val id = obj.str("_id", "id", "bookingId") ?: return@mapNotNull null
                val listing = obj["listing"]?.asObjectOrNull()
                val guest = obj["guest"]?.asObjectOrNull()

                val title = listing?.str("title", "name")
                    ?: obj.str("listingTitle", "title")
                    ?: "Booking"

                val status = obj.str("status")?.lowercase()?.trim() ?: ""
                val amount = obj.double("total", "amount", "priceTotal")

                val startRaw = obj.str("start", "startDate", "checkIn")
                val endRaw = obj.str("end", "endDate", "checkOut")
                val startDate = parseIsoDate(startRaw)
                val endDate = parseIsoDate(endRaw)

                val nightsCount = if (startDate != null && endDate != null) {
                    val diff = endDate.time - startDate.time
                    (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                } else 0

                val guestName = obj.str("guestName") ?: guest?.str("name", "displayName")
                val dateRange = formatDateRange(startDate, endDate, false)

                val nonCancelable = setOf("cancelled", "canceled", "declined", "completed")
                val isCancelable = status !in nonCancelable && (endDate?.after(Date()) ?: true)

                BookingListItem(
                    id = "host_$id",
                    role = BookingRole.HOST,
                    title = title,
                    imageUrl = listing?.str("image", "coverImage", "thumbnail"),
                    location = "—",
                    dateRange = dateRange,
                    guestsDisplay = guestName ?: "—",
                    status = status,
                    amount = amount,
                    nightsCount = nightsCount,
                    isCancelable = isCancelable,
                    guestName = guestName,
                    checkInDate = startRaw,
                    checkOutDate = endRaw,
                    referenceId = id.takeLast(8).uppercase()
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse host bookings")
            emptyList()
        }
    }

    // ============================================================================
    // Image URL extraction — matching iOS decodeImageURLBestEffort
    // ============================================================================
    private fun extractImageUrl(obj: JsonObject, listing: JsonObject?): String? {
        // Top-level string fields first (coverUrl from /bookings/mine)
        obj.str(
            "coverUrl", "cover_url", "coverImage", "cover_image",
            "image", "cover", "thumbnail", "photo", "picture",
            "coverPhoto", "mainImage"
        )?.let { return it }

        // Array fields: images[0], photos[0]
        obj["images"]?.asArrayOrNull()?.firstOrNull()?.let { el ->
            val imgObj = el.asObjectOrNull()
            (imgObj?.str("thumbnail", "url", "src", "secure_url", "secureUrl")
                ?: runCatching { el.jsonPrimitive.contentOrNull }.getOrNull())?.let { return it }
        }

        obj["photos"]?.asArrayOrNull()?.firstOrNull()?.let { el ->
            val imgObj = el.asObjectOrNull()
            (imgObj?.str("thumbnail", "url", "src") ?: runCatching { el.jsonPrimitive.contentOrNull }.getOrNull())
                ?.let { return it }
        }

        // Nested listing fields
        listing?.str(
            "coverImage", "cover_image", "image", "cover", "thumbnail"
        )?.let { return it }

        listing?.get("images")?.asArrayOrNull()?.firstOrNull()?.let { el ->
            val imgObj = el.asObjectOrNull()
            (imgObj?.str("thumbnail", "url", "src") ?: runCatching { el.jsonPrimitive.contentOrNull }.getOrNull())
                ?.let { return it }
        }

        listing?.get("gallery")?.let { gallery ->
            gallery.asObjectOrNull()?.str("thumbnail")?.let { return it }
            gallery.asArrayOrNull()?.firstOrNull()?.asObjectOrNull()
                ?.str("thumbnail", "url")?.let { return it }
        }

        return null
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private fun buildLocation(city: String?, state: String?): String {
        val c = city?.trim().orEmpty()
        val s = state?.trim().orEmpty()
        return when {
            c.isNotEmpty() && s.isNotEmpty() -> "$c, $s"
            c.isNotEmpty() -> c
            s.isNotEmpty() -> s
            else -> "—"
        }
    }

    private fun formatDateRange(start: Date?, end: Date?, includeTime: Boolean): String {
        if (start == null || end == null) return "—"
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        return "${fmt.format(start)} – ${fmt.format(end)}"
    }

    companion object {
        private val isoFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to "UTC",
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to "UTC",
            "yyyy-MM-dd'T'HH:mm:ssXXX" to null,
            "yyyy-MM-dd" to null
        )

        fun parseIsoDate(raw: String?): Date? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            for ((pattern, tz) in isoFormats) {
                try {
                    val fmt = SimpleDateFormat(pattern, Locale.US)
                    if (tz != null) fmt.timeZone = TimeZone.getTimeZone(tz)
                    return fmt.parse(trimmed)
                } catch (_: Exception) { }
            }
            // Try as timestamp
            val d = trimmed.toDoubleOrNull() ?: return null
            return when {
                d > 9_999_999_999 -> Date((d).toLong()) // ms
                d > 1_000_000_000 -> Date((d * 1000).toLong()) // s
                else -> null
            }
        }

        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            currency = Currency.getInstance("USD")
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }

        fun formatUsd(value: Double): String {
            return currencyFormatter.format(value)
        }
    }
}

// ============================================================================
// JSON extension helpers
// ============================================================================
private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonObject.str(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k ->
        this[k]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

private fun JsonObject.double(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { k ->
        val el = this[k] ?: return@firstNotNullOfOrNull null
        val prim = el.jsonPrimitive
        prim.doubleOrNull
            ?: prim.intOrNull?.toDouble()
            ?: prim.contentOrNull?.toDoubleOrNull()
    }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k ->
        val el = this[k] ?: return@firstNotNullOfOrNull null
        val prim = el.jsonPrimitive
        prim.intOrNull
            ?: prim.contentOrNull?.trim()?.toIntOrNull()
            ?: prim.doubleOrNull?.toInt()
    }
