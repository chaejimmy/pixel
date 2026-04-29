package com.pacedream.app.feature.destination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.pacedream.common.composables.components.PaceDreamEmptyState
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

import com.pacedream.common.composables.theme.PaceDreamSpacing
// ── Data Models (iOS DestinationLandingView parity) ──────────────

@Serializable
data class DestinationListing(
    val id: String = "", val title: String = "", val location: String = "",
    val price: Double = 0.0, val rating: Double = 0.0,
    @SerialName("reviewCount") val reviewCount: Int = 0,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("hostName") val hostName: String? = null
)

@Serializable
data class LocalCategory(val id: String = "", val name: String = "", val icon: String? = null)

@Serializable
data class FeaturedHost(
    val id: String = "", val name: String = "", val avatar: String? = null,
    @SerialName("listingCount") val listingCount: Int = 0
)

@Serializable
data class TravelDestination(
    val id: String = "", val name: String = "", val subtitle: String = "",
    val image: String? = null,
    @SerialName("popularListings") val popularListings: List<DestinationListing> = emptyList(),
    @SerialName("localCategories") val localCategories: List<LocalCategory> = emptyList(),
    @SerialName("featuredHosts") val featuredHosts: List<FeaturedHost> = emptyList()
)

@Serializable
data class DestinationsEnvelope(
    val status: Boolean? = null, val data: List<TravelDestination>? = null,
    val destinations: List<TravelDestination>? = null
) { val resolved get() = data ?: destinations ?: emptyList() }

@Serializable
data class DestinationEnvelope(val status: Boolean? = null, val data: TravelDestination? = null)

@Serializable
data class ListingsEnvelope(
    val status: Boolean? = null, val data: List<DestinationListing>? = null,
    val listings: List<DestinationListing>? = null, val total: Int? = null
) { val resolved get() = data ?: listings ?: emptyList() }

enum class SortOption(val label: String, val param: String) {
    RECOMMENDED("Recommended", "recommended"), PRICE_LOW("Price: Low to High", "price_asc"),
    PRICE_HIGH("Price: High to Low", "price_desc"), RATING("Highest Rating", "rating"),
    NEWEST("Newest", "newest")
}

// ── Repository ───────────────────────────────────────────────────

@Singleton
class DestinationRepository @Inject constructor(
    private val apiClient: ApiClient, private val appConfig: AppConfig, private val json: Json
) {
    suspend fun getDestinations(): ApiResult<DestinationsEnvelope> =
        fetch(appConfig.buildApiUrl("destinations"), DestinationsEnvelope.serializer())

    suspend fun getDestination(id: String): ApiResult<TravelDestination> {
        return when (val r = fetch(appConfig.buildApiUrl("destinations", id), DestinationEnvelope.serializer())) {
            is ApiResult.Success -> r.data.data?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ApiError.DecodingError())
            is ApiResult.Failure -> r
        }
    }

    suspend fun getDestinationListings(
        id: String, sort: SortOption = SortOption.RECOMMENDED, query: String = "", page: Int = 1
    ): ApiResult<ListingsEnvelope> {
        val url = appConfig.buildApiUrl("destinations", id, "listings", queryParams = mapOf("sort" to sort.param, "q" to query, "page" to page.toString(), "limit" to "20"))
        return fetch(url, ListingsEnvelope.serializer())
    }

    private suspend fun <T> fetch(url: okhttp3.HttpUrl, serializer: kotlinx.serialization.KSerializer<T>): ApiResult<T> =
        when (val r = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> try { ApiResult.Success(json.decodeFromString(serializer, r.data)) }
            catch (e: Exception) { Timber.e(e, "Parse error"); ApiResult.Failure(ApiError.DecodingError()) }
            is ApiResult.Failure -> r
        }
}

// ── ViewModel ────────────────────────────────────────────────────

