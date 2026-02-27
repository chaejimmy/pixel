package com.pacedream.app.feature.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onBookingClick: (String) -> Unit,
    viewModel: BookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bookings",
                        style = PaceDreamTypography.Title2
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PaceDreamColors.Primary)
                    }
                }

                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = PaceDreamIcons.ErrorOutline,
                                contentDescription = null,
                                tint = PaceDreamColors.Error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            Text(
                                uiState.error!!,
                                color = PaceDreamColors.TextSecondary,
                                style = PaceDreamTypography.Body
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaceDreamColors.Primary
                                ),
                                shape = RoundedCornerShape(PaceDreamRadius.MD)
                            ) {
                                Text("Retry", style = PaceDreamTypography.Button)
                            }
                        }
                    }
                }

                uiState.bookings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = PaceDreamIcons.CalendarToday,
                                contentDescription = null,
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            Text(
                                "No bookings yet",
                                style = PaceDreamTypography.Title3,
                                color = PaceDreamColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                            Text(
                                "Your reservations will appear here",
                                color = PaceDreamColors.TextSecondary,
                                style = PaceDreamTypography.Body
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(PaceDreamSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        items(uiState.bookings, key = { it.id }) { booking ->
                            BookingCard(
                                item = booking,
                                onClick = { onBookingClick(booking.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(item: BookingListItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = item.title,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            val line = listOfNotNull(item.date, item.startTime, item.endTime).joinToString(" · ").takeIf { it.isNotBlank() }
            if (line != null) {
                Text(
                    line,
                    color = PaceDreamColors.TextSecondary,
                    style = PaceDreamTypography.Callout
                )
            }
            item.status?.let {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    it,
                    color = PaceDreamColors.TextSecondary,
                    style = PaceDreamTypography.Caption
                )
            }
        }
    }
}
