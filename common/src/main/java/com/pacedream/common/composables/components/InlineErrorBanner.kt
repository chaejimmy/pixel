package com.pacedream.common.composables.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * Inline error banner for displaying recoverable errors
 * 
 * iOS parity: When a section fails to load on the home screen,
 * show an inline warning banner "Some content couldn't load. Pull to refresh."
 */
@Composable
fun InlineErrorBanner(
    message: String = "Some content couldn't load. Pull to refresh.",
    isVisible: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    onAction: (() -> Unit)? = null,
    actionText: String? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
                .background(
                    color = PaceDreamColors.Warning.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                )
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = PaceDreamColors.Warning,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                
                Text(
                    text = message,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Action button (optional)
                if (onAction != null && actionText != null) {
                    TextButton(onClick = onAction) {
                        Text(
                            text = actionText,
                            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                            color = PaceDreamColors.Primary
                        )
                    }
                }
                
                // Dismiss button (optional)
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section error banner - shows when a specific section fails
 */
@Composable
fun SectionErrorBanner(
    sectionName: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    InlineErrorBanner(
        message = "$sectionName couldn't load",
        isVisible = true,
        onDismiss = onDismiss,
        onAction = onRetry,
        actionText = "Retry",
        modifier = modifier
    )
}

/**
 * Network error banner - shows for network-related issues
 */
@Composable
fun NetworkErrorBanner(
    isVisible: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
                .background(
                    color = PaceDreamColors.Error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                )
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = PaceDreamColors.Error,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                
                Text(
                    text = "Connection issue. Check your network.",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary
                )
            }
            
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                    color = PaceDreamColors.Primary
                )
            }
        }
    }
}


