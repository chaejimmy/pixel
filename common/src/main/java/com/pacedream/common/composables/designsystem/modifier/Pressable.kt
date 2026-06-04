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
 * The one-and-only press affordance for tappable surfaces.
 *
 * Replaces the per-callsite `MutableInteractionSource` + `animateFloatAsState`
 * + `clickable` boilerplate that used to be copy-pasted (and subtly diverge)
 * across Home cards. Every tappable card should reach for this instead of
 * hand-rolling press scale, so the family stays consistent:
 *
 *  - **Ripple** — uses [LocalIndication], so presses ripple like every other
 *    Material surface. (The old `clickable(indication = null)` callsites had
 *    no ripple at all.)
 *  - **Cancel on slide-off** — built on [clickable], the press is cancelled
 *    when the pointer leaves the node's bounds and [onClick] does NOT fire.
 *    (The old `detectTapGestures { tryAwaitRelease(); onClick() }` callsites
 *    fired on release regardless of where the pointer ended up.)
 *  - **TalkBack** — announces as [role] (a button by default).
 *  - **Press scale** — shrinks to [pressedScale] while held, animated over a
 *    short [tween], driven by a single shared interaction source.
 *
 * Note: the ripple is drawn by [clickable] at this node, so it is only clipped
 * to a rounded shape if an ancestor clips. Callers that want the ripple
 * clipped should apply `.clip(shape)` BEFORE `.pressable(...)`.
 *
 * @param onClick Invoked on a completed tap (press + release inside bounds).
 * @param onClickLabel Accessibility label describing the click action.
 * @param pressedScale Scale applied while the press is held. `1f` disables the
 *   shrink. Chips use `0.95f`, cards `0.98f`, image tiles `0.96f`.
 * @param enabled When `false`, the node is not clickable and does not react.
 * @param role Accessibility role announced to TalkBack. Defaults to a button.
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
