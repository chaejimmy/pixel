package com.shourov.apps.pacedream.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * In-app pre-prompt for the POST_NOTIFICATIONS runtime permission on Android
 * 13+. Shown once after the user lands on the main shell for the first time
 * — never on auth screens. Tapping "Enable notifications" should fire the
 * system [android.Manifest.permission.POST_NOTIFICATIONS] request through
 * [androidx.activity.compose.rememberLauncherForActivityResult]. Tapping
 * "Not now" dismisses without surfacing the OS dialog so the user keeps the
 * option to grant later from Settings.
 *
 * The primer is purely presentational; gating ("show once", SDK version,
 * already granted) lives in the caller.
 */
@Composable
fun NotificationPermissionPrimer(
    onEnableClick: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Stay in the loop",
    body: String = "Get booking updates, host messages, and payment confirmations.",
    enableLabel: String = "Enable notifications",
    dismissLabel: String = "Not now",
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            // Tapping outside should NOT silently consume the primer — the
            // user gets one shot at this per device so we require an explicit
            // choice between Enable and Not now.
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(PaceDreamRadius.XL),
            color = PaceDreamColors.Card,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        PaddingValues(
                            horizontal = PaceDreamSpacing.LG,
                            vertical = PaceDreamSpacing.LG,
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                        .background(PaceDreamColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                Text(
                    text = title,
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                Text(
                    text = body,
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                Button(
                    onClick = onEnableClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary,
                        contentColor = PaceDreamColors.OnPrimary,
                    ),
                ) {
                    Text(
                        text = enableLabel,
                        style = PaceDreamTypography.Button,
                    )
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = dismissLabel,
                        style = PaceDreamTypography.Button,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Secondary fallback prompt shown after the user denies the system dialog
 * twice (i.e. `shouldShowRequestPermissionRationale` returns `false` and the
 * permission is still not granted). Routes the user to App Settings →
 * Notifications via [onOpenSettings].
 */
@Composable
fun NotificationPermissionSettingsFallback(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Notifications are turned off",
    body: String = "To get booking updates, host messages, and payment confirmations, turn on notifications in Settings.",
    openSettingsLabel: String = "Open Settings",
    dismissLabel: String = "Not now",
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(PaceDreamRadius.XL),
            color = PaceDreamColors.Card,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        PaddingValues(
                            horizontal = PaceDreamSpacing.LG,
                            vertical = PaceDreamSpacing.LG,
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                        .background(PaceDreamColors.Warning.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = PaceDreamColors.Warning,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                Text(
                    text = title,
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                Text(
                    text = body,
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary,
                        contentColor = PaceDreamColors.OnPrimary,
                    ),
                ) {
                    Text(
                        text = openSettingsLabel,
                        style = PaceDreamTypography.Button,
                    )
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = dismissLabel,
                        style = PaceDreamTypography.Button,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
            }
        }
    }
}
