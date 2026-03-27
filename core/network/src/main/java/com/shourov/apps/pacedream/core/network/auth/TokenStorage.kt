package com.shourov.apps.pacedream.core.network.auth

import android.content.Context
import kotlin.jvm.JvmName
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences
 * Matches iOS Keychain-based token storage pattern
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenProvider {
    
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create encrypted prefs, deleting corrupt prefs and retrying")
            // Delete potentially corrupt encrypted prefs file and retry once
            context.deleteSharedPreferences(PREFS_NAME)
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (retryException: Exception) {
                Timber.e(retryException, "EncryptedSharedPreferences retry also failed, falling back to plain SharedPreferences")
                context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
            }
        }
    }
    
    /**
     * Store the backend JWT access token
     */
    var accessToken: String?
        @JvmName("getAccessTokenProperty")
        get() = safeGetString(KEY_ACCESS_TOKEN)
        set(value) = safePutString(KEY_ACCESS_TOKEN, value)

    /**
     * Store the refresh token
     */
    var refreshToken: String?
        @JvmName("getRefreshTokenProperty")
        get() = safeGetString(KEY_REFRESH_TOKEN)
        set(value) = safePutString(KEY_REFRESH_TOKEN, value)

    /**
     * Store the Auth0 access token (for exchange purposes)
     */
    var auth0AccessToken: String?
        get() = safeGetString(KEY_AUTH0_ACCESS_TOKEN)
        set(value) = safePutString(KEY_AUTH0_ACCESS_TOKEN, value)

    /**
     * Store the Auth0 ID token
     */
    var auth0IdToken: String?
        get() = safeGetString(KEY_AUTH0_ID_TOKEN)
        set(value) = safePutString(KEY_AUTH0_ID_TOKEN, value)

    /**
     * Store user ID for caching purposes
     */
    var userId: String?
        get() = safeGetString(KEY_USER_ID)
        set(value) = safePutString(KEY_USER_ID, value)

    /**
     * Store cached user summary (for offline access)
     */
    var cachedUserSummary: String?
        get() = safeGetString(KEY_CACHED_USER)
        set(value) = safePutString(KEY_CACHED_USER, value)

    /**
     * Store last checkout session ID for resuming after app relaunch
     */
    var lastCheckoutSessionId: String?
        get() = safeGetString(KEY_CHECKOUT_SESSION_ID)
        set(value) = safePutString(KEY_CHECKOUT_SESSION_ID, value)

    /**
     * Store last checkout booking type
     */
    var lastCheckoutBookingType: String?
        get() = safeGetString(KEY_CHECKOUT_BOOKING_TYPE)
        set(value) = safePutString(KEY_CHECKOUT_BOOKING_TYPE, value)

    private fun safeGetString(key: String): String? {
        return try {
            encryptedPrefs.getString(key, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to read key: $key from encrypted prefs")
            null
        }
    }

    private fun safePutString(key: String, value: String?) {
        try {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(key, value)
                } else {
                    remove(key)
                }
            }.apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to write key: $key to encrypted prefs")
        }
    }
    
    /**
     * Check if user has tokens stored (considered authenticated)
     */
    fun hasTokens(): Boolean = try { !accessToken.isNullOrBlank() } catch (e: Exception) { Timber.e(e, "Failed to check tokens"); false }

    /**
     * Validate JWT shape (3 dot-separated parts)
     */
    fun isValidJwtShape(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val parts = token.split(".")
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }

    /**
     * Store both tokens at once
     */
    fun storeTokens(accessToken: String?, refreshToken: String?) {
        try {
            encryptedPrefs.edit().apply {
                if (accessToken != null) putString(KEY_ACCESS_TOKEN, accessToken) else remove(KEY_ACCESS_TOKEN)
                if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken) else remove(KEY_REFRESH_TOKEN)
            }.apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to store tokens")
        }
    }

    /**
     * Clear all tokens (sign out)
     */
    fun clearAll() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_AUTH0_ACCESS_TOKEN)
                .remove(KEY_AUTH0_ID_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_CACHED_USER)
                .remove(KEY_CHECKOUT_SESSION_ID)
                .remove(KEY_CHECKOUT_BOOKING_TYPE)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all tokens")
        }
    }

    /**
     * Clear only backend tokens (keep Auth0 tokens for re-auth)
     */
    fun clearBackendTokens() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear backend tokens")
        }
    }

    // TokenProvider interface implementation
    override fun getAccessToken(): String? = try { accessToken } catch (e: Exception) { Timber.e(e, "Failed to get access token"); null }
    override fun getRefreshToken(): String? = try { refreshToken } catch (e: Exception) { Timber.e(e, "Failed to get refresh token"); null }
    
    companion object {
        private const val PREFS_NAME = "pacedream_secure_prefs"
        private const val PREFS_NAME_FALLBACK = "pacedream_prefs_fallback"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_AUTH0_ACCESS_TOKEN = "auth0_access_token"
        private const val KEY_AUTH0_ID_TOKEN = "auth0_id_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CACHED_USER = "cached_user"
        private const val KEY_CHECKOUT_SESSION_ID = "checkout_session_id"
        private const val KEY_CHECKOUT_BOOKING_TYPE = "checkout_booking_type"
    }
}


