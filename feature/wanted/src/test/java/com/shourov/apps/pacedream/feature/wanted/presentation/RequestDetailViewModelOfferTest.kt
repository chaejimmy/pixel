package com.shourov.apps.pacedream.feature.wanted.presentation

import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Locks the offer submission contract on the detail view model:
 *  - Success flips `submitted` to true so the bottom sheet can pivot
 *    to its confirmation layout (the screen never auto-dismisses).
 *  - Every failure mode maps to a short, user-friendly string via the
 *    same pattern used in `CreateRequestViewModel.friendlyError` —
 *    raw exception messages must never bubble through to the UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RequestDetailViewModelOfferTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitOffer success flips submitted flag for the sheet's success layout`() = runTest(dispatcher) {
        val viewModel = newViewModel(
            offerResult = Result.success(
                WantedOffer(
                    id = "offer-1",
                    requestId = REQUEST_ID,
                    price = 25.0,
                    message = "I'll bring my own tools.",
                ),
            ),
        )
        viewModel.load(REQUEST_ID)
        advanceUntilIdle()

        viewModel.onPriceChange("25")
        viewModel.onMessageChange("I'll bring my own tools.")
        viewModel.submitOffer()
        advanceUntilIdle()

        val after = viewModel.offer.value
        assertTrue("submitted must be true on success", after.submitted)
        assertFalse(after.submitting)
        assertNull(after.error)
    }

    @Test
    fun `submitOffer maps 401 to a friendly auth message`() = runTest(dispatcher) {
        val viewModel = newViewModel(
            offerResult = Result.failure(IllegalStateException("HTTP 401 unauthorized")),
        )
        submitValidOffer(viewModel)
        advanceUntilIdle()

        val error = viewModel.offer.value.error
        assertEquals("Please sign in again to send your offer.", error)
        assertNoRawLeak(error)
    }

    @Test
    fun `submitOffer maps 429 to a friendly rate limit message`() = runTest(dispatcher) {
        val viewModel = newViewModel(
            offerResult = Result.failure(IllegalStateException("HTTP 429 Too Many Requests")),
        )
        submitValidOffer(viewModel)
        advanceUntilIdle()

        val error = viewModel.offer.value.error
        assertEquals(
            "You're sending offers a bit too quickly. Please wait a moment.",
            error,
        )
        assertNoRawLeak(error)
    }

    @Test
    fun `submitOffer maps network failures to the offline message`() = runTest(dispatcher) {
        val viewModel = newViewModel(
            offerResult = Result.failure(IOException("connect timeout: 10.0.0.1:443")),
        )
        submitValidOffer(viewModel)
        advanceUntilIdle()

        val error = viewModel.offer.value.error
        assertEquals(
            "You appear to be offline. Please check your connection.",
            error,
        )
        assertNoRawLeak(error)
    }

    @Test
    fun `submitOffer maps unknown failures to a generic friendly message`() = runTest(dispatcher) {
        val viewModel = newViewModel(
            offerResult = Result.failure(IllegalStateException("Server gibberish 5xx <html>boom</html>")),
        )
        submitValidOffer(viewModel)
        advanceUntilIdle()

        val error = viewModel.offer.value.error
        assertEquals("Couldn't send your offer. Please try again.", error)
        assertNoRawLeak(error)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun newViewModel(
        offerResult: Result<WantedOffer>,
    ): RequestDetailViewModel = RequestDetailViewModel(
        repository = FakeRepository(offerResult),
    )

    private fun submitValidOffer(viewModel: RequestDetailViewModel) {
        viewModel.load(REQUEST_ID)
        viewModel.onPriceChange("25")
        viewModel.onMessageChange("I'll bring my own tools.")
        viewModel.submitOffer()
    }

    private fun assertNoRawLeak(error: String?) {
        val msg = error ?: error("expected an error string")
        assertFalse("Raw HTTP details must not leak", msg.contains("HTTP"))
        assertFalse("Raw 5xx codes must not leak", msg.contains("5xx"))
        assertFalse("HTML must not leak", msg.contains("<html>"))
        assertFalse("Raw IP/port must not leak", msg.contains("10.0.0"))
    }

    private class FakeRepository(
        private val offerResult: Result<WantedOffer>,
    ) : WantedRepository {
        override suspend fun getRequests(): Result<List<WantedRequest>> =
            Result.success(emptyList())

        override suspend fun getRequest(id: String): Result<WantedRequest> =
            Result.success(
                WantedRequest(
                    id = id,
                    title = "Title",
                    description = "Description",
                    type = "service",
                    category = "moving",
                    location = "",
                    budget = null,
                    dateTime = null,
                    imageUrl = null,
                    authorName = "Maya",
                ),
            )

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> =
            error("createRequest must not be hit from offer tests")

        override suspend fun createOffer(
            requestId: String,
            body: CreateOfferBody,
        ): Result<WantedOffer> = offerResult
    }

    private companion object {
        const val REQUEST_ID = "req-1"
    }
}
