package com.shourov.apps.pacedream.feature.notification

import android.content.Intent
import com.shourov.apps.pacedream.feature.wifi.WifiSessionRouter
import com.shourov.apps.pacedream.feature.wifi.WifiSessionRouter.Intent as WifiIntent
import com.shourov.apps.pacedream.navigation.DashboardDestination
import com.shourov.apps.pacedream.navigation.BookingDestination
import com.shourov.apps.pacedream.navigation.InboxDestination
import com.shourov.apps.pacedream.navigation.NavigationRouter
import com.shourov.apps.pacedream.navigation.PropertyDestination
import com.shourov.apps.pacedream.navigation.TabRouter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Push notification routing matching iOS NotificationRouter.swift (iOS parity).
 *
 * Routes push notification payloads to the correct in-app screen based on the
 * `screen` key in the notification data. Falls back to notification type-based
 * routing and then deep link URL routing if no screen key is found.
 *
 * Uses [TabRouter] for tab switching and [NavigationRouter] for in-tab navigation,
 * both of which are observed by the Dashboard composable.
 *
 * Supported screens (full iOS parity):
 * - chat / messages -> Inbox tab, optionally open specific thread
 * - booking_detail / checkin / checkout / extend / receipt -> Bookings tab + detail
 * - listing_detail / edit / calendar -> Home tab + listing detail
 * - payment_methods / payment_history / payout_* -> Profile tab
 * - split_detail / split_payment -> Bookings tab + split detail
 * - write_review / review_detail / reviews -> respective screens
 * - verification / host_onboarding / host_dashboard -> Profile/Home tab
 * - dispute_detail / support / support_detail -> Support screen
 * - deals / safety_alert / explore -> Home tab
 * - wishlist / favorites -> Favorites tab
 * - profile / edit_profile / security_settings -> Profile tab
 */
object NotificationRouter {

    private val scope = MainScope()

    /**
     * Route a push notification payload to the correct screen.
     *
     * @param data The notification data payload (key-value map)
     */
    fun handleNotification(data: Map<String, String>) {
        // Extract the screen value
        val screen = data["screen"]
            ?: data["notification_type"]

        // 1. Screen-based routing (iOS parity: data.screen)
        if (!screen.isNullOrBlank()) {
            routeToScreen(screen, data)
            return
        }

        // 2. Thread-based routing (messaging notifications)
        val threadId = data["threadId"]
            ?: data["thread_id"]
            ?: data["conversationId"]
            ?: data["conversation_id"]
            ?: data["chat_id"]

        if (!threadId.isNullOrBlank()) {
            scope.launch {
                TabRouter.switchTo(DashboardDestination.INBOX)
                NavigationRouter.navigateTo("${InboxDestination.THREAD.name}/$threadId")
            }
            return
        }

        // 3. Type-based routing fallback
        val type = data["type"]
        if (!type.isNullOrBlank()) {
            routeByType(type, data)
            return
        }

        // 4. Deep link URL routing
        val deepLink = data["deep_link"] ?: data["deepLink"]
        if (!deepLink.isNullOrBlank()) {
            Timber.d("NotificationRouter: deep link routing to $deepLink")
            // Deep links are handled by MainActivity's existing DeepLinkHandler
        }
    }

    /**
     * Route from an Android Intent (notification tap).
     * Extracts extras and delegates to handleNotification.
     */
    fun handleIntent(intent: Intent): Boolean {
        if (!intent.getBooleanExtra("from_notification", false)) return false

        val data = mutableMapOf<String, String>()
        intent.extras?.let { extras ->
            for (key in extras.keySet()) {
                extras.getString(key)?.let { data[key] = it }
            }
        }

        if (data.isEmpty()) return false

        Timber.d("NotificationRouter: handling intent with keys=${data.keys}")
        handleNotification(data)
        return true
    }

