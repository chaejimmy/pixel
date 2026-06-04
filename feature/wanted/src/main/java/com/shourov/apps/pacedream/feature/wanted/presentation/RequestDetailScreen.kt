@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.paceDreamFontFamily
import com.pacedream.common.util.MoneyFormatter
import com.shourov.apps.pacedream.feature.wanted.model.ModerationStatus
import com.shourov.apps.pacedream.feature.wanted.model.RequestDetailUiState
import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.presentation.components.LifecycleBadge
import com.shourov.apps.pacedream.feature.wanted.presentation.components.ModerationBadge
import com.shourov.apps.pacedream.feature.wanted.presentation.components.OfferStatusPill
import com.shourov.apps.pacedream.feature.wanted.presentation.components.RequestDetailSkeleton
import com.shourov.apps.pacedream.feature.wanted.presentation.components.RequestTag
import com.shourov.apps.pacedream.feature.wanted.presentation.util.RequestDateFormatter
import com.shourov.apps.pacedream.feature.wanted.presentation.util.RequestExpiryResolver
import java.time.LocalDate

@Composable
fun RequestDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    onRequireAuth: () -> Unit = {},
    onNavigateToAuthor: (String) -> Unit = {},
    onMessageProvider: (offerId: String, providerName: String?) -> Unit = { _, _ -> },
    onAcceptOffer: (offerId: String) -> Unit = {},
    viewModel: RequestDetailViewModel = hiltViewModel(),
    onViewMyOffers: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val offerState by viewModel.offer.collectAsStateWithLifecycle()
    var sheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) { viewModel.load(requestId) }

    val requesterName = (state as? RequestDetailUiState.Content)?.request?.authorName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            (state as? RequestDetailUiState.Content)?.let { content ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM2),
                ) {
                    val effectiveStatus = remember(content.request) {
                        RequestExpiryResolver.effectiveStatus(content.request, LocalDate.now())
                    }
                    if (content.isOwner) {
                        OwnerActionsBar(
                            status = effectiveStatus,
                            onRenew = viewModel::renew,
                            onMarkFulfilled = viewModel::markFulfilled,
                            onCancel = viewModel::cancel,
                        )
                    } else {
                        // Once `submitted` flips true the bottom button locks
                        // into a disabled "Offer sent" state so users can't
                        // double-submit. Pre-submit we still need to send
                        // signed-out users through the auth flow before
                        // opening the sheet.
                        //
                        // A non-approved request shouldn't appear in the
                        // public browse feed, but a stale deep link can land
                        // a provider here. We disable the CTA in that case
                        // so the offer endpoint never sees a moderation
                        // mismatch.
                        //
                        // Same defense for lifecycle: an Expired / Fulfilled /
                        // Cancelled request cannot receive new offers. The
                        // server enforces this, but the client also blocks
                        // it so we never POST a doomed offer.
                        val alreadySent = offerState.submitted
                        val moderationBlocked =
                            content.request.moderationStatus != ModerationStatus.Approved
                        val lifecycleBlocked = effectiveStatus != RequestStatus.Active
                        Button(
                            onClick = {
                                if (!content.isSignedIn) {
                                    onRequireAuth()
                                } else {
                                    viewModel.resetOfferSheet()
                                    sheetVisible = true
                                }
                            },
                            enabled = !alreadySent && !moderationBlocked && !lifecycleBlocked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val label = when {
                                moderationBlocked -> "Not yet available"
                                lifecycleBlocked -> when (effectiveStatus) {
                                    RequestStatus.Expired -> "This request expired"
                                    RequestStatus.Fulfilled -> "Request fulfilled"
                                    RequestStatus.Cancelled -> "Request cancelled"
                                    else -> "Not accepting offers"
                                }
                                alreadySent -> "Offer sent"
                                else -> "Make an Offer"
                            }
                            Text(label)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                RequestDetailUiState.Loading -> RequestDetailSkeleton()
                is RequestDetailUiState.Error -> Centered {
                    Text(
                        text = s.message,
                        style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
                        color = PaceDreamColors.Error,
                        textAlign = TextAlign.Center,
                    )
                }
                is RequestDetailUiState.Content -> RequestDetailBody(
                    request = s.request,
                    offers = s.offers,
                    isOwner = s.isOwner,
                    onAuthorClick = s.request.authorId
                        ?.takeIf { it.isNotBlank() }
                        ?.let { id -> { onNavigateToAuthor(id) } },
                    onMessageProvider = onMessageProvider,
                    onAcceptOffer = onAcceptOffer,
                )
            }
        }
    }

    if (sheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            // Keep `submitted = true` after dismiss so the bottom bar
            // continues to show the disabled "Offer sent" state. Only
            // discard a draft (unsubmitted) form.
            onDismissRequest = {
                sheetVisible = false
                if (!offerState.submitted) viewModel.resetOfferSheet()
            },
            sheetState = sheetState,
        ) {
            OfferBottomSheetContent(
                state = offerState,
                onPriceChange = viewModel::onPriceChange,
                onMessageChange = viewModel::onMessageChange,
                onSubmit = viewModel::submitOffer,
                onDone = { sheetVisible = false },
                requesterName = requesterName,
                onViewMyOffers = onViewMyOffers?.let { nav ->
                    {
                        sheetVisible = false
                        nav()
                    }
                },
                onExpiryChange = viewModel::onExpiryChange,
                onLinkedListingChange = viewModel::onLinkedListingChange,
            )
        }
    }
}

