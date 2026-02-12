package com.pacedream.app.feature.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About PaceDream",
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
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(PaceDreamSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            item {
                Text(
                    text = "PaceDream",
                    style = PaceDreamTypography.LargeTitle,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                Text(
                    text = "Find Your Perfect Space",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.LG)
                    ) {
                        Text(
                            text = "Our Mission",
                            style = PaceDreamTypography.Headline,
                            fontWeight = FontWeight.Bold,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            text = "PaceDream is part of a growing community that values flexibility, convenience, and connectivity. Whether you're looking to manage living costs, find a temporary workspace, or grab that last-minute vacation deal, PaceDream is your go-to platform.",
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary,
                            lineHeight = PaceDreamTypography.Body.fontSize * 1.6
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.LG)
                    ) {
                        Text(
                            text = "What We Offer",
                            style = PaceDreamTypography.Headline,
                            fontWeight = FontWeight.Bold,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                        AboutFeatureItem(
                            title = "Hourly Spaces",
                            description = "Rent restrooms, meeting rooms, nap pods, study rooms, workspaces, and parking spots by the hour."
                        )
                        AboutFeatureItem(
                            title = "Find Roommates",
                            description = "Post listings or search for roommates by location, budget, lifestyle, and move-in dates."
                        )
                        AboutFeatureItem(
                            title = "Last-Minute Deals",
                            description = "Save 20-40% on available spaces with real-time pricing and instant booking."
                        )
                        AboutFeatureItem(
                            title = "Gear Rentals",
                            description = "Rent equipment and gear by the hour for your adventures."
                        )
                        AboutFeatureItem(
                            title = "Become a Host",
                            description = "List your space and earn by sharing it with others. Manage bookings, earnings, and analytics."
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.LG)
                    ) {
                        Text(
                            text = "Links",
                            style = PaceDreamTypography.Headline,
                            fontWeight = FontWeight.Bold,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                        AboutLinkItem(
                            icon = Icons.Default.Language,
                            title = "Website",
                            onClick = { uriHandler.openUri("https://www.pacedream.com") }
                        )
                        HorizontalDivider(color = PaceDreamColors.Border)
                        AboutLinkItem(
                            icon = Icons.Default.Description,
                            title = "Terms of Service",
                            onClick = { uriHandler.openUri("https://www.pacedream.com/terms") }
                        )
                        HorizontalDivider(color = PaceDreamColors.Border)
                        AboutLinkItem(
                            icon = Icons.Default.Policy,
                            title = "Privacy Policy",
                            onClick = { uriHandler.openUri("https://www.pacedream.com/privacy") }
                        )
                        HorizontalDivider(color = PaceDreamColors.Border)
                        AboutLinkItem(
                            icon = Icons.Default.Email,
                            title = "Contact Us",
                            onClick = { uriHandler.openUri("mailto:support@pacedream.com") }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                Text(
                    text = "Version 1.0.0",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = "Made with care by PaceDream, Inc.",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AboutFeatureItem(
    title: String,
    description: String
) {
    Column(modifier = Modifier.padding(vertical = PaceDreamSpacing.SM)) {
        Text(
            text = title,
            style = PaceDreamTypography.CalloutBold,
            color = PaceDreamColors.Primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary
        )
    }
}

@Composable
private fun AboutLinkItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
        Text(
            text = title,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text("Open", color = PaceDreamColors.Primary)
        }
    }
}
