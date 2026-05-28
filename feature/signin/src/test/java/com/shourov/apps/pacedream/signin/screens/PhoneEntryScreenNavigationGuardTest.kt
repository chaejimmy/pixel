package com.shourov.apps.pacedream.signin.screens

import PhoneEntryScreen
import PhoneEntryScreenTestTags
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.shourov.apps.pacedream.core.data.UserAuthPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Locks the C-03 navigation invariant: the Continue button on
 * `PhoneEntryScreen` (legacy `core/ui` flow) MUST gate forward navigation
 * on `PhoneNumberValidationState.isValid`. A regression that lets an
 * invalid phone past this gate is an authentication bypass — the OTP
 * step is skipped and the user lands on account setup.
 *
 * The test exercises the composable directly through Robolectric so it
 * runs in unit-test scope (same pattern as `OfferBottomSheetContentTest`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneEntryScreenNavigationGuardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun continue_withEmptyPhone_doesNotNavigate_andSurfacesSnackbar() {
        var otpPhone: String? = null
        var accountSetupCalls = 0
        var snackbarMessage: String? = null

        composeRule.setContent {
            PaceDreamTheme {
                PhoneEntryScreen(
                    onProceedToOtpVerification = { otpPhone = it },
                    onNavigateToCreateAccount = {},
                    onNavigateToAccountSignIn = {},
                    onNavigateToAccountSetup = { accountSetupCalls += 1 },
                    userAuthPath = UserAuthPath.NEW,
                    onShowSnackbar = { snackbarMessage = it },
                )
            }
        }

        composeRule.onNodeWithTag(PhoneEntryScreenTestTags.ContinueButton).performClick()

        assertNull("Empty phone must not route to OTP", otpPhone)
        assertEquals(
            "Empty phone must never reach account setup — that's the C-03 bypass",
            0,
            accountSetupCalls,
        )
        assertEquals(
            "Empty phone must surface the invalid-phone snackbar",
            "Enter a valid phone number",
            snackbarMessage,
        )
    }

    @Test
    fun continue_withInvalidPhone_doesNotNavigate_andSurfacesSnackbar() {
        var otpPhone: String? = null
        var accountSetupCalls = 0
        var snackbarMessage: String? = null

        composeRule.setContent {
            PaceDreamTheme {
                PhoneEntryScreen(
                    onProceedToOtpVerification = { otpPhone = it },
                    onNavigateToCreateAccount = {},
                    onNavigateToAccountSignIn = {},
                    onNavigateToAccountSetup = { accountSetupCalls += 1 },
                    userAuthPath = UserAuthPath.NEW,
                    onShowSnackbar = { snackbarMessage = it },
                )
            }
        }

        // A 3-digit US "phone" cannot parse to a valid number.
        composeRule.onNodeWithTag(PhoneEntryScreenTestTags.PhoneInput).performTextInput("123")
        composeRule.onNodeWithTag(PhoneEntryScreenTestTags.ContinueButton).performClick()

        assertNull("Invalid phone must not route to OTP", otpPhone)
        assertEquals(
            "Invalid phone must never reach account setup — that's the C-03 bypass",
            0,
            accountSetupCalls,
        )
        assertEquals(
            "Invalid phone must surface the invalid-phone snackbar",
            "Enter a valid phone number",
            snackbarMessage,
        )
    }

    @Test
    fun continue_withValidPhone_routesToOtp_andNeverToAccountSetup() {
        var otpPhone: String? = null
        var accountSetupCalls = 0
        var snackbarMessage: String? = null

        composeRule.setContent {
            PaceDreamTheme {
                PhoneEntryScreen(
                    onProceedToOtpVerification = { otpPhone = it },
                    onNavigateToCreateAccount = {},
                    onNavigateToAccountSignIn = {},
                    onNavigateToAccountSetup = { accountSetupCalls += 1 },
                    userAuthPath = UserAuthPath.NEW,
                    onShowSnackbar = { snackbarMessage = it },
                )
            }
        }

        // 10-digit US national number (libphonenumber accepts the bare
        // national form when paired with the "US" region).
        composeRule.onNodeWithTag(PhoneEntryScreenTestTags.PhoneInput).performTextInput("4155551234")
        composeRule.onNodeWithTag(PhoneEntryScreenTestTags.ContinueButton).performClick()

        assertEquals(
            "Valid phone must route to OTP with the entered digits",
            "4155551234",
            otpPhone,
        )
        assertEquals(
            "Valid phone must NEVER bypass OTP and land on account setup directly",
            0,
            accountSetupCalls,
        )
        assertNull(
            "Snackbar must not fire for a valid phone — would be a false alarm",
            snackbarMessage,
        )
    }
}
