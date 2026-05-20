package com.shourov.apps.pacedream.feature.host.data

import timber.log.Timber

/**
 * Lightweight, log-only counters for the host payout money path.
 * Same shape and rationale as `WebflowTelemetry` in the webflow module
 * — structured Timber lines under a single tag so a log query
 * (`tag=HostPayoutMoney AND event=...`) gives accurate counts without
 * needing the `core.analytics` Hilt module that is still a stub.
 *
 * Audit reference: `claude/audit-stripe-payments-7FXvv` Phase 2,
 * findings F-07 (fresh status check before payout) and F-08
 * (persist payout idempotency key).
 */
internal object HostPayoutTelemetry {

    private const val TAG = "HostPayoutMoney"

    /** A tap on Withdraw arrived at the ViewModel. */
    fun requestAttempt(amountCents: Long, reusedKey: Boolean) {
        Timber.tag(TAG).i(
            "event=request_attempt amountCents=%d reusedKey=%s",
            amountCents,
            reusedKey,
        )
    }

    /**
     * Fresh status fetch right before submission failed; we refused to
     * send the POST.  Distinct from a backend rejection — this is the
     * client choosing to fail closed when it can't verify the account
     * is currently allowed to receive payouts.
     */
    fun freshStatusBlocked(reason: String) {
        Timber.tag(TAG).w(
            "event=fresh_status_blocked reason=%s",
            reason.take(80),
        )
    }

    /** The fresh status fetch succeeded and `payoutsEnabled=true`; we'll proceed to POST. */
    fun freshStatusOk() {
        Timber.tag(TAG).i("event=fresh_status_ok")
    }

    /** The user dismissed the sheet (or the VM cleared state) while a key was persisted. */
    fun pendingKeyDiscarded(reason: String) {
        Timber.tag(TAG).i(
            "event=pending_key_discarded reason=%s",
            reason.take(80),
        )
    }

    /** Counted when a stale persisted key (older than the staleness window) is force-rotated. */
    fun pendingKeyExpired(ageMs: Long) {
        Timber.tag(TAG).w(
            "event=pending_key_expired ageMs=%d",
            ageMs,
        )
    }

    /**
     * Counted when the persisted key was for a different amount than
     * the user is now requesting; we rotate to a fresh key rather than
     * letting Stripe Connect return the prior amount's transfer for a
     * request the user thinks is a new amount.
     */
    fun pendingKeyAmountMismatch(persistedAmount: Long, newAmount: Long) {
        Timber.tag(TAG).w(
            "event=pending_key_amount_mismatch persisted=%d new=%d",
            persistedAmount,
            newAmount,
        )
    }

    /** Network POST succeeded. */
    fun requestSucceeded(amountCents: Long) {
        Timber.tag(TAG).i("event=request_success amountCents=%d", amountCents)
    }

    /** Network POST failed (any reason). */
    fun requestFailed(amountCents: Long, reason: String) {
        Timber.tag(TAG).w(
            "event=request_failed amountCents=%d reason=%s",
            amountCents,
            reason.take(80),
        )
    }
}
