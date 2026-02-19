package com.shourov.apps.pacedream.feature.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

data class FaqItem(
    val question: String,
    val answer: String
)

private val faqItems = listOf(
    FaqItem(
        question = "How does hourly booking work?",
        answer = "Browse available spaces, select your preferred time slot, and book instantly. " +
            "You'll receive a confirmation with access details. Payment is processed at the time " +
            "of booking, and you can cancel up to 1 hour before your reservation."
    ),
    FaqItem(
        question = "What types of spaces can I book?",
        answer = "PaceDream offers a variety of spaces including meeting rooms, nap pods, " +
            "study rooms, restrooms, parking spots, EV charging stations, apartments, " +
            "luxury rooms, and storage spaces. You can also rent gear like tech equipment, " +
            "music gear, photography equipment, and fashion items."
    ),
    FaqItem(
        question = "Is there a cancellation policy?",
        answer = "Yes. Free cancellation is available up to 1 hour before your booking start time. " +
            "Cancellations made within 1 hour of the booking may incur a fee. " +
            "For split stays and longer bookings, cancellation policies may vary by host."
    ),
    FaqItem(
        question = "How do I contact support?",
        answer = "You can reach our support team through the app by going to Profile > Help Center, " +
            "or by emailing support@pacedream.com. Our team typically responds within 24 hours."
    ),
    FaqItem(
        question = "Are there any membership benefits?",
        answer = "PaceDream members enjoy exclusive benefits including priority booking, " +
            "discounted rates on selected spaces, early access to new listings, and " +
            "special promotions. Create a free account to start enjoying these benefits."
    ),
    FaqItem(
        question = "How do I become a host?",
        answer = "Switch to Host Mode from your Profile screen. You can list your space, " +
            "set your availability and pricing, and start earning. PaceDream handles " +
            "payments and provides tools to manage your listings and bookings."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "FAQ",
                    style = PaceDreamTypography.Title2,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PaceDreamColors.Background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PaceDreamSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            item {
                Text(
                    text = "Frequently Asked Questions",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = "Find answers to common questions about PaceDream.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            }

            itemsIndexed(faqItems) { index, item ->
                FaqExpandableItem(
                    item = item,
                    showDivider = index < faqItems.size - 1
                )
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            }
        }
    }
}

@Composable
private fun FaqExpandableItem(
    item: FaqItem,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "faq_chevron_rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Question row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(PaceDreamSpacing.MD),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.question,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            // Answer (expandable)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column {
                    HorizontalDivider(
                        color = PaceDreamColors.Border,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    )
                    Text(
                        text = item.answer,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                        modifier = Modifier.padding(PaceDreamSpacing.MD)
                    )
                }
            }
        }
    }
}
