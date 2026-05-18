package com.shourov.apps.pacedream.feature.home.presentation.redesign

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.util.Consts
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRentedGearsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRoomsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenSplitStaysState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenViewModel
import com.shourov.apps.pacedream.feature.home.presentation.NavigationEvent

/**
 * Hilt-wired entry point for the redesigned home.
 *
 * Mirrors `EnhancedDashboardScreenWrapper` but renders the new marketplace home.
 *
 * Navigation is pushed up to the caller via callbacks — the caller decides
 * how to handle property clicks, search, notifications, the host CTA, and
 * "See all" actions per primary type.
 */
@Composable
fun HomeRedesignRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onPropertyClick: (listingId: String) -> Unit = {},
    onHostClick: () -> Unit = {},
    onSeeAll: (PrimaryType) -> Unit = {},
) {
    val roomsState by viewModel.homeScreenRoomsState.collectAsStateWithLifecycle()
    val gearsState by viewModel.homeScreenRentedGearsState.collectAsStateWithLifecycle()
    val splitStaysState by viewModel.homeScreenSplitStaysState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Kick off parallel loads on first composition, but skip the dispatch
    // when the ViewModel already holds data from a previous visit.  Without
    // this guard every pop-back from a detail screen triggered another full
    // refetch of all three sections (spaces / items / split-stays).
    //
    // The "needs initial load" check is intentionally local — re-evaluated on
    // every composition — so a manual sign-out that clears the VM state will
    // still cause a reload on the next visit.
    val needsInitialLoad = roomsState.rooms.isEmpty() &&
        gearsState.rentedGears.isEmpty() &&
        splitStaysState.splitStays.isEmpty() &&
        !roomsState.loading && !gearsState.loading && !splitStaysState.loading &&
        roomsState.error == null && gearsState.error == null && splitStaysState.error == null
    LaunchedEffect(Unit) {
        if (needsInitialLoad) {
            viewModel.onEvent(
                HomeScreenEvent.LoadAllSections(
                    roomType = Consts.ROOM_TYPE,
                    gearType = Consts.TECH_GEAR_TYPE,
                ),
            )
        }
    }

    // Surface navigation events emitted by the ViewModel (e.g. "See all" taps
    // from other entry points that share the same VM) back to the caller.
    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ToPropertyDetail -> onPropertyClick(event.propertyId)
                is NavigationEvent.ToSearch -> onSearchClick()
                is NavigationEvent.ToNotifications -> onNotificationClick()
                is NavigationEvent.ToSectionList -> {
                    // section is a free-form string; best-effort map to a PrimaryType.
                    val primary = when (event.section.lowercase()) {
                        "items", "gear", "rented_gear" -> PrimaryType.ITEMS
                        "services", "help" -> PrimaryType.SERVICES
                        else -> PrimaryType.SPACES
                    }
                    onSeeAll(primary)
                }
            }
        }
    }

    HomeRedesignScreenWrapper(
        modifier = modifier,
        roomsState = roomsState,
        gearsState = gearsState,
        splitStaysState = splitStaysState,
        favoriteIds = favoriteIds,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.onEvent(HomeScreenEvent.RefreshAll) },
        onFavoriteToggle = { id -> viewModel.onEvent(HomeScreenEvent.ToggleFavorite(id)) },
        onSearchClick = onSearchClick,
        onNotificationClick = onNotificationClick,
        onListingClick = { listing -> onPropertyClick(listing.id) },
        onHostClick = onHostClick,
        onSeeAll = onSeeAll,
    )
}

/**
 * Stateless adapter that maps domain state → presentational listings and
 * forwards to [HomeRedesignScreen]. Exposed separately from [HomeRedesignRoute]
 * so tests and previews can drive the screen without a Hilt graph.
 */
@Composable
fun HomeRedesignScreenWrapper(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState,
    favoriteIds: Set<String>,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onFavoriteToggle: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onListingClick: (Listing) -> Unit = {},
    onHostClick: () -> Unit = {},
    onSeeAll: (PrimaryType) -> Unit = {},
) {
    // The domain → UI mapping previously ran on every recomposition, which
    // — combined with the unstable home state classes (see
    // compose_compiler_config.conf) — re-mapped three lists on each keystroke,
    // scroll tick, etc.  Cache against the source lists so the mapping work
    // is only paid when the upstream data actually changes.
    val spacesListings = remember(roomsState.rooms) { roomsState.rooms.map { it.toListing() } }
    val itemsListings = remember(gearsState.rentedGears) { gearsState.rentedGears.map { it.toListing() } }
    val splitListings = remember(splitStaysState.splitStays) { splitStaysState.splitStays.map { it.toListing() } }

    HomeRedesignScreen(
        modifier = modifier,
        spacesListings = spacesListings,
        itemsListings = itemsListings,
        servicesListings = emptyList(), // no backend yet — primary rail shows a "coming soon" empty state
        splitListings = splitListings,
        favoriteIds = favoriteIds,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onSearchClick = onSearchClick,
        onNotificationClick = onNotificationClick,
        onListingClick = onListingClick,
        onFavoriteToggle = onFavoriteToggle,
        onHostClick = onHostClick,
        onSeeAll = onSeeAll,
    )
}
