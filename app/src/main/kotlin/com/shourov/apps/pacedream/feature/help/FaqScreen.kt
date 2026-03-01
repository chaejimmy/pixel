package com.shourov.apps.pacedream.feature.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamEasing
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

/**
 * Data class representing a single FAQ entry with a question and answer.
 */
data class FaqItem(
    val question: String,
    val answer: String,
)

private val faqItems = listOf(
    FaqItem(
        question = "How does hourly booking work?",
        answer = "Our platform allows you to book spaces by the hour. Simply search for available " +
            "spaces, select your preferred time slot, and complete the booking. You'll receive a " +
            "confirmation with access details.",
    ),
    FaqItem(
        question = "What types of spaces can I book?",
        answer = "We offer a variety of spaces including meeting rooms, nap pods, study rooms, " +
            "workspaces, parking spots, storage spaces, apartments, and luxury rooms. Each space " +
            "is vetted for quality and comfort.",
    ),
    FaqItem(
        question = "Is there a cancellation policy?",
        answer = "Yes, our cancellation policy varies by listing. Most spaces offer free " +
            "cancellation up to 24 hours before your booking. Check the specific listing for " +
            "detailed cancellation terms.",
    ),
    FaqItem(
        question = "How do I contact support?",
        answer = "You can reach our support team through the app by navigating to " +
            "Profile > Help & Support, or by emailing support@pacedream.com. We aim to respond " +
            "within 24 hours.",
    ),
    FaqItem(
        question = "Are there any membership benefits?",
        answer = "Currently, all users have equal access to our platform features. We're working " +
            "on a membership program that will offer exclusive benefits, early access, and " +
            "discounts.",
    ),
    FaqItem(
        question = "How do I become a host?",
        answer = "Becoming a host is easy! Go to your Profile, tap 'Switch to Host Mode', and " +
            "follow the prompts to list your space. You'll need to provide details about your " +
            "space, set pricing, and upload photos.",
    ),
)

/**
 * 200ms duration for expand/collapse animations, matching the iOS design spec.
 */
private const val FAQ_ANIMATION_DURATION_MS = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FAQ",
                        style = PaceDreamTypography.Title2,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background,
                ),
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            item {
                Text(
                    text = "Frequently Asked Questions",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = "Find answers to common questions about PaceDream.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            }

            itemsIndexed(faqItems, key = { _, item -> item.question }) { _, item ->
                FaqExpandableCard(item = item)
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            }
        }
    }
}

/**
 * A single expandable FAQ card with a rotating chevron icon.
 *
 * Uses a 200ms easeInOut animation for both the chevron rotation and the
 * content expand/collapse transition to match the iOS design.
 */
@Composable
private fun FaqExpandableCard(
    item: FaqItem,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = FAQ_ANIMATION_DURATION_MS,
            easing = PaceDreamEasing.EaseInOut,
        ),
        label = "faq_chevron_rotation",
    )

    val expandAnimationSpec = tween<Float>(
        durationMillis = FAQ_ANIMATION_DURATION_MS,
        easing = PaceDreamEasing.EaseInOut,
    )

    val expandIntAnimationSpec = tween<Int>(
        durationMillis = FAQ_ANIMATION_DURATION_MS,
        easing = PaceDreamEasing.EaseInOut,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Question row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(PaceDreamSpacing.MD),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.question,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = PaceDreamIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation),
                )
            }

            // Answer (expandable)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(expandAnimationSpec) + expandVertically(expandIntAnimationSpec),
                exit = fadeOut(expandAnimationSpec) + shrinkVertically(expandIntAnimationSpec),
            ) {
                Column {
                    HorizontalDivider(
                        color = PaceDreamColors.Border,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
                    )
                    Text(
                        text = item.answer,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                        modifier = Modifier.padding(PaceDreamSpacing.MD),
                    )
                }
            }
        }
    }
}
