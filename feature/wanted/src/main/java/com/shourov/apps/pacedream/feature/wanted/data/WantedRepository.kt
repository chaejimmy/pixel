package com.shourov.apps.pacedream.feature.wanted.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.toDomain
import com.shourov.apps.pacedream.feature.wanted.data.remote.WantedApiService
import com.shourov.apps.pacedream.feature.wanted.model.HostListingSummary
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
    /** Requests authored by the currently authenticated user. */
    suspend fun getMyRequests(): Result<List<WantedRequest>>
    suspend fun getRequest(id: String): Result<WantedRequest>
    /** Offers the current user has received on the given request. */
    suspend fun getOffersForRequest(requestId: String): Result<List<WantedOffer>>
    /** Offers the current user has sent (across all requests). */
    suspend fun getMyOffers(): Result<List<WantedOffer>>
    suspend fun createRequest(body: CreateRequestBody): Result<WantedRequest>
    suspend fun createOffer(requestId: String, body: CreateOfferBody): Result<WantedOffer>
    suspend fun getHostListings(): Result<List<HostListingSummary>>

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

    override suspend fun getMyRequests(): Result<List<WantedRequest>> = withContext(dispatcher) {
        runCatching {
            api.getRequests(mine = true).all
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

    override suspend fun getOffersForRequest(
        requestId: String,
    ): Result<List<WantedOffer>> = withContext(dispatcher) {
        runCatching {
            api.getOffersForRequest(requestId).all
                .filter { !it.id.isNullOrEmpty() }
                .map { it.toDomain(fallbackRequestId = requestId) }
        }
    }

    override suspend fun getMyOffers(): Result<List<WantedOffer>> = withContext(dispatcher) {
        runCatching {
            api.getOffers(mine = true).all
                .filter { !it.id.isNullOrEmpty() }
                .map { it.toDomain(fallbackRequestId = "") }
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

    /**
     * GET /v1/host/listings is shared with the host dashboard, which may
     * return any of:
     *   - a raw array
     *   - `{ data: [...] }` / `{ items: [...] }` / `{ results: [...] }`
     *   - category-keyed `{ rooms: [...], rentableItems: [...], ... }`
     * The composer only needs an id + title pair per published listing, so
     * we walk the response and drop everything else. Anything without a
     * usable id is filtered out.
     */
    override suspend fun getHostListings(): Result<List<HostListingSummary>> =
        withContext(dispatcher) {
            runCatching {
                extractListingSummaries(api.getHostListings())
            }
        }

    private fun extractListingSummaries(json: JsonElement): List<HostListingSummary> {
        val out = mutableListOf<HostListingSummary>()
        val seen = mutableSetOf<String>()

        fun emit(obj: JsonObject) {
            val id = obj.stringOrNull("_id")
                ?: obj.stringOrNull("id")
                ?: obj.stringOrNull("listingId")
                ?: return
            if (!seen.add(id)) return
            val title = obj.stringOrNull("title")
                ?: obj.stringOrNull("name")
                ?: "Untitled listing"
            out += HostListingSummary(id = id, title = title)
        }

        fun walk(element: JsonElement?) {
            element ?: return
            when {
                element.isJsonArray -> element.asJsonArray.forEach { item ->
                    if (item.isJsonObject) emit(item.asJsonObject)
                }
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val wrapped = obj["data"] ?: obj["items"] ?: obj["results"]
                        ?: obj["listings"]
                    if (wrapped != null) {
                        walk(wrapped)
                        if (out.isNotEmpty()) return
                    }
                    // Category-keyed shape used by the host dashboard.
                    for (key in CATEGORY_KEYS) {
                        walk(obj[key])
                    }
                }
            }
        }

        walk(json)
        return out
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val el = get(key) ?: return null
        return if (el.isJsonPrimitive && el.asJsonPrimitive.isString)
            el.asString.takeIf { it.isNotBlank() }
        else null
    }

    private companion object {
        private val CATEGORY_KEYS = listOf(
            "rooms", "properties", "rentableItems",
            "services", "attractions", "roommates",
        )
    }
}
