package com.shourov.apps.pacedream.feature.help

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    onFaqClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & Support",
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
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
        ) {
            item {
                Text(
                    text = "How can we help?",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = "Find answers or get in touch with our team.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary,
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
}

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
