package com.shourov.apps.pacedream.navigation

enum class UserStartTopLevelDestination {
    ONBOARDING,
    SIGN_IN_WITH_PHONE,
    SIGN_IN_WITH_MAIL,
    ACCOUNT_SETUP,
}

enum class DashboardDestination {
    HOME,
    SEARCH,
    FAVORITES,  // Wishlist - matches iOS "Favorites" tab
    INBOX,
    PROFILE
}

enum class PropertyDestination {
    DETAIL,
    SEARCH,
    FILTER,
    CATEGORY_LIST,
    DESTINATION_LIST,
    RECENT_SEARCHES
}

enum class BookingDestination {
    BOOKING_FORM,
    BOOKING_CONFIRMATION,
    BOOKING_CANCELLED,
    BOOKING_HISTORY,
    BOOKING_DETAIL
}

enum class InboxDestination {
    THREAD
}

enum class ProfileDestination {
    EDIT_PROFILE,
    SETTINGS,
    HELP,
    ABOUT
}