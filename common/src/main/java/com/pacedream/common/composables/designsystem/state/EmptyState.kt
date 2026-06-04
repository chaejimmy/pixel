package com.pacedream.common.composables.designsystem.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography as DSTypo
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily

/**
 * Branded empty / error placeholder — icon-in-circle, title, subtitle and a
 * primary CTA button. Shared across surfaces (Home, Wanted, Bookings) so the
 * "nothing here" moments read consistently.
 *
 * Originally a private composable in HomeScreen; promoted to the design system
 * and generalised so the copy and icon are caller-supplied. Home keeps its
 * previous rendering by passing the same strings/icon it hard-coded before.
 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    ctaLabel: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    PaceDreamColors.Primary.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.Layout.HomeGutter))
        Text(
            text = title,
            style = DSTypo.Title3.copy(
                fontFamily = paceDreamDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = PaceDreamColors.TextHeadline,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = subtitle,
            style = DSTypo.Subheadline.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.IconNeutral,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Button(
            onClick = onCta,
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            contentPadding = PaddingValues(
                horizontal = PaceDreamSpacing.LG,
                vertical = PaceDreamSpacing.SM2
            )
        ) {
            Text(
                text = ctaLabel,
                style = DSTypo.Subheadline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
