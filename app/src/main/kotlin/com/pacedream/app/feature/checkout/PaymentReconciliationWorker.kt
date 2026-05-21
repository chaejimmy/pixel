package com.pacedream.app.feature.checkout

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pacedream.app.core.network.ApiResult
import com.shourov.apps.pacedream.MainActivity
import com.shourov.apps.pacedream.R
import com.shourov.apps.pacedream.notification.PaceDreamNotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Background reconciliation worker for pending native payments.
 *
 * Enqueued whenever the app moves into
 * [CheckoutStatus.PAYMENT_CAPTURED_PENDING_CONFIRMATION] — i.e. Stripe
 * captured the funds but `/payments/native/confirm-booking` hasn't yet
 * resolved.  The worker is also enqueued again on app launch when a
 * pending record is hydrated from disk (so reconciliation continues
 * even if the user closes the app).
 *
 * The worker runs in the foreground (PAYMENTS channel) while it is
 * actively confirming so the user knows something is happening even
 * after a force-kill, and it posts a one-shot success/failure
 * notification on the same channel once it resolves so a user who
 * navigated away still finds out.
 *
 * The worker is **idempotency-key safe** and **idempotent at the
 * record level**:
 *   - It reads the persisted [PendingNativePayment] every run, so a
 *     ViewModel retry that already drained the queue won't double-bill.
 *   - It exits early when [PendingNativePayment.confirmedBookingId]
 *     is non-null — re-running the worker for an already-confirmed
 *     booking is a guaranteed no-op (no network, no notifications).
 *   - It reuses [PendingNativePayment.confirmIdempotencyKey] on every
 *     attempt, guaranteeing the backend deduplicates and returns the
 *     same booking instead of creating a duplicate.
 *
 * Termination conditions:
 *   - On success → marks the store Succeeded, posts a one-shot system
 *     notification, returns [Result.success].
 *   - When report-failure responds `alreadyBooked = true` → marks
 *     Succeeded with the recovered booking id, returns [Result.success].
 *   - On a transient failure → returns [Result.retry] so WorkManager
 *     re-runs us with its own backoff schedule.
 *   - When local retry budget is exhausted → posts report-failure,
 *     marks Failed, posts a one-shot "Payment failed — tap to retry"
 *     notification deep-linking to CheckoutScreen, and returns
 *     [Result.success]: the client gives up, the backend takes over
 *     reconciliation.
 */
