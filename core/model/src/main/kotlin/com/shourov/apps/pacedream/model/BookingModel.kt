package com.shourov.apps.pacedream.model

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
    val status: String = "PENDING"
)
