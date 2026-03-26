package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.runtime.Composable

/**
 * Host Inbox Screen — thin wrapper that delegates to the shared InboxScreen.
 *
 * Previously this added a duplicate TopAppBar and a redundant
 * Messages/Notifications segmented control on top of the embedded
 * InboxScreen (which already had its own title bar), causing a
 * confusing double-header. Now the host inbox is simply the shared
 * messages screen — the InboxViewModel already loads host-mode
 * threads based on the current mode.
 */
@Composable
fun HostInboxScreen(
    onThreadClick: (String) -> Unit = {},
    messagesContent: @Composable () -> Unit = {}
) {
    // Render the shared inbox content directly — no extra Scaffold or tabs.
    messagesContent()
}
