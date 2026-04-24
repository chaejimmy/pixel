package com.shourov.apps.pacedream.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * A theme-aware color set for a marketplace category. Each entry carries a
 * [tint] (primary color used for icons / accents), [onTint] (content color
 * drawn on top of a solid tint), and an optional gradient pair
 * ([gradientStart] / [gradientEnd]) used where Home renders hero gradients.
 *
 * Light and dark values are specified independently. Dark values are not
 * simple alpha adjustments of light values — they are parallel palette
 * shifts (e.g. Tailwind 500 → 400) so categories remain legible on a dark
 * background.
 */
@Immutable
data class CategoryColor internal constructor(
    private val lightTint: Color,
    private val darkTint: Color,
    private val lightOnTint: Color,
    private val darkOnTint: Color,
    private val lightGradientStart: Color?,
    private val darkGradientStart: Color?,
    private val lightGradientEnd: Color?,
    private val darkGradientEnd: Color?,
) {
    val tint: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) darkTint else lightTint

    val onTint: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) darkOnTint else lightOnTint

    val gradientStart: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            darkGradientStart ?: darkTint
        } else {
            lightGradientStart ?: lightTint
        }

    val gradientEnd: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            darkGradientEnd ?: darkTint
        } else {
            lightGradientEnd ?: lightTint
        }

    val gradient: List<Color>
        @Composable
        @ReadOnlyComposable
        get() = listOf(gradientStart, gradientEnd)
}

private fun categoryColor(
    lightTint: Color,
    darkTint: Color,
    lightOnTint: Color = Color.White,
    darkOnTint: Color = Color.White,
    lightGradientStart: Color? = null,
    darkGradientStart: Color? = null,
    lightGradientEnd: Color? = null,
    darkGradientEnd: Color? = null,
): CategoryColor = CategoryColor(
    lightTint = lightTint,
    darkTint = darkTint,
    lightOnTint = lightOnTint,
    darkOnTint = darkOnTint,
    lightGradientStart = lightGradientStart,
    darkGradientStart = darkGradientStart,
    lightGradientEnd = lightGradientEnd,
    darkGradientEnd = darkGradientEnd,
)

/**
 * Named palette for marketplace categories. Feature screens should reference
 * entries here instead of hardcoding hex literals so that categories stay
 * consistent across surfaces and flip correctly in dark mode.
 */
object CategoryColors {
    // ─── Space categories ───────────────────────────────────────────────────
    val Restroom: CategoryColor = categoryColor(
        lightTint = Color(0xFF3B82F6), // blue-500
        darkTint = Color(0xFF60A5FA),  // blue-400
    )

    val NapPod: CategoryColor = categoryColor(
        lightTint = Color(0xFF8B5CF6), // violet-500
        darkTint = Color(0xFFA78BFA),  // violet-400
    )

    val MeetingRoom: CategoryColor = categoryColor(
        lightTint = Color(0xFF8B5CF6), // violet-500
        darkTint = Color(0xFFA78BFA),  // violet-400
    )

    val StudyRoom: CategoryColor = categoryColor(
        lightTint = Color(0xFF5527D7), // deep indigo
        darkTint = Color(0xFF7C5CE7),  // indigo-400
    )

    /** Short-stay / time-based rentals. */
    val ShortStay: CategoryColor = categoryColor(
        lightTint = Color(0xFF5527D7),
        darkTint = Color(0xFF7C5CE7),
    )

    /** Full-apartment rentals. Also backs the "Spaces" browse gradient. */
    val Apartment: CategoryColor = categoryColor(
        lightTint = Color(0xFF5527D7),
        darkTint = Color(0xFF7C5CE7),
        lightGradientStart = Color(0xFF5527D7),
        lightGradientEnd = Color(0xFF7C5CE7),
        darkGradientStart = Color(0xFF7C5CE7),
        darkGradientEnd = Color(0xFFA78BFA),
    )

    val LuxuryRoom: CategoryColor = categoryColor(
        lightTint = Color(0xFFEC4899), // pink-500
        darkTint = Color(0xFFF472B6),  // pink-400
    )

    val Parking: CategoryColor = categoryColor(
        lightTint = Color(0xFFF59E0B), // amber-500
        darkTint = Color(0xFFFBBF24),  // amber-400
    )

    val StorageSpace: CategoryColor = categoryColor(
        lightTint = Color(0xFF10B981), // emerald-500
        darkTint = Color(0xFF34D399),  // emerald-400
    )

    // ─── Extra category tokens used by Home ────────────────────────────────
    /** EV-charging / parking variant. */
    val EVParking: CategoryColor = categoryColor(
        lightTint = Color(0xFFEC4899),
        darkTint = Color(0xFFF472B6),
    )

    /** Co-working desks / workspace. */
    val Workspace: CategoryColor = categoryColor(
        lightTint = Color(0xFF5527D7),
        darkTint = Color(0xFF7C5CE7),
    )

    /** Rentable items / gear. Also backs the "Items" browse gradient. */
    val Items: CategoryColor = categoryColor(
        lightTint = Color(0xFF3B82F6),
        darkTint = Color(0xFF60A5FA),
        lightGradientStart = Color(0xFF3B82F6),
        lightGradientEnd = Color(0xFF60A5FA),
        darkGradientStart = Color(0xFF60A5FA),
        darkGradientEnd = Color(0xFF93C5FD),
    )

    /** On-demand services. Also backs the "Services" browse gradient. */
    val Services: CategoryColor = categoryColor(
        lightTint = Color(0xFF10B981),
        darkTint = Color(0xFF34D399),
        lightGradientStart = Color(0xFF10B981),
        lightGradientEnd = Color(0xFF34D399),
        darkGradientStart = Color(0xFF34D399),
        darkGradientEnd = Color(0xFF6EE7B7),
    )
}
