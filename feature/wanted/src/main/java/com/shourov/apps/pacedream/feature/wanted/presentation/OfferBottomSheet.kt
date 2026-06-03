// @DesignSystemEscape (reason="legacy debt tracked in DESIGN_SYSTEM_COVERAGE.md — migrate per the suggested order in that file before removing this opt-out")
package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamStroke
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily
import com.shourov.apps.pacedream.feature.wanted.model.HostListingSummary
import com.shourov.apps.pacedream.feature.wanted.model.OFFER_MESSAGE_MAX_LENGTH
import com.shourov.apps.pacedream.feature.wanted.model.OfferExpiry
import com.shourov.apps.pacedream.feature.wanted.model.OfferFormState

/**
 * Bottom sheet body for the offer flow. Renders one of two layouts:
 *  - The submission form (default).
 *  - A success confirmation when [OfferFormState.submitted] is true.
 *
 * We never auto-dismiss the success state — users must tap "Done" so
 * they see explicit confirmation that the offer was sent and don't
 * resubmit duplicates.
 */
@Composable
fun OfferBottomSheetContent(
    state: OfferFormState,
    onPriceChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
    requesterName: String?,
    modifier: Modifier = Modifier,
    onViewMyOffers: (() -> Unit)? = null,
    onExpiryChange: (Int) -> Unit = {},
    onLinkedListingChange: (String?) -> Unit = {},
) {
    if (state.submitted) {
        OfferSubmittedContent(
            requesterName = requesterName,
            onDone = onDone,
            onViewMyOffers = onViewMyOffers,
            modifier = modifier,
        )
    } else {
        OfferFormContent(
            state = state,
            onPriceChange = onPriceChange,
            onMessageChange = onMessageChange,
            onSubmit = onSubmit,
            onExpiryChange = onExpiryChange,
            onLinkedListingChange = onLinkedListingChange,
            modifier = modifier,
        )
    }
}

@Composable
private fun OfferFormContent(
    state: OfferFormState,
    onPriceChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onExpiryChange: (Int) -> Unit,
    onLinkedListingChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageOverLimit = state.message.length > OFFER_MESSAGE_MAX_LENGTH
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // The composer now packs five rows (title, price, message,
            // expiry, optional listing row, submit) — let it scroll on
            // short devices instead of clipping the submit button.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter, vertical = PaceDreamSpacing.SM)
            .testTag(OfferSheetTestTags.Form),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        Text(
            text = "Make an Offer",
            style = PaceDreamTypography.Title2.copy(fontFamily = paceDreamDisplayFontFamily),
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = state.price,
            onValueChange = onPriceChange,
            label = { RequiredLabel("Price") },
            singleLine = true,
            prefix = { Text("$") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
        )
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = { RequiredLabel("Message") },
            placeholder = { Text("Briefly describe your offer") },
            isError = messageOverLimit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OfferSheetTestTags.MessageField),
            minLines = 3,
            maxLines = 5,
            enabled = !state.submitting,
        )
        // Live character counter — flips to the error color the moment
        // the user crosses the limit. Submit is also gated on this so
        // they can't ship an over-length message by accident.
        Text(
            text = "${state.message.length}/$OFFER_MESSAGE_MAX_LENGTH",
            style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
            color = if (messageOverLimit) PaceDreamColors.Error
                else PaceDreamColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OfferSheetTestTags.MessageCounter),
            textAlign = TextAlign.End,
        )
        ExpiryRow(
            selectedHours = state.expiresInHours,
            enabled = !state.submitting,
            onChange = onExpiryChange,
        )
        // The link-a-listing row is host-only AND requires at least one
        // published listing. Empty list ⇒ no row at all so hosts who
        // haven't published anything don't see a broken affordance.
        if (state.hostListings.isNotEmpty()) {
            LinkListingRow(
                listings = state.hostListings,
                selectedId = state.linkedListingId,
                enabled = !state.submitting,
                onSelect = onLinkedListingChange,
            )
        }
        state.error?.let {
            Text(
                text = it,
                style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.Error,
            )
        }
        Button(
            onClick = onSubmit,
            enabled = !state.submitting && !messageOverLimit,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM2),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    strokeWidth = PaceDreamStroke.Thick,
                    color = PaceDreamColors.OnPrimary,
                    modifier = Modifier
                        .padding(end = PaceDreamSpacing.SM)
                        // intentional: 18dp inline spinner sized to the button
                        // label, off the icon-size scale.
                        .size(18.dp),
                )
            }
            Text(if (state.submitting) "Submitting…" else "Send offer")
        }
    }
}

