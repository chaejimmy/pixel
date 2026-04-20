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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * Home Redesign V1 — conversion-focused marketplace home.
 *
 * Primary taxonomy: Spaces / Items / Services (via [TypeTabs]).
 * Access patterns (hourly, daily, split, instant…) are secondary filter chips and card badges.
 * Composition (top → bottom):
 *   • Hero (purple) — logo, bell, tagline, tabs, access chips, search card, trust strip
 *   • Category row — icon+label underline, adapts to the primary type
 *   • Primary rail — filtered listings (with empty state when a chip excludes all)
 *   • Trust strip — ID-verified callout
 *   • Cross-type rails — always surfaces the other two primary types
 *   • Host CTA — "Earn with what you have"
 *   • Sticky compact search — slides in once the hero scrolls out of view
 */
@Composable
fun HomeRedesignScreen(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onListingClick: (String) -> Unit = {},
    onHostClick: () -> Unit = {},
    onSeeAll: (PrimaryType) -> Unit = {},
) {
    var type by rememberSaveable { mutableStateOf(PrimaryType.SPACES) }
    var category by rememberSaveable(type) { mutableStateOf("all") }
    var access by rememberSaveable(type) { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    // The hero is ~first list item; once we scroll past the first item, snap in the sticky bar.
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }

    val listings = HomeRedesignData.listingsFor(type, access)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeRedesignTheme.PaperBg),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item("hero") {
                HomeHero(
                    type = type,
                    onTypeChange = { type = it; category = "all"; access = null },
                    access = access,
                    onAccessChange = { access = it },
                    onSearch = onSearchClick,
                    onOpenNotifications = onNotificationClick,
                )
            }

            // Category rail sitting just under the hero
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

            // Primary section
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
                if (listings.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No ${type.label.lowercase()} match that filter — try another chip.",
                            color = HomeRedesignTheme.InkFaint,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        androidx.compose.foundation.lazy.itemsIndexed(
                            items = listings,
                            key = { _, it -> it.id },
                        ) { idx, item ->
                            ListingCard(
                                item = item,
                                hueSeed = idx,
                                onClick = { onListingClick(item.id) },
                            )
                        }
                    }
                }
            }

            // Trust strip
            item("trust-strip") {
                Spacer(Modifier.height(28.dp))
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

            // Cross-type rails — always surface the other two types
            if (type != PrimaryType.ITEMS) {
                crossSection(
                    title = "Items to rent nearby",
                    subtitle = "Gear for your next project",
                    rowItems = HomeRedesignData.Listings[PrimaryType.ITEMS].orEmpty(),
                    onSeeAll = { onSeeAll(PrimaryType.ITEMS) },
                    onClick = onListingClick,
                )
            }
            if (type != PrimaryType.SERVICES) {
                crossSection(
                    title = "Services you might need",
                    subtitle = "Vetted local providers",
                    rowItems = HomeRedesignData.Listings[PrimaryType.SERVICES].orEmpty(),
                    onSeeAll = { onSeeAll(PrimaryType.SERVICES) },
                    onClick = onListingClick,
                )
            }
            if (type != PrimaryType.SPACES) {
                crossSection(
                    title = "Spaces to book near you",
                    subtitle = "Meeting rooms, study nooks, stays",
                    rowItems = HomeRedesignData.Listings[PrimaryType.SPACES].orEmpty(),
                    onSeeAll = { onSeeAll(PrimaryType.SPACES) },
                    onClick = onListingClick,
                )
            }

            // Host CTA
            item("host-cta") {
                Spacer(Modifier.height(28.dp))
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
                            .clickable { onHostClick() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Start listing",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
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
    }
}

/** Reusable cross-type rail as a [LazyListScope] extension. */
private fun androidx.compose.foundation.lazy.LazyListScope.crossSection(
    title: String,
    subtitle: String,
    rowItems: List<Listing>,
    onSeeAll: () -> Unit,
    onClick: (String) -> Unit,
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
            androidx.compose.foundation.lazy.itemsIndexed(
                items = rowItems,
                key = { _, it -> it.id },
            ) { idx, item ->
                ListingCard(
                    item = item,
                    hueSeed = idx + title.length,
                    compact = true,
                    onClick = { onClick(item.id) },
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

