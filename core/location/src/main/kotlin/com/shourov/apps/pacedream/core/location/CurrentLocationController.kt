package com.shourov.apps.pacedream.core.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Compose state for the "Use Current Location" affordance.
 *
 * The controller never requests permission on app start — it only runs
 * when the user explicitly calls [CurrentLocationController.request].
 * Permanent denial is detected by checking
 * `shouldShowRequestPermissionRationale` after the permission result:
 * `false` on a denied permission means the user picked "don't ask again"
 * (or the system has otherwise locked further prompts), so the caller
 * should surface an "Open Settings" affordance.
 */
sealed interface CurrentLocationState {
    /** Initial / reset state. */
    data object Idle : CurrentLocationState

    /** Permission request shown but no answer yet, or device call in flight. */
    data object Loading : CurrentLocationState

    /** Reverse-geocoded place ready to use. */
    data class Resolved(val place: ReverseGeocodedPlace) : CurrentLocationState

    /** User declined the OS prompt this turn — manual entry still available. */
    data object PermissionDenied : CurrentLocationState

    /**
     * User has previously denied with "don't ask again" (or device policy
     * blocks the prompt). The OS will silently no-op future prompts, so
     * the UI must direct the user to Settings instead.
     */
    data object PermissionPermanentlyDenied : CurrentLocationState

    /** Permission granted but the device returned no fix (no GPS, airplane mode, etc.). */
    data object Unavailable : CurrentLocationState
}

@Stable
class CurrentLocationController internal constructor() {
    var state: CurrentLocationState by mutableStateOf(CurrentLocationState.Idle)
        internal set

    internal var requestHandler: () -> Unit = {}
    internal var openSettingsHandler: () -> Unit = {}

    /** Tap entry point — handles permission flow + geocoding internally. */
    fun request() = requestHandler()

    /** Opens the app's system settings page so the user can re-enable location. */
    fun openSettings() = openSettingsHandler()

    /** Reset the visible state (e.g. after the host UI dismisses a banner). */
    fun reset() {
        state = CurrentLocationState.Idle
    }

    val isLoading: Boolean get() = state is CurrentLocationState.Loading
}

/**
 * Composable factory for [CurrentLocationController]. Wires up the
 * permission launcher and Hilt EntryPoint lookups for [LocationService].
 *
 * @param onResolved called once with the reverse-geocoded place when the
 *  flow succeeds — host code typically pushes this into a search VM and
 *  also persists it via [LastLocationStore].
 */
@Composable
fun rememberCurrentLocationController(
    onResolved: (ReverseGeocodedPlace) -> Unit = {},
): CurrentLocationController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { CurrentLocationController() }

    val locationService = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LocationServiceEntryPoint::class.java,
        ).locationService()
    }

    fun resolveCurrentLocation() {
        controller.state = CurrentLocationState.Loading
        scope.launch {
            try {
                val location = locationService.getCurrentLocation()
                if (location == null) {
                    controller.state = CurrentLocationState.Unavailable
                    return@launch
                }
                val place = locationService.getPlaceFromLocation(location.latitude, location.longitude)
                    ?: ReverseGeocodedPlace(
                        lat = location.latitude,
                        lng = location.longitude,
                        city = "",
                        state = "",
                        country = "",
                        formattedAddress = "",
                    )
                controller.state = CurrentLocationState.Resolved(place)
                onResolved(place)
            } catch (e: Exception) {
                Timber.w(e, "resolveCurrentLocation_failed")
                controller.state = CurrentLocationState.Unavailable
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            resolveCurrentLocation()
        } else {
            controller.state = classifyDenial(context)
        }
    }

    SideEffect {
        controller.requestHandler = {
            if (locationService.hasLocationPermission()) {
                resolveCurrentLocation()
            } else {
                controller.state = CurrentLocationState.Loading
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
        controller.openSettingsHandler = { openAppSettings(context) }
    }

    return controller
}

/**
 * After a denied permission result, decide whether the user can be
 * re-prompted or whether we have to ship them to Settings.
 *
 * Android suppresses `shouldShowRequestPermissionRationale` once the
 * user has selected "Don't ask again" (and on first-ever launch, before
 * the prompt has been shown). To avoid the first-launch ambiguity we
 * only enter the permanent-denied branch when the call is made from an
 * Activity context — non-Activity contexts (previews / tests) degrade
 * to the soft-denied state and retry on the next tap.
 */
private fun classifyDenial(context: Context): CurrentLocationState {
    val activity = context as? Activity ?: return CurrentLocationState.PermissionDenied
    val canAskFine = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.ACCESS_FINE_LOCATION,
    )
    val canAskCoarse = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    return if (!canAskFine && !canAskCoarse) {
        CurrentLocationState.PermissionPermanentlyDenied
    } else {
        CurrentLocationState.PermissionDenied
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Timber.w(e, "open_app_settings_failed")
    }
}
