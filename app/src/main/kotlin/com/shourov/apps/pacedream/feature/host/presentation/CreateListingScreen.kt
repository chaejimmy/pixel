package com.shourov.apps.pacedream.feature.host.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.feature.host.data.AvailabilityPayload
import com.shourov.apps.pacedream.feature.host.data.CreateListingDraftStore
import com.shourov.apps.pacedream.feature.host.data.CreateListingRequest
import com.shourov.apps.pacedream.feature.host.data.ImageUploadService
import com.shourov.apps.pacedream.feature.host.data.ListingDraftData
import com.shourov.apps.pacedream.feature.host.data.LocationPayload
import com.shourov.apps.pacedream.feature.host.data.PricingPayload
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.model.PricingUnit
import kotlinx.coroutines.launch
import java.util.TimeZone

/**
 * Listing type matching iOS: share | borrow | split.
 * iOS: ListingType enum { share, borrow, split }
 */
enum class ListingMode(val displayName: String, val backendValue: String) {
    SHARE("Share", "share"),
    BORROW("Book", "borrow"),
    SPLIT("Split", "split"),
}

/**
 * Resource kind — the top-level selector in Create Listing.
 * Users choose what kind of thing they're listing (supply side),
 * not a consumer action like Share/Book/Split.
 */
enum class ResourceKind(
    val label: String,
    val subtitle: String,
    val listingMode: ListingMode,
) {
    SPACES("Spaces", "List a space people can book by time or stay", ListingMode.SHARE),
    ITEMS("Items", "List an item people can rent or reserve", ListingMode.BORROW),
    SERVICES("Services", "List a service people can book", ListingMode.SHARE),
}

/**
 * Subcategory item matching iOS ListingSubcategoryPickerView.SubcategoryItem.
 */
private data class SubcategoryItem(
    val id: String,
    val value: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val needsSchedule: Boolean = false,
)

/**
 * Subcategories per resource kind.
 * Each resource kind shows only its own categories.
 */
private fun getSubcategoriesByResourceKind(resourceKind: ResourceKind): List<SubcategoryItem> = when (resourceKind) {
    ResourceKind.SPACES -> SPACE_SUBCATEGORIES
    ResourceKind.ITEMS -> ITEM_SUBCATEGORIES
    ResourceKind.SERVICES -> SERVICE_SUBCATEGORIES
}


private val SPACE_SUBCATEGORIES = listOf(
    SubcategoryItem("restroom", "restroom", "Restroom", "Quick, clean access", PaceDreamIcons.Home, needsSchedule = true),
    SubcategoryItem("nap_pod", "nap_pod", "Nap pod", "Recharge in privacy", PaceDreamIcons.Home, needsSchedule = true),
    SubcategoryItem("meeting_room", "meeting_room", "Meeting room", "Private meetings", PaceDreamIcons.Group, needsSchedule = true),
    SubcategoryItem("gym", "gym", "Gym", "Fitness access nearby", PaceDreamIcons.FitnessCenter, needsSchedule = true),
    SubcategoryItem("short_stay", "short_stay", "Short stay", "A few hours", PaceDreamIcons.Schedule, needsSchedule = false),
    SubcategoryItem("wifi", "wifi", "WIFI", "Share internet access", PaceDreamIcons.Wifi, needsSchedule = false),
    SubcategoryItem("parking", "parking", "Parking", "Rent your spot", PaceDreamIcons.LocalParking, needsSchedule = false),
    SubcategoryItem("storage_space", "storage_space", "Storage Space", "Secure extra space", PaceDreamIcons.Storage, needsSchedule = false),
    SubcategoryItem("others", "others", "Others", "Everything else", PaceDreamIcons.MoreHoriz, needsSchedule = false),
)

private val ITEM_SUBCATEGORIES = listOf(
    SubcategoryItem("sports_gear", "sports_gear", "Sports gear", "Boards, bikes, more", PaceDreamIcons.DirectionsBike, needsSchedule = true),
    SubcategoryItem("camera", "camera", "Camera", "Capture the moment", PaceDreamIcons.CameraAlt, needsSchedule = true),
    SubcategoryItem("tech", "tech", "Tech", "Laptops, gadgets", PaceDreamIcons.Laptop, needsSchedule = true),
    SubcategoryItem("instrument", "instrument", "Instrument", "Music gear", PaceDreamIcons.Category, needsSchedule = true),
    SubcategoryItem("tools", "tools", "Tools", "Power & hand tools", PaceDreamIcons.Build, needsSchedule = true),
    SubcategoryItem("games", "games", "Games", "Board & video games", PaceDreamIcons.SportsEsports, needsSchedule = true),
    SubcategoryItem("toys", "toys", "Toys", "Fun for everyone", PaceDreamIcons.SmartToy, needsSchedule = true),
    SubcategoryItem("micromobility", "micromobility", "Micromobility", "Scooters, e-bikes", PaceDreamIcons.DirectionsBike, needsSchedule = true),
    SubcategoryItem("others", "others", "Others", "Everything else", PaceDreamIcons.MoreHoriz, needsSchedule = true),
)

private val SERVICE_SUBCATEGORIES = listOf(
    SubcategoryItem("home_help", "home_help", "Home Help", "Handy help at home", PaceDreamIcons.Home, needsSchedule = false),
    SubcategoryItem("moving_help", "moving_help", "Moving Help", "Get help moving", PaceDreamIcons.Storage, needsSchedule = false),
    SubcategoryItem("cleaning_organizing", "cleaning_organizing", "Cleaning & Organizing", "Tidy up your space", PaceDreamIcons.LocalLaundryService, needsSchedule = false),
    SubcategoryItem("everyday_help", "everyday_help", "Everyday Help", "Errands and tasks", PaceDreamIcons.FavoriteBorder, needsSchedule = false),
    SubcategoryItem("fitness", "fitness", "Fitness", "Training sessions", PaceDreamIcons.FitnessCenter, needsSchedule = false),
    SubcategoryItem("learning", "learning", "Learning", "Lessons and tutoring", PaceDreamIcons.School, needsSchedule = false),
    SubcategoryItem("creative", "creative", "Creative", "Art, music, design", PaceDreamIcons.Category, needsSchedule = false),
    SubcategoryItem("others", "others", "Others", "Everything else", PaceDreamIcons.MoreHoriz, needsSchedule = false),
)

private val SERVICE_IDS = SERVICE_SUBCATEGORIES.map { it.value }.toSet()
private val SPACE_IDS = SPACE_SUBCATEGORIES.map { it.value }.toSet()
private val ITEM_IDS = ITEM_SUBCATEGORIES.map { it.value }.toSet()

/** Human-readable resource type label for preview/review. */
private fun resolveResourceTypeLabel(subCategory: String): String {
    val sc = subCategory.lowercase()
    return when {
        sc in SERVICE_IDS -> "Service"
        sc in SPACE_IDS -> "Space"
        sc in ITEM_IDS -> "Item"
        else -> "\u2014"
    }
}

/**
 * Allowed pricing units per subcategory – iOS parity.
 * iOS: PricingUnit.allowedUnits(for:listingType:)
 */
