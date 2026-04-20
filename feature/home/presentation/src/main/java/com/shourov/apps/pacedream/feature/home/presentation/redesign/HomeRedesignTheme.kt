package com.shourov.apps.pacedream.feature.home.presentation.redesign

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Home Redesign V1 — brand tokens
 *
 * Purple-forward palette (brandHue ≈ 268) with a coral accent (hue ≈ 22),
 * mirroring the design prototype's OKLCH ramp.
 */

@Immutable
data class BrandPalette(
    val c50: Color,
    val c100: Color,
    val c200: Color,
    val c300: Color,
    val c400: Color,
    val c500: Color,
    val c600: Color,
    val c700: Color,
    val c800: Color,
    val c900: Color,
)

object HomeRedesignTheme {
    val Purple = BrandPalette(
        c50  = Color(0xFFF5F1FB),
        c100 = Color(0xFFE8DFF6),
        c200 = Color(0xFFCFBDEC),
        c300 = Color(0xFFA885DE),
        c400 = Color(0xFF7F51CD),
        c500 = Color(0xFF5E2FBE),
        c600 = Color(0xFF5527D7),
        c700 = Color(0xFF3F1EA3),
        c800 = Color(0xFF2B1472),
        c900 = Color(0xFF1B0B4A),
    )

    val Coral = BrandPalette(
        c50  = Color(0xFFFFF3EC),
        c100 = Color(0xFFFDE0D0),
        c200 = Color(0xFFFAC1A4),
        c300 = Color(0xFFF19A73),
        c400 = Color(0xFFE57A4C),
        c500 = Color(0xFFDC6335),
        c600 = Color(0xFFC14E23),
        c700 = Color(0xFF9E3F1B),
        c800 = Color(0xFF7A3014),
        c900 = Color(0xFF5A220E),
    )

    // Paper / surface tokens (light mode only — dark support can be added later)
    val PaperBg = Color(0xFFF7F6F3)
    val PaperSurface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1A1522)
    val InkDim = Color(0xFF5B5568)
    val InkFaint = Color(0xFF8A8497)
    val Line = Color(0xFFE9E6EF)
    val LineSoft = Color(0xFFF2F0F5)

    // Star / rating
    val Star = Color(0xFFF5A623)

    // Rounded corner shapes — "soft" radius scale
    val RadiusXs = 8.dp
    val RadiusSm = 10.dp
    val RadiusMd = 14.dp
    val RadiusLg = 18.dp
    val RadiusXl = 24.dp
    val RadiusPill = 999.dp

    val CardShape = RoundedCornerShape(RadiusLg)
    val PillShape = RoundedCornerShape(RadiusPill)
    val SearchCardShape = RoundedCornerShape(RadiusLg)
    val HeroBottomShape = RoundedCornerShape(
        topStart = 0.dp, topEnd = 0.dp,
        bottomStart = 28.dp, bottomEnd = 28.dp,
    )
}
