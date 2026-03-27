package com.shourov.apps.pacedream.feature.wishlist.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local cache for favorite/wishlist IDs.
 *
 * Persists favorite IDs to SharedPreferences so that:
 * - Hearts render correctly on startup even when the network is unavailable
 * - Toggle operations that fail due to network errors are queued for retry
 *
 * Thread-safety: SharedPreferences apply() is atomic; concurrent reads/writes
 * are safe for the expected call patterns (UI thread reads, IO thread writes).
 */
@Singleton
class FavoritesCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Load cached favorite IDs (returns empty set if none cached).
     */
    fun getCachedFavoriteIds(): Set<String> {
        return try {
            prefs.getStringSet(KEY_FAVORITE_IDS, emptySet())?.toSet() ?: emptySet()
        } catch (e: Exception) {
            Timber.w(e, "Failed to read cached favorites")
            emptySet()
        }
    }

    /**
     * Save the current set of favorite IDs to disk.
     */
    fun saveFavoriteIds(ids: Set<String>) {
        try {
            prefs.edit().putStringSet(KEY_FAVORITE_IDS, ids).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to save cached favorites")
        }
    }

    /**
     * Get pending toggle operations that failed due to network errors.
     * Returns a set of property IDs that need to be toggled on next successful connection.
     */
    fun getPendingToggles(): Set<String> {
        return try {
            prefs.getStringSet(KEY_PENDING_TOGGLES, emptySet())?.toSet() ?: emptySet()
        } catch (e: Exception) {
            Timber.w(e, "Failed to read pending toggles")
            emptySet()
        }
    }

    /**
     * Add a property ID to the pending toggle queue.
     * If it's already pending, remove it (double-toggle cancels out).
     */
    fun addPendingToggle(propertyId: String) {
        try {
            val current = getPendingToggles().toMutableSet()
            if (current.contains(propertyId)) {
                current.remove(propertyId)
            } else {
                current.add(propertyId)
            }
            prefs.edit().putStringSet(KEY_PENDING_TOGGLES, current).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to save pending toggle")
        }
    }

    /**
     * Clear all pending toggles (called after successful sync).
     */
    fun clearPendingToggles() {
        try {
            prefs.edit().remove(KEY_PENDING_TOGGLES).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to clear pending toggles")
        }
    }

    /**
     * Remove a specific pending toggle (called after individual sync success).
     */
    fun removePendingToggle(propertyId: String) {
        try {
            val current = getPendingToggles().toMutableSet()
            current.remove(propertyId)
            prefs.edit().putStringSet(KEY_PENDING_TOGGLES, current).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to remove pending toggle")
        }
    }

    companion object {
        private const val PREFS_NAME = "pacedream_favorites_cache"
        private const val KEY_FAVORITE_IDS = "favorite_ids"
        private const val KEY_PENDING_TOGGLES = "pending_toggles"
    }
}
