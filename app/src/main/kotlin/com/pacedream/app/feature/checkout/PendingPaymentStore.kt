package com.pacedream.app.feature.checkout

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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
class PendingPaymentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns the current pending payment record, or null if none. */
    fun load(): PendingNativePayment? {
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
    fun save(record: PendingNativePayment): Boolean {
        return try {
            val raw = json.encodeToString(PendingNativePayment.serializer(), record)
            prefs.edit().putString(KEY_RECORD, raw).commit()
        } catch (e: Exception) {
            Timber.w(e, "[PendingPaymentStore] save failed")
            false
        }
    }

    /** Removes any stored pending record.  Idempotent. */
    fun clear(): Boolean {
        return try {
            prefs.edit().remove(KEY_RECORD).commit()
        } catch (e: Exception) {
            Timber.w(e, "[PendingPaymentStore] clear failed")
            false
        }
    }

    /**
     * Bumps `retryCount` and updates `lastAttemptAt`.  No-op when
     * there is no stored record.  Returns the updated record.
     */
    fun recordRetryAttempt(): PendingNativePayment? {
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
    fun markPaymentSucceededLocally(): PendingNativePayment? {
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
    fun recordLastError(message: String?): PendingNativePayment? {
        val current = load() ?: return null
        val updated = current.copy(lastErrorMessage = message?.take(512))
        return if (save(updated)) updated else current
    }

    private companion object {
        private const val PREFS_NAME = "pacedream_pending_payment"
        private const val KEY_RECORD = "pending_native_payment"
    }
}
