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
    // Stable memory-cache key for this render slot. Pass a value derived from
    // the domain id plus a render-context suffix (e.g. "$id@booking") so the
    // same URL rendered at two different sizes caches under distinct keys and
    // the two don't evict each other on scroll. Null falls back to Coil's
    // default URL-based key.
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
            .apply { cacheKey?.let { memoryCacheKey(it) } }
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