/**
 * Field label with a trailing red asterisk so the required affordance
 * is visible inline (matches the rest of the create flows).
 */
@Composable
private fun RequiredLabel(text: String) {
    Text(
        buildAnnotatedString {
            append(text)
            append(' ')
            withStyle(SpanStyle(color = PaceDreamColors.Error)) {
                append("*")
            }
        }
    )
}

@Composable
private fun ExpiryRow(
    selectedHours: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    // intentional: 6dp is a deliberately tight label→chips gap, between
    // XS (4dp) and SM (8dp) on the spacing scale.
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Expires in",
            style = PaceDreamTypography.Footnote.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextSecondary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            modifier = Modifier.testTag(OfferSheetTestTags.ExpiryRow),
        ) {
            OfferExpiry.entries.forEach { option ->
                FilterChip(
                    selected = option.hours == selectedHours,
                    onClick = { onChange(option.hours) },
                    enabled = enabled,
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
}

@Composable
private fun LinkListingRow(
    listings: List<HostListingSummary>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
) {
    Column(
        modifier = Modifier.testTag(OfferSheetTestTags.LinkListingRow),
        // intentional: 6dp tight label→chips gap (between XS=4dp and SM=8dp).
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Link a listing",
            style = PaceDreamTypography.Footnote.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextSecondary,
        )
        // Inline picker: each listing is a toggleable chip. Tapping the
        // selected chip again clears it (handled by the caller). The
        // column scrolls if the host has many listings.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // intentional: 180dp max picker height before it scrolls — a
                // layout cap, not a spacing-scale value.
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState()),
            // intentional: 6dp tight inter-chip gap (between XS=4dp and SM=8dp).
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listings.forEach { listing ->
                FilterChip(
                    selected = listing.id == selectedId,
                    onClick = { onSelect(listing.id) },
                    enabled = enabled,
                    label = { Text(listing.title) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OfferSubmittedContent(
    requesterName: String?,
    onDone: () -> Unit,
    onViewMyOffers: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val displayName = requesterName?.takeIf { it.isNotBlank() } ?: "The requester"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter, vertical = PaceDreamSpacing.MD)
            .testTag(OfferSheetTestTags.Success),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        Box(
            modifier = Modifier
                // intentional: 56dp success badge — a fixed component size.
                .size(56.dp)
                .clip(CircleShape)
                .background(SuccessBadgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = SuccessBadgeForeground,
                // intentional: 32dp check glyph centered in the 56dp badge.
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = "Offer sent",
            style = PaceDreamTypography.Title2.copy(fontFamily = paceDreamDisplayFontFamily),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$displayName will see your offer and reply in your inbox if they're interested.",
            style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (onViewMyOffers != null) {
            OutlinedButton(
                onClick = onViewMyOffers,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM2),
            ) {
                Text("View my offers")
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM2),
        ) {
            Text("Done")
        }
    }
}

/**
 * Test tags exposed so Espresso/Compose tests can pin to the form vs.
 * success layouts without depending on copy.
 */
object OfferSheetTestTags {
    const val Form = "offer_sheet_form"
    const val Success = "offer_sheet_success"
    const val MessageField = "offer_sheet_message_field"
    const val MessageCounter = "offer_sheet_message_counter"
    const val ExpiryRow = "offer_sheet_expiry_row"
    const val LinkListingRow = "offer_sheet_link_listing_row"
}

private val SuccessBadgeBackground = Color(0xFFE6F4EA)
private val SuccessBadgeForeground = Color(0xFF1E8E3E)
