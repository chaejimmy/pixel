package com.pacedream.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pacedream.common.icon.PaceDreamIcons
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
import com.pacedream.app.feature.bookings.BookingListItem
import com.pacedream.app.feature.bookings.BookingsScreen
import com.pacedream.app.feature.home.HomeScreen
import com.pacedream.app.feature.home.HomeSectionListScreen
import com.pacedream.app.feature.inbox.InboxScreen
import com.pacedream.app.feature.inbox.ThreadScreen
import com.pacedream.app.feature.listingdetail.ListingCardModel
import com.pacedream.app.feature.listingdetail.ListingDetailRoute
import com.pacedream.app.feature.profile.ProfileScreen
import com.pacedream.app.feature.settings.SettingsRootScreen
import com.pacedream.app.feature.settings.help.SettingsHelpSupportScreen
import com.pacedream.app.feature.settings.notifications.SettingsNotificationsScreen
import com.pacedream.app.feature.settings.payment.SettingsPaymentMethodsScreen
import com.pacedream.app.feature.settings.personal.SettingsPersonalInfoScreen
import com.pacedream.app.feature.settings.preferences.SettingsPreferencesScreen
import com.pacedream.app.feature.settings.security.SettingsLoginSecurityScreen
import com.pacedream.app.feature.about.AboutUsScreen
import com.pacedream.app.feature.collections.CollectionsScreen
import com.pacedream.app.feature.roommate.RoommateFinderScreen
import com.shourov.apps.pacedream.feature.search.SearchScreen
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
    var authSheetSubtitle by remember { mutableStateOf("") }
    
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
                            when (sectionType) {
                                "roommate" -> navController.navigate(NavRoutes.ROOMMATE_FINDER)
                                else -> navController.navigate(NavRoutes.homeSectionList(sectionType))
                            }
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
                
                // Search Tab
                composable(NavRoutes.SEARCH) {
                    SearchScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onListingClick = { listingId ->
                            navController.navigate(NavRoutes.listingDetail(listingId))
                        },
                        onShowAuthSheet = {
                            authSheetSubtitle = "Save your favorites and book spaces."
                            showAuthSheet = true
                        }
                    )
                }
                
                // Favorites/Wishlist Tab
                composable(NavRoutes.FAVORITES) {
                    WishlistScreen(
                        onItemClick = { itemId, itemType ->
                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                set("listing_initial_type", itemType)
                            }
                            navController.navigate(NavRoutes.listingDetail(itemId))
                        },
                        onLoginRequired = {
                            authSheetSubtitle = "Save your favorites and book spaces."
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
                                authSheetSubtitle = "Manage your upcoming bookings."
                                showAuthSheet = true
                            }
                        )
                    } else {
                        BookingsScreen(
                            onBookingClick = { bookingId ->
                                navController.navigate(NavRoutes.bookingDetail(bookingId))
                            },
                            onBookingClickWithData = { item ->
                                // Pass cached booking data to detail screen via savedStateHandle
                                // This matches iOS pattern: pass existing BookingSummary for immediate display
                                navController.currentBackStackEntry?.savedStateHandle?.apply {
                                    set("cached_title", item.title)
                                    set("cached_imageUrl", item.imageUrl)
                                    set("cached_location", item.propertyLocation)
                                    set("cached_status", item.status)
                                    set("cached_amount", item.amount?.toString())
                                    set("cached_checkInDate", item.checkInDate)
                                    set("cached_checkInTime", item.checkInTime)
                                    set("cached_checkOutDate", item.checkOutDate)
                                    set("cached_checkOutTime", item.checkOutTime)
                                    set("cached_guestCount", item.guestCount.toString())
                                    set("cached_nightsCount", item.nightsCount.toString())
                                    set("cached_referenceId", item.referenceId)
                                    set("cached_hostName", item.hostName)
                                    set("cached_hostAvatarUrl", item.hostAvatarUrl)
                                    set("cached_hostId", item.hostId)
                                    set("cached_verificationPin", item.verificationPin)
                                    set("cached_pinStatus", item.pinStatus)
                                }
                                navController.navigate(NavRoutes.bookingDetail(item.id))
                            }
                        )
                    }
                }
                
                // Inbox Tab
                composable(NavRoutes.INBOX) {
                    if (authState != AuthState.Authenticated) {
                        // Show locked state, trigger auth modal
                        LockedScreen(
                            title = "Messages",
                            message = "Sign in to view your messages",
                            onSignInClick = {
                                authSheetSubtitle = "Connect with hosts and guests."
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
                            authSheetSubtitle = "Access your profile and settings."
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
                        onBookingsClick = {
                            navController.navigate(NavRoutes.BOOKINGS) {
                                popUpTo(NavRoutes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onFavoritesClick = {
                            navController.navigate(NavRoutes.FAVORITES) {
                                popUpTo(NavRoutes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onCreateListingClick = {
                            if (authState == AuthState.Authenticated) {
                                navController.navigate(NavRoutes.HOST_HOME)
                            } else {
                                authSheetSubtitle = "Sign in to create a listing."
                                showAuthSheet = true
                            }
                        },
                        onNotificationsClick = {
                            navController.navigate(NavRoutes.SETTINGS_NOTIFICATIONS)
                        },
                        onHelpClick = {
                            navController.navigate(NavRoutes.SETTINGS_HELP_SUPPORT)
                        },
                    )
                }

                // Settings root screen
                composable(NavRoutes.SETTINGS) {
                    SettingsRootScreen(
                        onBackClick = { navController.popBackStack() },
                        onPersonalInfoClick = {
                            navController.navigate(NavRoutes.SETTINGS_PERSONAL_INFO)
                        },
                        onLoginSecurityClick = {
                            navController.navigate(NavRoutes.SETTINGS_LOGIN_SECURITY)
                        },
                        onNotificationsClick = {
                            navController.navigate(NavRoutes.SETTINGS_NOTIFICATIONS)
                        },
                        onPreferencesClick = {
                            navController.navigate(NavRoutes.SETTINGS_PREFERENCES)
                        },
                        onPaymentMethodsClick = {
                            navController.navigate(NavRoutes.SETTINGS_PAYMENT_METHODS)
                        },
                        onHelpSupportClick = {
                            navController.navigate(NavRoutes.SETTINGS_HELP_SUPPORT)
                        },
                        onIdentityVerificationClick = {
                            navController.navigate(NavRoutes.IDENTITY_VERIFICATION)
                        },
                        onLogoutClick = {
                            sessionManager.signOut()
                            navController.popBackStack(NavRoutes.PROFILE, inclusive = false)
                        }
                    )
                }

                composable(NavRoutes.SETTINGS_PERSONAL_INFO) {
                    SettingsPersonalInfoScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.SETTINGS_LOGIN_SECURITY) {
                    SettingsLoginSecurityScreen(
                        onBackClick = { navController.popBackStack() },
                        onAccountDeactivated = {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(NavRoutes.SETTINGS_NOTIFICATIONS) {
                    SettingsNotificationsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.SETTINGS_PREFERENCES) {
                    SettingsPreferencesScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Payment Methods
                composable(NavRoutes.SETTINGS_PAYMENT_METHODS) {
                    SettingsPaymentMethodsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.SETTINGS_HELP_SUPPORT) {
                    SettingsHelpSupportScreen(
                        onBackClick = { navController.popBackStack() },
                        onOpenFaq = { navController.navigate(NavRoutes.FAQ) }
                    )
                }
                
                // FAQ Screen
                composable(NavRoutes.FAQ) {
                    com.pacedream.app.feature.faq.FAQScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // About Us Screen
                composable(NavRoutes.ABOUT_US) {
                    AboutUsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Roommate Finder Screen
                composable(NavRoutes.ROOMMATE_FINDER) {
                    RoommateFinderScreen(
                        onBackClick = { navController.popBackStack() },
                        onListingClick = { listingId ->
                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                set("listing_initial_type", "split-stay")
                            }
                            navController.navigate(NavRoutes.listingDetail(listingId))
                        }
                    )
                }

                // Identity Verification Screen
                composable(NavRoutes.IDENTITY_VERIFICATION) {
                    com.pacedream.app.feature.verification.IdentityVerificationScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Host Home - uses real backend-driven dashboard
                composable(NavRoutes.HOST_HOME) {
                    com.shourov.apps.pacedream.feature.host.presentation.HostDashboardScreenWithViewModel(
                        onAddListingClick = {},
                        onListingClick = { listingId ->
                            navController.navigate(NavRoutes.listingDetail(listingId))
                        },
                        onBookingClick = { bookingId ->
                            navController.navigate(NavRoutes.bookingDetail(bookingId))
                        },
                        onEarningsClick = {},
                        onAnalyticsClick = {}
                    )
                }

                // Edit Profile
                composable(NavRoutes.EDIT_PROFILE) {
                    com.pacedream.app.feature.profile.EditProfileScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Collection Detail
                composable(
                    route = NavRoutes.COLLECTION_DETAIL,
                    arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Collection: $collectionId", modifier = Modifier.padding(16.dp))
                    }
                }

                // Collections / My Lists Screen
                composable(NavRoutes.COLLECTIONS) {
                    CollectionsScreen(
                        onCollectionClick = { collectionId ->
                            navController.navigate(NavRoutes.collectionDetail(collectionId))
                        },
                        onLoginRequired = {
                            authSheetSubtitle = "Create and manage your lists."
                            showAuthSheet = true
                        }
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

                    val listingType = navController.previousBackStackEntry?.savedStateHandle
                        ?.get<String>("listing_initial_type") ?: ""

                    ListingDetailRoute(
                        listingId = listingId,
                        listingType = listingType,
                        initialListing = initialListing,
                        onBackClick = { navController.popBackStack() },
                        onLoginRequired = {
                            authSheetSubtitle = ""
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
                                // Pop checkout off back stack so back button
                                // doesn't return to an actionable pay screen
                                navController.navigate(NavRoutes.confirmation(bookingId)) {
                                    popUpTo(NavRoutes.checkout(listingId)) { inclusive = true }
                                }
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
                        onBackClick = {
                            // After payment success, back should go Home,
                            // not return to checkout
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
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
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
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
                
                // Booking Detail
                composable(
                    route = NavRoutes.BOOKING_DETAIL,
                    arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
                ) { backStackEntry ->
                    // Transfer cached booking data from previous screen's savedStateHandle
                    // to the current entry's savedStateHandle so BookingDetailViewModel can access it.
                    val prevHandle = navController.previousBackStackEntry?.savedStateHandle
                    val currHandle = backStackEntry.savedStateHandle
                    listOf(
                        "cached_title", "cached_imageUrl", "cached_location",
                        "cached_status", "cached_amount", "cached_checkInDate",
                        "cached_checkInTime", "cached_checkOutDate", "cached_checkOutTime",
                        "cached_guestCount", "cached_nightsCount", "cached_referenceId",
                        "cached_hostName", "cached_hostAvatarUrl", "cached_hostId",
                        "cached_verificationPin", "cached_pinStatus"
                    ).forEach { key ->
                        prevHandle?.get<String>(key)?.let { currHandle[key] = it }
                    }

                    com.pacedream.app.feature.bookings.BookingDetailScreen(
                        onBack = { navController.popBackStack() },
                        onContactHost = { hostId ->
                            navController.navigate(NavRoutes.threadDetail(hostId))
                        }
                    )
                }
            }
            
        }
    }

    if (showAuthSheet) {
        AuthFlowSheet(
            subtitle = authSheetSubtitle,
            onDismiss = { showAuthSheet = false },
            onSuccess = { showAuthSheet = false }
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
        TabItem(NavRoutes.HOME, "Home", PaceDreamIcons.Home, PaceDreamIcons.HomeOutlined),
        TabItem(NavRoutes.SEARCH, "Search", PaceDreamIcons.Search, PaceDreamIcons.SearchOutlined),
        TabItem(NavRoutes.FAVORITES, "Favorites", PaceDreamIcons.Favorite, PaceDreamIcons.FavoriteBorderOutlined),
        TabItem(NavRoutes.BOOKINGS, "Bookings", PaceDreamIcons.DateRange, PaceDreamIcons.DateRangeOutlined),
        TabItem(NavRoutes.INBOX, "Messages", PaceDreamIcons.Mail, PaceDreamIcons.MailOutline),
        TabItem(NavRoutes.PROFILE, "Profile", PaceDreamIcons.Person, PaceDreamIcons.PersonOutlined)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.pacedream.common.composables.theme.PaceDreamColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clean header matching iOS (no purple hero)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = com.pacedream.common.composables.theme.PaceDreamSpacing.MD,
                    end = com.pacedream.common.composables.theme.PaceDreamSpacing.MD,
                    top = com.pacedream.common.composables.theme.PaceDreamSpacing.MD,
                    bottom = com.pacedream.common.composables.theme.PaceDreamSpacing.SM
                )
        ) {
            Text(
                text = title,
                style = com.pacedream.common.composables.theme.PaceDreamTypography.Title1,
                color = com.pacedream.common.composables.theme.PaceDreamColors.TextPrimary
            )
        }

        // Centered locked state
        Spacer(modifier = Modifier.height(com.pacedream.common.composables.theme.PaceDreamSpacing.XXXL))

        Icon(
            PaceDreamIcons.Lock,
            contentDescription = null,
            tint = com.pacedream.common.composables.theme.PaceDreamColors.TextTertiary,
            modifier = Modifier.size(com.pacedream.common.composables.theme.PaceDreamIconSize.XXL)
        )

        Spacer(modifier = Modifier.height(com.pacedream.common.composables.theme.PaceDreamSpacing.MD))

        Text(
            text = "Sign in to continue",
            style = com.pacedream.common.composables.theme.PaceDreamTypography.Title3,
            color = com.pacedream.common.composables.theme.PaceDreamColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(com.pacedream.common.composables.theme.PaceDreamSpacing.SM))

        Text(
            text = message,
            style = com.pacedream.common.composables.theme.PaceDreamTypography.Body,
            color = com.pacedream.common.composables.theme.PaceDreamColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = com.pacedream.common.composables.theme.PaceDreamSpacing.XL)
        )

        Spacer(modifier = Modifier.height(com.pacedream.common.composables.theme.PaceDreamSpacing.LG))

        Button(
            onClick = onSignInClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = com.pacedream.common.composables.theme.PaceDreamColors.Primary
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                com.pacedream.common.composables.theme.PaceDreamRadius.MD
            ),
            modifier = Modifier
                .height(com.pacedream.common.composables.theme.PaceDreamButtonHeight.LG)
                .padding(horizontal = com.pacedream.common.composables.theme.PaceDreamSpacing.XL),
            contentPadding = PaddingValues(
                horizontal = com.pacedream.common.composables.theme.PaceDreamSpacing.XL,
                vertical = com.pacedream.common.composables.theme.PaceDreamSpacing.SM2
            )
        ) {
            Text(
                "Sign In",
                style = com.pacedream.common.composables.theme.PaceDreamTypography.Button,
                color = androidx.compose.ui.graphics.Color.White
            )
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

