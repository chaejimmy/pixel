package com.shourov.apps.pacedream.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * NavigationRouter - iOS-parity "navigate to route" events for push notifications.
 *
 * Complements [TabRouter] which handles tab switching. This router handles
 * in-tab navigation to specific screens (e.g., booking detail, chat thread).
 *
 * Dashboard listens and calls navController.navigate() accordingly.
 */
object NavigationRouter {
    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val events: SharedFlow<String> = _events.asSharedFlow()

    fun navigateTo(route: String) {
        _events.tryEmit(route)
    }
}