private fun getAllowedPricingUnits(
    listingMode: ListingMode,
    subCategory: String,
): List<PricingUnit> {
    val sc = subCategory.lowercase()
    return when (listingMode) {
        ListingMode.SHARE -> when {
            sc in SERVICE_IDS -> listOf(PricingUnit.HOUR)
            sc in listOf("restroom", "nap_pod") -> listOf(PricingUnit.HOUR)
            sc in listOf("meeting_room", "gym", "parking") -> listOf(PricingUnit.HOUR, PricingUnit.DAY)
            sc in listOf("short_stay", "luxury_room") -> listOf(PricingUnit.DAY, PricingUnit.WEEK)
            sc == "apartment" -> listOf(PricingUnit.DAY, PricingUnit.WEEK, PricingUnit.MONTH)
            sc == "storage_space" -> listOf(PricingUnit.DAY, PricingUnit.WEEK, PricingUnit.MONTH)
            sc == "wifi" -> listOf(PricingUnit.HOUR, PricingUnit.DAY)
            else -> listOf(PricingUnit.HOUR, PricingUnit.DAY)
        }
        ListingMode.BORROW -> when {
            sc in SERVICE_IDS -> listOf(PricingUnit.HOUR)
            sc in listOf("sports_gear", "camera", "vehicle", "tech", "instrument",
                "tools", "games", "toys", "micromobility", "others") ->
                listOf(PricingUnit.DAY, PricingUnit.WEEK)
            else -> listOf(PricingUnit.DAY, PricingUnit.WEEK)
        }
        ListingMode.SPLIT -> when (sc) {
            "subscription", "sports", "membership", "events" -> listOf(PricingUnit.MONTH)
            "wifi" -> listOf(PricingUnit.DAY, PricingUnit.MONTH)
            else -> listOf(PricingUnit.MONTH)
        }
    }
}

/**
 * Whether a subcategory requires the schedule/availability step – iOS parity.
 * iOS: ListingDraft.needsSchedule
 */
private fun needsSchedule(listingMode: ListingMode, subCategory: String): Boolean {
    val sc = subCategory.lowercase()
    // Services never need the schedule step
    if (sc in SERVICE_IDS) return false
    return when (listingMode) {
        ListingMode.SHARE -> sc in listOf("restroom", "nap_pod", "meeting_room", "gym")
        ListingMode.BORROW -> sc in listOf("camera", "sports_gear", "tech", "instrument", "tools", "games", "toys", "micromobility", "others")
        ListingMode.SPLIT -> false
    }
}

// Duration options matching iOS (minutes)
private val DURATION_OPTIONS_MINUTES = listOf(15, 30, 60, 90, 120)

private val DAY_LABELS = listOf(
    "Sun" to 0, "Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6
)

/**
 * iOS-parity Create Listing screen.
 * Flow: Entry (Spaces/Items/Services) → Subcategory picker → Wizard → Success
 *
 * iOS flow:
 *   CreateListingEntryView → ListingSubcategoryPickerView → CreateListingWizardView
 *
 * Wizard steps (iOS: CreateListingFlowCoordinator.Step):
 *   1. Basics (title, description)
 *   2. Photos · Location · Pricing
 *   3. Schedule & Availability (if needsSchedule)
 *   4. Review & Publish
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    listingMode: ListingMode = ListingMode.SHARE,
    imageUploadService: ImageUploadService? = null,
    onBackClick: () -> Unit = {},
    onPublishSuccess: (String) -> Unit = {},
    onPublishListing: (CreateListingRequest) -> Unit = {},
    viewModel: CreateListingViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    // iOS parity: draft persistence
    val context = LocalContext.current
    val draftStore = remember { CreateListingDraftStore(context) }

    // Phase: entry → subcategory → wizard → success
    var phase by remember { mutableStateOf("entry") }
    var selectedMode by remember { mutableStateOf(listingMode) }
    var selectedResourceKind by remember { mutableStateOf(ResourceKind.SPACES) }
    var selectedSubCategory by remember { mutableStateOf("") }
    var publishedTitle by remember { mutableStateOf("") }
    var publishedListingId by remember { mutableStateOf("") }
    var publishedCoverUrl by remember { mutableStateOf<String?>(null) }
    var publishError by remember { mutableStateOf<String?>(null) }

    // Collect ViewModel effects for publish result
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateListingViewModel.Effect.PublishSuccess -> {
                    publishedTitle = effect.title
                    publishedListingId = effect.listingId
                    publishedCoverUrl = effect.coverUrl
                    draftStore.clear() // iOS parity: clear draft on success
                    phase = "success"
                    onPublishSuccess(effect.listingId)
                }
                is CreateListingViewModel.Effect.PublishError -> {
                    publishError = effect.message
                }
            }
        }
    }

    when (phase) {
        "entry" -> CreateListingEntryScreen(
            onResourceKindSelected = { kind ->
                selectedResourceKind = kind
                selectedMode = kind.listingMode
                phase = "subcategory"
            },
            onBackClick = onBackClick,
            draftStore = draftStore,
            onResumeDraft = { draft ->
                // Resume from saved draft: jump directly to wizard with saved state
                selectedMode = ListingMode.entries.firstOrNull { it.backendValue == draft.listingMode } ?: ListingMode.SHARE
                selectedResourceKind = ResourceKind.entries.firstOrNull { it.name.equals(draft.resourceKind, ignoreCase = true) } ?: ResourceKind.SPACES
                selectedSubCategory = draft.subCategory
                phase = if (draft.subCategory.isNotBlank()) "wizard" else "subcategory"
            },
        )
        "subcategory" -> SubcategoryPickerScreen(
            resourceKind = selectedResourceKind,
            listingMode = selectedMode,
            onSubcategorySelected = { sub ->
                selectedSubCategory = sub
                phase = "wizard"
            },
            onBackClick = { phase = "entry" },
        )
        "wizard" -> CreateListingWizardScreen(
            listingMode = selectedMode,
            subCategory = selectedSubCategory,
            imageUploadService = imageUploadService,
            draftStore = draftStore,
            onBackClick = { phase = "subcategory" },
            onPublishListing = { request ->
                publishError = null
                viewModel.publishListing(request)
            },
        )
        "success" -> PublishSuccessScreen(
            listingId = publishedListingId,
            title = publishedTitle,
            coverUrl = publishedCoverUrl,
            onViewListing = { /* TODO: navigate to listing detail */ },
            onGoToMyListings = onBackClick,
            onBackToHome = onBackClick,
        )
    }
}