data class DestinationUiState(
    val destinations: List<TravelDestination> = emptyList(),
    val selectedDestination: TravelDestination? = null,
    val listings: List<DestinationListing> = emptyList(),
    val searchQuery: String = "", val sortOption: SortOption = SortOption.RECOMMENDED,
    val activeFilters: List<String> = emptyList(),
    val isLoading: Boolean = false, val isRefreshing: Boolean = false, val error: String? = null
)

@HiltViewModel
class DestinationViewModel @Inject constructor(
    private val repository: DestinationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DestinationUiState(isLoading = true))
    val uiState: StateFlow<DestinationUiState> = _uiState.asStateFlow()

    init { loadDestinations() }

    fun loadDestinations() { viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        when (val r = repository.getDestinations()) {
            is ApiResult.Success -> _uiState.update { it.copy(destinations = r.data.resolved, isLoading = false, isRefreshing = false) }
            is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "Failed to load destinations") }
        }
    }}

    fun selectDestination(id: String) { viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        when (val r = repository.getDestination(id)) {
            is ApiResult.Success -> { _uiState.update { it.copy(selectedDestination = r.data, isLoading = false) }; loadListings(id) }
            is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, error = "Failed to load destination") }
        }
    }}

    fun loadListings(destinationId: String? = null) {
        val id = destinationId ?: _uiState.value.selectedDestination?.id ?: return
        viewModelScope.launch {
            try {
                val s = _uiState.value
                when (val r = repository.getDestinationListings(id, s.sortOption, s.searchQuery)) {
                    is ApiResult.Success -> _uiState.update { it.copy(listings = r.data.resolved) }
                    is ApiResult.Failure -> Timber.w("Failed to load listings")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load listings")
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        val id = _uiState.value.selectedDestination?.id
        if (id != null) selectDestination(id) else loadDestinations()
    }

    fun updateSearch(query: String) { _uiState.update { it.copy(searchQuery = query) }; loadListings() }
    fun updateSort(option: SortOption) { _uiState.update { it.copy(sortOption = option) }; loadListings() }
    fun toggleFilter(filter: String) {
        _uiState.update { it.copy(activeFilters = if (filter in it.activeFilters) it.activeFilters - filter else it.activeFilters + filter) }
        loadListings()
    }
}

// ── City Fallback Images ─────────────────────────────────────────

/** City-specific fallback image URLs so destination cards never show blank placeholders. */
private fun fallbackCityImage(name: String): String {
    return when (name.lowercase().trim()) {
        "new york", "manhattan", "brooklyn" -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=400&q=80"
        "los angeles" -> "https://images.unsplash.com/photo-1534190760961-74e8c1c5c3da?w=400&q=80"
        "san francisco" -> "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=400&q=80"
        "chicago" -> "https://images.unsplash.com/photo-1494522855154-9297ac14b55f?w=400&q=80"
        "miami" -> "https://images.unsplash.com/photo-1533106497176-45ae19e68ba2?w=400&q=80"
        "seattle" -> "https://images.unsplash.com/photo-1502175353174-a7a70e73b4c3?w=400&q=80"
        "austin" -> "https://images.unsplash.com/photo-1531218150217-54595bc2b934?w=400&q=80"
        "denver" -> "https://images.unsplash.com/photo-1619856699906-09e1f4ef478b?w=400&q=80"
        "boston" -> "https://images.unsplash.com/photo-1501979376754-1d3b25f22a4e?w=400&q=80"
        "honolulu" -> "https://images.unsplash.com/photo-1507876466758-bc54f384809c?w=400&q=80"
        "maui" -> "https://images.unsplash.com/photo-1542259009477-d625272157b7?w=400&q=80"
        "grand canyon" -> "https://images.unsplash.com/photo-1474044159687-1ee9f3a51722?w=400&q=80"
        "utah", "moab" -> "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=400&q=80"
        "glacier" -> "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?w=400&q=80"
        "nashville" -> "https://images.unsplash.com/photo-1545419913-775e2e168cd0?w=400&q=80"
        "portland" -> "https://images.unsplash.com/photo-1507245338956-79a3a4b41583?w=400&q=80"
        "san diego" -> "https://images.unsplash.com/photo-1538097304804-2a1b932466a9?w=400&q=80"
        "atlanta" -> "https://images.unsplash.com/photo-1575917649705-5b59aaa12e6b?w=400&q=80"
        "washington", "washington dc" -> "https://images.unsplash.com/photo-1501466044931-62695aada8e9?w=400&q=80"
        else -> "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=400&q=80"
    }
}

/** Resolve destination image URL with city-based fallback. */
private fun TravelDestination.resolvedImage(): String =
    image?.takeIf { it.isNotBlank() } ?: fallbackCityImage(name)

// ── Destination Landing Screen ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationLandingScreen(
    destinationId: String, onBackClick: () -> Unit = {},
    onListingClick: (String) -> Unit = {}, onViewAllListings: () -> Unit = {},
    viewModel: DestinationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(destinationId) { viewModel.selectDestination(destinationId) }

    Scaffold(containerColor = PaceDreamColors.Background) { padding ->
        PullToRefreshBox(uiState.isRefreshing, { viewModel.refresh() }, Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> DestinationLandingSkeleton()
                uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error", color = PaceDreamColors.TextSecondary)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Button(onClick = { viewModel.selectDestination(destinationId) }) { Text("Retry") }
                    }
                }
                else -> {
                    val dest = uiState.selectedDestination ?: return@PullToRefreshBox
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        // Hero Section
                        Box(Modifier.fillMaxWidth().height(280.dp)) {
                            AsyncImage(dest.resolvedImage(), dest.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                            IconButton(onClick = onBackClick, Modifier.align(Alignment.TopStart).padding(PaceDreamSpacing.SM)) {
                                Icon(PaceDreamIcons.ArrowBack, "Back", tint = Color.White)
                            }
                            Column(Modifier.align(Alignment.BottomStart).padding(PaceDreamSpacing.MD)) {
                                Text(dest.name, style = PaceDreamTypography.LargeTitle, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(dest.subtitle, style = PaceDreamTypography.Body, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                        // Search Bar
                        OutlinedTextField(uiState.searchQuery, { viewModel.updateSearch(it) },
                            Modifier.fillMaxWidth().padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                            placeholder = { Text("Search in ${dest.name}...") },
                            leadingIcon = { Icon(PaceDreamIcons.Search, null) },
                            shape = RoundedCornerShape(PaceDreamRadius.Round), singleLine = true)
                        // Popular Listings
                        if (dest.popularListings.isNotEmpty()) {
                            SectionHeader("Popular Listings", onAction = onViewAllListings)
                            LazyRow(contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                                items(dest.popularListings, key = { it.id }) { listing ->
                                    ListingCardCompact(listing) { onListingClick(listing.id) }
                                }
                            }
                            Spacer(Modifier.height(PaceDreamSpacing.LG))
                        }
                        // Local Categories Grid
                        if (dest.localCategories.isNotEmpty()) {
                            SectionHeader("Local Categories")
                            Column(Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                                dest.localCategories.chunked(2).forEach { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                                        row.forEach { cat ->
                                            Card(Modifier.weight(1f).padding(vertical = PaceDreamSpacing.XS),
                                                shape = RoundedCornerShape(PaceDreamRadius.MD),
                                                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
                                                Text(cat.name, Modifier.padding(PaceDreamSpacing.MD), style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(PaceDreamSpacing.LG))
                        }
                        // Featured Hosts
                        if (dest.featuredHosts.isNotEmpty()) {
                            SectionHeader("Featured Hosts")
                            LazyRow(contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                                items(dest.featuredHosts, key = { it.id }) { host ->
                                    Card(shape = RoundedCornerShape(PaceDreamRadius.MD),
                                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
                                        Column(Modifier.padding(PaceDreamSpacing.MD).width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(host.avatar, host.name,
                                                Modifier.size(56.dp).clip(CircleShape).background(PaceDreamColors.Divider))
                                            Spacer(Modifier.height(PaceDreamSpacing.XS))
                                            Text(host.name, style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${host.listingCount} listings", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(PaceDreamSpacing.XL))
                    }
                }
            }
        }
    }
}

// ── Destination Listings Screen ──────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationListingsScreen(
    destinationId: String, onBackClick: () -> Unit = {}, onListingClick: (String) -> Unit = {},
    viewModel: DestinationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(destinationId) { viewModel.selectDestination(destinationId) }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text(uiState.selectedDestination?.name ?: "Listings", style = PaceDreamTypography.Headline) },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(PaceDreamIcons.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
        ) },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            OutlinedTextField(uiState.searchQuery, { viewModel.updateSearch(it) },
                Modifier.fillMaxWidth().padding(horizontal = PaceDreamSpacing.MD),
                placeholder = { Text("Search listings...") }, leadingIcon = { Icon(PaceDreamIcons.Search, null) },
                shape = RoundedCornerShape(PaceDreamRadius.Round), singleLine = true)
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            // Sort + Filter Chips
            Row(Modifier.fillMaxWidth().padding(horizontal = PaceDreamSpacing.MD), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    FilterChip(true, { sortExpanded = true },
                        label = { Text(uiState.sortOption.label, style = PaceDreamTypography.Caption) },
                        trailingIcon = { Icon(PaceDreamIcons.ArrowDropDown, null, Modifier.size(18.dp)) })
                    DropdownMenu(sortExpanded, { sortExpanded = false }) {
                        SortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label, fontWeight = if (opt == uiState.sortOption) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { viewModel.updateSort(opt); sortExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.width(PaceDreamSpacing.XS))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
                    (uiState.selectedDestination?.localCategories?.map { it.name } ?: emptyList()).forEach { filter ->
                        FilterChip(filter in uiState.activeFilters, { viewModel.toggleFilter(filter) },
                            label = { Text(filter, style = PaceDreamTypography.Caption) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaceDreamColors.Primary, selectedLabelColor = Color.White))
                    }
                }
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            // Listings Grid
            when {
                uiState.isLoading -> DestinationListingsSkeleton()
                uiState.listings.isEmpty() -> PaceDreamEmptyState(
                    title = "No listings found",
                    description = "Try a different destination or check back later.",
                    icon = PaceDreamIcons.Search,
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyVerticalGrid(GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    items(uiState.listings, key = { it.id }) { listing ->
                        ListingCardGrid(listing) { onListingClick(listing.id) }
                    }
                }
            }
        }
    }
}

// ── Shared Components ────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold)
        if (onAction != null) TextButton(onClick = onAction) {
            Text("View All", color = PaceDreamColors.Primary, fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Caption)
        }
    }
}

@Composable
private fun ListingCardCompact(listing: DestinationListing, onClick: () -> Unit) {
    Card(Modifier.width(200.dp).clickable(onClick = onClick), shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column {
            AsyncImage(listing.imageUrl, listing.title,
                Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = PaceDreamRadius.MD, topEnd = PaceDreamRadius.MD)),
                contentScale = ContentScale.Crop)
            Column(Modifier.padding(PaceDreamSpacing.SM)) {
                Text(listing.title, style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listing.location, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, maxLines = 1)
                Spacer(Modifier.height(PaceDreamSpacing.XS))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PaceDreamIcons.Star, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(14.dp))
                    Text(String.format("%.1f", listing.rating), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                    Text(" (${listing.reviewCount})", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                    Spacer(Modifier.weight(1f))
                    Text("$${listing.price.toInt()}", style = PaceDreamTypography.Body, fontWeight = FontWeight.Bold, color = PaceDreamColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun ListingCardGrid(listing: DestinationListing, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column {
            AsyncImage(listing.imageUrl, listing.title,
                Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(topStart = PaceDreamRadius.MD, topEnd = PaceDreamRadius.MD)),
                contentScale = ContentScale.Crop)
            Column(Modifier.padding(PaceDreamSpacing.SM)) {
                Text(listing.title, style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listing.location, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, maxLines = 1)
                Spacer(Modifier.height(PaceDreamSpacing.XS))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PaceDreamIcons.Star, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(PaceDreamSpacing.XXS))
                    Text(String.format("%.1f", listing.rating), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                    Text(" (${listing.reviewCount})", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                }
                listing.hostName?.let { Text("by $it", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary, maxLines = 1) }
                Spacer(Modifier.height(PaceDreamSpacing.XXS))
                Text("$${listing.price.toInt()}/night", style = PaceDreamTypography.Body, fontWeight = FontWeight.Bold, color = PaceDreamColors.Primary)
            }
        }
    }
}

// ── Destinations Index / Picker ──────────────────────────────────
//
// Top-level destinations screen. Replaces the previous broken path where
// tapping "Destinations" in Profile tried to render DestinationListingsScreen
// with an empty destinationId and always landed on the error state.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationsIndexScreen(
    onBackClick: () -> Unit = {},
    onDestinationClick: (String) -> Unit = {},
    viewModel: DestinationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Destinations", style = PaceDreamTypography.Headline) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(PaceDreamIcons.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        when {
            uiState.isLoading && uiState.destinations.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            }
            uiState.error != null && uiState.destinations.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Couldn't load destinations", color = PaceDreamColors.TextSecondary)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Button(onClick = { viewModel.loadDestinations() }) { Text("Retry") }
                    }
                }
            }
            uiState.destinations.isEmpty() -> {
                PaceDreamEmptyState(
                    title = "No destinations yet",
                    description = "Check back soon for curated places to visit.",
                    icon = PaceDreamIcons.Search,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(PaceDreamSpacing.MD),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(uiState.destinations, key = { it.id }) { dest ->
                        DestinationIndexCard(dest = dest, onClick = { onDestinationClick(dest.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIndexCard(dest: TravelDestination, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)
    ) {
        Column {
            AsyncImage(
                model = dest.resolvedImage(),
                contentDescription = dest.name,
                modifier = Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(topStart = PaceDreamRadius.MD, topEnd = PaceDreamRadius.MD)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(PaceDreamSpacing.SM)) {
                Text(
                    dest.name,
                    style = PaceDreamTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dest.subtitle.isNotBlank()) {
                    Text(
                        dest.subtitle,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Skeleton Loading States ──────────────────────────────────────

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = PaceDreamRadius.SM,
    alpha: Float = 0.15f
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Gray.copy(alpha = alpha))
    )
}

@Composable
private fun DestinationLandingSkeleton() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Hero placeholder
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            cornerRadius = 0.dp
        )
        Spacer(Modifier.height(PaceDreamSpacing.MD))
        // Search bar placeholder
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD)
                .height(48.dp),
            cornerRadius = PaceDreamRadius.Round
        )
        Spacer(Modifier.height(PaceDreamSpacing.LG))
        // Section header placeholder
        SkeletonBlock(
            modifier = Modifier
                .padding(horizontal = PaceDreamSpacing.MD)
                .height(16.dp)
                .fillMaxWidth(0.4f)
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        // Horizontal listings row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            repeat(3) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(180.dp)
                        .height(200.dp),
                    cornerRadius = PaceDreamRadius.MD
                )
            }
        }
        Spacer(Modifier.height(PaceDreamSpacing.LG))
        // Local categories grid
        SkeletonBlock(
            modifier = Modifier
                .padding(horizontal = PaceDreamSpacing.MD)
                .height(16.dp)
                .fillMaxWidth(0.45f)
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        Column(Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
            repeat(2) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = PaceDreamSpacing.XS),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    repeat(2) {
                        SkeletonBlock(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            cornerRadius = PaceDreamRadius.MD
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(PaceDreamSpacing.XL))
    }
}

@Composable
private fun DestinationListingsSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(6) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        cornerRadius = 0.dp
                    )
                    Column(
                        Modifier.padding(PaceDreamSpacing.SM),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp),
                            alpha = 0.10f
                        )
                    }
                }
            }
        }
    }
}
