package com.shourov.apps.pacedream.feature.wanted.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.shourov.apps.pacedream.feature.wanted.model.OfferStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer

/**
 * Compact card used in the "My offers" tab and inline inside an author's
 * request-detail OffersList.
 *
 * Shows the request title (when the parent passes one through), the
 * provider's price, a one-line message snippet, and a status pill. The
 * "View request" link is the whole-card tap target.
 */
@Composable
fun OfferCard(
    offer: WantedOffer,
    onViewRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showRequestTitle: Boolean = true,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewRequest),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showRequestTitle) {
                offer.requestTitle?.takeIf { it.isNotBlank() }?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = formatBudget(offer.price, offer.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                OfferStatusPill(status = offer.status)
            }
            if (offer.message.isNotBlank()) {
                Text(
                    text = offer.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "View request",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Coloured pill that mirrors the web's offer-status chip.
 */
@Composable
fun OfferStatusPill(
    status: OfferStatus,
    modifier: Modifier = Modifier,
) {
    val container: Color
    val onContainer: Color
    when (status) {
        OfferStatus.Pending -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer
        }
        OfferStatus.Accepted -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer
        }
        OfferStatus.Declined -> {
            container = MaterialTheme.colorScheme.errorContainer
            onContainer = MaterialTheme.colorScheme.onErrorContainer
        }
    }
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = onContainer,
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.XS))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