// ── Phase 1: Entry (Spaces / Items / Services) ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateListingEntryScreen(
    onResourceKindSelected: (ResourceKind) -> Unit,
    onBackClick: () -> Unit,
    draftStore: CreateListingDraftStore? = null,
    onResumeDraft: ((ListingDraftData) -> Unit)? = null,
) {
    // iOS parity: load saved draft on appear and show resume banner
    var savedDraft by remember { mutableStateOf<ListingDraftData?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        savedDraft = draftStore?.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create a Listing", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background),
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PaceDreamSpacing.LG)
                .padding(top = PaceDreamSpacing.SM, bottom = PaceDreamSpacing.XL),
        ) {
            Text(
                text = "Create a Listing",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "What are you listing?",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
            )

            // iOS parity: "Continue your draft" resume banner
            savedDraft?.let { draft ->
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                DraftResumeBanner(
                    draftTitle = draft.title.ifBlank { "Untitled listing" },
                    onResume = { onResumeDraft?.invoke(draft) },
                    onDiscard = { showDiscardConfirm = true },
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            ModeCard(
                title = "Spaces",
                subtitle = "List a space people can book by time or stay",
                icon = PaceDreamIcons.Home,
                tint = PaceDreamColors.Primary,
                onClick = { onResourceKindSelected(ResourceKind.SPACES) },
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            ModeCard(
                title = "Items",
                subtitle = "List an item people can rent or reserve",
                icon = PaceDreamIcons.DirectionsBike,
                tint = Color(0xFF2196F3),
                onClick = { onResourceKindSelected(ResourceKind.ITEMS) },
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            ModeCard(
                title = "Services",
                subtitle = "List a service people can book",
                icon = PaceDreamIcons.Build,
                tint = Color(0xFF4CAF50),
                onClick = { onResourceKindSelected(ResourceKind.SERVICES) },
            )
        }
    }

    // Discard draft confirmation dialog (iOS parity)
    if (showDiscardConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Draft?") },
            text = { Text("This draft will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    draftStore?.clear()
                    savedDraft = null
                    showDiscardConfirm = false
                }) {
                    Text("Discard", color = PaceDreamColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Draft")
                }
            },
        )
    }
}

/**
 * iOS parity: orange "Continue your draft" banner with discard option.
 */
@Composable
private fun DraftResumeBanner(
    draftTitle: String,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column {
        // Resume row (orange background, top corners rounded)
        Card(
            onClick = onResume,
            shape = RoundedCornerShape(
                topStart = PaceDreamRadius.LG,
                topEnd = PaceDreamRadius.LG,
                bottomStart = 0.dp,
                bottomEnd = 0.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA500)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    PaceDreamIcons.History,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Continue your draft",
                        style = PaceDreamTypography.Callout,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        draftTitle,
                        style = PaceDreamTypography.Caption,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                    )
                }
                Icon(
                    PaceDreamIcons.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        // Discard row (red tint, bottom corners rounded)
        Card(
            onClick = onDiscard,
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = PaceDreamRadius.LG,
                bottomEnd = PaceDreamRadius.LG,
            ),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Error.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    PaceDreamIcons.Delete,
                    contentDescription = null,
                    tint = PaceDreamColors.Error,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Discard Draft",
                    style = PaceDreamTypography.Caption,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.Error,
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(PaceDreamIconSize.MD),
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                )
            }
            Icon(
                PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(PaceDreamIconSize.SM),
            )
        }
    }
}

// ── Phase 2: Subcategory Picker – iOS: ListingSubcategoryPickerView ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubcategoryPickerScreen(
    resourceKind: ResourceKind,
    listingMode: ListingMode,
    onSubcategorySelected: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val subcategories = getSubcategoriesByResourceKind(resourceKind)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resourceKind.label, style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background),
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PaceDreamSpacing.LG),
        ) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "Choose a subcategory",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                contentPadding = PaddingValues(bottom = PaceDreamSpacing.XL),
            ) {
                items(subcategories, key = { it.id }) { item ->
                    SubcategoryCard(
                        item = item,
                        onClick = { onSubcategorySelected(item.value) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubcategoryCard(
    item: SubcategoryItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.SM)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    .background(PaceDreamColors.Primary.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(PaceDreamIconSize.MD),
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = item.title,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = item.subtitle,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                maxLines = 2,
            )
            if (item.needsSchedule) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = "Schedule required",
                    style = PaceDreamTypography.Caption2,
                    color = PaceDreamColors.Info,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(PaceDreamRadius.Round))
                        .background(PaceDreamColors.Info.copy(alpha = 0.1f))
                        .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
                )
            }
        }
    }
}

