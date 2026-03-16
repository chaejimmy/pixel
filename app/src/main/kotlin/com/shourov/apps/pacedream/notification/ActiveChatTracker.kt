package com.shourov.apps.pacedream.notification

import java.util.concurrent.atomic.AtomicReference

/**
 * Tracks the currently visible chat thread so foreground notifications
 * can be suppressed when the user is actively reading the same conversation.
 *
 * iOS parity: mirrors ActiveChatTracker in AppDelegate.swift.
 *
 * Usage:
 * - Chat screen calls [set] with the threadId on onResume/onAppear.
 * - Chat screen calls [clear] on onPause/onDisappear.
 * - [PaceDreamNotificationService.showNotification] checks [shouldSuppress]
 *   before displaying a message notification.
 */
object ActiveChatTracker {
    private val currentThreadId = AtomicReference<String?>(null)

    fun set(threadId: String) {
        currentThreadId.set(threadId)
    }

    fun clear() {
        currentThreadId.set(null)
    }

    /**
     * Returns true if the given thread is currently being viewed,
     * meaning we should suppress the notification banner (iOS parity).
     */
    fun shouldSuppress(threadId: String?): Boolean {
        if (threadId.isNullOrBlank()) return false
        val active = currentThreadId.get() ?: return false
        return active == threadId
    }
}
