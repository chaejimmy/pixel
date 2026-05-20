package com.shourov.apps.pacedream.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color for text/icons rendered on top of the brand gradient hero
 * or on top of photographic imagery where the underlying surface is
 * guaranteed to be dark/coloured in both light and dark mode. Unlike
 * `MaterialTheme.colorScheme.onSurface` this does NOT flip with the theme.
 */
val OnBrandSurface: Color = Color(0xFFFFFFFF)

/**
 * Returns a translucent scrim colour suitable for laying over photographic
 * content. In light mode it's a light-black scrim; in dark mode the
 * scrim is toned down so it doesn't turn the image into a black rectangle.
 */
@Composable
fun scrimOnImage(lightAlpha: Float): Color {
    val dark = isSystemInDarkTheme()
    val alpha = if (dark) (lightAlpha * 0.7f).coerceIn(0f, 1f) else lightAlpha
    return Color(0xFF000000).copy(alpha = alpha)
}

/**
 * A translucent light tint for pills / badges placed on top of an image
 * (e.g. a "Space" type badge on a listing photo). Stays white because the
 * underlying surface is always an image, independent of theme.
 */
@Composable
fun badgeOnImageColor(alpha: Float = 0.95f): Color = OnBrandSurface.copy(alpha = alpha)
