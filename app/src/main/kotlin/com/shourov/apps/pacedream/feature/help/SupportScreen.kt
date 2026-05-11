package com.shourov.apps.pacedream.feature.help

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.help.chat.SupportCategory
import com.shourov.apps.pacedream.feature.help.chat.toSupportCategory
import kotlinx.coroutines.launch

// MARK: - Help Center Category Model

/**
 * Mobile-native Help Center categories. Each routes to a placeholder bottom
 * sheet for now; existing flows (inbox, payments support, safety report) can
 * be wired in later without changing call-sites.
 */
enum class HelpCenterCategory(
    val key: String,
    val title: String,
    val subtitle: String,
) {
    MessageHost(
        key = "message_host",
        title = "Message host about a booking",
        subtitle = "Open a conversation about an active booking.",
    ),
    PaymentsRefunds(
        key = "payments_refunds",
        title = "Payments & refunds",
        subtitle = "Charges, receipts, and refund status.",
    ),
    BookingIssues(
        key = "booking_issues",
        title = "Booking issues",
        subtitle = "Cancellations, dates, and listing problems.",
    ),
    AccountHelp(
        key = "account_help",
        title = "Account help",
        subtitle = "Sign-in, verification, and profile.",
    ),
    SafetyConcern(
        key = "safety_concern",
        title = "Safety concern",
        subtitle = "Report a safety or trust concern.",
    ),
    HelpArticles(
        key = "help_articles",
        title = "Help articles",
        subtitle = "Browse guides and frequently asked questions.",
    );
}

private val HelpCenterCategory.icon
    get() = when (this) {
        HelpCenterCategory.MessageHost -> PaceDreamIcons.Email
        HelpCenterCategory.PaymentsRefunds -> PaceDreamIcons.Settings
        HelpCenterCategory.BookingIssues -> PaceDreamIcons.CalendarToday
        HelpCenterCategory.AccountHelp -> PaceDreamIcons.Person
        HelpCenterCategory.SafetyConcern -> PaceDreamIcons.Security
        HelpCenterCategory.HelpArticles -> PaceDreamIcons.Help
    }

/**
 * Calm, accessible colors. Safety uses a muted amber rather than alert-red so
 * the entry point reads as supportive, not alarming.
 */
private val HelpCenterCategory.tint
    get() = when (this) {
        HelpCenterCategory.MessageHost -> PaceDreamColors.Info
        HelpCenterCategory.PaymentsRefunds -> PaceDreamColors.Success
        HelpCenterCategory.BookingIssues -> PaceDreamColors.Warning
        HelpCenterCategory.AccountHelp -> PaceDreamColors.Primary
        HelpCenterCategory.SafetyConcern -> Color(0xFFD97442)
        HelpCenterCategory.HelpArticles -> PaceDreamColors.Info
    }

// MARK: - Help Center Analytics

/**
 * Lightweight analytics shim — logs to logcat only. No third-party dependency
 * is introduced; a real analytics provider can be plugged in by replacing the
 * body of [log].
 */
object HelpCenterAnalytics {
    private const val TAG = "HelpCenter"

    sealed interface Event {
        val name: String
        val properties: Map<String, String>

        data object Opened : Event {
            override val name = "help_center_opened"
            override val properties: Map<String, String> = emptyMap()
        }

        data class CategoryTapped(val category: HelpCenterCategory) : Event {
            override val name = "help_category_tapped"
            override val properties: Map<String, String>
                get() = mapOf("category" to category.key)
        }

        data class ContextualTapped(val source: String) : Event {
            override val name = "contextual_help_tapped"
            override val properties: Map<String, String>
                get() = mapOf("source" to source)
        }
    }

    fun log(event: Event) {
        Log.d(TAG, "${event.name} ${event.properties}")
    }
}

