package com.shourov.apps.pacedream.feature.home.presentation.redesign

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.pacedream.common.composables.theme.PaceDreamRadius
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Primary type tabs — Spaces / Items / Services
// Light variant sits on the purple hero; dark variant sits on paper.
// ---------------------------------------------------------------------------

@Composable
fun TypeTabs(
    value: PrimaryType,
    onChange: (PrimaryType) -> Unit,
    onHero: Boolean,
    modifier: Modifier = Modifier,
) {
    val tabs = PrimaryType.values().toList()
    val containerBg = if (onHero) Color.White.copy(alpha = 0.14f) else HomeRedesignTheme.PaperSurface
    val containerBorder = if (onHero) Color.White.copy(alpha = 0.22f) else HomeRedesignTheme.Line
    val activeBg = if (onHero) Color.White else HomeRedesignTheme.Ink
    val activeInk = if (onHero) HomeRedesignTheme.Purple.c700 else Color.White
    val inactiveInk = if (onHero) Color.White.copy(alpha = 0.82f) else HomeRedesignTheme.Ink.copy(alpha = 0.72f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeRedesignTheme.PillShape)
            .background(containerBg)
            .border(1.dp, containerBorder, HomeRedesignTheme.PillShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { tab ->
            val active = tab == value
            val bg by animateColorAsState(
                if (active) activeBg else Color.Transparent,
                animationSpec = tween(220),
                label = "typeTabBg",
            )
            val ink by animateColorAsState(
                if (active) activeInk else inactiveInk,
                animationSpec = tween(220),
                label = "typeTabInk",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(HomeRedesignTheme.PillShape)
                    .background(bg)
                    .clickable { onChange(tab) }
                    .padding(vertical = 9.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tab.label,
                    color = ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    tab.subtitle,
                    color = ink.copy(alpha = if (active) 0.7f else 0.6f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Access pattern chips — secondary filter row
// ---------------------------------------------------------------------------

@Composable
fun AccessChipsRow(
    type: PrimaryType,
    value: String?,
    onChange: (String?) -> Unit,
    onHero: Boolean,
    modifier: Modifier = Modifier,
) {
    val chips = HomeRedesignData.AccessChips[type].orEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { chip ->
            val active = chip.id == value
            val bg = when {
                active && onHero -> Color.White
                active -> HomeRedesignTheme.Ink
                onHero -> Color.White.copy(alpha = 0.14f)
                else -> HomeRedesignTheme.PaperSurface
            }
            val ink = when {
                active && onHero -> HomeRedesignTheme.Purple.c700
                active -> Color.White
                onHero -> Color.White
                else -> HomeRedesignTheme.Ink
            }
            val border = when {
                active -> Color.Transparent
                onHero -> Color.White.copy(alpha = 0.25f)
                else -> HomeRedesignTheme.Line
            }
            Box(
                modifier = Modifier
                    .clip(HomeRedesignTheme.PillShape)
                    .background(bg)
                    .border(1.dp, border, HomeRedesignTheme.PillShape)
                    .clickable { onChange(if (active) null else chip.id) }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    chip.label,
                    color = ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Category pill row — icon + label with animated underline
// ---------------------------------------------------------------------------

@Composable
fun CategoryRow(
    value: String,
    onChange: (String) -> Unit,
    categories: List<CategoryOption>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        categories.forEach { cat ->
            val active = cat.id == value
            val ink = if (active) HomeRedesignTheme.Ink else HomeRedesignTheme.InkFaint
            val underlineW by animateDpAsState(
                if (active) 28.dp else 0.dp,
                animationSpec = tween(180),
                label = "categoryUnderline",
            )
            Column(
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .clickable { onChange(cat.id) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(cat.icon, contentDescription = cat.label, tint = ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    cat.label,
                    color = ink,
                    fontSize = 11.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .height(2.dp)
                        .width(underlineW)
                        .clip(RoundedCornerShape(PaceDreamRadius.XS))
                        .background(HomeRedesignTheme.Ink),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section header — title + subtitle + "See all" action
// ---------------------------------------------------------------------------

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = HomeRedesignTheme.Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = HomeRedesignTheme.InkFaint, fontSize = 12.5.sp)
            }
        }
        if (onSeeAll != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSeeAll() }.padding(4.dp),
            ) {
                Text(
                    "See all",
                    color = HomeRedesignTheme.Purple.c600,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = HomeRedesignTheme.Purple.c600,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Badge — inline pill shown on listing cards to surface access pattern
// ---------------------------------------------------------------------------

private enum class BadgeTone { Neutral, Brand, Accent }

private fun toneFor(badgeId: String): BadgeTone = when (badgeId) {
    "instant" -> BadgeTone.Accent
    "split", "shared" -> BadgeTone.Brand
    else -> BadgeTone.Neutral
}

@Composable
fun AccessBadge(badgeId: String) {
    val meta = HomeRedesignData.BadgeMeta[badgeId] ?: return
    val (bg, ink, dot) = when (toneFor(badgeId)) {
        BadgeTone.Accent  -> Triple(HomeRedesignTheme.Coral.c100,  HomeRedesignTheme.Coral.c700,  HomeRedesignTheme.Coral.c500)
        BadgeTone.Brand   -> Triple(HomeRedesignTheme.Purple.c50,  HomeRedesignTheme.Purple.c700, HomeRedesignTheme.Purple.c500)
        BadgeTone.Neutral -> Triple(HomeRedesignTheme.LineSoft,    HomeRedesignTheme.Ink,         HomeRedesignTheme.InkDim)
    }
    Row(
        modifier = Modifier
            .clip(HomeRedesignTheme.PillShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(5.dp))
        Text(meta.label, color = ink, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// Trust row — small inline chips shown inside the purple hero
// ---------------------------------------------------------------------------

@Composable
fun TrustRow(items: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items.forEach { label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(label, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

/** Placeholder photo strip — stripe pattern keyed by hue seed. Stands in for real imagery. */
@Composable
fun PhotoPlaceholder(hueSeed: Int, label: String? = null, modifier: Modifier = Modifier) {
    val base = remember(hueSeed) {
        val palette = listOf(
            Color(0xFFE8DFF6), Color(0xFFFDE0D0), Color(0xFFD5E4F5),
            Color(0xFFDDEED9), Color(0xFFF5E5D0), Color(0xFFE8D5E8),
        )
        palette[((hueSeed % palette.size) + palette.size) % palette.size]
    }
    val stripe = base.copy(
        red = (base.red * 0.92f).coerceIn(0f, 1f),
        green = (base.green * 0.92f).coerceIn(0f, 1f),
        blue = (base.blue * 0.92f).coerceIn(0f, 1f),
    )
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    0f to base,
                    0.5f to stripe,
                    1f to base,
                ),
            ),
    ) {
        if (label != null) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.XS))
                    .background(Color.White.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(label, fontSize = 10.sp, color = HomeRedesignTheme.Ink)
            }
        }
    }
}
