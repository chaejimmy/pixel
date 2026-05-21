package com.shourov.apps.pacedream.designsystem.modifier

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Guarantees a minimum hit-test area for a tappable element without
 * inflating the visual.
 *
 * Material's accessibility guidance specifies a 48×48dp tap target;
 * smaller controls correlate with measurable mis-tap rates, especially
 * when buttons sit flush against each other. iOS HIG's 44dp number is a
 * separate platform standard and should not be used on Android.
 *
 * Wraps Material 3's [minimumInteractiveComponentSize], which floors the
 * laid-out size of the next inner modifier to at least 48dp while
 * leaving the rendered visual (and any inner `.size(...)` constraints)
 * at the design dimensions. The reserved minimum is read from
 * `LocalMinimumInteractiveComponentSize`, which Material 3 defaults to
 * 48dp; the [size] parameter is accepted for call-site documentation
 * and forward compatibility with a future per-component override.
 *
 * Pattern:
 * ```
 * Surface(
 *     modifier = Modifier
 *         .touchTargetSize()  // 48dp hit area
 *         .size(42.dp)        // 42dp visual
 *         .clip(CircleShape)
 *         ...
 * )
 * ```
 */
@Suppress("unused", "UNUSED_PARAMETER")
fun Modifier.touchTargetSize(size: Dp = 48.dp): Modifier =
    this.minimumInteractiveComponentSize()
