package com.shourov.apps.pacedream.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.shourov.apps.pacedream.signin.navigation.userOnBoardingScreen
import com.shourov.apps.pacedream.ui.PaceDreamAppState

@Composable
fun PaceDreamNavHost(
    modifier: Modifier = Modifier,
    startDestination: String,
    appState: PaceDreamAppState,
) {
    val navController = appState.navController

    // iOS 26 parity: 200ms easeInOut for all transitions
    val iOSEaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(
                animationSpec = tween(200, easing = iOSEaseInOut),
            ) + slideIntoContainer(
                animationSpec = tween(200, easing = iOSEaseInOut),
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(200, easing = iOSEaseInOut),
            ) + slideOutOfContainer(
                animationSpec = tween(200, easing = iOSEaseInOut),
                towards = AnimatedContentTransitionScope.SlideDirection.End,
            )
        },
    ) {
        userOnBoardingScreen(
            navController = navController,
            onNavigateToSignInWithEmail = {
                appState.navigateToUserStartDestination(UserStartTopLevelDestination.SIGN_IN_WITH_MAIL)
            },
            onStartWithPhone = {
                appState.navigateToUserStartDestination(UserStartTopLevelDestination.SIGN_IN_WITH_PHONE)
            },
            onNavigateToAccountSetup = {
                appState.navigateToUserStartDestination(UserStartTopLevelDestination.ACCOUNT_SETUP)
            },
        )

        DashboardNavigation(hostModeManager = appState.hostModeManager)

        //SignInNavigation(navHostController = navController)
    }
}