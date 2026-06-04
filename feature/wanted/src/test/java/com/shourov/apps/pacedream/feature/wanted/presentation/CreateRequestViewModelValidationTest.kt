package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import com.shourov.apps.pacedream.core.upload.ImageUploader
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.model.CreateRequestField
import com.shourov.apps.pacedream.feature.wanted.model.HostListingSummary
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Field-level validation contract for the Post-a-Request form:
 *
 *  - A submit attempt on an invalid form surfaces per-field errors, points
 *    the screen at the first problem via [CreateRequestField], and never
 *    reaches the repository.
 *  - Fixing one field clears only that field's error — a sibling field's
 *    submit-time error must survive an unrelated keystroke.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateRequestViewModelValidationTest {

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
    fun `submitting a one-char title surfaces a title error and focuses it without POSTing`() =
        runTest(dispatcher) {
            val repository = RecordingRepository()
            val viewModel = newViewModel(repository)
            viewModel.update {
                it.copy(
                    title = "x",
                    description = "Looking for a quiet desk for two weeks of focused work.",
                    category = "workspace",
                    location = SAN_FRANCISCO,
                )
            }

            viewModel.submit()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNotNull(
                "a too-short title must surface a field error on submit",
                state.fieldErrors.titleError,
            )
            assertEquals(
                "the screen must be pointed at the title as the first invalid field",
                CreateRequestField.Title,
                state.focusTarget,
            )
            assertNull(
                "an invalid form must not reach the repository",
                repository.captured,
            )
        }

    @Test
    fun `fixing the title clears its error but leaves an unrelated field error intact`() =
        runTest(dispatcher) {
            val repository = RecordingRepository()
            val viewModel = newViewModel(repository)
            // Both title (too short) and description (too short) are invalid.
            viewModel.update {
                it.copy(
                    title = "x",
                    description = "short",
                    category = "workspace",
                    location = SAN_FRANCISCO,
                )
            }
            viewModel.submit()
            advanceUntilIdle()

            val afterSubmit = viewModel.state.value.fieldErrors
            assertNotNull(afterSubmit.titleError)
            assertNotNull(afterSubmit.descriptionError)

            // Typing into the title clears its error optimistically...
            viewModel.update { it.copy(title = "A valid request title") }
            advanceUntilIdle()

            val afterFix = viewModel.state.value.fieldErrors
            assertNull(
                "fixing the title clears its own error",
                afterFix.titleError,
            )
            assertNotNull(
                "...but the description's submit-time error must not be wiped",
                afterFix.descriptionError,
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

    private class RecordingRepository : WantedRepository {
        var captured: CreateRequestBody? = null
            private set

        override suspend fun getRequests(): Result<List<WantedRequest>> = error("unused")
        override suspend fun getMyRequests(): Result<List<WantedRequest>> = error("unused")
        override suspend fun getRequest(id: String): Result<WantedRequest> = error("unused")
        override suspend fun getOffersForRequest(requestId: String): Result<List<WantedOffer>> =
            error("unused")
        override suspend fun getMyOffers(): Result<List<WantedOffer>> = error("unused")
        override suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer> =
            error("unused")
        override suspend fun updateRequestStatus(
            id: String,
            status: com.shourov.apps.pacedream.feature.wanted.model.RequestStatus,
            expiresAt: String?,
        ): Result<WantedRequest> = error("unused")
        override suspend fun renewRequest(id: String, newExpiry: String?): Result<WantedRequest> =
            error("unused")
        override suspend fun getHostListings(): Result<List<HostListingSummary>> =
            Result.success(emptyList())
        override suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>> =
            Result.failure(IllegalStateException("not exercised by validation tests"))

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
                    imageUrl = body.coverImageUrl,
                ),
            )
        }
    }

    private object NoopImageUploader : ImageUploader {
        override suspend fun uploadImage(uri: Uri): Result<String> =
            CompletableDeferred<Result<String>>().await()
    }
}
