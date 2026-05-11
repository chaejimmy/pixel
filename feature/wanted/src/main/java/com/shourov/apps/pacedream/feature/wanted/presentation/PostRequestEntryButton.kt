package com.shourov.apps.pacedream.feature.wanted.presentation

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
 * Reusable, non-intrusive entry point to the "Post a Request" flow.
 *
 * Use this on home/search/profile/help surfaces to expose the feature
 * without a floating button (which on mobile competes with primary CTAs).
 *
 * Two variants:
 *  - [PostRequestEntryStyle.InlineLink] — text-only link, e.g. tucked
 *    under another CTA in a search empty state.
 *  - [PostRequestEntryStyle.SoftCard]   — small rounded card, suitable for
 *    a discover/promo slot on the home screen.
 */
enum class PostRequestEntryStyle { InlineLink, SoftCard }

@Composable
fun PostRequestEntryButton(
    source: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Can't find what you need? Post a request",
    style: PostRequestEntryStyle = PostRequestEntryStyle.InlineLink,
) {
    val handle = {
        Timber.d("post_request_entry_tapped source=$source style=$style")
        onClick()
    }
    when (style) {
        PostRequestEntryStyle.InlineLink -> InlineLink(title, handle, modifier)
        PostRequestEntryStyle.SoftCard -> SoftCard(title, handle, modifier)
    }
}

@Composable
private fun InlineLink(
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
            imageVector = PaceDreamIcons.AddCircle,
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
private fun SoftCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Primary.copy(alpha = 0.08f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = PaceDreamColors.Primary.copy(alpha = 0.18f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.AddCircle,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(PaceDreamSpacing.SM2))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Tell providers what you need — they'll send offers.",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
