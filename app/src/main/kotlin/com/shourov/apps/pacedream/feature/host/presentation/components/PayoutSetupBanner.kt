package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

/**
 * Persistent banner shown at the top of the wizard when the host has
 * not completed Stripe Connect payout onboarding.  The host can still
 * fill the form — publishing is gated separately via
 * [PayoutRequiredDialog] — but the banner sets expectations up front.
 */
@Composable
fun PayoutSetupBanner(
    onSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.Warning.copy(alpha = 0.12f))
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.Warning,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Set up payouts before publishing",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "You can fill in your listing now, but publishing is blocked until your Stripe account is active.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        }
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        Button(
            onClick = onSetup,
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Warning,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.XS,
            ),
        ) {
            Text("Set up", style = PaceDreamTypography.Button)
        }
    }
}

/**
 * Dialog shown when publish was blocked because Stripe payouts are
 * not enabled yet.  Replaces the old "red inline text with no CTA"
 * UX — hosts now have a clear single-tap path to Payment Setup.
 */
@Composable
fun PayoutRequiredDialog(
    message: String,
    onSetup: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = PaceDreamIcons.CreditCard,
                contentDescription = null,
                tint = PaceDreamColors.Warning,
            )
        },
        title = { Text("Payouts not set up") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onSetup) { Text("Go to payout setup") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

/**
 * Confirmation shown when the host presses system back on a dirty
 * wizard.  Autosave has already kept the draft on disk; the dialog
 * only asks the host whether to keep or discard it.
 */
@Composable
fun DiscardChangesDialog(
    onKeepDraft: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save your draft?") },
        text = {
            Text(
                "Keep your progress and come back later, or discard it now. " +
                    "You can only have one draft at a time.",
            )
        },
        confirmButton = {
            TextButton(onClick = onKeepDraft) { Text("Keep draft") }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard", color = PaceDreamColors.Error)
            }
        },
    )
}
