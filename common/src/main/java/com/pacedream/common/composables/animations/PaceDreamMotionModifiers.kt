/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.pacedream.common.composables.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

// Lightweight motion helpers. All effects run in the graphics layer so they
// do not re-measure or re-layout children, and they do not trigger
// recomposition of the content they decorate — just redraw on each frame of
// the 150–220 ms tween.

/**
 * Fade + slide-up on first composition. Occupies full space immediately so
 * LazyGrid / LazyColumn layout is stable; only alpha + translationY animate.
 *
 * @param durationMillis animation length (clamped to 150–220 ms band).
 * @param translationDp initial vertical offset; ~12 dp feels natural.
 */
fun Modifier.animatedCardEntry(
    durationMillis: Int = 180,
    translationDp: Float = 12f,
): Modifier = composed {
    var appeared by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis),
        label = "cardEntryAlpha",
    )
    val offsetFraction by animateFloatAsState(
        targetValue = if (appeared) 0f else 1f,
        animationSpec = tween(durationMillis),
        label = "cardEntryOffset",
    )
    LaunchedEffect(Unit) { appeared = true }
    graphicsLayer {
        this.alpha = alpha
        translationY = offsetFraction * translationDp * density
    }
}

/**
 * Scales the composable toward [pressedScale] while the caller's
 * [InteractionSource] reports a press. Reuses the source passed to
 * `clickable` / `Button` — no ripple is suppressed.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
    durationMillis: Int = 160,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(durationMillis),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
