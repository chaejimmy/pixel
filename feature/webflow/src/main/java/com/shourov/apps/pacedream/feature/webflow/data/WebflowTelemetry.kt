package com.shourov.apps.pacedream.feature.webflow.data

import timber.log.Timber

/**
 * Lightweight, log-only counters for the webflow money path.  We
 * intentionally keep these as structured Timber lines (consistent tag,
 * `event=…` key) instead of wiring them through the
 * `core.analytics` module — that module is still a Hilt-only stub and
 * we want signal in production logs today without taking on a bigger
 * dependency.  Each call site emits exactly one line so a logs query
 * like `tag=WebflowMoney AND event=in_flight_blocked` gives an
 * accurate count.
 *
 * Audit reference: `claude/audit-stripe-payments` Phase 0 telemetry
 * requirement.  Once `AnalyticsHelper` is reinstated, swap these calls
 * to `analyticsHelper.logEvent(…)` — the message format here was
 * picked so the transition is a search-and-replace.
 */
internal object WebflowTelemetry {

    private const val TAG = "WebflowMoney"

    /** Counted whenever a guest taps Reserve and we hit the booking-creation API. */
    fun createAttempt(itemId: String, type: BookingType, idempotencyKeyEnabled: Boolean) {
        Timber.tag(TAG).i(
            "event=create_attempt type=%s itemId=%s idempotency=%s",
            type.name,
            itemId,
            if (idempotencyKeyEnabled) "on" else "off",
        )
    }

    /**
     * Counted when the client-side in-flight guard refuses a duplicate
     * Reserve tap (the previous POST is still in flight).  This is the
     * primary signal that the guard is doing something.
     */
    fun inFlightBlocked(itemId: String, type: BookingType) {
        Timber.tag(TAG).w(
            "event=in_flight_blocked type=%s itemId=%s",
            type.name,
            itemId,
        )
    }

    /** Counted when booking creation returns a checkout URL. */
    fun createSucceeded(itemId: String, type: BookingType, hasSessionId: Boolean) {
        Timber.tag(TAG).i(
            "event=create_success type=%s itemId=%s hasSessionId=%s",
            type.name,
            itemId,
            hasSessionId,
        )
    }

    /** Counted on any failed booking creation (network or 4xx/5xx). */
    fun createFailed(itemId: String, type: BookingType, reason: String) {
        Timber.tag(TAG).w(
            "event=create_failed type=%s itemId=%s reason=%s",
            type.name,
            itemId,
            reason.take(80),
        )
    }

    /**
     * Counted when the success-confirmation response was rejected by
     * the fail-closed parser (missing bookingId, missing/invalid status).
     * This is the primary signal that the backend returned a 200 we
     * cannot trust as a confirmed booking.
     */
    fun confirmParseError(type: BookingType, reason: String) {
        Timber.tag(TAG).w(
            "event=confirm_parse_error type=%s reason=%s",
            type.name,
            reason.take(80),
        )
    }

    /** Counted on every successful confirmation parse — bookingId present + valid status. */
    fun confirmSucceeded(type: BookingType) {
        Timber.tag(TAG).i("event=confirm_success type=%s", type.name)
    }
}
