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
     * @param context Android context for content resolver
     * @param imageUri URI of the image to upload
     * @return secure_url from Cloudinary, or error
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): ApiResult<String> {
        return try {
            // 1. Read and downscale the image
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return ApiResult.Failure(
                    com.shourov.apps.pacedream.core.network.api.ApiError.Unknown("Cannot read image")
                )

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return ApiResult.Failure(
                    com.shourov.apps.pacedream.core.network.api.ApiError.Unknown("Cannot decode image")
                )
            }

            val scaledBitmap = downscaleBitmap(originalBitmap, MAX_DIMENSION)

            // 2. Convert to JPEG bytes
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val jpegBytes = outputStream.toByteArray()

            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            // 3. Encode as base64 data URL (matching iOS format)
            val base64 = android.util.Base64.encodeToString(jpegBytes, android.util.Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64"

            // 4. Upload to backend /api/upload endpoint
            val uploadUrl = appConfig.buildFrontendUrl("api", "upload")

            Timber.d("ImageUpload: uploading ${jpegBytes.size} bytes to $uploadUrl")

            val result = apiClient.postMultipart(
                url = uploadUrl,
                fieldName = "file",
                fieldValue = dataUrl,
                includeAuth = true
            )

            when (result) {
                is ApiResult.Success -> {
                    // Parse secure_url from response
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
        } catch (e: Exception) {
            Timber.e(e, "ImageUpload: failed")
            ApiResult.Failure(
                com.shourov.apps.pacedream.core.network.api.ApiError.Unknown(e.message ?: "Upload failed")
            )
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

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    companion object {
        private const val MAX_DIMENSION = 1280
        private const val JPEG_QUALITY = 80
    }
}
