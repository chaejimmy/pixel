package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.SavedStateHandle
import com.shourov.apps.pacedream.feature.wanted.data.RequestsFiltersStore
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.model.FilterState
import com.shourov.apps.pacedream.feature.wanted.model.RequestSort
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the client-side filter + sort behavior of [RequestsViewModel].
 *
 * The repository returns one request per `(type, category, budget)` triple
 * below; tests assert the visible subset and ordering for each
 * [FilterState] permutation. Persistence is verified via the FakeFiltersStore
 * to keep the contract honest: a filter change must hit the store.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RequestsViewModelFilterTest {

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
    fun `defaults to all requests sorted as returned by the API`() = runTest(dispatcher) {
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is RequestsListUiState.Content)
        assertEquals(
            seed.map { it.id },
            (state as RequestsListUiState.Content).requests.map { it.id },
        )
    }

    @Test
    fun `setType Item narrows to item requests across all categories`() = runTest(dispatcher) {
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        viewModel.setType(WantedType.Item)

        val visible = (viewModel.state.value as RequestsListUiState.Content).requests
        assertTrue(
            "every visible request must be an item",
            visible.all { it.type.equals("item", ignoreCase = true) },
        )
        assertEquals(
            "the three item requests are: cam-cheap, cam-pricey, tools-no-budget",
            listOf("cam-cheap", "cam-pricey", "tools-no-budget"),
            visible.map { it.id }.sorted(),
        )
    }

    @Test
    fun `setType then setCategory camera shows only camera-item requests`() = runTest(dispatcher) {
        // Acceptance: selecting Item + camera shows only camera-item requests.
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        viewModel.setType(WantedType.Item)
        viewModel.setCategory("camera")

        val visible = (viewModel.state.value as RequestsListUiState.Content).requests
        assertEquals(
            listOf("cam-cheap", "cam-pricey"),
            visible.map { it.id }.sorted(),
        )
    }

    @Test
    fun `changing type clears the previously selected category`() = runTest(dispatcher) {
        // Categories are scoped per type — "camera" is meaningless under
        // Service, so the ViewModel must drop it on a type switch.
        val viewModel = newViewModel(seed)
        advanceUntilIdle()
        viewModel.setType(WantedType.Item)
        viewModel.setCategory("camera")
        assertEquals("camera", viewModel.filter.value.category)

        viewModel.setType(WantedType.Service)

        assertEquals(WantedType.Service, viewModel.filter.value.type)
        assertEquals(null, viewModel.filter.value.category)
    }

    @Test
    fun `HighestBudget sort orders by budget descending with nulls last`() = runTest(dispatcher) {
        // Acceptance: sort by highest budget orders by budget desc, nulls last.
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        viewModel.setSort(RequestSort.HighestBudget)

        val ids = (viewModel.state.value as RequestsListUiState.Content)
            .requests
            .map { it.id }
        // Budgets in seed: cam-pricey=500, parking=200, cam-cheap=80,
        // service-delivery=40, parking-cheap=null, tools-no-budget=null.
        // The two nulls must trail the four non-null entries (relative order
        // among nulls is unspecified by the spec, so we don't assert it).
        assertEquals(
            listOf("cam-pricey", "parking", "cam-cheap", "service-delivery"),
            ids.take(4),
        )
        assertTrue("nulls must be last", ids.drop(4).toSet() == setOf("parking-cheap", "tools-no-budget"))
    }

    @Test
    fun `filter and sort compose`() = runTest(dispatcher) {
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        viewModel.setType(WantedType.Item)
        viewModel.setSort(RequestSort.HighestBudget)

        val ids = (viewModel.state.value as RequestsListUiState.Content)
            .requests
            .map { it.id }
        // Item requests by budget desc, nulls last:
        // cam-pricey (500), cam-cheap (80), tools-no-budget (null).
        assertEquals(listOf("cam-pricey", "cam-cheap", "tools-no-budget"), ids)
    }

    @Test
    fun `clearFilters returns to the unfiltered Newest view`() = runTest(dispatcher) {
        val viewModel = newViewModel(seed)
        advanceUntilIdle()
        viewModel.setType(WantedType.Item)
        viewModel.setCategory("camera")
        viewModel.setSort(RequestSort.HighestBudget)
        assertTrue(viewModel.filter.value.isActive)

        viewModel.clearFilters()

        assertEquals(FilterState(), viewModel.filter.value)
        assertFalse(viewModel.filter.value.isActive)
        assertEquals(
            seed.map { it.id },
            (viewModel.state.value as RequestsListUiState.Content).requests.map { it.id },
        )
    }

    @Test
    fun `empty filtered match still returns Content with an empty list`() = runTest(dispatcher) {
        // The screen relies on Content+isEmpty to render the "No matching
        // requests" call to action, so this must not flip back to Loading.
        val viewModel = newViewModel(seed)
        advanceUntilIdle()

        viewModel.setType(WantedType.Item)
        viewModel.setCategory("electronics") // no electronics in the seed

        val state = viewModel.state.value
        assertTrue(state is RequestsListUiState.Content)
        assertTrue((state as RequestsListUiState.Content).requests.isEmpty())
    }

    @Test
    fun `refresh preserves the active filter`() = runTest(dispatcher) {
        // Acceptance: pull-to-refresh still works and preserves filters.
        val viewModel = newViewModel(seed)
        advanceUntilIdle()
        viewModel.setType(WantedType.Item)
        viewModel.setCategory("camera")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(WantedType.Item, viewModel.filter.value.type)
        assertEquals("camera", viewModel.filter.value.category)
        val visible = (viewModel.state.value as RequestsListUiState.Content).requests
        assertEquals(listOf("cam-cheap", "cam-pricey"), visible.map { it.id }.sorted())
    }

    @Test
    fun `filter changes are persisted to the on-disk store`() = runTest(dispatcher) {
        val store = FakeFiltersStore()
        val viewModel = newViewModel(seed, store = store)
        advanceUntilIdle()

        viewModel.setType(WantedType.Item)
        viewModel.setCategory("camera")
        viewModel.setSort(RequestSort.HighestBudget)

        // The last save wins — verify it captured the full final state.
        assertEquals(
            FilterState(
                type = WantedType.Item,
                category = "camera",
                sort = RequestSort.HighestBudget,
            ),
            store.last,
        )
    }

    @Test
    fun `SavedStateHandle restores the last filter on rotation`() = runTest(dispatcher) {
        val savedState = SavedStateHandle(
            mapOf(
                "filter_type" to "item",
                "filter_category" to "camera",
                "filter_sort" to "highest_budget",
            ),
        )
        val viewModel = newViewModel(seed, savedStateHandle = savedState)
        advanceUntilIdle()

        assertEquals(
            FilterState(
                type = WantedType.Item,
                category = "camera",
                sort = RequestSort.HighestBudget,
            ),
            viewModel.filter.value,
        )
    }

    @Test
    fun `setHostMode flips the role flag observed by the empty-state UI`() = runTest(dispatcher) {
        // Default is guest mode; toggling pushes the new mode through the
        // StateFlow consumed by RequestsScreen to pick its empty-state copy.
        val viewModel = newViewModel(seed)
        advanceUntilIdle()
        assertFalse(viewModel.isHostMode.value)

        viewModel.setHostMode(true)

        assertTrue(viewModel.isHostMode.value)

        viewModel.setHostMode(false)
        assertFalse(viewModel.isHostMode.value)
    }

    @Test
    fun `SavedStateHandle restores host mode on rotation`() = runTest(dispatcher) {
        // Host mode is held on the ViewModel so the role-aware empty state
        // survives a configuration change without bouncing back to guest.
        val savedState = SavedStateHandle(mapOf("is_host_mode" to true))

        val viewModel = newViewModel(seed, savedStateHandle = savedState)
        advanceUntilIdle()

        assertTrue(viewModel.isHostMode.value)
    }

    @Test
    fun `cold start with no SavedStateHandle falls back to the on-disk store`() = runTest(dispatcher) {
        val store = FakeFiltersStore().apply {
            seedInitial(
                FilterState(
                    type = WantedType.Service,
                    category = "delivery",
                    sort = RequestSort.Newest,
                ),
            )
        }
        val viewModel = newViewModel(seed, store = store)
        advanceUntilIdle()

        assertEquals(WantedType.Service, viewModel.filter.value.type)
        assertEquals("delivery", viewModel.filter.value.category)
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun newViewModel(
        items: List<WantedRequest>,
        store: RequestsFiltersStore = FakeFiltersStore(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): RequestsViewModel = RequestsViewModel(
        repository = FakeRepository(items),
        filtersStore = store,
        savedStateHandle = savedStateHandle,
    )

    private val seed: List<WantedRequest> = listOf(
        request("cam-pricey", "item", "camera", budget = 500.0),
        request("parking", "space", "parking", budget = 200.0),
        request("cam-cheap", "item", "camera", budget = 80.0),
        request("service-delivery", "service", "delivery", budget = 40.0),
        request("parking-cheap", "space", "parking", budget = null),
        request("tools-no-budget", "item", "tools", budget = null),
    )

    private fun request(
        id: String,
        type: String,
        category: String,
        budget: Double?,
    ): WantedRequest = WantedRequest(
        id = id,
        title = "$id title",
        description = "$id description",
        type = type,
        category = category,
        location = "Test",
        budget = budget,
        dateTime = null,
        imageUrl = null,
    )

    private class FakeRepository(
        private val items: List<WantedRequest>,
    ) : WantedRepository {
        override suspend fun getRequests(): Result<List<WantedRequest>> = Result.success(items)

        override suspend fun getRequest(id: String): Result<WantedRequest> =
            error("unused in filter tests")

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> =
            error("unused in filter tests")

        override suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer> =
            error("unused in filter tests")

        override suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>> =
            error("unused in filter tests")
    }

    private class FakeFiltersStore : RequestsFiltersStore {
        private var initial: FilterState = FilterState()
        var last: FilterState? = null
            private set

        fun seedInitial(state: FilterState) {
            initial = state
        }

        override fun load(): FilterState = initial
        override fun save(state: FilterState) {
            last = state
        }
    }
}
