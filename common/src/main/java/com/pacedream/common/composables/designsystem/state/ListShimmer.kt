package com.pacedream.common.composables.designsystem.state

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing

/**
 * Vertical list of skeleton cards for list-style loading states.
 *
 * Reuses the same animated shimmer brush as Home's `ShimmerCard` so the
 * loading shimmer reads consistently across the app. Renders [count] card
 * skeletons that roughly match a text-row card (title + subtitle + meta).
 */
@Composable
fun ListShimmer(
    modifier: Modifier = Modifier,
    count: Int = 4,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
    ) {
        repeat(count) {
            ShimmerListCard()
        }
    }
}

@Composable
private fun ShimmerListCard() {
    // Same animated horizontal sweep used by HomeScreen's ShimmerCard.
    val transition = rememberInfiniteTransition(label = "listShimmer")
    val shimmerX = transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "listShimmerX"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            PaceDreamColors.ShimmerHighlight,
            PaceDreamColors.ShimmerBase,
            PaceDreamColors.ShimmerHighlight,
        ),
        start = Offset(shimmerX.value, 0f),
        end = Offset(shimmerX.value + 300f, 0f)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            // Tag pill
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
            // Title line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            // Subtitle line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
            // Meta row
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(shimmerBrush)
            )
        }
    }
}
