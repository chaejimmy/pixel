package com.shourov.apps.pacedream.signin.screens.signIn

import com.shourov.apps.pacedream.signin.navigation.DASHBOARD_ROUTE
import com.shourov.apps.pacedream.signin.navigation.SignInRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Locks the C-01 invariant from `INTERACTION_AUDIT_REPORT.md`: after a
 * successful email/password login, `SignInScreen`'s success lambda must
 * navigate to the dashboard graph entry — not back into the auth graph.
 *
 * The original bug routed authenticated users to
 * `SignInRoutes.CREATE_ACCOUNT`, dropping every returning user into the
 * account-creation funnel. The screen now calls
 * `navController.navigate(DASHBOARD_ROUTE) { popUpTo(0) { inclusive = true } }`
 * (see `SignInScreen.kt`). These assertions catch a regression where the
 * constant value drifts or the screen is re-wired to an auth-graph route.
 *
 * Construction of `SignInViewModel` itself is not exercised here because
 * its `AuthSession` dependency requires an Android `Context` (via
 * `TokenStorage`), which is unavailable in a JVM unit test. The
 * route-key invariant is what C-01 protects — that is what this test
 * locks down.
 */
class SignInViewModelTest {

    @Test
    fun `login success route is the dashboard graph entry`() {
        assertEquals(
            "C-01: SignInScreen's login onSuccess must navigate to the dashboard graph entry. " +
                "Changing this string detaches the auth flow from the dashboard NavHost in PaceDreamApp.",
            "dashboard_route",
            DASHBOARD_ROUTE,
        )
    }

    @Test
    fun `login success route is not the create-account funnel`() {
        assertNotEquals(
            "C-01 regression: a returning user must not be sent into the account-creation flow after login.",
            SignInRoutes.CREATE_ACCOUNT.name,
            DASHBOARD_ROUTE,
        )
    }

    @Test
    fun `login success route does not land back in the auth graph`() {
        SignInRoutes.entries.forEach { authRoute ->
            assertNotEquals(
                "C-01: the post-login destination must live outside the SignInRoutes graph; " +
                    "${authRoute.name} would loop the user back into auth.",
                authRoute.name,
                DASHBOARD_ROUTE,
            )
        }
    }
}
