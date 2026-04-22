package com.shourov.apps.pacedream.feature.wifi.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.pacedream.common.composables.components.PaceDreamBottomSheet
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.feature.wifi.util.WifiTime

/**
 * Read-only Wi-Fi session detail sheet. Shown when the user taps the pill
 * outside the warning/critical window. Surfaces SSID, password (copy), and
 * remaining time with a pathway to the extend sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WifiSessionSheet(
    state: WifiSessionUiState,
    onDismiss: () -> Unit,
    onExtend: () -> Unit
) {
    val context = LocalContext.current
    PaceDreamBottomSheet(
        onDismiss = onDismiss,
        title = "Wi-Fi access"
    ) {
        Text(
            text = "${WifiTime.formatRemaining(state.secondsRemaining)} remaining",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(Modifier.height(PaceDreamSpacing.MD))

        state.ssid?.let { ssid ->
            CredentialRow(label = "Network", value = ssid) {
                copyToClipboard(context, "SSID", ssid)
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
        }
        state.password?.let { pwd ->
            CredentialRow(label = "Password", value = pwd) {
                copyToClipboard(context, "Wi-Fi password", pwd)
            }
            Spacer(Modifier.height(PaceDreamSpacing.MD))
        }

        if (state.canExtend) {
            Button(
                onClick = onExtend,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Extend session")
            }
        } else {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Close") }
        }
    }
}

@Composable
private fun CredentialRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.Gray100)
            .clickable(onClick = onCopy)
            .padding(PaceDreamSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            Text(
                text = value,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )
        }
        Text(
            text = "Copy",
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.Primary
        )
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, value))
}
