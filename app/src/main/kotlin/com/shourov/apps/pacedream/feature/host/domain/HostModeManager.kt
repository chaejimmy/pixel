package com.shourov.apps.pacedream.feature.host.domain

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Guest/Host mode with persistence to SharedPreferences.
 * This matches the iOS AppModeStore behavior for mode switching.
 *
 * iOS parity: The source of truth for *whether the user is a host*
 * comes from the backend (/account/me → isHost / superHost / properties).
 * The local mode preference only controls *which view* the user is
 * currently seeing (guest dashboard vs host dashboard).
 *
 * On profile load, [syncWithBackendHostStatus] reconciles the local
 * preference with the backend state so that users who are already hosts
 * are never shown the "Start Hosting" CTA incorrectly.
 *
 * iOS parity: supports PendingHostRoute so that unauthenticated users
 * can tap "Create a listing" and be routed to Host → Post → Create Listing
 * after login completes (see iOS AppModeStore.setPendingHostRoute).
 */
@Singleton
class HostModeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    /**
     * iOS parity: mirrors iOS PendingHostRoute enum.
     * Stores the intended destination after switching to host mode.
     */
    enum class PendingHostRoute {
        /** Navigate to host listings / dashboard */
        LISTINGS,
        /** Navigate to Post tab and auto-open the Create Listing wizard */
        POST_CREATE_LISTING,
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isHostMode = MutableStateFlow(loadHostMode())
    val isHostMode: StateFlow<Boolean> = _isHostMode.asStateFlow()

    private val _isHostVerified = MutableStateFlow(loadHostVerified())
    val isHostVerified: StateFlow<Boolean> = _isHostVerified.asStateFlow()

    private val _pendingHostRoute = MutableStateFlow<PendingHostRoute?>(null)
    val pendingHostRoute: StateFlow<PendingHostRoute?> = _pendingHostRoute.asStateFlow()

    /**
     * Whether the backend has confirmed this user is a host
     * (has properties, is superHost, or has isHost flag).
     * This is NOT the same as isHostMode (which controls the current view).
     */
    private val _isBackendHost = MutableStateFlow(loadBackendHost())
    val isBackendHost: StateFlow<Boolean> = _isBackendHost.asStateFlow()

    private fun loadHostMode(): Boolean {
        val value = prefs.getBoolean(KEY_HOST_MODE, false)
        Timber.d("[HostMode] loadHostMode=$value")
        return value
    }

    private fun loadHostVerified(): Boolean {
        val value = prefs.getBoolean(KEY_HOST_VERIFIED, false)
        Timber.d("[HostMode] loadHostVerified=$value")
        return value
    }

    private fun loadBackendHost(): Boolean {
        return prefs.getBoolean(KEY_BACKEND_HOST, false)
    }

    /**
     * Toggle between Guest and Host modes
     */
    fun toggleHostMode() {
        setHostMode(!_isHostMode.value)
    }

    /**
     * Set the mode and persist to SharedPreferences
     */
    fun setHostMode(enabled: Boolean) {
        Timber.d("[HostMode] setHostMode: $enabled (was ${_isHostMode.value})")
        _isHostMode.value = enabled
        prefs.edit().putBoolean(KEY_HOST_MODE, enabled).apply()
    }

    /**
     * iOS parity: switchToHost(route:) — switch to host mode and store a pending route.
     */
    fun switchToHost(route: PendingHostRoute? = null) {
        _pendingHostRoute.value = route
        setHostMode(true)
    }

    /**
     * iOS parity: setPendingHostRoute — store intent without switching mode.
     * Used before auth when the user isn't logged in yet.
     */
    fun setPendingHostRoute(route: PendingHostRoute) {
        _pendingHostRoute.value = route
    }

    /**
     * iOS parity: consumePendingHostRoute — return and clear the pending route.
     */
    fun consumePendingHostRoute(): PendingHostRoute? {
        val route = _pendingHostRoute.value
        _pendingHostRoute.value = null
        return route
    }

    /**
     * iOS parity: switchToGuest — clear pending route and switch to guest mode.
     */
    fun switchToGuest() {
        _pendingHostRoute.value = null
        setHostMode(false)
    }

    /**
     * Set host verification status
     */
    fun setHostVerified(verified: Boolean) {
        Timber.d("[HostMode] setHostVerified: $verified (was ${_isHostVerified.value})")
        _isHostVerified.value = verified
        prefs.edit().putBoolean(KEY_HOST_VERIFIED, verified).apply()
    }

    /**
     * Check if user can switch to host mode
     */
    fun canSwitchToHostMode(): Boolean {
        return _isHostVerified.value
    }

    /**
     * iOS parity: Sync local host state with backend user profile.
     *
     * Called after /account/me is parsed. If the backend says the user
     * is already a host (has listings, superHost, or isHost flag), we:
     *  1. Mark isHostVerified = true so they can switch freely.
     *  2. Persist the backend host status for future sessions.
     *  3. If the user was NEVER in host mode before on this device
     *     AND the backend says they're a host, auto-enable host mode
     *     so they land in the correct view.
     *
     * This prevents the bug where a returning host sees "Start Hosting"
     * because the local pref was never set on this device.
     */
    fun syncWithBackendHostStatus(isHost: Boolean) {
        val previousBackendHost = _isBackendHost.value
        _isBackendHost.value = isHost
        prefs.edit().putBoolean(KEY_BACKEND_HOST, isHost).apply()

        Timber.d("[HostMode] syncWithBackendHostStatus: isHost=$isHost, previousBackendHost=$previousBackendHost, currentMode=${_isHostMode.value}, verified=${_isHostVerified.value}")

        if (isHost) {
            // User is a host on the backend — mark as verified
            if (!_isHostVerified.value) {
                Timber.d("[HostMode] Backend says user is host → setting verified=true")
                setHostVerified(true)
            }

            // Auto-enable host mode if we haven't seen this user as a host before
            // on this device and they're not currently in host mode already.
            // This handles the fresh-install / cleared-data case.
            if (!previousBackendHost && !_isHostMode.value && !prefs.contains(KEY_HOST_MODE)) {
                Timber.d("[HostMode] First time recognizing backend host → auto-enabling host mode")
                setHostMode(true)
            }
        }
    }

    /**
     * Clear all host mode data (for logout).
     * iOS parity: Allow sign out directly from host mode without
     * requiring the user to switch to guest mode first.
     */
    fun clearHostModeData() {
        Timber.d("[HostMode] clearHostModeData")
        _isHostMode.value = false
        _isHostVerified.value = false
        _pendingHostRoute.value = null
        _isBackendHost.value = false
        prefs.edit().clear().apply()
    }

    /**
     * iOS parity: Sign out directly from host mode.
     * Clears host state and resets to guest mode in one call.
     * This matches iOS behavior from commit b43a469 which allows
     * signing out directly from host mode.
     */
    fun signOutFromHostMode() {
        clearHostModeData()
    }

    companion object {
        private const val PREFS_NAME = "host_mode_prefs"
        private const val KEY_HOST_MODE = "is_host_mode"
        private const val KEY_HOST_VERIFIED = "is_host_verified"
        private const val KEY_BACKEND_HOST = "is_backend_host"
    }
}
