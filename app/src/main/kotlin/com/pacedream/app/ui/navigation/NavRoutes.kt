package com.pacedream.app.ui.navigation

/**
 * Navigation routes for the app
 * 
 * Tab routes are the 5 main bottom navigation destinations.
 * Sub-routes are nested screens within each tab.
 */
object NavRoutes {
    // Main tab routes (stable, never disappear)
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val BOOKINGS = "bookings"
    const val INBOX = "inbox"
    const val PROFILE = "profile"
    
    // Home sub-routes
    const val HOME_SECTION_LIST = "home/section/{sectionType}"
    const val LISTING_DETAIL = "listing/{listingId}"
    
    // Wishlist sub-routes
    const val WISHLIST_ITEM_DETAIL = "wishlist/item/{itemId}"
    
    // Inbox sub-routes
    const val THREAD_DETAIL = "inbox/thread/{threadId}"
    
    // Profile sub-routes
    const val HOST_HOME = "profile/host"
    const val HOST_LISTINGS = "profile/host/listings"
    const val EDIT_PROFILE = "profile/edit"
    const val SETTINGS = "profile/settings"
    const val FAQ = "profile/faq"
    
    // Webflow / Booking routes
    const val BOOKING_CONFIRMATION_TIMEBASED = "booking/confirmation/timebased"
    const val BOOKING_CONFIRMATION_GEAR = "booking/confirmation/gear"
    const val BOOKING_CANCELLED = "booking/cancelled"
    const val BOOKING_DETAIL = "bookingDetail/{bookingId}"

    // Native booking flow (Listing Detail → ReserveSheet → Checkout → Confirmation)
    const val CHECKOUT = "checkout/{listingId}"
    const val CONFIRMATION = "confirmation/{bookingId}"
    
    // Deep link routes
    const val DEEP_LINK_BOOKING_SUCCESS = "booking-success"
    const val DEEP_LINK_BOOKING_CANCELLED = "booking-cancelled"
    
    // Helper functions for parameterized routes
    fun homeSectionList(sectionType: String) = "home/section/$sectionType"
    fun listingDetail(listingId: String) = "listing/$listingId"
    fun threadDetail(threadId: String) = "inbox/thread/$threadId"
    fun bookingDetail(bookingId: String) = "bookingDetail/$bookingId"
    fun wishlistItemDetail(itemId: String) = "wishlist/item/$itemId"
    fun confirmation(bookingId: String) = "confirmation/$bookingId"
    fun checkout(listingId: String) = "checkout/$listingId"
}

/**
 * Bottom navigation tab configuration
 */
enum class BottomNavTab(
    val route: String,
    val title: String,
    val iconResName: String // We'll use material icons
) {
    HOME(NavRoutes.HOME, "Home", "home"),
    SEARCH(NavRoutes.SEARCH, "Search", "search"),
    FAVORITES(NavRoutes.FAVORITES, "Favorites", "favorite"),
    INBOX(NavRoutes.INBOX, "Inbox", "mail"),
    PROFILE(NavRoutes.PROFILE, "Profile", "person")
}


