package com.shourov.apps.pacedream.feature.wanted.presentation.util

import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Computes the effective lifecycle state of a [WantedRequest] given the
 * current date.
 *
 * The server is the source of truth for [WantedRequest.status]; this
 * utility is what we use to:
 *  - Show the right "Expiring soon" / "Expired" chip before the server
 *    has had a chance to roll over the column.
 *  - Hide expired posts from the public feed even if a stale row slips
 *    through.
 *
 * "Expiring soon" is anything that auto-closes within the next
 * [EXPIRING_SOON_WINDOW_DAYS] days. The number is deliberately short
 * — it's meant to nudge providers to act, not to be a perpetual badge.
 */
object RequestExpiryResolver {

    /** Default urgency window (today and the next two days, inclusive). */
    const val EXPIRING_SOON_WINDOW_DAYS = 3

    /**
     * Resolve the effective expiry day for a request.
     *
     *  - Prefers the explicit [WantedRequest.expiresAt] when present.
     *  - Falls back to [WantedRequest.requestEndDate] for ranged requests.
     *  - Falls back to [WantedRequest.requestStartDate] for single-day picks.
     *  - Returns `null` for legacy free-text dates ("Sat 10:00 AM") and
     *    open-ended requests — these are treated as "no auto-expiry".
     */
    fun resolveExpiry(request: WantedRequest): LocalDate? =
        request.expiresAt.parseFlexibleDate()
            ?: request.requestEndDate.parseFlexibleDate()
            ?: request.requestStartDate.parseFlexibleDate()

    /**
     * Effective lifecycle status of the request, applying client-side
     * expiry as a fallback for active records whose [expiresAt] has
     * passed. Server-declared closed states (Expired/Fulfilled/Cancelled)
     * are returned verbatim.
     */
    fun effectiveStatus(request: WantedRequest, today: LocalDate): RequestStatus {
        if (request.status != RequestStatus.Active) return request.status
        val expiry = resolveExpiry(request) ?: return RequestStatus.Active
        return if (expiry.isBefore(today)) RequestStatus.Expired else RequestStatus.Active
    }

    /**
     * `true` when the request will auto-expire within
     * [EXPIRING_SOON_WINDOW_DAYS] days. Includes today (an expiry on
     * "today" still counts as expiring soon, not expired yet).
     */
    fun isExpiringSoon(
        request: WantedRequest,
        today: LocalDate,
        windowDays: Int = EXPIRING_SOON_WINDOW_DAYS,
    ): Boolean {
        if (request.status != RequestStatus.Active) return false
        val expiry = resolveExpiry(request) ?: return false
        if (expiry.isBefore(today)) return false
        val daysLeft = today.until(expiry, java.time.temporal.ChronoUnit.DAYS)
        return daysLeft < windowDays
    }

    /**
     * Localized "Active until {date}" label, or null when the request has
     * no resolvable expiry.
     */
    fun activeUntilLabel(
        request: WantedRequest,
        locale: Locale = Locale.getDefault(),
    ): String? = formattedExpiry(request, locale)?.let { "Active until $it" }

    /**
     * Just the formatted expiry date (no prefix), or null when no expiry
     * resolves. Used by [RequestDetailScreen]'s detail rows, where the
     * label is rendered separately.
     */
    fun formattedExpiry(
        request: WantedRequest,
        locale: Locale = Locale.getDefault(),
    ): String? {
        val expiry = resolveExpiry(request) ?: return null
        val formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
        return formatter.format(expiry)
    }

    /**
     * Accept either a plain ISO date (`yyyy-MM-dd`) or an ISO-8601 instant
     * (`yyyy-MM-ddTHH:mm:ssZ`). The backend has historically used both
     * shapes — converting the instant to its `LocalDate` (UTC) is fine for
     * our day-grained "is it past today?" check.
     */
    private fun String?.parseFlexibleDate(): LocalDate? {
        val trimmed = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        runCatching { return LocalDate.parse(trimmed) }
        runCatching {
            return OffsetDateTime.parse(trimmed)
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDate()
        }
        return null
    }
}
