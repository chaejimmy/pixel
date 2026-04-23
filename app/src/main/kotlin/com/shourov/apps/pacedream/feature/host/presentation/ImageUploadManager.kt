package com.shourov.apps.pacedream.feature.host.presentation

import android.content.Context
import android.net.Uri
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.feature.host.data.ImageUploadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.UUID

/**
 * Status of a single photo attached to the Create Listing draft.
 * Pending → Uploading → (Succeeded | Failed). Failed can be retried.
 */
enum class UploadStatus { Pending, Uploading, Succeeded, Failed }

/**
 * Single source of truth for one photo in the wizard.
 *
 * [id] is stable across status transitions so the UI can key on it
 * without flicker.  A photo that originated from a resumed draft has
 * no [localUri] (it is already a remote URL); a photo the user just
 * picked has a [localUri] until the uploader succeeds.
 */
data class UploadedImage(
    val id: String = UUID.randomUUID().toString(),
    val localUri: Uri? = null,
    val remoteUrl: String? = null,
    val status: UploadStatus = UploadStatus.Pending,
    val progress: Float = 0f,
    val errorMessage: String? = null,
) {
    val displayModel: Any? get() = localUri ?: remoteUrl
}

/**
 * Eagerly uploads photos the user picks, bounded to [concurrency]
 * in-flight requests so phones on weak networks are not thrashed.
 * Failures are surfaced as [UploadStatus.Failed] so the UI can offer a
 * retry instead of blocking the whole flow.
 *
 * The manager is owned by the ViewModel — it lives as long as the
 * wizard and is torn down via [cancelAll] in `onCleared`.
 */
class ImageUploadManager(
    private val service: ImageUploadService,
    private val scope: CoroutineScope,
    private val concurrency: Int = 3,
) {
    private val semaphore = Semaphore(concurrency)
    private val jobs = mutableMapOf<String, Job>()

    /**
     * Enqueue [images] for upload.  Each status change (start, progress
     * tick, success/failure) is surfaced via [onUpdate] so the caller
     * can patch its own state-flow.  Safe to call multiple times; images
     * that are already complete are skipped.
     */
    fun enqueue(
        context: Context,
        images: List<UploadedImage>,
        onUpdate: (UploadedImage) -> Unit,
    ) {
        images.forEach { image ->
            if (image.status == UploadStatus.Succeeded) return@forEach
            val uri = image.localUri ?: return@forEach
            if (jobs[image.id]?.isActive == true) return@forEach

            val job = scope.launch(Dispatchers.IO) {
                semaphore.withPermit {
                    onUpdate(image.copy(status = UploadStatus.Uploading, progress = 0.05f))
                    val tick = launch {
                        // Coarse indeterminate progress — the upload call
                        // is a single await, so we fake steady progress
                        // until it resolves.
                        var p = 0.1f
                        while (isActive && p < 0.9f) {
                            delay(250)
                            p += 0.1f
                            onUpdate(
                                image.copy(
                                    status = UploadStatus.Uploading,
                                    progress = p.coerceAtMost(0.9f),
                                )
                            )
                        }
                    }
                    try {
                        when (val result = service.uploadImage(context, uri)) {
                            is ApiResult.Success -> onUpdate(
                                image.copy(
                                    status = UploadStatus.Succeeded,
                                    remoteUrl = result.data,
                                    progress = 1f,
                                    errorMessage = null,
                                )
                            )
                            is ApiResult.Failure -> onUpdate(
                                image.copy(
                                    status = UploadStatus.Failed,
                                    errorMessage = result.error.message ?: "Upload failed",
                                )
                            )
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        Timber.w(e, "Image upload failed for id=%s", image.id)
                        onUpdate(
                            image.copy(
                                status = UploadStatus.Failed,
                                errorMessage = e.message ?: "Upload failed",
                            )
                        )
                    } finally {
                        tick.cancel()
                    }
                }
            }
            jobs[image.id] = job
            job.invokeOnCompletion { jobs.remove(image.id) }
        }
    }

    fun cancel(id: String) {
        jobs[id]?.cancel()
        jobs.remove(id)
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
