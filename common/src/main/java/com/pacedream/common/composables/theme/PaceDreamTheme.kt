package com.pacedream.common.composables.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Light default theme color scheme
 */
@VisibleForTesting
val LightDefaultColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

/**
 * Dark default theme color scheme
 */
@VisibleForTesting
val DarkDefaultColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

/**
 * Light Android theme color scheme
 */
@VisibleForTesting
val LightAndroidColorScheme = lightColorScheme(
    primary = primaryLightAndroid,
    onPrimary = onPrimaryLightAndroid,
    primaryContainer = primaryContainerLightAndroid,
    onPrimaryContainer = onPrimaryContainerLightAndroid,
    secondary = secondaryLightAndroid,
    onSecondary = onSecondaryLightAndroid,
    secondaryContainer = secondaryContainerLightAndroid,
    onSecondaryContainer = onSecondaryContainerLightAndroid,
    tertiary = tertiaryLightAndroid,
    onTertiary = onTertiaryLightAndroid,
    tertiaryContainer = tertiaryContainerLightAndroid,
    onTertiaryContainer = onTertiaryContainerLightAndroid,
    error = errorLightAndroid,
    onError = onErrorLightAndroid,
    errorContainer = errorContainerLightAndroid,
    onErrorContainer = onErrorContainerLightAndroid,
    background = backgroundLightAndroid,
    onBackground = onBackgroundLightAndroid,
    surface = surfaceLightAndroid,
    onSurface = onSurfaceLightAndroid,
    surfaceVariant = surfaceVariantLightAndroid,
    onSurfaceVariant = onSurfaceVariantLightAndroid,
    outline = outlineLightAndroid,
    outlineVariant = outlineVariantLightAndroid,
    scrim = scrimLightAndroid,
    inverseSurface = inverseSurfaceLightAndroid,
    inverseOnSurface = inverseOnSurfaceLightAndroid,
    inversePrimary = inversePrimaryLightAndroid,
    surfaceDim = surfaceDimLightAndroid,
    surfaceBright = surfaceBrightLightAndroid,
    surfaceContainerLowest = surfaceContainerLowestLightAndroid,
    surfaceContainerLow = surfaceContainerLowLightAndroid,
    surfaceContainer = surfaceContainerLightAndroid,
    surfaceContainerHigh = surfaceContainerHighLightAndroid,
    surfaceContainerHighest = surfaceContainerHighestLightAndroid,
)

/**
 * Dark Android theme color scheme
 */
@VisibleForTesting
val DarkAndroidColorScheme = darkColorScheme(
    primary = primaryDarkAndroid,
    onPrimary = onPrimaryDarkAndroid,
    primaryContainer = primaryContainerDarkAndroid,
    onPrimaryContainer = onPrimaryContainerDarkAndroid,
    secondary = secondaryDarkAndroid,
    onSecondary = onSecondaryDarkAndroid,
    secondaryContainer = secondaryContainerDarkAndroid,
    onSecondaryContainer = onSecondaryContainerDarkAndroid,
    tertiary = tertiaryDarkAndroid,
    onTertiary = onTertiaryDarkAndroid,
    tertiaryContainer = tertiaryContainerDarkAndroid,
    onTertiaryContainer = onTertiaryContainerDarkAndroid,
    error = errorDarkAndroid,
    onError = onErrorDarkAndroid,
    errorContainer = errorContainerDarkAndroid,
    onErrorContainer = onErrorContainerDarkAndroid,
    background = backgroundDarkAndroid,
    onBackground = onBackgroundDarkAndroid,
    surface = surfaceDarkAndroid,
    onSurface = onSurfaceDarkAndroid,
    surfaceVariant = surfaceVariantDarkAndroid,
    onSurfaceVariant = onSurfaceVariantDarkAndroid,
    outline = outlineDarkAndroid,
    outlineVariant = outlineVariantDarkAndroid,
    scrim = scrimDarkAndroid,
    inverseSurface = inverseSurfaceDarkAndroid,
    inverseOnSurface = inverseOnSurfaceDarkAndroid,
    inversePrimary = inversePrimaryDarkAndroid,
    surfaceDim = surfaceDimDarkAndroid,
    surfaceBright = surfaceBrightDarkAndroid,
    surfaceContainerLowest = surfaceContainerLowestDarkAndroid,
    surfaceContainerLow = surfaceContainerLowDarkAndroid,
    surfaceContainer = surfaceContainerDarkAndroid,
    surfaceContainerHigh = surfaceContainerHighDarkAndroid,
    surfaceContainerHighest = surfaceContainerHighestDarkAndroid,
)


