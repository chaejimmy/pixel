package com.pacedream.app.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TokenStorage - Secure token storage using EncryptedSharedPreferences
 * 
 * Stores:
 * - Backend JWT access token
 * - Backend refresh token
 * - Auth0 tokens (for reference)
 * - User ID
 * - Cached user summary (for offline display)
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, deleting corrupt prefs and retrying")
            // Delete potentially corrupt encrypted prefs file and retry once.
            // Also clear the AndroidKeyStore master key to recover from keystore corruption
            // (common on Samsung/Huawei after OTA updates or cache clears).
            try { context.deleteSharedPreferences(PREFS_NAME) } catch (_: Exception) {}
            try {
                clearMasterKeyFromKeyStore()
            } catch (_: Exception) {}
            try {
                createEncryptedPrefs()
            } catch (retryException: Exception) {
                Timber.e(retryException, "EncryptedSharedPreferences retry also failed, falling back to plain SharedPreferences")
                context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun clearMasterKeyFromKeyStore() {
        try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("_androidx_security_master_key_")
        } catch (e: Exception) {
            Timber.w(e, "Failed to clear master key from AndroidKeyStore")
        }
    }
    
    /** Backend JWT access token */
    var accessToken: String?
        get() = safeGetString(KEY_ACCESS_TOKEN)
        set(value) = safePutString(KEY_ACCESS_TOKEN, value)

    /** Backend refresh token */
    var refreshToken: String?
        get() = safeGetString(KEY_REFRESH_TOKEN)
        set(value) = safePutString(KEY_REFRESH_TOKEN, value)

    /** Auth0 access token (for reference) */
    var auth0AccessToken: String?
        get() = safeGetString(KEY_AUTH0_ACCESS_TOKEN)
        set(value) = safePutString(KEY_AUTH0_ACCESS_TOKEN, value)

    /** Auth0 ID token */
    var auth0IdToken: String?
        get() = safeGetString(KEY_AUTH0_ID_TOKEN)
        set(value) = safePutString(KEY_AUTH0_ID_TOKEN, value)

    /** Current user ID */
    var userId: String?
        get() = safeGetString(KEY_USER_ID)
        set(value) = safePutString(KEY_USER_ID, value)

    /** Cached user summary JSON (for offline display) */
    var cachedUserSummary: String?
        get() = safeGetString(KEY_CACHED_USER_SUMMARY)
        set(value) = safePutString(KEY_CACHED_USER_SUMMARY, value)

    /** Last checkout session ID (for resume after relaunch) */
    var lastCheckoutSessionId: String?
        get() = safeGetString(KEY_LAST_CHECKOUT_SESSION_ID)
        set(value) = safePutString(KEY_LAST_CHECKOUT_SESSION_ID, value)

    /** Last checkout booking type (timebased/gear) */
    var lastCheckoutBookingType: String?
        get() = safeGetString(KEY_LAST_CHECKOUT_BOOKING_TYPE)
        set(value) = safePutString(KEY_LAST_CHECKOUT_BOOKING_TYPE, value)

    /** Guest/Host mode */
    var isHostMode: Boolean
        get() = try { prefs.getBoolean(KEY_IS_HOST_MODE, false) } catch (e: Exception) { Timber.e(e, "Failed to read isHostMode"); false }
        set(value) { try { prefs.edit().putBoolean(KEY_IS_HOST_MODE, value).apply() } catch (e: Exception) { Timber.e(e, "Failed to write isHostMode") } }

    private fun safeGetString(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to read key: $key from encrypted prefs")
            null
        }
    }

    private fun safePutString(key: String, value: String?) {
        try {
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to write key: $key to encrypted prefs")
        }
    }
    
    /**
     * Check if tokens exist
     */
    fun hasTokens(): Boolean = try { !accessToken.isNullOrBlank() } catch (e: Exception) { Timber.e(e, "Failed to check tokens"); false }

    /**
     * Store backend tokens
     */
    fun storeTokens(accessToken: String?, refreshToken: String?) {
        try {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply()
            Timber.d("Tokens stored")
        } catch (e: Exception) {
            Timber.e(e, "Failed to store tokens")
        }
    }
    
    /**
     * Store checkout session for resume
     */
    fun storeCheckoutSession(sessionId: String, bookingType: String) {
        lastCheckoutSessionId = sessionId
        lastCheckoutBookingType = bookingType
    }
    
    /**
     * Clear checkout session
     */
    fun clearCheckoutSession() {
        lastCheckoutSessionId = null
        lastCheckoutBookingType = null
    }
    
    /**
     * Clear all tokens and user data
     */
    fun clearAll() {
        try {
            prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_AUTH0_ACCESS_TOKEN)
                .remove(KEY_AUTH0_ID_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_CACHED_USER_SUMMARY)
                .apply()
            Timber.d("All tokens cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear tokens")
        }
    }
    
    /**
     * Validate JWT shape (3 dot-separated parts)
     * Matching iOS behavior
     */
    fun isValidJwtShape(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val parts = token.split(".")
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }
    
    companion object {
        private const val PREFS_NAME = "pacedream_secure_prefs"
        private const val PREFS_NAME_FALLBACK = "pacedream_prefs_fallback"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_AUTH0_ACCESS_TOKEN = "auth0_access_token"
        private const val KEY_AUTH0_ID_TOKEN = "auth0_id_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CACHED_USER_SUMMARY = "cached_user_summary"
        private const val KEY_LAST_CHECKOUT_SESSION_ID = "last_checkout_session_id"
        private const val KEY_LAST_CHECKOUT_BOOKING_TYPE = "last_checkout_booking_type"
        private const val KEY_IS_HOST_MODE = "is_host_mode"
    }
}


