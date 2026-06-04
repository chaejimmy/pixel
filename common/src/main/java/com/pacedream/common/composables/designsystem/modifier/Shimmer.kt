package com.pacedream.common.composables.designsystem.modifier

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.pacedream.common.composables.theme.PaceDreamColors

/**
 * Shared loading-shimmer fill for skeleton placeholders.
 *
 * Paints the receiver with the same animated horizontal sweep the rest of the
 * app uses for skeletons (Search's grid, the shared [ListShimmer], Home's
 * cards): a 1200ms left-to-right gradient over the `ShimmerHighlight →
 * ShimmerBase → ShimmerHighlight` tokens. Apply it after sizing/clipping the
 * box, e.g.
 *
 * ```
 * Box(
 *     Modifier
 *         .height(16.dp)
 *         .clip(RoundedCornerShape(PaceDreamRadius.XS))
 *         .shimmer(),
 * )
 * ```
 *
 * Reduced motion: when the system animator scale is 0 (Settings →
 * "Remove animations", or an explicit reduce-motion preference), the infinite
 * sweep is skipped and the box is filled statically with the base token so the
 * skeleton still reads as a placeholder without a moving gradient.
 */
@Composable
fun Modifier.shimmer(): Modifier {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
    }

    if (!animationsEnabled) {
        // Reduced-motion fallback: a flat fill, no infinite transition.
        return this.background(PaceDreamColors.ShimmerBase)
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            PaceDreamColors.ShimmerHighlight,
            PaceDreamColors.ShimmerBase,
            PaceDreamColors.ShimmerHighlight,
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 300f, 0f),
    )
    return this.background(brush)
}
