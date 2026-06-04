package com.pacedream.app.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.designsystem.modifier.pressable
import com.pacedream.common.composables.theme.PaceDreamTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins down the press/cancel contract for [Modifier.pressable].
 *
 * Every Home card (chips, listing cards, image tiles) now routes its press
 * affordance through this one modifier, so a single assertion here covers the
 * whole family: a press that slides off the node before release must NOT
 * invoke onClick. The old hand-rolled `detectTapGestures` callsites fired on
 * release regardless of pointer position — this test guards against that
 * regressing back in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PressableTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tag = "pressable-target"

    @Test
    fun tap_invokesOnClick() {
        var clicks = 0
        composeRule.setContent {
            PaceDreamTheme {
                Text(
                    text = "Tap me",
                    modifier = Modifier
                        .testTag(tag)
                        .size(120.dp)
                        .pressable(onClick = { clicks += 1 }),
                )
            }
        }

        composeRule.onNodeWithTag(tag).assertHasClickAction()
        composeRule.onNodeWithTag(tag).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun pressAndDragOff_doesNotInvokeOnClick() {
        var clicks = 0
        composeRule.setContent {
            PaceDreamTheme {
                Text(
                    text = "Tap me",
                    modifier = Modifier
                        .testTag(tag)
                        .size(120.dp)
                        .pressable(onClick = { clicks += 1 }),
                )
            }
        }

        // Press at the centre, drag well past the node's bounds, then release.
        // pressable is built on Modifier.clickable, which cancels the press once
        // the pointer leaves bounds, so onClick must NOT fire.
        composeRule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveTo(Offset(centerX + 2_000f, centerY + 2_000f))
            up()
        }

        assertEquals(
            "onClick must not fire when the press is cancelled by drag-off",
            0,
            clicks,
        )
    }
}
