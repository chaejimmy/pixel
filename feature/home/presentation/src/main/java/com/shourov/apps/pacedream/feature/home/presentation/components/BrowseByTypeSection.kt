package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pacedream.common.composables.components.PaceDreamShimmerCard
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.home.domain.models.RentedGearModel
import com.shourov.apps.pacedream.feature.home.domain.models.SplitStayModel
import com.shourov.apps.pacedream.feature.home.domain.models.rooms.RoomModel
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRentedGearsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRoomsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenSplitStaysState

/**
 * Browse by Type: Spaces / Items / Services — matching iOS ExploreByTypeSection.swift
 *
 * Segmented pill selector at top, subcategory chips below, inline listing preview.
 * Switches content in-place without navigation.
 */

enum class HomeBrowseType(
    val displayTitle: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
) {
    SPACES(
        displayTitle = "Spaces",
        subtitle = "Book flexible places nearby",
        icon = Icons.Default.Business,
        gradientColors = listOf(Color(0xFF5527D7), Color(0xFF7C5CE7)),
    ),
    ITEMS(
        displayTitle = "Items",
        subtitle = "Borrow useful things on demand",
        icon = Icons.Default.Inventory2,
        gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)),
    ),
    SERVICES(
        displayTitle = "Services",
        subtitle = "Find help for everyday needs",
        icon = Icons.Default.CleaningServices,
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399)),
    );

    val subcategories: List<BrowseSubcategory>
        get() = when (this) {
            SPACES -> listOf(
                BrowseSubcategory("parking", "Parking", Icons.Default.DirectionsCar),
                BrowseSubcategory("restroom", "Restroom", Icons.Default.Wc),
                BrowseSubcategory("nap_pod", "Nap Pod", Icons.Default.Bed),
                BrowseSubcategory("meeting_room", "Meeting Room", Icons.Default.Groups),
                BrowseSubcategory("storage_space", "Storage", Icons.Default.Archive),
                BrowseSubcategory("gym", "Gym", Icons.Default.FitnessCenter),
            )
            ITEMS -> listOf(
                BrowseSubcategory("camera", "Camera", Icons.Default.CameraAlt),
                BrowseSubcategory("sports_gear", "Sports Gear", Icons.Default.SportsBasketball),
                BrowseSubcategory("tools", "Tools", Icons.Default.Build),
                BrowseSubcategory("tech", "Tech", Icons.Default.Laptop),
                BrowseSubcategory("micromobility", "Bike", Icons.Default.PedalBike),
                BrowseSubcategory("instrument", "Instrument", Icons.Default.MusicNote),
            )
            SERVICES -> listOf(
                BrowseSubcategory("cleaning_organizing", "Cleaning", Icons.Default.CleaningServices),
                BrowseSubcategory("moving_help", "Moving Help", Icons.Default.LocalShipping),
                BrowseSubcategory("home_help", "Home Help", Icons.Default.Home),
                BrowseSubcategory("everyday_help", "Errands", Icons.Default.ShoppingBag),
                BrowseSubcategory("fitness", "Fitness", Icons.Default.FitnessCenter),
                BrowseSubcategory("learning", "Learning", Icons.Default.MenuBook),
            )
        }
}

data class BrowseSubcategory(
    val id: String,
    val title: String,
    val icon: ImageVector,
)

