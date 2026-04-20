package com.shourov.apps.pacedream.feature.home.presentation.redesign

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home Redesign V1 — conversion-focused marketplace home (stateless).
 *
 * Primary taxonomy: Spaces / Items / Services (via [TypeTabs]).
 * Access patterns (hourly, daily, split, instant…) are secondary filter
 * chips and inline card badges.
 *
 * This composable is fully driven by its props — the stateful entry point
 * is [HomeRedesignScreenWrapper], which collects domain state from
 * [com.shourov.apps.pacedream.feature.home.presentation.HomeScreenViewModel]
 * and maps it to presentational [Listing]s.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeRedesignScreen(
    /** Listings for the Spaces primary rail — typically rooms from backend. */
    spacesListings: List<Listing>,
    /** Listings for the Items primary rail — typically rented gear. */
    itemsListings: List<Listing>,
    /** Listings for the Services primary rail — optional; empty until the backend exists. */
    servicesListings: List<Listing>,
    /** Listings shown when the user picks the "Split cost" access chip on Spaces. */
    splitListings: List<Listing>,
    /** Favorited listing ids across all rails — drives heart state + triggers burst on transition. */
    favoriteIds: Set<String>,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onListingClick: (Listing) -> Unit = {},
    onFavoriteToggle: (listingId: String) -> Unit = {},
    onHostClick: () -> Unit = {},
    onSeeAll: (PrimaryType) -> Unit = {},
) {
    var type by rememberSaveable { mutableStateOf(PrimaryType.SPACES) }
    var category by rememberSaveable(type) { mutableStateOf("all") }
    var access by rememberSaveable(type) { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }

    val pullState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

    // Pick which rail data to show for the primary section
    val primaryListings: List<Listing> = when (type) {
        PrimaryType.SPACES ->
            if (access == "split") splitListings
            else spacesListings
        PrimaryType.ITEMS -> itemsListings
        PrimaryType.SERVICES -> servicesListings
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeRedesignTheme.PaperBg)
            .pullRefresh(pullState),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item("hero") {
                HomeHero(
                    type = type,
                    onTypeChange = { type = it },
                    access = access,
                    onAccessChange = { access = it },
                    onSearch = onSearchClick,
                    onOpenNotifications = onNotificationClick,
                )
            }

            item("category-row") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(HomeRedesignTheme.PaperBg)
                        .padding(top = 4.dp),
                ) {
                    CategoryRow(
                        value = category,
                        onChange = { category = it },
                        categories = HomeRedesignData.categoriesFor(type),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(HomeRedesignTheme.Line),
                    )
                }
            }

            item("primary-section-header") {
                Spacer(Modifier.height(22.dp))
                SectionHeader(
                    title = primaryTitle(type, access),
                    subtitle = primarySubtitle(type, access),
                    onSeeAll = { onSeeAll(type) },
                )
                Spacer(Modifier.height(12.dp))
            }

            item("primary-section-rail") {
                if (primaryListings.isEmpty()) {
                    val msg = when {
                        type == PrimaryType.SERVICES ->
                            "Services are coming soon — check back shortly."
                        access != null ->
                            "No ${type.label.lowercase()} match that filter — try another chip."
                        else ->
                            "Nothing here yet — pull to refresh."
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(msg, color = HomeRedesignTheme.InkFaint, fontSize = 13.sp)
                    }
                } else {
                    PrimaryRail(
                        items = primaryListings,
                        favoriteIds = favoriteIds,
                        onListingClick = onListingClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            item("trust-strip") {
                Spacer(Modifier.height(28.dp))
                TrustStripCard()
            }

            // Cross-type rails — always surface the other two primary types
            if (type != PrimaryType.ITEMS) {
                crossSection(
                    title = "Items to rent nearby",
                    subtitle = "Gear for your next project",
                    rowItems = itemsListings,
                    favoriteIds = favoriteIds,
                    onSeeAll = { onSeeAll(PrimaryType.ITEMS) },
                    onClick = onListingClick,
                    onFavoriteToggle = onFavoriteToggle,
                )
            }
            if (type != PrimaryType.SERVICES && servicesListings.isNotEmpty()) {
                crossSection(
                    title = "Services you might need",
                    subtitle = "Vetted local providers",
                    rowItems = servicesListings,
                    favoriteIds = favoriteIds,
                    onSeeAll = { onSeeAll(PrimaryType.SERVICES) },
                    onClick = onListingClick,
                    onFavoriteToggle = onFavoriteToggle,
                )
            }
            if (type != PrimaryType.SPACES) {
                crossSection(
                    title = "Spaces to book near you",
                    subtitle = "Meeting rooms, study nooks, stays",
                    rowItems = spacesListings,
                    favoriteIds = favoriteIds,
                    onSeeAll = { onSeeAll(PrimaryType.SPACES) },
                    onClick = onListingClick,
                    onFavoriteToggle = onFavoriteToggle,
                )
            }

            item("host-cta") {
                Spacer(Modifier.height(28.dp))
                HostCtaCard(onClick = onHostClick)
            }
        }

        // Sticky compact search — slides in once the hero scrolls out of view
        AnimatedVisibility(
            visible = isScrolled,
            enter = slideInVertically(animationSpec = tween(240)) { -it },
            exit = slideOutVertically(animationSpec = tween(240)) { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            StickyCompactSearch(type = type, onClick = onSearchClick)
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = HomeRedesignTheme.Purple.c600,
        )
    }
}

@Composable
private fun PrimaryRail(
    items: List<Listing>,
    favoriteIds: Set<String>,
    onListingClick: (Listing) -> Unit,
    onFavoriteToggle: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
            ListingCard(
                item = item,
                hueSeed = idx,
                liked = item.id in favoriteIds,
                onLikeToggle = { onFavoriteToggle(item.id) },
                onClick = { onListingClick(item) },
            )
        }
    }
}

