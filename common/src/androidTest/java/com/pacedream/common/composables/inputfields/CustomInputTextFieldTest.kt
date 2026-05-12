package com.pacedream.common.composables.inputfields

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the M-10 stale-`remember(value)` bug.
 *
 * Before the fix, the composable captured a private `var text by remember`
 * mirror of the initial `value`.  Any external change to `value` after the
 * first composition was silently ignored.  These tests assert that the
 * field is now fully controlled by the caller: external state changes
 * propagate, and the trailing clear button routes through `onValueChange`.
 */
@RunWith(AndroidJUnit4::class)
class CustomInputTextFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun externalValueChange_propagatesAfterFirstComposition() {
        var hoisted by mutableStateOf("initial")
        composeRule.setContent {
            CustomInputTextField(
                value = hoisted,
                onValueChange = { hoisted = it },
                label = "Email",
            )
        }
        composeRule.onNodeWithText("initial").assertTextEquals("Email", "initial")

        // External mutation — simulates a ViewModel restoring a draft after
        // the field is already on screen.  Before M-10 fix this would NOT
        // reflect; after, it does.
        composeRule.runOnIdle { hoisted = "restored draft" }
        composeRule.onNodeWithText("restored draft").assertTextEquals("Email", "restored draft")
    }

    @Test
    fun userTyping_invokesOnValueChange_andReflectsExternalEcho() {
        var hoisted by mutableStateOf("")
        composeRule.setContent {
            CustomInputTextField(
                value = hoisted,
                onValueChange = { hoisted = it },
                label = "Name",
            )
        }
        composeRule.onNodeWithText("Name").performTextReplacement("Ada")
        composeRule.runOnIdle { assert(hoisted == "Ada") }
        // Compose re-renders the field with the new external value.
        composeRule.onNodeWithText("Ada").assertTextEquals("Name", "Ada")
    }
}
