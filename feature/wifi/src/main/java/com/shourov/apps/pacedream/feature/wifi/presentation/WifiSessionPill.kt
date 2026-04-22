package com.shourov.apps.pacedream.feature.wifi.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.feature.wifi.util.WifiTime

private val PillHeight = 36.dp

/**
 * Persistent Wi-Fi session pill. Lives in the app shell above the Scaffold so
 * it survives tab switches and intra-tab navigation.
 *
 * Color shifts at thresholds:
 * - Active (>15 min): success green
 * - Warning (≤15 min): amber + inline "Extend" chip
 * - Critical (≤3 min): red (the extension sheet auto-presents)
 * - Expired: hidden (the WifiExpiredModal owns that state)
 */
@Composable
internal fun WifiSessionPill(
    state: WifiSessionUiState,
    onTap: () -> Unit,
    onQuickExtend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = state.isVisible && state.phase != WifiSessionUiState.Phase.Expired
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        val container = pillContainerColor(state.phase)
        val onContainer = pillOnContainerColor(state.phase)
        val showQuickExtend = state.canExtend &&
            state.phase == WifiSessionUiState.Phase.Warning

        val totalSeconds = remember(state.expiresAt, state.phase) {
            // Used for the small progress bar — assume 60-min session as a
            // visual baseline so the bar drains reasonably across phases.
            // (Server can pass canonical totals later; MVP keeps this simple.)
            3600L
        }
        val progress = (state.secondsRemaining.toFloat() / totalSeconds.toFloat())
            .coerceIn(0f, 1f)

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(PillHeight)
                        .clip(RoundedCornerShape(PaceDreamRadius.Round))
                        .background(container)
                        .clickable { onTap() }
                        .padding(horizontal = PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Tiny dot acting as a glanceable status indicator.
                    Box(
                        modifier = Modifier
                            .size(PaceDreamSpacing.SM)
                            .clip(RoundedCornerShape(PaceDreamRadius.Round))
                            .background(onContainer)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        text = "Wi-Fi · ${WifiTime.formatRemaining(state.secondsRemaining)} left",
                        style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = onContainer
                    )
                    if (showQuickExtend) {
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        TextButton(
                            onClick = onQuickExtend,
                            contentPadding = PaddingValues(
                                horizontal = PaceDreamSpacing.SM,
                                vertical = 0.dp
                            )
                        ) {
                            Text(
                                text = "Extend",
                                style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.Bold),
                                color = onContainer
                            )
                        }
                    }
                }
            }

            // Hairline progress bar directly under the pill, full-width, so users
            // get a peripheral sense of drain without re-reading the timer.
            LinearProgressIndicator(
                progress = { progress },
                color = onContainer,
                trackColor = container.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}

private fun pillContainerColor(phase: WifiSessionUiState.Phase): Color = when (phase) {
    WifiSessionUiState.Phase.Critical -> PaceDreamColors.Error.copy(alpha = 0.16f)
    WifiSessionUiState.Phase.Warning -> PaceDreamColors.Warning.copy(alpha = 0.18f)
    WifiSessionUiState.Phase.Active -> PaceDreamColors.Success.copy(alpha = 0.16f)
    else -> PaceDreamColors.Gray200
}

private fun pillOnContainerColor(phase: WifiSessionUiState.Phase): Color = when (phase) {
    WifiSessionUiState.Phase.Critical -> PaceDreamColors.Error
    WifiSessionUiState.Phase.Warning -> PaceDreamColors.Warning
    WifiSessionUiState.Phase.Active -> PaceDreamColors.Success
    else -> PaceDreamColors.Gray700
}