// MARK: - Help Center Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    onFaqClick: () -> Unit,
    onSupportChatClick: (category: SupportCategory, source: String) -> Unit = { _, _ -> },
    onPostRequestClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf<HelpCenterCategory?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { HelpCenterAnalytics.log(HelpCenterAnalytics.Event.Opened) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help Center",
                        style = PaceDreamTypography.Headline,
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
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
        ) {
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = "How can we help?",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = "Chat with us anytime — our assistant answers instantly and a teammate joins when needed.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            // Primary entry — real-time support chat. Sits above the category
            // list so users always have a one-tap path to a human regardless
            // of which topic they pick.
            item {
                SupportChatPrimaryCard(
                    onClick = {
                        HelpCenterAnalytics.log(
                            HelpCenterAnalytics.Event.ContextualTapped("help_center_primary")
                        )
                        onSupportChatClick(SupportCategory.General, "help_center_primary")
                    },
                )
            }

            items(HelpCenterCategory.entries) { category ->
                HelpCenterCategoryCard(
                    category = category,
                    onClick = {
                        HelpCenterAnalytics.log(HelpCenterAnalytics.Event.CategoryTapped(category))
                        selectedCategory = category
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                Text(
                    text = "Other support options",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            // FAQ Card
            item {
                SupportOptionCard(
                    icon = PaceDreamIcons.Help,
                    title = "FAQ",
                    subtitle = "Find answers to common questions",
                    onClick = onFaqClick,
                )
            }

            // Email Support Card
            item {
                SupportOptionCard(
                    icon = PaceDreamIcons.Email,
                    title = "Email Support",
                    subtitle = "support@pacedream.com",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@pacedream.com")
                            putExtra(Intent.EXTRA_SUBJECT, "PaceDream Support Request")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // No email client available
                        }
                    },
                )
            }

            // Report a Problem
            item {
                SupportOptionCard(
                    icon = PaceDreamIcons.Warning,
                    title = "Report a Problem",
                    subtitle = "Let us know about any issues you've encountered",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@pacedream.com")
                            putExtra(Intent.EXTRA_SUBJECT, "PaceDream Bug Report")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // No email client available
                        }
                    },
                )
            }

            // Safety Information
            item {
                SupportOptionCard(
                    icon = PaceDreamIcons.Security,
                    title = "Safety Information",
                    subtitle = "Learn about our safety features and guidelines",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/safety"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Could not open URL
                        }
                    },
                )
            }

            // Post a Request — web-parity discovery CTA. Users who arrive in
            // the Help Center often can't find what they're looking for; this
            // lets them broadcast their need to the marketplace instead of
            // bouncing.
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                com.shourov.apps.pacedream.feature.wanted.presentation.PostRequestEntryButton(
                    source = "help_center_footer",
                    style = com.shourov.apps.pacedream.feature.wanted.presentation.PostRequestEntryStyle.SoftCard,
                    onClick = onPostRequestClick,
                )
            }

            // Response time notice
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                HorizontalDivider(
                    color = PaceDreamColors.Border,
                    thickness = 0.5.dp,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                Text(
                    text = "We aim to respond within 24 hours",
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextTertiary,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            }
        }
    }

    val category = selectedCategory
    if (category != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCategory = null },
            sheetState = sheetState,
            containerColor = PaceDreamColors.Background,
        ) {
            HelpCenterCategorySheetContent(
                category = category,
                onChatWithSupport = {
                    scope.launch { sheetState.hide() }
                    val resolvedCategory = category.toSupportCategory()
                    selectedCategory = null
                    HelpCenterAnalytics.log(
                        HelpCenterAnalytics.Event.ContextualTapped(
                            "help_center_category_${category.key}"
                        )
                    )
                    onSupportChatClick(
                        resolvedCategory,
                        "help_center_category_${category.key}",
                    )
                },
                onEmailSupport = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@pacedream.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Help: ${category.title}")
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                },
                onVisitWebHelpCenter = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/help")))
                    } catch (_: Exception) {}
                },
                onClose = {
                    scope.launch { sheetState.hide() }
                    selectedCategory = null
                },
            )
        }
    }
}

// MARK: - Help Center Category Card

@Composable
private fun HelpCenterCategoryCard(
    category: HelpCenterCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = category.tint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXS))
                Text(
                    text = category.subtitle,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary,
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// MARK: - Help Center Category Sheet (Placeholder)

@Composable
private fun HelpCenterCategorySheetContent(
    category: HelpCenterCategory,
    onChatWithSupport: () -> Unit,
    onEmailSupport: () -> Unit,
    onVisitWebHelpCenter: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .padding(bottom = PaceDreamSpacing.XL),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = category.tint,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = category.title,
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = category.subtitle,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        // Primary action: jump straight into the support chat with this
        // category preselected.
        androidx.compose.material3.Button(
            onClick = onChatWithSupport,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary,
                contentColor = PaceDreamColors.OnPrimary,
            ),
        ) {
            Icon(
                imageVector = PaceDreamIcons.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = "Chat with support",
                style = PaceDreamTypography.Headline,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = "Our assistant replies right away. Ask for a human anytime.",
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        TextButton(
            onClick = onEmailSupport,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = PaceDreamIcons.Email,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = "Email Support",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        TextButton(
            onClick = onVisitWebHelpCenter,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = PaceDreamIcons.Help,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = "Visit Help Center on web",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Close",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextSecondary,
            )
        }
    }
}

// MARK: - Primary Support Chat Card

/**
 * Featured row that opens the live support chat. Visually distinct from the
 * generic category and "other option" rows so users can spot it at a glance.
 */
@Composable
private fun SupportChatPrimaryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Primary.copy(alpha = 0.10f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PaceDreamIcons.Chat,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Chat with PaceDream Support",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    Text(
                        text = "LIVE",
                        style = PaceDreamTypography.Caption2,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                color = PaceDreamColors.Success,
                                shape = RoundedCornerShape(PaceDreamRadius.Round),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXS))
                Text(
                    text = "Real-time help — assistant + human teammates.",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary,
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// MARK: - Generic Support Option Card

@Composable
private fun SupportOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXS))
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary,
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
