package com.pacedream.common.composables.designsystem.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role

/**
 * The single design-system gesture for "shrink slightly while pressed, then
 * fire onClick" — the press affordance shared by every tappable Home card,
 * chip and tile.
 *
 * Why this exists: callsites used to re-implement press feedback by hand and
 * drifted apart. Three flavours had shipped:
 *  - `detectTapGestures { onPress { ...; tryAwaitRelease(); onClick() } }` —
 *    fires on release *regardless of whether the pointer is still inside the
 *    node*, so a drag-off-to-abort still navigated, and it draws no ripple.
 *  - `clickable(indication = null)` — correct cancel semantics but no ripple.
 *  - `clickable(indication = LocalIndication.current)` — correct, but every
 *    callsite owned its own `MutableInteractionSource` + `animateFloatAsState`.
 *
 * [pressable] folds all three into one modifier with the correct behaviour:
 *  - **Ripple** — uses [LocalIndication], so Material's indication shows.
 *  - **Cancel on slide-off** — built on [clickable], whose press is cancelled
 *    when the pointer leaves the node's bounds, so onClick does not fire.
 *  - **Announced as a button** — exposes [role] (default [Role.Button]) and
 *    [onClickLabel] to the accessibility tree for TalkBack.
 *
 * The press-scale animation reads from a single [MutableInteractionSource]
 * that also drives [clickable], so the shrink and the click/ripple stay in
 * lockstep — no separate sources to keep in sync.
 *
 * Ripple clipping: [scale] is applied before [clickable], but the ripple is
 * not clipped to any shape here. Callers that draw a rounded/clipped surface
 * should apply `.clip(shape)` *before* `.pressable(...)` so the ripple is
 * bounded to that shape:
 * ```
 * Modifier
 *     .clip(RoundedCornerShape(16.dp))
 *     .pressable(onClick = onClick)
 * ```
 *
 * @param onClick invoked on a completed tap (press + release inside bounds).
 * @param onClickLabel accessibility label describing the action, e.g.
 *   "Open Restroom"; surfaced to TalkBack.
 * @param pressedScale the scale factor held while pressed. Chips use ~0.95,
 *   cards ~0.98, image tiles ~0.96; pass the callsite's value explicitly.
 * @param enabled when false, the node neither animates nor reacts to taps.
 * @param role the accessibility role; defaults to [Role.Button].
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    onClickLabel: String? = null,
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
    role: Role = Role.Button,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "pressable",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            role = role,
            onClickLabel = onClickLabel,
            enabled = enabled,
            onClick = onClick,
        )
}
