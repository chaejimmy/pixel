package com.pacedream.common.composables.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pacedream.common.icon.PaceDreamIcons
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@Composable
fun PaceDreamListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = PaceDreamColors.TextSecondary,
    trailingContent: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = leadingIconTint,
                    modifier = Modifier.size(PaceDreamIconSize.MD)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = PaceDreamTypography.Footnote,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else if (onClick != null) {
                Icon(
                    imageVector = PaceDreamIcons.ChevronRight,
                    contentDescription = null,
                    tint = PaceDreamColors.TextTertiary,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = PaceDreamColors.Border,
                modifier = Modifier.padding(start = PaceDreamSpacing.MD)
            )
        }
    }
}

@Composable
fun PaceDreamNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    showDivider: Boolean = true,
) {
    PaceDreamListRow(
        title = title,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        onClick = onClick,
        showDivider = showDivider,
        modifier = modifier,
    )
}
