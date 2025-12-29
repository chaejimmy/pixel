package com.shourov.apps.pacedream.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.shourov.apps.pacedream.feature.auth.presentation.AuthBottomSheet
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.pacedream.common.util.Consts.ROOM_TYPE
import com.pacedream.common.util.Consts.TECH_GEAR_TYPE
import com.shourov.apps.pacedream.feature.home.presentation.DashboardScreen
import com.shourov.apps.pacedream.feature.home.presentation.EnhancedDashboardScreenWrapper
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenViewModel
import com.shourov.apps.pacedream.feature.home.presentation.components.PropertyDetailScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.EnhancedDashboardScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.SearchScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.FilterScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.CategoryListScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.DestinationListScreen
import com.shourov.apps.pacedream.feature.home.presentation.components.RecentSearchesScreen
import com.shourov.apps.pacedream.feature.booking.presentation.BookingTabScreen
import com.shourov.apps.pacedream.feature.host.presentation.PostTabScreen
import com.shourov.apps.pacedream.feature.chat.presentation.InboxTabScreen
import com.shourov.apps.pacedream.feature.inbox.presentation.InboxScreen
import com.shourov.apps.pacedream.feature.inbox.presentation.ThreadScreen
import com.shourov.apps.pacedream.feature.profile.presentation.ProfileTabScreen
import com.shourov.apps.pacedream.feature.webflow.presentation.BookingConfirmationScreen
import com.shourov.apps.pacedream.feature.webflow.presentation.BookingCancelledScreen
import com.shourov.apps.pacedream.feature.wishlist.presentation.WishlistScreen
import com.shourov.apps.pacedream.signin.navigation.DASHBOARD_ROUTE

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
                // iOS-parity tabs: Home, Search, Favorites (Wishlist), Inbox, Profile
                val bottomNavList = mutableListOf(
                    BottomNavigationItem(
                        com.shourov.apps.pacedream.R.drawable.ic_home,
                        com.shourov.apps.pacedream.R.string.home,
                    ),
                    BottomNavigationItem(
                        com.shourov.apps.pacedream.R.drawable.ic_search,
                        com.shourov.apps.pacedream.R.string.search,
                    ),
                    BottomNavigationItem(
                        com.shourov.apps.pacedream.R.drawable.ic_favorite,
                        com.shourov.apps.pacedream.R.string.favorites,
                    ),
                    BottomNavigationItem(
                        com.shourov.apps.pacedream.R.drawable.ic_notifications,
                        com.shourov.apps.pacedream.R.string.inbox,
                    ),
                    BottomNavigationItem(
                        com.shourov.apps.pacedream.R.drawable.ic_profile,
                        com.shourov.apps.pacedream.R.string.profile,
                    ),
                )
                bottomNavList.apply {
                    val bottomNavigationItems = remember {
                        bottomNavList
                    }

                    val backStackState = navController.currentBackStackEntryAsState().value
                    var selectedItem by rememberSaveable {
                        mutableIntStateOf(0)
                    }

                    // Bottom bar visibility - always show for main tabs
                    val isBottomBarShow = remember(key1 = backStackState) {
                        backStackState?.destination?.route == DashboardDestination.HOME.name ||
                            backStackState?.destination?.route == DashboardDestination.SEARCH.name ||
                            backStackState?.destination?.route == DashboardDestination.FAVORITES.name ||
                            backStackState?.destination?.route == DashboardDestination.INBOX.name ||
                            backStackState?.destination?.route == DashboardDestination.PROFILE.name
                    }

                    selectedItem = remember(backStackState) {
                        when (backStackState?.destination?.route) {
                            DashboardDestination.HOME.name -> 0
                            DashboardDestination.SEARCH.name -> 1
                            DashboardDestination.FAVORITES.name -> 2
                            DashboardDestination.INBOX.name -> 3
                            else -> 4
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
                                                    DashboardDestination.SEARCH.name,
                                                )
                                            }

                                            2 -> {
                                                navigateToTab(
                                                    navController,
                                                    DashboardDestination.FAVORITES.name,
                                                )
                                            }

                                            3 -> {
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
                        ) {
                            composable(DashboardDestination.HOME.name) {
                                val homeScreenViewModel = hiltViewModel<HomeScreenViewModel>()
                                val homeScreenRoomsState =
                                    homeScreenViewModel.homeScreenRoomsState.collectAsStateWithLifecycle()
                                val homeScreenRentedGearsState =
                                    homeScreenViewModel.homeScreenRentedGearsState.collectAsStateWithLifecycle()
                                val homeScreenSplitStaysState =
                                    homeScreenViewModel.homeScreenSplitStaysState.collectAsStateWithLifecycle()
                                val isRefreshing =
                                    homeScreenViewModel.isRefreshing.collectAsStateWithLifecycle()
                                
                                // Load all sections in parallel on initial load
                                LaunchedEffect(Unit) {
                                    homeScreenViewModel.onEvent(
                                        HomeScreenEvent.LoadAllSections(
                                            roomType = ROOM_TYPE,
                                            gearType = TECH_GEAR_TYPE
                                        )
                                    )
                                }
                                
                                // Use the enhanced dashboard screen with all sections
                                EnhancedDashboardScreen(
                                    roomsState = homeScreenRoomsState.value,
                                    gearsState = homeScreenRentedGearsState.value,
                                    splitStaysState = homeScreenSplitStaysState.value,
                                    isRefreshing = isRefreshing.value,
                                    onTimeBasedRoomsChanged = { type ->
                                        homeScreenViewModel.onEvent(
                                            HomeScreenEvent.GetTimeBasedRooms(type)
                                        )
                                    },
                                    onRentedGearsChanged = { type ->
                                        homeScreenViewModel.onEvent(
                                            HomeScreenEvent.GetRentedGears(type)
                                        )
                                    },
                                    onSplitStaysRetry = {
                                        homeScreenViewModel.onEvent(HomeScreenEvent.GetSplitStays)
                                    },
                                    onRefresh = {
                                        homeScreenViewModel.onEvent(HomeScreenEvent.RefreshAll)
                                    },
                                    onPropertyClick = { propertyId ->
                                        navController.navigate("${PropertyDestination.DETAIL.name}/$propertyId")
                                    },
                                    onCategoryClick = { category ->
                                        navController.navigate("${PropertyDestination.CATEGORY_LIST.name}/$category")
                                    },
                                    onViewAllClick = { section ->
                                        when (section) {
                                            "categories" -> navController.navigate(PropertyDestination.CATEGORY_LIST.name)
                                            "destinations" -> navController.navigate(PropertyDestination.DESTINATION_LIST.name)
                                            "recent" -> navController.navigate(PropertyDestination.RECENT_SEARCHES.name)
                                            "time-based" -> navController.navigate(DashboardDestination.SEARCH.name)
                                            "gear" -> navController.navigate(DashboardDestination.SEARCH.name)
                                            "split-stays" -> navController.navigate(DashboardDestination.SEARCH.name)
                                            else -> { /* Handle other sections */ }
                                        }
                                    },
                                    onSearchClick = {
                                        navController.navigate(DashboardDestination.SEARCH.name)
                                    },
                                    onFilterClick = {
                                        navController.navigate(PropertyDestination.FILTER.name)
                                    },
                                    onNotificationClick = {
                                        navController.navigate(DashboardDestination.INBOX.name)
                                    }
                                )
                            }
                            
                            // Search Tab
                            composable(DashboardDestination.SEARCH.name) {
                                SearchScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onPropertyClick = { propertyId ->
                                        navController.navigate("${PropertyDestination.DETAIL.name}/$propertyId")
                                    }
                                )
                            }
                            
                            // Favorites/Wishlist Tab with Auth Modal
                            composable(DashboardDestination.FAVORITES.name) {
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
                                    AuthBottomSheet(
                                        onDismiss = { showAuthSheet = false },
                                        onLoginSuccess = {
                                            showAuthSheet = false
                                            // Refresh wishlist after login
                                        }
                                    )
                                }
                            }
                            
                            // Inbox Tab - Connected to real ViewModel
                            composable(DashboardDestination.INBOX.name) {
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
                                    AuthBottomSheet(
                                        onDismiss = { showAuthSheet = false },
                                        onLoginSuccess = {
                                            showAuthSheet = false
                                        }
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
                            
                            // Profile Tab with Guest/Host mode
                            composable(DashboardDestination.PROFILE.name) {
                                val isHostMode by hostModeManager.isHostMode.collectAsState()
                                ProfileTabScreen(
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
                                PropertyDetailScreen(
                                    propertyId = propertyId,
                                    onBackClick = { navController.popBackStack() },
                                    onBookClick = { 
                                        navController.navigate("${BookingDestination.BOOKING_FORM.name}/$propertyId")
                                    },
                                    onShareClick = { /* Handle share */ },
                                    onFavoriteClick = { /* Handle favorite */ }
                                )
                            }
                            
                            // Search Screen
                            composable(PropertyDestination.SEARCH.name) {
                                SearchScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onPropertyClick = { propertyId ->
                                        navController.navigate("${PropertyDestination.DETAIL.name}/$propertyId")
                                    }
                                )
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
                                CategoryListScreen(
                                    category = category,
                                    onBackClick = { navController.popBackStack() },
                                    onPropertyClick = { propertyId ->
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
                                        navController.navigate(DashboardDestination.HOME.name) {
                                            popUpTo(DashboardDestination.HOME.name) { inclusive = true }
                                        }
                                    },
                                    onClose = { navController.popBackStack() }
                                )
                            }
                            
                            // Booking Cancelled Screen (Deep link target)
                            composable(BookingDestination.BOOKING_CANCELLED.name) {
                                BookingCancelledScreen(
                                    onGoHome = {
                                        navController.navigate(DashboardDestination.HOME.name) {
                                            popUpTo(DashboardDestination.HOME.name) { inclusive = true }
                                        }
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
                                // TODO: Implement BookingDetailScreen
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Booking Detail: $bookingId")
                                }
                            }
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
        navController.graph.startDestinationRoute?.let { homeScreen ->
            popUpTo(homeScreen) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

