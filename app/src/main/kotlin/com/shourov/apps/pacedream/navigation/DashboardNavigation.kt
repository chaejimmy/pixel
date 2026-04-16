package com.shourov.apps.pacedream.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.pacedream.app.feature.listingdetail.ListingDetailRoute
import com.pacedream.app.feature.profile.EditProfileScreen
import com.pacedream.app.feature.checkout.BookingDraft
import com.pacedream.app.feature.checkout.BookingDraftCodec
import com.pacedream.app.feature.checkout.CheckoutScreen
import com.pacedream.app.feature.checkout.ConfirmationScreen
import com.shourov.apps.pacedream.feature.homefeed.HomeFeedScreen
import com.shourov.apps.pacedream.feature.homefeed.HomeSectionKey
import com.shourov.apps.pacedream.feature.homefeed.HomeSectionListScreen
import com.shourov.apps.pacedream.feature.search.SearchScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shourov.apps.pacedream.feature.home.presentation.components.FilterScreen
import com.shourov.apps.pacedream.feature.search.CategoryResultsScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.DestinationListScreen
import com.shourov.apps.pacedream.feature.destinations.DestinationsViewModel
import com.shourov.apps.pacedream.feature.home.presentation.components.RecentSearchesScreen
import com.shourov.apps.pacedream.feature.booking.presentation.BookingTabScreen
import com.shourov.apps.pacedream.feature.inbox.presentation.InboxScreen
import com.shourov.apps.pacedream.feature.inbox.presentation.ThreadScreen
import com.shourov.apps.pacedream.feature.profile.presentation.ProfileTabScreen
import com.shourov.apps.pacedream.feature.webflow.presentation.BookingConfirmationScreen
import com.shourov.apps.pacedream.feature.webflow.presentation.BookingCancelledScreen
import com.shourov.apps.pacedream.feature.notification.NotificationCenterScreen
import com.shourov.apps.pacedream.feature.wishlist.presentation.WishlistScreen
import com.shourov.apps.pacedream.feature.booking.presentation.BookingFormScreen
import com.shourov.apps.pacedream.feature.bookingdetail.BookingDetailScreen
import com.shourov.apps.pacedream.feature.host.data.ImageUploadService
import com.shourov.apps.pacedream.feature.host.navigation.ImageUploadEntryPoint
import com.shourov.apps.pacedream.feature.host.presentation.CreateListingScreen
import com.shourov.apps.pacedream.feature.host.presentation.ListingMode
import com.shourov.apps.pacedream.signin.navigation.DASHBOARD_ROUTE
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors

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

                // Refresh badge counts when returning from thread screens
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            badgesViewModel.refreshInboxUnread()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // iOS-parity: allow other screens to request switching tabs.
                LaunchedEffect(navController) {
                    TabRouter.events.collectLatest { destination ->
                        try {
                            navigateToTab(navController, destination.name)
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "TabRouter.navigateToTab failed for: ${destination.name}")
                        }
                    }
                }

                // iOS-parity: handle push notification in-tab navigation.
                LaunchedEffect(navController) {
                    NavigationRouter.events.collectLatest { route ->
                        try {
                            navController.navigate(route)
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "NavigationRouter.navigate failed for route: $route")
                        }
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

                    val backStackState by navController.currentBackStackEntryAsState()
                    val mainTabRoutes = remember {
                        setOf(
                            DashboardDestination.HOME.name,
                            DashboardDestination.FAVORITES.name,
                            DashboardDestination.BOOKINGS.name,
                            DashboardDestination.INBOX.name,
                            DashboardDestination.PROFILE.name
                        )
                    }

                    // Bottom bar visibility - always show for main tabs
                    val isBottomBarShow = remember(backStackState) {
                        val destination = backStackState?.destination ?: return@remember false
                        destination.hierarchy.any { it.route in mainTabRoutes }
                    }

                    val selectedItem = remember(backStackState) {
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

                    // iOS parity: listing detail presented as a modal bottom sheet
                    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }
                    // iOS parity: search presented as full-screen cover
                    var showSearchDialog by rememberSaveable { mutableStateOf(false) }

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

                        // iOS parity: fast crossfade for tab switches, slide for push/pop
                        val iOSEaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

                        NavHost(
                            navController = navController,
                            startDestination = DashboardDestination.HOME.name,
                            modifier = Modifier.padding(bottom = bottomPadding),
                            enterTransition = {
                                fadeIn(animationSpec = tween(200, easing = iOSEaseInOut)) +
                                    slideIntoContainer(
                                        animationSpec = tween(200, easing = iOSEaseInOut),
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        initialOffset = { it / 4 }
                                    )
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(150, easing = iOSEaseInOut))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(200, easing = iOSEaseInOut)) +
                                    slideIntoContainer(
                                        animationSpec = tween(200, easing = iOSEaseInOut),
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        initialOffset = { it / 4 }
                                    )
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(150, easing = iOSEaseInOut))
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
                                            selectedListingId = listingId
                                        },
                                        onSearchClick = { showSearchDialog = true },
                                        onSeeAll = { section ->
                                            navController.navigate("home_section/${section.name}")
                                        },
                                        onShowAuthSheet = { showAuthSheet = true },
                                        onNotificationClick = {
                                            navController.navigate("notifications")
                                        }
                                    )

                                    if (showAuthSheet) {
                                        com.pacedream.app.ui.components.AuthFlowSheet(
                                            subtitle = "Save your favorites and book spaces.",
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
                                        .getOrDefault(HomeSectionKey.SPACES)
                                    HomeSectionListScreen(
                                        section = section,
                                        onBack = { navController.popBackStack() },
                                        onListingClick = { listingId ->
                                            selectedListingId = listingId
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
                                            selectedListingId = propertyId
                                        },
                                        onShowAuthSheet = { showAuthSheet = true }
                                    )

                                    if (showAuthSheet) {
                                        com.pacedream.app.ui.components.AuthFlowSheet(
                                            subtitle = "Save your favorites and book spaces.",
                                            onDismiss = { showAuthSheet = false },
                                            onSuccess = { showAuthSheet = false }
                                        )
                                    }
                                }
                            }

                            // Bookings Tab (3rd) — fetches with role=renter|host like web
                            navigation(
                                startDestination = "bookings_root",
                                route = DashboardDestination.BOOKINGS.name
                            ) {
                                composable("bookings_root") {
                                var showAuthSheet by remember { mutableStateOf(false) }

                                BookingTabScreen(
                                    onBookingClick = { bookingId ->
                                        navController.navigate("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                                    },
                                    onNewBookingClick = {
                                        // Navigate to Home tab to explore spaces (iOS parity)
                                        TabRouter.switchTo(DashboardDestination.HOME)
                                    },
                                    onShowAuthSheet = { showAuthSheet = true }
                                )

                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        subtitle = "Manage your upcoming bookings.",
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
                                        selectedListingId = itemId
                                    },
                                    onNavigateToGearDetail = { gearId ->
                                        selectedListingId = gearId
                                    },
                                    onShowAuthSheet = {
                                        showAuthSheet = true
                                    },
                                    onExploreListings = {
                                        // iOS parity: switch to Home tab (same as iOS "Explore listings" → PD_SwitchToHomeTab)
                                        TabRouter.switchTo(DashboardDestination.HOME)
                                    }
                                )
                                
                                // Auth Modal - shows over tabs (tabs remain visible)
                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        subtitle = "Save your favorites and book spaces.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
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
                                        subtitle = "Connect with hosts and guests.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
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
                                val isHostMode by hostModeManager.isHostMode.collectAsStateWithLifecycle()
                                    var showAuthSheet by remember { mutableStateOf(false) }
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val profileAuthGate = hiltViewModel<AuthGateViewModel>()
                                ProfileTabScreen(
                                        onShowAuthSheet = { showAuthSheet = true },
                                    onEditProfileClick = {
                                        navController.navigate("edit_profile")
                                    },
                                    onSettingsClick = {
                                        navController.navigate("settings_root")
                                    },
                                    onNotificationsClick = {
                                        navController.navigate("settings_notifications")
                                    },
                                    onBookingsClick = {
                                        navigateToTab(navController, DashboardDestination.BOOKINGS.name)
                                    },
                                    onWishlistClick = {
                                        navigateToTab(navController, DashboardDestination.FAVORITES.name)
                                    },
                                    onHelpClick = {
                                        navController.navigate("support")
                                    },
                                    onFaqClick = {
                                        navController.navigate("faq")
                                    },
                                    onAboutClick = {
                                        navController.navigate("about")
                                    },
                                    onPrivacyPolicyClick = {
                                        try {
                                            val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                                            intent.launchUrl(context, Uri.parse("https://www.pacedream.com/privacy"))
                                        } catch (_: Exception) {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/privacy")))
                                            } catch (_: Exception) { /* No browser available */ }
                                        }
                                    },
                                    onTermsOfServiceClick = {
                                        try {
                                            val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                                            intent.launchUrl(context, Uri.parse("https://www.pacedream.com/terms"))
                                        } catch (_: Exception) {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/terms")))
                                            } catch (_: Exception) { /* No browser available */ }
                                        }
                                    },
                                    onLogoutClick = {
                                        // Clear BOTH auth systems. ProfileTabViewModel
                                        // already invokes the legacy AuthSession.signOut()
                                        // before this callback runs, but SessionManager
                                        // also holds its own _authState / _currentUser.
                                        // Call signOut() here so both sources flip to
                                        // Unauthenticated in the same pass (was a no-op
                                        // in 2026-04-12 E2E review → users could not
                                        // actually sign out without an app restart).
                                        profileAuthGate.signOut()
                                        hostModeManager.setHostMode(false)
                                        navigateToTab(navController, DashboardDestination.HOME.name)
                                    },
                                    onSwitchToHostMode = {
                                        hostModeManager.setHostMode(true)
                                    },
                                    onSwitchToGuestMode = {
                                        hostModeManager.setHostMode(false)
                                    },
                                    onCreateListingClick = {
                                        navController.navigate("create_listing") {
                                            launchSingleTop = true
                                        }
                                    },
                                    isHostMode = isHostMode,
                                    onReviewsClick = { navController.navigate("reviews") },
                                    onTripPlannerClick = { navController.navigate("trip_planner") },
                                    onDestinationsClick = { navController.navigate("destinations") }
                                )

                                    if (showAuthSheet) {
                                        com.pacedream.app.ui.components.AuthFlowSheet(
                                            subtitle = "Access your profile and settings.",
                                            onDismiss = { showAuthSheet = false },
                                            onSuccess = { showAuthSheet = false }
                                        )
                                    }
                                }
                            }

                            // FAQ Screen
                            composable("faq") {
                                com.shourov.apps.pacedream.feature.help.FaqScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Help & Support Screen
                            composable("support") {
                                com.shourov.apps.pacedream.feature.help.SupportScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onFaqClick = { navController.navigate("faq") }
                                )
                            }

                            // Notification Center Screen (iOS parity)
                            composable("notifications") {
                                NotificationCenterScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Property Detail Screen – replaced with new ListingDetailRoute
                            composable(
                                route = "${PropertyDestination.DETAIL.name}/{propertyId}",
                                arguments = listOf(
                                    navArgument("propertyId") {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
                                var showAuthSheet by remember { mutableStateOf(false) }

                                ListingDetailRoute(
                                    listingId = propertyId,
                                    onBackClick = { navController.popBackStack() },
                                    onLoginRequired = {
                                        showAuthSheet = true
                                    },
                                    onNavigateToInbox = {
                                        navigateToTab(navController, DashboardDestination.INBOX.name)
                                    },
                                    onNavigateToThread = { threadId ->
                                        navController.navigate("${InboxDestination.THREAD.name}/$threadId")
                                    },
                                    onNavigateToCheckout = { draft ->
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "booking_draft_json_${draft.listingId}",
                                            BookingDraftCodec.encode(draft)
                                        )
                                        navController.navigate("${BookingDestination.BOOKING_FORM.name}/${draft.listingId}")
                                    }
                                )

                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        subtitle = "Book spaces and save your favorites.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
                                    )
                                }
                            }

                            // Checkout Screen – receives BookingDraft via savedStateHandle (iOS parity)
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

                                // Decode the BookingDraft that was saved by the detail page
                                val draftJson = navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.get<String>("booking_draft_json_$propertyId")
                                val draft = draftJson?.let {
                                    runCatching { BookingDraftCodec.decode(it) }.getOrNull()
                                }

                                // Booking is a protected action; keep parity with iOS by gating with AuthFlowSheet.
                                if (authState == com.pacedream.app.core.auth.AuthState.Unauthenticated) {
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
                                } else if (draft != null) {
                                    // Use CheckoutScreen with the real BookingDraft and backend API.
                                    // On success, navigate to the native booking success screen
                                    // (ConfirmationScreen) — previously went straight to the
                                    // booking detail screen, which meant users never got an explicit
                                    // "Booking Confirmed" moment after Stripe PaymentSheet completed.
                                    CheckoutScreen(
                                        draft = draft,
                                        onBackClick = { navController.popBackStack() },
                                        onConfirmSuccess = { bookingId ->
                                            navController.navigate("booking_success/$bookingId") {
                                                // Pop checkout off the stack so back goes home/detail,
                                                // not back to a re-payable checkout screen.
                                                popUpTo("${BookingDestination.BOOKING_FORM.name}/$propertyId") {
                                                    inclusive = true
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    // Fallback: if draft is somehow missing, show BookingFormScreen
                                    BookingFormScreen(
                                        propertyId = propertyId,
                                        onBookingCreated = { bookingId ->
                                            navController.navigate("booking_success/$bookingId")
                                        }
                                    )
                                }

                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        subtitle = "Complete your booking.",
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
                                        selectedListingId = propertyId
                                    },
                                    initialQuery = initialQuery,
                                    onShowAuthSheet = { showAuthSheet = true }
                                )

                                if (showAuthSheet) {
                                    com.pacedream.app.ui.components.AuthFlowSheet(
                                        subtitle = "Save your favorites and book spaces.",
                                        onDismiss = { showAuthSheet = false },
                                        onSuccess = { showAuthSheet = false }
                                    )
                                }
                            }
                            
                            // Filter Screen
                            composable(PropertyDestination.FILTER.name) {
                                FilterScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onApplyFilters = { navController.popBackStack() }
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
                                val destinationsViewModel: DestinationsViewModel = hiltViewModel()
                                val destState by destinationsViewModel.state.collectAsStateWithLifecycle()
                                DestinationListScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onDestinationClick = { destination ->
                                        navController.navigate("${PropertyDestination.SEARCH.name}?destination=$destination")
                                    },
                                    popularDestinations = destState.popularDestinations,
                                    allDestinations = destState.allDestinations,
                                    isLoading = destState.isLoading,
                                    errorMessage = destState.errorMessage,
                                    onRetry = { destinationsViewModel.retry() }
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
                            
                            // Native Booking Success (post Stripe PaymentSheet).
                            // Shown after CheckoutScreen -> onConfirmSuccess so the user gets
                            // an explicit "Booking confirmed" moment with a reference code and
                            // clear next actions. Different from BOOKING_CONFIRMATION below,
                            // which is the web/deep-link landing target (driven by Stripe
                            // Checkout session redirect + server webhook).
                            composable(
                                route = "booking_success/{bookingId}",
                                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                                ConfirmationScreen(
                                    bookingId = bookingId,
                                    onBackClick = {
                                        // Back from success goes home, never back to checkout
                                        navigateToTab(navController, DashboardDestination.HOME.name)
                                    },
                                    onViewBooking = {
                                        navController.navigate("${BookingDestination.BOOKING_DETAIL.name}/$bookingId") {
                                            popUpTo("booking_success/$bookingId") { inclusive = true }
                                        }
                                    },
                                    onDone = {
                                        navigateToTab(navController, DashboardDestination.HOME.name)
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
                                    onBack = { navController.popBackStack() },
                                    onWriteReview = { bId, lId, title, location ->
                                        // Nav arguments are path segments, so any slashes or empty
                                        // strings would either be misrouted or crash. We URL-encode
                                        // each dynamic value and fall back to "-" for blanks so the
                                        // route always matches the declared pattern below.
                                        val safeB = bId.ifBlank { "-" }.encodePathSegment()
                                        val safeL = lId.ifBlank { "-" }.encodePathSegment()
                                        val safeT = title.ifBlank { "-" }.encodePathSegment()
                                        val safeLoc = location.ifBlank { "-" }.encodePathSegment()
                                        navController.navigate("write_review/$safeB/$safeL/$safeT/$safeLoc")
                                    }
                                )
                            }

                            // Write Review Screen
                            composable(
                                route = "write_review/{bookingId}/{listingId}/{bookingTitle}/{bookingLocation}",
                                arguments = listOf(
                                    navArgument("bookingId") { type = NavType.StringType },
                                    navArgument("listingId") { type = NavType.StringType },
                                    navArgument("bookingTitle") { type = NavType.StringType },
                                    navArgument("bookingLocation") { type = NavType.StringType },
                                )
                            ) {
                                com.pacedream.app.feature.reviews.WriteReviewScreen(
                                    onBack = { navController.popBackStack() },
                                    onReviewSubmitted = { navController.popBackStack() }
                                )
                            }

                            // ── New Feature Screens (iOS/Web parity) ─────────────

                            // Reviews Screen
                            composable("reviews") {
                                com.pacedream.app.feature.reviews.ReviewsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Trip Planner Screen
                            composable("trip_planner") {
                                com.pacedream.app.feature.tripplanner.TripPlannerScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Bidding Screen
                            composable("bids") {
                                com.pacedream.app.feature.bidding.BiddingScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Destination Screens
                            //
                            // The no-arg "destinations" route previously passed an empty
                            // destinationId down to DestinationListingsScreen, which then
                            // called repository.getDestination("") and always landed on
                            // the Failure branch. The user saw "Failed to load destination"
                            // with no way to recover — this looked like a broken feature.
                            //
                            // We now show the destination picker (DestinationsIndexScreen)
                            // so that entering this flow from Profile actually yields a
                            // list the user can pick from, and only then navigate to
                            // the detail route with a real id.
                            composable("destinations") {
                                com.pacedream.app.feature.destination.DestinationsIndexScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onDestinationClick = { destId ->
                                        if (destId.isNotBlank()) {
                                            navController.navigate("destination_listings/$destId")
                                        }
                                    }
                                )
                            }
                            composable(
                                route = "destination_listings/{destinationId}",
                                arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val destId = backStackEntry.arguments?.getString("destinationId") ?: ""
                                com.pacedream.app.feature.destination.DestinationListingsScreen(
                                    destinationId = destId,
                                    onBackClick = { navController.popBackStack() },
                                    onListingClick = { listingId ->
                                        selectedListingId = listingId
                                    }
                                )
                            }

                            // Edit Listing (Host)
                            composable(
                                route = "edit_listing/{listingId}",
                                arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                                val editListingViewModel: com.shourov.apps.pacedream.feature.host.presentation.EditListingViewModel = hiltViewModel()
                                com.shourov.apps.pacedream.feature.host.presentation.EditListingScreen(
                                    listingId = listingId,
                                    viewModel = editListingViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onSaveSuccess = { navController.popBackStack() },
                                    onManageCalendarClick = { id ->
                                        if (id.isNotBlank()) {
                                            navController.navigate("listing_calendar/$id")
                                        }
                                    },
                                )
                            }

                            // Listing Calendar Screen (shared with host mode)
                            composable(
                                route = "listing_calendar/{listingId}",
                                arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                            ) {
                                com.shourov.apps.pacedream.feature.host.presentation.ListingCalendarScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Edit Profile Screen (with photo editing)
                            composable("edit_profile") {
                                EditProfileScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // ── Settings & Account Screens (iOS parity) ──────────

                            // Settings Root Screen
                            composable("settings_root") {
                                val settingsAuthGate = hiltViewModel<AuthGateViewModel>()
                                com.pacedream.app.feature.settings.SettingsRootScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onPersonalInfoClick = { navController.navigate("settings_personal_info") },
                                    onLoginSecurityClick = { navController.navigate("settings_login_security") },
                                    onNotificationsClick = { navController.navigate("settings_notifications") },
                                    onPreferencesClick = { navController.navigate("settings_preferences") },
                                    onPaymentMethodsClick = { navController.navigate("settings_payment_methods") },
                                    onHelpSupportClick = { navController.navigate("support") },
                                    onIdentityVerificationClick = { navController.navigate("settings_identity_verification") },
                                    onLogoutClick = {
                                        settingsAuthGate.signOut()
                                        hostModeManager.setHostMode(false)
                                        navigateToTab(navController, DashboardDestination.HOME.name)
                                    }
                                )
                            }

                            // Personal Info Screen
                            composable("settings_personal_info") {
                                com.pacedream.app.feature.settings.personal.SettingsPersonalInfoScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Login & Security Screen
                            composable("settings_login_security") {
                                com.pacedream.app.feature.settings.security.SettingsLoginSecurityScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onAccountDeactivated = { navController.popBackStack() }
                                )
                            }

                            // Notifications Settings Screen
                            composable("settings_notifications") {
                                com.pacedream.app.feature.settings.notifications.SettingsNotificationsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Preferences Screen (Language & Region)
                            composable("settings_preferences") {
                                com.pacedream.app.feature.settings.preferences.SettingsPreferencesScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Payment Methods Screen
                            composable("settings_payment_methods") {
                                com.pacedream.app.feature.settings.payment.SettingsPaymentMethodsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Identity Verification Screen
                            composable("settings_identity_verification") {
                                com.pacedream.app.feature.verification.IdentityVerificationScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // About Us Screen
                            composable("about") {
                                com.pacedream.app.feature.about.AboutUsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Create Listing Screen (accessible from guest profile tab)
                            composable("create_listing") {
                                val context = LocalContext.current
                                val uploadService = try {
                                    EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        ImageUploadEntryPoint::class.java
                                    ).imageUploadService()
                                } catch (_: Exception) { null }

                                CreateListingScreen(
                                    listingMode = ListingMode.SHARE,
                                    imageUploadService = uploadService,
                                    onBackClick = { navController.popBackStack() },
                                    onPublishSuccess = { listingId ->
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }

                    // iOS parity: listing detail as modal bottom sheet (like iOS .sheet(item:))
                    if (selectedListingId != null) {
                        var showAuthSheetForDetail by remember { mutableStateOf(false) }

                        ModalBottomSheet(
                            onDismissRequest = { selectedListingId = null },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = PaceDreamColors.Background,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            dragHandle = { BottomSheetDefaults.DragHandle() },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.93f)
                            ) {
                            ListingDetailRoute(
                                listingId = selectedListingId ?: return@ModalBottomSheet,
                                onBackClick = { selectedListingId = null },
                                onLoginRequired = { showAuthSheetForDetail = true },
                                onNavigateToInbox = {
                                    selectedListingId = null
                                    navigateToTab(navController, DashboardDestination.INBOX.name)
                                },
                                onNavigateToThread = { threadId ->
                                    selectedListingId = null
                                    navController.navigate("${InboxDestination.THREAD.name}/$threadId")
                                },
                                onNavigateToCheckout = { draft ->
                                    selectedListingId = null
                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                        "booking_draft_json_${draft.listingId}",
                                        BookingDraftCodec.encode(draft)
                                    )
                                    navController.navigate("${BookingDestination.BOOKING_FORM.name}/${draft.listingId}")
                                }
                            )
                            } // end Box
                        }

                        if (showAuthSheetForDetail) {
                            com.pacedream.app.ui.components.AuthFlowSheet(
                                subtitle = "Book spaces and save your favorites.",
                                onDismiss = { showAuthSheetForDetail = false },
                                onSuccess = { showAuthSheetForDetail = false }
                            )
                        }
                    }

                    // iOS parity: search as full-screen cover (like iOS .fullScreenCover)
                    if (showSearchDialog) {
                        var showAuthSheetForSearch by remember { mutableStateOf(false) }

                        Dialog(
                            onDismissRequest = { showSearchDialog = false },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false,
                                decorFitsSystemWindows = false
                            )
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = PaceDreamColors.Background
                            ) {
                                SearchScreen(
                                    onBackClick = { showSearchDialog = false },
                                    onListingClick = { propertyId ->
                                        showSearchDialog = false
                                        selectedListingId = propertyId
                                    },
                                    onShowAuthSheet = { showAuthSheetForSearch = true }
                                )
                            }
                        }

                        if (showAuthSheetForSearch) {
                            com.pacedream.app.ui.components.AuthFlowSheet(
                                subtitle = "Save your favorites and book spaces.",
                                onDismiss = { showAuthSheetForSearch = false },
                                onSuccess = { showAuthSheetForSearch = false }
                            )
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
    try {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } catch (e: Exception) {
        timber.log.Timber.e(e, "navigateToTab failed for route: $route")
    }
}

/**
 * URL-encodes a string for use as a NavController path segment.
 *
 * NavHost splits routes on "/" and parses the URI, so raw strings with
 * slashes, spaces or reserved characters break matching. We percent-encode
 * via URLEncoder, then convert "+" back to "%20" because URLEncoder uses
 * form-encoding, but path segments expect "%20" for spaces. Consumers are
 * expected to pair this with [decodePathSegment] when reading the value
 * out of SavedStateHandle — if we skipped decoding, the user-visible text
 * on the Write Review header would include "%20" instead of real spaces.
 */
private fun String.encodePathSegment(): String =
    java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

