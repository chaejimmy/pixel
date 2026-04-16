package com.pacedream.common.util

/**
 * User-facing error representation that carries both a display message and
 * a hint about the correct recovery action.
 *
 * ## Why this exists
 * A plain `error: String?` field on UI state loses the critical distinction
 * between "something broke, retry" and "you need to sign in again". This
 * sealed type lets ViewModels communicate the type of failure so the UI
 * can show the correct CTA — **Retry** for transient failures, **Sign In**
 * for auth errors, **Contact Support** for account restrictions, etc.
 *
 * ## Usage
 * ```kotlin
 * // In a ViewModel:
 * is ApiResult.Failure -> {
 *     _uiState.update {
 *         it.copy(uiError = UiError.from(result.error))
 *     }
 * }
 *
 * // In a Composable:
 * when (val err = uiState.uiError) {
 *     is UiError.AuthRequired -> PaceDreamLockedState(
 *         title = err.title,
 *         description = err.message,
 *         actionText = "Sign In",
 *         onActionClick = onSignInClick
 *     )
 *     is UiError.Retryable -> PaceDreamErrorState(
 *         title = err.title,
 *         description = err.message,
 *         onRetryClick = onRetry
 *     )
 *     // ...
 * }
 * ```
 */
sealed class UiError {
    abstract val title: String
    abstract val message: String

    /**
     * Session expired / 401 — user must re-authenticate to recover.
     * UI should show a "Sign In" button, not a "Retry" button.
     */
    data class AuthRequired(
        override val title: String = "Sign in required",
        override val message: String = "Your session has expired. Please sign in again to continue."
    ) : UiError()

    /**
     * Account restricted / security block — retrying won't help.
     * UI should show a "Contact Support" button.
     */
    data class AccountRestricted(
        override val title: String = "Account restricted",
        override val message: String = "Your account is currently restricted. Please contact support for help."
    ) : UiError()

    /**
     * Network / transient failure — retrying is the right action.
     * UI should show a "Retry" / "Try Again" button.
     */
    data class Retryable(
        override val title: String = "Something went wrong",
        override val message: String = UserFacingErrorMapper.DEFAULT_MESSAGE
    ) : UiError()

    /**
     * Rate limited — retrying too soon will fail. UI should show a
     * countdown or just a wait message.
     */
    data class RateLimited(
        override val title: String = "Too many attempts",
        override val message: String = "You're doing that too often. Please wait a moment and try again.",
        val retryAfterSeconds: Int? = null
    ) : UiError()

    /**
     * Permission denied — not an auth issue, but user can't perform this
     * specific action. UI should show dismissible info, not a retry button.
     */
    data class PermissionDenied(
        override val title: String = "Not allowed",
        override val message: String = "You don't have permission to perform this action."
    ) : UiError()

    companion object {
        /**
         * Map any [Throwable] to the correct [UiError] variant based on its
         * type/content. This is the primary constructor used by ViewModels.
         */
        fun from(error: Throwable?): UiError {
            if (error == null) return Retryable()

            // Check for ApiError subclasses first (typed signal is most reliable)
            val className = error::class.java.name
            if (className.endsWith(".ApiError\$Unauthorized") ||
                className.endsWith(".ApiError\$Unauthorized")) {
                return AuthRequired()
            }
            if (className.endsWith(".ApiError\$Forbidden") ||
                className.endsWith(".ApiError\$Forbidden")) {
                return PermissionDenied()
            }
            if (className.endsWith(".ApiError\$AccountRestricted") ||
                className.endsWith(".ApiError\$FraudBlocked")) {
                return AccountRestricted(
                    message = UserFacingErrorMapper.mapMessage(
                        error.message,
                        "Your account is currently restricted. Please contact support for help."
                    )
                )
            }
            if (className.endsWith(".ApiError\$RateLimited")) {
                // Best-effort extract retry-after via reflection
                val retryAfter = runCatching {
                    val field = error::class.java.getDeclaredField("retryAfterSeconds")
                    field.isAccessible = true
                    field.get(error) as? Int
                }.getOrNull()
                return RateLimited(retryAfterSeconds = retryAfter)
            }

            // Fall back to message-based classification
            return fromMessage(error.message)
        }

        /**
         * Map an already-extracted error message (e.g. from Stripe SDK) to a
         * [UiError] variant.
         */
        fun fromMessage(raw: String?): UiError {
            if (raw.isNullOrBlank()) return Retryable()
            val lower = raw.lowercase()
            return when {
                lower.contains("unauthorized") || lower.contains("401") ||
                    lower.contains("session expired") ||
                    (lower.contains("token") && lower.contains("expired")) ->
                    AuthRequired()
                lower.contains("forbidden") || lower.contains("403") ->
                    PermissionDenied()
                lower.contains("restricted") || lower.contains("suspended") ||
                    lower.contains("fraud") || lower.contains("blocked") ->
                    AccountRestricted()
                lower.contains("429") || lower.contains("too many") ||
                    (lower.contains("rate") && lower.contains("limit")) ->
                    RateLimited()
                else ->
                    Retryable(message = UserFacingErrorMapper.mapMessage(raw))
            }
        }

        /**
         * Convenience: build a Retryable with a specific fallback message.
         * Use when you want the default "Retry" UX but with domain-specific copy.
         */
        fun retryable(message: String): UiError = Retryable(message = message)

        /**
         * Convenience: detect whether an ApiError is an auth-expired signal
         * without converting to UiError (useful in condition checks).
         */
        fun isAuthError(error: Throwable?): Boolean {
            if (error == null) return false
            val className = error::class.java.name
            return className.endsWith(".ApiError\$Unauthorized") ||
                (error.message?.let {
                    it.contains("401") || it.contains("unauthorized", ignoreCase = true) ||
                        it.contains("session expired", ignoreCase = true)
                } ?: false)
        }
    }

    /**
     * True if the correct UI affordance is a "Sign In" button rather than a
     * "Retry" button.
     */
    val requiresSignIn: Boolean get() = this is AuthRequired

    /**
     * True if the correct UI affordance is a "Contact Support" link rather
     * than a "Retry" button.
     */
    val requiresSupport: Boolean get() = this is AccountRestricted
}
