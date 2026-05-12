package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import com.shourov.apps.pacedream.core.upload.ImageUploader
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Locks the wire contract for the date-range picker:
 *  - A single picked day serializes as ISO-8601 `date` only (no `endDate`).
 *  - A picked range serializes as both `date` (start) and `endDate`.
 *  - A blank selection submits neither field.
 *  - Submitting the same start and end day deduplicates the end.
 *
 * Round-tripping a request through `GET /v1/requests/{id}` must yield the
 * same ISO strings the user picked, so the dates are stored as UTC-midnight
 * epoch millis on the form to avoid timezone drift.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateRequestViewModelDateSerializationTest {

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
    fun `single picked date serializes as ISO start with no end`() = runTest(dispatcher) {
        val repository = RecordingRepository()
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )

        viewModel.update {
            it.copy(
                title = "Need a covered parking spot in SF",
                description = "Looking for a covered spot near downtown for the long weekend.",
                category = "parking",
                startDate = utcMidnight(2026, 7, 4),
            )
        }
        viewModel.submit()
        advanceUntilIdle()

        val body = repository.captured ?: error("submit must call createRequest")
        assertEquals("2026-07-04", body.date)
        assertNull("endDate must be omitted when only a start date is picked", body.endDate)
    }

    @Test
    fun `picked range serializes both start and endDate as ISO`() = runTest(dispatcher) {
        val repository = RecordingRepository()
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )

        viewModel.update {
            it.copy(
                title = "Need a covered parking spot in SF",
                description = "Looking for a covered spot for the holiday weekend.",
                category = "parking",
                startDate = utcMidnight(2026, 7, 4),
                endDate = utcMidnight(2026, 7, 5),
            )
        }
        viewModel.submit()
        advanceUntilIdle()

        val body = repository.captured ?: error("submit must call createRequest")
        assertEquals("2026-07-04", body.date)
        assertEquals("2026-07-05", body.endDate)
    }

    @Test
    fun `no date selection omits both fields`() = runTest(dispatcher) {
        val repository = RecordingRepository()
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )

        viewModel.update {
            it.copy(
                title = "Need a covered parking spot in SF",
                description = "Open to any date — let providers tell me what's available.",
                category = "parking",
                startDate = null,
                endDate = null,
            )
        }
        viewModel.submit()
        advanceUntilIdle()

        val body = repository.captured ?: error("submit must call createRequest")
        assertNull("date must be null when nothing is picked", body.date)
        assertNull("endDate must be null when nothing is picked", body.endDate)
    }

    @Test
    fun `same start and end day deduplicates the end`() = runTest(dispatcher) {
        val repository = RecordingRepository()
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )

        val day = utcMidnight(2026, 7, 4)
        viewModel.update {
            it.copy(
                title = "Need a covered parking spot in SF",
                description = "Just for the Saturday — single-day need.",
                category = "parking",
                startDate = day,
                endDate = day,
            )
        }
        viewModel.submit()
        advanceUntilIdle()

        val body = repository.captured ?: error("submit must call createRequest")
        assertEquals("2026-07-04", body.date)
        assertNull("a same-day range collapses to a single date", body.endDate)
    }

    // ── Test doubles ───────────────────────────────────────────────────────

    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    private class RecordingRepository : WantedRepository {
        var captured: CreateRequestBody? = null
            private set

        override suspend fun getRequests(): Result<List<WantedRequest>> =
            error("not exercised by serialization tests")

        override suspend fun getMyRequests(): Result<List<WantedRequest>> =
            error("not exercised by serialization tests")

        override suspend fun getRequest(id: String): Result<WantedRequest> =
            error("not exercised by serialization tests")

        override suspend fun getOffersForRequest(requestId: String): Result<List<WantedOffer>> =
            error("not exercised by serialization tests")

        override suspend fun getMyOffers(): Result<List<WantedOffer>> =
            error("not exercised by serialization tests")

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> {
            captured = body
            return Result.success(
                WantedRequest(
                    id = "req_1",
                    title = body.title,
                    description = body.description,
                    type = body.type,
                    category = body.category,
                    location = "",
                    budget = body.budget,
                    dateTime = body.date,
                    endDate = body.endDate,
                    imageUrl = body.coverImageUrl,
                )
            )
        }

        override suspend fun createOffer(
            requestId: String,
            body: CreateOfferBody,
        ): Result<WantedOffer> = error("not exercised by serialization tests")

        override suspend fun getHostListings():
            Result<List<com.shourov.apps.pacedream.feature.wanted.model.HostListingSummary>> =
            Result.success(emptyList())

        override suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>> =
            Result.failure(IllegalStateException("not exercised by serialization tests"))
    }

    private object NoopImageUploader : ImageUploader {
        override suspend fun uploadImage(uri: Uri): Result<String> =
            CompletableDeferred<Result<String>>().await()
    }
}
