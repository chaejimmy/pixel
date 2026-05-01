package com.shourov.apps.pacedream.feature.search

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.components.InlineErrorBanner
import com.pacedream.common.composables.components.PaceDreamEmptyState
import com.pacedream.common.composables.components.PaceDreamErrorState
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.listing.ListingPreview
import com.shourov.apps.pacedream.listing.ListingPreviewStore
import com.pacedream.app.core.location.LocationService
import com.pacedream.app.core.location.LocationServiceEntryPoint
import com.pacedream.app.core.location.PlacePrediction
import com.pacedream.app.core.location.PlacesAutocompleteService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Category chips per marketplace mode (matching website CATEGORIES_BY_MODE)
 */
private val CATEGORIES_BY_MODE: Map<String, List<String>> = mapOf(
    "SHARE" to listOf(
        "Restrooms", "Nap Pods", "Meeting Rooms", "Study Rooms",
        "Short Stays", "Studios", "Parking", "Storage Space"
    ),
    "BORROW" to listOf(
        "Sports Gear", "Cameras", "Tech Gadgets", "E-Bikes",
        "Scooters", "Musical Instruments", "Books", "Games"
    ),
    "SPLIT" to listOf(
        "Subscription", "Sports", "WIFI", "Events"
    )
)

/**
 * Sort options matching the website's sort dropdown
 */
private data class SortOption(val value: String, val label: String)

private val SORT_OPTIONS = listOf(
    SortOption("relevance", "Relevance"),
    SortOption("price_low", "Price: Low to High"),
    SortOption("price_high", "Price: High to Low"),
    SortOption("rating", "Rating")
)

/**
 * Map frontend category names to backend category names (website parity)
 */
private fun mapCategoryToBackend(cat: String): String {
    val categoryMap = mapOf(
        "Restrooms" to "restroom",
        "Nap Pods" to "nap_pod",
        "Meeting Rooms" to "meeting_room",
        "Study Rooms" to "study_room",
        "Short Stays" to "short_stay",
        "Studios" to "apartment",
        "Parking" to "parking",
        "Storage Space" to "storage_space",
        "Sports Gear" to "sports_gear",
        "Cameras" to "camera",
        "Tech Gadgets" to "tech",
        "E-Bikes" to "micromobility",
        "Scooters" to "micromobility",
        "Musical Instruments" to "instrument",
        "Books" to "tech",
        "Games" to "games",
        "Subscription" to "subscription",
        "Sports" to "sports",
        "WIFI" to "wifi",
        "Events" to "membership"
    )
    return categoryMap[cat] ?: cat.lowercase().replace(" ", "_")
}

/**
 * Target field to auto-engage when the search screen opens.  Used so the
 * hero "What / Where / When" segments on Home route the user directly to
 * the right picker in a single tap, matching Airbnb/Turo.  WHO is kept so
 * booking / listing detail entry points can still deep-link to the guest
 * sheet; the Home search bar no longer surfaces it.
 */
