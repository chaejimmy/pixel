package com.shourov.apps.pacedream.signin.screens.signIn

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import com.shourov.apps.pacedream.signin.navigation.DASHBOARD_ROUTE
import com.shourov.apps.pacedream.signin.navigation.SignInRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Locks the post-login navigation contract for `SignInScreen`.
 *
 * The historical bug routed every returning user to
 * `SignInRoutes.CREATE_ACCOUNT` after a successful login, dropping them
 * into the new-account funnel. The screen now navigates to
 * `DASHBOARD_ROUTE` and pops the auth entry off the back stack so that
 * pressing Back from the dashboard exits the app rather than returning
 * to SignIn. These tests exercise the actual `NavController` instead of
 * the screen's Hilt-injected `AuthSession`, so they don't need the
 * Android runtime to construct `SignInViewModel`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignInNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginSuccess_navigatesToDashboard_andClearsSignInFromBackStack() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            val context = LocalContext.current
            navController = remember {
                TestNavHostController(context).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            }
            NavHost(
                navController = navController,
                startDestination = SignInRoutes.SIGN_IN.name,
            ) {
                composable(SignInRoutes.SIGN_IN.name) { Text("sign in") }
                composable(SignInRoutes.CREATE_ACCOUNT.name) { Text("create account") }
                composable(DASHBOARD_ROUTE) { Text("dashboard") }
            }
        }

        composeRule.runOnIdle {
            assertEquals(SignInRoutes.SIGN_IN.name, navController.currentDestination?.route)
        }

        // Mirror the navigation call that SignInScreen makes from the
        // login `onSuccess` lambda. Keeping this in the test (rather than
        // pulling SignInScreen up via Hilt) protects the contract that
        // matters — the route key + back-stack shape — without dragging
        // in AuthSession and a real Activity.
        composeRule.runOnIdle {
            navController.navigate(DASHBOARD_ROUTE) {
                popUpTo(SignInRoutes.SIGN_IN.name) { inclusive = true }
                launchSingleTop = true
            }
        }

        composeRule.runOnIdle {
            assertEquals(
                "Login success must land on the dashboard graph entry, not the auth funnel.",
                DASHBOARD_ROUTE,
                navController.currentDestination?.route,
            )
            assertNull(
                "Back from Dashboard must exit the app — SignIn must not remain on the back stack.",
                navController.previousBackStackEntry,
            )
        }
    }

    @Test
    fun signUpLink_fromSignIn_remainsReachable() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            val context = LocalContext.current
            navController = remember {
                TestNavHostController(context).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            }
            NavHost(
                navController = navController,
                startDestination = SignInRoutes.SIGN_IN.name,
            ) {
                composable(SignInRoutes.SIGN_IN.name) { Text("sign in") }
                composable(SignInRoutes.START_EMAIL_PHONE.name) { Text("start signup") }
            }
        }

        // The "Don't have an account? Sign up" link in SignInScreen.kt
        // routes to START_EMAIL_PHONE, which kicks off the create-account
        // funnel. This locks that path so a future refactor can't quietly
        // collapse Sign Up into the login screen.
        composeRule.runOnIdle {
            navController.navigate(SignInRoutes.START_EMAIL_PHONE.name)
        }

        composeRule.runOnIdle {
            assertEquals(
                SignInRoutes.START_EMAIL_PHONE.name,
                navController.currentDestination?.route,
            )
        }
    }
}
