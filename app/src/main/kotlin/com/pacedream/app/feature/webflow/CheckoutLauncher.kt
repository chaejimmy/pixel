package com.pacedream.app.feature.webflow

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CheckoutLauncher - Opens Stripe checkout URLs in Custom Tabs
 * 
 * iOS Parity:
 * - Create booking via POST endpoint
 * - Open checkoutUrl in Custom Tabs
 * - Persist session for resume after app relaunch
 * 
 * Booking endpoints:
 * - Time-based: POST /v1/properties/bookings/timebased
 * - Gear: POST /v1/gear-rentals/book
 */
@Singleton
class CheckoutLauncher @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json
) {
    
    /**
     * Create time-based booking and launch checkout
     */
    suspend fun createTimeBasedBooking(
        context: Context,
        itemId: String,
        startTime: String,
        endTime: String,
        amount: Double
    ): Result<Unit> {
        val url = appConfig.buildApiUrl("properties", "bookings", "timebased")
        val body = json.encodeToString(
            TimeBasedBookingRequest.serializer(),
            TimeBasedBookingRequest(
                itemId = itemId,
                start_time = startTime,
                end_time = endTime,
                amount = amount
            )
        )
        
        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                val checkoutUrl = parseCheckoutUrl(result.data)
                if (checkoutUrl != null) {
                    // Persist for resume
                    parseSessionId(checkoutUrl)?.let { sessionId ->
                        tokenStorage.storeCheckoutSession(sessionId, "timebased")
                    }
                    
                    launchCheckoutUrl(context, checkoutUrl)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("No checkout URL returned"))
                }
            }
            is ApiResult.Failure -> {
                Timber.e("Failed to create time-based booking: ${result.error.message}")
                Result.failure(Exception(result.error.message))
            }
        }
    }
    
    /**
     * Create gear booking and launch checkout
     */
    suspend fun createGearBooking(
        context: Context,
        gearId: String,
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        amount: Double
    ): Result<Unit> {
        val url = appConfig.buildApiUrl("gear-rentals", "book")
        val body = json.encodeToString(
            GearBookingRequest.serializer(),
            GearBookingRequest(
                gearId = gearId,
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                amount = amount
            )
        )
        
        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                val checkoutUrl = parseCheckoutUrl(result.data)
                if (checkoutUrl != null) {
                    // Persist for resume
                    parseSessionId(checkoutUrl)?.let { sessionId ->
                        tokenStorage.storeCheckoutSession(sessionId, "gear")
                    }
                    
                    launchCheckoutUrl(context, checkoutUrl)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("No checkout URL returned"))
                }
            }
            is ApiResult.Failure -> {
                Timber.e("Failed to create gear booking: ${result.error.message}")
                Result.failure(Exception(result.error.message))
            }
        }
    }
    
    /**
     * Launch checkout URL in Custom Tabs
     */
    private fun launchCheckoutUrl(context: Context, url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            
            customTabsIntent.launchUrl(context, Uri.parse(url))
            Timber.d("Launched checkout: $url")
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch Custom Tabs")
            throw e
        }
    }
    
    /**
     * Parse checkout URL from response
     */
    private fun parseCheckoutUrl(responseBody: String): String? {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            val data = obj["data"]?.jsonObject ?: obj
            
            data["checkoutUrl"]?.jsonPrimitive?.content
                ?: data["checkout_url"]?.jsonPrimitive?.content
                ?: data["url"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse checkout URL")
            null
        }
    }
    
    /**
     * Extract session_id from Stripe checkout URL
     */
    private fun parseSessionId(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter("session_id")
                ?: uri.pathSegments.lastOrNull()?.takeIf { it.startsWith("cs_") }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse session ID from URL")
            null
        }
    }
}

/**
 * Time-based booking request
 */
@Serializable
data class TimeBasedBookingRequest(
    val itemId: String,
    val start_time: String,
    val end_time: String,
    val amount: Double
)

/**
 * Gear booking request
 */
@Serializable
data class GearBookingRequest(
    val gearId: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val amount: Double
)