// ── Phase 3: Create Listing Wizard – iOS: CreateListingWizardView ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateListingWizardScreen(
    listingMode: ListingMode,
    subCategory: String,
    imageUploadService: ImageUploadService? = null,
    draftStore: CreateListingDraftStore? = null,
    onBackClick: () -> Unit,
    onPublishListing: (CreateListingRequest) -> Unit,
) {
    val hasSchedule = needsSchedule(listingMode, subCategory)
    val steps = if (hasSchedule) {
        listOf("Basics", "Photos \u00b7 Location \u00b7 Pricing", "Schedule & Availability", "Review & Publish")
    } else {
        listOf("Basics", "Photos \u00b7 Location \u00b7 Pricing", "Review & Publish")
    }
    val totalSteps = steps.size
    var currentStep by remember { mutableIntStateOf(0) }

    // Form state – iOS parity (ListingDraft fields)
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Split-specific
    var deadlineAt by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf("") }

    // Photos – iOS parity: selected image URIs + uploaded Cloudinary URLs
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    val uploadedImageUrls = remember { mutableStateListOf<String>() }
    var isUploadingImages by remember { mutableStateOf(false) }

    // Photos/Location/Pricing
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var basePrice by remember { mutableStateOf("") }
    val amenities = remember { mutableStateListOf<String>() }

    // Pricing unit – iOS parity
    val allowedUnits = getAllowedPricingUnits(listingMode, subCategory)
    var selectedPricingUnit by remember { mutableStateOf(allowedUnits.firstOrNull() ?: PricingUnit.HOUR) }

    // Schedule/Availability – use device timezone (iOS parity: TimeZone.current.identifier)
    val selectedDurations = remember { mutableStateListOf<Int>() }
    val selectedDays = remember { mutableStateListOf<Int>() }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var timezone by remember { mutableStateOf(TimeZone.getDefault().id) }

    // Daily-mode fields
    var minStay by remember { mutableIntStateOf(1) }
    var maxStay by remember { mutableIntStateOf(7) }
    var checkinTime by remember { mutableStateOf("15:00") }
    var checkoutTime by remember { mutableStateOf("11:00") }

    // Monthly-mode fields
    var minMonths by remember { mutableIntStateOf(1) }
    var availableFrom by remember { mutableStateOf("") }

    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }
    var isUploadingOverlay by remember { mutableStateOf(false) }
    var uploadProgressText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val progress = (currentStep + 1).toFloat() / totalSteps

    // iOS parity: validate each step.
    // iOS does NOT enforce a description minimum length — only title is required for basics.
    fun validateStep(step: Int): String? {
        return when (step) {
            0 -> {
                if (title.isBlank()) return "Title is required."
                null
            }
            1 -> {
                val price = if (listingMode == ListingMode.SPLIT) {
                    totalCost.toDoubleOrNull() ?: 0.0
                } else {
                    basePrice.toDoubleOrNull() ?: 0.0
                }
                if (price <= 0) return "Price must be greater than 0."
                // iOS parity: require at least 1 photo for non-split listings
                if (listingMode != ListingMode.SPLIT && selectedImageUris.isEmpty() && uploadedImageUrls.isEmpty()) {
                    return "Add at least 1 photo."
                }
                if (listingMode != ListingMode.SPLIT) {
                    if (address.isBlank()) return "Address is required."
                    if (city.isBlank() || state.isBlank()) return "City and state are required."
                }
                null
            }
            2 -> {
                if (hasSchedule) {
                    when (selectedPricingUnit) {
                        PricingUnit.HOUR -> {
                            if (selectedDurations.isEmpty()) return "Select at least 1 duration."
                            if (selectedDays.isEmpty()) return "Select at least 1 available day."
                        }
                        PricingUnit.DAY, PricingUnit.WEEK -> {
                            if (minStay < 1) return "Minimum stay must be at least 1 day."
                            if (maxStay < minStay) return "Maximum stay must be at least the minimum stay."
                            if (selectedDays.isEmpty()) return "Select at least 1 available day."
                        }
                        PricingUnit.MONTH -> {
                            if (minMonths < 1) return "Minimum months must be at least 1."
                        }
                    }
                }
                null
            }
            else -> null
        }
    }

    // iOS parity: reviewPublish step re-validates ALL prior steps before allowing publish.
    fun validate(): String? {
        if (currentStep == totalSteps - 1) {
            // Final step: cross-validate all previous steps
            for (s in 0 until totalSteps - 1) {
                val msg = validateStep(s)
                if (msg != null) return msg
            }
            return null
        }
        return validateStep(currentStep)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = steps[currentStep], style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onBackClick()
                    }) {
                        Icon(
                            imageVector = if (currentStep > 0) PaceDreamIcons.ArrowBack else PaceDreamIcons.Close,
                            contentDescription = if (currentStep > 0) "Back" else "Close",
                        )
                    }
                },
                actions = {
                    Text(
                        text = "Step ${currentStep + 1} of $totalSteps",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        modifier = Modifier.padding(end = PaceDreamSpacing.MD),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background),
            )
        },
        bottomBar = {
            WizardBottomBar(
                currentStep = currentStep,
                totalSteps = totalSteps,
                validationMessage = validationMessage,
                onBack = {
                    validationMessage = null
                    if (currentStep > 0) currentStep--
                },
                onNext = {
                    validationMessage = null
                    val msg = validate()
                    if (msg != null) {
                        validationMessage = msg
                        return@WizardBottomBar
                    }
                    if (currentStep < totalSteps - 1) {
                        // iOS parity: auto-save draft when navigating between steps
                        draftStore?.save(ListingDraftData(
                            listingMode = listingMode.backendValue,
                            resourceKind = "",
                            subCategory = subCategory,
                            title = title.trim(),
                            description = description.trim(),
                            address = address,
                            city = city,
                            state = state,
                            basePrice = basePrice,
                            totalCost = totalCost,
                            pricingUnit = selectedPricingUnit.value,
                            amenities = amenities.toList(),
                            deadlineAt = deadlineAt,
                            requirements = requirements,
                        ))
                        currentStep++
                    } else {
                        // Build payload matching iOS ListingsPublisherService
                        isPublishing = true
                        isUploadingOverlay = true
                        scope.launch {
                            // iOS parity: upload images to Cloudinary first, fallback to base64
                            val imageUrls = mutableListOf<String>()
                            imageUrls.addAll(uploadedImageUrls)

                            if (imageUploadService != null && selectedImageUris.isNotEmpty()) {
                                isUploadingImages = true
                                val totalImages = selectedImageUris.size
                                var uploadedCount = 0
                                for (uri in selectedImageUris) {
                                    if (uploadedImageUrls.any { it.contains(uri.lastPathSegment ?: "") }) {
                                        uploadedCount++
                                        continue
                                    }
                                    val pct = ((uploadedCount.toFloat() / totalImages) * 100).toInt()
                                    uploadProgressText = "Uploading photos\u2026 $pct%"
                                    when (val result = imageUploadService.uploadImage(context, uri)) {
                                        is ApiResult.Success -> imageUrls.add(result.data)
                                        is ApiResult.Failure -> {
                                            // iOS fallback: encode as base64 data URL
                                            try {
                                                val stream = context.contentResolver.openInputStream(uri)
                                                val bytes = stream?.readBytes()
                                                stream?.close()
                                                if (bytes != null) {
                                                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                                    imageUrls.add("data:image/jpeg;base64,$b64")
                                                }
                                            } catch (_: Exception) { }
                                        }
                                    }
                                    uploadedCount++
                                }
                                uploadProgressText = "Publishing\u2026"
                                isUploadingImages = false
                            } else {
                                uploadProgressText = "Publishing\u2026"
                            }

                            val resolvedPrice = if (listingMode == ListingMode.SPLIT) {
                                totalCost.toDoubleOrNull() ?: 0.0
                            } else {
                                basePrice.toDoubleOrNull() ?: 0.0
                            }

                            // iOS parity: summary is first 150 chars of description
                            val trimmedDesc = description.trim()
                            val summary = if (trimmedDesc.isNotBlank()) {
                                trimmedDesc.take(150)
                            } else null

                            val request = CreateListingRequest(
                                listing_type = listingMode.backendValue,
                                subCategory = subCategory,
                                title = title.trim(),
                                description = trimmedDesc.ifBlank { null },
                                summary = summary,
                                price = resolvedPrice,
                                pricing_type = selectedPricingUnit.backendPricingType,
                                pricing = PricingPayload(
                                    base_price = resolvedPrice,
                                    unit = selectedPricingUnit.value,
                                    pricing_type = selectedPricingUnit.backendPricingType,
                                    currency = "USD",
                                    frequency = selectedPricingUnit.backendFrequency,
                                ),
                                address = if (listingMode != ListingMode.SPLIT) address else null,
                                amenities = amenities.ifEmpty { null },
                                images = imageUrls.ifEmpty { null },
                                location = if (listingMode != ListingMode.SPLIT && city.isNotBlank()) {
                                    LocationPayload(lat = 0.0, lng = 0.0, city = city, state = state)
                                } else null,
                                durations = if (hasSchedule) selectedDurations.toList() else null,
                                availability = if (hasSchedule) AvailabilityPayload(
                                    start_time = startTime,
                                    end_time = endTime,
                                    available_days = selectedDays.toList(),
                                    timezone = timezone,
                                    instant_booking = false,
                                ) else null,
                                shareType = if (listingMode == ListingMode.SPLIT) "SPLIT" else null,
                                share_type = if (listingMode == ListingMode.SPLIT) "SPLIT" else null,
                                totalCost = totalCost.toDoubleOrNull(),
                                deadlineAt = deadlineAt.ifBlank { null },
                                requirements = requirements.ifBlank { null },
                            )
                            onPublishListing(request)
                            // ViewModel handles the API call and emits success/error via effects.
                            // isPublishing will be reset when the effect is received.
                            isPublishing = false
                            isUploadingOverlay = false
                        }
                    }
                },
                isPublishing = isPublishing,
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = PaceDreamColors.Primary,
                trackColor = PaceDreamColors.Border,
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "step_transition",
            ) { step ->
                when {
                    step == 0 -> BasicsStep(
                        listingMode = listingMode,
                        title = title,
                        description = description,
                        deadlineAt = deadlineAt,
                        requirements = requirements,
                        onTitleChange = { title = it },
                        onDescriptionChange = { description = it },
                        onDeadlineAtChange = { deadlineAt = it },
                        onRequirementsChange = { requirements = it },
                    )
                    step == 1 -> PhotosLocationPricingStep(
                        listingMode = listingMode,
                        subCategory = subCategory,
                        selectedImageUris = selectedImageUris,
                        onImagesSelected = { uris ->
                            val remaining = MAX_PHOTOS - selectedImageUris.size
                            selectedImageUris.addAll(uris.take(remaining))
                        },
                        onRemoveImage = { index ->
                            if (index in selectedImageUris.indices) selectedImageUris.removeAt(index)
                        },
                        address = address,
                        city = city,
                        state = state,
                        basePrice = basePrice,
                        totalCost = totalCost,
                        amenities = amenities,
                        selectedPricingUnit = selectedPricingUnit,
                        allowedUnits = allowedUnits,
                        onAddressChange = { address = it },
                        onCityChange = { city = it },
                        onStateChange = { state = it },
                        onBasePriceChange = { basePrice = it },
                        onTotalCostChange = { totalCost = it },
                        onToggleAmenity = { name ->
                            if (amenities.contains(name)) amenities.remove(name)
                            else amenities.add(name)
                        },
                        onPricingUnitChange = { selectedPricingUnit = it },
                    )
                    step == 2 && hasSchedule -> ScheduleAvailabilityStep(
                        pricingUnit = selectedPricingUnit,
                        selectedDurations = selectedDurations,
                        selectedDays = selectedDays,
                        startTime = startTime,
                        endTime = endTime,
                        timezone = timezone,
                        minStay = minStay,
                        maxStay = maxStay,
                        checkinTime = checkinTime,
                        checkoutTime = checkoutTime,
                        minMonths = minMonths,
                        availableFrom = availableFrom,
                        onToggleDuration = { min ->
                            if (selectedDurations.contains(min)) selectedDurations.remove(min)
                            else { selectedDurations.add(min); selectedDurations.sort() }
                        },
                        onToggleDay = { day ->
                            if (selectedDays.contains(day)) selectedDays.remove(day)
                            else { selectedDays.add(day); selectedDays.sort() }
                        },
                        onStartTimeChange = { startTime = it },
                        onEndTimeChange = { endTime = it },
                        onTimezoneChange = { timezone = it },
                        onMinStayChange = { minStay = it; if (maxStay < it) maxStay = it },
                        onMaxStayChange = { maxStay = it },
                        onCheckinTimeChange = { checkinTime = it },
                        onCheckoutTimeChange = { checkoutTime = it },
                        onMinMonthsChange = { minMonths = it },
                        onAvailableFromChange = { availableFrom = it },
                    )
                    else -> ReviewPublishStep(
                        listingMode = listingMode,
                        subCategory = subCategory,
                        title = title,
                        description = description,
                        address = address,
                        city = city,
                        state = state,
                        basePrice = basePrice,
                        totalCost = totalCost,
                        selectedPricingUnit = selectedPricingUnit,
                        hasSchedule = hasSchedule,
                        selectedDurations = selectedDurations,
                        selectedDays = selectedDays,
                        startTime = startTime,
                        endTime = endTime,
                        timezone = timezone,
                        minStay = minStay,
                        maxStay = maxStay,
                        checkinTime = checkinTime,
                        checkoutTime = checkoutTime,
                        minMonths = minMonths,
                        availableFrom = availableFrom,
                        selectedImageUris = selectedImageUris,
                        amenities = amenities,
                    )
                }
            }
        }

        // iOS parity: full-screen publish overlay with progress
        if (isUploadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                    Text(
                        text = uploadProgressText.ifBlank { "Publishing\u2026" },
                        style = PaceDreamTypography.Headline,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Text(
                        text = "If this takes too long, check your connection and try again.",
                        style = PaceDreamTypography.Caption,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.XXXL),
                    )
                }
            }
        }
    }
  }
}

