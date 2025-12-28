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
            return entries.find { it.name.equals(status, ignoreCase = true) } ?: PENDING
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
    val updatedAt: String = ""
)
