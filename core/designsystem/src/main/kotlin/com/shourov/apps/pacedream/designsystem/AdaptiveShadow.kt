package com.shourov.apps.pacedream.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Theme-aware card shadow. In light mode it draws a soft black shadow;
 * in dark mode it draws a much fainter white glow so cards still separate
 * from a dark background without the "solid black square" look that a
 * black shadow produces on a dark surface.
 *
 * The pressed variant boosts the spot-color alpha for interactive cards —
 * callers pass `pressed = true` while a press interaction is active.
 */
fun Modifier.adaptiveShadow(
    elevation: Dp,
    shape: Shape = RoundedCornerShape(16.dp),
    pressed: Boolean = false,
): Modifier = composed {
    val dark = isSystemInDarkTheme()
    val ambient = if (dark) OnDarkGlow.copy(alpha = 0.04f) else OnLightShadow.copy(alpha = 0.06f)
    val spot = if (dark) {
        OnDarkGlow.copy(alpha = if (pressed) 0.08f else 0.05f)
    } else {
        OnLightShadow.copy(alpha = if (pressed) 0.12f else 0.08f)
    }
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambient,
        spotColor = spot,
    )
}

/**
 * Semantic color for text/icons rendered on top of the brand gradient hero
 * or on top of photographic imagery where the underlying surface is
 * guaranteed to be dark/coloured in both light and dark mode. Unlike
 * [MaterialTheme.colorScheme.onSurface] this does NOT flip with the theme.
 */
val OnBrandSurface: Color = Color(0xFFFFFFFF)

/** Shadow base colour used by [adaptiveShadow] in light mode. */
internal val OnLightShadow: Color = Color(0xFF000000)

/** Glow base colour used by [adaptiveShadow] in dark mode. */
internal val OnDarkGlow: Color = Color(0xFFFFFFFF)

/**
 * Returns a translucent scrim colour suitable for laying over photographic
 * content. In light mode it's a light-black scrim; in dark mode the
 * scrim is toned down so it doesn't turn the image into a black rectangle.
 */
@Composable
fun scrimOnImage(lightAlpha: Float): Color {
    val dark = isSystemInDarkTheme()
    val alpha = if (dark) (lightAlpha * 0.7f).coerceIn(0f, 1f) else lightAlpha
    return OnLightShadow.copy(alpha = alpha)
}

/**
 * A translucent light tint for pills / badges placed on top of an image
 * (e.g. a "Space" type badge on a listing photo). Stays white because the
 * underlying surface is always an image, independent of theme.
 */
@Composable
fun badgeOnImageColor(alpha: Float = 0.95f): Color = OnBrandSurface.copy(alpha = alpha)
