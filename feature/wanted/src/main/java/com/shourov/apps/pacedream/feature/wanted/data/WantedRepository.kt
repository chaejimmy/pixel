package com.shourov.apps.pacedream.feature.wanted.data

import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.toDomain
import com.shourov.apps.pacedream.feature.wanted.data.remote.WantedApiService
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface WantedRepository {
    suspend fun getRequests(): Result<List<WantedRequest>>
    suspend fun getRequest(id: String): Result<WantedRequest>
    suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest>
    suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer>

    /**
     * Per-type category taxonomy as advertised by the backend. Cached for
     * the duration of the process so the dropdown doesn't refetch on every
     * screen open. Callers should fall back to the hardcoded
     * [com.shourov.apps.pacedream.feature.wanted.model.WantedCategoriesByType]
     * when this returns a failure so the form is never blank offline.
     */
    suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>>
}

@Singleton
class WantedRepositoryImpl @Inject constructor(
    private val api: WantedApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WantedRepository {

    // Session-scoped cache. The taxonomy is small (~tens of strings) and
    // changes rarely, so a single in-memory map outlives every screen open
    // and avoids a network round-trip on each navigation into the form.
    private val categoriesMutex = Mutex()
    @Volatile
    private var cachedCategories: Map<WantedType, List<WantedCategoryOption>>? = null

    override suspend fun getRequests(): Result<List<WantedRequest>> = withContext(dispatcher) {
        runCatching {
            api.getRequests().all
                .filter { !it.id.isNullOrEmpty() }
                .map { it.toDomain() }
        }
    }

    override suspend fun getRequest(id: String): Result<WantedRequest> = withContext(dispatcher) {
        runCatching {
            val dto = api.getRequest(id).payload
                ?: error("Request not found")
            dto.toDomain()
        }
    }

    override suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest> =
        withContext(dispatcher) {
            runCatching {
                val dto = api.createRequest(body).payload
                    ?: error("Failed to create request")
                dto.toDomain()
            }
        }

    override suspend fun createOffer(
        requestId: String,
        body: CreateOfferBody,
    ): Result<WantedOffer> = withContext(dispatcher) {
        runCatching {
            val dto = api.createOffer(requestId, body).payload
                ?: error("Failed to submit offer")
            dto.toDomain(fallbackRequestId = requestId)
        }
    }

    override suspend fun getCategories(): Result<Map<WantedType, List<WantedCategoryOption>>> {
        cachedCategories?.let { return Result.success(it) }
        return withContext(dispatcher) {
            runCatching {
                // Double-checked under the mutex: a parallel caller may have
                // populated the cache while we were waiting on the lock.
                categoriesMutex.withLock {
                    cachedCategories?.let { return@withLock it }
                    val fresh = api.getCategories().toDomain()
                    if (fresh.isEmpty()) error("empty categories payload")
                    cachedCategories = fresh
                    fresh
                }
            }
        }
    }
}
