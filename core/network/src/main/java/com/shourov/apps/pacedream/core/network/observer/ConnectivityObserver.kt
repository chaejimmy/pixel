package com.shourov.apps.pacedream.core.network.observer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionState {
    /** Network is up and validated (has internet access). */
    Available,

    /** Network is connected but not yet validated, or capabilities are degraded. */
    Losing,

    /** No network is available, or it was just lost. */
    Unavailable,
}

interface ConnectivityObserver {
    val state: StateFlow<ConnectionState>
}

/**
 * Observes [ConnectivityManager] callbacks and exposes the resulting connection
 * state as a hot [StateFlow] so UI code can render an offline banner without
 * managing the callback lifecycle. Registered against an [InternetCapability]
 * network request so we only flip to [ConnectionState.Available] for networks
 * that can actually reach the public internet (not e.g. captive-portal Wi-Fi).
 */
@Singleton
class AndroidConnectivityObserver @Inject constructor(
    context: Context,
) : ConnectivityObserver {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val state: StateFlow<ConnectionState> = networkStateFlow()
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = currentState(),
        )

    private fun currentState(): ConnectionState {
        val cm = connectivityManager ?: return ConnectionState.Unavailable
        val active = cm.activeNetwork ?: return ConnectionState.Unavailable
        val caps = cm.getNetworkCapabilities(active) ?: return ConnectionState.Unavailable
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            ConnectionState.Available
        } else {
            ConnectionState.Losing
        }
    }

    private fun networkStateFlow(): Flow<ConnectionState> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(ConnectionState.Unavailable)
            awaitClose { /* nothing to unregister */ }
            return@callbackFlow
        }

        // Re-emit the current state on collection start so a late subscriber
        // sees the connection status without waiting for the next callback.
        trySend(currentState())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentState())
            }

            override fun onLost(network: Network) {
                trySend(ConnectionState.Unavailable)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(ConnectionState.Losing)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(currentState())
            }

            override fun onUnavailable() {
                trySend(ConnectionState.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}
