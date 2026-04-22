package com.shourov.apps.pacedream.feature.wifi.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * Final-state modal. Server has confirmed the session is over. We offer a
 * grace-period reconnect when allowed; otherwise the user dismisses and re-
 * books from the booking flow.
 */
@Composable
internal fun WifiExpiredModal(
    state: WifiSessionUiState,
    onReconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Wi-Fi session ended",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary
            )
        },
        text = {
            val body = if (state.canExtend) {
                "Your Wi-Fi access just ran out. Tap reconnect to restore access for the grace period."
            } else {
                "Your Wi-Fi access ended. Book again from your bookings to get a new session."
            }
            Text(
                text = body,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
        },
        confirmButton = {
            if (state.canExtend) {
                TextButton(
                    onClick = onReconnect,
                    enabled = !state.extensionInProgress
                ) { Text("Reconnect") }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (state.canExtend) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    )
}
