package com.shourov.apps.pacedream.feature.wanted.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.designsystem.modifier.shimmer
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.shourov.apps.pacedream.designsystem.modifier.adaptiveShadow

/**
 * Single shimmering placeholder block. Sized by the caller, clipped to the
 * shared small radius, and painted with the app-wide [shimmer] sweep so every
 * skeleton box reads identically to Search & Bookings.
 */
@Composable
private fun ShimmerBlock(
    modifier: Modifier = Modifier,
    height: Dp,
    width: Dp? = null,
    widthFraction: Float = 1f,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth(widthFraction))
            .height(height)
            .clip(RoundedCornerShape(PaceDreamRadius.XS))
            .shimmer(),
    )
}

/**
 * Loading placeholder for a [RequestCard]. Mirrors the real card's shape,
 * shadow, border and 16dp padding, with shimmering blocks standing in for the
 * tag, the two-line title, a description line and the trailing budget chip.
 *
 * The whole card is wrapped in [clearAndSetSemantics] so TalkBack skips the
 * empty placeholder boxes instead of announcing a row of blank nodes.
 */
@Composable
fun RequestCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .adaptiveShadow(
                elevation = PaceDreamElevation.SM,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            )
            .clearAndSetSemantics { },
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, PaceDreamColors.Border.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Tag + category row.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShimmerBlock(width = 56.dp, height = 20.dp)
                ShimmerBlock(width = 88.dp, height = 14.dp)
            }
            // Title — two lines.
            ShimmerBlock(height = 18.dp)
            ShimmerBlock(widthFraction = 0.6f, height = 18.dp)
            // Description line.
            ShimmerBlock(widthFraction = 0.8f, height = 14.dp)
            // Budget chip, trailing.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ShimmerBlock(width = 72.dp, height = 20.dp)
            }
        }
    }
}

/**
 * Loading placeholder for `RequestDetailBody`. Echoes the detail layout: the
 * 200dp hero image, the tag row, the title bar, two detail rows and three
 * description lines. Wrapped in [clearAndSetSemantics] so TalkBack ignores the
 * placeholder blocks.
 */
@Composable
fun RequestDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.MD)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        // Hero image block.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                .shimmer(),
        )
        // Tag row.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            ShimmerBlock(width = 56.dp, height = 20.dp)
            ShimmerBlock(width = 96.dp, height = 14.dp)
        }
        // Title bar.
        ShimmerBlock(widthFraction = 0.8f, height = 28.dp)
        // Two detail rows.
        ShimmerBlock(widthFraction = 0.5f, height = 16.dp)
        ShimmerBlock(widthFraction = 0.45f, height = 16.dp)
        // Description — three lines.
        ShimmerBlock(height = 14.dp)
        ShimmerBlock(height = 14.dp)
        ShimmerBlock(widthFraction = 0.7f, height = 14.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews — light & dark. Confirm the skeletons read on the migrated tokens in
// both themes without standing up a Hilt graph.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RequestCardSkeletonPreviewBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        repeat(3) { RequestCardSkeleton() }
    }
}

@Preview(name = "RequestCardSkeleton Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestCardSkeletonLightPreview() {
    PaceDreamTheme(darkTheme = false) { RequestCardSkeletonPreviewBody() }
}

@Preview(
    name = "RequestCardSkeleton Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestCardSkeletonDarkPreview() {
    PaceDreamTheme(darkTheme = true) { RequestCardSkeletonPreviewBody() }
}

@Preview(name = "RequestDetailSkeleton Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestDetailSkeletonLightPreview() {
    PaceDreamTheme(darkTheme = false) { RequestDetailSkeleton() }
}

@Preview(
    name = "RequestDetailSkeleton Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestDetailSkeletonDarkPreview() {
    PaceDreamTheme(darkTheme = true) { RequestDetailSkeleton() }
}
