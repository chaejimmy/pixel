package com.shourov.apps.pacedream.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Circular favourite/heart toggle used on top of imagery (e.g. listing cards).
 *
 * Announces "Add to favorites" / "Remove from favorites" based on [isFavorite]
 * so TalkBack users know both the current state and what tapping will do.
 * The icon is sized proportionally to the Surface so callers control the
 * visual footprint via [modifier] (apply `.size(...)` and any alignment).
 */
@Composable
fun FavoriteIconButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onToggle,
        modifier = modifier,
        shape = CircleShape,
        color = scrimOnImage(0.30f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) MaterialTheme.colorScheme.error else OnBrandSurface,
                modifier = Modifier.fillMaxSize(0.53f),
            )
        }
    }
}
