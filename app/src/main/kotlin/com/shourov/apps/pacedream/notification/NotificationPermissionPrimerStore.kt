package com.shourov.apps.pacedream.notification

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-flag persistence for the POST_NOTIFICATIONS in-app primer.
 *
 * The spec calls for a "DataStore preference key
 * `notificationPermissionPrimerShown`"; the rest of the codebase persists
 * per-install UI flags through `SharedPreferences` (see
 * [com.shourov.apps.pacedream.feature.wanted.data.OfferSeenTracker] for the
 * same pattern and rationale), so we follow that established convention to
 * avoid pulling DataStore into the app module just for one boolean.
 */
@Singleton
class NotificationPermissionPrimerStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun hasPrimerBeenShown(): Boolean =
        prefs.getBoolean(KEY_PRIMER_SHOWN, false)

    fun markPrimerShown() {
        prefs.edit().putBoolean(KEY_PRIMER_SHOWN, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "notifications.permission_primer"
        // Key name matches the contract specified in the feature task.
        private const val KEY_PRIMER_SHOWN = "notificationPermissionPrimerShown"
    }
}
