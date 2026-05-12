package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shourov.apps.pacedream.feature.wanted.model.OfferFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Locks the offer-submitted closure flow.
 *
 * Before this change the bottom sheet closed silently after a successful
 * submission — users had no confirmation and would post duplicates. The
 * sheet now flips to an "Offer sent" success layout that the user must
 * dismiss explicitly. These tests pin that contract:
 *  - The success layout is reachable from the form once `submitted=true`.
 *  - The form is no longer rendered while showing success.
 *  - "Done" routes to the dismiss callback (it never auto-closes).
 *  - "View my offers" is hidden when no navigation callback is wired
 *    (i.e. P-05 hasn't landed yet).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfferBottomSheetContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successLayout_isReachable_whenStateFlipsToSubmitted() {
        var state by mutableStateOf(OfferFormState(price = "25", message = "I'll bring my own tools."))
        composeRule.setContent {
            OfferBottomSheetContent(
                state = state,
                onPriceChange = {},
                onMessageChange = {},
                onSubmit = {},
                onDone = {},
                requesterName = "Maya",
            )
        }

        // Form is visible before submission; success layout is absent.
        composeRule.onNodeWithTag(OfferSheetTestTags.Form).assertIsDisplayed()
        composeRule.onNodeWithTag(OfferSheetTestTags.Success).assertDoesNotExist()

        // Flip the state as the ViewModel would after a successful POST.
        state = state.copy(submitting = false, submitted = true)

        composeRule.onNodeWithTag(OfferSheetTestTags.Success).assertIsDisplayed()
        composeRule.onNodeWithTag(OfferSheetTestTags.Form).assertDoesNotExist()
        // Confirmation copy must name the requester so the user knows
        // who will see the offer.
        composeRule.onNodeWithText("Offer sent").assertIsDisplayed()
        composeRule
            .onNodeWithText("Maya will see your offer and reply in your inbox if they're interested.")
            .assertIsDisplayed()
    }

    @Test
    fun done_doesNotAutoDismiss_andOnlyFiresWhenTapped() {
        var dismissed = 0
        composeRule.setContent {
            OfferBottomSheetContent(
                state = OfferFormState(submitted = true),
                onPriceChange = {},
                onMessageChange = {},
                onSubmit = {},
                onDone = { dismissed += 1 },
                requesterName = "Maya",
            )
        }

        // Until the user taps Done we must remain on the success layout.
        composeRule.onNodeWithTag(OfferSheetTestTags.Success).assertIsDisplayed()
        assertEquals("Done must never fire on its own", 0, dismissed)

        composeRule.onNodeWithText("Done").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun viewMyOffers_hidden_whenNoNavigationCallbackProvided() {
        composeRule.setContent {
            OfferBottomSheetContent(
                state = OfferFormState(submitted = true),
                onPriceChange = {},
                onMessageChange = {},
                onSubmit = {},
                onDone = {},
                requesterName = null,
                onViewMyOffers = null,
            )
        }

        composeRule.onNodeWithText("View my offers").assertDoesNotExist()
    }

    @Test
    fun viewMyOffers_visible_andRoutesToNavigation_whenProvided() {
        var navigated = false
        composeRule.setContent {
            OfferBottomSheetContent(
                state = OfferFormState(submitted = true),
                onPriceChange = {},
                onMessageChange = {},
                onSubmit = {},
                onDone = {},
                requesterName = "Maya",
                onViewMyOffers = { navigated = true },
            )
        }

        composeRule.onNodeWithText("View my offers").performClick()
        assertTrue(navigated)
    }
}
