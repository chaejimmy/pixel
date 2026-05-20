package com.shourov.apps.pacedream.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pacedream.app.core.auth.AuthState
import com.shourov.apps.pacedream.designsystem.NotificationPermissionPrimer
import com.shourov.apps.pacedream.designsystem.NotificationPermissionSettingsFallback
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

/**
 * Exposes [NotificationPermissionPrimerStore] without forcing every call site
 * to be `@HiltViewModel` — this composable is called from inside a NavHost
 * `composable {}` block so we'd otherwise need a dedicated ViewModel just to
 * read one boolean.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationPermissionPrimerStoreEntryPoint {
    fun notificationPermissionPrimerStore(): NotificationPermissionPrimerStore
}

/**
 * Drives the POST_NOTIFICATIONS in-app primer flow once the user is on the
 * main shell (post-onboarding/login). Renders nothing on API < 33, on auth
 * screens, or when the permission has already been granted.
 *
 * Lifecycle:
 *  1. First time the user is authenticated AND lands on the host screen,
 *     show the in-app primer. Persist the "shown" flag so we never re-show.
 *  2. On "Enable notifications", launch the system permission dialog.
 *  3. If the user denies and the OS reports
 *     `shouldShowRequestPermissionRationale == false` (denied twice / "Don't
 *     ask again"), surface the Settings fallback that deep-links to App
 *     Notification Settings.
 *  4. On "Not now" the primer is dismissed and we do NOT show the system
 *     dialog — the user can re-enable from Settings → Notifications.
 *
 * The composable is keyed on [authState]; we only fire on the
 * [AuthState.Authenticated] branch.
 */
@Composable
fun NotificationPermissionGate(
    authState: AuthState,
) {
    // Pre-Android 13 has no runtime permission — skip the whole flow.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val activity = context as? Activity

    val store = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationPermissionPrimerStoreEntryPoint::class.java,
        ).notificationPermissionPrimerStore()
    }

    var showPrimer by rememberSaveable { mutableStateOf(false) }
    var showSettingsFallback by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Timber.d("[NotificationPermissionGate] POST_NOTIFICATIONS granted=$granted")
        if (!granted && activity != null) {
            // After a denial, `shouldShowRequestPermissionRationale` returns
            // true on the first deny and false on the second (or after the
            // user toggled "Don't ask again"). The latter is the only case
            // where the OS will silently swallow future requests, so that's
            // when we escalate to the Settings deep-link.
            val canAskAgain = activity.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (!canAskAgain) {
                showSettingsFallback = true
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState != AuthState.Authenticated) return@LaunchedEffect
        if (store.hasPrimerBeenShown()) return@LaunchedEffect
        if (isNotificationPermissionGranted(context)) {
            // Already granted on a prior install / OS-level grant — there's
            // nothing to ask for; persist the flag so we don't pop the primer
            // later if the user revokes and re-grants.
            store.markPrimerShown()
            return@LaunchedEffect
        }
        showPrimer = true
    }

    if (showPrimer) {
        NotificationPermissionPrimer(
            onEnableClick = {
                showPrimer = false
                store.markPrimerShown()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                showPrimer = false
                store.markPrimerShown()
            },
        )
    }

    if (showSettingsFallback) {
        NotificationPermissionSettingsFallback(
            onOpenSettings = {
                showSettingsFallback = false
                openAppNotificationSettings(context)
            },
            onDismiss = {
                showSettingsFallback = false
            },
        )
    }
}

/**
 * Returns true if POST_NOTIFICATIONS is granted (Android 13+) or if the OS
 * is too old to gate notifications behind a runtime permission. Safe to call
 * from any thread.
 */
internal fun isNotificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Deep-link into the system "App notifications" page for this app. Falls
 * back to the generic application details screen if the channel-aware
 * intent isn't resolvable (some OEM ROMs strip ACTION_APP_NOTIFICATION_SETTINGS).
 */
internal fun openAppNotificationSettings(context: Context) {
    val packageName = context.packageName
    val intents = buildList {
        // Preferred: jump straight to the per-app "Notifications" page so the
        // user is one tap away from flipping the toggle back on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        // Fallback: generic app info screen — always present on stock Android.
        add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            Timber.w(e, "[NotificationPermissionGate] Failed to launch %s", intent.action)
        }
    }
}
