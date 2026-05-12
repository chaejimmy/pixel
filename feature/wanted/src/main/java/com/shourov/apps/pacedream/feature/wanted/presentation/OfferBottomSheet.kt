package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag(OfferSheetTestTags.Form),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Make an Offer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = state.price,
            onValueChange = onPriceChange,
            label = { Text("Price") },
            singleLine = true,
            prefix = { Text("$") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
        )
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = { Text("Message") },
            placeholder = { Text("Briefly describe your offer") },
            modifier = Modifier
                .fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            enabled = !state.submitting,
        )
        state.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = onSubmit,
            enabled = !state.submitting,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                )
            }
            Text(if (state.submitting) "Submitting…" else "Send offer")
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag(OfferSheetTestTags.Success),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SuccessBadgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = SuccessBadgeForeground,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = "Offer sent",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$displayName will see your offer and reply in your inbox if they're interested.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onViewMyOffers != null) {
            OutlinedButton(
                onClick = onViewMyOffers,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text("View my offers")
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
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
}

private val SuccessBadgeBackground = Color(0xFFE6F4EA)
private val SuccessBadgeForeground = Color(0xFF1E8E3E)
