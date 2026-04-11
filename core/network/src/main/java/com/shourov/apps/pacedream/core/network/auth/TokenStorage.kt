package com.shourov.apps.pacedream.core.network.auth

import android.content.Context
import android.os.Looper
import kotlin.jvm.JvmName
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences
 * Matches iOS Keychain-based token storage pattern
 *
 * EncryptedSharedPreferences is eagerly initialised on a background thread.
 * The getter never blocks the main thread — it returns a plain-text fallback
 * (empty) if called before init completes, preventing ANR.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenProvider {

    private val prefsRef = AtomicReference<SharedPreferences?>()
    private val prefsLatch = CountDownLatch(1)

    init {
        // Defensively remove any legacy plaintext fallback file that earlier
        // builds may have created. We no longer use a plaintext fallback
        // because it caused ghost logouts (and could leak tokens at rest).
        try {
            context.deleteSharedPreferences(PREFS_NAME_FALLBACK)
        } catch (_: Exception) {
            // Best-effort; ignore.
        }

        Thread({
            try {
                prefsRef.set(createEncryptedPrefsOrNull())
            } catch (e: Exception) {
                Timber.e(e, "EncryptedSharedPreferences init failed completely")
                prefsRef.set(null)
            } finally {
                prefsLatch.countDown()
            }
        }, "TokenStorage-init").start()
    }

    /**
     * Returns the encrypted SharedPreferences, or null if they are not ready
     * yet. Never returns a plaintext fallback file (callers tolerate null).
     */
    private val encryptedPrefs: SharedPreferences?
        get() {
            // Fast path: already initialised.
            prefsRef.get()?.let { return it }

            // On the main thread, never block.
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Timber.w("TokenStorage: main-thread access before encrypted prefs ready — returning null")
                return null
            }

            // Background / IO thread: wait for the init thread to finish.
            if (!prefsLatch.await(10, TimeUnit.SECONDS)) {
                Timber.e("TokenStorage: encrypted prefs init timed out after 10 s — returning null")
                return null
            }
            return prefsRef.get()
        }

    private fun createEncryptedPrefsOrNull(): SharedPreferences? {
        return try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Timber.e(e, "Failed to create encrypted prefs, deleting corrupt prefs and retrying")
            try { context.deleteSharedPreferences(PREFS_NAME) } catch (_: Exception) {}
            try { clearMasterKeyFromKeyStore() } catch (_: Exception) {}
            try {
                createEncryptedPrefs()
            } catch (retryException: Exception) {
                Timber.e(retryException, "EncryptedSharedPreferences retry also failed; no plaintext fallback")
                null
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
            encryptedPrefs?.getString(key, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to read key: $key from encrypted prefs")
            null
        }
    }

    private fun safePutString(key: String, value: String?) {
        try {
            val p = encryptedPrefs
            if (p == null) {
                Timber.w("Dropping write for $key: encrypted prefs not ready")
                return
            }
            p.edit().apply {
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
            val p = encryptedPrefs
            if (p == null) {
                Timber.w("Dropping storeTokens: encrypted prefs not ready")
                return
            }
            p.edit().apply {
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
            val p = encryptedPrefs
            if (p == null) {
                Timber.w("Dropping clearAll: encrypted prefs not ready")
                return
            }
            p.edit()
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
            val p = encryptedPrefs
            if (p == null) {
                Timber.w("Dropping clearBackendTokens: encrypted prefs not ready")
                return
            }
            p.edit()
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
