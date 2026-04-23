package com.shourov.apps.pacedream.feature.home.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hero — purple canvas with logo, bell, tagline, primary tabs, access chips,
 * layered search card (What / Where / When / Who) and an inline trust row.
 */
@Composable
fun HomeHero(
    type: PrimaryType,
    onTypeChange: (PrimaryType) -> Unit,
    access: String?,
    onAccessChange: (String?) -> Unit,
    onSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val whatPlaceholder = when (type) {
        PrimaryType.SPACES -> "Parking, meeting room, gym, storage…"
        PrimaryType.ITEMS -> "Camera, bike, drone, tools…"
        PrimaryType.SERVICES -> "Cleaning, tutoring, lessons, repair…"
    }
    val whatSuggestions = HomeRedesignData.WhatSuggestions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeRedesignTheme.HeroBottomShape)
            .background(
                Brush.verticalGradient(
                    0f to HomeRedesignTheme.Purple.c800,
                    0.5f to HomeRedesignTheme.Purple.c700,
                    1f to HomeRedesignTheme.Purple.c600,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 12.dp, bottom = 22.dp),
        ) {
            // Top bar: logo + bell
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "PaceDream",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(HomeRedesignTheme.RadiusMd))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(HomeRedesignTheme.RadiusMd))
                        .clickable { onOpenNotifications() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    // Unread dot
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(HomeRedesignTheme.Coral.c500)
                            .border(1.5.dp, HomeRedesignTheme.Purple.c700, CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "Spaces, items, services —\nall in one place.",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                lineHeight = 32.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Rent by the hour, day, or split the cost with others.",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(16.dp))
            TypeTabs(value = type, onChange = onTypeChange, onHero = true)

            Spacer(Modifier.height(10.dp))
            AccessChipsRow(type = type, value = access, onChange = onAccessChange, onHero = true)

            Spacer(Modifier.height(12.dp))
            SearchCard(whatPlaceholder = whatPlaceholder, onClick = onSearch)

            Spacer(Modifier.height(10.dp))
            WhatSuggestionsRow(suggestions = whatSuggestions, onSuggestionClick = { onSearch() })

            Spacer(Modifier.height(12.dp))
            TrustRow(items = listOf("Verified hosts", "Free cancel 24h", "Secure payments"))
        }
    }
}

/** Layered search card shown inside the hero — What / Where / When / Who + primary Search button. */
@Composable
private fun SearchCard(whatPlaceholder: String, onClick: () -> Unit) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clip(HomeRedesignTheme.SearchCardShape)
            .background(HomeRedesignTheme.PaperSurface)
            .border(1.dp, HomeRedesignTheme.Line, HomeRedesignTheme.SearchCardShape)
            .clickable { onClick() }
            .padding(6.dp),
    ) {
        SearchRow(icon = Icons.Outlined.Search, label = "WHAT", value = whatPlaceholder)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(1.dp)
                .background(HomeRedesignTheme.LineSoft),
        )
        Row {
            Box(Modifier.weight(1f)) {
                SearchRow(icon = Icons.Outlined.LocationOn, label = "WHERE", value = "Brooklyn", compact = true)
            }
            VerticalFieldDivider()
            Box(Modifier.weight(1f)) {
                SearchRow(icon = Icons.Outlined.CalendarMonth, label = "WHEN", value = "Thu · 2–5PM", compact = true)
            }
            VerticalFieldDivider()
            Box(Modifier.weight(1f)) {
                SearchRow(icon = Icons.Outlined.Person, label = "WHO", value = "Add guests", compact = true)
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(HomeRedesignTheme.RadiusMd))
                .background(HomeRedesignTheme.Ink)
                .clickable { onClick() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Search",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SearchRow(icon: ImageVector, label: String, value: String, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 8.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 26.dp else 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HomeRedesignTheme.Purple.c50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = HomeRedesignTheme.Purple.c600,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp),
            )
        }
        Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = HomeRedesignTheme.InkFaint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                value,
                color = HomeRedesignTheme.Ink,
                fontSize = if (compact) 12.5.sp else 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VerticalFieldDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(44.dp)
            .background(HomeRedesignTheme.LineSoft),
    )
}

/**
 * Lightweight chip row surfacing the marketplace scope — Parking, Gym, Storage, etc.
 * Sits just under the search card so users can tap straight into a category
 * without opening the full search experience first.
 */
@Composable
private fun WhatSuggestionsRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Try",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
        Spacer(Modifier.width(8.dp))
        suggestions.forEachIndexed { index, label ->
            if (index > 0) Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(HomeRedesignTheme.PillShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), HomeRedesignTheme.PillShape)
                    .clickable { onSuggestionClick(label) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    label,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Compact sticky search bar that slides in once the user scrolls past the hero. */
@Composable
fun StickyCompactSearch(type: PrimaryType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val subtitle = when (type) {
        PrimaryType.SPACES -> "Spaces · Brooklyn"
        PrimaryType.ITEMS -> "Items · Near you"
        PrimaryType.SERVICES -> "Services · Brooklyn"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeRedesignTheme.PaperSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(HomeRedesignTheme.PillShape)
            .background(HomeRedesignTheme.PaperBg)
            .border(1.dp, HomeRedesignTheme.Line, HomeRedesignTheme.PillShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = HomeRedesignTheme.Ink,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(subtitle, color = HomeRedesignTheme.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Thu · 2–5 PM · 3 people", color = HomeRedesignTheme.InkFaint, fontSize = 11.5.sp)
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HomeRedesignTheme.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = "Filters",
                tint = HomeRedesignTheme.PaperSurface,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
