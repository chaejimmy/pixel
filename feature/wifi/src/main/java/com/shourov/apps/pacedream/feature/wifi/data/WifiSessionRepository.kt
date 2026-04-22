package com.shourov.apps.pacedream.feature.wifi.data

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiSessionRepository @Inject constructor(
    private val api: WifiSessionApi
) {
    suspend fun getSession(sessionId: String): Result<WifiSessionResponse> = try {
        val response = api.getSession(sessionId)
        if (response.isSuccessful) {
            response.body()?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Empty wifi session body"))
        } else {
            Result.failure(IllegalStateException("Wifi session fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Timber.e(e, "WifiSessionRepository.getSession failed")
        Result.failure(e)
    }

    suspend fun extend(sessionId: String, minutes: Int): Result<WifiExtendResponse> = try {
        val response = api.extendSession(sessionId, WifiExtendRequest(minutes))
        if (response.isSuccessful) {
            response.body()?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Empty extend body"))
        } else {
            Result.failure(IllegalStateException("Wifi extend failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Timber.e(e, "WifiSessionRepository.extend failed")
        Result.failure(e)
    }

    suspend fun reconnect(sessionId: String): Result<WifiSessionResponse> = try {
        val response = api.reconnect(sessionId)
        if (response.isSuccessful) {
            response.body()?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Empty reconnect body"))
        } else {
            Result.failure(IllegalStateException("Wifi reconnect failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Timber.e(e, "WifiSessionRepository.reconnect failed")
        Result.failure(e)
    }
}
