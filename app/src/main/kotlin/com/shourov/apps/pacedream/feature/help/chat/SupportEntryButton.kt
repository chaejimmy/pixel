package com.shourov.apps.pacedream.feature.help.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import timber.log.Timber

/**
 * Reusable, non-intrusive entry point to the customer support chat.
 *
 * Use this in screen footers (Booking Detail, Checkout, Listing Detail) to
 * give users a "Need help with this?" shortcut without committing to a
 * floating button, which on mobile tends to block primary actions.
 *
 * Two compact variants:
 *  - [SupportEntryStyle.InlineLink] — text-only link, suitable for under a CTA
 *  - [SupportEntryStyle.SoftCard]   — small rounded card, suitable for an
 *                                     empty state or list footer
 *
 * Both navigate to the support chat with [category] preselected so the AI
 * greeting is on-topic for the surface that opened it. A contextual
 * analytics event is logged so we can measure which surfaces drive
 * support load.
 */
enum class SupportEntryStyle { InlineLink, SoftCard }

@Composable
fun SupportEntryButton(
    source: String,
    onNavigateToSupportChat: (category: SupportCategory, initialMessage: String?) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Need help? Chat with support",
    category: SupportCategory = SupportCategory.General,
    style: SupportEntryStyle = SupportEntryStyle.InlineLink,
    initialMessage: String? = null,
) {
    val onClick = {
        Timber.d("support_chat_entry_tapped source=$source category=${category.key} style=$style")
        onNavigateToSupportChat(category, initialMessage)
    }
    when (style) {
        SupportEntryStyle.InlineLink -> InlineLinkLabel(
            title = title,
            onClick = onClick,
            modifier = modifier,
        )
        SupportEntryStyle.SoftCard -> SoftCardLabel(
            title = title,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun InlineLinkLabel(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = PaceDreamIcons.Help,
            contentDescription = null,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.Primary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.Primary.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun SoftCardLabel(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.SM2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = PaceDreamColors.Primary.copy(alpha = 0.15f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Help,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(PaceDreamSpacing.SM2))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Average reply in a few minutes.",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
