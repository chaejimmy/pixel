package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import com.shourov.apps.pacedream.core.upload.ImageUploader
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Locks the cover-image upload contract: the picker hands us a local
 * `content://` URI which must be exchanged for a remote HTTPS URL via
 * [ImageUploader] before `coverImageUrl` is set on the form. While the
 * exchange is in flight the ViewModel must surface an `uploading` flag,
 * and on failure must show the InlineErrorBanner copy verbatim — never
 * the raw exception message and never the local URI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateRequestViewModelUploadTest {

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
    fun `uploadCoverImage flips uploading flag and stores returned public URL on success`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val uploader = FakeImageUploader { uri ->
            assertEquals("content://provider/1", uri.toString())
            gate.await()
            Result.success("https://cdn.example.com/cover/abc.jpg")
        }
        val viewModel = CreateRequestViewModel(
            repository = ThrowingRepository,
            imageUploader = uploader,
        )

        viewModel.uploadCoverImage(Uri.parse("content://provider/1"))

        // Let the launched coroutine run far enough to flip `uploading`
        // but not finish (the gate is still closed).
        advanceUntilIdle()
        val whileUploading = viewModel.state.value
        assertTrue("uploading must be true while the request is in flight", whileUploading.uploading)
        assertNull("imageUrl must not yet be set", whileUploading.form.imageUrl)

        gate.complete(Unit)
        advanceUntilIdle()

        val after = viewModel.state.value
        assertFalse("uploading must be cleared on success", after.uploading)
        assertEquals(
            "form.imageUrl must be the public URL the uploader returned",
            "https://cdn.example.com/cover/abc.jpg",
            after.form.imageUrl,
        )
        assertNull("no error on success", after.error)
    }

    @Test
    fun `uploadCoverImage surfaces friendly error and leaves imageUrl null on failure`() = runTest(dispatcher) {
        val uploader = FakeImageUploader {
            Result.failure(RuntimeException("S3 upload failed: 403 — secret-token-exposed"))
        }
        val viewModel = CreateRequestViewModel(
            repository = ThrowingRepository,
            imageUploader = uploader,
        )

        viewModel.uploadCoverImage(Uri.parse("content://provider/2"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("uploading must be cleared on failure", state.uploading)
        assertNull("imageUrl must remain null when the upload fails", state.form.imageUrl)
        val error = state.error ?: error("expected an error banner")
        assertEquals(
            "Couldn't upload image — try again or post without one.",
            error,
        )
        // Raw exception details must never bubble through to the UI.
        assertFalse(
            "raw exception text must not leak into the banner",
            error.contains("S3") || error.contains("403"),
        )
    }

    @Test
    fun `submit is a no-op while an upload is in flight`() = runTest(dispatcher) {
        val viewModel = CreateRequestViewModel(
            repository = ExplodingRepository,
            imageUploader = NeverCompletesImageUploader,
        )
        // Fill the form so validate() would otherwise pass and a submit
        // would reach the repository (which would explode the test).
        viewModel.update {
            it.copy(
                title = "A valid title for the request",
                description = "A description long enough to clear the 10-char floor.",
                category = "parking",
            )
        }

        viewModel.uploadCoverImage(Uri.parse("content://provider/3"))
        advanceUntilIdle()
        assertTrue("upload should have flipped the uploading flag", viewModel.state.value.uploading)

        // submit() must short-circuit; the ExplodingRepository would fail
        // the test if it were called.
        viewModel.submit()
        advanceUntilIdle()

        assertFalse("submit must not start while uploading", viewModel.state.value.submitting)
    }

    // ── Test doubles ───────────────────────────────────────────────────────

    private class FakeImageUploader(
        private val block: suspend (Uri) -> Result<String>,
    ) : ImageUploader {
        override suspend fun uploadImage(uri: Uri): Result<String> = block(uri)
    }

    private object NeverCompletesImageUploader : ImageUploader {
        override suspend fun uploadImage(uri: Uri): Result<String> =
            CompletableDeferred<Result<String>>().await()
    }

    private object ThrowingRepository : WantedRepository {
        override suspend fun getRequests(): Result<List<WantedRequest>> =
            error("repository must not be hit from upload tests")

        override suspend fun getRequest(id: String): Result<WantedRequest> =
            error("repository must not be hit from upload tests")

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> =
            error("repository must not be hit from upload tests")

        override suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer> =
            error("repository must not be hit from upload tests")
    }

    private object ExplodingRepository : WantedRepository {
        override suspend fun getRequests(): Result<List<WantedRequest>> =
            error("submit must not reach the repository while uploading")

        override suspend fun getRequest(id: String): Result<WantedRequest> =
            error("submit must not reach the repository while uploading")

        override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> =
            error("submit must not reach the repository while uploading")

        override suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer> =
            error("submit must not reach the repository while uploading")
    }

}
