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
     * Parse URI and determine navigation destination.
     *
     * Match logic lives in [Companion.matchFromParts], a pure function
     * that takes the already-decomposed URI parts so it can be unit
     * tested without Robolectric.  Side effects (notifying
     * [BookingRepository] of a cancellation, looking up a stored
     * checkout session) are layered on top here so behaviour is
     * identical to the old single-method implementation.
     */
    fun parseUri(uri: Uri): DeepLinkResult? {
        val host = uri.host?.lowercase() ?: return null
        val path = uri.path?.lowercase() ?: return null

        Timber.d("Parsing deep link for host: %s, path: %s", host, path)

        val match = matchFromParts(
            scheme = uri.scheme?.lowercase(),
            host = host,
            path = path,
            segments = uri.pathSegments.orEmpty(),
            sessionIdQuery = uri.getQueryParameter("session_id"),
        ) ?: return null

        return when (match) {
            // booking-cancelled path needs to notify the repository as a
            // side effect before we forward the result.  Kept here (not in
            // the pure matcher) so the matcher remains stateless.
            is MatchResult.BookingCancelled -> {
                bookingRepository.handleBookingCancelled()
                DeepLinkResult.BookingCancelled
            }
            // booking-success without an explicit session_id falls back to
            // the most-recent stored checkout, again as a side effect.
            is MatchResult.BookingSuccessFallback -> checkStoredCheckout()
            is MatchResult.Direct -> match.result
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
     * Internal sealed result produced by [matchFromParts].  Direct results
     * map straight to a [DeepLinkResult]; the BookingCancelled and
     * BookingSuccessFallback variants signal that [parseUri] should run
     * an additional side effect against the booking repository.
     */
    internal sealed interface MatchResult {
        data class Direct(val result: DeepLinkResult) : MatchResult
        data object BookingCancelled : MatchResult
        data object BookingSuccessFallback : MatchResult
    }

    companion object {
        private val VALID_ID_PATTERN = Regex("^[a-zA-Z0-9_\\-]+$")

        /**
         * Validate that an ID from a deep link is safe to use.
         * Allows alphanumeric characters, hyphens, and underscores (covers
         * UUIDs, MongoDB ObjectIds, Stripe session IDs).
         */
        private fun isValidId(id: String): Boolean {
            return id.length in 1..256 && id.matches(VALID_ID_PATTERN)
        }

        /**
         * Pure URI matcher.  No repository or platform dependencies, so
         * unit tests construct decomposed parts directly without needing
         * an Android [Uri] instance.  Behaviour matches the historical
         * single-method [parseUri] except that two paths return marker
         * results that [parseUri] wraps with side effects.
         */
        @JvmStatic
        internal fun matchFromParts(
            scheme: String?,
            host: String,
            path: String,
            segments: List<String>,
            sessionIdQuery: String?,
        ): MatchResult? {
            if (!host.contains("pacedream.com")) {
                return null
            }

            // Two parallel lists: lowercase for keyword matching, and the
            // original-case segments for the IDs we hand back (Stripe /
            // Mongo IDs can be case-sensitive).
            val rawSegments = segments.filter { it.isNotBlank() }
            val lowerSegments = rawSegments.map { it.lowercase() }
            val firstSegment = lowerSegments.firstOrNull()
            val secondSegment = rawSegments.getOrNull(1)
            val lastSegment = rawSegments.lastOrNull()

            return when {
                scheme == "pacedream" && (host == "stripe-connect-return" || host == "stripe-connect-refresh") ->
                    MatchResult.Direct(DeepLinkResult.StripeConnectReturn)

                path.contains("stripe-connect-return") || path.contains("stripe-connect-refresh") ->
                    MatchResult.Direct(DeepLinkResult.StripeConnectReturn)

                path.contains("booking-success") -> {
                    if (sessionIdQuery != null && isValidId(sessionIdQuery)) {
                        MatchResult.Direct(DeepLinkResult.BookingSuccess(sessionIdQuery))
                    } else {
                        MatchResult.BookingSuccessFallback
                    }
                }

                path.contains("booking-cancelled") -> MatchResult.BookingCancelled

                // /bookings/{id} — direct link to a booking detail.
                firstSegment == "bookings" -> {
                    val bookingId = secondSegment ?: lastSegment.takeIf { lowerSegments.size > 1 }
                    if (bookingId != null && bookingId.lowercase() != "bookings" && isValidId(bookingId)) {
                        MatchResult.Direct(DeepLinkResult.BookingDetail(bookingId))
                    } else null
                }

                // /threads/{id} or /messages/{id} — direct link to a chat thread.
                firstSegment == "threads" || firstSegment == "messages" -> {
                    val threadId = secondSegment ?: lastSegment.takeIf { lowerSegments.size > 1 }
                    if (threadId != null &&
                        threadId.lowercase() != firstSegment &&
                        isValidId(threadId)
                    ) {
                        MatchResult.Direct(DeepLinkResult.Thread(threadId))
                    } else null
                }

                path.contains("listing") || path.contains("property") -> {
                    if (lastSegment != null && isValidId(lastSegment)) {
                        MatchResult.Direct(DeepLinkResult.ListingDetail(lastSegment))
                    } else null
                }

                path.contains("gear") -> {
                    if (lastSegment != null && isValidId(lastSegment)) {
                        MatchResult.Direct(DeepLinkResult.GearDetail(lastSegment))
                    } else null
                }

                else -> null
            }
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

    /** External /bookings/{id} deep link — opens BookingDetail. */
    data class BookingDetail(val bookingId: String) : DeepLinkResult()

    /** External /threads/{id} or /messages/{id} deep link — opens chat thread. */
    data class Thread(val threadId: String) : DeepLinkResult()

    /** iOS PR #200 parity: Stripe Connect onboarding return */
    object StripeConnectReturn : DeepLinkResult()
}


