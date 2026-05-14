package com.shourov.apps.pacedream.feature.homefeed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.pacedream.common.composables.components.InlineErrorBanner
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.PaceDreamAnimationDuration
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamEasing
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.core.network.observer.ConnectionState
import com.shourov.apps.pacedream.ui.ConnectivityViewModel
import com.shourov.apps.pacedream.listing.ListingPreview
import com.shourov.apps.pacedream.listing.ListingPreviewStore
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Brand accents — track `PaceDreamColors.Primary` (PaceDream purple, #5527D7).
// Local tints/edges are kept here so the hero canvas can layer soft tonal
// surfaces without having to expose every variant on the global token.
// ─────────────────────────────────────────────────────────────────────────────

private val BrandPurple = PaceDreamColors.Primary // alias to global brand purple
private val BrandPurpleSoft = Color(0xFFF5F1FB)
private val BrandPurpleEdge = Color(0xFFE2D8F5)
private val InkPrimary = Color(0xFF1A1522)
private val InkMuted = Color(0xFF5B5568)
private val PaperBg = Color(0xFFF7F6F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    onListingClick: (String) -> Unit,
    onSeeAll: (HomeSectionKey) -> Unit,
    onShowAuthSheet: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onWhatClick: () -> Unit = onSearchClick,
    onWhereClick: () -> Unit = onSearchClick,
    onWhenClick: () -> Unit = onSearchClick,
    onNotificationClick: () -> Unit = {},
    // The "Can't find what you need? Post a request" rail-end card routes
    // to the wanted/post-request flow, not to Search.
    onPostRequestClick: () -> Unit = {},
    // The "Earn from unused spaces" promo and its "Start hosting" pill route
    // to the host / create-listing flow, not to Search.
    onStartHostingClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeFeedViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
) {
    val state by viewModel.filteredState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val connectionState by connectivityViewModel.state.collectAsStateWithLifecycle()
    val isOffline = connectionState != ConnectionState.Available
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun onFavorite(listingId: String) {
        if (authState == AuthState.Unauthenticated) {
            onShowAuthSheet()
            return
        }
        scope.launch {
            try {
                val wasFavorited = favoriteIds.contains(listingId)
                when (val res = viewModel.toggleFavorite(listingId)) {
                    is ApiResult.Success -> snackbarHostState.showSnackbar(if (wasFavorited) "Removed from Favorites" else "Saved to Favorites")
                    is ApiResult.Failure -> {
                        if (res.error is com.shourov.apps.pacedream.core.network.api.ApiError.Unauthorized) {
                            onShowAuthSheet()
                        } else {
                            snackbarHostState.showSnackbar(res.error.message ?: "Failed to save")
                        }
                    }
                }
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Failed to save")
            }
        }
    }

    val spacesSection = state.sections.firstOrNull { it.key == HomeSectionKey.SPACES }
    val gearSection = state.sections.firstOrNull { it.key == HomeSectionKey.ITEMS }
    val helpSection = state.sections.firstOrNull { it.key == HomeSectionKey.SERVICES }

    // The Stays section pulls lodging-ish items; Flexible Spaces pulls the rest.
    // Both feed from the same backend bucket (shareType=USE) — we partition
    // client-side by subcategory keywords so the home reads like a marketplace,
    // not a single dense "Spaces" rail.
    val staysItems = remember(spacesSection?.items) {
        spacesSection?.items?.filter(::isStayLike).orEmpty()
    }
    val flexSpaceItems = remember(spacesSection?.items) {
        spacesSection?.items?.filterNot(::isStayLike).orEmpty()
    }
    val popularItems = remember(spacesSection?.items, gearSection?.items) {
        // Cross-type curated rail — pick highest-rated items across stays + gear.
        val pool = (spacesSection?.items.orEmpty() + gearSection?.items.orEmpty())
        pool
            .filter { (it.rating ?: 0.0) > 0.0 }
            .sortedByDescending { it.rating ?: 0.0 }
            .take(8)
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaperBg),
                contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXXL)
            ) {
                // 1. Hero — discovery header + search experience.
                item(key = "discover_header", contentType = "header") {
                    DiscoverHeader(
                        title = state.headerTitle,
                        subtitle = state.headerSubtitle,
                        onSearchClick = onSearchClick,
                        onWhatClick = onWhatClick,
                        onWhereClick = onWhereClick,
                        onWhenClick = onWhenClick,
                        onNotificationClick = onNotificationClick
                    )
                }

                // 2. Main categories — Stays / Gear / Spaces / Help.
                //    Replaces the dense restroom/nap-pod chip row that used to
                //    sit here. Utility taxonomy moves deeper into search.
                item(key = "main_categories", contentType = "main_categories") {
                    MainCategoryRow(
                        onCategoryClick = { category -> onSeeAll(category.section) }
                    )
                }

                state.globalErrorMessage?.let { globalErr ->
                    item(key = "global_error") {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        InlineErrorBanner(
                            message = globalErr,
                            onAction = { viewModel.refresh() },
                            actionText = "Retry",
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }
                }

                // 3. Popular nearby — curated cross-type rail.
                if (popularItems.isNotEmpty() || spacesSection?.isLoading == true) {
                    railSection(
                        sectionKey = "popular_nearby",
                        title = "Popular nearby",
                        subtitle = "Loved by guests this week",
                        items = popularItems,
                        isLoading = spacesSection?.isLoading == true,
                        favoriteIds = favoriteIds,
                        onListingClick = onListingClick,
                        onFavorite = ::onFavorite,
                        onSeeAll = { onSeeAll(HomeSectionKey.SPACES) },
                        isOffline = isOffline,
                        emptyMessage = "Nothing trending right now — pull to refresh.",
                        onRefresh = { viewModel.refresh() }
                    )
                }

                // 4. Flexible spaces — hourly/daily utility rooms.
                railSection(
                    sectionKey = "flex_spaces",
                    title = "Flexible spaces",
                    subtitle = "Meeting rooms, desks, parking — by the hour",
                    items = flexSpaceItems,
                    isLoading = spacesSection?.isLoading == true,
                    sectionError = spacesSection?.errorMessage?.takeIf { !isOffline },
                    favoriteIds = favoriteIds,
                    onListingClick = onListingClick,
                    onFavorite = ::onFavorite,
                    onSeeAll = { onSeeAll(HomeSectionKey.SPACES) },
                    isOffline = isOffline,
                    emptyMessage = "No flexible spaces nearby yet. Try a wider search.",
                    onRefresh = { viewModel.refresh() }
                )

                // 5. Weekend stays — lodging-style rail.
                railSection(
                    sectionKey = "weekend_stays",
                    title = "Weekend stays",
                    subtitle = "Short stays, cabins and lofts near you",
                    items = staysItems,
                    isLoading = spacesSection?.isLoading == true,
                    sectionError = spacesSection?.errorMessage?.takeIf { !isOffline && staysItems.isEmpty() },
                    favoriteIds = favoriteIds,
                    onListingClick = onListingClick,
                    onFavorite = ::onFavorite,
                    onSeeAll = { onSeeAll(HomeSectionKey.SPACES) },
                    isOffline = isOffline,
                    emptyMessage = "No weekend stays nearby yet.",
                    onRefresh = { viewModel.refresh() }
                )

                // 6. Gear nearby — physical items to borrow.
                railSection(
                    sectionKey = "gear_nearby",
                    title = "Gear nearby",
                    subtitle = "Cameras, bikes, tools — pick up today",
                    items = gearSection?.items.orEmpty(),
                    isLoading = gearSection?.isLoading == true,
                    sectionError = gearSection?.errorMessage?.takeIf { !isOffline },
                    favoriteIds = favoriteIds,
                    onListingClick = onListingClick,
                    onFavorite = ::onFavorite,
                    onSeeAll = { onSeeAll(HomeSectionKey.ITEMS) },
                    isOffline = isOffline,
                    emptyMessage = "No gear listed nearby yet.",
                    onRefresh = { viewModel.refresh() }
                )

                // 7. Help nearby — services rail. Rail name aligns with
                //    the "Help" main category tile so the taxonomy reads as
                //    one system, not two.
                railSection(
                    sectionKey = "local_help",
                    title = "Help nearby",
                    subtitle = "Trusted helpers for what you need today",
                    items = helpSection?.items.orEmpty(),
                    isLoading = helpSection?.isLoading == true,
                    sectionError = helpSection?.errorMessage?.takeIf { !isOffline },
                    favoriteIds = favoriteIds,
                    onListingClick = onListingClick,
                    onFavorite = ::onFavorite,
                    onSeeAll = { onSeeAll(HomeSectionKey.SERVICES) },
                    isOffline = isOffline,
                    emptyMessage = "No helpers listed yet. Be the first to offer help.",
                    onRefresh = { viewModel.refresh() }
                )

                // 8. Request card — routes to the Post a Request flow, not Search.
                item(key = "request_card", contentType = "request") {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
                    RequestCard(onClick = onPostRequestClick)
                }

                // 9. Host CTA — routes to the host / create-listing flow, not Search.
                item(key = "host_cta", contentType = "host_cta") {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    HostCtaCard(onClick = onStartHostingClick)
                }

                // Spacer to clear the bottom nav comfortably.
                item { Spacer(modifier = Modifier.height(PaceDreamSpacing.XXL)) }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaceDreamSpacing.LG)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero — premium discovery header with airy gradient surface and structured
// search pill. Keeps the iOS three-segment shape (What / Where / When) so the
// downstream pickers don't change, but the new copy and softer surface read as
// "flexible local access", not a utility inventory list.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiscoverHeader(
    title: String,
    subtitle: String,
    onSearchClick: () -> Unit,
    onWhatClick: () -> Unit = onSearchClick,
    onWhereClick: () -> Unit = onSearchClick,
    onWhenClick: () -> Unit = onSearchClick,
    onNotificationClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to PaperBg,
                    0.55f to BrandPurpleSoft,
                    1f to PaperBg
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = PaceDreamSpacing.LG,
                    end = PaceDreamSpacing.LG,
                    top = PaceDreamSpacing.SM,
                    bottom = PaceDreamSpacing.MD
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.SM))
                            .background(BrandPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        text = "PaceDream",
                        style = PaceDreamTypography.Headline.copy(
                            fontFamily = paceDreamDisplayFontFamily,
                            letterSpacing = (-0.2).sp
                        ),
                        color = InkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(PaceDreamButtonHeight.MD)
                        .clickable(onClick = onNotificationClick),
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurpleEdge)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PaceDreamIcons.Notifications,
                            contentDescription = "Notifications",
                            tint = InkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Hero copy kept tight so the first listing rail clears the fold
            // on common Android screen sizes (5.5" / 360dp width and up).
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = title,
                style = PaceDreamTypography.LargeTitle.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.6).sp
                ),
                color = InkPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = PaceDreamTypography.Body.copy(
                    fontFamily = paceDreamFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                color = InkMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Premium search pill — Material 3 surface with soft shadow.
            // Tappable three-segment structure preserved so existing pickers
            // (what / where / when) stay wired up.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        ambientColor = BrandPurple.copy(alpha = 0.08f),
                        spotColor = BrandPurple.copy(alpha = 0.10f)
                    ),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                color = Color.White,
                tonalElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurpleEdge)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchSegment(
                        label = "What",
                        value = "Stays · gear · spaces · help",
                        onClick = onWhatClick,
                        modifier = Modifier.weight(1.4f)
                    )
                    SegmentDivider()
                    SearchSegment(
                        label = "Where",
                        value = "Anywhere",
                        onClick = onWhereClick,
                        modifier = Modifier.weight(1f)
                    )
                    SegmentDivider()
                    SearchSegment(
                        label = "When",
                        value = "Any time",
                        onClick = onWhenClick,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSearchClick),
                        shape = CircleShape,
                        color = BrandPurple
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = PaceDreamIcons.Search,
                                contentDescription = "Search stays, gear, spaces, or help",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSegment(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = PaceDreamTypography.Caption.copy(
                fontFamily = paceDreamFontFamily,
                fontSize = 12.sp
            ),
            color = InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SegmentDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(BrandPurpleEdge)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main category row — Stays / Gear / Spaces / Help.
// Replaces the previous restroom/nap-pod utility chip row that used to sit
// here. Utility taxonomy moves into search filters; the home stays high level.
// ─────────────────────────────────────────────────────────────────────────────

private data class MainCategory(
    val label: String,
    val tagline: String,
    val icon: ImageVector,
    val section: HomeSectionKey,
)

private val MAIN_CATEGORIES = listOf(
    MainCategory("Stays", "Short stays nearby", PaceDreamIcons.HotelOutlined, HomeSectionKey.SPACES),
    MainCategory("Gear", "Cameras, bikes, tools", PaceDreamIcons.ShoppingBag, HomeSectionKey.ITEMS),
    MainCategory("Spaces", "Rooms, desks, parking", PaceDreamIcons.MeetingRoomOutlined, HomeSectionKey.SPACES),
    MainCategory("Help", "Trusted local helpers", PaceDreamIcons.HelpOutline, HomeSectionKey.SERVICES),
)

@Composable
private fun MainCategoryRow(
    onCategoryClick: (MainCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = PaceDreamSpacing.SM)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(MAIN_CATEGORIES) { category ->
                MainCategoryTile(category = category, onClick = { onCategoryClick(category) })
            }
        }
    }
}

