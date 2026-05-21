package com.pacedream.app.feature.checkout

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coarse user-facing state of the most recent native payment reconciliation
 * attempt.  Observed by Bookings + Checkout surfaces so we can render the
 * "Payment processing" pill / banner without the UI having to poke at the
 * persisted JSON record directly.
 *
 *  - [None]      No payment is currently being reconciled, and no terminal
 *                state is being held for the user to acknowledge.
 *  - [Pending]   The reconciliation worker has work to do — Stripe captured
 *                the funds but `/payments/native/confirm-booking` has not
 *                resolved yet.  `bookingId` is a best-effort stable id (the
 *                PaymentIntent id or quote id) used to disambiguate when
 *                multiple reconciliations land in close succession.
 *  - [Succeeded] Reconciliation just resolved with a server booking id.
 *                Used to fire the one-shot "Booking confirmed" notification
 *                and to swap any in-app banner copy to a success state.
 *  - [Failed]    Reconciliation gave up after the worker's retry budget.
 *                `reason` is the last user-facing error message so the
 *                banner / notification can explain why and offer retry.
 */
sealed class PendingPaymentState {
    object None : PendingPaymentState()
    data class Pending(val bookingId: String?, val attemptedAt: Long) : PendingPaymentState()
    data class Succeeded(val bookingId: String) : PendingPaymentState()
    data class Failed(val bookingId: String?, val reason: String?) : PendingPaymentState()
}

/**
 * Snapshot of an in-flight native payment session that the app needs
 * to be able to recover after process death or crash.
 *
 * Written right before the Stripe PaymentSheet is presented and
 * cleared as soon as the backend confirms the booking.  If the app
 * is killed (OOM, force-quit, system restart) between a successful
 * PaymentSheet completion and a successful confirm-booking call,
 * this record is what the next launch uses to auto-retry — and, if
 * the retries stay exhausted, to post a structured report to
 * `/v1/payments/native/report-failure`.
 */
@Serializable
data class PendingNativePayment(
    /** Stripe PaymentIntent client secret. */
    val clientSecret: String,
    /** Server-side quote id (qt_...) the PI was created from. */
    val quoteId: String,
    /** Extracted pi_xxx, cached so recovery doesn't re-parse. */
    val paymentIntentId: String? = null,
    /** Listing id for operator correlation. */
    val listingId: String? = null,
    /** Retry attempt counter, bumped on every confirm-booking call. */
    val retryCount: Int = 0,
    /** Epoch millis when the record was first written. */
    val createdAt: Long = System.currentTimeMillis(),
    /** Epoch millis of the last confirm-booking attempt. */
    val lastAttemptAt: Long? = null,
    /**
     * True once PaymentSheet reports `.Completed` locally.  Recovery
     * only auto-retries when this is true, so a record from a sheet
     * the user abandoned mid-flow doesn't trigger a phantom confirm
     * call on the next launch.
     */
    val paymentSucceededLocally: Boolean = false,
    /**
     * Idempotency-Key used on every confirm-booking call for this PI.
     * Persisted so process-death recovery and manual retries reuse the
     * SAME key — the backend deduplicates and returns the existing
     * booking instead of creating a duplicate.
     */
    val confirmIdempotencyKey: String? = null,
    /**
     * X-Request-ID reused across every confirm-booking attempt for this
     * PaymentIntent.  Surfaced to the user as a support reference and
     * lets backend logs trace every retry to a single token.
     */
    val confirmRequestId: String? = null,
    /** Total amount captured, in minor units (cents). */
    val amountCents: Int? = null,
    /** ISO currency code (lowercase, e.g. "usd"). */
    val currency: String? = null,
    /** Authenticated user id, when known. */
    val userId: String? = null,
    /** Listing title, when known.  Surfaced to the user / support. */
    val listingTitle: String? = null,
    /** Booking start ISO-8601 string. */
    val startTimeISO: String? = null,
    /** Booking end ISO-8601 string. */
    val endTimeISO: String? = null,
    /** Last user-facing error message, if any. */
    val lastErrorMessage: String? = null,
    /**
     * Server booking id once `/confirm-booking` has resolved.  Persisted so
     * the worker can detect "already confirmed" on a re-run and short-circuit
     * without making a duplicate network call.  When non-null, the worker is
     * a no-op regardless of [paymentSucceededLocally].
     */
    val confirmedBookingId: String? = null,
)

