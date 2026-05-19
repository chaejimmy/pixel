package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import com.shourov.apps.pacedream.designsystem.scrimOnImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

    // Cache the .take(6) slice so the inner rows iterate over a stable List
    // instance instead of allocating a fresh sub-list every recomposition.
    val displayDestinations = remember(destinations) { destinations.take(6) }

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
        Column(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG),
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
    val context = LocalContext.current

    // Memoize the Coil request and the scrim gradient — both are stable for
    // a given destination + size and would otherwise be reallocated whenever
    // the parent recomposes (e.g. when a sibling card animates).
    val imageRequest = remember(destination.imageUrl, context) {
        ImageRequest.Builder(context)
            .data(destination.imageUrl)
            .crossfade(200)
            .build()
    }
    val scrimBrush = remember(height) {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, scrimOnImage(0.55f)),
            startY = 0.4f * height.value,
        )
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            // Stable placeholder color: rendered before the AsyncImage commits
            // its first frame so the card never flashes empty / fully
            // transparent on slow networks.  Coil's ConstraintsSizeResolver
            // already sizes the bitmap to this Box's pixel size, so no
            // explicit ImageRequest.size() is needed.
            .background(PaceDreamGray100)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = destination.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush)
        )

        // Label
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(PaceDreamSpacing.SM2),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = destination.title,
                fontSize = if (isLarge) 18.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = OnBrandSurface,
                maxLines = 1,
            )
            destination.propertyCount?.let { count ->
                Text(
                    text = "$count properties",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnBrandSurface.copy(alpha = 0.85f),
                )
            }
        }
    }
}
