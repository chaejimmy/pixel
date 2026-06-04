package com.shourov.apps.pacedream.feature.wanted.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.designsystem.modifier.pressable
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.util.MoneyFormatter
import com.shourov.apps.pacedream.designsystem.modifier.adaptiveShadow
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
        style = PaceDreamTypography.Caption2,
        color = PaceDreamColors.OnPrimary,
        fontWeight = FontWeight.SemiBold,
        // Decorative chip: clearAndSetSemantics (applied at the end of the
        // chain) keeps it out of TalkBack's focus order so the whole card reads
        // as one button. defaultMinSize gives it a chip-like minimum height.
        modifier = modifier
            .defaultMinSize(minHeight = 20.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.XS))
            .background(PaceDreamColors.Primary)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clearAndSetSemantics { },
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
        style = PaceDreamTypography.Caption,
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
            // Lifted look via the design-system shadow (dark-mode safe), not
            // Material elevation — matches FeaturedFullWidthCard.
            .adaptiveShadow(
                elevation = PaceDreamElevation.SM,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            )
            // Clip BEFORE pressable so the Material ripple is clipped to the
            // card's rounded corners (the ripple is drawn at the pressable node).
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            // The shared press affordance: ripple + cancel-on-slide-off + a
            // subtle card-scale, with the action verb + Role.Button for TalkBack.
            .pressable(
                onClick = onClick,
                onClickLabel = "Open request: ${request.title.ifBlank { "Untitled request" }}",
                pressedScale = 0.98f,
                role = Role.Button,
            ),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, PaceDreamColors.Border.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                // Merge the card's text into one semantics node so TalkBack reads
                // it as a single stop instead of walking each line; the action
                // verb is supplied by .pressable's onClickLabel above.
                .semantics(mergeDescendants = true) {
                    contentDescription = buildString {
                        append("Request. ")
                        append(request.title.ifBlank { "Untitled request" })
                        append(", ${request.category}")
                        if (request.location.isNotBlank()) append(", ${request.location}")
                        requestDateLabel?.let { append(", $it") }
                        request.budget?.let {
                            append(", budget ${MoneyFormatter.formatAmount(it, request.budgetCurrency)}")
                        }
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RequestTag()
                Text(
                    text = request.category,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                )
                if (request.moderationStatus != ModerationStatus.Approved) {
                    ModerationBadge(status = request.moderationStatus)
                }
                LifecycleBadge(status = effectiveStatus, expiringSoon = expiringSoon)
            }
            Text(
                text = request.title.ifBlank { "Untitled request" },
                style = PaceDreamTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = PaceDreamColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (request.description.isNotBlank()) {
                Text(
                    text = request.description,
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextSecondary,
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
                            style = PaceDreamTypography.Footnote,
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    requestDateLabel?.let {
                        Text(
                            text = it,
                            style = PaceDreamTypography.Footnote,
                            color = PaceDreamColors.TextSecondary,
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
                                style = PaceDreamTypography.Footnote,
                                color = if (expiringSoon) PaceDreamColors.Warning
                                else PaceDreamColors.TextSecondary,
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
                        text = MoneyFormatter.formatAmount(budget, request.budgetCurrency),
                        style = PaceDreamTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = PaceDreamColors.Primary,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews — light & dark. Two fixtures: one with a budget (so the primary-tinted
// budget label renders) and one without (so the layout collapses gracefully).
// Renders without a Hilt graph; the dark pass confirms the card surface uses the
// migrated tokens rather than a stray Material default.
// ─────────────────────────────────────────────────────────────────────────────

private val SampleRequests = listOf(
    WantedRequest(
        id = "preview-1",
        title = "Need a quiet meeting room for 2 hours",
        description = "Looking for a small room downtown this afternoon.",
        type = "space",
        category = "Meeting room",
        location = "Downtown, San Francisco",
        budget = 40.0,
        imageUrl = null,
    ),
    WantedRequest(
        id = "preview-2",
        title = "Borrow a DSLR camera for the weekend",
        description = "Any Canon or Nikon body with a kit lens works.",
        type = "item",
        category = "Camera",
        location = "Mission, San Francisco",
        budget = null,
        imageUrl = null,
    ),
)

@Composable
private fun RequestCardPreviewBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaceDreamColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SampleRequests.forEach { request ->
            RequestCard(
                request = request,
                onClick = {},
                // Pin "today" so the derived lifecycle/expiry labels stay
                // deterministic across preview renders.
                today = LocalDate.of(2025, 1, 1),
            )
        }
    }
}

@Preview(name = "RequestCard Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestCardLightPreview() {
    PaceDreamTheme(darkTheme = false) {
        RequestCardPreviewBody()
    }
}

@Preview(
    name = "RequestCard Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestCardDarkPreview() {
    PaceDreamTheme(darkTheme = true) {
        RequestCardPreviewBody()
    }
}