@Composable
private fun MainCategoryTile(
    category: MainCategory,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurpleEdge),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(BrandPurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = BrandPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = category.label,
                    style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
                    color = InkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = category.tagline,
                    style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = InkMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header + rail wrapper. Replaces the old PaceDreamSectionHeader and
// keeps the title/subtitle/See-all rhythm consistent across the home.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp
                ),
                color = InkPrimary,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = InkMuted,
                    fontSize = 12.5.sp
                )
            }
        }
        if (onSeeAll != null) {
            Text(
                text = "See all",
                style = PaceDreamTypography.Callout.copy(fontFamily = paceDreamFontFamily),
                color = BrandPurple,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable(onClick = onSeeAll)
                    .padding(start = 8.dp, top = 2.dp)
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.railSection(
    sectionKey: String,
    title: String,
    subtitle: String,
    items: List<HomeCard>,
    isLoading: Boolean,
    sectionError: String? = null,
    favoriteIds: Set<String>,
    onListingClick: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onSeeAll: () -> Unit,
    isOffline: Boolean,
    emptyMessage: String,
    onRefresh: () -> Unit
) {
    item(key = "${sectionKey}_header") {
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        SectionHeader(title = title, subtitle = subtitle, onSeeAll = onSeeAll)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
    }

    sectionError?.let {
        item(key = "${sectionKey}_error") {
            InlineErrorBanner(
                message = it,
                onAction = onRefresh,
                actionText = "Retry",
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
    }

    item(key = "${sectionKey}_body") {
        AnimatedContent(
            targetState = SectionContentState(
                isLoading = isLoading,
                isEmpty = items.isEmpty()
            ),
            transitionSpec = {
                fadeIn(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)) togetherWith
                    fadeOut(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut))
            },
            label = "section_content_$sectionKey"
        ) { contentState ->
            when {
                contentState.isLoading -> SkeletonRow()
                contentState.isEmpty -> EmptyInline(
                    message = if (isOffline) "You're offline. Connect to see more here." else emptyMessage,
                    onRefresh = onRefresh
                )
                else -> CardsRow(
                    items = items,
                    onListingClick = { onListingClick(it) },
                    favoriteIds = favoriteIds,
                    onFavorite = onFavorite
                )
            }
        }
    }
}

private data class SectionContentState(
    val isLoading: Boolean,
    val isEmpty: Boolean,
)

// ─────────────────────────────────────────────────────────────────────────────
// Listing card — image-first, premium hierarchy.
//   1. imagery (4:3-ish)
//   2. title  (semibold, 1 line)
//   3. price  (bold, prominent)
//   4. trust  (rating)
//   5. location (dim caption)
// Removes the floating price-pill that used to sit on top of the photo, and
// drops the extra heavy gradient overlay — cleaner, less busy, more Airbnb /
// Hipcamp-feeling.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardsRow(
    items: List<HomeCard>,
    onListingClick: (String) -> Unit,
    favoriteIds: Set<String>,
    onFavorite: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(items, key = { it.id }) { item ->
            ListingCard(
                item = item,
                onClick = { onListingClick(item.id) },
                isFavorited = favoriteIds.contains(item.id),
                onFavorite = { onFavorite(item.id) }
            )
        }
    }
}

