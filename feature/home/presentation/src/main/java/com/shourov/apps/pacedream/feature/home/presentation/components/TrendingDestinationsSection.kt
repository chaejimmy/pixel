package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.*

/**
 * Trending Destinations grid matching iOS TrendingDestinationsSection.swift
 *
 * 2-column grid with gradient overlays and text labels.
 * First 2 cards are taller (200dp), remaining are shorter (150dp).
 */

data class TrendingDestination(
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String,
    val propertyCount: Int? = null,
)

@Composable
fun TrendingDestinationsSection(
    destinations: List<TrendingDestination>,
    onDestinationClick: (TrendingDestination) -> Unit = {},
    onViewAllClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Section header — uses shared component for consistency
        SectionHeader(
            title = "Trending Destinations",
            subtitle = "Popular places our community loves",
            onViewAllClick = onViewAllClick,
        )

        // 2-column grid
        val displayDestinations = destinations.take(6)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (i in displayDestinations.indices step 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TrendingDestinationCard(
                        destination = displayDestinations[i],
                        isLarge = i < 2,
                        onClick = { onDestinationClick(displayDestinations[i]) },
                        modifier = Modifier.weight(1f),
                    )
                    if (i + 1 < displayDestinations.size) {
                        TrendingDestinationCard(
                            destination = displayDestinations[i + 1],
                            isLarge = i < 2,
                            onClick = { onDestinationClick(displayDestinations[i + 1]) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingDestinationCard(
    destination: TrendingDestination,
    isLarge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = if (isLarge) 170.dp else 130.dp

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = destination.imageUrl,
            contentDescription = destination.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        startY = 0.4f * height.value,
                    )
                )
        )

        // Label
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = destination.title,
                fontSize = if (isLarge) 18.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
            )
            destination.propertyCount?.let { count ->
                Text(
                    text = "$count properties",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
