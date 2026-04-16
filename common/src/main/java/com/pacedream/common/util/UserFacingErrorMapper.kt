package com.pacedream.common.util

/**
 * Centralized error mapper that converts raw exceptions and error messages
 * into user-friendly strings safe for display in the UI.
 *
 * ## Why this exists
 * ViewModels and repositories must NEVER surface raw `exception.message`,
 * `error.message`, or HTTP response details to users. This object provides
 * a single place to map any Throwable into a friendly, non-technical message
 * while preserving the raw detail for logging/analytics.
 *
 * ## Usage
 * ```kotlin
 * // In a ViewModel catch block:
 * val userMessage = UserFacingErrorMapper.map(exception)
 * _uiState.value = _uiState.value.copy(error = userMessage)
 * Timber.e(exception, "Technical detail for logs only")
 * ```
 */
object UserFacingErrorMapper {

    // ── Generic mapping ────────────────────────────────────────

    /**
     * Map any [Throwable] to a user-friendly message.
     * [fallback] is used only when no pattern matches.
     */
    fun map(error: Throwable?, fallback: String = DEFAULT_MESSAGE): String {
        if (error == null) return fallback
        val raw = error.message ?: return fallback
        return classifyMessage(raw, fallback)
    }

    /**
     * Sanitise an already-extracted error string.
     * Use this when you already have `error.message` as a String rather than
     * a Throwable reference.
     */
    fun mapMessage(raw: String?, fallback: String = DEFAULT_MESSAGE): String {
        if (raw.isNullOrBlank()) return fallback
        return classifyMessage(raw, fallback)
    }

    // ── Context-specific helpers ───────────────────────────────

    fun forLogin(error: Throwable?): String = map(error, "We couldn't sign you in. Please try again.")
    fun forRegistration(error: Throwable?): String = map(error, "We couldn't create your account. Please try again.")
    fun forGoogleLogin(error: Throwable?): String = map(error, "Google sign-in didn't complete. Please try again.")
    fun forAppleLogin(error: Throwable?): String = map(error, "Apple sign-in didn't complete. Please try again.")
    fun forPasswordReset(error: Throwable?): String = map(error, "We couldn't send the reset link. Please try again.")
    fun forProfileUpdate(error: Throwable?): String = map(error, "We couldn't save your profile. Please try again.")
    fun forBookingCreate(error: Throwable?): String = map(error, "We couldn't complete your booking. Please try again.")
    fun forBookingCancel(error: Throwable?): String = map(error, "We couldn't cancel this booking. Please try again.")
    fun forBookingConfirm(error: Throwable?): String = map(error, "We couldn't confirm this booking. Please try again.")
    fun forLoadBookings(error: Throwable?): String = map(error, "We couldn't load your bookings right now. Please try again.")
    fun forLoadMessages(error: Throwable?): String = map(error, "We couldn't load messages. Please try again.")
    fun forSendMessage(error: Throwable?): String = map(error, "Your message couldn't be sent. Please try again.")
    fun forUploadMedia(error: Throwable?): String = map(error, "We couldn't upload your photos. Please try again.")
    fun forLoadNotifications(error: Throwable?): String = map(error, "We couldn't load notifications right now. Please try again.")
    fun forLoadWishlist(error: Throwable?): String = map(error, "We couldn't load your wishlist. Please try again.")
    fun forPaymentMethods(error: Throwable?): String = map(error, "We couldn't load your payment methods. Please try again.")
    fun forLoadProperties(error: Throwable?): String = map(error, "We couldn't load listings right now. Please try again.")

    // ── Internal classification ────────────────────────────────