enum class SearchInitialFocus { WHAT, WHERE, WHEN, WHO }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onListingClick: (String) -> Unit,
    initialQuery: String? = null,
    initialFocus: SearchInitialFocus? = null,
    onShowAuthSheet: () -> Unit = {},
    onOpenFilters: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    var inlineBannerMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Hero "Where / When / Who" focus state — hoisted so the guest
    // sheet can be rendered at the root of SearchScreen regardless of
    // where the search bar lives inside the Scaffold.
    // adultGuests lives in SearchUiState so it survives rotation and
    // is reflected in the collapsed summary bar; the sheet reads from
    // and writes to the ViewModel.  NOTE: not yet forwarded to the
    // search repository — see SearchUiState.adultGuests doc.
    val adultGuests = state.adultGuests
    var showGuestsSheet by remember { mutableStateOf(false) }
    var pickerHandled by remember { mutableStateOf(false) }

    // Sort state
    var selectedSort by remember { mutableStateOf("relevance") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Category filter state
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LocationServiceEntryPoint::class.java
        )
    }
    val locationService = remember { entryPoint.locationService() }
    val placesService = remember { entryPoint.placesAutocompleteService() }
    var placeSuggestions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var placesJob by remember { mutableStateOf<Job?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasPermission) {
            scope.launch {
                val location = locationService.getCurrentLocation()
                if (location != null) {
                    val address = locationService.getAddressFromLocation(
                        location.latitude,
                        location.longitude
                    )
                    if (address != null) {
                        viewModel.onQueryChanged(address)
                        viewModel.updateSearchParams(city = address)
                        inlineBannerMessage = ("Location set: $address")
                    } else {
                        inlineBannerMessage = ("Could not determine address")
                    }
                } else {
                    inlineBannerMessage = ("Location unavailable")
                }
            }
        } else {
            inlineBannerMessage = "Location permission denied"
        }
    }

    // Clear categories when mode changes (categories differ per mode, like website)
    val currentShareType = state.shareType?.uppercase() ?: "SHARE"
    LaunchedEffect(currentShareType) {
        val validCats = CATEGORIES_BY_MODE[currentShareType] ?: emptyList()
        selectedCategories = selectedCategories.filter { it in validCats }.toSet()
    }

    LaunchedEffect(initialQuery) {
        val q = initialQuery?.trim().orEmpty()
        if (q.isNotBlank() && viewModel.uiState.value.query.isBlank()) {
            viewModel.onQueryChanged(q)
            viewModel.submitSearch()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            PaceDreamIcons.ArrowBack,
                            contentDescription = "Go back",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        "Explore",
                        style = PaceDreamTypography.Headline
                    )
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.phase == SearchPhase.Loading && state.items.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Inline banner
                InlineErrorBanner(
                    message = inlineBannerMessage ?: "",
                    isVisible = inlineBannerMessage != null,
                    onDismiss = { inlineBannerMessage = null },
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM)
                )

                // Enhanced Search Bar with collapse support
                var isSearchBarExpanded by remember { mutableStateOf(true) }
                val hasResults = state.phase == SearchPhase.Success || state.phase == SearchPhase.LoadingMore
                var selectedTab by remember { mutableStateOf(com.pacedream.app.feature.search.SearchTab.SPACES) }
                var whatQuery by remember { mutableStateOf(state.whatQuery ?: "") }
                var whereQuery by remember { mutableStateOf(state.query) }
                val (selectedDateDisplay, selectedDateISO, openDatePicker) = com.pacedream.app.feature.search.rememberDatePickerState()

                // Hero "Where / When / Who" focus routing ----------------
                // Adults / showGuestsSheet / pickerHandled are declared at
                // SearchScreen function-body level (above) so the guest
                // sheet can be hosted outside the Scaffold.  Adults is
                // stored locally until repo.search() accepts guest count.
                LaunchedEffect(initialFocus) {
                    if (pickerHandled) return@LaunchedEffect
                    isSearchBarExpanded = true
                    when (initialFocus) {
                        SearchInitialFocus.WHEN -> {
                            openDatePicker()
                            pickerHandled = true
                        }
                        SearchInitialFocus.WHO -> {
                            showGuestsSheet = true
                            pickerHandled = true
                        }
                        SearchInitialFocus.WHAT,
                        SearchInitialFocus.WHERE,
                        null -> Unit
                    }
                }

                LaunchedEffect(state.shareType) {
                    selectedTab = when (state.shareType?.uppercase()) {
                        "SHARE" -> com.pacedream.app.feature.search.SearchTab.SPACES
                        "BORROW" -> com.pacedream.app.feature.search.SearchTab.ITEMS
                        "SPLIT" -> com.pacedream.app.feature.search.SearchTab.SERVICES
                        else -> com.pacedream.app.feature.search.SearchTab.SPACES
                    }
                }

                // Collapsed search summary bar - tap to expand
                if (hasResults && !isSearchBarExpanded) {
                    Surface(
                        onClick = { isSearchBarExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        color = PaceDreamColors.Card,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                PaceDreamIcons.Search,
                                contentDescription = null,
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                text = buildString {
                                    if (whatQuery.isNotBlank()) append(whatQuery)
                                    if (whereQuery.isNotBlank()) {
                                        if (isNotBlank()) append(" · ")
                                        append(whereQuery)
                                    }
                                    // Guest count is tracked locally as a UX
                                    // hint; it is not yet a backend search
                                    // filter — see SearchUiState.adultGuests.
                                    if (adultGuests > 0) {
                                        if (isNotBlank()) append(" · ")
                                        append(
                                            if (adultGuests == 1) "1 guest"
                                            else "$adultGuests guests"
                                        )
                                    }
                                    if (isEmpty()) append("Search")
                                },
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                PaceDreamIcons.ExpandMore,
                                contentDescription = "Expand search",
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Full search bar - animated expand/collapse
                AnimatedVisibility(
                    visible = !hasResults || isSearchBarExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    com.pacedream.app.feature.search.EnhancedSearchBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            val shareType = when (tab) {
                                com.pacedream.app.feature.search.SearchTab.SPACES -> "SHARE"
                                com.pacedream.app.feature.search.SearchTab.ITEMS -> "BORROW"
                                com.pacedream.app.feature.search.SearchTab.SERVICES -> "SPLIT"
                            }
                            viewModel.updateSearchParams(shareType = shareType)
                        },
                        whatQuery = whatQuery,
                        onWhatQueryChange = {
                            whatQuery = it
                            viewModel.updateSearchParams(whatQuery = it.takeIf { it.isNotBlank() })
                        },
                        whereQuery = whereQuery,
                        onWhereQueryChange = { newQuery ->
                            whereQuery = newQuery
                            viewModel.onQueryChanged(newQuery)
                            viewModel.updateSearchParams(city = newQuery.takeIf { it.isNotBlank() })
                            // Fetch place autocomplete suggestions
                            placesJob?.cancel()
                            if (newQuery.trim().length >= 2) {
                                placesJob = scope.launch {
                                    delay(300)
                                    placeSuggestions = placesService.getAutocompletePredictions(newQuery.trim())
                                }
                            } else {
                                placeSuggestions = emptyList()
                            }
                        },
                        placeSuggestions = placeSuggestions,
                        onPlaceSuggestionClick = { prediction ->
                            whereQuery = prediction.description
                            placeSuggestions = emptyList()
                            viewModel.onQueryChanged(prediction.description)
                            viewModel.updateSearchParams(city = prediction.mainText)
                        },
                        selectedDate = selectedDateDisplay,
                        onDateClick = openDatePicker,
                        onUseMyLocation = {
                            if (!locationService.hasLocationPermission()) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                scope.launch {
                                    val location = locationService.getCurrentLocation()
                                    if (location != null) {
                                        val address = locationService.getAddressFromLocation(
                                            location.latitude,
                                            location.longitude
                                        )
                                        if (address != null) {
                                            whereQuery = address
                                            viewModel.onQueryChanged(address)
                                            viewModel.updateSearchParams(city = address)
                                            inlineBannerMessage = ("Location set: $address")
                                        } else {
                                            inlineBannerMessage = ("Could not determine address")
                                        }
                                    } else {
                                        inlineBannerMessage = ("Location unavailable")
                                    }
                                }
                            }
                        },
                        onSearchClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            isSearchBarExpanded = false
                            viewModel.updateSearchParams(
                                shareType = when (selectedTab) {
                                    com.pacedream.app.feature.search.SearchTab.SPACES -> "SHARE"
                                    com.pacedream.app.feature.search.SearchTab.ITEMS -> "BORROW"
                                    com.pacedream.app.feature.search.SearchTab.SERVICES -> "SPLIT"
                                },
                                whatQuery = whatQuery.takeIf { it.isNotBlank() },
                                city = whereQuery.takeIf { it.isNotBlank() },
                                startDate = selectedDateISO,
                                endDate = selectedDateISO
                            )
                            viewModel.submitSearch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
                    )
                }

                // Autocomplete suggestions
                if (state.suggestions.isNotEmpty() && state.query.length >= 2 && state.phase == SearchPhase.Idle) {
                    SuggestionsList(
                        suggestions = state.suggestions,
                        onClick = { suggestion ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            whereQuery = suggestion.value
                            viewModel.onQueryChanged(suggestion.value)
                            viewModel.submitSearch()
                        }
                    )
                    return@PullToRefreshBox
                }

                Column {
                            // Sort and category filters row (matching website)
                            FiltersRow(
                                selectedSort = selectedSort,
                                showSortMenu = showSortMenu,
                                onSortMenuToggle = { showSortMenu = it },
                                onSortSelected = { sort ->
                                    selectedSort = sort
                                    showSortMenu = false
                                    viewModel.updateSearchParams()
                                    if (state.phase != SearchPhase.Idle) {
                                        viewModel.submitSearch()
                                    }
                                },
                                shareType = currentShareType,
                                selectedCategories = selectedCategories,
                                onCategoryToggle = { cat ->
                                    selectedCategories = if (cat in selectedCategories) {
                                        selectedCategories - cat
                                    } else {
                                        selectedCategories + cat
                                    }
                                    // Update category in ViewModel
                                    val mappedCats = selectedCategories.map { mapCategoryToBackend(it) }
                                    viewModel.updateSearchParams(
                                        category = mappedCats.firstOrNull()
                                    )
                                    if (state.phase != SearchPhase.Idle) {
                                        viewModel.submitSearch()
                                    }
                                },
                                activeFilterCount = state.activeFilterCount,
                                onOpenFilters = onOpenFilters,
                                onClearFilters = { viewModel.clearFilters() },
                            )

                            when (state.phase) {
                                SearchPhase.Idle -> IdleState()
                                SearchPhase.Loading -> SearchSkeleton()
                                SearchPhase.Error -> ErrorState(
                                    message = state.errorMessage ?: "Search failed",
                                    onRetry = { viewModel.submitSearch() }
                                )
                                SearchPhase.Empty -> EmptyState(shareType = currentShareType)
                                SearchPhase.Success, SearchPhase.LoadingMore -> {
                                    // List / Map toggle — only rendered once
                                    // we have results so it doesn't appear on
                                    // empty or error states.
                                    SearchViewModeToggle(
                                        mode = state.viewMode,
                                        onModeChange = { viewModel.updateViewMode(it) },
                                        mappableCount = state.items.count {
                                            it.latitude != null && it.longitude != null
                                        },
                                        totalCount = state.items.size,
                                    )

                                    when (state.viewMode) {
                                        SearchViewMode.LIST -> ResultsList(
                                            items = state.items,
                                            isLoadingMore = state.phase == SearchPhase.LoadingMore,
                                            hasMore = state.hasMore,
                                            onLoadMore = { viewModel.loadMoreIfNeeded() },
                                            onItemClick = onListingClick,
                                            favoriteIds = favoriteIds,
                                            onFavoriteClick = { listingId ->
                                                if (authState == AuthState.Unauthenticated) {
                                                    onShowAuthSheet()
                                                    return@ResultsList
                                                }
                                                scope.launch {
                                                    val wasFavorited = favoriteIds.contains(listingId)
                                                    when (val res = viewModel.toggleFavorite(listingId)) {
                                                        is ApiResult.Success -> inlineBannerMessage = (if (wasFavorited) "Removed from Favorites" else "Saved to Favorites")
                                                        is ApiResult.Failure -> {
                                                            if (res.error is com.shourov.apps.pacedream.core.network.api.ApiError.Unauthorized) {
                                                                onShowAuthSheet()
                                                            } else {
                                                                inlineBannerMessage = (res.error.message ?: "Failed to save")
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            shareType = currentShareType,
                                            displayLocation = state.city,
                                            totalCount = state.items.size
                                        )
                                        SearchViewMode.MAP -> SearchMapResults(
                                            items = state.items,
                                            lastSearchedBounds = state.mapBounds,
                                            onItemClick = onListingClick,
                                            onSwitchToList = {
                                                viewModel.updateViewMode(SearchViewMode.LIST)
                                            },
                                            onSearchThisArea = { bounds ->
                                                viewModel.searchInArea(bounds)
                                            },
                                            onClearMapBounds = {
                                                viewModel.clearMapBounds()
                                            },
                                        )
                                    }
                                }
                            }
                }
            }
        }
    }

    // Hero "Who" picker sheet — adults 1..16, matches Airbnb guest stepper.
    if (showGuestsSheet) {
        GuestsPickerSheet(
            initialCount = adultGuests.coerceAtLeast(1),
            onDismiss = { showGuestsSheet = false },
            onConfirm = { count ->
                viewModel.updateAdultGuests(count)
                showGuestsSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestsPickerSheet(
    initialCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var count by remember { mutableStateOf(initialCount.coerceIn(1, 16)) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaceDreamColors.Background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.MD)
        ) {
            Text(
                text = "Who's coming?",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "Choose how many guests.",
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Adults",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary
                    )
                    Text(
                        text = "Ages 13 or above",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (count > 1) count-- },
                        enabled = count > 1
                    ) {
                        Icon(
                            PaceDreamIcons.Remove,
                            contentDescription = "Decrease",
                            tint = if (count > 1) PaceDreamColors.TextPrimary else PaceDreamColors.Gray400
                        )
                    }
                    Text(
                        text = count.toString(),
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    )
                    IconButton(
                        onClick = { if (count < 16) count++ },
                        enabled = count < 16
                    ) {
                        Icon(
                            PaceDreamIcons.Add,
                            contentDescription = "Increase",
                            tint = if (count < 16) PaceDreamColors.TextPrimary else PaceDreamColors.Gray400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Button(
                onClick = { onConfirm(count) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
            ) {
                Text(
                    text = "Apply",
                    style = PaceDreamTypography.Headline,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        }
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<AutocompleteSuggestion>,
    onClick: (AutocompleteSuggestion) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
    ) {
        items(suggestions, key = { it.value }) { s ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(s) },
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                color = PaceDreamColors.Card,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PaceDreamColors.Surface,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                PaceDreamIcons.Search,
                                contentDescription = null,
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                    Text(
                        s.value,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Sort dropdown and category filter chips (website parity).
 */
@Composable
private fun FiltersRow(
    selectedSort: String,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    onSortSelected: (String) -> Unit,
    shareType: String,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    activeFilterCount: Int = 0,
    onOpenFilters: () -> Unit = {},
    onClearFilters: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Sort row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filters entry-point chip — opens the FilterScreen.  Badged
            // with the active filter count so users can tell at a glance
            // how many constraints are currently applied.
            FilterChip(
                selected = activeFilterCount > 0,
                onClick = onOpenFilters,
                label = {
                    Text(
                        text = if (activeFilterCount > 0) "Filters · $activeFilterCount" else "Filters",
                        style = PaceDreamTypography.Subheadline
                    )
                },
                leadingIcon = {
                    Icon(
                        PaceDreamIcons.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = if (activeFilterCount > 0) {
                    {
                        IconButton(
                            onClick = onClearFilters,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                PaceDreamIcons.Close,
                                contentDescription = "Clear filters",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = PaceDreamColors.Surface,
                    selectedContainerColor = PaceDreamColors.Primary.copy(alpha = 0.12f),
                    selectedLabelColor = PaceDreamColors.Primary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                border = null,
            )
            // Sort dropdown
            Box {
                val sortLabel = SORT_OPTIONS.find { it.value == selectedSort }?.label ?: "Relevance"
                FilterChip(
                    selected = selectedSort != "relevance",
                    onClick = { onSortMenuToggle(!showSortMenu) },
                    label = { Text(sortLabel, style = PaceDreamTypography.Subheadline) },
                    leadingIcon = {
                        Icon(
                            PaceDreamIcons.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PaceDreamColors.Surface,
                        selectedContainerColor = PaceDreamColors.Primary.copy(alpha = 0.12f),
                        selectedLabelColor = PaceDreamColors.Primary
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    border = null
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onSortMenuToggle(false) }
                ) {
                    SORT_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    style = PaceDreamTypography.Callout,
                                    fontWeight = if (option.value == selectedSort) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (option.value == selectedSort) PaceDreamColors.Primary else PaceDreamColors.TextPrimary
                                )
                            },
                            onClick = { onSortSelected(option.value) },
                            trailingIcon = if (option.value == selectedSort) {
                                {
                                    Icon(
                                        PaceDreamIcons.Check,
                                        contentDescription = null,
                                        tint = PaceDreamColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Category filter chips - dynamic per mode (matching website)
        val categories = CATEGORIES_BY_MODE[shareType] ?: emptyList()
        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat in selectedCategories
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryToggle(cat) },
                        label = {
                            Text(
                                cat,
                                style = PaceDreamTypography.Caption,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PaceDreamColors.Surface,
                            selectedContainerColor = PaceDreamColors.Primary,
                            selectedLabelColor = Color.White,
                            labelColor = PaceDreamColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        PaceDreamEmptyState(
            title = "Start exploring",
            description = "Search for a city, neighborhood, or listing.",
            icon = PaceDreamIcons.Search,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyState(shareType: String = "") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        val (title, description) = when (shareType) {
            "SHARE" -> "No space listings available yet" to "Be the first to create a space listing!"
            "BORROW" -> "No items available yet" to "Be the first to list an item!"
            "SPLIT" -> "No split listings available yet" to "Be the first to create a split listing!"
            else -> "No results" to "Try a different search or pull to refresh."
        }
        PaceDreamEmptyState(
            title = title,
            description = description,
            icon = PaceDreamIcons.Search,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        PaceDreamErrorState(
            title = "Search failed",
            description = message,
            onRetryClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SearchSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        items(6) { _ ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                color = PaceDreamColors.Card
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(PaceDreamColors.Surface)
                            .shimmerEffect()
                    )
                    Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(16.dp)
                                .background(
                                    PaceDreamColors.Surface,
                                    RoundedCornerShape(PaceDreamRadius.XS)
                                )
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(12.dp)
                                .background(
                                    PaceDreamColors.Surface,
                                    RoundedCornerShape(PaceDreamRadius.XS)
                                )
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(14.dp)
                                .background(
                                    PaceDreamColors.Surface,
                                    RoundedCornerShape(PaceDreamRadius.XS)
                                )
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultsList(
    items: List<SearchResultItem>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (String) -> Unit,
    favoriteIds: Set<String>,
    onFavoriteClick: (String) -> Unit,
    shareType: String = "",
    displayLocation: String? = null,
    totalCount: Int = 0
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        // Mode-specific banner (matching website gradient banners)
        if (shareType.isNotBlank()) {
            item(key = "mode_banner") {
                ModeBanner(shareType = shareType)
            }
        }

        // Results count header (matching website)
        item(key = "results_header") {
            ResultsHeader(
                totalCount = totalCount,
                shareType = shareType,
                displayLocation = displayLocation
            )
        }

        items(items, key = { it.id }) { item ->
            ModernSearchResultCard(
                item = item,
                onClick = { onItemClick(item.id) },
                isFavorited = favoriteIds.contains(item.id),
                onFavorite = { onFavoriteClick(item.id) },
                modifier = Modifier.animateItemPlacement()
            )
        }

        if (hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.MD),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                LaunchedEffect(Unit) { onLoadMore() }
            }
        } else if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.MD),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Mode-specific gradient banner (matching website's mode banners)
 */
@Composable
private fun ModeBanner(shareType: String) {
    val (title, description) = when (shareType) {
        "SHARE" -> "Share - Space Rentals" to "Discover restrooms, nap pods, meeting rooms, study spaces, parking, and more available by the hour"
        "BORROW" -> "Book - Items & Equipment" to "Book sports gear, cameras, e-bikes, scooters, instruments, and more"
        "SPLIT" -> "Split - Share the Cost" to "Join others and split the cost of stays, rides, memberships, and more"
        else -> return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF3EFFF),
                            Color(0xFFEDE5FF),
                            Color(0xFFE2D8FF)
                        )
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFD4C4FF),
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
                .padding(PaceDreamSpacing.MD)
        ) {
            Column {
                Text(
                    text = title,
                    style = PaceDreamTypography.Title2,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = description,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Results count header (matching website's results count display)
 */
@Composable
private fun ResultsHeader(
    totalCount: Int,
    shareType: String,
    displayLocation: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Location chip
        if (!displayLocation.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(bottom = PaceDreamSpacing.SM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Showing results in: ",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    color = PaceDreamColors.Primary.copy(alpha = 0.08f),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = PaceDreamColors.Primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(PaceDreamRadius.Round)
                    )
                ) {
                    Text(
                        text = displayLocation,
                        style = PaceDreamTypography.Caption,
                        fontWeight = FontWeight.Medium,
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.SM,
                            vertical = PaceDreamSpacing.XS
                        )
                    )
                }
            }
        }

        // Results count
        val listingNoun = if (totalCount == 1) "listing" else "listings"
        val typeLabel = when (shareType) {
            "SHARE" -> "space"
            "BORROW" -> "borrowable"
            "SPLIT" -> "split"
            else -> ""
        }

        Text(
            text = buildString {
                append("$totalCount $typeLabel $listingNoun".trim())
                if (!displayLocation.isNullOrBlank()) {
                    append(" in ${displayLocation.replaceFirstChar { it.uppercase() }}")
                }
            },
            style = PaceDreamTypography.Headline,
            fontWeight = FontWeight.Bold,
            color = PaceDreamColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
    }
}

/**
 * Modernized result card with full-width image, overlaid info, and glass-style badges.
 */
@Composable
private fun ModernSearchResultCard(
    item: SearchResultItem,
    isFavorited: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = {
            ListingPreviewStore.put(
                ListingPreview(
                    id = item.id,
                    title = item.title.ifBlank { "Listing" },
                    location = item.location?.takeIf { it.isNotBlank() },
                    imageUrl = item.imageUrl?.takeIf { it.isNotBlank() },
                    priceText = item.priceText?.takeIf { it.isNotBlank() },
                    rating = item.rating
                )
            )
            onClick()
        },
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            // Full-width image with overlay elements
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(PaceDreamRadius.LG))
            ) {
                if (item.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaceDreamColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Search,
                            contentDescription = null,
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .crossfade(200)
                            .size(coil.size.Size(800, 600))
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )

                // Favorite button
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .size(36.dp)
                ) {
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AnimatedContent(
                            targetState = isFavorited,
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(
                                    initialScale = 0.85f,
                                    animationSpec = tween(200)
                                )) togetherWith
                                    (fadeOut(tween(200)) + scaleOut(
                                        targetScale = 0.9f,
                                        animationSpec = tween(200)
                                    ))
                            },
                            label = "favorite_toggle"
                        ) { favored ->
                            Icon(
                                imageVector = if (favored) PaceDreamIcons.Favorite else PaceDreamIcons.FavoriteBorder,
                                contentDescription = if (favored) "Remove from favorites" else "Save to favorites",
                                tint = if (favored) PaceDreamColors.Error else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Instant Book badge - top left.  Rendered only when the
                // backend confirms the flag.  Unknown (null) stays hidden so
                // a missing field is never interpreted as "not instant".
                if (item.instantBook == true) {
                    Surface(
                        shape = RoundedCornerShape(PaceDreamRadius.SM),
                        color = PaceDreamColors.Primary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(PaceDreamSpacing.SM),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = PaceDreamSpacing.SM,
                                vertical = PaceDreamSpacing.XS,
                            ),
                        ) {
                            Icon(
                                PaceDreamIcons.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                            Text(
                                text = "Instant Book",
                                style = PaceDreamTypography.Caption2,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Rating badge - bottom left
                item.rating?.let { r ->
                    Surface(
                        shape = RoundedCornerShape(PaceDreamRadius.SM),
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(PaceDreamSpacing.SM)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = PaceDreamSpacing.SM,
                                vertical = PaceDreamSpacing.XS
                            )
                        ) {
                            Icon(
                                PaceDreamIcons.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFCC00),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                            Text(
                                text = String.format("%.1f", r),
                                style = PaceDreamTypography.Caption,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Content section below image
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                Text(
                    text = item.title.ifBlank { "Listing" },
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                item.location?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            it,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                val price = item.priceText?.takeIf { it.isNotBlank() }
                Text(
                    text = price ?: "Price unavailable",
                    style = PaceDreamTypography.Callout,
                    color = if (price != null) PaceDreamColors.Primary else PaceDreamColors.TextTertiary,
                    fontWeight = if (price != null) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// List / Map toggle + map renderer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Two-segment control for switching between List and Map presentations.
 * Shown above the current results, only when results exist.  The
 * helper caption ("N of M on map") tells the user up-front how many
 * listings have coordinates, so the map toggle is honest even when
 * only a subset of the page is mappable.
 */
@Composable
private fun SearchViewModeToggle(
    mode: SearchViewMode,
    onModeChange: (SearchViewMode) -> Unit,
    mappableCount: Int,
    totalCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.LG))
                .background(PaceDreamColors.Gray100)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SearchModeSegment(
                label = "List",
                icon = PaceDreamIcons.AppsOutlined,
                selected = mode == SearchViewMode.LIST,
                onClick = { onModeChange(SearchViewMode.LIST) },
                modifier = Modifier.weight(1f),
            )
            SearchModeSegment(
                label = "Map",
                icon = PaceDreamIcons.LocationOn,
                selected = mode == SearchViewMode.MAP,
                onClick = { onModeChange(SearchViewMode.MAP) },
                modifier = Modifier.weight(1f),
            )
        }
        if (mode == SearchViewMode.MAP && totalCount > 0 && mappableCount < totalCount) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Showing $mappableCount of $totalCount listings with coordinates.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SearchModeSegment(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        color = if (selected) PaceDreamColors.Card else Color.Transparent,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
            Text(
                text = label,
                style = PaceDreamTypography.Callout,
                color = if (selected) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/**
 * Map renderer for search results.  Uses the `maps-compose` composable
 * (already a declared dependency in app/build.gradle.kts).  When the
 * Google Maps API key is not configured, we fall back to an honest
 * empty state and a "Use list instead" action — we do not attempt to
 * fake an interactive map with a static-image approximation.
 *
 * Markers are drawn only for items carrying valid coordinates.  Tapping
 * a marker shows a small bottom preview card; tapping the card opens
 * the listing detail via the same [onItemClick] the list mode uses, so
 * there is no parallel data path.
 */
@Composable
private fun SearchMapResults(
    items: List<SearchResultItem>,
    lastSearchedBounds: MapBounds?,
    onItemClick: (String) -> Unit,
    onSwitchToList: () -> Unit,
    onSearchThisArea: (MapBounds) -> Unit,
    onClearMapBounds: () -> Unit,
) {
    val mapsKey = stringResource(com.shourov.apps.pacedream.R.string.google_maps_key)
    val mapsEnabled = mapsKey.isNotBlank()

    val mappable = remember(items) {
        items.filter { it.latitude != null && it.longitude != null }
    }

    if (!mapsEnabled || mappable.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.LG),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = PaceDreamIcons.LocationOn,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Text(
                text = if (!mapsEnabled) "Map view isn\u2019t available on this device."
                       else "None of these listings have coordinates to place on a map yet.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            OutlinedButton(
                onClick = onSwitchToList,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            ) {
                Text("View as list", style = PaceDreamTypography.Button)
            }
        }
        return
    }

    // Interactive map
    val latLngs = remember(mappable) {
        mappable.mapNotNull { item ->
            val lat = item.latitude ?: return@mapNotNull null
            val lng = item.longitude ?: return@mapNotNull null
            item.id to com.google.android.gms.maps.model.LatLng(lat, lng)
        }
    }

    val cameraPositionState = com.google.maps.android.compose.rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
            latLngs.first().second, 11f
        )
    }
    // Re-center whenever the set of mappable results changes so the map
    // always focuses on the current result set rather than stale pins.
    LaunchedEffect(latLngs) {
        if (latLngs.size == 1) {
            cameraPositionState.position =
                com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                    latLngs.first().second, 13f
                )
        } else if (latLngs.size > 1) {
            val builder = com.google.android.gms.maps.model.LatLngBounds.builder()
            latLngs.forEach { (_, ll) -> builder.include(ll) }
            runCatching {
                cameraPositionState.move(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(builder.build(), 80)
                )
            }
        }
    }

    var selectedId by remember { mutableStateOf<String?>(null) }
    val selectedItem = remember(selectedId, mappable) {
        selectedId?.let { id -> mappable.firstOrNull { it.id == id } }
    }

    // "Search this area" gating — only surfaces after the user has
    // gestured the camera AND the visible bounds meaningfully differ
    // from what was last searched.  Dismissed as soon as the ViewModel
    // accepts a new bounded search (lastSearchedBounds changes).
    var showSearchThisAreaPill by remember { mutableStateOf(false) }
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving &&
            cameraPositionState.cameraMoveStartedReason ==
                com.google.maps.android.compose.CameraMoveStartedReason.GESTURE
        ) {
            val visible = cameraPositionState.projection
                ?.visibleRegion
                ?.latLngBounds
                ?: return@LaunchedEffect
            val next = MapBounds(
                swLat = visible.southwest.latitude,
                swLng = visible.southwest.longitude,
                neLat = visible.northeast.latitude,
                neLng = visible.northeast.longitude,
            )
            showSearchThisAreaPill = shouldOfferSearchThisArea(next, lastSearchedBounds)
        }
    }
    LaunchedEffect(lastSearchedBounds) {
        // A successful bounded search just landed; camera will be
        // auto-fitted by the latLngs effect above.  Hide the pill until
        // the user gestures again.
        showSearchThisAreaPill = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        com.google.maps.android.compose.GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = com.google.maps.android.compose.MapProperties(
                mapType = com.google.maps.android.compose.MapType.NORMAL,
            ),
            uiSettings = com.google.maps.android.compose.MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false,
            ),
            onMapClick = { selectedId = null },
        ) {
            latLngs.forEach { (id, ll) ->
                val markerItem = mappable.firstOrNull { it.id == id }
                com.google.maps.android.compose.Marker(
                    state = com.google.maps.android.compose.rememberMarkerState(
                        key = id,
                        position = ll,
                    ),
                    title = markerItem?.title,
                    snippet = markerItem?.priceText,
                    onClick = {
                        selectedId = id
                        false  // allow default info-window
                    },
                )
            }
        }

        // Bounded-results chip — small, muted indicator at the top-start
        // so the user always knows when the visible list is scoped to a
        // searched area.  Tapping the trailing X clears the bbox via
        // clearMapBounds() and re-runs search unbounded.  Hidden when
        // the current search is global (lastSearchedBounds == null).
        if (lastSearchedBounds != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(PaceDreamSpacing.MD),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                color = Color.White,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = PaceDreamSpacing.MD,
                        end = 4.dp,
                        top = 4.dp,
                        bottom = 4.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Results in this area",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onClearMapBounds,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Close,
                            contentDescription = "Clear area filter",
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        // "Search this area" pill — centered at the top of the map.
        // Only visible after user gesture + meaningful delta from the
        // last searched bounds.  Tapping re-runs search via the same
        // pipeline; other filters (q / city / category / shareType /
        // dates / sort) compose with bbox server-side.
        if (showSearchThisAreaPill) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = PaceDreamSpacing.MD),
                onClick = {
                    val visible = cameraPositionState.projection
                        ?.visibleRegion
                        ?.latLngBounds
                    if (visible != null) {
                        onSearchThisArea(
                            MapBounds(
                                swLat = visible.southwest.latitude,
                                swLng = visible.southwest.longitude,
                                neLat = visible.northeast.latitude,
                                neLng = visible.northeast.longitude,
                            )
                        )
                        showSearchThisAreaPill = false
                    }
                },
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                color = PaceDreamColors.Primary,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.MD,
                        vertical = PaceDreamSpacing.SM,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Search this area",
                        style = PaceDreamTypography.Callout,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Bottom preview card — appears when the user taps a marker and
        // routes to listing detail via the same path list mode uses.
        selectedItem?.let { item ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                onClick = { onItemClick(item.id) },
            ) {
                Row(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
                ) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        item.location?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        item.priceText?.takeIf { it.isNotBlank() }?.let {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = it,
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.Primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (item.instantBook == true) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    PaceDreamIcons.Bolt,
                                    contentDescription = null,
                                    tint = PaceDreamColors.Primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Instant Book",
                                    style = PaceDreamTypography.Caption2,
                                    color = PaceDreamColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns true when the currently visible bounds differ enough from the
 * last searched bounds that rerunning the search would meaningfully
 * change the result set.  First-time use (lastSearched == null) always
 * qualifies — the user has panned inside map mode and has not yet
 * committed an area-bounded search.  Otherwise, the center must have
 * shifted by more than a quarter of the averaged bounds dimension in
 * either axis.  Avoids noisy pill toggling on small gestures.
 */
private fun shouldOfferSearchThisArea(
    next: MapBounds,
    lastSearched: MapBounds?,
): Boolean {
    if (lastSearched == null) return true
    val dLat = kotlin.math.abs(next.centerLat - lastSearched.centerLat)
    val dLng = kotlin.math.abs(next.centerLng - lastSearched.centerLng)
    val avgLatSpan = (next.latSpan + lastSearched.latSpan) / 2.0
    val avgLngSpan = (next.lngSpan + lastSearched.lngSpan) / 2.0
    val latThreshold = (avgLatSpan * 0.25).coerceAtLeast(0.0001)
    val lngThreshold = (avgLngSpan * 0.25).coerceAtLeast(0.0001)
    return dLat > latThreshold || dLng > lngThreshold
}

