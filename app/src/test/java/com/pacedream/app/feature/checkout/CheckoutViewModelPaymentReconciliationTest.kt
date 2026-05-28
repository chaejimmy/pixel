package com.pacedream.app.feature.checkout

import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.shourov.apps.pacedream.core.common.result.Result as DomainResult
import com.shourov.apps.pacedream.core.data.repository.BookingRepository
import com.shourov.apps.pacedream.model.BookingModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Behavioural regression suite for the post-payment hardening pass:
 *
 *  1. Double-tapping "Pay" creates exactly one PaymentIntent on the
 *     backend (idempotency-key + in-flight gate).
 *  2. A PaymentSheet failure routes through StripeErrorMapper and
 *     surfaces an inline error with [CheckoutFailureKind.PAYMENT_FAILED]
 *     so the screen can render the inline ErrorBanner + "Try again"
 *     CTA above the Pay button (rather than just a snackbar).
 *  3. An interrupted payment (process killed mid-3DS) hydrates the
 *     persisted [PendingNativePayment] into a
 *     PAYMENT_CAPTURED_PENDING_CONFIRMATION state on the very next
 *     view-model construction, and enqueues the worker so the user
 *     sees the reconciliation banner instead of staring at a quote.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelPaymentReconciliationTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Test 1: double-tap Pay creates one intent ─────────────────

    @Test
    fun `double-tap Pay creates exactly one PaymentIntent`() = runTest(dispatcher) {
        val intentGate = CompletableDeferred<Unit>()
        val paymentIntentCalls = AtomicInteger(0)
        val capturedIdempotencyKeys = mutableListOf<String?>()
        val paymentRepo = object : RecordingPaymentRepository() {
            override suspend fun createQuote(
                listingId: String,
                bookingType: String,
                startTime: String,
                endTime: String,
                quantity: Int,
                idempotencyKey: String?,
            ): ApiResult<QuoteResponse> = ApiResult.Success(sampleQuote)

            override suspend fun createPaymentIntent(
                quoteId: String,
                idempotencyKey: String?,
            ): ApiResult<PaymentSheetConfig> {
                paymentIntentCalls.incrementAndGet()
                capturedIdempotencyKeys += idempotencyKey
                // Suspend so the second tap arrives WHILE the first
                // createPaymentIntent is still in flight — the worst
                // case for a stale-state-based debounce.
                intentGate.await()
                return ApiResult.Success(
                    PaymentSheetConfig(
                        publishableKey = "pk_test_123",
                        paymentIntentClientSecret = "pi_xyz_secret_abc",
                    )
                )
            }

            override suspend fun resolvePublishableKey(config: PaymentSheetConfig): String? =
                config.publishableKey

            override fun extractPaymentIntentId(clientSecret: String): String? = "pi_xyz"
        }

        val vm = buildViewModel(paymentRepo = paymentRepo)
        vm.setDraft(sampleDraft)
        advanceUntilIdle() // settle the quote fetch → status = READY

        assertEquals(CheckoutStatus.READY, vm.uiState.value.status)

        // Fire the two taps essentially synchronously — the second
        // tap runs BEFORE the first createPaymentIntent returns, which
        // is the precise race the atomic in-flight gate closes.
        vm.submitPayment()
        vm.submitPayment()
        // Give the launched coroutines a chance to advance up to the
        // suspension inside createPaymentIntent.
        advanceUntilIdle()

        assertEquals(
            "Only one createPaymentIntent should be triggered for a double-tap",
            1,
            paymentIntentCalls.get(),
        )
        assertTrue(
            "The single attempt should have sent a non-blank Idempotency-Key",
            !capturedIdempotencyKeys.single().isNullOrBlank(),
        )
        assertTrue(
            "UI state must reflect in-flight Pay so the button is disabled",
            vm.uiState.value.isPaymentInFlight,
        )

        // Release the gate so the test doesn't leak a suspended coroutine.
        intentGate.complete(Unit)
        advanceUntilIdle()
    }

    // ── Test 2: PaymentSheetResult.Failed surfaces ErrorBanner ────

    @Test
    fun `PaymentSheet Failed surfaces inline payment-failed error banner`() = runTest(dispatcher) {
        val paymentRepo = object : RecordingPaymentRepository() {
            override suspend fun createQuote(
                listingId: String,
                bookingType: String,
                startTime: String,
                endTime: String,
                quantity: Int,
                idempotencyKey: String?,
            ): ApiResult<QuoteResponse> = ApiResult.Success(sampleQuote)
        }
        val vm = buildViewModel(paymentRepo = paymentRepo)
        vm.setDraft(sampleDraft)
        advanceUntilIdle()

        assertEquals(CheckoutStatus.READY, vm.uiState.value.status)

        // The screen calls onPaymentSheetFailed inside the rememberPaymentSheet
        // callback after routing the throwable through StripeErrorMapper.  The
        // VM accepts the already-mapped string and must surface a
        // PAYMENT_FAILED state so the screen can render the inline
        // ErrorBanner + "Try again" CTA above the Pay button.
        vm.onPaymentSheetFailed("Your card was declined")

        val failed = vm.uiState.value
        assertEquals(CheckoutStatus.FAILED, failed.status)
        assertEquals(
            "ErrorBanner should categorise this as a payment failure",
            CheckoutFailureKind.PAYMENT_FAILED,
            failed.failureKind,
        )
        assertNotNull(
            "ErrorBanner needs a non-null message to render",
            failed.errorMessage,
        )
        assertTrue(
            "Message should be the curated copy from StripeErrorMapper, not the raw input",
            failed.errorMessage!!.contains("declined", ignoreCase = true),
        )
        assertFalse(
            "Payment is no longer in flight after a failure",
            failed.isPaymentInFlight,
        )
    }

    // ── Test 3: interrupted payment + worker fires right transition

    @Test
    fun `interrupted payment is hydrated into pending-confirmation state on next launch`() =
        runTest(dispatcher) {
            val scheduler = CountingScheduler()
            // Pre-seed the store as if a previous process died after the
            // PaymentSheet completed but before confirm-booking landed.
            val store = InMemoryPendingPaymentStore().apply {
                save(
                    PendingNativePayment(
                        clientSecret = "pi_xyz_secret_abc",
                        quoteId = "q_1",
                        paymentIntentId = "pi_xyz",
                        listingId = "listing_1",
                        retryCount = 0,
                        paymentSucceededLocally = true,
                        confirmIdempotencyKey = "dedup_key_1",
                        confirmRequestId = "req_1",
                        amountCents = 12500,
                        currency = "usd",
                        listingTitle = "Test Listing",
                    )
                )
            }
            // confirm-booking will succeed on the recovery retry so the
            // VM transitions all the way through to SUCCEEDED.
            val paymentRepo = object : RecordingPaymentRepository() {
                override suspend fun confirmBooking(
                    paymentIntentId: String,
                    idempotencyKey: String?,
                    requestId: String?,
                ): ApiResult<ConfirmBookingResponse> {
                    // Recovery MUST replay the persisted idempotency key.
                    assertEquals("dedup_key_1", idempotencyKey)
                    assertEquals("req_1", requestId)
                    return ApiResult.Success(
                        ConfirmBookingResponse(
                            success = true,
                            booking = ConfirmBookingData(id = "b_42", status = "confirmed"),
                        )
                    )
                }

                override fun extractPaymentIntentId(clientSecret: String): String? = "pi_xyz"
            }

            val vm = buildViewModel(
                paymentRepo = paymentRepo,
                pendingPaymentStore = store,
                scheduler = scheduler,
            )

            // attemptRecoveryIfNeeded runs synchronously in init.  The
            // very first state emission MUST already be the pending
            // confirmation state — never IDLE / READY.
            val initial = vm.uiState.value
            assertEquals(
                "Recovery should land directly on PAYMENT_CAPTURED_PENDING_CONFIRMATION",
                CheckoutStatus.PAYMENT_CAPTURED_PENDING_CONFIRMATION,
                initial.status,
            )
            assertEquals("pi_xyz", initial.pendingPaymentIntentId)
            assertEquals("req_1", initial.lastConfirmRequestId)
            assertTrue(
                "Worker must be enqueued so reconciliation continues in the background",
                scheduler.enqueueCount.get() >= 1,
            )

            // Now drive the auto-retry through to a SUCCEEDED state to
            // prove the right transition fires once confirm-booking
            // resolves (the same transition the worker drives).
            advanceUntilIdle()

            val resolved = vm.uiState.value
            assertEquals(CheckoutStatus.SUCCEEDED, resolved.status)
            assertEquals("b_42", resolved.bookingId)
            assertEquals(
                "Pending record must be cleared once the booking is live",
                null,
                store.load(),
            )
        }

    // ── Helpers ───────────────────────────────────────────────────

    private fun buildViewModel(
        paymentRepo: NativePaymentRepository = RecordingPaymentRepository(),
        pendingPaymentStore: PendingPaymentStore = InMemoryPendingPaymentStore(),
        scheduler: ReconciliationScheduler = CountingScheduler(),
        bookingGate: CheckoutBookingGate = AlwaysAvailableGate(),
    ): CheckoutViewModel = CheckoutViewModel(
        nativePaymentRepository = paymentRepo,
        bookingGate = bookingGate,
        pendingPaymentStore = pendingPaymentStore,
        reconciliationScheduler = scheduler,
    )

    private val sampleDraft = BookingDraft(
        listingId = "listing_1",
        listingType = "time-based",
        date = "2026-06-10",
        startTimeISO = "2026-06-10T10:00:00",
        endTimeISO = "2026-06-10T12:00:00",
        guests = 2,
    )

    private val sampleQuote = QuoteResponse(
        quoteId = "q_1",
        currency = "usd",
        baseAmountCents = 10000,
        serviceFeeCents = 2000,
        taxCents = 500,
        totalCents = 12500,
        expiresAt = null,
    )

    /**
     * Base fake repository — every method throws unless overridden so
     * each test only stubs what it actually exercises.  Casting an
     * anonymous-derived class lets us subclass and override only the
     * specific calls.
     */
    private open class RecordingPaymentRepository : NativePaymentRepository(
        apiClient = throwingApiClient(),
        appConfig = throwingAppConfig(),
        json = kotlinx.serialization.json.Json.Default,
    ) {
        override suspend fun createQuote(
            listingId: String,
            bookingType: String,
            startTime: String,
            endTime: String,
            quantity: Int,
            idempotencyKey: String?,
        ): ApiResult<QuoteResponse> =
            ApiResult.Failure(ApiError.Unknown("not stubbed"))

        override suspend fun createPaymentIntent(
            quoteId: String,
            idempotencyKey: String?,
        ): ApiResult<PaymentSheetConfig> =
            ApiResult.Failure(ApiError.Unknown("not stubbed"))

        override suspend fun confirmBooking(
            paymentIntentId: String,
            idempotencyKey: String?,
            requestId: String?,
        ): ApiResult<ConfirmBookingResponse> =
            ApiResult.Failure(ApiError.Unknown("not stubbed"))

        override suspend fun resolvePublishableKey(config: PaymentSheetConfig): String? = null

        override fun extractPaymentIntentId(clientSecret: String): String? =
            clientSecret.substringBefore("_secret_").takeIf { it.startsWith("pi_") }

        override suspend fun reportFailure(
            paymentIntentId: String,
            quoteId: String?,
            retryCount: Int,
            errorMessage: String?,
            errorCode: String?,
        ): ApiResult<ReportFailureResponse> =
            ApiResult.Success(ReportFailureResponse(success = true))
    }

    /**
     * In-memory replacement for the SharedPreferences-backed store —
     * no Android Context required, so the VM is fully unit-testable.
     */
    private class InMemoryPendingPaymentStore : PendingPaymentStore(
        context = throwingContext(),
        json = kotlinx.serialization.json.Json.Default,
    ) {
        private var record: PendingNativePayment? = null

        override fun load(): PendingNativePayment? = record
        override fun save(record: PendingNativePayment): Boolean {
            this.record = record
            return true
        }
        override fun clear(): Boolean {
            record = null
            return true
        }
        override fun recordRetryAttempt(): PendingNativePayment? {
            val cur = record ?: return null
            val updated = cur.copy(
                retryCount = cur.retryCount + 1,
                lastAttemptAt = System.currentTimeMillis(),
            )
            record = updated
            return updated
        }
        override fun markPaymentSucceededLocally(): PendingNativePayment? {
            val cur = record ?: return null
            val updated = cur.copy(paymentSucceededLocally = true)
            record = updated
            return updated
        }
        override fun recordLastError(message: String?): PendingNativePayment? {
            val cur = record ?: return null
            val updated = cur.copy(lastErrorMessage = message?.take(512))
            record = updated
            return updated
        }
    }

    private class CountingScheduler : ReconciliationScheduler {
        val enqueueCount = AtomicInteger(0)
        val cancelCount = AtomicInteger(0)
        override fun enqueue() { enqueueCount.incrementAndGet() }
        override fun cancel() { cancelCount.incrementAndGet() }
    }

    private class AlwaysAvailableGate : CheckoutBookingGate {
        override suspend fun checkAvailability(
            listingId: String,
            startDate: String,
            endDate: String,
        ): DomainResult<BookingRepository.AvailabilityCheckResult> =
            DomainResult.Success(
                BookingRepository.AvailabilityCheckResult(
                    available = true,
                    reason = null,
                    listingBookable = true,
                    listingStatus = "active",
                    listingTimezone = null,
                )
            )

        override suspend fun cacheBooking(booking: BookingModel) { /* no-op */ }
    }

    companion object {
        /**
         * Bypass Kotlin's `as`-cast null-check by routing through a
         * generic parameter — at the JVM level type erasure makes the
         * cast a no-op, so `uninitialised<Context>()` hands us a null
         * Context the subclass never dereferences (every method that
         * touches `context` is overridden in our test fakes).
         *
         * Used purely to satisfy the parent class's non-null constructor
         * signature without standing up Android infrastructure.
         */
        @Suppress("UNCHECKED_CAST")
        private fun <T> uninitialised(): T = null as T

        private fun throwingApiClient(): com.pacedream.app.core.network.ApiClient =
            uninitialised()

        private fun throwingAppConfig(): com.pacedream.app.core.config.AppConfig =
            uninitialised()

        private fun throwingContext(): android.content.Context = uninitialised()
    }
}
