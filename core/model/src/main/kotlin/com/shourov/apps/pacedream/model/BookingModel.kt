package com.shourov.apps.pacedream.model

/**
 * Booking status enum matching both iOS and screen usage
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    REJECTED;
    
    companion object {
        fun fromString(status: String?): BookingStatus {
            if (status.isNullOrBlank()) return PENDING
            // Direct enum match
            entries.find { it.name.equals(status, ignoreCase = true) }?.let { return it }
            // Map server-side status variants to enum values
            val s = status.lowercase()
            return when {
                s == "accepted" || s == "confirmed" -> CONFIRMED
                s == "declined" || s == "rejected" -> REJECTED
                s.contains("cancel") || s.contains("refund") -> CANCELLED
                s == "completed" || s.contains("finish") -> COMPLETED
                s == "created" || s == "pending_host" || s == "requires_capture" ||
                    s.contains("pending") || s.contains("await") || s == "ongoing" -> PENDING
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
    val pinStatus: String? = null
)
