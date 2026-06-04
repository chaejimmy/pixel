package com.shourov.apps.pacedream.feature.notifications

import android.net.Uri
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Decoded representation of a `pacedream://` push deep link.
 *
 * Supported schemes:
 *   pacedream://requests/{requestId}
 *   pacedream://requests/{requestId}/offers
 *   pacedream://bookings/{bookingId}
 */
sealed class PushDeepLink {
    data class RequestDetail(val requestId: String) : PushDeepLink()
    data class RequestOffers(val requestId: String) : PushDeepLink()
    data class BookingDetail(val bookingId: String) : PushDeepLink()

    companion object {
        fun parse(uri: Uri?): PushDeepLink? {
            if (uri == null) return null
            if (!uri.scheme.equals("pacedream", ignoreCase = true)) return null

            val host = uri.host?.lowercase().orEmpty()
            val pathSegments = uri.pathSegments.orEmpty()
            // Build a unified segment list because Uri puts the first token in host.
            val segments = buildList {
                add(host)
                addAll(pathSegments)
            }.filter { it.isNotBlank() }

            val first = segments.firstOrNull() ?: return null
            return when (first) {
                "requests" -> {
                    val requestId = segments.getOrNull(1) ?: return null
                    val sub = segments.getOrNull(2)?.lowercase()
                    if (sub == "offers") RequestOffers(requestId) else RequestDetail(requestId)
                }
                "bookings" -> {
                    val bookingId = segments.getOrNull(1) ?: return null
                    BookingDetail(bookingId)
                }
                else -> null
            }
        }

        fun parse(raw: String?): PushDeepLink? {
            if (raw.isNullOrBlank()) return null
            return runCatching { Uri.parse(raw) }.getOrNull()?.let { parse(it) }
        }
    }
}

/**
 * Routes incoming push deep links to the navigation graph.
 *
 * Wire it up in MainActivity:
 *
 *     class MainActivity : ComponentActivity() {
 *         override fun onCreate(savedInstanceState: Bundle?) {
 *             super.onCreate(savedInstanceState)
 *             intent?.data?.let { PushDeepLinkHandler.dispatch(it) }
 *             // ...
 *         }
 *         override fun onNewIntent(intent: Intent?) {
 *             super.onNewIntent(intent)
 *             intent?.data?.let { PushDeepLinkHandler.dispatch(it) }
 *         }
 *     }
 *
 * And in the NavHost / nav-component scope:
 *
 *     LaunchedEffect(Unit) {
 *         PushDeepLinkHandler.deepLinks.collect { link ->
 *             when (link) {
 *                 is PushDeepLink.RequestDetail -> navController.navigate(Routes.requestDetail(link.requestId))
 *                 is PushDeepLink.RequestOffers -> navController.navigate(Routes.requestOffers(link.requestId))
 *                 is PushDeepLink.BookingDetail -> navController.navigate(Routes.bookingDetail(link.bookingId))
 *             }
 *         }
 *     }
 */
object PushDeepLinkHandler {

    private val _deepLinks = MutableSharedFlow<PushDeepLink>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val deepLinks: SharedFlow<PushDeepLink> = _deepLinks.asSharedFlow()

    /**
     * Dispatch a push-notification URL (from intent data, OneSignal payload,
     * or App Links). Returns true if a deep link was emitted.
     */
    fun dispatch(uri: Uri?): Boolean {
        val link = PushDeepLink.parse(uri) ?: return false
        _deepLinks.tryEmit(link)
        return true
    }

    fun dispatch(raw: String?): Boolean {
        val link = PushDeepLink.parse(raw) ?: return false
        _deepLinks.tryEmit(link)
        return true
    }

    /**
     * OneSignal forwards `additionalData` as a JSONObject. Inspect both
     * `deepLink` and the lower-cased alias used by older clients.
     */
    fun dispatchFromAdditionalData(additionalData: Map<String, Any?>?): Boolean {
        if (additionalData == null) return false
        val raw = (additionalData["deepLink"] ?: additionalData["deeplink"]) as? String
        return dispatch(raw)
    }
}