/**
 * Plain `SharedPreferences`-backed single-slot store for
 * [PendingNativePayment].  One file (`pacedream_pending_payment`),
 * one JSON blob under a fixed key.  Not encrypted — the contents are
 * non-credential references (PI id, quote id, retry count) and we
 * want to avoid the main-thread Keystore init cost that
 * [com.pacedream.app.core.auth.TokenStorage] has to guard against.
 *
 * Thread-safe — SharedPreferences is.  All mutations use `commit()`
 * so the pending record is durable before the Stripe sheet is shown:
 * `apply()` is async and would race a crash immediately after.
 */
@Singleton
open class PendingPaymentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _state: MutableStateFlow<PendingPaymentState> =
        MutableStateFlow(hydrateInitialState())

    /**
     * Coarse public state for the active reconciliation cycle.  UI surfaces
     * (Bookings list, Booking detail, Checkout) observe this to render the
     * "Payment processing" pill / banner without touching prefs themselves.
     * Transitions:
     *   save()           → Pending
     *   markSucceeded()  → Succeeded (and clears the persisted record)
     *   markFailed()     → Failed    (and clears the persisted record)
     *   clear()          → None
     */
    val state: StateFlow<PendingPaymentState> = _state.asStateFlow()

    private fun hydrateInitialState(): PendingPaymentState {
        val record = load() ?: return PendingPaymentState.None
        // A persisted record with a recorded server booking id means the
        // last cycle succeeded but the success notification hadn't been
        // acknowledged before the process died.  Don't re-fire it; treat
        // the cycle as resolved.
        record.confirmedBookingId?.let { return PendingPaymentState.Succeeded(it) }
        return PendingPaymentState.Pending(
            bookingId = record.paymentIntentId ?: record.quoteId,
            attemptedAt = record.lastAttemptAt ?: record.createdAt,
        )
    }

    /** Returns the current pending payment record, or null if none. */
    open fun load(): PendingNativePayment? {
        val raw = try {
            prefs.getString(KEY_RECORD, null)
        } catch (e: Exception) {
            Timber.w(e, "[PendingPaymentStore] read failed")
            null
        } ?: return null
        return try {
            json.decodeFromString(PendingNativePayment.serializer(), raw)
        } catch (e: Exception) {
            Timber.w(e, "[PendingPaymentStore] decode failed — clearing corrupt record")
            clear()
            null
        }
    }

    /**
     * Writes (or overwrites) the pending record.  Uses `commit()`
     * so the value is on disk before the caller hands control over
     * to the Stripe PaymentSheet — `apply()` would race a crash.
     */
    open fun save(record: PendingNativePayment): Boolean {
        val ok = try {
            val raw = json.encodeToString(PendingNativePayment.serializer(), record)
            prefs.edit().putString(KEY_RECORD, raw).commit()
        } catch (e: Exception) {
            Timber.w(e, "[PendingPaymentStore] save failed")
            false
        }
        if (ok) {
            // A non-null confirmedBookingId means save() is being used to
            // stamp the terminal-success record — don't downgrade the
            // observable state back to Pending in that case.
            _state.value = record.confirmedBookingId?.let {
                PendingPaymentState.Succeeded(it)
            } ?: PendingPaymentState.Pending(
                bookingId = record.paymentIntentId ?: record.quoteId,
                attemptedAt = record.lastAttemptAt ?: record.createdAt,
            )
        }
        return ok
    }

    /** Removes any stored pending record.  Idempotent. */
    open fun clear(): Boolean {
        val ok = try {
            prefs.edit().remove(KEY_RECORD).commit()
        } catch (e: Exception) {
            Timber.w(e, "[PendingPaymentStore] clear failed")
            false
        }
        if (ok) _state.value = PendingPaymentState.None
        return ok
    }

    /**
     * Terminal-success transition: clears the persisted record and emits
     * [PendingPaymentState.Succeeded] on the public flow.  Called by the
     * worker once `/confirm-booking` resolves so observers (BookingDetail
     * banner, foreground notification) can swap to the confirmed state.
     */
    fun markSucceeded(bookingId: String): Boolean {
        val ok = try {
            prefs.edit().remove(KEY_RECORD).commit()
        } catch (e: Exception) {
            Timber.w(e, "[PendingPaymentStore] markSucceeded clear failed")
            false
        }
        // Always emit the transition — even if the prefs clear failed the
        // server-side booking is real and the user needs to see success.
        _state.value = PendingPaymentState.Succeeded(bookingId)
        return ok
    }

    /**
     * Terminal-failure transition: keeps the persisted record around so the
     * Checkout deep link can rebuild a BookingDraft from it, and emits
     * [PendingPaymentState.Failed] on the public flow.  Pass `clearRecord =
     * true` when the caller is sure no further retry is possible (e.g. the
     * backend reported alreadyBooked = true and we resolved via a different
     * code path).
     */
    fun markFailed(bookingId: String?, reason: String?, clearRecord: Boolean = false): Boolean {
        var ok = true
        if (clearRecord) {
            ok = try {
                prefs.edit().remove(KEY_RECORD).commit()
            } catch (e: Exception) {
                Timber.w(e, "[PendingPaymentStore] markFailed clear failed")
                false
            }
        }
        _state.value = PendingPaymentState.Failed(bookingId, reason?.take(512))
        return ok
    }

    /**
     * Bumps `retryCount` and updates `lastAttemptAt`.  No-op when
     * there is no stored record.  Returns the updated record.
     */
    open fun recordRetryAttempt(): PendingNativePayment? {
        val current = load() ?: return null
        val updated = current.copy(
            retryCount = current.retryCount + 1,
            lastAttemptAt = System.currentTimeMillis(),
        )
        return if (save(updated)) updated else current
    }

    /**
     * Flips `paymentSucceededLocally` to true and stamps
     * `lastAttemptAt`.  No-op when there is no stored record.
     */
    open fun markPaymentSucceededLocally(): PendingNativePayment? {
        val current = load() ?: return null
        val updated = current.copy(
            paymentSucceededLocally = true,
            lastAttemptAt = System.currentTimeMillis(),
        )
        return if (save(updated)) updated else current
    }

    /**
     * Persists the last user-facing error against the in-flight pending
     * record (e.g. a transient confirm-booking failure).  No-op when
     * there is no stored record.
     */
    open fun recordLastError(message: String?): PendingNativePayment? {
        val current = load() ?: return null
        val updated = current.copy(lastErrorMessage = message?.take(512))
        return if (save(updated)) updated else current
    }

    /**
     * Rebuild a [BookingDraft] from the persisted record so the failure
     * notification deep link can land in CheckoutScreen with the original
     * trip pre-filled.  Returns null when the persisted record is missing
     * the time-window fields the draft requires (the caller falls through
     * to the missing-draft recovery surface in that case).
     */
    fun restoreBookingDraft(): BookingDraft? {
        val record = load() ?: return null
        val listingId = record.listingId ?: return null
        val start = record.startTimeISO ?: return null
        val end = record.endTimeISO ?: return null
        val date = start.substringBefore('T').takeIf { it.isNotBlank() } ?: return null
        return BookingDraft(
            listingId = listingId,
            date = date,
            startTimeISO = start,
            endTimeISO = end,
            guests = 1,
        )
    }

    private companion object {
        private const val PREFS_NAME = "pacedream_pending_payment"
        private const val KEY_RECORD = "pending_native_payment"
    }
}

/**
 * Hilt entry point so composables (specifically the BOOKING_FORM route
 * handler in DashboardNavigation) can pull the [PendingPaymentStore]
 * out of the SingletonComponent without becoming ViewModel-scoped.  Used
 * for the failure-notification deep link path: when the route lands
 * without a `booking_draft_json_$propertyId` in its previous backstack
 * entry, it falls back to [PendingPaymentStore.restoreBookingDraft] so
 * the user lands directly in CheckoutScreen with the trip restored
 * rather than the missing-draft recovery surface.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PendingPaymentStoreEntryPoint {
    fun pendingPaymentStore(): PendingPaymentStore
}
