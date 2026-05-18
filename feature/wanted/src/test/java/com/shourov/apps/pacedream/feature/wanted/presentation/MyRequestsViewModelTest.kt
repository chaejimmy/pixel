package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.SavedStateHandle
import com.shourov.apps.pacedream.feature.wanted.data.OfferSeenTracker
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.model.HostListingSummary
import com.shourov.apps.pacedream.feature.wanted.model.MyRequestsTab
import com.shourov.apps.pacedream.feature.wanted.model.MyRequestsUiState
import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lifecycle behaviour of [MyRequestsViewModel]:
 *
 *  - Tab partitioning honours both the server-declared status column and
 *    the client-side expiry computation, so a stale Active row whose
 *    `expiresAt` has passed shows up in the Expired tab immediately.
 *  - Renew Request flips the post back to Active with a new expiry,
 *    moving it out of the Expired tab.
 *  - Mark as Fulfilled and Cancel transition the post into the respective
 *    history tabs.
 *  - Acceptance: My Requests still displays the expired history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MyRequestsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fixedToday: LocalDate = LocalDate.of(2026, 5, 18)
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-05-18T12:00:00Z"),
        ZoneId.of("UTC"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lifecycle tabs partition the requester's posts by effective status`() = runTest(dispatcher) {
        val seed = listOf(
            request("active-1", status = RequestStatus.Active, expiresAt = "2026-12-01"),
            request("active-fresh", status = RequestStatus.Active, expiresAt = "2026-05-25"),
            // Active server-side but expired by the calendar — must land in Expired.
            request("stale-active", status = RequestStatus.Active, expiresAt = "2026-04-01"),
            request("expired-server", status = RequestStatus.Expired, expiresAt = "2026-03-01"),
            request("fulfilled-1", status = RequestStatus.Fulfilled),
            request("cancelled-1", status = RequestStatus.Cancelled),
        )
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        val content = viewModel.state.value as MyRequestsUiState.Content
        assertEquals(MyRequestsTab.Active, content.selectedTab)
        assertEquals(
            "Active tab should hold only currently-active requests",
            setOf("active-1", "active-fresh"),
            content.visible.map { it.id }.toSet(),
        )
        assertEquals(
            "Stale active rows are reclassified as Expired client-side",
            2,
            content.counts[MyRequestsTab.Expired],
        )
        assertEquals(1, content.counts[MyRequestsTab.Fulfilled])
        assertEquals(1, content.counts[MyRequestsTab.Cancelled])
    }

    @Test
    fun `selecting Expired tab surfaces both server- and client-expired rows`() = runTest(dispatcher) {
        // Acceptance: My Requests still displays expired history.
        val seed = listOf(
            request("active-1", status = RequestStatus.Active, expiresAt = "2026-12-01"),
            request("stale-active", status = RequestStatus.Active, expiresAt = "2026-04-01"),
            request("expired-server", status = RequestStatus.Expired, expiresAt = "2026-03-01"),
        )
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        viewModel.selectTab(MyRequestsTab.Expired)
        advanceUntilIdle()

        val content = viewModel.state.value as MyRequestsUiState.Content
        assertEquals(MyRequestsTab.Expired, content.selectedTab)
        assertEquals(
            setOf("stale-active", "expired-server"),
            content.visible.map { it.id }.toSet(),
        )
    }

    @Test
    fun `renew transitions an Expired request back to Active`() = runTest(dispatcher) {
        val repository = ScriptedRepository(
            initial = listOf(
                request("expired-1", status = RequestStatus.Expired, expiresAt = "2026-04-01"),
            ),
        )
        repository.nextUpdateReturns { id, _, expiresAt ->
            request(
                id = id,
                status = RequestStatus.Active,
                expiresAt = expiresAt ?: "2026-06-17",
            )
        }
        val viewModel = newViewModel(repository = repository)
        advanceUntilIdle()
        viewModel.selectTab(MyRequestsTab.Expired)
        advanceUntilIdle()

        viewModel.renew("expired-1")
        advanceUntilIdle()

        // After renewal the Expired tab should be empty, and switching to
        // Active should reveal the renewed record.
        viewModel.selectTab(MyRequestsTab.Active)
        val active = viewModel.state.value as MyRequestsUiState.Content
        assertEquals(
            listOf("expired-1"),
            active.visible.map { it.id },
        )
        // The recorded renewal call must include a future expiry computed
        // from the injected clock.
        val (_, _, expiresAt) = repository.lastUpdate
            ?: error("renew must call updateRequestStatus")
        assertNotNull("renew must push a new expiresAt forward", expiresAt)
        assertTrue(
            "the new expiry must be in the future relative to the fixed clock",
            LocalDate.parse(expiresAt).isAfter(fixedToday),
        )
    }

    @Test
    fun `markFulfilled moves the post into the Fulfilled tab`() = runTest(dispatcher) {
        val repository = ScriptedRepository(
            initial = listOf(
                request("active-1", status = RequestStatus.Active, expiresAt = "2026-12-01"),
            ),
        )
        repository.nextUpdateReturns { id, status, _ ->
            request(id = id, status = status, expiresAt = "2026-12-01")
        }
        val viewModel = newViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.markFulfilled("active-1")
        advanceUntilIdle()

        viewModel.selectTab(MyRequestsTab.Fulfilled)
        val fulfilled = viewModel.state.value as MyRequestsUiState.Content
        assertEquals(listOf("active-1"), fulfilled.visible.map { it.id })

        viewModel.selectTab(MyRequestsTab.Active)
        val active = viewModel.state.value as MyRequestsUiState.Content
        assertTrue("the post leaves Active once marked fulfilled", active.visible.isEmpty())
    }

    @Test
    fun `cancel moves the post into the Cancelled tab`() = runTest(dispatcher) {
        val repository = ScriptedRepository(
            initial = listOf(
                request("active-1", status = RequestStatus.Active, expiresAt = "2026-12-01"),
            ),
        )
        repository.nextUpdateReturns { id, status, _ ->
            request(id = id, status = status, expiresAt = "2026-12-01")
        }
        val viewModel = newViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.cancel("active-1")
        advanceUntilIdle()

        viewModel.selectTab(MyRequestsTab.Cancelled)
        val cancelled = viewModel.state.value as MyRequestsUiState.Content
        assertEquals(listOf("active-1"), cancelled.visible.map { it.id })
    }

    @Test
    fun `failed action surfaces an inline error and clears the pending flag`() = runTest(dispatcher) {
        val repository = ScriptedRepository(
            initial = listOf(
                request("active-1", status = RequestStatus.Active, expiresAt = "2026-12-01"),
            ),
        )
        repository.nextUpdateFails(RuntimeException("HTTP 500"))
        val viewModel = newViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.markFulfilled("active-1")
        advanceUntilIdle()

        val content = viewModel.state.value as MyRequestsUiState.Content
        assertNull(
            "the pending action flag must clear after the failure",
            content.pendingActionId,
        )
        assertNotNull(
            "a friendly error must surface on the banner",
            content.actionError,
        )
        // Original record stays put.
        viewModel.selectTab(MyRequestsTab.Active)
        val active = viewModel.state.value as MyRequestsUiState.Content
        assertEquals(listOf("active-1"), active.visible.map { it.id })

        viewModel.dismissActionError()
        assertNull(
            "dismissActionError clears the banner",
            (viewModel.state.value as MyRequestsUiState.Content).actionError,
        )
    }

    @Test
    fun `inner tab selection survives via SavedStateHandle`() = runTest(dispatcher) {
        val savedState = SavedStateHandle(
            mapOf("wanted.my_requests.inner_tab" to "expired"),
        )
        val viewModel = newViewModel(
            seed = listOf(
                request("expired-1", status = RequestStatus.Expired, expiresAt = "2026-04-01"),
            ),
            savedStateHandle = savedState,
        )
        advanceUntilIdle()

        assertEquals(MyRequestsTab.Expired, viewModel.selectedTab.value)
        val content = viewModel.state.value as MyRequestsUiState.Content
        assertEquals(MyRequestsTab.Expired, content.selectedTab)
    }

    @Test
    fun `concurrent action requests are debounced while one is in flight`() = runTest(dispatcher) {
        val repository = ScriptedRepository(
            initial = listOf(
                request("active-1", status = RequestStatus.Active, expiresAt = "2026-12-01"),
            ),
        )
        repository.nextUpdateReturns { id, status, _ ->
            request(id = id, status = status, expiresAt = "2026-12-01")
        }
        val viewModel = newViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.markFulfilled("active-1")
        // Second call must be ignored — the in-flight first call wins.
        viewModel.cancel("active-1")
        advanceUntilIdle()

        assertEquals(
            "only the first action must reach the repository",
            1,
            repository.updateCount,
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun newViewModel(
        seed: List<WantedRequest> = emptyList(),
        repository: ScriptedRepository = ScriptedRepository(seed),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): MyRequestsViewModel {
        // Robolectric stands up an Application context; OfferSeenTracker
        // uses SharedPreferences under the hood, which we wire to that
        // in-memory backing store for the duration of the test.
        val context = RuntimeEnvironment.getApplication()
        return MyRequestsViewModel(
            repository = repository,
            offerSeenTracker = OfferSeenTracker(context),
            savedStateHandle = savedStateHandle,
            clock = fixedClock,
        )
    }

    private fun request(
        id: String,
        status: RequestStatus,
        expiresAt: String? = null,
        startDate: String? = null,
        endDate: String? = null,
    ): WantedRequest = WantedRequest(
        id = id,
        title = "$id title",
        description = "$id description",
        type = "space",
        category = "parking",
        location = "City",
        budget = null,
        requestStartDate = startDate,
        requestEndDate = endDate,
        expiresAt = expiresAt,
        imageUrl = null,
        status = status,
    )

    private class ScriptedRepository(
        initial: List<WantedRequest>,
    ) : WantedRepository {
        private var stored: List<WantedRequest> = initial
        private var nextUpdate: ((String, RequestStatus, String?) -> WantedRequest)? = null
        private var failNextWith: Throwable? = null
        var updateCount: Int = 0
            private set
        var lastUpdate: Triple<String, RequestStatus, String?>? = null
            private set

        fun nextUpdateReturns(producer: (String, RequestStatus, String?) -> WantedRequest) {
            nextUpdate = producer
        }

        fun nextUpdateFails(error: Throwable) {
            failNextWith = error
        }

        override suspend fun getMyRequests(): Result<List<WantedRequest>> = Result.success(stored)

        override suspend fun updateRequestStatus(
            id: String,
            status: RequestStatus,
            expiresAt: String?,
        ): Result<WantedRequest> {
            updateCount += 1
            lastUpdate = Triple(id, status, expiresAt)
            failNextWith?.let {
                failNextWith = null
                return Result.failure(it)
            }
            val produced = nextUpdate?.invoke(id, status, expiresAt)
                ?: error("nextUpdateReturns was not configured")
            stored = stored.map { if (it.id == id) produced else it }
            return Result.success(produced)
        }

        override suspend fun renewRequest(id: String, newExpiry: String?): Result<WantedRequest> =
            updateRequestStatus(id, RequestStatus.Active, newExpiry)

        override suspend fun getRequests(): Result<List<WantedRequest>> =
            error("unused in MyRequests tests")

        override suspend fun getRequest(id: String): Result<WantedRequest> =
            error("unused in MyRequests tests")

        override suspend fun getOffersForRequest(requestId: String): Result<List<WantedOffer>> =
            error("unused in MyRequests tests")

        override suspend fun getMyOffers(): Result<List<WantedOffer>> =
            error("unused in MyRequests tests")

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> =
            error("unused in MyRequests tests")

        override suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer> =
            error("unused in MyRequests tests")

        override suspend fun getHostListings(): Result<List<HostListingSummary>> =
            Result.success(emptyList())

        override suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>> =
            Result.failure(IllegalStateException("unused in MyRequests tests"))
    }

}