@Composable
private fun RequestDetailBody(
    request: WantedRequest,
    offers: List<WantedOffer>,
    isOwner: Boolean,
    onAuthorClick: (() -> Unit)? = null,
    onMessageProvider: (offerId: String, providerName: String?) -> Unit,
    onAcceptOffer: (offerId: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        request.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.MD)),
            )
        }
        val today = remember { LocalDate.now() }
        val effectiveStatus = remember(request, today) {
            RequestExpiryResolver.effectiveStatus(request, today)
        }
        val expiringSoon = remember(request, today) {
            RequestExpiryResolver.isExpiringSoon(request, today)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            RequestTag()
            Text(
                text = request.category,
                style = PaceDreamTypography.Footnote.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.TextSecondary,
            )
            if (request.moderationStatus != ModerationStatus.Approved) {
                ModerationBadge(status = request.moderationStatus)
            }
            LifecycleBadge(status = effectiveStatus, expiringSoon = expiringSoon)
        }
        if (isOwner && request.moderationStatus != ModerationStatus.Approved) {
            ModerationBanner(
                status = request.moderationStatus,
                reason = request.moderationReason,
            )
        }
        Text(
            text = request.title.ifBlank { "Untitled request" },
            style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextHeadline,
            fontWeight = FontWeight.Bold,
        )
        AuthorRow(
            name = request.authorName,
            avatarUrl = request.authorAvatarUrl,
            onClick = onAuthorClick,
        )
        request.budget?.let { budget ->
            Text(
                text = "Budget: ${MoneyFormatter.formatAmount(budget, request.budgetCurrency)}",
                style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (request.location.isNotBlank()) {
            DetailRow(label = "Location", value = request.location)
        }
        RequestDateFormatter.format(request.requestStartDate, request.requestEndDate)?.let {
            DetailRow(label = "Request date", value = it)
        }
        if (effectiveStatus == RequestStatus.Active) {
            RequestExpiryResolver.formattedExpiry(request)?.let { value ->
                DetailRow(label = "Active until", value = value)
            }
        }

        if (isOwner) {
            Spacer(Modifier.height(PaceDreamSpacing.XS))
            OffersList(
                offers = offers,
                onMessageProvider = onMessageProvider,
                onAcceptOffer = onAcceptOffer,
            )
        }

        Spacer(Modifier.height(PaceDreamSpacing.XS))
        Text(
            text = "Description",
            style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextHeadline,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = request.description.ifBlank { "No description provided." },
            style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextPrimary,
        )
    }
}

