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

package com.pacedream.common.composables.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * iOS-matched primary process button.
 *
 * - 44dp minimum height (iOS HIG tap target)
 * - 12dp corner radius (matches iOS PaceDreamDesignSystem.CornerRadius.md)
 * - No elevation (iOS flat style)
 * - 17sp Semi-Bold text (iOS button label)
 * - Press scale 0.95 animation (matches iOS PaceDreamButtonStyle)
 */
@Composable
fun ProcessButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = PaceDreamColors.Primary,
        contentColor = Color.White,
        disabledContainerColor = PaceDreamColors.Gray200,
        disabledContentColor = PaceDreamColors.TextSecondary,
    ),
    text: String = "",
    isEnabled: Boolean = true,
    isProcessing: Boolean = false,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "button_press_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(PaceDreamButtonHeight.MD)
            .scale(scale),
        enabled = !isProcessing && isEnabled,
        colors = colors,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { interactionSource ->
            isPressed = interactionSource.collectIsPressedAsState().value
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = PaceDreamSpacing.XS),
        ) {
            Text(
                text = text,
                style = PaceDreamTypography.Button,
            )
            AnimatedVisibility(isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = PaceDreamSpacing.XS),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/**
 * Gradient primary button matching iOS PaceDreamDesignSystem.Gradients.primary.
 * Uses a gradient background (primary → primaryDark) with press scale animation.
 */
@Composable
fun GradientProcessButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String = "",
    isEnabled: Boolean = true,
    isProcessing: Boolean = false,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "gradient_button_press_scale"
    )

    val buttonShape = RoundedCornerShape(PaceDreamRadius.MD)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PaceDreamButtonHeight.LG)
            .scale(scale)
            .clip(buttonShape)
            .background(
                brush = if (isEnabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            PaceDreamColors.Primary,
                            PaceDreamColors.PrimaryDark
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            PaceDreamColors.Gray200,
                            PaceDreamColors.Gray300
                        )
                    )
                },
                shape = buttonShape
            )
            .pointerInput(isEnabled) {
                if (isEnabled && !isProcessing) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                            onClick()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = PaceDreamTypography.Button,
                color = if (isEnabled) Color.White else PaceDreamColors.TextSecondary,
            )
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = PaceDreamSpacing.XS),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Outline button variant matching iOS PaceDreamButtonStyle.outline.
 * Includes a 1dp border in the primary color (or gray when disabled).
 */
@Composable
fun OutlineProcessButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String = "",
    isEnabled: Boolean = true,
    isProcessing: Boolean = false,
) {
    val effectiveEnabled = !isProcessing && isEnabled
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "outline_button_press_scale"
    )

    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(PaceDreamButtonHeight.MD)
            .scale(scale),
        enabled = effectiveEnabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = PaceDreamColors.Primary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = PaceDreamColors.TextTertiary,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (effectiveEnabled) PaceDreamColors.Primary else PaceDreamColors.Gray300,
        ),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { interactionSource ->
            isPressed = interactionSource.collectIsPressedAsState().value
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = PaceDreamSpacing.XS),
        ) {
            Text(
                text = text,
                style = PaceDreamTypography.Button,
            )
            AnimatedVisibility(isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = PaceDreamSpacing.XS),
                    strokeWidth = 2.dp,
                    color = PaceDreamColors.Primary,
                )
            }
        }
    }
}

/**
 * Destructive button for dangerous actions (Cancel booking, Delete account, Sign out).
 * Red fill matching iOS destructive button pattern.
 */
@Composable
fun DestructiveProcessButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String = "",
    isEnabled: Boolean = true,
    isProcessing: Boolean = false,
) {
    ProcessButton(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = PaceDreamColors.Error,
            contentColor = Color.White,
            disabledContainerColor = PaceDreamColors.Gray200,
            disabledContentColor = PaceDreamColors.TextSecondary,
        ),
        text = text,
        isEnabled = isEnabled,
        isProcessing = isProcessing,
    )
}

/**
 * Compact button for card-level inline actions.
 * Uses SM height (36dp) and Callout typography.
 * Matches iOS compact button in cards and inline contexts.
 */
@Composable
fun CompactProcessButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String = "",
    isEnabled: Boolean = true,
    containerColor: Color = PaceDreamColors.Primary,
    contentColor: Color = Color.White,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "compact_button_press_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(PaceDreamButtonHeight.SM)
            .scale(scale),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = PaceDreamColors.Gray200,
            disabledContentColor = PaceDreamColors.TextSecondary,
        ),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = 0.dp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { interactionSource ->
            isPressed = interactionSource.collectIsPressedAsState().value
        },
    ) {
        Text(
            text = text,
            style = PaceDreamTypography.Callout.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        )
    }
}

