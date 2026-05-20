package com.pacedream.app.feature.tripplanner

import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Locks the destructive-delete contract for [TripPlannerViewModel]:
 *
 *  - On a Confirm tap the ViewModel calls `repository.deleteTrip` **once**.
 *  - On success the deleted trip is removed from `uiState.trips` and
 *    `uiState.message` carries [TripPlannerCopy.DELETE_SUCCESS] so the
 *    screen's `LaunchedEffect(uiState.message)` raises a Snackbar.
 *  - On failure the trip stays in `uiState.trips`, no success message is
 *    set, and `uiState.error` carries [TripPlannerCopy.DELETE_FAILURE] so
 *    the screen's existing error-snackbar `LaunchedEffect(uiState.error)`
 *    raises a visible error.
 *
 * Together with [DeleteTripConfirmDialogTest] this covers the audit's
 * "tap delete → dialog appears, no API call yet; Confirm → API once;
 * Failure → error shown, item still present" expectations end-to-end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripPlannerViewModelDeleteTest {

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
    fun `deleteTrip success — calls repository once, removes trip, surfaces success message`() =
        runTest(dispatcher) {
            val initial = listOf(
                TripPlan(id = "trip-1", name = "Lisbon"),
                TripPlan(id = "trip-2", name = "Madrid"),
            )
            val repo = ScriptedRepository(seedTrips = initial)
            val viewModel = TripPlannerViewModel(repo)
            advanceUntilIdle()
            // Seeded trips are loaded from init {}.
            assertEquals(initial, viewModel.uiState.value.trips)

            viewModel.deleteTrip("trip-1")
            advanceUntilIdle()

            assertEquals(
                "deleteTrip must hit the repository exactly once per Confirm tap",
                1,
                repo.deleteCalls,
            )
            assertEquals(
                "Successful delete must filter the trip out of uiState",
                listOf(TripPlan(id = "trip-2", name = "Madrid")),
                viewModel.uiState.value.trips,
            )
            assertEquals(
                "Success path must surface the documented snackbar copy",
                TripPlannerCopy.DELETE_SUCCESS,
                viewModel.uiState.value.message,
            )
            assertNull(
                "Success path must not leak into the error channel",
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `deleteTrip failure — surfaces error copy, leaves the trip in the list`() =
        runTest(dispatcher) {
            val initial = listOf(
                TripPlan(id = "trip-1", name = "Lisbon"),
                TripPlan(id = "trip-2", name = "Madrid"),
            )
            val repo = ScriptedRepository(seedTrips = initial).apply {
                nextDeleteResult = ApiResult.Failure(ApiError.NetworkTimeout)
            }
            val viewModel = TripPlannerViewModel(repo)
            advanceUntilIdle()

            viewModel.deleteTrip("trip-1")
            advanceUntilIdle()

            assertEquals(
                "Failure must still trigger exactly one repository call — not zero, not retry-loop",
                1,
                repo.deleteCalls,
            )
            assertEquals(
                "Failure must leave the trip in place — no optimistic mutation",
                initial,
                viewModel.uiState.value.trips,
            )
            assertEquals(
                "Failure must surface the documented snackbar copy",
                TripPlannerCopy.DELETE_FAILURE,
                viewModel.uiState.value.error,
            )
            assertNull(
                "Failure path must not also pretend to succeed via message",
                viewModel.uiState.value.message,
            )
        }

    @Test
    fun `consumeMessage and consumeError clear transient snackbar fields`() =
        runTest(dispatcher) {
            // Exercise the consume hooks the screen calls after showing a
            // Snackbar, to guarantee a delete failure followed by a successful
            // retry cleanly transitions message/error.
            val repo = ScriptedRepository(seedTrips = listOf(TripPlan(id = "t-1", name = "A")))
            val viewModel = TripPlannerViewModel(repo)
            advanceUntilIdle()

            repo.nextDeleteResult = ApiResult.Failure(ApiError.NetworkTimeout)
            viewModel.deleteTrip("t-1")
            advanceUntilIdle()
            assertSame(
                TripPlannerCopy.DELETE_FAILURE,
                viewModel.uiState.value.error,
            )

            viewModel.consumeError()
            assertNull(viewModel.uiState.value.error)

            repo.nextDeleteResult = null // Success
            viewModel.deleteTrip("t-1")
            advanceUntilIdle()
            assertEquals(
                TripPlannerCopy.DELETE_SUCCESS,
                viewModel.uiState.value.message,
            )

            viewModel.consumeMessage()
            assertNull(viewModel.uiState.value.message)
            assertTrue(
                "Trip list must reflect the eventual successful delete",
                viewModel.uiState.value.trips.isEmpty(),
            )
        }

    /**
     * In-memory fake [TripPlannerRepository] that records call counts and
     * supports per-call result scripting. Returns success by default; set
     * [nextDeleteResult] to override the next [deleteTrip] response.
     */
    private class ScriptedRepository(
        private val seedTrips: List<TripPlan> = emptyList(),
    ) : TripPlannerRepository {
        var deleteCalls: Int = 0
            private set
        var nextDeleteResult: ApiResult<String>? = null

        override suspend fun getTrips(): ApiResult<TripsEnvelope> =
            ApiResult.Success(TripsEnvelope(data = seedTrips))

        override suspend fun createTrip(request: CreateTripRequest): ApiResult<String> =
            ApiResult.Success("created")

        override suspend fun deleteTrip(tripId: String): ApiResult<String> {
            deleteCalls += 1
            return nextDeleteResult ?: ApiResult.Success("deleted")
        }

        override suspend fun getTours(city: String): ApiResult<ToursEnvelope> =
            ApiResult.Success(ToursEnvelope(data = emptyList()))
    }
}
