package com.pacedream.app.feature.checkout

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pacedream.app.core.network.ApiResult
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
 * The worker is **idempotency-key safe**:
 *   - It reads the persisted [PendingNativePayment] every run, so a
 *     ViewModel retry that already drained the queue won't double-bill.
 *   - It reuses [PendingNativePayment.confirmIdempotencyKey] on every
 *     attempt, guaranteeing the backend deduplicates and returns the
 *     same booking instead of creating a duplicate.
 *
 * Termination conditions:
 *   - On success → clears the store, returns [Result.success].
 *   - When report-failure responds `alreadyBooked = true` → clears
 *     the store, returns [Result.success].
 *   - On a transient failure → returns [Result.retry] so WorkManager
 *     re-runs us with its own backoff schedule.
 *   - When local retry budget is exhausted → posts report-failure and
 *     returns [Result.success]: the client gives up, the backend takes
 *     over reconciliation.
 */
@HiltWorker
class PaymentReconciliationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pendingPaymentStore: PendingPaymentStore,
    private val nativePaymentRepository: NativePaymentRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val record = pendingPaymentStore.load()
        if (record == null || !record.paymentSucceededLocally) {
            // No work to do — either nothing pending or the sheet was
            // abandoned (no money captured).
            return Result.success()
        }

        val paymentIntentId = record.paymentIntentId
            ?: nativePaymentRepository.extractPaymentIntentId(record.clientSecret)
        if (paymentIntentId == null) {
            Timber.w("[PaymentReconcile] corrupt record — no PaymentIntent id, clearing")
            pendingPaymentStore.clear()
            return Result.success()
        }

        if (record.retryCount >= MAX_BACKGROUND_RETRIES) {
            Timber.w(
                "[PaymentReconcile] retry budget exhausted (count=%d) pi=%s — reporting",
                record.retryCount,
                paymentIntentId,
            )
            reportAndPossiblyClear(record, paymentIntentId, "Background retry budget exhausted")
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
                Timber.i(
                    "[PaymentReconcile] confirmed bookingId=%s pi=%s",
                    result.data.booking?.id,
                    paymentIntentId,
                )
                pendingPaymentStore.clear()
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
                    reportAndPossiblyClear(
                        updated,
                        paymentIntentId,
                        result.error.message ?: "confirm-booking failed",
                    )
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        }
    }

    private suspend fun reportAndPossiblyClear(
        record: PendingNativePayment,
        paymentIntentId: String,
        errorMessage: String,
    ) {
        val reported = nativePaymentRepository.reportFailure(
            paymentIntentId = paymentIntentId,
            quoteId = record.quoteId,
            retryCount = record.retryCount,
            errorMessage = errorMessage,
            errorCode = "BACKGROUND_RECONCILE_EXHAUSTED",
        )
        if (reported is ApiResult.Success && reported.data.alreadyBooked == true) {
            Timber.i("[PaymentReconcile] already booked pi=%s — clearing", paymentIntentId)
            pendingPaymentStore.clear()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "payment_reconciliation"

        /**
         * Worker-side retry ceiling.  Independent from
         * [CheckoutViewModel.MAX_CONFIRM_RETRIES] so the background
         * reconciler can keep going past the foreground budget.
         */
        const val MAX_BACKGROUND_RETRIES = 8

        /**
         * Enqueue or replace the unique reconciliation worker.  Safe to
         * call multiple times — WorkManager dedupes by [UNIQUE_WORK_NAME].
         */
        fun enqueue(context: Context) {
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
        }

        /** Cancel any pending reconciliation work. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        private const val BACKOFF_INITIAL_SECONDS = 30L
    }
}
