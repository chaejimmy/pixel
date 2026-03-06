package com.shourov.apps.pacedream.feature.notification

import com.shourov.apps.pacedream.navigation.DashboardDestination
import com.shourov.apps.pacedream.navigation.BookingDestination
import com.shourov.apps.pacedream.navigation.InboxDestination
import com.shourov.apps.pacedream.navigation.PropertyDestination
import com.shourov.apps.pacedream.navigation.TabRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Push notification routing matching iOS NotificationRouter.swift.
 *
 * Routes push notification payloads to the correct in-app screen based on the
 * `screen` key in the notification data. Falls back to deep link URL routing
 * if no screen key is found.
 *
 * Supported screens (iOS parity):
 * - chat / messages -> Inbox tab, optionally open specific thread
 * - booking_detail / checkin / checkout -> Bookings tab + booking detail
 * - listing_detail / edit / calendar -> Home tab + listing detail
 * - payment_methods / payment_history -> Profile tab + payment screen
 * - split_detail / split_payment -> Bookings tab + split detail
 * - write_review / review_detail -> respective screens
 * - verification / host_onboarding -> Profile tab
 * - dispute_detail / support -> Support screen
 * - deals / safety_alert -> Home tab
 */
object NotificationRouter {

    /**
     * Route a push notification payload to the correct screen.
     * Call this from FirebaseMessagingService.onMessageReceived() when the app
     * is in the foreground, or from the pending-intent handler on cold start.
     *
     * @param data The notification data payload (key-value map)
     * @param navigateTo Callback to navigate to a specific route string
     */
    fun handleNotification(
        data: Map<String, String>,
        navigateTo: (String) -> Unit
    ) {
        // Extract the data dict (may be nested under "data" or "custom.a" keys)
        val screen = data["screen"]
            ?: data["notification_type"]
            ?: return

        Timber.d("NotificationRouter: routing to screen=$screen")

        // Extract common IDs with fallbacks for various naming conventions
        val threadId = data["threadId"]
            ?: data["thread_id"]
            ?: data["conversationId"]
            ?: data["conversation_id"]

        val bookingId = data["bookingId"]
            ?: data["booking_id"]

        val propertyId = data["propertyId"]
            ?: data["property_id"]
            ?: data["listingId"]
            ?: data["listing_id"]

        CoroutineScope(Dispatchers.Main).launch {
            when (screen.lowercase()) {
                // ── Chat / Messages ──────────────────────────────
                "chat", "messages", "message", "new_message" -> {
                    TabRouter.switchTo(DashboardDestination.INBOX)
                    if (threadId != null) {
                        navigateTo("${InboxDestination.THREAD.name}/$threadId")
                    }
                }

                // ── Booking ──────────────────────────────────────
                "booking_detail", "booking", "checkin", "checkout",
                "extend", "receipt", "booking_confirmed",
                "booking_cancelled", "booking_reminder" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                    if (bookingId != null) {
                        navigateTo("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                    }
                }

                // ── Listing / Property ───────────────────────────
                "listing_detail", "listing", "property",
                "listing_edit", "listing_calendar",
                "new_review", "price_change" -> {
                    TabRouter.switchTo(DashboardDestination.HOME)
                    if (propertyId != null) {
                        navigateTo("${PropertyDestination.DETAIL.name}/$propertyId")
                    }
                }

                // ── Payment ──────────────────────────────────────
                "payment_methods", "payment_history",
                "payout_history", "payout_settings",
                "payment_received", "payment_failed" -> {
                    TabRouter.switchTo(DashboardDestination.PROFILE)
                }

                // ── Split Booking ────────────────────────────────
                "split_detail", "split_payment", "split_invite" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                    if (bookingId != null) {
                        navigateTo("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                    }
                }

                // ── Reviews ──────────────────────────────────────
                "write_review", "review_detail" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                }

                // ── Profile / Settings ───────────────────────────
                "profile", "security_settings", "verification",
                "host_onboarding", "host_dashboard" -> {
                    TabRouter.switchTo(DashboardDestination.PROFILE)
                }

                // ── Support ──────────────────────────────────────
                "dispute_detail", "support", "help" -> {
                    navigateTo("support")
                }

                // ── General ──────────────────────────────────────
                "deals", "safety_alert", "promotion" -> {
                    TabRouter.switchTo(DashboardDestination.HOME)
                }

                // ── Favorites ────────────────────────────────────
                "wishlist", "favorites" -> {
                    TabRouter.switchTo(DashboardDestination.FAVORITES)
                }

                else -> {
                    Timber.w("NotificationRouter: unhandled screen=$screen")
                }
            }
        }
    }
}
