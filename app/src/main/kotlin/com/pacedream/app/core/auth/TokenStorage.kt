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
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val prefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, falling back to regular prefs")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /** Backend JWT access token */
    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()
    
    /** Backend refresh token */
    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()
    
    /** Auth0 access token (for reference) */
    var auth0AccessToken: String?
        get() = prefs.getString(KEY_AUTH0_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH0_ACCESS_TOKEN, value).apply()
    
    /** Auth0 ID token */
    var auth0IdToken: String?
        get() = prefs.getString(KEY_AUTH0_ID_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH0_ID_TOKEN, value).apply()
    
    /** Current user ID */
    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()
    
    /** Cached user summary JSON (for offline display) */
    var cachedUserSummary: String?
        get() = prefs.getString(KEY_CACHED_USER_SUMMARY, null)
        set(value) = prefs.edit().putString(KEY_CACHED_USER_SUMMARY, value).apply()
    
    /** Last checkout session ID (for resume after relaunch) */
    var lastCheckoutSessionId: String?
        get() = prefs.getString(KEY_LAST_CHECKOUT_SESSION_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_CHECKOUT_SESSION_ID, value).apply()
    
    /** Last checkout booking type (timebased/gear) */
    var lastCheckoutBookingType: String?
        get() = prefs.getString(KEY_LAST_CHECKOUT_BOOKING_TYPE, null)
        set(value) = prefs.edit().putString(KEY_LAST_CHECKOUT_BOOKING_TYPE, value).apply()
    
    /** Guest/Host mode */
    var isHostMode: Boolean
        get() = prefs.getBoolean(KEY_IS_HOST_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_HOST_MODE, value).apply()
    
    /**
     * Check if tokens exist
     */
    fun hasTokens(): Boolean = !accessToken.isNullOrBlank()
    
    /**
     * Store backend tokens
     */
    fun storeTokens(accessToken: String?, refreshToken: String?) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        Timber.d("Tokens stored")
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
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_AUTH0_ACCESS_TOKEN)
            .remove(KEY_AUTH0_ID_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_CACHED_USER_SUMMARY)
            .apply()
        Timber.d("All tokens cleared")
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


