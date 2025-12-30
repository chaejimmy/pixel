package com.shourov.apps.pacedream.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * TabRouter - iOS-parity "switch to tab" events.
 *
 * Any screen can call [switchTo] to request a tab switch (similar to iOS NotificationCenter).
 * Dashboard listens and navigates the tab NavController accordingly.
 */
object TabRouter {
    private val _events = MutableSharedFlow<DashboardDestination>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val events: SharedFlow<DashboardDestination> = _events.asSharedFlow()

    fun switchTo(destination: DashboardDestination) {
        _events.tryEmit(destination)
    }
}

