package com.pacedream.common.util

/**
 * Safely map Stripe SDK errors to user-friendly messages without leaking
 * raw SDK strings.
 *
 * ## Why this exists
 * Stripe SDK exceptions (`StripeException`, `CardException`, etc.) expose
 * `localizedMessage` that sometimes contains useful card-specific feedback
 * ("Your card was declined") but may also contain technical detail
 * ("Sending credit card numbers directly to the Stripe API is generally
 * unsafe"). Rather than passing the raw message through or suppressing it
 * entirely, this mapper extracts the error *code* where possible and
 * returns a curated, app-controlled message for each whitelisted code.
 *
 * ## Usage
 * ```kotlin
 * // UI layer:
 * is PaymentSheetResult.Failed -> {
 *     val userMsg = StripeErrorMapper.mapPaymentSheetError(result.error)
 *     snackbarHostState.showSnackbar(userMsg)
 *     Timber.e(result.error, "PaymentSheet failed")
 * }
 * ```
 *
 * Uses reflection/string matching so this module does not need a hard
 * compile-time dependency on the Stripe SDK.
 */
object StripeErrorMapper {

    // ── Whitelisted decline codes (per Stripe docs) ────────────────

    /**
     * Known decline codes from Stripe and their user-friendly messages.
     * Only codes in this map are allowed to influence the displayed message.
     * Any other code falls back to a generic payment-failed message.
     *
     * Reference: https://stripe.com/docs/declines/codes
     */
    private val DECLINE_CODE_MESSAGES: Map<String, String> = mapOf(
        // Card issues
        "card_declined" to "Your card was declined. Please try a different card.",
        "insufficient_funds" to "Your card has insufficient funds. Please try a different card.",
        "lost_card" to "This card can't be used. Please contact your bank or try a different card.",
        "stolen_card" to "This card can't be used. Please contact your bank or try a different card.",
        "expired_card" to "Your card has expired. Please try a different card.",
        "incorrect_cvc" to "The security code (CVC) is incorrect. Please check and try again.",
        "incorrect_number" to "Your card number is incorrect. Please check and try again.",
        "invalid_cvc" to "The security code (CVC) is invalid. Please check and try again.",
        "invalid_expiry_month" to "The expiration month is invalid. Please check and try again.",
        "invalid_expiry_year" to "The expiration year is invalid. Please check and try again.",
        "invalid_number" to "Your card number is invalid. Please check and try again.",
        "processing_error" to "We couldn't process your card. Please try again in a moment.",
        "pickup_card" to "This card can't be used. Please contact your bank or try a different card.",
        "restricted_card" to "This card can't be used for this purchase. Please try a different card.",
        "generic_decline" to "Your card was declined. Please try a different card or contact your bank.",
        // Fraud / security
        "fraudulent" to "This payment was blocked for security reasons. Please contact your bank.",
        "do_not_honor" to "Your card was declined. Please contact your bank or try a different card.",
        "do_not_try_again" to "Your card was declined. Please try a different card.",
        "security_violation" to "This payment was blocked for security reasons. Please try a different card.",
        // 3DS / authentication
        "authentication_required" to "Your bank requires additional verification. Please try again and complete the steps.",
        "approve_with_id" to "Your bank needs to verify this payment. Please try again.",
        // Rate / limits
        "try_again_later" to "Your bank asked us to try again later. Please wait a moment and retry.",
        "withdrawal_count_limit_exceeded" to "You've reached your daily spending limit. Please try again tomorrow or use another card.",
        "card_velocity_exceeded" to "Your card has hit a spending limit. Please try again later or use another card.",
        "transaction_not_allowed" to "This type of transaction isn't allowed on your card. Please try a different card.",
        // Currency / region
        "currency_not_supported" to "Your card doesn't support this currency. Please try a different card.",
        "invalid_amount" to "The payment amount is invalid. Please try again.",
    )

    // ── Whitelisted error codes (non-decline StripeException.code) ─

