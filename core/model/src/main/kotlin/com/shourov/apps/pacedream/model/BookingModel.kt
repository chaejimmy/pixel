package com.shourov.apps.pacedream.model

/**
 * Booking status enum aligned with backend source of truth.
 *
 * Backend statuses (bookingStatuses.js):
 *   ACTIVE_BOOKING_STATUSES  = [created, pending, pending_host, confirmed, requires_capture, ongoing]
 *   TERMINAL_BOOKING_STATUSES = [cancelled, completed, refunded, expired]
 *
 * These ACTIVE statuses block availability; TERMINAL statuses free the slot.
 */
enum class BookingStatus {
    PENDING,
    PENDING_HOST,
    CREATED,
    CONFIRMED,
    REQUIRES_CAPTURE,
    ONGOING,
    CANCELLED,
    COMPLETED,
    REFUNDED,
    EXPIRED,
    REJECTED;

    /** True if this status blocks availability (backend ACTIVE_BOOKING_STATUSES). */
    val isActive: Boolean get() = this in ACTIVE_STATUSES

    /** True if this status is terminal and frees the slot. */
    val isTerminal: Boolean get() = this in TERMINAL_STATUSES

    /** Display label for UI. */
    val displayLabel: String get() = when (this) {
        PENDING -> "Pending"
        PENDING_HOST -> "Awaiting Host"
        CREATED -> "Created"
        CONFIRMED -> "Confirmed"
        REQUIRES_CAPTURE -> "Processing"
        ONGOING -> "Ongoing"
        CANCELLED -> "Cancelled"
        COMPLETED -> "Completed"
        REFUNDED -> "Refunded"
        EXPIRED -> "Expired"
        REJECTED -> "Rejected"
    }

    companion object {
        /** Matches backend ACTIVE_BOOKING_STATUSES — these block availability. */
        val ACTIVE_STATUSES = setOf(CREATED, PENDING, PENDING_HOST, CONFIRMED, REQUIRES_CAPTURE, ONGOING)

        /** Matches backend TERMINAL_BOOKING_STATUSES — these free the slot. */
        val TERMINAL_STATUSES = setOf(CANCELLED, COMPLETED, REFUNDED, EXPIRED)

        fun fromString(status: String?): BookingStatus {
            if (status.isNullOrBlank()) return PENDING
            // Direct enum match
            entries.find { it.name.equals(status, ignoreCase = true) }?.let { return it }
            // Map server-side status variants to enum values
            val s = status.trim().lowercase()
            return when {
                // Backend exact matches first
                s == "pending_host" -> PENDING_HOST
                s == "requires_capture" -> REQUIRES_CAPTURE
                s == "created" -> CREATED
                s == "ongoing" -> ONGOING
                s == "refunded" || s == "refunded and cancelled" -> REFUNDED
                s == "expired" -> EXPIRED
                // Confirmed variants
                s == "accepted" || s == "confirmed" || s == "active" ||
                    s == "booked" || s == "paid" || s == "succeeded" ||
                    s == "captured" -> CONFIRMED
                // Rejected
                s == "declined" || s == "rejected" -> REJECTED
                // Cancelled variants
                s.contains("cancel") -> CANCELLED
                s.contains("refund") -> REFUNDED
                s == "failed" || s == "void" -> CANCELLED
                // Completed variants
                s == "completed" || s.contains("finish") -> COMPLETED
                // Pending variants
                s.contains("pending") || s.contains("await") -> PENDING
                else -> PENDING
            }
        }
    }
}

data class BookingModel(
    val id: String = "",
    val userProfilePic: Int? = null,
    val userName: String? = null,
    val checkOutTime: String? = null,
    val checkInTime: String? = null,
    val bookingStatus: String? = null,
    val price: String? = null,
    // Properties needed by screens
    val propertyImage: String? = null,
    val propertyName: String = "",
    val hostName: String = "",
    val currency: String = "USD",
    val totalPrice: Double = 0.0,
    val startDate: String = "",
    val endDate: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    // Additional fields used by BookingTabScreen
    val propertyId: String = "",
    val userId: String = "",
    val hostId: String = "",
    val guestCount: Int = 1,
    val createdAt: String = "",
    val updatedAt: String = "",
    // iOS PR #202 parity: verification PIN for guest/host check-in
    val verificationPin: String? = null,
    val pinStatus: String? = null,
    // Optional payment breakdown — only populated when the booking-detail
    // backend response carries per-line amounts.  When all null the UI
    // falls back to rendering Total only (current behavior).  No PDF /
    // invoice endpoint exists; these are the line items captured from
    // the booking document itself if present.
    val subtotal: Double? = null,
    val serviceFee: Double? = null,
    val cleaningFee: Double? = null,
    val taxAmount: Double? = null
)
