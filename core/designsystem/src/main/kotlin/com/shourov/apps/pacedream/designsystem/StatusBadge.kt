package com.shourov.apps.pacedream.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing

/**
 * Non-interactive pill that labels the state of its owning row (e.g. "Default",
 * "Verified", "Active"). Use this instead of a disabled button — TalkBack
 * announces a disabled button as "<label>, button, disabled", which reads as
 * a broken action rather than a status. StatusBadge declares its semantic
 * role as [Role.Image] so screen readers treat it purely as a label, and
 * merges the text + icon into a single accessibility node.
 *
 * The background is a low-opacity tint of [color] so the badge blends with
 * any surface, while the text and optional [icon] render in the full color
 * for legibility on light backgrounds.
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = PaceDreamColors.Primary,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.Round))
            .background(color.copy(alpha = BACKGROUND_ALPHA))
            .padding(
                horizontal = PaceDreamSpacing.SM2,
                vertical = PaceDreamSpacing.XS,
            )
            .semantics(mergeDescendants = true) {
                role = Role.Image
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

private const val BACKGROUND_ALPHA = 0.12f