    /**
     * Error codes that indicate a user-actionable problem. These come from
     * `StripeException.stripeError?.code`.
     */
    private val ERROR_CODE_MESSAGES: Map<String, String> = mapOf(
        "card_declined" to "Your card was declined. Please try a different card.",
        "expired_card" to "Your card has expired. Please try a different card.",
        "incorrect_cvc" to "The security code (CVC) is incorrect. Please check and try again.",
        "incorrect_number" to "Your card number is incorrect. Please check and try again.",
        "invalid_cvc" to "The security code (CVC) is invalid. Please check and try again.",
        "invalid_expiry_month" to "The expiration month is invalid. Please check and try again.",
        "invalid_expiry_year" to "The expiration year is invalid. Please check and try again.",
        "invalid_number" to "Your card number is invalid. Please check and try again.",
        "processing_error" to "We couldn't process your card. Please try again in a moment.",
        "authentication_required" to "Your bank requires additional verification. Please try again and complete the steps.",
    )

    // ── Public API ─────────────────────────────────────────────────

    /**
     * Map a Stripe PaymentSheet error (a Throwable, usually a StripeException)
     * to a user-friendly message. Never returns raw SDK text.
     *
     * @param fallback The message to show when the error doesn't match any
     *                 whitelisted code. Defaults to a payment-specific message.
     */
    fun mapPaymentSheetError(
        throwable: Throwable?,
        fallback: String = "Your payment couldn't be completed. Please try again."
    ): String {
        if (throwable == null) return fallback

        // Try to extract decline_code first (most specific)
        val declineCode = extractDeclineCode(throwable)
        if (declineCode != null) {
            DECLINE_CODE_MESSAGES[declineCode.lowercase()]?.let { return it }
        }

        // Then try the top-level error code
        val errorCode = extractErrorCode(throwable)
        if (errorCode != null) {
            ERROR_CODE_MESSAGES[errorCode.lowercase()]?.let { return it }
        }

        // Last resort: classify by exception type name (no raw message leak)
        val className = throwable::class.java.name
        return when {
            className.endsWith(".CardException") ->
                "Your card couldn't be used. Please check the details or try a different card."
            className.endsWith(".RateLimitException") ->
                "Too many requests. Please wait a moment and try again."
            className.endsWith(".AuthenticationException") ->
                "We couldn't authorise this payment. Please try again."
            className.endsWith(".APIConnectionException") ->
                "Please check your internet connection and try again."
            className.endsWith(".InvalidRequestException") ->
                "We couldn't set up this payment. Please try again."
            className.endsWith(".PermissionException") ->
                "This payment method isn't available right now. Please try a different card."
            else -> fallback
        }
    }

    /**
     * Map an extracted error message string (already a String) to a safe
     * user-facing message, routed through the existing UserFacingErrorMapper
     * for consistency with non-Stripe errors.
     */
    fun mapPaymentSheetMessage(
        rawMessage: String?,
        fallback: String = "Your payment couldn't be completed. Please try again."
    ): String {
        if (rawMessage.isNullOrBlank()) return fallback

        // Check the raw message against known decline codes
        val lower = rawMessage.lowercase()
        for ((code, message) in DECLINE_CODE_MESSAGES) {
            if (lower.contains(code.replace("_", " ")) || lower.contains(code)) {
                return message
            }
        }
        // Route through general mapper as a fallback
        return UserFacingErrorMapper.mapMessage(rawMessage, fallback)
    }

    // ── Internal: reflection-based extraction ──────────────────────

    /**
     * Best-effort extract `stripeError.declineCode` from a Stripe exception
     * using reflection. Returns null if the path isn't present.
     */
    private fun extractDeclineCode(throwable: Throwable): String? = runCatching {
        val stripeError = throwable::class.java
            .getMethod("getStripeError")
            .invoke(throwable) ?: return null
        stripeError::class.java
            .getMethod("getDeclineCode")
            .invoke(stripeError) as? String
    }.getOrNull()

    /**
     * Best-effort extract `stripeError.code` from a Stripe exception.
     */
    private fun extractErrorCode(throwable: Throwable): String? = runCatching {
        val stripeError = throwable::class.java
            .getMethod("getStripeError")
            .invoke(throwable) ?: return null
        stripeError::class.java
            .getMethod("getCode")
            .invoke(stripeError) as? String
    }.getOrNull()
}