@Composable
private fun OffersList(
    offers: List<WantedOffer>,
    onMessageProvider: (offerId: String, providerName: String?) -> Unit,
    onAcceptOffer: (offerId: String) -> Unit,
) {
    Text(
        text = if (offers.isEmpty()) "Offers" else "Offers (${offers.size})",
        style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
        color = PaceDreamColors.TextHeadline,
        fontWeight = FontWeight.SemiBold,
    )
    if (offers.isEmpty()) {
        Text(
            text = "No offers yet — we'll notify you when providers respond.",
            style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextSecondary,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)) {
        offers.forEach { offer ->
            OfferRow(
                offer = offer,
                onMessageProvider = { onMessageProvider(offer.id, offer.authorName) },
                onAccept = { onAcceptOffer(offer.id) },
            )
        }
    }
}

@Composable
private fun OfferRow(
    offer: WantedOffer,
    onMessageProvider: () -> Unit,
    onAccept: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Card,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProviderAvatar(
                    avatarUrl = offer.authorAvatarUrl,
                    initial = offer.authorName?.firstOrNull()?.uppercaseChar()?.toString(),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.authorName?.takeIf { it.isNotBlank() } ?: "Provider",
                        style = PaceDreamTypography.Callout.copy(fontFamily = paceDreamFontFamily),
                        color = PaceDreamColors.TextHeadline,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = MoneyFormatter.formatAmount(offer.price, offer.currency),
                        style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OfferStatusPill(status = offer.status)
            }
            if (offer.message.isNotBlank()) {
                Text(
                    text = offer.message,
                    style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = PaceDreamColors.TextSecondary,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onMessageProvider,
                    modifier = Modifier.weight(1f),
                ) { Text("Message provider") }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                ) { Text("Accept") }
            }
        }
    }
}

