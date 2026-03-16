package com.shourov.apps.pacedream.notification

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists FCM token and registration state in SharedPreferences.
 *
 * iOS parity: mirrors PushTokenStore.swift which stores APNs token in UserDefaults
 * and tracks (token, userId) pairs for deduplication across app sessions.
 */
@Singleton
class FcmTokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pd_fcm_token_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_LAST_REGISTERED = "last_registered_key"
    }

    /**
     * Save the current FCM token.
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        Timber.d("FCM token persisted")
    }

    /**
     * Get the stored FCM token, or null if not yet saved.
     */
    fun getToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)

    /**
     * Check whether this (token, userId) pair has already been registered
     * with the backend. Persists across process restarts (iOS parity).
     */
    fun isAlreadyRegistered(token: String, userId: String?): Boolean {
        val key = "${token}_${userId}"
        return prefs.getString(KEY_LAST_REGISTERED, null) == key
    }

    /**
     * Mark a (token, userId) pair as registered with the backend.
     */
    fun markRegistered(token: String, userId: String?) {
        val key = "${token}_${userId}"
        prefs.edit().putString(KEY_LAST_REGISTERED, key).apply()
    }

    /**
     * Reset registration state. Called on sign-in so the token is
     * re-registered for the new user (iOS parity: PushDeviceRegistrar.reset()).
     */
    fun reset() {
        prefs.edit().remove(KEY_LAST_REGISTERED).apply()
        Timber.d("FCM token registration state reset")
    }

    /**
     * Clear all stored data (sign-out).
     */
    fun clear() {
        prefs.edit().clear().apply()
        Timber.d("FCM token store cleared")
    }
}
