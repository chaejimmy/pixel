package com.pacedream.app.feature.checkout

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.test.core.app.ApplicationProvider
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins down the idempotency contract for [PaymentReconciliationWorker]:
 *
 *  1. With no persisted record, the worker is a no-op and never touches the
 *     network.  This is the "already handled" path — confirm-booking
 *     succeeded earlier and the store was cleared.
 *  2. With a persisted record carrying a non-null `confirmedBookingId`,
 *     the worker is also a no-op — even though the record is still on
 *     disk, the booking exists on the backend and re-running must not
 *     resubmit the confirm call (which the idempotency key would dedupe
 *     anyway, but we don't want the latency or the wasted round trip).
 *  3. Running the worker through a successful confirm and then again must
 *     short-circuit the second run — i.e. the second invocation produces
 *     zero new network calls.  This guards the worker against double-firing
 *     the success notification or moving the store off Succeeded.
 *
 * The fake repository records every invocation so the test can assert the
 * exact wire-call count rather than relying on indirect effects.  Robolectric
 * stands up an in-memory Application + SharedPreferences so we can use the
 * real PendingPaymentStore — its state-flow transitions are part of the
 * worker's behaviour and are exercised end-to-end here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PaymentReconciliationWorkerIdempotencyTest {

    private lateinit var context: Context
    private lateinit var store: PendingPaymentStore
    private lateinit var repo: RecordingNativePaymentRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clean slate per test — Robolectric's SharedPreferences are
        // process-scoped, not test-scoped.
        context
            .getSharedPreferences("pacedream_pending_payment", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = PendingPaymentStore(context, Json { ignoreUnknownKeys = true })
        repo = RecordingNativePaymentRepository()
    }

    @Test
    fun `worker is a no-op when no pending record exists`() {
        // Pre-condition: nothing in flight.
        assertEquals(PendingPaymentState.None, store.state.value)

        val result = runBlocking { buildWorker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, repo.confirmCalls)
        assertEquals(0, repo.reportFailureCalls)
    }

    @Test
    fun `worker short-circuits when record carries a confirmedBookingId`() {
        // Already-confirmed marker — re-running the worker for this record
        // (e.g. WorkManager retried us after a transient crash) must be a
        // guaranteed no-op.
        store.save(
            PendingNativePayment(
                clientSecret = "pi_test_secret_abc",
                quoteId = "quote_42",
                paymentIntentId = "pi_test",
                paymentSucceededLocally = true,
                confirmedBookingId = "booking_already_locked_in",
            )
        )

        val result = runBlocking { buildWorker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, repo.confirmCalls)
        assertEquals(0, repo.reportFailureCalls)
        // The record stays put — the marker exists exactly so we recognise
        // future re-runs as idempotent.
        assertEquals("booking_already_locked_in", store.load()?.confirmedBookingId)
    }

    @Test
    fun `re-running after a successful confirm produces no extra network calls`() {
        store.save(
            PendingNativePayment(
                clientSecret = "pi_double_secret_xyz",
                quoteId = "quote_99",
                paymentIntentId = "pi_double",
                paymentSucceededLocally = true,
                confirmIdempotencyKey = "idem_99",
                confirmRequestId = "req_99",
            )
        )
        repo.confirmReturns = ApiResult.Success(
            ConfirmBookingResponse(
                success = true,
                booking = ConfirmBookingData(id = "booking_double", title = "T"),
            )
        )

        // First run: confirms with the backend.
        val firstResult = runBlocking { buildWorker().doWork() }
        assertEquals(ListenableWorker.Result.success(), firstResult)
        assertEquals(1, repo.confirmCalls)

        // The store has transitioned to Succeeded.
        assertTrue(store.state.value is PendingPaymentState.Succeeded)

        // Second run: should be a no-op — the record was cleared on success
        // and a re-enqueued worker has nothing to do.
        val secondResult = runBlocking { buildWorker().doWork() }
        assertEquals(ListenableWorker.Result.success(), secondResult)
        // Confirm call count is unchanged: the idempotency contract holds.
        assertEquals(1, repo.confirmCalls)
        assertEquals(0, repo.reportFailureCalls)
    }

    // ── Test fixtures ────────────────────────────────────────────────

    private fun buildWorker(): PaymentReconciliationWorker =
        TestListenableWorkerBuilder<PaymentReconciliationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = PaymentReconciliationWorker(
                    appContext,
                    workerParameters,
                    store,
                    repo,
                )
            })
            .build()

    /**
     * NativePaymentRepository fake that records invocations.  The parent
     * class is `open` and we override every method the worker touches so
     * the (uninitialised) ApiClient / AppConfig dependencies on the super
     * constructor are never dereferenced — the same pattern
     * [CheckoutViewModelPaymentReconciliationTest] uses.
     */
    private open class RecordingNativePaymentRepository : NativePaymentRepository(
        apiClient = uninitialised(),
        appConfig = uninitialised(),
        json = Json.Default,
    ) {
        var confirmCalls: Int = 0
            private set
        var reportFailureCalls: Int = 0
            private set
        var confirmReturns: ApiResult<ConfirmBookingResponse> =
            ApiResult.Failure(ApiError.Unknown("not configured"))

        override fun extractPaymentIntentId(clientSecret: String): String? {
            // Mirror the real parser so the worker proceeds to the
            // confirm-booking step.
            val parts = clientSecret.split("_secret_")
            return parts.firstOrNull()?.takeIf { it.startsWith("pi_") }
        }

        override suspend fun confirmBooking(
            paymentIntentId: String,
            idempotencyKey: String?,
            requestId: String?,
        ): ApiResult<ConfirmBookingResponse> {
            confirmCalls += 1
            return confirmReturns
        }

        override suspend fun reportFailure(
            paymentIntentId: String,
            quoteId: String?,
            retryCount: Int,
            errorMessage: String?,
            errorCode: String?,
        ): ApiResult<ReportFailureResponse> {
            reportFailureCalls += 1
            return ApiResult.Success(ReportFailureResponse(success = true))
        }
    }
}

/**
 * Lets us hand a parent class a non-null reference whose methods are never
 * called because every accessor is overridden by the fake.  Same pattern as
 * the matching helper in [CheckoutViewModelPaymentReconciliationTest].
 */
@Suppress("UNCHECKED_CAST")
private fun <T> uninitialised(): T = null as T