@Composable
private fun ProviderAvatar(avatarUrl: String?, initial: String?) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial ?: "P",
                style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AuthorRow(
    name: String?,
    avatarUrl: String?,
    onClick: (() -> Unit)?,
) {
    val displayName = name?.takeIf { it.isNotBlank() } ?: "Member"
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(vertical = PaceDreamSpacing.XS)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        val avatarModifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                color = PaceDreamColors.SurfaceVariant,
                shape = CircleShape,
            )
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = avatarModifier,
            )
        } else {
            Box(
                modifier = avatarModifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.first().uppercaseChar().toString(),
                    style = PaceDreamTypography.Footnote.copy(fontFamily = paceDreamFontFamily),
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = displayName,
            style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
            fontWeight = FontWeight.SemiBold,
            color = PaceDreamColors.TextPrimary,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Footnote.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextSecondary,
            modifier = Modifier.padding(end = PaceDreamSpacing.XS),
        )
        Text(
            text = value,
            style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextPrimary,
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.LG),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Owner CTA row at the bottom of the detail screen. The buttons exposed
 * depend on the current lifecycle status:
 *  - Active: Mark as Fulfilled (primary) + Cancel (outlined)
 *  - Expired: Renew (primary) + Cancel (outlined)
 *  - Fulfilled / Cancelled: a read-only label so the bar still occupies
 *    the same space as on Active requests.
 */
@Composable
private fun OwnerActionsBar(
    status: RequestStatus,
    onRenew: () -> Unit,
    onMarkFulfilled: () -> Unit,
    onCancel: () -> Unit,
) {
    when (status) {
        RequestStatus.Active -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) { Text("Cancel Request") }
            Button(
                onClick = onMarkFulfilled,
                modifier = Modifier.weight(1f),
            ) { Text("Mark as Fulfilled") }
        }
        RequestStatus.Expired -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) { Text("Cancel Request") }
            Button(
                onClick = onRenew,
                modifier = Modifier.weight(1f),
            ) { Text("Renew Request") }
        }
        RequestStatus.Fulfilled -> Text(
            text = "Marked as fulfilled.",
            style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        RequestStatus.Cancelled -> Text(
            text = "This request was cancelled.",
            style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Owner-only inline notice surfaced when the request is gated by
 * moderation. Pending review prints reassuring copy; rejected prints the
 * reviewer note (or a generic explainer) and steers the user toward
 * editing/reposting.
 */
@Composable
private fun ModerationBanner(
    status: ModerationStatus,
    reason: String?,
) {
    val accent = when (status) {
        ModerationStatus.PendingReview -> PaceDreamColors.Warning
        ModerationStatus.Rejected -> PaceDreamColors.Error
        ModerationStatus.Approved -> return
    }
    val title = when (status) {
        ModerationStatus.PendingReview -> "Awaiting moderator review"
        ModerationStatus.Rejected -> "This request needs changes"
        ModerationStatus.Approved -> return
    }
    val body = when (status) {
        ModerationStatus.PendingReview ->
            "Only you can see this request right now. We'll publish it as " +
                "soon as a moderator clears it — usually within a few hours."
        ModerationStatus.Rejected -> reason
            ?.takeIf { it.isNotBlank() }
            ?: "A moderator declined this post. Update the details and " +
                "submit again to try once more."
        ModerationStatus.Approved -> return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(accent.copy(alpha = 0.12f))
            .padding(PaceDreamSpacing.SM2),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS),
    ) {
        Text(
            text = title,
            style = PaceDreamTypography.Callout.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextPrimary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews — light & dark. The hand-built sample state is fed straight into the
// stateless RequestDetailBody (no Hilt ViewModel). Two variants: the provider's
// view and the owner's view (which surfaces the offers list). The dark pass
// confirms the detail surfaces use the migrated tokens.
// ─────────────────────────────────────────────────────────────────────────────

private val SampleRequest = WantedRequest(
    id = "preview-1",
    title = "Need a quiet meeting room for 2 hours",
    description = "Looking for a small, quiet room downtown this afternoon for a " +
        "client call. Wi-Fi and a door that closes are the only must-haves.",
    type = "space",
    category = "Meeting room",
    location = "Downtown, San Francisco",
    budget = 40.0,
    imageUrl = null,
    authorName = "Jordan Lee",
)

private val SampleOffers = listOf(
    WantedOffer(
        id = "offer-1",
        requestId = "preview-1",
        price = 35.0,
        message = "I have a quiet room two blocks from downtown — available all afternoon.",
        authorName = "Casey Morgan",
    ),
    WantedOffer(
        id = "offer-2",
        requestId = "preview-1",
        price = 42.0,
        message = "Private studio with fast Wi-Fi, flexible on timing.",
        authorName = "Riley Chen",
    ),
)

@Composable
private fun RequestDetailPreviewBody(
    isOwner: Boolean,
    darkTheme: Boolean,
) {
    PaceDreamTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            RequestDetailBody(
                request = SampleRequest,
                offers = if (isOwner) SampleOffers else emptyList(),
                isOwner = isOwner,
                onMessageProvider = { _, _ -> },
                onAcceptOffer = {},
            )
        }
    }
}

@Preview(name = "RequestDetail Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestDetailLightPreview() {
    RequestDetailPreviewBody(isOwner = false, darkTheme = false)
}

@Preview(
    name = "RequestDetail Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestDetailDarkPreview() {
    RequestDetailPreviewBody(isOwner = false, darkTheme = true)
}

@Preview(name = "RequestDetail Owner Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestDetailOwnerLightPreview() {
    RequestDetailPreviewBody(isOwner = true, darkTheme = false)
}

@Preview(
    name = "RequestDetail Owner Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestDetailOwnerDarkPreview() {
    RequestDetailPreviewBody(isOwner = true, darkTheme = true)
}
