package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import com.shourov.apps.pacedream.core.upload.ImageUploader
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.model.HostListingSummary
import com.shourov.apps.pacedream.feature.wanted.model.ModerationStatus
import com.shourov.apps.pacedream.feature.wanted.model.SelectedPlace
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Web-parity moderation contract for [CreateRequestViewModel]:
 *
 *  - Structured location is required: an empty/null pick blocks submit
 *    and surfaces an inline error.
 *  - The "Use current location" fallback (lat/lng without geocoded
 *    labels) counts as a verified pick — the moderator can still resolve
 *    the place from the coordinates.
 *  - Submit always lands the requester on the "pending review" success
 *    path even if a legacy backend echoes back `approved` (the platform
 *    contract is binding regardless of the server response).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateRequestViewModelModerationTest {

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
    fun `submit without a location surfaces an inline error and does not POST`() = runTest(dispatcher) {
        val repository = RecordingRepository()
        val viewModel = newViewModel(repository)
        viewModel.update {
            it.copy(
                title = "Need a quiet desk near downtown",
                description = "Looking for a quiet desk for two weeks of focused work.",
                category = "workspace",
                location = null,
            )
        }

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(
            "required-fields gate must fail without a verified location",
            state.requiredFieldsPresent,
        )
        assertNotNull(
            "the inline location error must surface after a submit attempt",
            state.fieldErrors.locationError,
        )
        assertNull(
            "createdId must remain null when submit short-circuits",
            state.createdId,
        )
        assertNull(
            "no network call must reach the repository",
            repository.captured,
        )
    }

    @Test
    fun `picking a location clears the location error and unblocks submit`() = runTest(dispatcher) {
        val repository = RecordingRepository()
        val viewModel = newViewModel(repository)
        viewModel.update {
            it.copy(
                title = "Need a quiet desk near downtown",
                description = "Looking for a quiet desk for two weeks of focused work.",
                category = "workspace",
            )
        }
        viewModel.submit()
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.fieldErrors.locationError)

        viewModel.update { it.copy(location = SAN_FRANCISCO) }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(
            "picking a place must clear the inline location error",
            state.fieldErrors.locationError,
        )
        assertTrue(
            "the required-fields gate flips green once a location is picked",
            state.requiredFieldsPresent,
        )
    }

    @Test
    fun `current-location pick with coordinates only still counts as verified`() = runTest(dispatcher) {
        // The "Use current location" path may skip geocoding entirely and
        // return a SelectedPlace populated only with lat/lng. The
        // moderator can still resolve that to a place; the form must
        // accept it so the user isn't forced into the autocomplete sheet.
        val repository = RecordingRepository()
        val viewModel = newViewModel(repository)
        viewModel.update {
            it.copy(
                title = "Need a quiet desk near downtown",
                description = "Looking for a quiet desk for two weeks of focused work.",
                category = "workspace",
                location = SelectedPlace(
                    city = "",
                    region = "",
                    country = "",
                    lat = 37.7749,
                    lng = -122.4194,
                ),
            )
        }

        viewModel.submit()
        advanceUntilIdle()

        val captured = repository.captured
            ?: error("the coordinates-only pick must reach the repository")
        assertEquals(37.7749, captured.location?.lat)
        assertEquals(-122.4194, captured.location?.lng)
        assertNull(captured.location?.city)
    }

    @Test
    fun `successful submit surfaces pending-review status regardless of server echo`() = runTest(dispatcher) {
        // Some legacy backends still default the moderation column to
        // "approved" before the migration ships. The platform contract is
        // that *every* new request enters review, so the success screen
        // must always show review copy — we treat an echoed `approved` as
        // unmigrated rather than as a verdict.
        val repository = RecordingRepository(
            responseModerationStatus = ModerationStatus.Approved,
        )
        val viewModel = newViewModel(repository)
        viewModel.update {
            it.copy(
                title = "Need a quiet desk near downtown",
                description = "Looking for a quiet desk for two weeks of focused work.",
                category = "workspace",
                location = SAN_FRANCISCO,
            )
        }

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("req_1", state.createdId)
        assertEquals(
            "an `approved` echo from a legacy backend must still resolve to PendingReview",
            ModerationStatus.PendingReview,
            state.createdModerationStatus,
        )
    }

    @Test
    fun `server-rejected response is honoured verbatim`() = runTest(dispatcher) {
        // If the backend explicitly says "rejected" we trust it — the
        // success screen will surface the rejection copy so the user
        // knows their submission didn't pass review.
        val repository = RecordingRepository(
            responseModerationStatus = ModerationStatus.Rejected,
        )
        val viewModel = newViewModel(repository)
        viewModel.update {
            it.copy(
                title = "Need a quiet desk near downtown",
                description = "Looking for a quiet desk for two weeks of focused work.",
                category = "workspace",
                location = SAN_FRANCISCO,
            )
        }

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            ModerationStatus.Rejected,
            viewModel.state.value.createdModerationStatus,
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private val SAN_FRANCISCO = SelectedPlace(
        city = "San Francisco",
        region = "California",
        country = "United States",
        lat = 37.7749,
        lng = -122.4194,
    )

    private fun newViewModel(repository: WantedRepository): CreateRequestViewModel =
        CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )

    private class RecordingRepository(
        private val responseModerationStatus: ModerationStatus = ModerationStatus.PendingReview,
    ) : WantedRepository {
        var captured: CreateRequestBody? = null
            private set

        override suspend fun getRequests(): Result<List<WantedRequest>> =
            error("unused")
        override suspend fun getMyRequests(): Result<List<WantedRequest>> =
            error("unused")
        override suspend fun getRequest(id: String): Result<WantedRequest> =
            error("unused")
        override suspend fun getOffersForRequest(requestId: String): Result<List<WantedOffer>> =
            error("unused")
        override suspend fun getMyOffers(): Result<List<WantedOffer>> =
            error("unused")
        override suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer> =
            error("unused")
        override suspend fun getHostListings(): Result<List<HostListingSummary>> =
            Result.success(emptyList())
        override suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>> =
            Result.failure(IllegalStateException("not exercised by moderation tests"))

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> {
            captured = body
            return Result.success(
                WantedRequest(
                    id = "req_1",
                    title = body.title,
                    description = body.description,
                    type = body.type,
                    category = body.category,
                    location = body.location?.city.orEmpty(),
                    budget = body.budget,
                    dateTime = body.date,
                    endDate = body.endDate,
                    imageUrl = body.coverImageUrl,
                    moderationStatus = responseModerationStatus,
                ),
            )
        }
    }

    private object NoopImageUploader : ImageUploader {
        override suspend fun uploadImage(uri: Uri): Result<String> =
            CompletableDeferred<Result<String>>().await()
    }
}
