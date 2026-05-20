package com.shourov.apps.pacedream.navigation

/**
 * Single source of truth for the listing-detail and booking-detail navigation
 * routes. Both destinations are registered on the top-level NavHost so they
 * are reachable from both host and guest contexts; using these helpers at
 * every callsite keeps the patterns aligned.
 */
object Routes {
    const val LISTING_ARG = "listingId"
    const val BOOKING_DETAIL_ARG = "bookingId"

    const val LISTING_PATTERN = "listing/{$LISTING_ARG}"
    const val BOOKING_DETAIL_PATTERN = "booking_details/{$BOOKING_DETAIL_ARG}"

    fun listing(id: String) = "listing/$id"
    fun bookingDetail(id: String) = "booking_details/$id"
}
