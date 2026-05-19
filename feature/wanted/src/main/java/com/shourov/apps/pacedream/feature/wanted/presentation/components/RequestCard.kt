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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.shourov.apps.pacedream.feature.wanted.model.ModerationStatus
import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.presentation.util.RequestDateFormatter
import com.shourov.apps.pacedream.feature.wanted.presentation.util.RequestExpiryResolver
import java.time.LocalDate
import java.util.Locale

@Composable
fun RequestTag(
    modifier: Modifier = Modifier,
    label: String = "Request",
) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.XS))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * Small pill that surfaces the moderation lifecycle of a [WantedRequest].
 *
 * Web parity: pending posts show an amber chip, rejected posts show a red
 * chip with the reviewer reason in the detail view. Approved requests
 * have no badge (the absence is the signal).
 */
@Composable
fun ModerationBadge(
    status: ModerationStatus,
    modifier: Modifier = Modifier,
) {
    val (background, foreground, label) = when (status) {
        ModerationStatus.PendingReview ->
            Triple(PaceDreamColors.Warning, PaceDreamColors.OnWarning, "Pending review")
        ModerationStatus.Rejected ->
            Triple(PaceDreamColors.Error, PaceDreamColors.OnError, "Rejected")
        ModerationStatus.Approved -> return
    }
    StatusPill(label = label, background = background, foreground = foreground, modifier = modifier)
}

/**
 * Lifecycle pill — shown only when there's something to communicate:
 *  - "Expiring soon" for active requests within the urgency window.
 *  - "Expired" for closed-by-time records.
 *  - "Fulfilled" / "Cancelled" for explicitly-closed records (Mine tab).
 *  - Hidden for plain Active requests (the absence is the default).
 */
@Composable
fun LifecycleBadge(
    status: RequestStatus,
    expiringSoon: Boolean,
    modifier: Modifier = Modifier,
) {
    val descriptor = remember(status, expiringSoon) {
        lifecycleDescriptor(status, expiringSoon)
    } ?: return
    StatusPill(
        label = descriptor.label,
        background = descriptor.background,
        foreground = descriptor.foreground,
        modifier = modifier,
    )
}

@Composable
private fun StatusPill(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.XS))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Immutable
private data class LifecycleDescriptor(
    val label: String,
    val background: Color,
    val foreground: Color,
)

/**
 * Returns the descriptor for a lifecycle pill, or null when no pill should
 * be rendered. Kept as a plain function so [LifecycleBadge] can wrap it in
 * a `remember(...)` with stable keys instead of recomputing on every
 * recomposition.
 */
private fun lifecycleDescriptor(
    status: RequestStatus,
    expiringSoon: Boolean,
): LifecycleDescriptor? = when {
    status == RequestStatus.Expired -> LifecycleDescriptor(
        label = "Expired",
        background = PaceDreamColors.Error,
        foreground = PaceDreamColors.OnError,
    )
    status == RequestStatus.Fulfilled -> LifecycleDescriptor(
        label = "Fulfilled",
        background = PaceDreamColors.Primary,
        foreground = PaceDreamColors.OnPrimary,
    )
    status == RequestStatus.Cancelled -> LifecycleDescriptor(
        label = "Cancelled",
        background = PaceDreamColors.Surface,
        foreground = PaceDreamColors.TextSecondary,
    )
    expiringSoon -> LifecycleDescriptor(
        label = "Expiring soon",
        background = PaceDreamColors.Warning,
        foreground = PaceDreamColors.OnWarning,
    )
    else -> null
}

@Composable
fun RequestCard(
    request: WantedRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    // Status + activeUntil are derived from inputs that change only when the
    // request itself or the current day changes — keying remember on those
    // explicitly avoids re-parsing the ISO date on every recomposition.
    val effectiveStatus by remember(request, today) {
        derivedStateOf { RequestExpiryResolver.effectiveStatus(request, today) }
    }
    val expiringSoon by remember(request, today) {
        derivedStateOf { RequestExpiryResolver.isExpiringSoon(request, today) }
    }
    val activeUntilLabel = remember(request) {
        RequestExpiryResolver.activeUntilLabel(request, Locale.getDefault())
    }
    val requestDateLabel = remember(request) {
        RequestDateFormatter.format(request.requestStartDate, request.requestEndDate)
            ?.let { "Request date: $it" }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RequestTag()
                Text(
                    text = request.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (request.moderationStatus != ModerationStatus.Approved) {
                    ModerationBadge(status = request.moderationStatus)
                }
                LifecycleBadge(status = effectiveStatus, expiringSoon = expiringSoon)
            }
            Text(
                text = request.title.ifBlank { "Untitled request" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (request.description.isNotBlank()) {
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (request.location.isNotBlank()) {
                        Text(
                            text = request.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    requestDateLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // "Active until" surfaces the auto-expiry as a separate
                    // line so users can tell apart the requested window from
                    // the moment the post itself closes. Hidden once the
                    // request is no longer Active.
                    if (effectiveStatus == RequestStatus.Active) {
                        activeUntilLabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (expiringSoon) PaceDreamColors.Warning
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (expiringSoon) FontWeight.SemiBold
                                else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                request.budget?.let { budget ->
                    Text(
                        text = formatBudget(budget, request.budgetCurrency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

fun formatBudget(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$currency "
    }
    val rounded = if (amount % 1.0 == 0.0) amount.toLong().toString() else "%.2f".format(amount)
    return "$symbol$rounded"
}
