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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
 * Explore by Category — the primary interactive browser for the marketplace.
 *
 * Segmented pill selector (Spaces / Items / Services), subcategory chips,
 * and inline listing preview cards.  This is the single, authoritative place
 * users go to drill into a specific marketplace pillar.
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
        gradientColors = listOf(PaceDreamColors.Primary, PaceDreamColors.Primary.copy(alpha = 0.7f)),
    ),
    ITEMS(
        displayTitle = "Items",
        subtitle = "Borrow useful things on demand",
        icon = Icons.Default.Inventory2,
        gradientColors = listOf(PaceDreamColors.Info, PaceDreamColors.Info.copy(alpha = 0.7f)),
    ),
    SERVICES(
        displayTitle = "Services",
        subtitle = "Find help for everyday needs",
        icon = Icons.Default.CleaningServices,
        gradientColors = listOf(PaceDreamColors.Success, PaceDreamColors.Success.copy(alpha = 0.7f)),
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section header — uses the shared SectionHeader component
        SectionHeader(
            title = "Explore by Category",
            subtitle = "Browse stays, gear, spaces, and local help nearby",
            helperText = "Available today",
        )

        // Segmented pill selector
        Row(
            modifier = Modifier
                .padding(horizontal = PaceDreamSpacing.LG)
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.XL))
                .background(PaceDreamGray100)
                .padding(PaceDreamSpacing.XS),
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

        // Subcategory chips (description merged into chips row)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = PaceDreamSpacing.LG),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // View All sub-header
                if (hasContent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.LG),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedType.displayTitle,
                            style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
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
                            },
                            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "View All",
                                    style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Medium),
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

                // Cache the .take(6) slices so that the LazyRow's key
                // function operates on a stable List instance across
                // recompositions instead of a fresh allocation each time.
                val browseRooms = remember(roomsState.rooms) { roomsState.rooms.take(6) }
                val browseGears = remember(gearsState.rentedGears) { gearsState.rentedGears.take(6) }
                val browseServices = remember(splitStaysState.splitStays) {
                    splitStaysState.splitStays.take(6)
                }

                // Listing cards — unified card width and structure
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isLoading) {
                        items(3) {
                            BrowseInlineSkeletonCard()
                        }
                    } else {
                        when (selectedType) {
                            HomeBrowseType.SPACES -> {
                                items(browseRooms, key = { it.id }) { room ->
                                    UnifiedListingCard(
                                        title = room.title,
                                        subtitle = room.location.city,
                                        price = "$${room.price?.firstOrNull()?.amount ?: 0}/hr",
                                        rating = room.rating.toDouble(),
                                        imageUrl = room.gallery.thumbnail,
                                        accentColor = selectedType.gradientColors.first(),
                                        onClick = { onPropertyClick(room.id) },
                                    )
                                }
                            }
                            HomeBrowseType.ITEMS -> {
                                items(browseGears, key = { it.id }) { gear ->
                                    UnifiedListingCard(
                                        title = gear.name,
                                        subtitle = gear.location,
                                        price = "$${gear.hourlyRate}/hr",
                                        imageUrl = gear.images?.firstOrNull(),
                                        accentColor = selectedType.gradientColors.first(),
                                        onClick = { onPropertyClick(gear.id) },
                                    )
                                }
                            }
                            HomeBrowseType.SERVICES -> {
                                // Stable key: fall back to the index when the
                                // backend omits _id so we never use a
                                // content-derived hashCode for identity.
                                itemsIndexed(
                                    browseServices,
                                    key = { idx, stay -> stay._id ?: "browse-split-idx-$idx" }
                                ) { _, stay ->
                                    // Memoize the formatted price per item to
                                    // avoid re-running the when-chain on every
                                    // recomposition triggered by sibling state.
                                    val priceText = remember(stay.priceUnit, stay.price) {
                                        "$${stay.price?.toInt() ?: "0"}/${normalizeSplitStayPriceUnit(stay.priceUnit)}"
                                    }
                                    UnifiedListingCard(
                                        title = stay.name ?: "Service",
                                        subtitle = stay.location ?: stay.city ?: "Location",
                                        price = priceText,
                                        rating = stay.rating?.toDouble(),
                                        imageUrl = stay.images?.firstOrNull(),
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

// ────────────────────────────────────────────────────────────────────────────
// Unified Listing Card
// ────────────────────────────────────────────────────────────────────────────

/**
 * Consistent inline listing card used across all Browse by Type listings.
 * Fixed width, fixed image height, consistent text layout, optional rating.
 */
@Composable
private fun UnifiedListingCard(
    title: String,
    subtitle: String,
    price: String,
    rating: Double? = null,
    imageUrl: String?,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    // Memoize the Coil request per URL so we don't reallocate the builder
    // whenever a sibling card in the LazyRow recomposes.
    val cardImageRequest = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(200)
            // 156x108dp thumbnail — cap decode so we never sample a full-res
            // CDN photo into the small browse-by-type tile.
            .size(coil.size.Size(480, 340))
            .build()
    }
    Column(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Image — fixed aspect ratio with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                .background(PaceDreamGray100),
        ) {
            AsyncImage(
                model = cardImageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Text content — consistent spacing and line clamping
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamTextPrimary,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
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
                    text = price,
                    style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                )
                if (rating != null && rating > 0) {
                    val formattedRating = remember(rating) { String.format("%.1f", rating) }
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
                            text = formattedRating,
                            style = PaceDreamTypography.Caption2,
                            color = PaceDreamTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Browse Type Pill
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrowseTypePill(
    type: HomeBrowseType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) OnBrandSurface else PaceDreamTextSecondary,
        animationSpec = tween(200),
        label = "pillText"
    )

    // Memoize both brush variants per type — the gradient colours are static
    // per enum entry, so the only thing that flips is which one we hand to
    // .background().  This avoids reallocating two Brushes on every tap and
    // on every recomposition triggered by the row's animateColorAsState.
    val selectedBrush = remember(type) { Brush.horizontalGradient(type.gradientColors) }
    val unselectedBrush = remember { Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)) }
    val pillInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .background(if (isSelected) selectedBrush else unselectedBrush)
            .clickable(
                interactionSource = pillInteractionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = PaceDreamSpacing.SM2),
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

// ────────────────────────────────────────────────────────────────────────────
// Subcategory Chip
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubcategoryChip(
    subcategory: BrowseSubcategory,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val chipBorderBrush = remember { Brush.horizontalGradient(listOf(PaceDreamGray200, PaceDreamGray200)) }
    val chipBorder = ButtonDefaults.outlinedButtonBorder.copy(brush = chipBorderBrush)
    Surface(
        onClick = onClick,
        color = PaceDreamSurface,
        shape = RoundedCornerShape(PaceDreamRadius.XL),
        border = chipBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM2, vertical = PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = subcategory.icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = subcategory.title,
                style = PaceDreamTypography.Caption2.copy(fontWeight = FontWeight.Medium),
                color = PaceDreamTextPrimary,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Skeleton Card
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrowseInlineSkeletonCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(156.dp)
                .height(108.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                .background(PaceDreamGray100)
        )
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.XS))
                .background(PaceDreamGray100)
        )
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(9.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.XS))
                .background(PaceDreamGray100)
        )
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.XS))
                .background(PaceDreamGray100)
        )
    }
}
