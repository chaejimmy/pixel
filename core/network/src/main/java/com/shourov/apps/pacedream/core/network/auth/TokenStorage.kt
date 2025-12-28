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
            Timber.e(e, "Failed to create encrypted prefs, falling back to regular prefs")
            // Fallback to regular SharedPreferences (less secure but functional)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Store the backend JWT access token
     */
    var accessToken: String?
        @JvmName("getAccessTokenProperty")
        get() = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_ACCESS_TOKEN, value)
                } else {
                    remove(KEY_ACCESS_TOKEN)
                }
            }.apply()
        }
    
    /**
     * Store the refresh token
     */
    var refreshToken: String?
        @JvmName("getRefreshTokenProperty")
        get() = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_REFRESH_TOKEN, value)
                } else {
                    remove(KEY_REFRESH_TOKEN)
                }
            }.apply()
        }
    
    /**
     * Store the Auth0 access token (for exchange purposes)
     */
    var auth0AccessToken: String?
        get() = encryptedPrefs.getString(KEY_AUTH0_ACCESS_TOKEN, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_AUTH0_ACCESS_TOKEN, value)
                } else {
                    remove(KEY_AUTH0_ACCESS_TOKEN)
                }
            }.apply()
        }
    
    /**
     * Store the Auth0 ID token
     */
    var auth0IdToken: String?
        get() = encryptedPrefs.getString(KEY_AUTH0_ID_TOKEN, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_AUTH0_ID_TOKEN, value)
                } else {
                    remove(KEY_AUTH0_ID_TOKEN)
                }
            }.apply()
        }
    
    /**
     * Store user ID for caching purposes
     */
    var userId: String?
        get() = encryptedPrefs.getString(KEY_USER_ID, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_USER_ID, value)
                } else {
                    remove(KEY_USER_ID)
                }
            }.apply()
        }
    
    /**
     * Store cached user summary (for offline access)
     */
    var cachedUserSummary: String?
        get() = encryptedPrefs.getString(KEY_CACHED_USER, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_CACHED_USER, value)
                } else {
                    remove(KEY_CACHED_USER)
                }
            }.apply()
        }
    
    /**
     * Store last checkout session ID for resuming after app relaunch
     */
    var lastCheckoutSessionId: String?
        get() = encryptedPrefs.getString(KEY_CHECKOUT_SESSION_ID, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_CHECKOUT_SESSION_ID, value)
                } else {
                    remove(KEY_CHECKOUT_SESSION_ID)
                }
            }.apply()
        }
    
    /**
     * Store last checkout booking type
     */
    var lastCheckoutBookingType: String?
        get() = encryptedPrefs.getString(KEY_CHECKOUT_BOOKING_TYPE, null)
        set(value) {
            encryptedPrefs.edit().apply {
                if (value != null) {
                    putString(KEY_CHECKOUT_BOOKING_TYPE, value)
                } else {
                    remove(KEY_CHECKOUT_BOOKING_TYPE)
                }
            }.apply()
        }
    
    /**
     * Check if user has tokens stored (considered authenticated)
     */
    fun hasTokens(): Boolean = !accessToken.isNullOrBlank()
    
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
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    
    /**
     * Clear all tokens (sign out)
     */
    fun clearAll() {
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
    }
    
    /**
     * Clear only backend tokens (keep Auth0 tokens for re-auth)
     */
    fun clearBackendTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }
    
    // TokenProvider interface implementation
    override fun getAccessToken(): String? = accessToken
    override fun getRefreshToken(): String? = refreshToken
    
    companion object {
        private const val PREFS_NAME = "pacedream_secure_prefs"
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


