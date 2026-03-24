package com.shourov.apps.pacedream.feature.host.domain

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Guest/Host mode with persistence to SharedPreferences.
 * This matches the iOS behavior for mode switching.
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
     * Load host mode from SharedPreferences on startup
     */
    private fun loadHostMode(): Boolean {
        return prefs.getBoolean(KEY_HOST_MODE, false)
    }

    /**
     * Load host verification status from SharedPreferences
     */
    private fun loadHostVerified(): Boolean {
        return prefs.getBoolean(KEY_HOST_VERIFIED, false)
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
     * Clear all host mode data (for logout).
     * iOS parity: Allow sign out directly from host mode without
     * requiring the user to switch to guest mode first.
     */
    fun clearHostModeData() {
        _isHostMode.value = false
        _isHostVerified.value = false
        _pendingHostRoute.value = null
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
    }
}