@Composable
private fun TrustStripCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(HomeRedesignTheme.CardShape)
            .background(HomeRedesignTheme.PaperSurface)
            .border(1.dp, HomeRedesignTheme.Line, HomeRedesignTheme.CardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(HomeRedesignTheme.Purple.c50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = HomeRedesignTheme.Purple.c600,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Every host & provider is ID-verified",
                color = HomeRedesignTheme.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Book with confidence — reviews are from real transactions.",
                color = HomeRedesignTheme.InkDim,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun HostCtaCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(HomeRedesignTheme.CardShape)
            .background(HomeRedesignTheme.Purple.c50)
            .border(1.dp, HomeRedesignTheme.Purple.c100, HomeRedesignTheme.CardShape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Earn with what you have",
                color = HomeRedesignTheme.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "List a space, rent out gear, or offer your skills — your terms.",
                color = HomeRedesignTheme.InkDim,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .clip(HomeRedesignTheme.PillShape)
                .background(HomeRedesignTheme.Ink)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Start listing", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Reusable cross-type rail as a [LazyListScope] extension. */
private fun LazyListScope.crossSection(
    title: String,
    subtitle: String,
    rowItems: List<Listing>,
    favoriteIds: Set<String>,
    onSeeAll: () -> Unit,
    onClick: (Listing) -> Unit,
    onFavoriteToggle: (String) -> Unit,
) {
    item("cross-$title-header") {
        Spacer(Modifier.height(26.dp))
        SectionHeader(title = title, subtitle = subtitle, onSeeAll = onSeeAll)
        Spacer(Modifier.height(12.dp))
    }
    item("cross-$title-rail") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(rowItems, key = { _, it -> it.id }) { idx, item ->
                ListingCard(
                    item = item,
                    hueSeed = idx + title.length,
                    compact = true,
                    liked = item.id in favoriteIds,
                    onLikeToggle = { onFavoriteToggle(item.id) },
                    onClick = { onClick(item) },
                )
            }
        }
    }
}

private fun primaryTitle(type: PrimaryType, access: String?): String = when (type) {
    PrimaryType.SPACES -> if (access == "split") "Active splits you can join" else "Available near you"
    PrimaryType.ITEMS -> "Ready to rent today"
    PrimaryType.SERVICES -> "Top providers near you"
}

private fun primarySubtitle(type: PrimaryType, access: String?): String = when (type) {
    PrimaryType.SPACES ->
        if (access == "split") "Meet travelers · split the bill"
        else "Book the next hour or night — instant confirmation"
    PrimaryType.ITEMS -> "Pickup or delivery · verified owners"
    PrimaryType.SERVICES -> "Book trusted pros · verified & reviewed"
}