val extendedLight = ExtendedColorScheme(
    warning = ColorFamily(
        warningLight,
        onWarningLight,
        warningContainerLight,
        onWarningContainerLight,
    ),
    success = ColorFamily(
        successLight,
        onSuccessLight,
        successContainerLight,
        onSuccessContainerLight,
    ),
)

val extendedDark = ExtendedColorScheme(
    warning = ColorFamily(
        warningDark,
        onWarningDark,
        warningContainerDark,
        onWarningContainerDark,
    ),
    success = ColorFamily(
        successDark,
        onSuccessDark,
        successContainerDark,
        onSuccessContainerDark,
    ),
)

val extendedLightHighContrast = ExtendedColorScheme(
    warning = ColorFamily(
        warningLightHighContrast,
        onWarningLightHighContrast,
        warningContainerLightHighContrast,
        onWarningContainerLightHighContrast,
    ),
    success = ColorFamily(
        successLightHighContrast,
        onSuccessLightHighContrast,
        successContainerLightHighContrast,
        onSuccessContainerLightHighContrast,
    ),
)

val extendedDarkHighContrast = ExtendedColorScheme(
    warning = ColorFamily(
        warningDarkHighContrast,
        onWarningDarkHighContrast,
        warningContainerDarkHighContrast,
        onWarningContainerDarkHighContrast,
    ),
    success = ColorFamily(
        successDarkHighContrast,
        onSuccessDarkHighContrast,
        successContainerDarkHighContrast,
        onSuccessContainerDarkHighContrast,
    ),
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

@Immutable
data class ExtendedColorScheme(
    val warning: ColorFamily,
    val success: ColorFamily,
)

val unspecified_scheme = ColorFamily(
    Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
)

/**
 * Light Android gradient colors
 */
val LightAndroidGradientColors = GradientColors(container = surfaceLight)

/**
 * Dark Android gradient colors
 */
val DarkAndroidGradientColors = GradientColors(container = Color.Black)

/**
 * Light Android background theme
 */
val LightAndroidBackgroundTheme = BackgroundTheme(color = surfaceLight)

/**
 * Dark Android background theme
 */
val DarkAndroidBackgroundTheme = BackgroundTheme(color = Color.Black)

const val stronglyDeemphasizedAlpha = 0.6f
const val slightlyDeemphasizedAlpha = 0.87f
const val extremelyDeemphasizedAlpha = 0.32f

/**
 * Now in Android theme.
 *
 * @param darkTheme Whether the theme should use a dark color scheme (follows system by default).
 * @param androidTheme Whether the theme should use the Android theme color scheme instead of the
 *        default theme.
 * @param disableDynamicTheming If `true`, disables the use of dynamic theming, even when it is
 *        supported. This parameter has no effect if [androidTheme] is `true`.
 */
@Composable
fun PaceDreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    androidTheme: Boolean = false,
    disableDynamicTheming: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Color scheme
    val colorScheme = when {
        androidTheme -> if (darkTheme) DarkAndroidColorScheme else LightAndroidColorScheme
        !disableDynamicTheming && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> if (darkTheme) DarkDefaultColorScheme else LightDefaultColorScheme
    }
    // Gradient colors
    val emptyGradientColors = GradientColors(container = colorScheme.surfaceColorAtElevation(2.dp))
    val defaultGradientColors = GradientColors(
        top = colorScheme.inverseOnSurface,
        bottom = colorScheme.primaryContainer,
        container = colorScheme.surface,
    )
    val gradientColors = when {
        androidTheme -> if (darkTheme) DarkAndroidGradientColors else LightAndroidGradientColors
        !disableDynamicTheming && supportsDynamicTheming() -> emptyGradientColors
        else -> defaultGradientColors
    }
    // Background theme
    val defaultBackgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
    )
    val backgroundTheme = when {
        androidTheme -> if (darkTheme) DarkAndroidBackgroundTheme else LightAndroidBackgroundTheme
        else -> defaultBackgroundTheme
    }
    val tintTheme = when {
        androidTheme -> TintTheme()
        !disableDynamicTheming && supportsDynamicTheming() -> TintTheme(colorScheme.primary)
        else -> TintTheme()
    }
    // Composition locals
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PaceDreamMaterialTypography,
            content = content,
        )
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