    private fun routeToScreen(screen: String, data: Map<String, String>) {
        Timber.d("NotificationRouter: routing to screen=$screen")

        val bookingId = data["bookingId"] ?: data["booking_id"]
        val propertyId = data["propertyId"] ?: data["property_id"]
            ?: data["listingId"] ?: data["listing_id"]
        val threadId = data["threadId"] ?: data["thread_id"]
            ?: data["conversationId"] ?: data["conversation_id"]
            ?: data["chat_id"]
        val wifiSessionId = data["wifiSessionId"] ?: data["wifi_session_id"]
        val wifiExpiresAt = data["wifiExpiresAt"] ?: data["wifi_expires_at"]
            ?: data["expires_at"] ?: data["expiresAt"]
        val wifiSsid = data["wifiSsid"] ?: data["wifi_ssid"] ?: data["ssid"]

        scope.launch {
            when (screen.lowercase()) {
                // ── Chat / Messages (iOS parity) ──────────────
                "chat", "messages", "message", "new_message" -> {
                    TabRouter.switchTo(DashboardDestination.INBOX)
                    if (!threadId.isNullOrBlank()) {
                        NavigationRouter.navigateTo("${InboxDestination.THREAD.name}/$threadId")
                    }
                }

                // ── Bookings (iOS parity) ─────────────────────
                "booking_detail", "booking", "booking_checkin", "booking_checkout",
                "booking_extend", "booking_receipt", "checkin", "checkout",
                "extend", "receipt", "booking_confirmed",
                "booking_cancelled", "booking_reminder" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                    if (!bookingId.isNullOrBlank()) {
                        NavigationRouter.navigateTo("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                    }
                }

                // ── Listing / Property (iOS parity) ───────────
                "listing_detail", "listing", "property",
                "listing_edit", "listing_calendar",
                "new_review", "price_change" -> {
                    TabRouter.switchTo(DashboardDestination.HOME)
                    if (!propertyId.isNullOrBlank()) {
                        NavigationRouter.navigateTo("${PropertyDestination.DETAIL.name}/$propertyId")
                    }
                }

                // ── Payment / Payout (iOS parity) ─────────────
                "payment_methods", "payment_history",
                "payout_history", "payout_settings",
                "payment_received", "payment_failed" -> {
                    TabRouter.switchTo(DashboardDestination.PROFILE)
                }

                // ── Split Booking (iOS parity) ────────────────
                "split_detail", "split_payment", "split_invite" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                    if (!bookingId.isNullOrBlank()) {
                        NavigationRouter.navigateTo("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                    }
                }

                // ── Reviews (iOS parity) ──────────────────────
                "write_review", "review_detail", "reviews" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                }

                // ── Profile / Settings / Security (iOS parity) ─
                "profile", "edit_profile", "security_settings",
                "security_activity", "account_status",
                "verification", "verification_status",
                "verification_upload" -> {
                    TabRouter.switchTo(DashboardDestination.PROFILE)
                }

                // ── Host (iOS parity) ─────────────────────────
                "host_onboarding", "host_dashboard", "host_verification" -> {
                    TabRouter.switchTo(DashboardDestination.HOME)
                }

                // ── Disputes / Support (iOS parity) ───────────
                "dispute_detail", "support", "support_detail", "help" -> {
                    NavigationRouter.navigateTo("support")
                }

                // ── Marketing / Explore (iOS parity) ──────────
                "deals", "explore", "safety_alert", "promotion" -> {
                    TabRouter.switchTo(DashboardDestination.HOME)
                }

                // ── Favorites (iOS parity) ────────────────────
                "wishlist", "favorites" -> {
                    TabRouter.switchTo(DashboardDestination.FAVORITES)
                }

                // ── Wi-Fi access session ──────────────────────
                // Wi-Fi UI is a shell-level overlay (pill + sheet), not a tab.
                // We seed the session in the shared router; the WifiSessionHost
                // composable observes intents and presents the right surface.
                "wifi", "wifi_access", "wifi_access_started" -> {
                    WifiSessionRouter.dispatch(
                        WifiIntent.Start(
                            sessionId = wifiSessionId,
                            ssid = wifiSsid,
                            expiresAtIso = wifiExpiresAt,
                            bookingId = bookingId
                        )
                    )
                }
                "wifi_extend_prompt" -> {
                    WifiSessionRouter.dispatch(
                        WifiIntent.ShowExtend(sessionId = wifiSessionId)
                    )
                }
                "wifi_session_expired" -> {
                    WifiSessionRouter.dispatch(
                        WifiIntent.ShowExpired(sessionId = wifiSessionId)
                    )
                }
                "wifi_extension_confirmed" -> {
                    WifiSessionRouter.dispatch(
                        WifiIntent.Refresh(sessionId = wifiSessionId)
                    )
                }

                else -> {
                    Timber.w("NotificationRouter: unhandled screen=$screen")
                }
            }
        }
    }

    /**
     * Route based on notification type when no screen key is provided.
     */
    private fun routeByType(type: String, data: Map<String, String>) {
        Timber.d("NotificationRouter: routing by type=$type")

        val bookingId = data["bookingId"] ?: data["booking_id"]
        val threadId = data["threadId"] ?: data["thread_id"]
            ?: data["chat_id"]
        val wifiSessionId = data["wifiSessionId"] ?: data["wifi_session_id"]
        val wifiExpiresAt = data["wifiExpiresAt"] ?: data["wifi_expires_at"]
            ?: data["expires_at"] ?: data["expiresAt"]
        val wifiSsid = data["wifiSsid"] ?: data["wifi_ssid"] ?: data["ssid"]

        scope.launch {
            when (type.lowercase()) {
                "message", "message_received" -> {
                    TabRouter.switchTo(DashboardDestination.INBOX)
                    if (!threadId.isNullOrBlank()) {
                        NavigationRouter.navigateTo("${InboxDestination.THREAD.name}/$threadId")
                    }
                }

                "booking", "booking_request", "booking_confirmed",
                "booking_cancelled", "booking_receipt", "booking_refund",
                "checkin_reminder", "extend_prompt", "overtime_warning",
                "session_ended", "split_invite", "split_payment_needed",
                "split_completed", "split_credit" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                    if (!bookingId.isNullOrBlank()) {
                        NavigationRouter.navigateTo("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                    }
                }

                "payment_received", "payment_failed",
                "payout_initiated", "payout_failed",
                "chargeback_created", "chargeback_resolved" -> {
                    TabRouter.switchTo(DashboardDestination.PROFILE)
                }

                "review_received" -> {
                    TabRouter.switchTo(DashboardDestination.BOOKINGS)
                }

                "friend_request", "friend_accepted",
                "roommate_request", "roommate_accepted" -> {
                    TabRouter.switchTo(DashboardDestination.INBOX)
                }

                "property_approved", "property_rejected",
                "listing_paused", "listing_reported",
                "listing_inquiry" -> {
                    TabRouter.switchTo(DashboardDestination.HOME)
                }

                "security_alert", "verification_status",
                "account_warning", "account_suspended" -> {
                    TabRouter.switchTo(DashboardDestination.PROFILE)
                }

                "marketing" -> {
                    TabRouter.switchTo(DashboardDestination.HOME)
                }

                "wifi_access_started" -> WifiSessionRouter.dispatch(
                    WifiIntent.Start(
                        sessionId = wifiSessionId,
                        ssid = wifiSsid,
                        expiresAtIso = wifiExpiresAt,
                        bookingId = bookingId
                    )
                )
                "wifi_10min_left", "wifi_3min_left" -> WifiSessionRouter.dispatch(
                    WifiIntent.ShowExtend(sessionId = wifiSessionId)
                )
                "wifi_expired" -> WifiSessionRouter.dispatch(
                    WifiIntent.ShowExpired(sessionId = wifiSessionId)
                )
                "wifi_extension_confirmed" -> WifiSessionRouter.dispatch(
                    WifiIntent.Refresh(sessionId = wifiSessionId)
                )

                else -> {
                    Timber.w("NotificationRouter: unhandled type=$type")
                }
            }
        }
    }
}
