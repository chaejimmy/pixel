package com.shourov.apps.pacedream.feature.wifi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Process-wide intent bus for Wi-Fi session UI.
 *
 * Mirrors the [com.shourov.apps.pacedream.navigation.TabRouter] pattern: the
 * push notification handler does not own a NavHost reference, so we publish
 * intents through this object and let [WifiSessionHost] (mounted in the app
 * shell) react to them.
 *
 * Intents are coalesced through an unbuffered Channel to avoid replaying old
 * events on subscriber re-attach (e.g. configuration change).
 */
object WifiSessionRouter {

    sealed class Intent {
        /** Server confirmed a new Wi-Fi session is active. */
        data class Start(
            val sessionId: String?,
            val ssid: String?,
            val expiresAtIso: String?,
            val bookingId: String?
        ) : Intent()

        /** Open the extension bottom sheet (10/3-min warnings or pill tap). */
        data class ShowExtend(val sessionId: String?) : Intent()

        /** Show the expired modal with grace-period reconnect option. */
        data class ShowExpired(val sessionId: String?) : Intent()

        /** Force a refetch of session state (e.g. after extension confirmed). */
        data class Refresh(val sessionId: String?) : Intent()

        /** Open the read-only session sheet (SSID, password, end time). */
        data class ShowSheet(val sessionId: String?) : Intent()

        /** Clear local session state (e.g. user dismissed expired). */
        data object Clear : Intent()
    }

    private val intents = Channel<Intent>(Channel.BUFFERED)
    val flow = intents.receiveAsFlow()

    fun dispatch(intent: Intent) {
        intents.trySend(intent)
    }
}
