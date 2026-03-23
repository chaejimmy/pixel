package com.pacedream.app.feature.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamButtonHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onBookingClick: (String) -> Unit,
    viewModel: BookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Compact header card with gradient
        BookingsHeader()

        // Main content area
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PaceDreamColors.Primary)
                    }
                }

                uiState.error != null -> {
                    BookingsErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }

                uiState.bookings.isEmpty() -> {
                    BookingsEmptyState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = PaceDreamSpacing.MD,
                            vertical = PaceDreamSpacing.SM2
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
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

/**
 * Compact gradient header — shorter and tighter than the old hero banner.
 * Uses 20dp vertical padding instead of 32dp, Title2 instead of Title1.
 */
@Composable
private fun BookingsHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary,
                        PaceDreamColors.Primary.copy(alpha = 0.88f)
                    )
                ),
                shape = RoundedCornerShape(
                    bottomStart = PaceDreamRadius.LG,
                    bottomEnd = PaceDreamRadius.LG
                )
            )
            .padding(
                start = PaceDreamSpacing.MD,
                end = PaceDreamSpacing.MD,
                top = PaceDreamSpacing.LG,
                bottom = PaceDreamSpacing.MD
            )
    ) {
        Column {
            Text(
                text = "My Bookings",
                style = PaceDreamTypography.Title2,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Manage your reservations",
                style = PaceDreamTypography.Subheadline,
                color = Color.White.copy(alpha = 0.80f)
            )
        }
    }
}

/**
 * Empty state — positioned in upper-center rather than dead-center
 * so the screen feels intentional, not hollow.
 */
@Composable
private fun BookingsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))

        Icon(
            imageVector = PaceDreamIcons.CalendarToday,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(PaceDreamIconSize.XXL)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = "No bookings yet",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = "Your reservations will appear here\nonce you book a stay.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Error state with retry — positioned like the empty state.
 */
@Composable
private fun BookingsErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))

        Icon(
            imageVector = PaceDreamIcons.ErrorOutline,
            contentDescription = null,
            tint = PaceDreamColors.Error,
            modifier = Modifier.size(PaceDreamIconSize.XXL)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = "Something went wrong",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = message,
            color = PaceDreamColors.TextSecondary,
            style = PaceDreamTypography.Body,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            modifier = Modifier.height(PaceDreamButtonHeight.MD)
        ) {
            Text("Retry", style = PaceDreamTypography.Button, color = Color.White)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = item.title,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val line = listOfNotNull(item.date, item.startTime, item.endTime)
                .joinToString(" · ")
                .takeIf { it.isNotBlank() }
            if (line != null) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
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
