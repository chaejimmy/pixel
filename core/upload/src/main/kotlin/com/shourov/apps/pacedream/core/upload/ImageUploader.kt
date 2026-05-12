package com.shourov.apps.pacedream.core.upload

import android.net.Uri

/**
 * Uploads a local image (referenced by a content:// [Uri]) to object
 * storage and returns the publicly reachable HTTPS URL the server should
 * persist on the domain object.
 *
 * Mirrors the presigned-URL pattern used by the verification flow
 * (`/v1/users/verification/upload-urls`): ask the API for a presigned PUT
 * URL, PUT the bytes directly to storage, then use the cleaned URL (no
 * signature query params) as the public asset URL.
 *
 * Implementations are expected to be safe to call from any dispatcher —
 * they switch to IO internally.
 */
interface ImageUploader {
    suspend fun uploadImage(uri: Uri): Result<String>
}