@Composable
private fun ListingCard(
    item: HomeCard,
    isFavorited: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
        .width(260.dp)
) {
    Card(
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
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Explicit 4:3 ratio so home + skeleton + redesign cards
                    // all share the same media slot proportions.
                    .aspectRatio(4f / 3f)
                    .shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    )
                    .clip(RoundedCornerShape(PaceDreamRadius.LG))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl?.takeIf { it.isNotBlank() })
                        .crossfade(200)
                        .size(coil.size.Size(560, 420))
                        .build(),
                    contentDescription = item.title.ifBlank { "Listing" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrandPurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Search,
                            contentDescription = null,
                            tint = BrandPurple
                        )
                    }
                }
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.18f))
                ) {
                    AnimatedContent(
                        targetState = isFavorited,
                        transitionSpec = {
                            (fadeIn(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)) +
                                scaleIn(initialScale = 0.85f, animationSpec = tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut))) togetherWith
                                (fadeOut(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)) +
                                    scaleOut(targetScale = 0.9f, animationSpec = tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)))
                        },
                        label = "favorite_toggle"
                    ) { favored ->
                        Icon(
                            imageVector = if (favored) PaceDreamIcons.Favorite else PaceDreamIcons.FavoriteBorder,
                            contentDescription = if (favored) "Remove from favorites" else "Save to favorites",
                            tint = if (favored) PaceDreamColors.Error else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title.ifBlank { "Listing" },
                    style = PaceDreamTypography.Callout.copy(fontFamily = paceDreamFontFamily),
                    color = InkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = PaceDreamSpacing.SM)
                )
                item.rating?.takeIf { it > 0.0 }?.let { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = null,
                            tint = PaceDreamColors.StarRating,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", r),
                            style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                            color = InkPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            item.location?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = InkMuted,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            item.priceText?.takeIf { it.isNotBlank() }?.let { price ->
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = price,
                    style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
    }
}

