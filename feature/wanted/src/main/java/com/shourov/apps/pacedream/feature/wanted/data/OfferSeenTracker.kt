package com.shourov.apps.pacedream.feature.wanted.data

import android.content.Context
import android.content.SharedPreferences
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks how many offers the user has *seen* on each of their requests so
 * the Mine tab can render a dot badge when a fresh offer arrives.
 *
 * The spec called for DataStore; the rest of this codebase uses
 * SharedPreferences for the same kind of small per-user UI flags (see
 * `HostModeManager`), so we follow the established pattern.
 *
 * The contract is intentionally narrow:
 *  - [hasUnreadOffers] returns true when any request's current offerCount
 *    is greater than the last value we marked as seen for that request.
 *  - [markRequestsSeen] is called when the user opens the Mine tab — it
 *    snapshots the current counts so the badge clears.
 */
@Singleton
class OfferSeenTracker @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun hasUnreadOffers(requests: List<WantedRequest>): Boolean =
        requests.any { it.offerCount > seenCount(it.id) }

    fun markRequestsSeen(requests: List<WantedRequest>) {
        if (requests.isEmpty()) return
        prefs.edit().apply {
            requests.forEach { putInt(keyFor(it.id), it.offerCount) }
        }.apply()
    }

    private fun seenCount(requestId: String): Int =
        prefs.getInt(keyFor(requestId), 0)

    private fun keyFor(requestId: String): String = "$KEY_PREFIX$requestId"

    companion object {
        private const val PREFS_NAME = "wanted.offer_seen"
        private const val KEY_PREFIX = "seen."
    }
}
