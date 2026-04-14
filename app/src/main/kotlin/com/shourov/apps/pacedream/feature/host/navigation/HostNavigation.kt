package com.shourov.apps.pacedream.feature.host.navigation

import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.ui.graphics.vector.ImageVector

sealed class HostScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : HostScreen("host_dashboard", "Dashboard", PaceDreamIcons.Dashboard)
    object Listings : HostScreen("host_listings", "My Listings", PaceDreamIcons.Home)
    object Bookings : HostScreen("host_bookings", "Bookings", PaceDreamIcons.CalendarToday)
    object Earnings : HostScreen("host_earnings", "Earnings", PaceDreamIcons.AttachMoney)
    object Analytics : HostScreen("host_analytics", "Analytics", PaceDreamIcons.Analytics)
    object Settings : HostScreen("host_settings", "Settings", PaceDreamIcons.Settings)
    object PaymentSetup : HostScreen("host_payment_setup", "Payment Setup", PaceDreamIcons.CreditCard)
    // iOS parity tabs
    object Post : HostScreen("host_post", "Post", PaceDreamIcons.AddCircle)
    object Inbox : HostScreen("host_inbox", "Messages", PaceDreamIcons.Mail)
    object Profile : HostScreen("host_profile", "Profile", PaceDreamIcons.Person)
    object Notifications : HostScreen("host_notifications", "Notifications", PaceDreamIcons.Notifications)
    object HelpSupport : HostScreen("host_help_support", "Help & Support", PaceDreamIcons.Help)
}

object HostNavigationDestinations {
    const val HOST_ROUTE = "host_route"
    const val HOST_DASHBOARD = "host_dashboard"
    const val HOST_LISTINGS = "host_listings"
    const val HOST_BOOKINGS = "host_bookings"
    const val HOST_EARNINGS = "host_earnings"
    const val HOST_ANALYTICS = "host_analytics"
    const val HOST_SETTINGS = "host_settings"
    const val HOST_PAYMENT_SETUP = "host_payment_setup"
    const val ADD_LISTING = "add_listing"
    const val EDIT_LISTING = "edit_listing/{listingId}"
    const val BOOKING_DETAILS = "booking_details/{bookingId}"
    const val LISTING_CALENDAR = "listing_calendar/{listingId}"
}
