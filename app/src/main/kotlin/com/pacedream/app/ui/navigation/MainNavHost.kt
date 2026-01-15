package com.pacedream.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.feature.checkout.BookingDraftCodec
import com.pacedream.app.feature.checkout.CheckoutScreen
import com.pacedream.app.feature.checkout.ConfirmationScreen
import com.pacedream.app.feature.bookings.BookingsScreen
import com.pacedream.app.feature.home.HomeScreen
import com.pacedream.app.feature.home.HomeSectionListScreen
import com.pacedream.app.feature.inbox.InboxScreen
import com.pacedream.app.feature.inbox.ThreadScreen
import com.pacedream.app.feature.listingdetail.ListingCardModel
import com.pacedream.app.feature.listingdetail.ListingDetailRoute
import com.pacedream.app.feature.profile.ProfileScreen
import com.pacedream.app.feature.webflow.BookingCancelledScreen
import com.pacedream.app.feature.webflow.BookingConfirmationScreen
import com.pacedream.app.feature.wishlist.WishlistScreen
import com.pacedream.app.ui.components.AuthFlowSheet

/**
 * MainNavHost - Root navigation with stable bottom tabs
 * 
 * iOS Parity:
 * - Bottom tabs are ALWAYS visible, even when logged out
 * - Auth is presented as a modal overlay (sheet/dialog), not root reset
 * - Tabs preserve state when switching between them
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost(
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Auth state
    val authState by sessionManager.authState.collectAsStateWithLifecycle()
    
    // Auth modal state
    var showAuthSheet by remember { mutableStateOf(false) }
    var authSheetTitle by remember { mutableStateOf("Sign in") }
    var authSheetSubtitle by remember { mutableStateOf("Sign in to continue.") }
    
    // Determine if bottom bar should be shown (always for main tabs)
    val showBottomBar = remember(currentRoute) {
        currentRoute in listOf(
            NavRoutes.HOME,
            NavRoutes.SEARCH,
            NavRoutes.FAVORITES,
            NavRoutes.BOOKINGS,
            NavRoutes.INBOX,
            NavRoutes.PROFILE
        )
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Bottom tabs are ALWAYS visible (iOS parity)
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                PaceDreamBottomBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                // Home Tab
                composable(NavRoutes.HOME) {
                    HomeScreen(
                        onSectionViewAll = { sectionType ->
                            navController.navigate(NavRoutes.homeSectionList(sectionType))
                        },
                        onListingClick = { item ->
                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                set("listing_initial_id", item.id)
                                set("listing_initial_title", item.title)
                                set("listing_initial_imageUrl", item.imageUrl)
                                set("listing_initial_location", item.location)
                                set("listing_initial_price", item.price)
                                set("listing_initial_rating", item.rating)
                                set("listing_initial_type", item.type)
                            }
                            val listingId = item.id
                            navController.navigate(NavRoutes.listingDetail(listingId))
                        },
                        onSearchClick = {
                            navController.navigate(NavRoutes.SEARCH)
                        },
                        onCategoryClick = { categoryName ->
                            // Navigate to search with category filter
                            navController.navigate("${NavRoutes.SEARCH}?category=$categoryName")
                        },
                        onCategoryFilterClick = { categoryFilter ->
                            // Filter listings by category (could update view model state)
                            // For now, navigate to search with filter
                            navController.navigate("${NavRoutes.SEARCH}?filter=$categoryFilter")
                        }
                    )
                }
                
                // Home Section List (View All)
                composable(
                    route = NavRoutes.HOME_SECTION_LIST,
                    arguments = listOf(navArgument("sectionType") { type = NavType.StringType })
                ) { backStackEntry ->
                    val sectionType = backStackEntry.arguments?.getString("sectionType") ?: ""
                    HomeSectionListScreen(
                        sectionType = sectionType,
                        onBackClick = { navController.popBackStack() },
                        onListingClick = { item ->
                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                set("listing_initial_id", item.id)
                                set("listing_initial_title", item.title)
                                set("listing_initial_imageUrl", item.imageUrl)
                                set("listing_initial_location", item.location)
                                set("listing_initial_price", item.price)
                                set("listing_initial_rating", item.rating)
                                set("listing_initial_type", item.type)
                            }
                            val listingId = item.id
                            navController.navigate(NavRoutes.listingDetail(listingId))
                        }
                    )
                }
                
                // Search Tab (placeholder)
                composable(NavRoutes.SEARCH) {
                    SearchPlaceholderScreen()
                }
                
                // Favorites/Wishlist Tab
                composable(NavRoutes.FAVORITES) {
                    WishlistScreen(
                        onItemClick = { itemId, itemType ->
                            navController.navigate(NavRoutes.listingDetail(itemId))
                        },
                        onLoginRequired = {
                            authSheetTitle = "Sign in"
                            authSheetSubtitle = "Sign in to access your favorites."
                            showAuthSheet = true
                        }
                    )
                }

                // Bookings Tab
                composable(NavRoutes.BOOKINGS) {
                    if (authState != AuthState.Authenticated) {
                        LockedScreen(
                            title = "Bookings",
                            message = "Sign in to view your bookings",
                            onSignInClick = {
                                authSheetTitle = "Sign in"
                                authSheetSubtitle = "Sign in to view your bookings."
                                showAuthSheet = true
                            }
                        )
                    } else {
                        BookingsScreen(
                            onBookingClick = { bookingId ->
                                navController.navigate(NavRoutes.bookingDetail(bookingId))
                            }
                        )
                    }
                }
                
                // Inbox Tab
                composable(NavRoutes.INBOX) {
                    if (authState != AuthState.Authenticated) {
                        // Show locked state, trigger auth modal
                        LockedScreen(
                            title = "Inbox",
                            message = "Sign in to view your messages",
                            onSignInClick = {
                                authSheetTitle = "Sign in"
                                authSheetSubtitle = "Sign in to view your messages."
                                showAuthSheet = true
                            }
                        )
                    } else {
                        InboxScreen(
                            onThreadClick = { threadId ->
                                navController.navigate(NavRoutes.threadDetail(threadId))
                            }
                        )
                    }
                }
                
                // Thread Detail
                composable(
                    route = NavRoutes.THREAD_DETAIL,
                    arguments = listOf(navArgument("threadId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
                    ThreadScreen(
                        threadId = threadId,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                
                // Profile Tab
                composable(NavRoutes.PROFILE) {
                    ProfileScreen(
                        onLoginClick = {
                            authSheetTitle = "Sign in"
                            authSheetSubtitle = "Sign in to manage your profile."
                            showAuthSheet = true
                        },
                        onHostModeClick = {
                            navController.navigate(NavRoutes.HOST_HOME)
                        },
                        onEditProfileClick = {
                            navController.navigate(NavRoutes.EDIT_PROFILE)
                        },
                        onSettingsClick = {
                            navController.navigate(NavRoutes.SETTINGS)
                        },
                        onIdentityVerificationClick = {
                            navController.navigate(NavRoutes.IDENTITY_VERIFICATION)
                        },
                        onHelpClick = {
                            navController.navigate(NavRoutes.FAQ)
                        }
                    )
                }
                
                // FAQ Screen
                composable(NavRoutes.FAQ) {
                    com.pacedream.app.feature.faq.FAQScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                
                // Identity Verification Screen
                composable(NavRoutes.IDENTITY_VERIFICATION) {
                    com.pacedream.app.feature.verification.IdentityVerificationScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                
                // Listing Detail (stub)
                composable(
                    route = NavRoutes.LISTING_DETAIL,
                    arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                    val initialListing = navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                        val id = handle.get<String>("listing_initial_id")
                        val title = handle.get<String>("listing_initial_title")
                        if (!id.isNullOrBlank() && !title.isNullOrBlank()) {
                            ListingCardModel(
                                id = id,
                                title = title,
                                imageUrl = handle.get("listing_initial_imageUrl"),
                                location = handle.get("listing_initial_location"),
                                priceLabel = handle.get("listing_initial_price"),
                                rating = handle.get("listing_initial_rating"),
                                type = handle.get("listing_initial_type") ?: ""
                            )
                        } else null
                    }

                    ListingDetailRoute(
                        listingId = listingId,
                        initialListing = initialListing,
                        onBackClick = { navController.popBackStack() },
                        onLoginRequired = {
                            authSheetTitle = "Sign in"
                            authSheetSubtitle = "Sign in to continue."
                            showAuthSheet = true
                        },
                        onNavigateToInbox = {
                            navController.navigate(NavRoutes.INBOX) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToCheckout = { draft ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "booking_draft_json_${draft.listingId}",
                                BookingDraftCodec.encode(draft)
                            )
                            navController.navigate(NavRoutes.checkout(draft.listingId))
                        }
                    )
                }

                // Native Checkout
                composable(
                    route = NavRoutes.CHECKOUT,
                    arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                ) { entry ->
                    val listingId = entry.arguments?.getString("listingId") ?: ""
                    val raw = navController.previousBackStackEntry?.savedStateHandle
                        ?.get<String>("booking_draft_json_${listingId}")
                    val draft = raw?.let { runCatching { BookingDraftCodec.decode(it) }.getOrNull() }
                    if (draft == null) {
                        BookingDetailPlaceholder(
                            bookingId = "Missing BookingDraft",
                            onBackClick = { navController.popBackStack() }
                        )
                    } else {
                        CheckoutScreen(
                            draft = draft,
                            onBackClick = { navController.popBackStack() },
                            onConfirmSuccess = { bookingId ->
                                navController.navigate(NavRoutes.confirmation(bookingId))
                            }
                        )
                    }
                }

                // Native Confirmation
                composable(
                    route = NavRoutes.CONFIRMATION,
                    arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
                ) { entry ->
                    val bookingId = entry.arguments?.getString("bookingId") ?: ""
                    ConfirmationScreen(
                        bookingId = bookingId,
                        onBackClick = { navController.popBackStack() },
                        onViewBooking = {
                            navController.navigate(NavRoutes.BOOKINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            navController.navigate(NavRoutes.bookingDetail(bookingId))
                        },
                        onDone = {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                
                // Booking Confirmation - Timebased (with deep link)
                composable(
                    route = NavRoutes.BOOKING_CONFIRMATION_TIMEBASED,
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "https://www.pacedream.com/booking-success?session_id={sessionId}&type=timebased"
                        },
                        navDeepLink {
                            uriPattern = "https://pacedream.com/booking-success?session_id={sessionId}&type=timebased"
                        }
                    )
                ) {
                    BookingConfirmationScreen(
                        bookingType = "timebased",
                        onViewBookingClick = { bookingId ->
                            navController.navigate(NavRoutes.bookingDetail(bookingId))
                        },
                        onHomeClick = {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                
                // Booking Confirmation - Gear (with deep link)
                composable(
                    route = NavRoutes.BOOKING_CONFIRMATION_GEAR,
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "https://www.pacedream.com/booking-success?session_id={sessionId}&type=gear"
                        },
                        navDeepLink {
                            uriPattern = "https://pacedream.com/booking-success?session_id={sessionId}&type=gear"
                        }
                    )
                ) {
                    BookingConfirmationScreen(
                        bookingType = "gear",
                        onViewBookingClick = { bookingId ->
                            navController.navigate(NavRoutes.bookingDetail(bookingId))
                        },
                        onHomeClick = {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                
                // Booking Cancelled (with deep link)
                composable(
                    route = NavRoutes.BOOKING_CANCELLED,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "https://www.pacedream.com/booking-cancelled" },
                        navDeepLink { uriPattern = "https://pacedream.com/booking-cancelled" }
                    )
                ) {
                    BookingCancelledScreen(
                        onHomeClick = {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                
                // Booking Detail (stub)
                composable(
                    route = NavRoutes.BOOKING_DETAIL,
                    arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                    BookingDetailPlaceholder(
                        bookingId = bookingId,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            
        }
    }

    if (showAuthSheet) {
        AuthFlowSheet(
            title = authSheetTitle,
            subtitle = authSheetSubtitle,
            onDismiss = { showAuthSheet = false },
            onSuccess = {
                // session bootstrap happens inside session manager
            }
        )
    }
}

/**
 * Bottom navigation bar with 5 stable tabs
 */
@Composable
fun PaceDreamBottomBar(
    navController: NavHostController,
    currentRoute: String?
) {
    val tabs = listOf(
        TabItem(NavRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        TabItem(NavRoutes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
        TabItem(NavRoutes.FAVORITES, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        TabItem(NavRoutes.BOOKINGS, "Bookings", Icons.Filled.DateRange, Icons.Outlined.DateRange),
        TabItem(NavRoutes.INBOX, "Inbox", Icons.Filled.Mail, Icons.Outlined.MailOutline),
        TabItem(NavRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )
    
    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) }
            )
        }
    }
}

data class TabItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// Placeholder screens
@Composable
fun SearchPlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Search - Coming Soon", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun LockedScreen(
    title: String,
    message: String,
    onSignInClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSignInClick) {
                Text("Sign In")
            }
        }
    }
}

@Composable
fun ListingDetailPlaceholder(listingId: String, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Listing Detail: $listingId", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun BookingDetailPlaceholder(bookingId: String, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Booking Detail: $bookingId", modifier = Modifier.padding(16.dp))
    }
}

