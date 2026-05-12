package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import com.shourov.apps.pacedream.core.upload.ImageUploader
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoriesByType
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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Locks the server-driven category taxonomy contract on
 * [CreateRequestViewModel]:
 *
 *  - First-paint state shows the hardcoded [WantedCategoriesByType] so
 *    the dropdown is never blank on cold start or while offline.
 *  - A successful `getCategories()` overwrites the in-memory map.
 *  - A failed `getCategories()` leaves the fallback in place — the
 *    user-facing error banner must stay empty (this is a soft load).
 *  - If the user already picked a category that no longer exists for
 *    their type, the VM snaps to the first server option so we never
 *    POST an invalid pair.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateRequestViewModelCategoriesTest {

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
    fun `initial state surfaces hardcoded fallback before the server responds`() = runTest(dispatcher) {
        val repository = FakeRepository(
            categoriesResult = CompletableDeferred(),  // never completes
        )
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )

        val initial = viewModel.state.value
        assertSame(
            "first paint must use the hardcoded fallback",
            WantedCategoriesByType,
            initial.categoriesByType,
        )
    }

    @Test
    fun `successful load replaces the fallback with the server taxonomy`() = runTest(dispatcher) {
        val serverTaxonomy = mapOf(
            WantedType.Space to listOf(
                WantedCategoryOption("parking", "Parking"),
                WantedCategoryOption("rooftop", "Rooftop"),  // new, server-only
            ),
            WantedType.Item to listOf(
                WantedCategoryOption("drone", "Drone"),
            ),
            WantedType.Service to listOf(
                WantedCategoryOption("dog_walking", "Dog walking"),
            ),
        )
        val repository = FakeRepository(
            categoriesResult = CompletableDeferred(Result.success(serverTaxonomy)),
        )
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )
        advanceUntilIdle()

        assertEquals(serverTaxonomy, viewModel.state.value.categoriesByType)
    }

    @Test
    fun `failed load leaves the fallback intact and shows no error`() = runTest(dispatcher) {
        val repository = FakeRepository(
            categoriesResult = CompletableDeferred(
                Result.failure(RuntimeException("Unable to resolve host requests.example.com"))
            ),
        )
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertSame(
            "taxonomy load failure must keep the fallback in place",
            WantedCategoriesByType,
            state.categoriesByType,
        )
        assertEquals(
            "soft background load must not flip the form error banner",
            null,
            state.error,
        )
    }

    @Test
    fun `loaded taxonomy missing the picked category snaps to the first server option`() = runTest(dispatcher) {
        // The user opens the form (default form.category = "parking"),
        // the server returns a Space list that no longer contains
        // "parking" — the VM must snap to the first remaining option so
        // submit() can't POST an invalid (type, category) pair.
        val serverTaxonomy = mapOf(
            WantedType.Space to listOf(
                WantedCategoryOption("garage", "Garage"),
                WantedCategoryOption("driveway", "Driveway"),
            ),
            WantedType.Item to WantedCategoriesByType.getValue(WantedType.Item),
            WantedType.Service to WantedCategoriesByType.getValue(WantedType.Service),
        )
        val repository = FakeRepository(
            categoriesResult = CompletableDeferred(Result.success(serverTaxonomy)),
        )
        val viewModel = CreateRequestViewModel(
            repository = repository,
            imageUploader = NoopImageUploader,
        )
        advanceUntilIdle()

        assertEquals("garage", viewModel.state.value.form.category)
    }

    // ── Test doubles ───────────────────────────────────────────────────

    private class FakeRepository(
        private val categoriesResult: CompletableDeferred<Result<Map<WantedType, List<WantedCategoryOption>>>>,
    ) : WantedRepository {
        override suspend fun getRequests(): Result<List<WantedRequest>> =
            error("unused")

        override suspend fun getRequest(id: String): Result<WantedRequest> =
            error("unused")

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> =
            error("unused")

        override suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer> =
            error("unused")

        override suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>> =
            categoriesResult.await()
    }

    private object NoopImageUploader : ImageUploader {
        override suspend fun uploadImage(uri: Uri): Result<String> =
            CompletableDeferred<Result<String>>().await()
    }
}
