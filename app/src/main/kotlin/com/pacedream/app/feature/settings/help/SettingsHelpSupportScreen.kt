package com.pacedream.app.feature.settings.help

import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
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
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHelpSupportScreen(
    onBackClick: () -> Unit,
    onOpenFaq: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Help & Support",
                        style = PaceDreamTypography.Headline
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            // Self-service section
            SectionLabel("Self-Service")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    HelpRow(
                        icon = PaceDreamIcons.HelpOutline,
                        title = "FAQ",
                        subtitle = "Browse frequently asked questions",
                        onClick = onOpenFaq
                    )

                    HorizontalDivider(
                        color = PaceDreamColors.Border,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    )

                    HelpRow(
                        icon = PaceDreamIcons.OpenInNew,
                        title = "Help Center",
                        subtitle = "Guides and articles on pacedream.com",
                        onClick = {
                            val url = "https://www.pacedream.com/help"
                            val customTabsIntent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            customTabsIntent.launchUrl(context, android.net.Uri.parse(url))
                        }
                    )
                }
            }

            // Contact section
            SectionLabel("Contact Us")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    HelpRow(
                        icon = PaceDreamIcons.Email,
                        title = "Email Support",
                        subtitle = "support@pacedream.com",
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_SENDTO,
                                android.net.Uri.parse("mailto:support@pacedream.com")
                            )
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(
                        color = PaceDreamColors.Border,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    )

                    HelpRow(
                        icon = PaceDreamIcons.Report,
                        title = "Report an Issue",
                        subtitle = "Let us know about bugs or problems",
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_SENDTO,
                                android.net.Uri.parse("mailto:support@pacedream.com?subject=Bug%20Report")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Legal section
            SectionLabel("Legal")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    HelpRow(
                        icon = PaceDreamIcons.Description,
                        title = "Terms of Service",
                        subtitle = "Review our terms and conditions",
                        onClick = {
                            val url = "https://www.pacedream.com/terms"
                            val customTabsIntent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            customTabsIntent.launchUrl(context, android.net.Uri.parse(url))
                        }
                    )

                    HorizontalDivider(
                        color = PaceDreamColors.Border,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    )

                    HelpRow(
                        icon = PaceDreamIcons.Policy,
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = {
                            val url = "https://www.pacedream.com/privacy"
                            val customTabsIntent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            customTabsIntent.launchUrl(context, android.net.Uri.parse(url))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = PaceDreamTypography.Caption,
        color = PaceDreamColors.TextTertiary,
        modifier = Modifier.padding(start = PaceDreamSpacing.XS)
    )
}

@Composable
private fun HelpRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }

        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
