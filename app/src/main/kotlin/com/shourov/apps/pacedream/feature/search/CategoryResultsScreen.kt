package com.shourov.apps.pacedream.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryResultsScreen(
    category: String,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryResultsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(category) {
        viewModel.start(category)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Category", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaceDreamColors.Background),
                contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXXL),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                item {
                    HeroHeader(category)
                }

                if (state.isLoading && state.items.isEmpty()) {
                    items(8) { _ ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG),
                            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .background(PaceDreamColors.Border.copy(alpha = 0.25f), RoundedCornerShape(PaceDreamRadius.MD))
                            )
                        }
                    }
                } else if (state.errorMessage != null && state.items.isEmpty()) {
                    item {
                        ErrorState(
                            message = state.errorMessage ?: "Failed to load",
                            onRetry = { viewModel.refresh() }
                        )
                    }
                } else if (state.items.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(state.items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG),
                            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            onClick = { onListingClick(item.id) }
                        ) {
                            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                                Text(item.title, style = PaceDreamTypography.Headline, fontWeight = FontWeight.SemiBold)
                                item.location?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(it, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                                }
                                item.priceText?.let {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(it, style = PaceDreamTypography.Caption, color = PaceDreamColors.Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (state.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaceDreamSpacing.MD),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PaceDreamColors.Primary, modifier = Modifier.size(22.dp))
                            }
                            LaunchedEffect(Unit) { viewModel.loadMoreIfNeeded() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(category: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary,
                        PaceDreamColors.Primary.copy(alpha = 0.65f)
                    )
                )
            )
            .padding(PaceDreamSpacing.LG),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                text = category,
                style = PaceDreamTypography.Title1,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Explore listings",
                style = PaceDreamTypography.Body,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PaceDreamIcons.Search, contentDescription = null, tint = PaceDreamColors.TextSecondary)
            Spacer(modifier = Modifier.size(PaceDreamSpacing.SM))
            Text("No results. Pull to refresh.", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = PaceDreamTypography.Body, color = PaceDreamColors.Error)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
            ) {
                Text("Retry")
            }
        }
    }
}

