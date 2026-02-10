package com.shourov.apps.pacedream.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.util.Consts.ROOM_TYPE
import com.pacedream.common.util.Consts.TECH_GEAR_TYPE
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.shourov.apps.pacedream.feature.home.presentation.DashboardScreen
import com.shourov.apps.pacedream.feature.home.presentation.EnhancedDashboardScreenWrapper
import com.shourov.apps.pacedream.feature.propertydetail.PropertyDetailScreen
import com.shourov.apps.pacedream.feature.homefeed.HomeFeedScreen
import com.shourov.apps.pacedream.feature.homefeed.HomeSectionKey
import com.shourov.apps.pacedream.feature.homefeed.HomeSectionListScreen
import com.shourov.apps.pacedream.feature.search.SearchScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.FilterScreen
import com.shourov.apps.pacedream.feature.search.CategoryResultsScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.DestinationListScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.RecentSearchesScreen
import com.shourov.apps.pacedream.feature.booking.presentation.BookingTabScreen
import com.shourov.apps.pacedream.feature.host.presentation.PostTabScreen
import com.shourov.apps.pacedream.feature.inbox.presentation.InboxScreen
import com.shourov.apps.pacedream.feature.inbox.presentation.ThreadScreen
import com.shourov.apps.pacedream.feature.profile.presentation.ProfileTabScreen
import com.shourov.apps.pacedream.feature.webflow.presentation.BookingConfirmationScreen
import com.shourov.apps.pacedream.feature.webflow.presentation.BookingCancelledScreen
import com.shourov.apps.pacedream.feature.wishlist.presentation.WishlistScreen
import com.shourov.apps.pacedream.feature.booking.presentation.BookingFormScreen
import com.shourov.apps.pacedream.feature.bookingdetail.BookingDetailScreen
import com.shourov.apps.pacedream.signin.navigation.DASHBOARD_ROUTE

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.DashboardNavigation(
    hostModeManager: com.shourov.apps.pacedream.feature.host.domain.HostModeManager
) {
    with(this) {
        navigation(
            startDestination = DashboardDestination.HOME.name,
            route = DASHBOARD_ROUTE,
        ) {
            composable(route = DashboardDestination.HOME.name) {
                val navController = rememberNavController()
                val badgesViewModel = hiltViewModel<DashboardBadgesViewModel>()
                val inboxUnread by badgesViewModel.inboxUnread.collectAsStateWithLifecycle()

                // iOS-parity: allow other screens to request switching tabs.
                LaunchedEffect(navController) {
                    TabRouter.events.collectLatest { destination ->
                        navigateToTab(navController, destination.name)
                    }
                }

                // Tab order (matches requested): Home, Favorites, Bookings, Messages, Profile
                val bottomNavigationItems = remember(inboxUnread) {
                    listOf(
                    BottomNavigationItem(
                            icon = com.shourov.apps.pacedream.R.drawable.ic_home,
                            text = com.shourov.apps.pacedream.R.string.home,
                    ),
                    BottomNavigationItem(
                            icon = com.shourov.apps.pacedream.R.drawable.ic_favorite,
                            text = com.shourov.apps.pacedream.R.string.favorites,
                    ),
                    BottomNavigationItem(
                            icon = com.shourov.apps.pacedream.R.drawable.ic_booking,
                            text = com.shourov.apps.pacedream.R.string.bookings,
                    ),
                    BottomNavigationItem(
                            icon = com.shourov.apps.pacedream.R.drawable.ic_notifications,
                            text = com.shourov.apps.pacedream.R.string.messages,
                            badgeCount = inboxUnread
                    ),
                    BottomNavigationItem(
                            icon = com.shourov.apps.pacedream.R.drawable.ic_profile,
                            text = com.shourov.apps.pacedream.R.string.profile,
                    ),
                )
                    }

                    val backStackState = navController.currentBackStackEntryAsState().value
                    var selectedItem by rememberSaveable {
                        mutableIntStateOf(0)
                    }

                    // Bottom bar visibility - always show for main tabs
                    val isBottomBarShow = remember(key1 = backStackState) {
                        val destination = backStackState?.destination ?: return@remember false
                        destination.hierarchy.any { d ->
                            d.route == DashboardDestination.HOME.name ||
                                d.route == DashboardDestination.FAVORITES.name ||
                                d.route == DashboardDestination.BOOKINGS.name ||
                                d.route == DashboardDestination.INBOX.name ||
                                d.route == DashboardDestination.PROFILE.name ||
                                // Keep the bottom bar visible for Search even though it's not a tab.
                                d.route == DashboardDestination.SEARCH.name
                        }
                    }

                    selectedItem = remember(backStackState) {
                        val destination = backStackState?.destination
                        when {
                            destination?.hierarchy?.any { it.route == DashboardDestination.HOME.name } == true -> 0
                            destination?.hierarchy?.any { it.route == DashboardDestination.FAVORITES.name } == true -> 1
                            destination?.hierarchy?.any { it.route == DashboardDestination.BOOKINGS.name } == true -> 2
                            destination?.hierarchy?.any { it.route == DashboardDestination.INBOX.name } == true -> 3
                            destination?.hierarchy?.any { it.route == DashboardDestination.PROFILE.name } == true -> 4
                            // Search is launched from the Home header; keep Home highlighted.
                            destination?.hierarchy?.any { it.route == DashboardDestination.SEARCH.name } == true -> 0
                            else -> 0
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (isBottomBarShow)
                                AppBottomNavigation(
                                    items = bottomNavigationItems,
                                    selectedIndex = selectedItem,
                                    onItemClick = { index ->
                                        when (index) {
                                            0 -> {
                                                navigateToTab(
                                                    navController,
                                                    DashboardDestination.HOME.name,
                                                )
                                            }

                                            1 -> {
                                                navigateToTab(
                                                    navController,
                                                    DashboardDestination.FAVORITES.name,
                                                )
                                            }

                                            2 -> {
                                                navigateToTab(
                                                    navController,
                                                    DashboardDestination.BOOKINGS.name,
                                                )
                                            }

                                            3 -> {
                                                badgesViewModel.refreshInboxUnread()
                                                navigateToTab(
                                                    navController,
                                                    DashboardDestination.INBOX.name,
                                                )
                                            }

                                            4 -> {
                                                navigateToTab(
                                                    navController,
                                                    DashboardDestination.PROFILE.name,
                                                )
                                            }
                                        }

                                    },
                                )

                        },
                    ) {
                        val bottomPadding = it.calculateBottomPadding()

                        NavHost(
                            navController = navController,
                            startDestination = DashboardDestination.HOME.name,
                            modifier = Modifier.padding(bottom = bottomPadding),
                            enterTransition = {
                                fadeIn(
                                    animationSpec = tween(
                                        250,
                                        easing = LinearEasing,
                                    ),
                                ) + slideIntoContainer(
                                    animationSpec = tween(250, easing = EaseIn),
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                )
                            },
                            exitTransition = {
                                fadeOut(
                                    animationSpec = tween(
                                        200,
                                        easing = LinearEasing,
                                    ),
                                ) + slideOutOfContainer(
                                    animationSpec = tween(200, easing = EaseOut),
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                )
                            },
                            popEnterTransition = {
                                fadeIn(
                                    animationSpec = tween(
                                        250,
                                        easing = LinearEasing,
                                    ),
                                ) + slideIntoContainer(
                                    animationSpec = tween(250, easing = EaseIn),
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                )
                            },
                            popExitTransition = {
                                fadeOut(
                                    animationSpec = tween(
                                        200,
                                        easing = LinearEasing,
                                    ),
                                ) + slideOutOfContainer(
                                    animationSpec = tween(200, easing = EaseOut),
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                )
                            },
                        ) {
                            navigation(
                                startDestination = "home_root",
                                route = DashboardDestination.HOME.name
                            ) {
                                composable("home_root") {
                                    var showAuthSheet by remember { mutableStateOf(false) }
                                    HomeFeedScreen(
                                        onListingClick = { listingId ->
                                            navController.navigate("${PropertyDestination.DETAIL.name}/$listingId")
                                        },
                                        onSeeAll = { section ->
                                            navController.navigate("home_section/${section.name}")
                                        },
                                        onShowAuthSheet = { showAuthSheet = true }
                                    )

                                    if (showAuthSheet) {
                                        com.pacedream.app.ui.components.AuthFlowSheet(
                                            title = "Sign in",
                                            subtitle = "Sign in to save favorites.",
                                            onDismiss = { showAuthSheet = false },
                                            onSuccess = { showAuthSheet = false }
                                        )
                                    }
                                }

                                composable(
                                    route = "home_section/{sectionKey}",
                                    arguments = listOf(
                                        navArgument("sectionKey") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val raw = backStackEntry.arguments?.getString("sectionKey")
                                    val section = runCatching { HomeSectionKey.valueOf(raw ?: "") }
                                        .getOrDefault(HomeSectionKey.HOURLY)
                                    HomeSectionListScreen(
                                        section = section,
                                        onBack = { navController.popBackStack() },
                                        onListingClick = { listingId ->
                                            navController.navigate("${PropertyDestination.DETAIL.name}/$listingId")
                                        }
                                    )
                                }
                            }
                            
                            // Search Tab
                            navigation(
                                startDestination = "search_root",
                                route = DashboardDestination.SEARCH.name
                            ) {
                                composable("search_root") {
                                    var showAuthSheet by remember { mutableStateOf(false) }
                                    SearchScreen(
                                        onBackClick = { navController.popBackStack() },
                                        onListingClick = { propertyId ->
                                            navController.navigate("${PropertyDestination.DETAIL.name}/$propertyId")
                                        },
                                        onShowAuthSheet = { showAuthSheet = true }
                                    )

                                    if (showAuthSheet) {
                                        com.pacedream.app.ui.components.AuthFlowSheet(
                                            title = "Sign in",
                                            subtitle = "Sign in to save favorites.",
                                            onDismiss = { showAuthSheet = false },
                                            onSuccess = { showAuthSheet = false }
                                        )
                                    }
                                }
                            }

                            // Bookings Tab (3rd)
                            navigation(
                                startDestination = "bookings_root",
                                route = DashboardDestination.BOOKINGS.name
                            ) {
                                composable("bookings_root") {
                                val authGate = hiltViewModel<AuthGateViewModel>()
                                val authState by authGate.authState.collectAsStateWithLifecycle()
                                var showAuthSheet by remember { mutableStateOf(false) }

                                if (authState == com.shourov.apps.pacedream.core.network.auth.AuthState.Unauthenticated) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Sign in to view your bookings",
                                                style = PaceDreamTypography.Title3,
                                                color = PaceDreamColors.TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                                            androidx.compose.material3.Button(
                                                onClick = { showAuthSheet = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
                                            ) {
                                                Text("Sign in")
                                            }
                                        }
                                    }
                                } else {
                                    BookingTabScreen(
                                        onBookingClick = { bookingId ->
                                            navController.navigate("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                                        }
                                    )
                                }

                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        title = "Sign in",
                                        subtitle = "Sign in to view your bookings.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
                                    )
                                }
                            }
                            }
                            
                            // Favorites/Wishlist Tab with Auth Modal
                            navigation(
                                startDestination = "favorites_root",
                                route = DashboardDestination.FAVORITES.name
                            ) {
                                composable("favorites_root") {
                                var showAuthSheet by remember { mutableStateOf(false) }
                                
                                WishlistScreen(
                                    onNavigateToTimeBasedDetail = { itemId ->
                                        navController.navigate("${PropertyDestination.DETAIL.name}/$itemId")
                                    },
                                    onNavigateToGearDetail = { gearId ->
                                        navController.navigate("${PropertyDestination.DETAIL.name}/$gearId")
                                    },
                                    onShowAuthSheet = {
                                        showAuthSheet = true
                                    }
                                )
                                
                                // Auth Modal - shows over tabs (tabs remain visible)
                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        title = "Sign in",
                                        subtitle = "Sign in to access your favorites.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = {
                                            // Session bootstrap happens in session manager
                                        }
                                    )
                                    }
                                }
                            }
                            
                            // Inbox Tab - Connected to real ViewModel
                            navigation(
                                startDestination = "inbox_root",
                                route = DashboardDestination.INBOX.name
                            ) {
                                composable("inbox_root") {
                                var showAuthSheet by remember { mutableStateOf(false) }
                                
                                InboxScreen(
                                    onNavigateToThread = { threadId ->
                                        navController.navigate("${InboxDestination.THREAD.name}/$threadId")
                                    },
                                    onShowAuthSheet = {
                                        showAuthSheet = true
                                    }
                                )
                                
                                // Auth Modal
                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        title = "Sign in",
                                        subtitle = "Sign in to view your messages.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { }
                                    )
                                }
                            }
                            
                            // Thread Screen (Chat detail)
                            composable(
                                route = "${InboxDestination.THREAD.name}/{threadId}",
                                arguments = listOf(
                                    navArgument("threadId") {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
                                ThreadScreen(
                                    threadId = threadId,
                                    onBackClick = { navController.popBackStack() }
                                )
                                }
                            }
                            
                            // Profile Tab with Guest/Host mode
                            navigation(
                                startDestination = "profile_root",
                                route = DashboardDestination.PROFILE.name
                            ) {
                                composable("profile_root") {
                                val isHostMode by hostModeManager.isHostMode.collectAsState()
                                    var showAuthSheet by remember { mutableStateOf(false) }
                                val context = androidx.compose.ui.platform.LocalContext.current
                                ProfileTabScreen(
                                        onShowAuthSheet = { showAuthSheet = true },
                                    onEditProfileClick = {
                                        // Navigate to edit profile
                                    },
                                    onSettingsClick = {
                                        // Navigate to settings
                                    },
                                    onHelpClick = {
                                        // Navigate to help
                                    },
                                    onAboutClick = {
                                        // Navigate to about
                                    },
                                    onPrivacyPolicyClick = {
                                        try {
                                            val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                                            intent.launchUrl(context, Uri.parse("https://www.pacedream.com/privacy-policy"))
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/privacy-policy")))
                                        }
                                    },
                                    onTermsOfServiceClick = {
                                        try {
                                            val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                                            intent.launchUrl(context, Uri.parse("https://www.pacedream.com/terms-of-service"))
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/terms-of-service")))
                                        }
                                    },
                                    onLogoutClick = {
                                        // Handle logout
                                    },
                                    onSwitchToHostMode = {
                                        hostModeManager.setHostMode(true)
                                    },
                                    onSwitchToGuestMode = {
                                        hostModeManager.setHostMode(false)
                                    },
                                    isHostMode = isHostMode
                                )

                                    if (showAuthSheet) {
                                        com.pacedream.app.ui.components.AuthFlowSheet(
                                            title = "Sign in",
                                            subtitle = "Sign in to view your profile.",
                                            onDismiss = { showAuthSheet = false },
                                            onSuccess = { showAuthSheet = false }
                                        )
                                    }
                                }
                            }
                            
                            // Property Detail Screen
                            composable(
                                route = "${PropertyDestination.DETAIL.name}/{propertyId}",
                                arguments = listOf(
                                    navArgument("propertyId") {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
                                val authGate = hiltViewModel<AuthGateViewModel>()
                                val authState by authGate.authState.collectAsStateWithLifecycle()
                                var showAuthSheet by remember { mutableStateOf(false) }
                                PropertyDetailScreen(
                                    propertyId = propertyId,
                                    onBackClick = { navController.popBackStack() },
                                    onBookClick = { 
                                        if (authState == com.shourov.apps.pacedream.core.network.auth.AuthState.Unauthenticated) {
                                            showAuthSheet = true
                                        } else {
                                            navController.navigate("${BookingDestination.BOOKING_FORM.name}/$propertyId")
                                        }
                                    },
                                    onShareClick = { /* Handle share */ },
                                    onShowAuthSheet = { showAuthSheet = true }
                                )

                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        title = "Sign in",
                                        subtitle = "Sign in to book and save favorites.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
                                    )
                                }
                            }

                            composable(
                                route = "${BookingDestination.BOOKING_FORM.name}/{propertyId}",
                                arguments = listOf(
                                    navArgument("propertyId") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
                                val authGate = hiltViewModel<AuthGateViewModel>()
                                val authState by authGate.authState.collectAsStateWithLifecycle()
                                var showAuthSheet by remember { mutableStateOf(false) }
                                // Booking is a protected action; keep parity with iOS by gating with AuthFlowSheet.
                                if (authState == com.shourov.apps.pacedream.core.network.auth.AuthState.Unauthenticated) {
                                    // Don't force login globally, but booking is protected.
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Sign in to book", style = PaceDreamTypography.Title3)
                                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                                            androidx.compose.material3.Button(
                                                onClick = { showAuthSheet = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
                                            ) { Text("Sign in") }
                                        }
                                    }
                                } else {
                                    BookingFormScreen(
                                        propertyId = propertyId,
                                        onBookingCreated = { bookingId ->
                                            navController.navigate("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                                        }
                                    )
                                }

                                // If user is logged out mid-flow, the BookingFormViewModel will surface an error;
                                // keep an escape hatch to sign in.
                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        title = "Sign in",
                                        subtitle = "Sign in to complete your booking.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
                                    )
                                }
                            }
                            
                            // Search Screen
                            composable(
                                route = "${PropertyDestination.SEARCH.name}?destination={destination}&query={query}",
                                arguments = listOf(
                                    navArgument("destination") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("query") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
                                var showAuthSheet by remember { mutableStateOf(false) }
                                val destination = backStackEntry.arguments?.getString("destination")
                                val query = backStackEntry.arguments?.getString("query")
                                val initialQuery = query?.takeIf { it.isNotBlank() }
                                    ?: destination?.takeIf { it.isNotBlank() }
                                SearchScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onListingClick = { propertyId ->
                                        navController.navigate("${PropertyDestination.DETAIL.name}/$propertyId")
                                    },
                                    initialQuery = initialQuery,
                                    onShowAuthSheet = { showAuthSheet = true }
                                )

                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        title = "Sign in",
                                        subtitle = "Sign in to save favorites.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
                                    )
                                }
                            }
                            
                            // Filter Screen
                            composable(PropertyDestination.FILTER.name) {
                                FilterScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onApplyFilters = { /* Handle filter application */ }
                                )
                            }
                            
                            // Category List Screen
                            composable(
                                route = "${PropertyDestination.CATEGORY_LIST.name}/{category}",
                                arguments = listOf(
                                    navArgument("category") {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val category = backStackEntry.arguments?.getString("category") ?: ""
                                CategoryResultsScreen(
                                    category = category,
                                    onBack = { navController.popBackStack() },
                                    onListingClick = { propertyId ->
                                        navController.navigate("${PropertyDestination.DETAIL.name}/$propertyId")
                                    }
                                )
                            }
                            
                            // Destination List Screen
                            composable(PropertyDestination.DESTINATION_LIST.name) {
                                DestinationListScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onDestinationClick = { destination ->
                                        navController.navigate("${PropertyDestination.SEARCH.name}?destination=$destination")
                                    }
                                )
                            }
                            
                            // Recent Searches Screen
                            composable(PropertyDestination.RECENT_SEARCHES.name) {
                                RecentSearchesScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onSearchClick = { searchQuery ->
                                        navController.navigate("${PropertyDestination.SEARCH.name}?query=$searchQuery")
                                    }
                                )
                            }
                            
                            // Booking Confirmation Screen (Deep link target)
                            composable(
                                route = "${BookingDestination.BOOKING_CONFIRMATION.name}/{sessionId}/{bookingType}",
                                arguments = listOf(
                                    navArgument("sessionId") { type = NavType.StringType },
                                    navArgument("bookingType") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                                val bookingType = backStackEntry.arguments?.getString("bookingType") ?: "time_based"
                                
                                BookingConfirmationScreen(
                                    sessionId = sessionId,
                                    bookingType = bookingType,
                                    onViewBooking = { bookingId ->
                                        navController.navigate("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                                    },
                                    onGoHome = {
                                        navigateToTab(navController, DashboardDestination.HOME.name)
                                    },
                                    onClose = { navController.popBackStack() }
                                )
                            }
                            
                            // Booking Cancelled Screen (Deep link target)
                            composable(BookingDestination.BOOKING_CANCELLED.name) {
                                BookingCancelledScreen(
                                    onGoHome = {
                                        navigateToTab(navController, DashboardDestination.HOME.name)
                                    }
                                )
                            }
                            
                            // Booking Detail stub
                            composable(
                                route = "${BookingDestination.BOOKING_DETAIL.name}/{bookingId}",
                                arguments = listOf(
                                    navArgument("bookingId") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                                BookingDetailScreen(
                                    bookingId = bookingId,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
fun navigateToTab(
    navController: NavController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

