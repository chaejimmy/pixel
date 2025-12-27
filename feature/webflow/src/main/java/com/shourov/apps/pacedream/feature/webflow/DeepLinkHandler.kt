package com.shourov.apps.pacedream.feature.webflow

import android.content.Intent
import android.net.Uri
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.webflow.data.BookingRepository
import com.shourov.apps.pacedream.feature.webflow.data.BookingType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles deep links for:
 * - https://www.pacedream.com/booking-success?session_id=...
 * - https://www.pacedream.com/booking-cancelled
 * 
 * Also handles resume after app relaunch by checking stored session
 */
@Singleton
class DeepLinkHandler @Inject constructor(
    private val appConfig: AppConfig,
    private val bookingRepository: BookingRepository
) {
    
    /**
     * Parse deep link from intent and return navigation destination
     */
    fun parseDeepLink(intent: Intent?): DeepLinkResult? {
        val uri = intent?.data ?: return null
        return parseUri(uri)
    }
    
    /**
     * Parse URI and determine navigation destination
     */
    fun parseUri(uri: Uri): DeepLinkResult? {
        val host = uri.host?.lowercase() ?: return null
        val path = uri.path?.lowercase() ?: return null
        
        Timber.d("Parsing deep link: $uri")
        
        // Check if this is a PaceDream URL
        if (!host.contains("pacedream.com")) {
            return null
        }
        
        return when {
            path.contains("booking-success") -> {
                val sessionId = uri.getQueryParameter("session_id")
                if (sessionId != null) {
                    DeepLinkResult.BookingSuccess(sessionId)
                } else {
                    // Check stored session
                    checkStoredCheckout()
                }
            }
            
            path.contains("booking-cancelled") -> {
                bookingRepository.handleBookingCancelled()
                DeepLinkResult.BookingCancelled
            }
            
            // Could add more deep link patterns here
            path.contains("listing") || path.contains("property") -> {
                val listingId = uri.lastPathSegment
                if (listingId != null) {
                    DeepLinkResult.ListingDetail(listingId)
                } else null
            }
            
            path.contains("gear") -> {
                val gearId = uri.lastPathSegment
                if (gearId != null) {
                    DeepLinkResult.GearDetail(gearId)
                } else null
            }
            
            else -> null
        }
    }
    
    /**
     * Check for stored checkout session (for resume after relaunch)
     */
    fun checkStoredCheckout(): DeepLinkResult? {
        val stored = bookingRepository.getStoredCheckout()
        return stored?.let {
            DeepLinkResult.BookingSuccess(it.sessionId, it.bookingType)
        }
    }
    
    /**
     * Determine booking type from stored session or context
     */
    fun getBookingTypeForSession(sessionId: String): BookingType {
        // Check stored booking type
        val stored = bookingRepository.getStoredCheckout()
        if (stored?.sessionId == sessionId) {
            return stored.bookingType
        }
        
        // Default to time-based if unknown
        return BookingType.TIME_BASED
    }
}

/**
 * Result of parsing a deep link
 */
sealed class DeepLinkResult {
    data class BookingSuccess(
        val sessionId: String,
        val bookingType: BookingType? = null
    ) : DeepLinkResult()
    
    object BookingCancelled : DeepLinkResult()
    
    data class ListingDetail(val listingId: String) : DeepLinkResult()
    
    data class GearDetail(val gearId: String) : DeepLinkResult()
}

