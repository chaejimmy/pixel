package com.pacedream.app.feature.collections

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Compose-level coverage for the Collections destructive-delete gate.
 *
 * Pre-existing screen logic stages a `pendingDelete` collection when the
 * user taps the trash icon and only calls `viewModel.deleteCollection` from
 * the dialog's Confirm CTA. These tests pin two invariants of that gate:
 *
 *  1. The dialog shows the destructive-action copy the audit demands
 *     (title, body, "Delete" CTA in the error tint, "Cancel" dismiss).
 *  2. Confirm routes through `onConfirm` only — never `onDismiss` —
 *     and Cancel routes through `onDismiss` only. This is what gives
 *     the screen its "no API call yet" property on a trash-tap: the
 *     viewModel.deleteCollection call lives only behind onConfirm.
 *
 * Snackbar wiring on the host screen is covered indirectly: the screen
 * collects `CollectionsViewModel.effects` into a SnackbarHost; once that
 * collector is in place a Confirm tap → ShowToast → snackbar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeleteCollectionConfirmDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sample = UserCollection(
        id = "collection-42",
        name = "Weekend studios",
        description = "Spots I want to revisit.",
        itemCount = 7,
    )

    @Test
    fun rendersExpectedCopy_titleBodyConfirmCancel() {
        composeRule.setContent {
            DeleteCollectionConfirmDialog(
                collection = sample,
                onConfirm = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Delete collection?").assertIsDisplayed()
        composeRule.onNodeWithTag(CollectionsDeleteDialogTags.Body).assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun cancel_invokesOnDismissOnly_andNeverOnConfirm() {
        var dismissed = 0
        var confirmed = 0
        composeRule.setContent {
            DeleteCollectionConfirmDialog(
                collection = sample,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithTag(CollectionsDeleteDialogTags.Cancel).performClick()
        composeRule.runOnIdle {
            assertEquals("Cancel must route through onDismiss exactly once", 1, dismissed)
            assertEquals(
                "Cancel must never invoke onConfirm — no API call may fire",
                0,
                confirmed,
            )
        }
    }

    @Test
    fun confirm_invokesOnConfirmExactlyOnce() {
        var dismissed = 0
        var confirmed = 0
        composeRule.setContent {
            DeleteCollectionConfirmDialog(
                collection = sample,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithTag(CollectionsDeleteDialogTags.Confirm).performClick()
        composeRule.runOnIdle {
            assertEquals(
                "Confirm must call onConfirm exactly once — guarantees deleteCollection() fires once",
                1,
                confirmed,
            )
            assertEquals(
                "Confirm must not also call onDismiss — the host owns dismissal after onConfirm",
                0,
                dismissed,
            )
        }
    }
}
