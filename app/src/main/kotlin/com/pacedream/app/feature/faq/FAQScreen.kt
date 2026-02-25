package com.pacedream.app.feature.faq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * FAQ Screen with expandable items
 * Matches website FAQ section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val faqItems = remember {
        listOf(
            FAQItem(
                question = "How does hourly booking work?",
                answer = "Hourly booking allows you to book spaces for short periods, typically by the hour. Simply select your desired time slot, complete the booking, and enjoy flexible access to the space."
            ),
            FAQItem(
                question = "What types of spaces can I book?",
                answer = "You can book various types of spaces including entire homes, private rooms, nap rooms, meeting rooms, workspaces, restrooms, EV parking, nap pods, study rooms, storage spaces, parking spots, apartments, and luxury rooms. We offer flexible options for every need."
            ),
            FAQItem(
                question = "What are Last-Minute Deals?",
                answer = "Last-Minute Deals offer discounted rates on available spaces that haven't been booked. These deals are perfect for spontaneous needs and can save you 20-40% off regular prices. Deals are updated in real-time and can be booked instantly for immediate use."
            ),
            FAQItem(
                question = "How does the Roommate Finder work?",
                answer = "Our Roommate Finder helps you connect with potential roommates. You can post listings for available rooms or search for roommates based on preferences like location, budget, lifestyle, and move-in dates. The platform facilitates safe connections with verified users."
            ),
            FAQItem(
                question = "How do I send a proposal to a host?",
                answer = "On any listing detail page, tap the 'Propose' button to send a custom proposal to the host. You can include your preferred price, duration, and a message explaining your needs. The host will review and respond through the messaging system."
            ),
            FAQItem(
                question = "Is there a cancellation policy?",
                answer = "Yes, we have flexible cancellation policies. Most hourly rentals allow free cancellation up to 2 hours before the scheduled time. For roommate arrangements, cancellation policies are set by the host and clearly displayed in the listing details."
            ),
            FAQItem(
                question = "What is the cleaning fee?",
                answer = "Some listings include a one-time cleaning fee that is added to your booking total. The cleaning fee amount varies by listing and is shown in the price breakdown before you confirm your booking."
            ),
            FAQItem(
                question = "Are there weekly discounts?",
                answer = "Yes! Many hosts offer weekly discounts for longer bookings. When available, the discount percentage is shown in the price breakdown on the listing detail page. Discounts are automatically applied at checkout."
            ),
            FAQItem(
                question = "How do I contact support?",
                answer = "You can contact our support team 24/7 through the app's messaging feature, or send us a direct email. Our support team is always ready to help with any questions or issues you may have."
            ),
            FAQItem(
                question = "How do I become a host?",
                answer = "Becoming a host is easy! Simply go to your profile, select 'Switch to Host Mode', and follow the simple onboarding process. You can list your space, set your availability, and start earning by sharing your space with others."
            ),
            FAQItem(
                question = "Can I rent gear and equipment?",
                answer = "Yes! PaceDream offers gear rentals by the hour for your adventures. Browse our gear categories including tech gear, music gear, photography equipment, and fashion items. Simply select what you need and book it instantly."
            )
        )
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Frequently Asked Questions",
                        style = PaceDreamTypography.Title2,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = PaceDreamSpacing.LG,
                vertical = PaceDreamSpacing.MD
            ),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(faqItems, key = { it.question }) { item ->
                ExpandableFAQItem(item = item)
            }
        }
    }
}

/**
 * Expandable FAQ Item Component
 */
@Composable
private fun ExpandableFAQItem(
    item: FAQItem,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Question Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(PaceDreamSpacing.MD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.question,
                    style = PaceDreamTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Icon(
                    imageVector = if (isExpanded) PaceDreamIcons.ExpandLess else PaceDreamIcons.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = PaceDreamColors.TextSecondary
                )
            }
            
            // Answer (Animated)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = PaceDreamSpacing.MD,
                            vertical = PaceDreamSpacing.SM
                        )
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = PaceDreamSpacing.SM),
                        color = PaceDreamColors.Border
                    )
                    Text(
                        text = item.answer,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                        lineHeight = PaceDreamTypography.Body.fontSize * 1.5
                    )
                }
            }
        }
    }
}

/**
 * FAQ Item Data Model
 */
data class FAQItem(
    val question: String,
    val answer: String
)
