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
 */
@Singleton
class HostModeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val _isHostMode = MutableStateFlow(loadHostMode())
    val isHostMode: StateFlow<Boolean> = _isHostMode.asStateFlow()
    
    private val _isHostVerified = MutableStateFlow(loadHostVerified())
    val isHostVerified: StateFlow<Boolean> = _isHostVerified.asStateFlow()
    
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
     * Clear all host mode data (for logout)
     */
    fun clearHostModeData() {
        _isHostMode.value = false
        _isHostVerified.value = false
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_NAME = "host_mode_prefs"
        private const val KEY_HOST_MODE = "is_host_mode"
        private const val KEY_HOST_VERIFIED = "is_host_verified"
    }
}