@Composable
private fun SkeletonRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(6) { _ ->
            Column(modifier = Modifier.width(260.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Match the live ListingCard 4:3 ratio so the skeleton
                        // doesn't jump when the real card resolves.
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                        .background(BrandPurpleEdge.copy(alpha = 0.6f))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .background(BrandPurpleEdge, RoundedCornerShape(PaceDreamRadius.SM))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(12.dp)
                        .background(BrandPurpleEdge.copy(alpha = 0.7f), RoundedCornerShape(PaceDreamRadius.SM))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun EmptyInline(
    message: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .then(modifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurpleEdge)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Search,
                contentDescription = null,
                tint = BrandPurple
            )
            Text(
                text = message,
                style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
                color = InkMuted,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onRefresh,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPurple),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurple.copy(alpha = 0.5f))
            ) {
                Text("Refresh")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Request card — surfaces the "can't find it? post a request" flow as a soft
// marketplace prompt, not an aggressive CTA.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RequestCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurpleEdge),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandPurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Send,
                    contentDescription = null,
                    tint = BrandPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Can't find what you need?",
                    style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
                    color = InkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Post a request and local hosts can help.",
                    style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = InkMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = InkMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Host CTA — softer "earn from unused spaces and gear" promo. Calmer than
// the previous bottom-of-screen onboarding banners, with a clear secondary
// CTA hierarchy (purple-tinted card, dark button, no gradient noise).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostCtaCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = BrandPurpleSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurpleEdge),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Earn from unused spaces and gear",
                    style = PaceDreamTypography.Title3.copy(
                        fontFamily = paceDreamDisplayFontFamily,
                        fontSize = 17.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "List a stay, rent out gear, or offer your help — on your terms.",
                    style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = InkMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(PaceDreamRadius.MD)),
                color = InkPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Start hosting",
                        color = Color.White,
                        style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Heuristic split of the Spaces backend bucket into lodging-like ("Stays")
 * vs hourly utility ("Flexible spaces") on the client. Both rails feed from
 * shareType=USE; we just present them as two visually distinct sections so
 * the home doesn't read as one dense utility catalogue.
 */
private val STAY_SUBCATEGORY_KEYWORDS = listOf(
    "apartment", "loft", "cabin", "villa", "hotel", "short_stay", "stay", "house", "studio", "suite", "bnb"
)

private fun isStayLike(card: HomeCard): Boolean {
    val sub = card.subCategory?.lowercase().orEmpty()
    val title = card.title.lowercase()
    return STAY_SUBCATEGORY_KEYWORDS.any { sub.contains(it) || title.contains(it) }
}
