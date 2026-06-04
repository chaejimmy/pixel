package com.pacedream.common.composables.designsystem.modifier

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins down the press-and-drag-off contract for [Modifier.pressable], the one
 * design-system gesture every Home card, chip and tile now shares.
 *
 * The flavours [pressable] replaced disagreed on this exact behaviour: the
 * `detectTapGestures { onPress { ...; tryAwaitRelease(); onClick() } }` variant
 * fired on release even after the pointer had been dragged off the node, so a
 * slide-off-to-abort still navigated. [pressable] is built on
 * `Modifier.clickable`, which cancels the press when the pointer leaves the
 * node's bounds. Because every callsite routes through this one modifier, this
 * single assertion covers the whole family.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PressableTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tap_invokesOnClick() {
        var clicks = 0
        composeRule.setContent {
            PaceDreamTheme {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .pressable(onClick = { clicks += 1 }),
                ) {
                    Text("Pressable")
                }
            }
        }

        composeRule.onNodeWithText("Pressable").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Pressable").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun pressAndDragOff_doesNotInvokeOnClick() {
        var clicks = 0
        composeRule.setContent {
            PaceDreamTheme {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .pressable(onClick = { clicks += 1 }),
                ) {
                    Text("Pressable")
                }
            }
        }

        // Press at the centre, drag the pointer far outside the node's bounds
        // (well past the touch slop), then release. clickable cancels the press
        // on slide-off, so onClick must NOT fire — this is the contract every
        // migrated card now inherits.
        composeRule.onNodeWithText("Pressable").performTouchInput {
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
