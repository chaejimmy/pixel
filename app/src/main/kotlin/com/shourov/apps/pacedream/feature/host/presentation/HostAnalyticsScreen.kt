package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostAnalyticsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PaceDreamColors.Background)
                .padding(PaceDreamSpacing.LG),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                Text(
                    text = "Analytics Dashboard",
                    style = PaceDreamTypography.Title2,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Coming Soon",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = "Track your performance with detailed analytics and insights",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
    }
}