@HiltWorker
class PaymentReconciliationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val pendingPaymentStore: PendingPaymentStore,
    private val nativePaymentRepository: NativePaymentRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val record = pendingPaymentStore.load()
        if (record == null) {
            // No work to do — record was cleared by a foreground confirm
            // or a previous run.  This is the idempotent "already-handled"
            // path: re-runs after success are a guaranteed no-op.
            return Result.success()
        }
        if (record.confirmedBookingId != null) {
            // Idempotency guard: a previous run already confirmed this
            // booking on the backend.  No network call, no notification,
            // no state churn — just succeed and let the record stay until
            // the user acknowledges (or a new payment overwrites it).
            Timber.d(
                "[PaymentReconcile] no-op: bookingId=%s already confirmed",
                record.confirmedBookingId,
            )
            return Result.success()
        }
        if (!record.paymentSucceededLocally) {
            // The Stripe sheet was abandoned before it reported success —
            // no money was captured, so there is nothing to reconcile.
            return Result.success()
        }

        // Show "Confirming your payment…" on the PAYMENTS channel so the
        // user sees we're still working even after a force-kill or while
        // they're in another app.  setForeground is best-effort: on Android
        // 14+ data-sync needs the FOREGROUND_SERVICE_DATA_SYNC permission;
        // failures fall through to background execution rather than
        // crashing reconciliation.
        runCatching { setForeground(buildForegroundInfo(record)) }
            .onFailure { Timber.w(it, "[PaymentReconcile] setForeground failed; continuing in background") }

        val paymentIntentId = record.paymentIntentId
            ?: nativePaymentRepository.extractPaymentIntentId(record.clientSecret)
        if (paymentIntentId == null) {
            Timber.w("[PaymentReconcile] corrupt record — no PaymentIntent id, clearing")
            pendingPaymentStore.markFailed(
                bookingId = null,
                reason = "Missing PaymentIntent id",
                clearRecord = true,
            )
            postFailureNotification(record, "We couldn't read your payment reference.")
            return Result.success()
        }

        if (record.retryCount >= MAX_BACKGROUND_RETRIES) {
            Timber.w(
                "[PaymentReconcile] retry budget exhausted (count=%d) pi=%s — reporting",
                record.retryCount,
                paymentIntentId,
            )
            val resolved = reportAndPossiblyClear(
                record,
                paymentIntentId,
                "Background retry budget exhausted",
            )
            if (!resolved) {
                pendingPaymentStore.markFailed(
                    bookingId = paymentIntentId,
                    reason = record.lastErrorMessage ?: "Background retry budget exhausted",
                )
                postFailureNotification(
                    record,
                    record.lastErrorMessage ?: "We couldn't confirm your booking.",
                )
            }
            // Either way we stop — the server now owns reconciliation.
            return Result.success()
        }

        val idempotencyKey = record.confirmIdempotencyKey
            ?: UUID.randomUUID().toString().also {
                pendingPaymentStore.save(record.copy(confirmIdempotencyKey = it))
            }
        val requestId = record.confirmRequestId
            ?: UUID.randomUUID().toString().also {
                pendingPaymentStore.save(record.copy(confirmRequestId = it))
            }

        // Bump persisted retry counter BEFORE the call so a process kill
        // mid-call doesn't make us think we have unlimited attempts.
        val updated = pendingPaymentStore.recordRetryAttempt() ?: record
        Timber.d(
            "[PaymentReconcile] confirm-booking attempt=%d pi=%s reqId=%s",
            updated.retryCount,
            paymentIntentId,
            requestId,
        )

        val result = nativePaymentRepository.confirmBooking(
            paymentIntentId = paymentIntentId,
            idempotencyKey = idempotencyKey,
            requestId = requestId,
        )
        return when (result) {
            is ApiResult.Success -> {
                val bookingId = result.data.booking?.id
                Timber.i(
                    "[PaymentReconcile] confirmed bookingId=%s pi=%s",
                    bookingId,
                    paymentIntentId,
                )
                if (bookingId != null) {
                    pendingPaymentStore.markSucceeded(bookingId)
                    postSuccessNotification(updated, bookingId)
                } else {
                    // Confirm returned 200 but with no booking — treat as
                    // a transient anomaly and let WorkManager retry.
                    Timber.w("[PaymentReconcile] confirm returned no booking id; retrying")
                    return Result.retry()
                }
                Result.success()
            }
            is ApiResult.Failure -> {
                Timber.w(
                    "[PaymentReconcile] attempt=%d failed: %s",
                    updated.retryCount,
                    result.error.message,
                )
                pendingPaymentStore.recordLastError(result.error.message)
                if (updated.retryCount >= MAX_BACKGROUND_RETRIES) {
                    val resolved = reportAndPossiblyClear(
                        updated,
                        paymentIntentId,
                        result.error.message ?: "confirm-booking failed",
                    )
                    if (!resolved) {
                        pendingPaymentStore.markFailed(
                            bookingId = paymentIntentId,
                            reason = result.error.message,
                        )
                        postFailureNotification(
                            updated,
                            result.error.message ?: "We couldn't confirm your booking.",
                        )
                    }
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        }
    }

    /**
     * Returns true when the failure report resolved the cycle (the backend
     * answered `alreadyBooked = true` and we surfaced Succeeded).  Returns
     * false if the caller should fall through to the regular Failed path.
     */
    private suspend fun reportAndPossiblyClear(
        record: PendingNativePayment,
        paymentIntentId: String,
        errorMessage: String,
    ): Boolean {
        val reported = nativePaymentRepository.reportFailure(
            paymentIntentId = paymentIntentId,
            quoteId = record.quoteId,
            retryCount = record.retryCount,
            errorMessage = errorMessage,
            errorCode = "BACKGROUND_RECONCILE_EXHAUSTED",
        )
        if (reported is ApiResult.Success && reported.data.alreadyBooked == true) {
            val resolvedId = reported.data.bookingId ?: reported.data.id ?: paymentIntentId
            Timber.i("[PaymentReconcile] already booked pi=%s — surfacing %s", paymentIntentId, resolvedId)
            pendingPaymentStore.markSucceeded(resolvedId)
            postSuccessNotification(record, resolvedId)
            return true
        }
        return false
    }

    // ── Notifications ───────────────────────────────────────────────

    private fun buildForegroundInfo(record: PendingNativePayment): ForegroundInfo {
        val title = appContext.getString(R.string.payment_reconciliation_running_title)
        val text = record.listingTitle?.let {
            appContext.getString(R.string.payment_reconciliation_running_text_with_listing, it)
        } ?: appContext.getString(R.string.payment_reconciliation_running_text)

        val notification = NotificationCompat.Builder(
            appContext,
            PaceDreamNotificationService.CHANNEL_ID_PAYMENTS,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .setContentIntent(openBookingsIntent(record.paymentIntentId))
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID_FOREGROUND, notification)
        }
    }

    private fun postSuccessNotification(record: PendingNativePayment, bookingId: String) {
        if (!hasNotificationPermission()) return
        val title = appContext.getString(R.string.payment_reconciliation_success_title)
        val text = record.listingTitle?.let {
            appContext.getString(R.string.payment_reconciliation_success_text_with_listing, it)
        } ?: appContext.getString(R.string.payment_reconciliation_success_text)

        val notification = NotificationCompat.Builder(
            appContext,
            PaceDreamNotificationService.CHANNEL_ID_PAYMENTS,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(openBookingDetailIntent(bookingId))
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NOTIFICATION_ID_RESOLVED, notification)
        }.onFailure { Timber.w(it, "[PaymentReconcile] success notify failed") }
    }

    private fun postFailureNotification(record: PendingNativePayment, reason: String) {
        if (!hasNotificationPermission()) return
        val title = appContext.getString(R.string.payment_reconciliation_failure_title)
        val text = reason

        val notification = NotificationCompat.Builder(
            appContext,
            PaceDreamNotificationService.CHANNEL_ID_PAYMENTS,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(openCheckoutRetryIntent(record))
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NOTIFICATION_ID_RESOLVED, notification)
        }.onFailure { Timber.w(it, "[PaymentReconcile] failure notify failed") }
    }

    private fun openBookingsIntent(paymentIntentId: String?): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra("screen", "booking_detail")
            paymentIntentId?.let { putExtra("payment_intent_id", it) }
        }
        return PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_FOREGROUND,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openBookingDetailIntent(bookingId: String): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra("screen", "booking_detail")
            putExtra("booking_id", bookingId)
            putExtra("type", "booking_confirmed")
        }
        return PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_SUCCESS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Deep-link the failure notification back into CheckoutScreen with the
     * original [BookingDraft] reconstructed from the persisted record so
     * the user can retry without re-entering trip details.  We encode the
     * draft on the intent as JSON; MainActivity hands it back to the
     * checkout route via a sticky deep-link channel.
     */
    private fun openCheckoutRetryIntent(record: PendingNativePayment): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra("screen", "checkout")
            putExtra("type", "payment_failed")
            record.paymentIntentId?.let { putExtra("payment_intent_id", it) }
            record.listingId?.let { putExtra("property_id", it) }
            // Restore the booking draft so CheckoutScreen can rehydrate
            // without bouncing the user back to listing detail.
            val draft = record.toBookingDraft()
            if (draft != null) {
                putExtra("booking_draft_json", BookingDraftCodec.encode(draft))
            }
            data = Uri.parse("pacedream://checkout/retry")
        }
        return PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_FAILURE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return appContext.checkSelfPermission(
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val UNIQUE_WORK_NAME = "payment_reconciliation"

        /**
         * Worker-side retry ceiling.  Independent from
         * [CheckoutViewModel.MAX_CONFIRM_RETRIES] so the background
         * reconciler can keep going past the foreground budget.
         */
        const val MAX_BACKGROUND_RETRIES = 8

        // Foreground notification stays put while the worker is running;
        // the resolved (success/failure) notification gets a different id
        // so it survives independently after the worker exits.
        private const val NOTIFICATION_ID_FOREGROUND = 30_010
        private const val NOTIFICATION_ID_RESOLVED = 30_011
        private const val REQUEST_CODE_FOREGROUND = 30_100
        private const val REQUEST_CODE_SUCCESS = 30_101
        private const val REQUEST_CODE_FAILURE = 30_102

        /**
         * Enqueue or replace the unique reconciliation worker.  Safe to
         * call multiple times — WorkManager dedupes by [UNIQUE_WORK_NAME].
         *
         * Wrapped in a try/catch so a missing/unitialised WorkManager
         * (e.g. early-launch race, or a host process that didn't run our
         * Application class) never crashes the checkout flow.  The user's
         * foreground retries still drive reconciliation.
         */
        fun enqueue(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = OneTimeWorkRequestBuilder<PaymentReconciliationWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        BACKOFF_INITIAL_SECONDS,
                        TimeUnit.SECONDS,
                    )
                    .addTag(UNIQUE_WORK_NAME)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            } catch (t: Throwable) {
                Timber.w(t, "[PaymentReconcile] WorkManager unavailable — reconciliation deferred")
            }
        }

        /** Cancel any pending reconciliation work. */
        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(UNIQUE_WORK_NAME)
            } catch (t: Throwable) {
                Timber.w(t, "[PaymentReconcile] WorkManager cancel failed")
            }
        }

        private const val BACKOFF_INITIAL_SECONDS = 30L
    }
}

/**
 * Reconstructs a [BookingDraft] from a persisted pending-payment record so
 * the failure-notification deep link can land in CheckoutScreen with the
 * trip pre-filled.  Returns null when the record is missing the time-window
 * fields the draft requires — in that case the deep link falls through to
 * the listing detail page instead.
 */
private fun PendingNativePayment.toBookingDraft(): BookingDraft? {
    val listingId = listingId ?: return null
    val start = startTimeISO ?: return null
    val end = endTimeISO ?: return null
    val date = start.substringBefore('T').takeIf { it.isNotBlank() } ?: return null
    return BookingDraft(
        listingId = listingId,
        date = date,
        startTimeISO = start,
        endTimeISO = end,
        guests = 1,
    )
}
