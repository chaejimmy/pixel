package com.pacedream.app.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
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
 *
 * EncryptedSharedPreferences is eagerly initialised on a background thread
 * during construction. The [prefs] getter will:
 *   - Return immediately if init has already completed.
 *   - Block up to 10 s on background / IO threads (normal path — auth init
 *     runs from `Dispatchers.IO` in `PaceDreamApplication.onCreate`).
 *   - **Never block** the main thread, and return `null` if the encrypted
 *     prefs are not ready yet. Callers treat a null return as "not
 *     initialised yet" and must tolerate it.
 *
 * Pre-init writes are NOT silently dropped. A write performed before the
 * encrypted prefs are ready is queued in [pendingOps] and replayed in
 * order when the init thread completes (under [opsLock] so a write that
 * arrives during drain does not race ahead of queued writes). Pre-init
 * reads on a thread that cannot block also consult the in-memory mirror
 * [pendingValues] so a token written and then read in the same cold-start
 * window is visible.
 *
 * We deliberately do NOT fall back to a plaintext SharedPreferences file:
 * writing auth tokens to a separate unencrypted file would both (a) leak
 * tokens at rest and (b) cause ghost logouts when the fallback file is
 * empty but the encrypted file is populated. This structure still prevents
 * the ANR that was caused by MasterKey / KeyStore IPC blocking the main
 * thread on some devices and emulators.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefsRef = AtomicReference<SharedPreferences?>()
    private val prefsLatch = CountDownLatch(1)

    // Pre-init write queue. Operations are enqueued (in order) when a write
    // arrives before encrypted prefs are ready, then replayed under
    // [opsLock] when init finishes. Direct writes that arrive after prefs
    // are ready also acquire [opsLock] briefly so they cannot interleave
    // ahead of queued operations during drain.
    private val opsLock = Any()
    private val pendingOps = ArrayList<(SharedPreferences) -> Unit>()
    // In-memory mirror of pre-init string writes, so a put-then-get in the
    // same cold-start window returns the queued value instead of null.
    // null in the map means a tombstone (the key was removed / cleared).
    private val pendingValues = HashMap<String, String?>()

    init {
        // Proactively delete any legacy plaintext fallback file that earlier
        // builds may have created. Defensive: any stale tokens that leaked
        // into it should be removed.
        try {
            context.deleteSharedPreferences(PREFS_NAME_FALLBACK)
        } catch (_: Exception) {
            // Best-effort; ignore.
        }

        Thread({
            val created = try {
                createEncryptedPrefsOrNull()
            } catch (e: Exception) {
                Timber.e(e, "EncryptedSharedPreferences init failed completely")
                null
            }
            // Publish prefs and drain the pending-op queue under the same
            // lock new writes will take, so wall-clock-later writes that
            // bypass the queue cannot land before queued writes do.
            synchronized(opsLock) {
                prefsRef.set(created)
                if (created != null && pendingOps.isNotEmpty()) {
                    val drained = pendingOps.size
                    pendingOps.forEach { op ->
                        try {
                            op(created)
                        } catch (e: Exception) {
                            Timber.e(e, "TokenStorage: queued op failed during drain")
                        }
                    }
                    pendingOps.clear()
                    pendingValues.clear()
                    Timber.d("TokenStorage: drained $drained queued op(s) after init")
                } else if (created == null && pendingOps.isNotEmpty()) {
                    Timber.e("TokenStorage: ${pendingOps.size} queued op(s) lost — encrypted prefs unavailable")
                    pendingOps.clear()
                    pendingValues.clear()
                }
            }
            prefsLatch.countDown()
        }, "TokenStorage-init").start()
    }

    /**
     * Suspend-friendly readiness check. Returns true if encrypted prefs
     * finished initialising within [timeoutMs], false otherwise. Callers
     * that must guarantee a token write is durable before continuing
     * (e.g. a logout that has to invalidate state on disk) should await
     * this from a background dispatcher before issuing the write.
     */
    fun awaitReady(timeoutMs: Long = 10_000L): Boolean {
        return try {
            prefsLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    /**
     * Returns the encrypted SharedPreferences, or null if they are not
     * ready yet. Never returns a plaintext fallback file.
     */
    private val prefs: SharedPreferences?
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
            Timber.e(e, "Failed to create EncryptedSharedPreferences, deleting corrupt prefs and retrying")
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

    /**
     * Cached user summary JSON (for offline display).
     *
     * Reads from the shared canonical key [KEY_CACHED_USER_SHARED] first. If
     * missing, falls back to the legacy key [KEY_CACHED_USER_SUMMARY] once,
     * then migrates the value to the shared key. This collapses the two
     * TokenStorage classes (`pacedream.app.core.auth` and
     * `shourov.apps.pacedream.core.network.auth`) onto a single key so the
     * two `User` caches never drift.
     */
    var cachedUserSummary: String?
        get() {
            val shared = safeGetString(KEY_CACHED_USER_SHARED)
            if (!shared.isNullOrBlank()) return shared
            // Legacy fallback + one-time migration to the shared key.
            val legacy = safeGetString(KEY_CACHED_USER_SUMMARY)
            if (!legacy.isNullOrBlank()) {
                safePutString(KEY_CACHED_USER_SHARED, legacy)
                return legacy
            }
            return null
        }
        set(value) {
            // Always write to the shared key. Also mirror to the legacy key
            // so older code paths / the legacy TokenStorage keep working
            // until the code is fully removed.
            safePutString(KEY_CACHED_USER_SHARED, value)
            safePutString(KEY_CACHED_USER_SUMMARY, value)
        }

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
        get() = try {
            prefs?.getBoolean(KEY_IS_HOST_MODE, false) ?: false
        } catch (e: Exception) { Timber.e(e, "Failed to read isHostMode"); false }
        set(value) {
            withWritablePrefs(
                onReady = { p ->
                    try {
                        p.edit().putBoolean(KEY_IS_HOST_MODE, value).apply()
                    } catch (e: Exception) { Timber.e(e, "Failed to write isHostMode") }
                },
                enqueue = {
                    pendingOps += { p ->
                        try { p.edit().putBoolean(KEY_IS_HOST_MODE, value).apply() }
                        catch (e: Exception) { Timber.e(e, "Failed to replay isHostMode write") }
                    }
                }
            )
        }

    private fun safeGetString(key: String): String? {
        // Fast path: prefs ready (no lock needed for read of an immutable
        // SharedPreferences reference).
        prefsRef.get()?.let { p ->
            return try { p.getString(key, null) }
            catch (e: Exception) { Timber.e(e, "Failed to read key: $key"); null }
        }
        // Pre-init fallback: consult the in-memory mirror so a value that
        // was written in this same cold-start window is still visible.
        synchronized(opsLock) {
            // Re-check inside the lock in case init completed between the
            // fast-path read and the lock acquisition.
            prefsRef.get()?.let { p ->
                return try { p.getString(key, null) }
                catch (e: Exception) { Timber.e(e, "Failed to read key: $key"); null }
            }
            if (pendingValues.containsKey(key)) return pendingValues[key]
            // No queued write for this key; if the caller is on a background
            // thread, fall through to prefs (which will block on the latch).
            // On the main thread, prefs returns null without blocking.
        }
        return try {
            prefs?.getString(key, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to read key: $key from encrypted prefs")
            null
        }
    }

    private fun safePutString(key: String, value: String?) {
        withWritablePrefs(
            onReady = { p ->
                try { p.edit().putString(key, value).apply() }
                catch (e: Exception) { Timber.e(e, "Failed to write key: $key") }
            },
            enqueue = {
                pendingValues[key] = value
                pendingOps += { p ->
                    try { p.edit().putString(key, value).apply() }
                    catch (e: Exception) { Timber.e(e, "Failed to replay write for $key") }
                }
            }
        )
    }

    /**
     * Acquires [opsLock] briefly. If encrypted prefs are ready, runs
     * [onReady] with the prefs (the caller writes through). Otherwise
     * runs [enqueue] which is expected to mutate [pendingOps] and
     * [pendingValues] for later replay. The lock is what keeps
     * post-init direct writes from interleaving ahead of pre-init
     * queued writes during drain.
     */
    private inline fun withWritablePrefs(
        onReady: (SharedPreferences) -> Unit,
        enqueue: () -> Unit,
    ) {
        synchronized(opsLock) {
            val ready = prefsRef.get()
            if (ready != null) {
                onReady(ready)
            } else {
                enqueue()
            }
        }
    }

    /**
     * Check if tokens exist.
     *
     * Returns false only when the encrypted prefs are initialised and empty.
     * When prefs are not yet initialised on the main thread, this returns
     * false, but callers (e.g. `SessionManager.initialize()`) always run
     * from `Dispatchers.IO` so they block on the init latch instead of
     * hitting this path.
     */
    fun hasTokens(): Boolean = try { !accessToken.isNullOrBlank() } catch (e: Exception) { Timber.e(e, "Failed to check tokens"); false }

    /**
     * Store backend tokens. Pre-init writes are queued and replayed when
     * encrypted prefs become available, so a token written during cold
     * start is never silently dropped.
     */
    fun storeTokens(accessToken: String?, refreshToken: String?) {
        withWritablePrefs(
            onReady = { p ->
                try {
                    p.edit()
                        .putString(KEY_ACCESS_TOKEN, accessToken)
                        .putString(KEY_REFRESH_TOKEN, refreshToken)
                        .apply()
                    Timber.d("Tokens stored")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to store tokens")
                }
            },
            enqueue = {
                pendingValues[KEY_ACCESS_TOKEN] = accessToken
                pendingValues[KEY_REFRESH_TOKEN] = refreshToken
                pendingOps += { p ->
                    try {
                        p.edit()
                            .putString(KEY_ACCESS_TOKEN, accessToken)
                            .putString(KEY_REFRESH_TOKEN, refreshToken)
                            .apply()
                        Timber.d("Tokens stored (queued -> replayed)")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to replay storeTokens")
                    }
                }
            }
        )
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
     * Clear all tokens and user data. Pre-init clears are queued so a
     * logout issued during cold start still takes effect.
     */
    fun clearAll() {
        withWritablePrefs(
            onReady = { p ->
                try {
                    p.edit()
                        .remove(KEY_ACCESS_TOKEN)
                        .remove(KEY_REFRESH_TOKEN)
                        .remove(KEY_AUTH0_ACCESS_TOKEN)
                        .remove(KEY_AUTH0_ID_TOKEN)
                        .remove(KEY_USER_ID)
                        .remove(KEY_CACHED_USER_SUMMARY)
                        .remove(KEY_CACHED_USER_SHARED)
                        .apply()
                    Timber.d("All tokens cleared")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to clear tokens")
                }
            },
            enqueue = {
                // Tombstone every key in the in-memory mirror so reads
                // during the pre-init window see the cleared state.
                listOf(
                    KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN,
                    KEY_AUTH0_ACCESS_TOKEN, KEY_AUTH0_ID_TOKEN,
                    KEY_USER_ID, KEY_CACHED_USER_SUMMARY, KEY_CACHED_USER_SHARED,
                ).forEach { pendingValues[it] = null }
                pendingOps += { p ->
                    try {
                        p.edit()
                            .remove(KEY_ACCESS_TOKEN)
                            .remove(KEY_REFRESH_TOKEN)
                            .remove(KEY_AUTH0_ACCESS_TOKEN)
                            .remove(KEY_AUTH0_ID_TOKEN)
                            .remove(KEY_USER_ID)
                            .remove(KEY_CACHED_USER_SUMMARY)
                            .remove(KEY_CACHED_USER_SHARED)
                            .apply()
                        Timber.d("All tokens cleared (queued -> replayed)")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to replay clearAll")
                    }
                }
            }
        )
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
        /** Legacy key kept for one-time migration. */
        private const val KEY_CACHED_USER_SUMMARY = "cached_user_summary"
        /**
         * Canonical cached-user key, shared with the legacy
         * `core/network/.../auth/TokenStorage.kt` TokenStorage so that the
         * two user caches stay in sync.
         */
        private const val KEY_CACHED_USER_SHARED = "cached_user"
        private const val KEY_LAST_CHECKOUT_SESSION_ID = "last_checkout_session_id"
        private const val KEY_LAST_CHECKOUT_BOOKING_TYPE = "last_checkout_booking_type"
        private const val KEY_IS_HOST_MODE = "is_host_mode"
    }
}