    private fun classifyMessage(raw: String, fallback: String): String {
        val lower = raw.lowercase()
        return when {
            // Network / connectivity
            lower.contains("no internet") || lower.contains("unable to resolve") ||
                lower.contains("unknownhost") || lower.contains("no address associated") ||
                lower.contains("network is unreachable") ->
                "Please check your internet connection and try again."

            lower.contains("timeout") || lower.contains("timed out") ->
                "The connection timed out. Please try again."

            lower.contains("ssl") || lower.contains("secure connection") ||
                lower.contains("certificate") || lower.contains("handshake") ->
                "A secure connection couldn't be established. Please try again."

            lower.contains("connect") && lower.contains("fail") ||
                lower.contains("connection reset") || lower.contains("connection refused") ||
                lower.contains("unable to reach") ->
                "We couldn't reach the server. Please check your connection and try again."

            lower.contains("socket") || lower.contains("broken pipe") ||
                lower.contains("eof") || lower.contains("stream") && lower.contains("reset") ->
                "The connection was interrupted. Please try again."

            // Rate limiting
            lower.contains("429") || lower.contains("rate") && lower.contains("limit") ||
                lower.contains("too many request") || lower.contains("too many attempt") ->
                "Too many attempts. Please wait a moment and try again."

            // Auth / session
            lower.contains("unauthorized") || lower.contains("401") ||
                lower.contains("session expired") || lower.contains("token") && lower.contains("expired") ->
                "Your session has expired. Please sign in again."

            lower.contains("forbidden") || lower.contains("403") ->
                "You don't have permission to perform this action."

            // Account restrictions / security
            lower.contains("restricted") || lower.contains("suspended") ->
                "Your account is currently restricted. Please contact support."

            lower.contains("fraud") || lower.contains("blocked") && !lower.contains("ad") ->
                "This action has been blocked for security reasons. Please contact support."

            lower.contains("review") && lower.contains("paused") ||
                lower.contains("under review") ->
                "This action is paused while under review. We'll notify you when it's resolved."

            lower.contains("spam") ->
                "This action was blocked. Please contact support if you believe this is an error."

            // Server errors
            lower.contains("500") || lower.contains("internal server") ->
                "Something went wrong on our end. Please try again."

            lower.contains("502") || lower.contains("503") || lower.contains("504") ||
                lower.contains("service unavailable") || lower.contains("bad gateway") ->
                "Our service is temporarily unavailable. Please try again in a moment."

            // Parsing / decoding (never show to user)
            lower.contains("json") || lower.contains("parse") || lower.contains("decode") ||
                lower.contains("serializ") || lower.contains("unexpected char") ||
                lower.contains("missing field") || lower.contains("expected") && lower.contains("found") ->
                "Something went wrong. Please try again."

            // Null / empty response (never show to user)
            lower.contains("null") && lower.contains("response") ||
                lower.contains("empty") && lower.contains("response") ||
                lower.contains("null response") || lower.contains("empty body") ->
                "We didn't get a valid response. Please try again."

            // HTTP-specific patterns that should never leak
            lower.startsWith("http ") || lower.matches(Regex("^\\d{3}\\b.*")) ||
                lower.contains("response.message") || lower.contains("status code") ->
                "Something went wrong. Please try again."

            // "Failed to X: <technical detail>" — strip the technical suffix
            lower.startsWith("failed to") && lower.contains(":") -> {
                val prefix = raw.substringBefore(":").trim()
                // Only use the prefix if it's a reasonable user message
                if (prefix.length in 10..80) "$prefix. Please try again." else fallback
            }

            // API client not initialized — config error, not user's fault
            lower.contains("api client") || lower.contains("not initialized") ||
                lower.contains("not configured") ->
                "Something went wrong with the app setup. Please restart the app."

            // Cancellation — not an error
            lower.contains("cancel") && (lower.contains("user") || lower.contains("login")) ->
                "Sign-in was cancelled."

            // If the message already looks user-friendly (no technical indicators), pass it through
            else -> {
                if (looksUserFriendly(raw)) raw else fallback
            }
        }
    }

    /**
     * Heuristic: does this string look safe to show to a real user?
     * Returns false if it contains technical indicators.
     */
    private fun looksUserFriendly(msg: String): Boolean {
        val lower = msg.lowercase()
        val technicalIndicators = listOf(
            "exception", "error:", "null", "http ", "status",
            "stacktrace", "at com.", "at java.", "at kotlin.",
            "caused by", ".kt:", ".java:", "retrofit",
            "okhttp", "gson", "moshi", "kotlinx.serialization",
            "firebase", "io.ktor", "javax.", "java.net.",
            "java.io.", "android.", "org.json", "npx", "npm",
            "{", "}", "[", "]", "\\n", "response body",
            "endpoint", "api/", "/v1/", "/v2/", "base_url",
            "bearer", "token=", "key=", "secret="
        )
        return technicalIndicators.none { lower.contains(it) }
    }

    // ── Constants ──────────────────────────────────────────────

    const val DEFAULT_MESSAGE = "Something went wrong. Please try again."
    const val NETWORK_MESSAGE = "Please check your internet connection and try again."
    const val SESSION_EXPIRED_MESSAGE = "Your session has expired. Please sign in again."
}
