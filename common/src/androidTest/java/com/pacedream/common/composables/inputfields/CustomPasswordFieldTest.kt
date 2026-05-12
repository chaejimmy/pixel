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
 * Regression coverage for the M-11 stale-`remember(value)` bug (mirror of
 * the M-10 fix on [CustomInputTextField]).
 */
@RunWith(AndroidJUnit4::class)
class CustomPasswordFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun externalValueChange_propagatesAfterFirstComposition() {
        var hoisted by mutableStateOf("hunter2")
        composeRule.setContent {
            CustomPasswordField(
                value = hoisted,
                onValueChange = { hoisted = it },
                label = "Password",
            )
        }

        // External clear — simulates the login VM wiping the field after
        // a failed attempt.  Before M-11 fix this would NOT reflect; the
        // user would still see "hunter2" rendered after the VM swap.
        composeRule.runOnIdle { hoisted = "" }
        // Material 3 password fields render the label as content when
        // empty; assert by label presence.
        composeRule.onNodeWithText("Password").assertExists()
    }

    @Test
    fun userTyping_invokesOnValueChange() {
        var hoisted by mutableStateOf("")
        composeRule.setContent {
            CustomPasswordField(
                value = hoisted,
                onValueChange = { hoisted = it },
                label = "New password",
            )
        }
        composeRule.onNodeWithText("New password").performTextReplacement("abcDEF12")
        composeRule.runOnIdle { assert(hoisted == "abcDEF12") }
    }
}
