package com.shourov.apps.pacedream.feature.home.presentation.redesign

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Rich listing card — swipe-able placeholder gallery, heart tap burst,
 * trust/access badges, social proof ("X booked today").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListingCard(
    item: Listing,
    hueSeed: Int,
    compact: Boolean = false,
    onClick: () -> Unit = {},
) {
    val cardWidth = if (compact) 200.dp else 288.dp
    val photoHeight = if (compact) 160.dp else 220.dp

    var photoIdx by remember(item.id) { mutableIntStateOf(0) }
    var liked by remember(item.id) { mutableStateOf(false) }
    var burst by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(burst) {
        if (burst) {
            delay(500)
            burst = false
        }
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(photoHeight)
                .clip(HomeRedesignTheme.CardShape)
                .background(HomeRedesignTheme.LineSoft),
        ) {
            // Gallery "pages" — swap the visible placeholder based on photoIdx
            PhotoPlaceholder(
                hueSeed = hueSeed + photoIdx * 17,
                label = "photo ${photoIdx + 1}/${item.photos}",
                modifier = Modifier.fillMaxSize(),
            )

            // Prev / Next hit targets
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable {
                            photoIdx = (photoIdx - 1 + item.photos) % item.photos
                        },
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable {
                            photoIdx = (photoIdx + 1) % item.photos
                        },
                )
            }

            // Arrow controls
            if (photoIdx > 0) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.ChevronLeft,
                        contentDescription = "Previous photo",
                        tint = HomeRedesignTheme.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (photoIdx < item.photos - 1) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Next photo",
                        tint = HomeRedesignTheme.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Page dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(item.photos) { i ->
                    val size by animateDpAsState(
                        if (i == photoIdx) 7.dp else 5.dp,
                        animationSpec = tween(160),
                        label = "photoDot",
                    )
                    Box(
                        Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(
                                if (i == photoIdx) Color.White
                                else Color.White.copy(alpha = 0.55f),
                            ),
                    )
                }
            }

            // Heart button
            val heartScale by animateFloatAsState(
                targetValue = if (burst) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "heartScale",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .graphicsLayer { scaleX = heartScale; scaleY = heartScale }
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable {
                        if (!liked) burst = true
                        liked = !liked
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (liked) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = "Unfavorite",
                        tint = HomeRedesignTheme.Coral.c500,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Tag chip
            if (item.tag != null) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(HomeRedesignTheme.PillShape)
                        .background(Color.White)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(item.tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HomeRedesignTheme.Ink)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                item.title,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                color = HomeRedesignTheme.Ink,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    tint = HomeRedesignTheme.Star,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "%.2f".format(item.rating),
                    color = HomeRedesignTheme.Ink,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "(${item.reviews})",
                    color = HomeRedesignTheme.InkFaint,
                    fontSize = 11.5.sp,
                )
            }
        }

        Spacer(Modifier.height(2.dp))
        Text(item.area, color = HomeRedesignTheme.InkDim, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 2.dp))

        if ("split" in item.badges && item.dates != null) {
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp),
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = HomeRedesignTheme.InkFaint,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(item.dates, color = HomeRedesignTheme.InkFaint, fontSize = 12.sp)
            }
        }

        if (item.badges.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 2.dp),
            ) {
                item.badges.take(2).forEach { AccessBadge(it) }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "$${item.price}",
                color = HomeRedesignTheme.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(3.dp))
            Text("/ ${item.unit}", color = HomeRedesignTheme.InkDim, fontSize = 12.5.sp)
            if (item.bookedToday != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    "${item.bookedToday} booked today",
                    color = HomeRedesignTheme.Coral.c700,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
