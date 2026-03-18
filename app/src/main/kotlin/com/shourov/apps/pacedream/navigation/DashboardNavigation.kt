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
import com.pacedream.app.feature.listingdetail.ListingDetailRoute
import com.pacedream.app.feature.checkout.BookingDraft
import com.pacedream.app.feature.checkout.BookingDraftCodec
import com.pacedream.app.feature.checkout.CheckoutScreen
import com.shourov.apps.pacedream.feature.homefeed.HomeFeedScreen
import com.shourov.apps.pacedream.feature.homefeed.HomeSectionKey
import com.shourov.apps.pacedream.feature.homefeed.HomeSectionListScreen
import com.shourov.apps.pacedream.feature.search.SearchScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

                // iOS-parity: handle push notification in-tab navigation.
                LaunchedEffect(navController) {
                    NavigationRouter.events.collectLatest { route ->
                        navController.navigate(route)
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
                                d.route == DashboardDestination.PROFILE.name
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

                        // iOS 26 parity: 200ms easeInOut for all transitions
                        val iOSEaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

                        NavHost(
                            navController = navController,
                            startDestination = DashboardDestination.HOME.name,
                            modifier = Modifier.padding(bottom = bottomPadding),
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
                            popEnterTransition = {
                                fadeIn(
                                    animationSpec = tween(200, easing = iOSEaseInOut),
                                ) + slideIntoContainer(
                                    animationSpec = tween(200, easing = iOSEaseInOut),
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                )
                            },
                            popExitTransition = {
                                fadeOut(
                                    animationSpec = tween(200, easing = iOSEaseInOut),
                                ) + slideOutOfContainer(
                                    animationSpec = tween(200, easing = iOSEaseInOut),
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
                                            selectedListingId = listingId
                                        },
                                        onSearchClick = { showSearchDialog = true },
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
                                            title = "Sign in",
                                            subtitle = "Sign in to save favorites.",
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
                                    onShowAuthSheet = { showAuthSheet = true }
                                )

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
                                        selectedListingId = itemId
                                    },
                                    onNavigateToGearDetail = { gearId ->
                                        selectedListingId = gearId
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
                                        title = "Sign in",
                                        subtitle = "Sign in to view your messages.",
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
                                ProfileTabScreen(
                                        onShowAuthSheet = { showAuthSheet = true },
                                    onEditProfileClick = {
                                        // Navigate to edit profile
                                    },
                                    onSettingsClick = {
                                        // Navigate to settings
                                    },
                                    onHelpClick = {
                                        navController.navigate("support")
                                    },
                                    onFaqClick = {
                                        navController.navigate("faq")
                                    },
                                    onAboutClick = {
                                        // Navigate to about
                                    },
                                    onPrivacyPolicyClick = {
                                        try {
                                            val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                                            intent.launchUrl(context, Uri.parse("https://www.pacedream.com/privacy"))
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/privacy")))
                                        }
                                    },
                                    onTermsOfServiceClick = {
                                        try {
                                            val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                                            intent.launchUrl(context, Uri.parse("https://www.pacedream.com/terms"))
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pacedream.com/terms")))
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
                                    isHostMode = isHostMode,
                                    // iOS/Web parity: navigate to new feature screens
                                    onReviewsClick = { navController.navigate("reviews") },
                                    onBlogClick = { navController.navigate("blog") },
                                    onTripPlannerClick = { navController.navigate("trip_planner") },
                                    onSplitBookingsClick = { navController.navigate("split_bookings") },
                                    onBidsClick = { navController.navigate("bids") },
                                    onDestinationsClick = { navController.navigate("destinations") }
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
                                        title = "Sign in",
                                        subtitle = "Sign in to book and save favorites.",
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
                                    // Use CheckoutScreen with the real BookingDraft and backend API
                                    CheckoutScreen(
                                        draft = draft,
                                        onBackClick = { navController.popBackStack() },
                                        onConfirmSuccess = { bookingId ->
                                            navController.navigate("${BookingDestination.BOOKING_DETAIL.name}/$bookingId") {
                                                // Pop checkout off the stack so back goes to detail
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
                                            navController.navigate("${BookingDestination.BOOKING_DETAIL.name}/$bookingId")
                                        }
                                    )
                                }

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
                                        selectedListingId = propertyId
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

                            // ── New Feature Screens (iOS/Web parity) ─────────────

                            // Reviews Screen
                            composable("reviews") {
                                com.pacedream.app.feature.reviews.ReviewsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Blog Screens
                            composable("blog") {
                                com.pacedream.app.feature.blog.BlogListScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onPostClick = { postId ->
                                        navController.navigate("blog_detail/$postId")
                                    }
                                )
                            }
                            composable(
                                route = "blog_detail/{postId}",
                                arguments = listOf(navArgument("postId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                                com.pacedream.app.feature.blog.BlogDetailScreen(
                                    postId = postId,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Trip Planner Screen
                            composable("trip_planner") {
                                com.pacedream.app.feature.tripplanner.TripPlannerScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // Split Booking Screens
                            composable("split_bookings") {
                                com.pacedream.app.feature.splitbooking.SplitBookingListScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onSplitClick = { splitId ->
                                        navController.navigate("split_booking_detail/$splitId")
                                    }
                                )
                            }
                            composable(
                                route = "split_booking_detail/{splitId}",
                                arguments = listOf(navArgument("splitId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val splitId = backStackEntry.arguments?.getString("splitId") ?: ""
                                com.pacedream.app.feature.splitbooking.SplitBookingScreen(
                                    splitId = splitId,
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
                            composable("destinations") {
                                com.pacedream.app.feature.destination.DestinationLandingScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onDestinationClick = { destId ->
                                        navController.navigate("destination_listings/$destId")
                                    },
                                    onListingClick = { listingId ->
                                        selectedListingId = listingId
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
                                com.shourov.apps.pacedream.feature.host.presentation.EditListingScreen(
                                    listingId = listingId,
                                    onBackClick = { navController.popBackStack() },
                                    onSaveSuccess = { navController.popBackStack() }
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
                                title = "Sign in",
                                subtitle = "Sign in to book and save favorites.",
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
                                title = "Sign in",
                                subtitle = "Sign in to save favorites.",
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
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

