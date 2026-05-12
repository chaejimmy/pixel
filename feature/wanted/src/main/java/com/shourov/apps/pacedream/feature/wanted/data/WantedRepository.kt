package com.shourov.apps.pacedream.feature.wanted.data

import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.toDomain
import com.shourov.apps.pacedream.feature.wanted.data.remote.WantedApiService
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
}

@Singleton
class WantedRepositoryImpl @Inject constructor(
    private val api: WantedApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WantedRepository {

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
}
