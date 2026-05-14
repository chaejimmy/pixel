package com.pacedream.common.composables.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * Shared signed-out empty state for Favorites / Bookings / Messages / Profile.
 *
 * Replaces the previous "giant centered lock icon on a blank screen" pattern
 * with a marketplace-native card: a soft purple icon badge, title, subtitle,
 * primary CTA, and an optional secondary discovery action. The icon is the
 * feature's own icon (heart / calendar / chat / person), not a lock — the
 * sign-in affordance is the button, not the iconography.
 */
@Composable
fun SignedOutEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primaryCtaText: String,
    onPrimaryCta: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryCtaText: String? = null,
    onSecondaryCta: (() -> Unit)? = null,
    applySystemInsets: Boolean = true,
) {
    val rootModifier = modifier
        .fillMaxSize()
        .background(PaceDreamColors.Background)
        .let { if (applySystemInsets) it.statusBarsPadding().navigationBarsPadding() else it }

    Box(
        modifier = rootModifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.XL),
        ) {
            // Purple-tinted icon badge — premium feel, replaces the lone lock
            // icon. The icon reflects the tab's own affordance.
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = PaceDreamColors.Primary.copy(alpha = 0.10f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(34.dp),
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Text(
                text = title,
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = subtitle,
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.86f),
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Button(
                onClick = onPrimaryCta,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary,
                    contentColor = PaceDreamColors.OnPrimary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier.fillMaxWidth(0.72f),
            ) {
                Text(
                    text = primaryCtaText,
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (secondaryCtaText != null && onSecondaryCta != null) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                TextButton(
                    onClick = onSecondaryCta,
                ) {
                    Text(
                        text = secondaryCtaText,
                        style = PaceDreamTypography.Subheadline,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