@Composable
fun BrowseByTypeSection(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState,
    onSubcategoryClick: (HomeBrowseType, BrowseSubcategory) -> Unit = { _, _ -> },
    onPropertyClick: (String) -> Unit = {},
    onViewAllClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(HomeBrowseType.SPACES) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Browse by Type",
                style = PaceDreamTypography.Title2,
                color = PaceDreamTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Explore spaces, items, and services near you",
                style = PaceDreamTypography.Footnote,
                color = PaceDreamTextSecondary,
            )
        }

        // Segmented pill selector
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.XL))
                .background(PaceDreamGray100)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            HomeBrowseType.entries.forEach { type ->
                BrowseTypePill(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { selectedType = type },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Type description
        Text(
            text = selectedType.subtitle,
            style = PaceDreamTypography.Caption,
            color = PaceDreamTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        // Subcategory chips
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            selectedType.subcategories.forEach { sub ->
                SubcategoryChip(
                    subcategory = sub,
                    accentColor = selectedType.gradientColors.first(),
                    onClick = { onSubcategoryClick(selectedType, sub) },
                )
            }
        }

        // Inline listings preview
        val isLoading = when (selectedType) {
            HomeBrowseType.SPACES -> roomsState.loading
            HomeBrowseType.ITEMS -> gearsState.loading
            HomeBrowseType.SERVICES -> splitStaysState.loading
        }

        val hasContent = when (selectedType) {
            HomeBrowseType.SPACES -> roomsState.rooms.isNotEmpty()
            HomeBrowseType.ITEMS -> gearsState.rentedGears.isNotEmpty()
            HomeBrowseType.SERVICES -> splitStaysState.splitStays.isNotEmpty()
        }

        if (isLoading || hasContent) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // View All header
                if (hasContent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedType.displayTitle,
                            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                            color = PaceDreamTextPrimary,
                        )
                        TextButton(
                            onClick = {
                                onViewAllClick(
                                    when (selectedType) {
                                        HomeBrowseType.SPACES -> "time-based"
                                        HomeBrowseType.ITEMS -> "gear"
                                        HomeBrowseType.SERVICES -> "services"
                                    }
                                )
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "View All",
                                    style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.Medium),
                                    color = selectedType.gradientColors.first(),
                                )
                                Icon(
                                    imageVector = PaceDreamIcons.ChevronRight,
                                    contentDescription = null,
                                    tint = selectedType.gradientColors.first(),
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }

                // Listing cards
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (isLoading) {
                        items(3) {
                            BrowseInlineSkeletonCard()
                        }
                    } else {
                        when (selectedType) {
                            HomeBrowseType.SPACES -> {
                                items(roomsState.rooms.take(6), key = { it.id }) { room ->
                                    BrowseInlineRoomCard(
                                        room = room,
                                        accentColor = selectedType.gradientColors.first(),
                                        onClick = { onPropertyClick(room.id) },
                                    )
                                }
                            }
                            HomeBrowseType.ITEMS -> {
                                items(gearsState.rentedGears.take(6), key = { it.id }) { gear ->
                                    BrowseInlineGearCard(
                                        gear = gear,
                                        accentColor = selectedType.gradientColors.first(),
                                        onClick = { onPropertyClick(gear.id) },
                                    )
                                }
                            }
                            HomeBrowseType.SERVICES -> {
                                items(
                                    splitStaysState.splitStays.take(6),
                                    key = { it._id ?: it.hashCode() }
                                ) { stay ->
                                    BrowseInlineServiceCard(
                                        stay = stay,
                                        accentColor = selectedType.gradientColors.first(),
                                        onClick = { onPropertyClick(stay._id ?: "") },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----- Browse Type Pill -----

@Composable
private fun BrowseTypePill(
    type: HomeBrowseType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) type.gradientColors.first() else Color.Transparent,
        animationSpec = tween(200),
        label = "pillBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else PaceDreamTextSecondary,
        animationSpec = tween(200),
        label = "pillText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .background(
                if (isSelected) Brush.horizontalGradient(type.gradientColors) else Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Transparent)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = type.displayTitle,
                style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
            )
        }
    }
}

// ----- Subcategory Chip -----

@Composable
private fun SubcategoryChip(
    subcategory: BrowseSubcategory,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = PaceDreamSurface,
        shape = RoundedCornerShape(PaceDreamRadius.XL),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(PaceDreamGray200, PaceDreamGray200))
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = subcategory.icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = subcategory.title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Medium),
                color = PaceDreamTextPrimary,
            )
        }
    }
}

// ----- Inline Cards -----

@Composable
private fun BrowseInlineRoomCard(
    room: RoomModel,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = room.gallery.thumbnail,
            contentDescription = room.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = room.title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = room.location.city,
                style = PaceDreamTypography.Caption2,
                color = PaceDreamTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$${room.price?.firstOrNull()?.amount ?: 0}/hr",
                    style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                )
                if (room.rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = null,
                            tint = PaceDreamColors.StarRating,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = String.format("%.1f", room.rating.toDouble()),
                            style = PaceDreamTypography.Caption2,
                            color = PaceDreamTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseInlineGearCard(
    gear: RentedGearModel,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = gear.images?.firstOrNull(),
            contentDescription = gear.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = gear.name,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = gear.location,
                style = PaceDreamTypography.Caption2,
                color = PaceDreamTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$${gear.hourlyRate}/hr",
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
            )
        }
    }
}

@Composable
private fun BrowseInlineServiceCard(
    stay: SplitStayModel,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = stay.images?.firstOrNull(),
            contentDescription = stay.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stay.name ?: "Service",
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stay.location ?: stay.city ?: "Location",
                style = PaceDreamTypography.Caption2,
                color = PaceDreamTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$${stay.price ?: "0"} total",
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
            )
        }
    }
}

@Composable
private fun BrowseInlineSkeletonCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                .background(PaceDreamGray100)
        )
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PaceDreamGray100)
        )
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PaceDreamGray100)
        )
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PaceDreamGray100)
        )
    }
}
