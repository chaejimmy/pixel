package com.pacedream.common.composables.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.icon.PaceDreamIcons

/**
 * Shared Coil image wrapper. Handles the three non-happy paths uniformly so
 * callers never render a blank rectangle while the network is in flight or
 * after a failure:
 *  - null / blank URL → static placeholder
 *  - in-flight        → shimmer
 *  - error            → placeholder
 */
@Composable
fun PaceDreamImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIcon: ImageVector = PaceDreamIcons.Image,
    // Stable per-render-slot memory cache key. Defaults to null (Coil keys by
    // the request URL). Pass a distinct suffix per render context — e.g.
    // "$id@booking" vs "$id@card" — so the same URL decoded at two sizes
    // doesn't evict the other on cold scroll.
    cacheKey: String? = null,
) {
    if (url.isNullOrBlank()) {
        ImagePlaceholder(icon = placeholderIcon, modifier = modifier)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .memoryCacheKey(cacheKey)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { ImageShimmer(modifier = Modifier.fillMaxSize()) },
        error = { ImagePlaceholder(icon = placeholderIcon, modifier = Modifier.fillMaxSize()) },
    )
}

@Composable
private fun ImagePlaceholder(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(PaceDreamColors.Gray200.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.Gray400,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun ImageShimmer(modifier: Modifier = Modifier) {
    Box(modifier = modifier.shimmerEffect())
}
