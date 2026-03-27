package com.pacedream.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Location Service for getting current location and geocoding
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient? by lazy {
        try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize FusedLocationProviderClient")
            null
        }
    }

    private val geocoder: Geocoder? by lazy {
        try {
            Geocoder(context, Locale.getDefault())
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Geocoder")
            null
        }
    }
    
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get current location
     * Returns null if permission not granted or location unavailable
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        val client = fusedLocationClient ?: return null

        return try {
            val cancellationToken = CancellationTokenSource()
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()
        } catch (e: SecurityException) {
            Timber.w(e, "Location permission revoked while getting location")
            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to get current location")
            null
        }
    }

    /**
     * Get address from location coordinates (reverse geocoding)
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        val geo = geocoder ?: return null

        return try {
            val addresses = geo.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                // Format: City, State or full address
                val city = address.locality
                val state = address.adminArea
                val country = address.countryName

                when {
                    !city.isNullOrBlank() && !state.isNullOrBlank() -> "$city, $state"
                    !city.isNullOrBlank() -> city
                    !state.isNullOrBlank() -> state
                    !country.isNullOrBlank() -> country
                    else -> address.getAddressLine(0) // Full address as fallback
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get address from location")
            null
        }
    }

    /**
     * Get coordinates from address (forward geocoding)
     */
    suspend fun getLocationFromAddress(address: String): Pair<Double, Double>? {
        val geo = geocoder ?: return null

        return try {
            val addresses = geo.getFromLocationName(address, 1)
            addresses?.firstOrNull()?.let { addr ->
                Pair(addr.latitude, addr.longitude)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get location from address")
            null
        }
    }
}
