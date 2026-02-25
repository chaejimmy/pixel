package com.shourov.apps.pacedream.core.network.model

import com.google.gson.annotations.SerializedName

// ── Generic API response wrappers ────────────────────────────────────────────

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: T? = null
) {
    val isSuccessful: Boolean get() = success || ok
}

data class ApiListResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: List<T> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20
)

// ── Property models ──────────────────────────────────────────────────────────

data class PropertyResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("location") val location: PropertyLocationResponse? = null,
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("reviewCount") val reviewCount: Int = 0,
    @SerializedName("amenities") val amenities: List<String> = emptyList(),
    @SerializedName("propertyType") val propertyType: String = "",
    @SerializedName("isAvailable") val isAvailable: Boolean = true,
    @SerializedName("bedrooms") val bedrooms: Int = 0,
    @SerializedName("bathrooms") val bathrooms: Int = 0,
    @SerializedName("maxGuests") val maxGuests: Int = 1,
    @SerializedName("hostId") val hostId: String = "",
    @SerializedName("createdAt") val createdAt: String = "",
    @SerializedName("updatedAt") val updatedAt: String = ""
)

data class PropertyLocationResponse(
    @SerializedName("city") val city: String = "",
    @SerializedName("country") val country: String = "",
    @SerializedName("address") val address: String = "",
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0
)

// ── Category & Destination ───────────────────────────────────────────────────

data class CategoryResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("icon") val icon: String = "",
    @SerializedName("count") val count: Int = 0
)

data class DestinationResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("imageUrl") val imageUrl: String = "",
    @SerializedName("propertyCount") val propertyCount: Int = 0
)

// ── Wishlist ─────────────────────────────────────────────────────────────────

data class WishlistItemResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("propertyId") val propertyId: String = "",
    @SerializedName("property") val property: PropertyResponse? = null,
    @SerializedName("createdAt") val createdAt: String = ""
)

// ── Booking ──────────────────────────────────────────────────────────────────

data class BookingResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("propertyId") val propertyId: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("hostId") val hostId: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("startDate") val startDate: String = "",
    @SerializedName("endDate") val endDate: String = "",
    @SerializedName("totalPrice") val totalPrice: Double = 0.0,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("guestCount") val guestCount: Int = 1,
    @SerializedName("propertyName") val propertyName: String = "",
    @SerializedName("propertyImage") val propertyImage: String = "",
    @SerializedName("hostName") val hostName: String = "",
    @SerializedName("createdAt") val createdAt: String = "",
    @SerializedName("updatedAt") val updatedAt: String = ""
)

data class BookingAvailabilityResponse(
    @SerializedName("available") val available: Boolean = false,
    @SerializedName("unavailableDates") val unavailableDates: List<String> = emptyList()
)

// ── Chat / Messaging ─────────────────────────────────────────────────────────

data class ChatResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("participants") val participants: List<String> = emptyList(),
    @SerializedName("lastMessage") val lastMessage: String = "",
    @SerializedName("lastMessageTime") val lastMessageTime: String = "",
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("otherUserName") val otherUserName: String = "",
    @SerializedName("otherUserAvatar") val otherUserAvatar: String = ""
)

data class MessageResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("chatId") val chatId: String = "",
    @SerializedName("senderId") val senderId: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("createdAt") val createdAt: String = "",
    @SerializedName("isRead") val isRead: Boolean = false
)

// ── Notification ─────────────────────────────────────────────────────────────

data class NotificationResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("createdAt") val createdAt: String = ""
)

// ── Payment ──────────────────────────────────────────────────────────────────

data class PaymentIntentResponse(
    @SerializedName("clientSecret") val clientSecret: String = "",
    @SerializedName("paymentIntentId") val paymentIntentId: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("currency") val currency: String = "USD"
)

data class PaymentHistoryResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("status") val status: String = "",
    @SerializedName("bookingId") val bookingId: String = "",
    @SerializedName("createdAt") val createdAt: String = ""
)

data class PaymentMethodResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("last4") val last4: String = "",
    @SerializedName("brand") val brand: String = "",
    @SerializedName("expiryMonth") val expiryMonth: Int = 0,
    @SerializedName("expiryYear") val expiryYear: Int = 0,
    @SerializedName("isDefault") val isDefault: Boolean = false
)

// ── Reviews ──────────────────────────────────────────────────────────────────

data class ReviewResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("propertyId") val propertyId: String = "",
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("comment") val comment: String = "",
    @SerializedName("userName") val userName: String = "",
    @SerializedName("userAvatar") val userAvatar: String = "",
    @SerializedName("createdAt") val createdAt: String = ""
)

// ── Collections ──────────────────────────────────────────────────────────────

data class CollectionResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("items") val items: List<CollectionItemResponse> = emptyList(),
    @SerializedName("createdAt") val createdAt: String = ""
)

data class CollectionItemResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("propertyId") val propertyId: String = "",
    @SerializedName("addedAt") val addedAt: String = ""
)

// ── Host ─────────────────────────────────────────────────────────────────────

data class HostListingResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("bookingCount") val bookingCount: Int = 0,
    @SerializedName("earnings") val earnings: Double = 0.0,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("propertyType") val propertyType: String = "",
    @SerializedName("location") val location: PropertyLocationResponse? = null,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("currency") val currency: String = "USD"
)

data class HostEarningsResponse(
    @SerializedName("totalEarnings") val totalEarnings: Double = 0.0,
    @SerializedName("pendingPayout") val pendingPayout: Double = 0.0,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("transactions") val transactions: List<HostTransactionResponse> = emptyList()
)

data class HostTransactionResponse(
    @SerializedName("_id") val id: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("type") val type: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("bookingId") val bookingId: String = "",
    @SerializedName("createdAt") val createdAt: String = ""
)

data class HostAnalyticsResponse(
    @SerializedName("totalViews") val totalViews: Int = 0,
    @SerializedName("totalBookings") val totalBookings: Int = 0,
    @SerializedName("occupancyRate") val occupancyRate: Double = 0.0,
    @SerializedName("averageRating") val averageRating: Double = 0.0,
    @SerializedName("revenue") val revenue: Double = 0.0,
    @SerializedName("currency") val currency: String = "USD"
)

// ── Analytics ────────────────────────────────────────────────────────────────

data class AnalyticsEventResponse(
    @SerializedName("success") val success: Boolean = false
)
