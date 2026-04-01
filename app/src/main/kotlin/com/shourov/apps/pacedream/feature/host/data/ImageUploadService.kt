package com.shourov.apps.pacedream.feature.host.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Image upload service matching iOS CloudinaryUploader.swift.
 *
 * Routes uploads through the backend `/api/upload` endpoint instead of
 * direct Cloudinary uploads. The backend handles signing and Cloudinary
 * API key management.
 *
 * Flow:
 * 1. Client picks image from gallery/camera
 * 2. Image is downscaled (max 1280px) and converted to JPEG
 * 3. Sends base64 data URL to backend via multipart/form-data
 * 4. Backend uploads to Cloudinary and returns secure_url
 */
@Singleton
class ImageUploadService @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {

    /**
     * Upload an image from a content URI.
     * iOS parity: Two-path strategy.
     *   Path A (preferred): Upload to /api/upload, get Cloudinary URL back.
     *   Path B (fallback): Return base64 data URL for inline in listing payload.
     *
     * Also matches iOS retry-on-413: if upload fails with 413 (too large),
     * retry with smaller dimensions (1024px, lower quality).
     *
     * @param context Android context for content resolver
     * @param imageUri URI of the image to upload
     * @return secure_url from Cloudinary, or error
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): ApiResult<String> {
        return try {
            // 1. Read the image
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return ApiResult.Failure(
                    com.shourov.apps.pacedream.core.network.api.ApiError.Unknown("Cannot read image")
                )

            val originalBitmap = try {
                BitmapFactory.decodeStream(inputStream)
            } catch (e: OutOfMemoryError) {
                Timber.e("ImageUpload: OutOfMemoryError decoding image")
                return ApiResult.Failure(
                    com.shourov.apps.pacedream.core.network.api.ApiError.Unknown("Image too large to process")
                )
            } finally {
                try { inputStream.close() } catch (_: Exception) {}
            }

            if (originalBitmap == null) {
                return ApiResult.Failure(
                    com.shourov.apps.pacedream.core.network.api.ApiError.Unknown("Cannot decode image")
                )
            }

            // 2. Try upload at full quality first (iOS: max 1280px, 450KB target)
            val result = tryUpload(originalBitmap, MAX_DIMENSION, JPEG_QUALITY)

            when (result) {
                is ApiResult.Success -> {
                    originalBitmap.recycle()
                    result
                }
                is ApiResult.Failure -> {
                    // iOS parity: retry with smaller dimensions on 413 or large payload errors
                    Timber.d("ImageUpload: first attempt failed, retrying with smaller size")
                    val retryResult = tryUpload(originalBitmap, MAX_DIMENSION_RETRY, JPEG_QUALITY_RETRY)
                    originalBitmap.recycle()
                    retryResult
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "ImageUpload: failed")
            ApiResult.Failure(
                com.shourov.apps.pacedream.core.network.api.ApiError.Unknown(e.message ?: "Upload failed")
            )
        }
    }

    private suspend fun tryUpload(originalBitmap: Bitmap, maxDim: Int, quality: Int): ApiResult<String> {
        val scaledBitmap = downscaleBitmap(originalBitmap, maxDim)

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val jpegBytes = outputStream.toByteArray()

        if (scaledBitmap != originalBitmap) {
            scaledBitmap.recycle()
        }

        // iOS parity: if bytes too large, try progressively lower quality
        // iOS tries: [0.62, 0.54, 0.46, 0.38, 0.30, 0.24, 0.18]
        val maxBytes = if (maxDim == MAX_DIMENSION) MAX_BYTES_CLOUDINARY else MAX_BYTES_FALLBACK
        var finalBytes = jpegBytes
        if (finalBytes.size > maxBytes) {
            val qualities = listOf(62, 54, 46, 38, 30, 24, 18)
            for (q in qualities) {
                try {
                    val recompressed = ByteArrayOutputStream()
                    val bmp = downscaleBitmap(originalBitmap, maxDim)
                    if (bmp == null) {
                        Timber.w("ImageUpload: downscaleBitmap returned null at quality=$q, using last good bytes")
                        break
                    }
                    bmp.compress(Bitmap.CompressFormat.JPEG, q, recompressed)
                    if (bmp != originalBitmap) bmp.recycle()
                    finalBytes = recompressed.toByteArray()
                    if (finalBytes.size <= maxBytes) break
                } catch (e: OutOfMemoryError) {
                    Timber.e("ImageUpload: OutOfMemoryError during quality retry at q=$q, using last good bytes")
                    break
                }
            }
        }

        val base64 = android.util.Base64.encodeToString(finalBytes, android.util.Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64"

        val uploadUrl = appConfig.buildFrontendUrl("api", "upload")

        Timber.d("ImageUpload: uploading ${finalBytes.size} bytes (maxDim=$maxDim) to $uploadUrl")

        val result = apiClient.postMultipart(
            url = uploadUrl,
            fieldName = "file",
            fieldValue = dataUrl,
            includeAuth = true
        )

        return when (result) {
            is ApiResult.Success -> {
                val secureUrl = parseSecureUrl(result.data)
                if (secureUrl != null) {
                    Timber.d("ImageUpload: success -> $secureUrl")
                    ApiResult.Success(secureUrl)
                } else {
                    ApiResult.Failure(
                        com.shourov.apps.pacedream.core.network.api.ApiError.DecodingError(
                            "Missing secure_url in response", null
                        )
                    )
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Upload raw JPEG bytes (for camera capture).
     */
    suspend fun uploadBytes(jpegBytes: ByteArray): ApiResult<String> {
        val base64 = android.util.Base64.encodeToString(jpegBytes, android.util.Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64"

        val uploadUrl = appConfig.buildFrontendUrl("api", "upload")

        return when (val result = apiClient.postMultipart(
            url = uploadUrl,
            fieldName = "file",
            fieldValue = dataUrl,
            includeAuth = true
        )) {
            is ApiResult.Success -> {
                val secureUrl = parseSecureUrl(result.data)
                if (secureUrl != null) {
                    ApiResult.Success(secureUrl)
                } else {
                    ApiResult.Failure(
                        com.shourov.apps.pacedream.core.network.api.ApiError.DecodingError(
                            "Missing secure_url in response", null
                        )
                    )
                }
            }
            is ApiResult.Failure -> result
        }
    }

    private fun parseSecureUrl(responseBody: String): String? {
        return try {
            val root = json.parseToJsonElement(responseBody)
            val obj = root.jsonObject
            // Try direct field, then nested under "data"
            obj["secure_url"]?.jsonPrimitive?.content
                ?: obj["url"]?.jsonPrimitive?.content
                ?: obj["data"]?.jsonObject?.get("secure_url")?.jsonPrimitive?.content
                ?: obj["data"]?.jsonObject?.get("url")?.jsonPrimitive?.content
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse upload response")
            null
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)

        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: OutOfMemoryError) {
            Timber.e("ImageUpload: OutOfMemoryError scaling bitmap, returning original")
            bitmap
        }
    }

    companion object {
        // Web parity: website compresses to 1024px max, 0.7 quality JPEG
        private const val MAX_DIMENSION = 1024          // Match website canvas.toDataURL dimensions
        private const val MAX_DIMENSION_RETRY = 800     // Retry on 413
        private const val JPEG_QUALITY = 70             // Match website 0.7 quality
        private const val JPEG_QUALITY_RETRY = 50       // Retry quality
        private const val MAX_BYTES_CLOUDINARY = 460_800 // ~450KB target
        private const val MAX_BYTES_FALLBACK = 204_800   // ~200KB fallback
    }
}