// ── Step: Basics (iOS: ListingBasicsStepView) ──

@Composable
private fun BasicsStep(
    listingMode: ListingMode,
    title: String,
    description: String,
    deadlineAt: String,
    requirements: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDeadlineAtChange: (String) -> Unit,
    onRequirementsChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
    ) {
        FormSection(title = "Title") {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Give it a clear, searchable title", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        FormSection(title = "Description") {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Describe your listing", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
        }

        if (listingMode == ListingMode.SPLIT) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            FormSection(title = "Split details (optional)") {
                OutlinedTextField(
                    value = deadlineAt,
                    onValueChange = onDeadlineAtChange,
                    label = { Text("Deadline (optional)", style = PaceDreamTypography.Callout) },
                    placeholder = { Text("e.g. 2026-04-01", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
                OutlinedTextField(
                    value = requirements,
                    onValueChange = onRequirementsChange,
                    label = { Text("Requirements (optional)", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
            }
        }
    }
}

// ── Step: Photos · Location · Pricing (iOS: ListingPhotosLocationPricingStepView) ──

@Composable
private fun PhotosLocationPricingStep(
    listingMode: ListingMode,
    subCategory: String,
    selectedImageUris: List<Uri>,
    onImagesSelected: (List<Uri>) -> Unit,
    onRemoveImage: (Int) -> Unit,
    address: String,
    city: String,
    state: String,
    basePrice: String,
    totalCost: String,
    amenities: List<String>,
    selectedPricingUnit: PricingUnit,
    allowedUnits: List<PricingUnit>,
    onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onBasePriceChange: (String) -> Unit,
    onTotalCostChange: (String) -> Unit,
    onToggleAmenity: (String) -> Unit,
    onPricingUnitChange: (PricingUnit) -> Unit,
) {
    val isSplit = listingMode == ListingMode.SPLIT

    // iOS parity: image picker using ActivityResultContracts
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImagesSelected(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
    ) {
        // Photos – iOS parity: show selected images + add button with count badge
        FormSection(
            title = if (isSplit) "Photos (optional)"
                    else "Photos" + if (selectedImageUris.isNotEmpty()) " (${selectedImageUris.size}/$MAX_PHOTOS)" else "",
        ) {
            Text(
                text = if (isSplit) "Add photos to help describe your split."
                       else "Add at least 1 photo (up to $MAX_PHOTOS).",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                // Show selected image thumbnails
                selectedImageUris.forEachIndexed { index, uri ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Photo ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
                        )
                        // Remove button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onRemoveImage(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                PaceDreamIcons.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                // Add photo button (if under limit)
                if (selectedImageUris.size < MAX_PHOTOS) {
                    PhotoUploadPlaceholder(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        photoCount = selectedImageUris.size,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Location
        FormSection(title = if (isSplit) "Location (optional)" else "Location") {
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                label = { Text("Search address\u2026", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
                OutlinedTextField(
                    value = state,
                    onValueChange = onStateChange,
                    label = { Text("State", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Pricing
        FormSection(title = "Pricing") {
            if (isSplit) {
                OutlinedTextField(
                    value = totalCost,
                    onValueChange = { raw ->
                        val cleaned = raw.replace(Regex("[^0-9.]"), "")
                        val parts = cleaned.split(".")
                        val sanitized = if (parts.size > 1) "${parts[0]}.${parts[1].take(2)}" else cleaned
                        onTotalCostChange(sanitized)
                    },
                    label = { Text("Total cost", style = PaceDreamTypography.Callout) },
                    placeholder = { Text("0.00", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Text("$", style = PaceDreamTypography.Title3, color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            } else {
                if (allowedUnits.size > 1) {
                    PricingUnitSelector(
                        allowedUnits = allowedUnits,
                        selectedUnit = selectedPricingUnit,
                        onUnitSelected = onPricingUnitChange,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                } else if (allowedUnits.size == 1) {
                    Text(
                        text = "Pricing: ${allowedUnits.first().displayLabel}",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                }

                OutlinedTextField(
                    value = basePrice,
                    onValueChange = { raw ->
                        val cleaned = raw.replace(Regex("[^0-9.]"), "")
                        val parts = cleaned.split(".")
                        val sanitized = if (parts.size > 1) "${parts[0]}.${parts[1].take(2)}" else cleaned
                        onBasePriceChange(sanitized)
                    },
                    label = { Text("Price", style = PaceDreamTypography.Callout) },
                    placeholder = { Text("0.00", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Text("$", style = PaceDreamTypography.Title3, color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.SemiBold) },
                    trailingIcon = {
                        Text(
                            "/${selectedPricingUnit.shortLabel}",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextTertiary,
                        )
                    },
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Amenities
        FormSection(title = "Amenities") {
            val options = getAmenities(subCategory)
            AmenitiesGrid(
                options = options,
                selectedAmenities = amenities,
                onToggle = onToggleAmenity,
            )
        }
    }
}

// ── Step: Schedule & Availability (mode-aware, iOS parity) ──

private val HOURLY_DURATION_OPTIONS = listOf(
    15 to "15 min", 30 to "30 min", 60 to "1 hr", 120 to "2 hrs",
    180 to "3 hrs", 240 to "4 hrs", 360 to "6 hrs", 480 to "8 hrs",
    720 to "12 hrs", 1440 to "24 hrs"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleAvailabilityStep(
    pricingUnit: PricingUnit,
    selectedDurations: List<Int>,
    selectedDays: List<Int>,
    startTime: String,
    endTime: String,
    timezone: String,
    minStay: Int,
    maxStay: Int,
    checkinTime: String,
    checkoutTime: String,
    minMonths: Int,
    availableFrom: String,
    onToggleDuration: (Int) -> Unit,
    onToggleDay: (Int) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onMinStayChange: (Int) -> Unit,
    onMaxStayChange: (Int) -> Unit,
    onCheckinTimeChange: (String) -> Unit,
    onCheckoutTimeChange: (String) -> Unit,
    onMinMonthsChange: (Int) -> Unit,
    onAvailableFromChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
    ) {
        // Mode indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                .background(PaceDreamColors.Primary.copy(alpha = 0.08f))
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            Icon(
                PaceDreamIcons.Schedule,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Pricing mode: ${pricingUnit.displayLabel}",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── HOURLY MODE ──
        if (pricingUnit == PricingUnit.HOUR) {
            Text(
                text = "Available Durations",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Guests can book any of the selected durations.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            ) {
                HOURLY_DURATION_OPTIONS.forEach { (min, label) ->
                    val isSelected = selectedDurations.contains(min)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleDuration(min) },
                        label = { Text(label, style = PaceDreamTypography.Callout) },
                        leadingIcon = if (isSelected) {
                            { Icon(PaceDreamIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaceDreamColors.Primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = PaceDreamColors.Card,
                            labelColor = PaceDreamColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = PaceDreamColors.Border,
                            selectedBorderColor = PaceDreamColors.Primary,
                            enabled = true,
                            selected = isSelected,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Text(
                text = "Operating Hours",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = onStartTimeChange,
                    label = { Text("Start Time", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = onEndTimeChange,
                    label = { Text("End Time", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
            }
        }

        // ── DAILY MODE ──
        if (pricingUnit == PricingUnit.DAY || pricingUnit == PricingUnit.WEEK) {
            Text(
                text = "Stay Duration",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            ) {
                OutlinedTextField(
                    value = minStay.toString(),
                    onValueChange = { raw ->
                        val v = raw.filter { it.isDigit() }.toIntOrNull() ?: 1
                        onMinStayChange(maxOf(1, v))
                    },
                    label = { Text("Min Stay (days)", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
                OutlinedTextField(
                    value = maxStay.toString(),
                    onValueChange = { raw ->
                        val v = raw.filter { it.isDigit() }.toIntOrNull() ?: minStay
                        onMaxStayChange(maxOf(minStay, v))
                    },
                    label = { Text("Max Stay (days)", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Text(
                text = "Check-in & Check-out",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            ) {
                OutlinedTextField(
                    value = checkinTime,
                    onValueChange = onCheckinTimeChange,
                    label = { Text("Check-in Time", style = PaceDreamTypography.Callout) },
                    placeholder = { Text("15:00", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
                OutlinedTextField(
                    value = checkoutTime,
                    onValueChange = onCheckoutTimeChange,
                    label = { Text("Check-out Time", style = PaceDreamTypography.Callout) },
                    placeholder = { Text("11:00", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Guests must check out by the check-out time.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        }

        // ── MONTHLY MODE ──
        if (pricingUnit == PricingUnit.MONTH) {
            Text(
                text = "Lease Terms",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            OutlinedTextField(
                value = minMonths.toString(),
                onValueChange = { raw ->
                    val v = raw.filter { it.isDigit() }.toIntOrNull() ?: 1
                    onMinMonthsChange(maxOf(1, v))
                },
                label = { Text("Minimum Months", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "The shortest lease term you will accept.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Text(
                text = "Available From",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            OutlinedTextField(
                value = availableFrom,
                onValueChange = onAvailableFromChange,
                label = { Text("Earliest move-in date", style = PaceDreamTypography.Callout) },
                placeholder = { Text("YYYY-MM-DD", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "The earliest date a tenant can move in.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        }

        // Available Days (hourly & daily)
        if (pricingUnit == PricingUnit.HOUR || pricingUnit == PricingUnit.DAY || pricingUnit == PricingUnit.WEEK) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Text(
                text = "Available Days",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                DAY_LABELS.forEach { (label, value) ->
                    val isOn = selectedDays.contains(value)
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 34.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.SM))
                            .background(if (isOn) PaceDreamColors.Primary else PaceDreamColors.Primary.copy(alpha = 0.06f))
                            .clickable { onToggleDay(value) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = PaceDreamTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            color = if (isOn) Color.White else PaceDreamColors.TextPrimary,
                        )
                    }
                }
            }
        }

        // Timezone (all modes)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        OutlinedTextField(
            value = timezone,
            onValueChange = onTimezoneChange,
            label = { Text("Timezone", style = PaceDreamTypography.Callout) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaceDreamColors.Primary,
                unfocusedBorderColor = PaceDreamColors.Border,
            ),
        )
    }
}

// ── Step: Review & Publish (iOS: ListingReviewPublishStepView) ──

@Composable
private fun ReviewPublishStep(
    listingMode: ListingMode,
    subCategory: String,
    title: String,
    description: String = "",
    address: String,
    city: String,
    state: String,
    basePrice: String,
    totalCost: String,
    selectedPricingUnit: PricingUnit,
    hasSchedule: Boolean,
    selectedDurations: List<Int>,
    selectedDays: List<Int>,
    startTime: String,
    endTime: String,
    timezone: String,
    minStay: Int = 1,
    maxStay: Int = 7,
    checkinTime: String = "15:00",
    checkoutTime: String = "11:00",
    minMonths: Int = 1,
    availableFrom: String = "",
    selectedImageUris: List<Uri> = emptyList(),
    amenities: List<String> = emptyList(),
) {
    val resolvedPrice = if (listingMode == ListingMode.SPLIT) {
        totalCost.toDoubleOrNull() ?: 0.0
    } else {
        basePrice.toDoubleOrNull() ?: 0.0
    }

    val priceDisplay = if (listingMode == ListingMode.SPLIT) {
        "$${resolvedPrice.toInt()} total"
    } else {
        "$${resolvedPrice.toInt()}/${selectedPricingUnit.shortLabel}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
    ) {
        Text(
            text = "Preview",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        // Preview card – iOS parity: show first photo or placeholder
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        ) {
            if (selectedImageUris.isNotEmpty()) {
                AsyncImage(
                    model = selectedImageUris.first(),
                    contentDescription = "Cover photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    PaceDreamColors.Primary.copy(alpha = 0.08f),
                                    PaceDreamColors.Primary.copy(alpha = 0.03f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        PaceDreamIcons.CameraAlt,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifBlank { "Untitled listing" },
                            style = PaceDreamTypography.Title3,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                        )
                        if (city.isNotBlank() || state.isNotBlank()) {
                            Text(
                                text = "$city, $state".trim(' ', ','),
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.TextSecondary,
                            )
                        }
                        Text(
                            text = priceDisplay,
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = subCategory,
                        style = PaceDreamTypography.Caption2,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(PaceDreamRadius.Round))
                            .background(PaceDreamColors.Primary.copy(alpha = 0.1f))
                            .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        Text(
            text = "What will be published",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        SummaryRow("Listing Type", resolveResourceTypeLabel(subCategory))
        SummaryRow("Subcategory", subCategory)
        SummaryRow("Pricing", selectedPricingUnit.displayLabel)
        SummaryRow("Price", priceDisplay)
        if (description.isNotBlank()) {
            SummaryRow("Description", description.trim().take(100) + if (description.trim().length > 100) "\u2026" else "")
        }
        if (selectedImageUris.isNotEmpty()) {
            SummaryRow("Photos", "${selectedImageUris.size} photo${if (selectedImageUris.size > 1) "s" else ""}")
        }
        if (listingMode != ListingMode.SPLIT) {
            SummaryRow("Address", address.ifBlank { "\u2014" })
            SummaryRow("City/State", "$city, $state".trim(' ', ',').ifBlank { "\u2014" })
        }
        if (amenities.isNotEmpty()) {
            SummaryRow("Amenities", amenities.joinToString(", "))
        }
        if (hasSchedule) {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            when (selectedPricingUnit) {
                PricingUnit.HOUR -> {
                    SummaryRow("Durations", selectedDurations.joinToString(", ") {
                        HOURLY_DURATION_OPTIONS.firstOrNull { (v, _) -> v == it }?.second ?: "$it min"
                    }.ifBlank { "\u2014" })
                    SummaryRow("Days", selectedDays.sorted().joinToString(", ") { dayNames.getOrElse(it) { "$it" } }.ifBlank { "\u2014" })
                    SummaryRow("Hours", "$startTime\u2013$endTime")
                }
                PricingUnit.DAY, PricingUnit.WEEK -> {
                    SummaryRow("Stay", "$minStay\u2013$maxStay days")
                    SummaryRow("Check-in", checkinTime)
                    SummaryRow("Check-out", checkoutTime)
                    SummaryRow("Days", selectedDays.sorted().joinToString(", ") { dayNames.getOrElse(it) { "$it" } }.ifBlank { "\u2014" })
                }
                PricingUnit.MONTH -> {
                    SummaryRow("Min. months", "$minMonths")
                    if (availableFrom.isNotBlank()) {
                        SummaryRow("Available", availableFrom)
                    }
                }
            }
            SummaryRow("Timezone", timezone)
        }
    }
}

// ── Phase 4: Publish Success (iOS: ListingPublishSuccessView) ──
// iOS parity: shows green checkmark, "Submitted!", Under Review banner,
// cover image preview, and 3 CTAs: View Listing, Go to My Listings, Back to Home.

@Composable
private fun PublishSuccessScreen(
    listingId: String,
    title: String,
    coverUrl: String? = null,
    onViewListing: () -> Unit,
    onGoToMyListings: () -> Unit,
    onBackToHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Green checkmark (iOS parity)
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Success.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                PaceDreamIcons.CheckCircle,
                contentDescription = null,
                tint = PaceDreamColors.Success,
                modifier = Modifier.size(PaceDreamIconSize.XXL),
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Text(
            text = "Submitted!",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = title,
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 22.dp),
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        // Under Review banner (iOS parity: orange banner with clock icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.LG))
                .background(Color(0xFFFFA500).copy(alpha = 0.12f))
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                PaceDreamIcons.Schedule,
                contentDescription = null,
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Under Review",
                    style = PaceDreamTypography.Callout,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.TextPrimary,
                )
                Text(
                    text = "Your listing is being reviewed and will be visible once approved.",
                    style = PaceDreamTypography.Caption,
                    fontWeight = FontWeight.Medium,
                    color = PaceDreamColors.TextSecondary,
                )
            }
        }

        // Cover image preview (iOS parity)
        if (coverUrl != null && coverUrl.startsWith("http")) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            AsyncImage(
                model = coverUrl,
                contentDescription = "Listing cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3 CTAs matching iOS: View Listing, Go to My Listings, Back to Home
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PaceDreamSpacing.XL),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            // Primary: View Listing (iOS parity)
            Button(
                onClick = onViewListing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text("View Listing", style = PaceDreamTypography.Button)
            }

            // Secondary: Go to My Listings (iOS parity)
            Button(
                onClick = onGoToMyListings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Gray100,
                    contentColor = PaceDreamColors.TextPrimary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    "Go to My Listings",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Tertiary: Back to Home (iOS parity)
            TextButton(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Back to Home",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextSecondary,
                )
            }
        }
    }
}

// ── Shared Components ──

@Composable
private fun WizardBottomBar(
    currentStep: Int,
    totalSteps: Int,
    validationMessage: String?,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isPublishing: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Card.copy(alpha = PaceDreamGlass.ThickAlpha),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.MD)
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
        ) {
            if (validationMessage != null) {
                Text(
                    text = validationMessage,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.Error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = PaceDreamSpacing.SM),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.height(PaceDreamButtonHeight.MD),
                        shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                    ) {
                        Text("Back", style = PaceDreamTypography.Button)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier.height(PaceDreamButtonHeight.MD),
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                    shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    enabled = !isPublishing,
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text("Publishing\u2026", style = PaceDreamTypography.Button)
                    } else {
                        Text(
                            text = if (currentStep == totalSteps - 1) "Publish" else "Next",
                            style = PaceDreamTypography.Button,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PricingUnitSelector(
    allowedUnits: List<PricingUnit>,
    selectedUnit: PricingUnit,
    onUnitSelected: (PricingUnit) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.Primary.copy(alpha = 0.06f))
            .padding(PaceDreamSpacing.XS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS),
        ) {
            allowedUnits.forEach { unit ->
                val isSelected = selectedUnit == unit
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    animationSpec = tween(200),
                    label = "segmented_bg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                    animationSpec = tween(200),
                    label = "segmented_text",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSelected) Modifier
                                .shadow(PaceDreamElevation.XS, RoundedCornerShape(PaceDreamRadius.SM))
                                .border(
                                    1.dp,
                                    PaceDreamColors.Primary.copy(alpha = 0.2f),
                                    RoundedCornerShape(PaceDreamRadius.SM),
                                )
                            else Modifier
                        )
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(bgColor)
                        .clickable { onUnitSelected(unit) }
                        .padding(vertical = PaceDreamSpacing.SM),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = unit.displayLabel,
                        style = PaceDreamTypography.Callout,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesGrid(
    options: List<String>,
    selectedAmenities: List<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
    ) {
        options.forEach { name ->
            val isOn = selectedAmenities.contains(name)
            FilterChip(
                selected = isOn,
                onClick = { onToggle(name) },
                label = { Text(name, style = PaceDreamTypography.Callout) },
                leadingIcon = if (isOn) {
                    { Icon(PaceDreamIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PaceDreamColors.Primary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = PaceDreamColors.Card,
                    labelColor = PaceDreamColors.TextPrimary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PaceDreamColors.Border,
                    selectedBorderColor = PaceDreamColors.Primary,
                    enabled = true,
                    selected = isOn,
                ),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.XS),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(92.dp),
        )
        Text(
            text = value,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = PaceDreamSpacing.XS, bottom = PaceDreamSpacing.SM),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PhotoUploadPlaceholder(onClick: () -> Unit, photoCount: Int = 0) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    PaceDreamIcons.Add,
                    contentDescription = "Add photo",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(28.dp),
                )
                if (photoCount == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add",
                        style = PaceDreamTypography.Caption2,
                        color = PaceDreamColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// iOS parity: max 10 photos per listing
private const val MAX_PHOTOS = 10

/**
 * Amenities per subcategory – iOS parity (Amenity.swift).
 * Each group matches the iOS Amenity model's subcategory grouping.
 */
private fun getAmenities(subCategory: String): List<String> {
    return when (subCategory.lowercase()) {
        // Share
        "restroom" -> listOf("Toilet Paper", "Hand Soap", "Hand Towels", "Air Freshener", "Paper Towels")
        "nap_pod" -> listOf(
            "Noise Cancellation", "Soundproof Walls", "White Noise Machine", "Earplugs",
            "Calming Music", "Reclining Chair", "Blanket", "Pillow", "Eye Mask", "Temperature Control"
        )
        "meeting_room" -> listOf("WiFi", "Power Outlets", "Projector", "Whiteboard", "Monitor/TV", "Desk Space", "Chairs")
        "gym" -> listOf("Exercise Equipment", "Locker Room", "Showers", "Water Fountain", "Towel Service", "Air Conditioning")
        "parking" -> listOf("Covered Parking", "Security Camera", "EV Charging", "Gated Access", "Well Lit", "24/7 Access")
        "storage_space" -> listOf("Climate Controlled", "Security Camera", "Gated Access", "24/7 Access", "Ground Floor", "Drive-Up Access")
        "wifi" -> listOf("High Speed", "Unlimited Data", "Router Included", "5G Support", "Password Protected")
        "short_stay", "apartment", "luxury_room" -> listOf(
            "WiFi", "Kitchen", "Parking", "Washer/Dryer", "Heating", "Air Conditioning",
            "TV", "Bathroom Essentials", "Bed Linens"
        )
        // Borrow
        "sports_gear" -> listOf("Adjustable Straps", "Carrying Case", "Extra Batteries", "Charger", "Quick Release")
        "camera" -> listOf("Lens Included", "Extra Batteries", "Memory Card", "Camera Bag", "Tripod", "Filters", "Remote Control")
        "tech" -> listOf("Charger Included", "Case/Cover", "Screen Protector", "Headphones", "Warranty", "Extra Cable")
        "instrument" -> listOf("Case/Bag", "Strap Included", "Tuner", "Extra Strings", "Metronome", "Stand")
        "tools" -> listOf("Carrying Case", "Safety Gear", "Extra Blades", "Charger", "Manual Included", "Extension Cord")
        "games" -> listOf("All Pieces Included", "Instructions", "Extra Controllers", "Carrying Case", "Batteries")
        "toys" -> listOf("Batteries Included", "All Parts Included", "Carrying Case", "Safety Certified", "Instructions")
        "micromobility" -> listOf("Helmet Included", "Lock Included", "Charger", "Lights", "Bell", "Basket")
        // Services
        "home_help" -> listOf("Materials Included", "Indoor", "Outdoor", "Beginner Friendly", "Equipment Provided", "Flexible Schedule")
        "moving_help" -> listOf("Materials Included", "Equipment Provided", "Flexible Schedule", "Indoor", "Outdoor")
        "cleaning_organizing" -> listOf("Materials Included", "Equipment Provided", "Indoor", "Flexible Schedule")
        "everyday_help" -> listOf("Materials Included", "Flexible Schedule", "Indoor", "Outdoor", "Beginner Friendly")
        "fitness" -> listOf("Equipment Provided", "Indoor", "Outdoor", "Beginner Friendly", "Group Session", "Flexible Schedule")
        "learning" -> listOf("Materials Included", "Indoor", "Beginner Friendly", "Group Session", "Flexible Schedule")
        "creative" -> listOf("Materials Included", "Equipment Provided", "Indoor", "Beginner Friendly", "Group Session", "Flexible Schedule")
        // Split
        "membership" -> listOf("24/7 Access", "Guest Pass", "Parking", "Locker", "Towel Service", "Sauna", "Classes")
        else -> listOf("WiFi", "AC", "Parking", "Clean", "Accessible")
    }
}
