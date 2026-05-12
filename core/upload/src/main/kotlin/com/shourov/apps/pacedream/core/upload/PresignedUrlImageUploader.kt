package com.shourov.apps.pacedream.core.upload

import android.content.Context
import android.net.Uri
import com.shourov.apps.pacedream.core.common.network.Dispatcher
import com.shourov.apps.pacedream.core.common.network.PaceDreamDispatchers
import com.shourov.apps.pacedream.core.network.model.verification.UploadFileRequest
import com.shourov.apps.pacedream.core.network.repository.VerificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [ImageUploader] backed by the same presigned-URL endpoint the
 * verification flow uses. The endpoint returns an S3 PUT URL plus a
 * storage key; we PUT the bytes and then strip the signing query string
 * to get the public object URL that other clients can render.
 */
@Singleton
class PresignedUrlImageUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verificationRepository: VerificationRepository,
    @Dispatcher(PaceDreamDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ImageUploader {

    override suspend fun uploadImage(uri: Uri): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val resolver = context.contentResolver
            val contentType = resolver.getType(uri) ?: DEFAULT_CONTENT_TYPE
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Couldn't read the selected image.")
            require(bytes.isNotEmpty()) { "Selected image is empty." }

            val presignRequest = UploadFileRequest(
                side = COVER_SIDE,
                contentType = contentType,
            )
            val presignResponse = verificationRepository.getUploadURLs(listOf(presignRequest))
                .getOrThrow()
            val uploadInfo = presignResponse.data?.uploads?.firstOrNull()
                ?: error("Server did not return an upload URL.")

            verificationRepository.uploadToS3(
                uploadUrl = uploadInfo.uploadUrl,
                imageData = bytes,
                contentType = contentType,
            ).getOrThrow()

            // The presigned URL is the object URL with signing query
            // parameters appended. Stripping the query gives the public
            // URL that can be rendered by other clients (assumes the
            // bucket policy grants public read on uploaded objects, which
            // matches the verification flow's behavior).
            val publicUrl = uploadInfo.uploadUrl.substringBefore('?')
            Timber.d("ImageUploader: uploaded ${bytes.size} bytes → $publicUrl")
            publicUrl
        }
    }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "image/jpeg"
        // The verification endpoint requires a `side` field. "cover" is
        // used for non-document image uploads (e.g. request cover images);
        // it is opaque to S3 and only affects the storage key prefix.
        const val COVER_SIDE = "cover"
    }
}
