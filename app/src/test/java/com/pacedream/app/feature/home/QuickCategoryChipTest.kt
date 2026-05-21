package com.pacedream.app.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.designsystem.CategoryColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins down the press-and-drag-off contract for QuickCategoryChip.
 *
 * Before this change the chip used detectTapGestures and fired onClick on
 * release regardless of whether the pointer was still inside the chip —
 * users who dragged off to abort accidentally navigated. The chip now
 * uses Modifier.clickable, which cancels the press when the pointer
 * leaves bounds. These tests pin that contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickCategoryChipTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleCategory = CategoryCardData(
        name = "Restroom",
        icon = PaceDreamIcons.Wc,
        color = CategoryColors.Restroom,
    )

    @Test
    fun tap_invokesOnClick() {
        var clicks = 0
        composeRule.setContent {
            PaceDreamTheme {
                QuickCategoryChip(
                    category = sampleCategory,
                    onClick = { clicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Restroom").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Restroom").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun pressAndDragOff_doesNotInvokeOnClick() {
        var clicks = 0
        composeRule.setContent {
            PaceDreamTheme {
                QuickCategoryChip(
                    category = sampleCategory,
                    onClick = { clicks += 1 },
                )
            }
        }

        // Simulate a press at the centre of the chip, drag the pointer far
        // outside the chip's bounds (well past the touch slop), then release.
        // Modifier.clickable cancels the press when the pointer leaves the
        // node's bounds, so onClick must NOT fire.
        composeRule.onNodeWithText("Restroom").performTouchInput {
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
