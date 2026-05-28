package com.pacedream.app.feature.tripplanner

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
 * Compose-level coverage for the Trip Planner destructive-delete gate.
 *
 * Mirrors [com.pacedream.app.feature.collections.DeleteCollectionConfirmDialogTest]:
 * the production trip card stages a `pendingDelete` trip on tap and only
 * fires `viewModel.deleteTrip` through the dialog's Confirm CTA. These
 * tests pin the dialog copy and the click-routing contract; the failure /
 * success snackbar surfacing is covered by [TripPlannerViewModelDeleteTest]
 * and the existing `LaunchedEffect(uiState.error / uiState.message)` blocks
 * in the host screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeleteTripConfirmDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sample = TripPlan(
        id = "trip-7",
        name = "Lisbon long weekend",
        fromCity = "London",
        toCity = "Lisbon",
        departureDate = "2026-06-12",
        returnDate = "2026-06-15",
        travelers = 2,
    )

    @Test
    fun rendersExpectedCopy_titleBodyConfirmCancel() {
        composeRule.setContent {
            DeleteTripConfirmDialog(
                trip = sample,
                onConfirm = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Delete trip?").assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerDeleteDialogTags.Body).assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun cancel_invokesOnDismissOnly_andNeverOnConfirm() {
        var dismissed = 0
        var confirmed = 0
        composeRule.setContent {
            DeleteTripConfirmDialog(
                trip = sample,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithTag(TripPlannerDeleteDialogTags.Cancel).performClick()
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
            DeleteTripConfirmDialog(
                trip = sample,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithTag(TripPlannerDeleteDialogTags.Confirm).performClick()
        composeRule.runOnIdle {
            assertEquals(
                "Confirm must call onConfirm exactly once — guarantees deleteTrip() fires once",
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
