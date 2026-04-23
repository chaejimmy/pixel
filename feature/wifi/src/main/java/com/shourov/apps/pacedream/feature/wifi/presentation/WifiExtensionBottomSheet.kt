@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wifi.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pacedream.common.composables.components.PaceDreamBottomSheet
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.feature.wifi.util.WifiTime

/**
 * Auto-presents at the 3-min critical threshold and on demand from the pill.
 * The two primary CTAs are wide and side-by-side — this is the highest-
 * converting moment for extensions, so it gets the strongest UI treatment.
 */
@Composable
internal fun WifiExtensionBottomSheet(
    state: WifiSessionUiState,
    onExtend: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    PaceDreamBottomSheet(
        onDismiss = onDismiss,
        title = "Extend Wi-Fi?"
    ) {
        Text(
            text = "${WifiTime.formatRemaining(state.secondsRemaining)} remaining",
            style = PaceDreamTypography.Title2,
            color = if (state.phase == WifiSessionUiState.Phase.Critical)
                PaceDreamColors.Error else PaceDreamColors.TextPrimary
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Add more time so you don't get disconnected.",
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary
        )
        Spacer(Modifier.height(PaceDreamSpacing.LG))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Button(
                onClick = { onExtend(30) },
                enabled = !state.extensionInProgress && state.canExtend,
                modifier = Modifier.weight(1f)
            ) { Text("+30 min") }
            Button(
                onClick = { onExtend(60) },
                enabled = !state.extensionInProgress && state.canExtend,
                modifier = Modifier.weight(1f)
            ) { Text("+60 min") }
        }
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        OutlinedButton(
            onClick = { onExtend(15) },
            enabled = !state.extensionInProgress && state.canExtend,
            modifier = Modifier.fillMaxWidth()
        ) { Text("+15 min") }

        Spacer(Modifier.height(PaceDreamSpacing.SM))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Let it expire") }

        state.errorMessage?.let { error ->
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = error,
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.Error
            )
        }
    }
}
