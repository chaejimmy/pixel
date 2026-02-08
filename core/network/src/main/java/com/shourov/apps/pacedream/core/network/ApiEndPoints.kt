package com.shourov.apps.pacedream.core.network

/**
 * API Endpoints - iOS parity
 *
 * Endpoints organized by domain matching the iOS networking layer.
 * All paths are relative to the base URL (/v1 is appended by AppConfig).
 */
object ApiEndPoints {

    // ── Auth ───────────────────────────────────────────────
    const val SEND_OTP = "auth/send-otp"
    const val AUTH_LOGIN_MOBILE = "auth/login/mobile"
    const val AUTH_LOGIN_EMAIL = "auth/login/email"
    const val AUTH_SIGNUP_EMAIL = "auth/signup/email"
    const val AUTH_SEND_EMAIL_CODE = "auth/send-email-code"
    const val AUTH_VERIFY_EMAIL = "auth/verify-email-code"
    const val AUTH_FORGOT_PASSWORD = "auth/forgot-password"
    const val AUTH_SIGNUP_MOBILE = "auth/signup/mobile"
    const val AUTH_REFRESH_TOKEN = "auth/refresh-token"
    const val AUTH_AUTH0_CALLBACK = "auth/auth0/callback"
    const val AUTH_LOGOUT = "auth/logout"

    // ── User / Account ────────────────────────────────────
    const val ACCOUNT_ME = "account/me"
    const val USER_GET_PROFILE = "user/get/profile"
    const val UPDATE_USER_PROFILE = "user/profile"
    const val USER_UPLOAD_AVATAR = "user/profile/avatar"
    const val USER_DELETE_ACCOUNT = "user/account"

    // ── Properties ────────────────────────────────────────
    const val GET_ALL_PROPERTIES = "property/get-all-properties"
    const val GET_PROPERTY_BY_ID = "properties/{propertyId}"
    const val SEARCH_PROPERTIES = "properties/search"
    const val GET_PROPERTY_CATEGORIES = "properties/categories"
    const val GET_POPULAR_DESTINATIONS = "properties/destinations"
    const val GET_FEATURED_PROPERTIES = "properties/featured"
    const val GET_NEARBY_PROPERTIES = "properties/nearby"
    const val GET_TIMEBASE_ROOM = "properties/filter-rentable-items-by-group/time_based"

    // ── Wishlist / Favorites (iOS parity) ─────────────────
    const val GET_WISHLIST = "wishlist"
    const val ADD_TO_WISHLIST = "wishlist"
    const val REMOVE_FROM_WISHLIST = "wishlist/{propertyId}"

    // ── Bookings ──────────────────────────────────────────
    const val CREATE_BOOKING = "bookings"
    const val GET_USER_BOOKINGS = "bookings/user/{userId}"
    const val GET_BOOKING_BY_ID = "bookings/{bookingId}"
    const val UPDATE_BOOKING = "bookings/{bookingId}"
    const val CANCEL_BOOKING = "bookings/{bookingId}"
    const val CONFIRM_BOOKING = "bookings/{bookingId}/confirm"
    const val GET_BOOKING_AVAILABILITY = "bookings/availability/{propertyId}"

    // ── Messaging / Chat ──────────────────────────────────
    const val GET_USER_CHATS = "chats/user/{userId}"
    const val GET_CHAT_MESSAGES = "chats/{chatId}/messages"
    const val CREATE_CHAT = "chats"
    const val SEND_MESSAGE = "chats/{chatId}/messages"
    const val MARK_MESSAGE_READ = "chats/{chatId}/messages/{messageId}/read"

    // ── Notifications ─────────────────────────────────────
    const val GET_USER_NOTIFICATIONS = "notifications/user/{userId}"
    const val MARK_NOTIFICATION_READ = "notifications/{notificationId}/read"
    const val MARK_ALL_NOTIFICATIONS_READ = "notifications/user/{userId}/read-all"
    const val REGISTER_PUSH_TOKEN = "notifications/register-device"

    // ── Payments ──────────────────────────────────────────
    const val CREATE_PAYMENT_INTENT = "payments/create-intent"
    const val CONFIRM_PAYMENT = "payments/confirm"
    const val GET_PAYMENT_HISTORY = "payments/user/{userId}/history"
    const val GET_PAYMENT_METHODS = "payments/methods"

    // ── Reviews ───────────────────────────────────────────
    const val GET_USER_REVIEWS = "reviews"
    const val CREATE_REVIEW = "reviews"
    const val GET_PROPERTY_REVIEWS = "reviews/property/{propertyId}"
    const val GET_USER_REVIEWS_BY_ID = "reviews/user/{userId}"

    // ── Host (iOS parity) ─────────────────────────────────
    const val HOST_GET_LISTINGS = "host/listings"
    const val HOST_CREATE_LISTING = "host/listings"
    const val HOST_UPDATE_LISTING = "host/listings/{listingId}"
    const val HOST_DELETE_LISTING = "host/listings/{listingId}"
    const val HOST_GET_EARNINGS = "host/earnings"
    const val HOST_GET_BOOKINGS = "host/bookings"
    const val HOST_ACCEPT_BOOKING = "host/bookings/{bookingId}/accept"
    const val HOST_DECLINE_BOOKING = "host/bookings/{bookingId}/decline"
    const val HOST_GET_ANALYTICS = "host/analytics"

    // ── Verification ──────────────────────────────────────
    const val VERIFICATION_SEND_PHONE_CODE = "users/verification/phone/send-code"
    const val VERIFICATION_CONFIRM_PHONE = "users/verification/phone/confirm"
    const val VERIFICATION_STATUS = "users/verification"
    const val VERIFICATION_UPLOAD_URLS = "users/verification/upload-urls"
    const val VERIFICATION_SUBMIT = "users/verification/submit"

    // ── Analytics ─────────────────────────────────────────
    const val TRACK_EVENT = "analytics/event"
    const val TRACK_PROPERTY_VIEW = "analytics/property-view"

    // ── Legacy ────────────────────────────────────────────
    const val GET_ALREADY_BOOKED = ""
    const val HOURLY_RENTED_GEAR = "gear-rentals/get/hourly-rental-gear/{type}"
    const val ROOMMATE_ROOM_STAY = "roommate/get/room-stay"
}
