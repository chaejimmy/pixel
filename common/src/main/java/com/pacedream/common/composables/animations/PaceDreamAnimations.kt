/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pacedream.common.composables.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Enhanced Animation Utilities for PaceDream App
 * Provides smooth, consistent animations throughout the app
 */

// Animation Durations
object PaceDreamAnimationDuration {
    const val SHORT = 200
    const val MEDIUM = 300
    const val LONG = 500
    const val EXTRA_LONG = 800
}

// Animation Easing
object PaceDreamEasing {
    val EaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val EaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val EaseIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val Bounce = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
}

// Fade In Animation
@Composable
fun FadeInAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = PaceDreamAnimationDuration.MEDIUM,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(duration, delayMillis = delay, easing = PaceDreamEasing.EaseOut)),
        exit = fadeOut(animationSpec = tween(duration, easing = PaceDreamEasing.EaseIn)),
        modifier = modifier
    ) {
        content()
    }
}

// Slide In Animation
@Composable
fun SlideInAnimation(
    visible: Boolean,
    slideDirection: SlideDirection = SlideDirection.Up,
    modifier: Modifier = Modifier,
    duration: Int = PaceDreamAnimationDuration.MEDIUM,
    content: @Composable () -> Unit
) {
    val slideIn = when (slideDirection) {
        SlideDirection.Up -> slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseOut)
        )
        SlideDirection.Down -> slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseOut)
        )
        SlideDirection.Left -> slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseOut)
        )
        SlideDirection.Right -> slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseOut)
        )
    }
    
    val slideOut = when (slideDirection) {
        SlideDirection.Up -> slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseIn)
        )
        SlideDirection.Down -> slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseIn)
        )
        SlideDirection.Left -> slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseIn)
        )
        SlideDirection.Right -> slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseIn)
        )
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideIn,
        exit = slideOut,
        modifier = modifier
    ) {
        content()
    }
}

// Scale Animation
@Composable
fun ScaleAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = PaceDreamAnimationDuration.MEDIUM,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = tween(duration, easing = PaceDreamEasing.Bounce),
            initialScale = 0.8f
        ),
        exit = scaleOut(
            animationSpec = tween(duration, easing = PaceDreamEasing.EaseIn),
            targetScale = 0.8f
        ),
        modifier = modifier
    ) {
        content()
    }
}

// Pulse Animation
@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    duration: Int = 1000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = PaceDreamEasing.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}

// Shimmer Animation
@Composable
fun ShimmerAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = PaceDreamEasing.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
        }
    ) {
        content()
    }
}

// Staggered Animation for Lists
@Composable
fun StaggeredAnimation(
    visible: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val delay = index * 100
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = PaceDreamAnimationDuration.MEDIUM,
                delayMillis = delay,
                easing = PaceDreamEasing.EaseOut
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = PaceDreamAnimationDuration.MEDIUM,
                delayMillis = delay,
                easing = PaceDreamEasing.EaseOut
            )
        ),
        modifier = modifier
    ) {
        content()
    }
}

// Card Hover Animation
@Composable
fun CardHoverAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(
            durationMillis = PaceDreamAnimationDuration.SHORT,
            easing = PaceDreamEasing.EaseOut
        ),
        label = "card_hover"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 8.dp else 4.dp,
        animationSpec = tween(
            durationMillis = PaceDreamAnimationDuration.SHORT,
            easing = PaceDreamEasing.EaseOut
        ),
        label = "card_elevation"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        content()
    }
}

// Loading Animation
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_rotation"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation
        }
    ) {
        content()
    }
}

enum class SlideDirection {
    Up, Down, Left, Right
}
