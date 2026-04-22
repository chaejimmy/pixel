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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.collectAsState
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
import com.shourov.apps.pacedream.feature.host.data.DetailsPayload
import com.shourov.apps.pacedream.feature.host.data.CreateListingDraftStore
import com.shourov.apps.pacedream.feature.host.data.CreateListingRequest
import com.shourov.apps.pacedream.feature.host.data.ImageUploadService
import com.shourov.apps.pacedream.feature.host.data.ListingDraftData
import com.shourov.apps.pacedream.feature.host.data.LocationPayload
import com.shourov.apps.pacedream.feature.host.data.WifiAccessDraft
import com.shourov.apps.pacedream.feature.host.data.WifiAccessPayload
import com.pacedream.app.core.location.LocationServiceEntryPoint
import com.pacedream.app.core.location.PlacePrediction
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.shourov.apps.pacedream.feature.host.data.PricingPayload
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.model.PricingUnit
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
    SubcategoryItem("wifi", "wifi", "Wi-Fi", "Bookable, time-limited internet access", PaceDreamIcons.Wifi, needsSchedule = true),
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

/** Map the wizard's ResourceKind to the schema registry's ListingCategory. */
private fun ResourceKind.toListingCategory(): ListingCategory = when (this) {
    ResourceKind.SPACES -> ListingCategory.SPACE
    ResourceKind.ITEMS -> ListingCategory.ITEM
    ResourceKind.SERVICES -> ListingCategory.SERVICE
}

/**
 * Resolve the active field schema for the wizard. Splits always use
 * monthly pricing and skip the category-specific sections, so they return
 * a minimal schema rather than whatever the subcategory would normally
 * dictate.
 */
private fun resolveSchema(
    listingMode: ListingMode,
    resourceKind: ResourceKind,
    subCategory: String,
): SubcategorySchema {
    if (listingMode == ListingMode.SPLIT) {
        return SubcategorySchema(
            id = subCategory.ifBlank { "split" },
            category = resourceKind.toListingCategory(),
            displayLabel = subCategory.ifBlank { "Split" },
            fields = setOf(
                ListingField.TITLE,
                ListingField.DESCRIPTION,
                ListingField.PHOTOS,
                ListingField.PRICING_UNIT,
                ListingField.AMENITIES,
                ListingField.LOCATION,
                ListingField.SPLIT_DEADLINE,
                ListingField.SPLIT_REQUIREMENTS,
                ListingField.SPLIT_TOTAL_COST,
            ),
            allowedPricingUnits = listOf(PricingUnit.MONTH),
            needsSchedule = false,
        )
    }
    return ListingSchemaRegistry.schemaFor(
        resourceKind.toListingCategory(),
        subCategory,
    )
}

/** Pricing units from the schema with a safe fallback for edge cases. */
private fun allowedPricingUnitsFor(schema: SubcategorySchema): List<PricingUnit> =
    schema.allowedPricingUnits.ifEmpty { listOf(PricingUnit.HOUR) }

/**
 * Whether the wizard surfaces the schedule/availability step.
 * Splits never have time-based scheduling; for everything else, the
 * schema decides.  Subcategories without time semantics (e.g. a
 * one-off service, or a storage space priced monthly only) can opt out
 * by setting [SubcategorySchema.needsSchedule] to false.
 */
private fun needsSchedule(listingMode: ListingMode, schema: SubcategorySchema): Boolean {
    return listingMode != ListingMode.SPLIT && schema.needsSchedule
}

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
    onGoToMyListings: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onPublishListing: (CreateListingRequest) -> Unit = {},
    viewModel: CreateListingViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    // iOS parity: draft persistence — scoped to the authenticated user so
    // drafts never leak between accounts on a shared device.
    val context = LocalContext.current
    val currentUserId by viewModel.currentUserId.collectAsState()
    val draftStore = remember(currentUserId) {
        CreateListingDraftStore(context, userId = currentUserId)
    }

    // Phase: entry → subcategory → wizard → success
    var phase by remember { mutableStateOf("entry") }
    var selectedMode by remember { mutableStateOf(listingMode) }
    var selectedResourceKind by remember { mutableStateOf(ResourceKind.SPACES) }
    var selectedSubCategory by remember { mutableStateOf("") }
    /** Draft to rehydrate into the wizard when the host taps "Continue your draft". */
    var resumedDraft by remember { mutableStateOf<ListingDraftData?>(null) }
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
                    // Don't call onPublishSuccess here — let the success screen
                    // display first so the user sees the confirmation. The CTAs
                    // on the success screen handle navigation.
                }
                is CreateListingViewModel.Effect.PublishError -> {
                    publishError = effect.message
                }
                is CreateListingViewModel.Effect.PayoutSetupRequired -> {
                    // Surface as a blocking prompt on the wizard screen so the
                    // host sees the reason and a clear next step instead of a
                    // generic "publish failed" toast.
                    publishError = effect.reason
                }
            }
        }
    }

    when (phase) {
        "entry" -> CreateListingEntryScreen(
            onResourceKindSelected = { kind ->
                selectedResourceKind = kind
                selectedMode = kind.listingMode
                resumedDraft = null  // fresh flow discards any prior resume attempt
                phase = "subcategory"
            },
            onBackClick = onBackClick,
            draftStore = draftStore,
            onResumeDraft = { draft ->
                // Resume from saved draft: jump directly to wizard with saved state
                selectedMode = ListingMode.entries.firstOrNull { it.backendValue == draft.listingMode } ?: ListingMode.SHARE
                selectedResourceKind = ResourceKind.entries.firstOrNull { it.name.equals(draft.resourceKind, ignoreCase = true) } ?: ResourceKind.SPACES
                selectedSubCategory = draft.subCategory
                resumedDraft = draft
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
        "wizard" -> {
            val vmPublishing by viewModel.isPublishing.collectAsState()
            CreateListingWizardScreen(
                listingMode = selectedMode,
                subCategory = selectedSubCategory,
                resourceKind = selectedResourceKind,
                imageUploadService = imageUploadService,
                draftStore = draftStore,
                initialDraft = resumedDraft,
                onBackClick = { phase = "subcategory" },
                onPublishListing = { request ->
                    publishError = null
                    viewModel.publishListing(request)
                },
                isApiPublishing = vmPublishing,
                publishError = publishError,
                onClearPublishError = { publishError = null },
            )
        }
        "success" -> PublishSuccessScreen(
            listingId = publishedListingId,
            title = publishedTitle,
            coverUrl = publishedCoverUrl,
            onViewListing = { phase = "preview" },
            onGoToMyListings = onGoToMyListings,
            onBackToHome = onBackToHome,
        )
        "preview" -> HostListingPreviewScreen(
            listingId = publishedListingId,
            onBackClick = { phase = "success" }
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
                title = { Text("Create listing", style = PaceDreamTypography.Headline) },
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
                text = "Create listing",
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
                tint = PaceDreamColors.HostAccent,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Border.copy(alpha = 0.5f)
        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Border.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.SM)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    .background(PaceDreamColors.HostAccent.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = PaceDreamColors.HostAccent,
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
    resourceKind: ResourceKind,
    imageUploadService: ImageUploadService? = null,
    draftStore: CreateListingDraftStore? = null,
    initialDraft: ListingDraftData? = null,
    onBackClick: () -> Unit,
    onPublishListing: (CreateListingRequest) -> Unit,
    isApiPublishing: Boolean = false,
    publishError: String? = null,
    onClearPublishError: () -> Unit = {},
) {
    // Single source of truth for which fields this subcategory supports.
    val schema = remember(listingMode, resourceKind, subCategory) {
        resolveSchema(listingMode, resourceKind, subCategory)
    }
    val hasSchedule = needsSchedule(listingMode, schema)
    val step1Label = when {
        schema.hasField(ListingField.LOCATION) -> "Photos \u00b7 Location \u00b7 Pricing"
        schema.hasField(ListingField.PHOTOS) -> "Photos \u00b7 Pricing"
        else -> "Pricing"
    }
    val hasWifi = schema.hasField(ListingField.WIFI_ACCESS)
    // Wi-Fi listings get a dedicated access step inserted before the
    // schedule step, and the schedule step is re-framed as "Access
    // Validity" so hosts see booking duration as connection time.
    val steps = buildList {
        add("Basics")
        add(step1Label)
        if (hasWifi) add("Wi-Fi Access")
        if (hasSchedule) {
            add(if (hasWifi) "Access Validity" else "Schedule & Availability")
        }
        add("Review & Publish")
    }
    val totalSteps = steps.size
    var currentStep by remember { mutableIntStateOf(0) }
    // Resolved step indices — keeps validation + dispatch in sync with
    // the dynamic `steps` list above.
    val wifiStepIndex = if (hasWifi) 2 else -1
    val scheduleStepIndex = when {
        hasSchedule && hasWifi -> 3
        hasSchedule -> 2
        else -> -1
    }
    val reviewStepIndex = totalSteps - 1

    // Form state – iOS parity (ListingDraft fields). All `remember` calls are
    // keyed on the draft identity so resuming a saved draft hydrates the wizard
    // with the saved values instead of blank strings.
    val draftKey = initialDraft?.hashCode() ?: 0
    var title by remember(draftKey) { mutableStateOf(initialDraft?.title.orEmpty()) }
    var description by remember(draftKey) { mutableStateOf(initialDraft?.description.orEmpty()) }

    // Split-specific
    var deadlineAt by remember(draftKey) { mutableStateOf(initialDraft?.deadlineAt.orEmpty()) }
    var requirements by remember(draftKey) { mutableStateOf(initialDraft?.requirements.orEmpty()) }
    var totalCost by remember(draftKey) { mutableStateOf(initialDraft?.totalCost.orEmpty()) }

    // Photos – iOS parity: selected image URIs + uploaded Cloudinary URLs.
    // Already-uploaded URLs from the draft seed the uploaded list so resuming
    // does not re-upload them; locally-picked URIs (not serializable) start empty.
    val selectedImageUris = remember(draftKey) { mutableStateListOf<Uri>() }
    val uploadedImageUrls = remember(draftKey) {
        mutableStateListOf<String>().apply {
            initialDraft?.uploadedImageUrls?.let { addAll(it) }
        }
    }
    var isUploadingImages by remember { mutableStateOf(false) }

    // Photos/Location/Pricing
    var address by remember(draftKey) { mutableStateOf(initialDraft?.address.orEmpty()) }
    var city by remember(draftKey) { mutableStateOf(initialDraft?.city.orEmpty()) }
    var state by remember(draftKey) { mutableStateOf(initialDraft?.state.orEmpty()) }
    var locationLat by remember(draftKey) { mutableStateOf(initialDraft?.latitude ?: 0.0) }
    var locationLng by remember(draftKey) { mutableStateOf(initialDraft?.longitude ?: 0.0) }
    var basePrice by remember(draftKey) { mutableStateOf(initialDraft?.basePrice.orEmpty()) }
    val amenities = remember(draftKey) {
        mutableStateListOf<String>().apply {
            initialDraft?.amenities?.let { addAll(it) }
        }
    }

    // Pricing unit — schema-driven.  Resumed drafts restore the saved unit
    // when still allowed; otherwise fall back to the schema's default.
    val allowedUnits = allowedPricingUnitsFor(schema)
    val initialPricingUnit = initialDraft?.pricingUnit
        ?.let { saved -> allowedUnits.firstOrNull { it.value == saved } }
        ?: allowedUnits.firstOrNull() ?: PricingUnit.HOUR
    var selectedPricingUnit by remember(draftKey) { mutableStateOf(initialPricingUnit) }

    // Schedule/Availability – use device timezone (iOS parity: TimeZone.current.identifier).
    val defaultTimezone = TimeZone.getDefault().id
    val selectedDurations = remember(draftKey) {
        mutableStateListOf<Int>().apply {
            initialDraft?.selectedDurations?.let { addAll(it) }
        }
    }
    val selectedDays = remember(draftKey) {
        mutableStateListOf<Int>().apply {
            initialDraft?.selectedDays?.let { addAll(it) }
        }
    }
    var startTime by remember(draftKey) { mutableStateOf(initialDraft?.startTime?.ifBlank { "09:00" } ?: "09:00") }
    var endTime by remember(draftKey) { mutableStateOf(initialDraft?.endTime?.ifBlank { "17:00" } ?: "17:00") }
    var timezone by remember(draftKey) {
        mutableStateOf(initialDraft?.timezone?.ifBlank { defaultTimezone } ?: defaultTimezone)
    }

    // Daily-mode fields
    var minStay by remember(draftKey) { mutableIntStateOf(initialDraft?.minStay ?: 1) }
    var maxStay by remember(draftKey) { mutableIntStateOf(initialDraft?.maxStay ?: 7) }
    var checkinTime by remember(draftKey) { mutableStateOf(initialDraft?.checkinTime?.ifBlank { "15:00" } ?: "15:00") }
    var checkoutTime by remember(draftKey) { mutableStateOf(initialDraft?.checkoutTime?.ifBlank { "11:00" } ?: "11:00") }

    // Monthly-mode fields
    var minMonths by remember(draftKey) { mutableIntStateOf(initialDraft?.minMonths ?: 1) }
    var availableFrom by remember(draftKey) { mutableStateOf(initialDraft?.availableFrom.orEmpty()) }

    // Capacity — surfaced only for accommodation-style space listings, as
    // declared by the subcategory schema.  Items and services never see
    // these fields, so parking/camera/gym listings no longer ship stray
    // bedroom counts to the backend.
    var maxGuests by remember(draftKey) {
        mutableIntStateOf(initialDraft?.maxGuests?.coerceIn(1, 32) ?: 1)
    }
    var bedrooms by remember(draftKey) {
        mutableIntStateOf(initialDraft?.bedrooms?.coerceIn(0, 20) ?: 0)
    }
    var bathrooms by remember(draftKey) {
        mutableIntStateOf(initialDraft?.bathrooms?.coerceIn(0, 20) ?: 0)
    }

    // Parking-specific state (parking / EV parking schemas).
    var vehicleCapacity by remember(draftKey) {
        mutableIntStateOf(initialDraft?.vehicleCapacity?.coerceIn(1, 20) ?: 1)
    }
    var parkingCovered by remember(draftKey) {
        mutableStateOf(initialDraft?.parkingCovered ?: false)
    }
    var parkingEvCharging by remember(draftKey) {
        mutableStateOf(initialDraft?.parkingEvCharging ?: false)
    }
    var parkingAccess247 by remember(draftKey) {
        mutableStateOf(initialDraft?.parkingAccess247 ?: false)
    }
    var parkingSizeLimit by remember(draftKey) {
        mutableStateOf(initialDraft?.parkingSizeLimit.orEmpty())
    }
    val parkingSecurityFeatures = remember(draftKey) {
        mutableStateListOf<String>().apply {
            initialDraft?.parkingSecurityFeatures?.let { addAll(it) }
        }
    }

    // Item-rental state (gear / camera / tech / etc.).
    var deposit by remember(draftKey) { mutableStateOf(initialDraft?.deposit.orEmpty()) }
    var condition by remember(draftKey) { mutableStateOf(initialDraft?.condition.orEmpty()) }
    val pickupDeliveryOptions = remember(draftKey) {
        mutableStateListOf<String>().apply {
            initialDraft?.pickupDeliveryOptions?.let { addAll(it) }
        }
    }

    // Service state — one-off session length in minutes.
    var serviceDurationMinutes by remember(draftKey) {
        mutableIntStateOf(initialDraft?.serviceDurationMinutes?.coerceIn(15, 600) ?: 60)
    }

    // Wi-Fi access state — only meaningful when the schema opts in.
    // Persisted via the draft store like every other wizard field.
    val initialWifi = initialDraft?.wifi ?: WifiAccessDraft()
    var wifiIncluded by remember(draftKey) { mutableStateOf(initialWifi.included) }
    var wifiSsid by remember(draftKey) { mutableStateOf(initialWifi.ssid) }
    var wifiPassword by remember(draftKey) { mutableStateOf(initialWifi.password) }
    var wifiShowAfterBooking by remember(draftKey) {
        mutableStateOf(initialWifi.showAfterBooking)
    }
    var wifiAutoQr by remember(draftKey) {
        mutableStateOf(initialWifi.autoGenerateQrCode)
    }
    var wifiExtensionEnabled by remember(draftKey) {
        mutableStateOf(initialWifi.extensionEnabled)
    }
    var wifiExtensionPrice by remember(draftKey) {
        mutableStateOf(initialWifi.extensionPricePerHour)
    }
    val wifiExperienceTags = remember(draftKey) {
        mutableStateListOf<String>().apply { addAll(initialWifi.experienceTags) }
    }

    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isUploadingOverlay by remember { mutableStateOf(false) }
    var uploadProgressText by remember { mutableStateOf("") }

    // Combined publishing state: uploading images locally OR waiting for API response
    val isPublishing = isUploadingImages || isApiPublishing

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val progress = (currentStep + 1).toFloat() / totalSteps

    // Dismiss upload overlay when API call finishes (success transitions to success screen;
    // error should dismiss overlay so user can see the error and retry)
    LaunchedEffect(isApiPublishing, publishError) {
        if (!isApiPublishing && isUploadingOverlay) {
            isUploadingOverlay = false
        }
    }

    // Schema-driven validation.  Each step only validates fields the active
    // subcategory schema actually surfaced, so hidden inputs can never block
    // publish.  Required-copy lengths are intentionally relaxed from the
    // legacy "website parity" limits — a clear 10-char title was too strict
    // for real-world listings like "EV stall 4".
    fun validateStep(step: Int): String? {
        // Wi-Fi step: SSID + password required only when the host opts
        // Wi-Fi in. Hosts can still publish a "Wi-Fi Not Included" shell
        // if they want to (rare, but we don't want to block them).
        if (step == wifiStepIndex) {
            if (wifiIncluded) {
                val trimmedSsid = wifiSsid.trim()
                if (trimmedSsid.isEmpty()) return "Network name (SSID) is required."
                if (trimmedSsid.length > 32) {
                    return "Network name must be 32 characters or fewer."
                }
                if (wifiPassword.length < 8) {
                    return "Wi-Fi password must be at least 8 characters."
                }
                if (wifiExtensionEnabled) {
                    val ext = wifiExtensionPrice.toDoubleOrNull() ?: 0.0
                    if (ext < 1.0) {
                        return "Extension price must be at least $1 per hour."
                    }
                }
            }
            return null
        }
        if (step == scheduleStepIndex) {
            if (hasSchedule) {
                when (selectedPricingUnit) {
                    PricingUnit.HOUR -> {
                        if (schema.hasField(ListingField.SCHEDULE_HOURLY_DURATIONS) &&
                            selectedDurations.isEmpty()
                        ) {
                            return "Select at least 1 duration."
                        }
                        if (schema.hasField(ListingField.SCHEDULE_AVAILABLE_DAYS) &&
                            selectedDays.isEmpty()
                        ) {
                            return "Select at least 1 available day."
                        }
                    }
                    PricingUnit.DAY, PricingUnit.WEEK -> {
                        if (schema.hasField(ListingField.SCHEDULE_STAY_LIMITS)) {
                            if (minStay < 1) return "Minimum stay must be at least 1 day."
                            if (maxStay < minStay) return "Maximum stay must be at least the minimum stay."
                        }
                        if (schema.hasField(ListingField.SCHEDULE_AVAILABLE_DAYS) &&
                            selectedDays.isEmpty()
                        ) {
                            return "Select at least 1 available day."
                        }
                    }
                    PricingUnit.MONTH -> {
                        if (schema.hasField(ListingField.SCHEDULE_MIN_MONTHS) && minMonths < 1) {
                            return "Minimum months must be at least 1."
                        }
                    }
                }
            }
            return null
        }
        return when (step) {
            0 -> {
                if (schema.hasField(ListingField.TITLE)) {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) return "Title is required."
                    if (trimmedTitle.length < 3) return "Title must be at least 3 characters."
                    if (trimmedTitle.length > 100) return "Title must be 100 characters or fewer."
                }
                if (schema.hasField(ListingField.DESCRIPTION)) {
                    val trimmedDesc = description.trim()
                    // Soft validation — description is optional and the
                    // wizard surfaces a helper warning under the input.
                    if (trimmedDesc.length > 2000) {
                        return "Description must be 2,000 characters or fewer."
                    }
                }
                null
            }
            1 -> {
                // Price applies to every schema that declares PRICE (split
                // uses totalCost instead).
                if (listingMode == ListingMode.SPLIT) {
                    val price = totalCost.toDoubleOrNull() ?: 0.0
                    if (price < 1.0) return "Total cost must be at least \$1."
                } else if (schema.hasField(ListingField.PRICE)) {
                    val price = basePrice.toDoubleOrNull() ?: 0.0
                    if (price < 1.0) return "Price must be at least \$1."
                }
                if (schema.hasField(ListingField.PHOTOS) &&
                    listingMode != ListingMode.SPLIT &&
                    selectedImageUris.isEmpty() && uploadedImageUrls.isEmpty()
                ) {
                    return "Add at least 1 photo."
                }
                if (schema.hasField(ListingField.LOCATION) && listingMode != ListingMode.SPLIT) {
                    if (address.isBlank()) return "Address is required."
                    // The autocomplete contract: pick a suggestion so the
                    // place-details payload fills city/state reliably.
                    // Free-text fallbacks previously yielded junk values
                    // like city="IL 62701", state="USA".
                    if (city.isBlank() || state.isBlank()) {
                        return "Please select an address from the suggestions so we capture the correct city and state."
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
                validationMessage = validationMessage ?: publishError,
                onBack = {
                    validationMessage = null
                    onClearPublishError()
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
                        // iOS parity: auto-save draft when navigating between steps.
                        // Persist everything the wizard will need to rehydrate,
                        // including already-uploaded photo URLs so Resume does
                        // not lose them.
                        draftStore?.save(ListingDraftData(
                            listingMode = listingMode.backendValue,
                            resourceKind = resourceKind.name,
                            subCategory = subCategory,
                            title = title.trim(),
                            description = description.trim(),
                            address = address,
                            city = city,
                            state = state,
                            latitude = locationLat,
                            longitude = locationLng,
                            basePrice = basePrice,
                            totalCost = totalCost,
                            pricingUnit = selectedPricingUnit.value,
                            amenities = amenities.toList(),
                            deadlineAt = deadlineAt,
                            requirements = requirements,
                            uploadedImageUrls = uploadedImageUrls.toList(),
                            selectedDurations = selectedDurations.toList(),
                            selectedDays = selectedDays.toList(),
                            startTime = startTime,
                            endTime = endTime,
                            timezone = timezone,
                            minStay = minStay,
                            maxStay = maxStay,
                            checkinTime = checkinTime,
                            checkoutTime = checkoutTime,
                            minMonths = minMonths,
                            availableFrom = availableFrom,
                            maxGuests = maxGuests,
                            bedrooms = bedrooms,
                            bathrooms = bathrooms,
                            vehicleCapacity = vehicleCapacity,
                            parkingCovered = parkingCovered,
                            parkingEvCharging = parkingEvCharging,
                            parkingAccess247 = parkingAccess247,
                            parkingSizeLimit = parkingSizeLimit,
                            parkingSecurityFeatures = parkingSecurityFeatures.toList(),
                            deposit = deposit,
                            condition = condition,
                            pickupDeliveryOptions = pickupDeliveryOptions.toList(),
                            serviceDurationMinutes = serviceDurationMinutes,
                            wifi = WifiAccessDraft(
                                included = wifiIncluded,
                                ssid = wifiSsid.trim(),
                                password = wifiPassword,
                                showAfterBooking = wifiShowAfterBooking,
                                autoGenerateQrCode = wifiAutoQr,
                                extensionEnabled = wifiExtensionEnabled,
                                extensionPricePerHour = wifiExtensionPrice,
                                experienceTags = wifiExperienceTags.toList(),
                            ),
                        ))
                        currentStep++
                    } else {
                        // Build payload matching iOS ListingsPublisherService
                        isUploadingOverlay = true
                        onClearPublishError()
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
                                            // Fallback: compress and encode as base64 data URL
                                            try {
                                                val stream = context.contentResolver.openInputStream(uri)
                                                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                                                stream?.close()
                                                if (bitmap != null) {
                                                    // Compress: max 1024px, JPEG 70% quality (matches website: 1024px, 0.7 quality)
                                                    val maxDim = 1024
                                                    val ratio = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                                                        minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                                                    } else 1f
                                                    val scaled = if (ratio < 1f) {
                                                        android.graphics.Bitmap.createScaledBitmap(
                                                            bitmap,
                                                            (bitmap.width * ratio).toInt().coerceAtLeast(1),
                                                            (bitmap.height * ratio).toInt().coerceAtLeast(1),
                                                            true,
                                                        )
                                                    } else bitmap
                                                    val baos = java.io.ByteArrayOutputStream()
                                                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
                                                    if (scaled !== bitmap) scaled.recycle()
                                                    bitmap.recycle()
                                                    val compressedBytes = baos.toByteArray()
                                                    timber.log.Timber.d("Image fallback: compressed to ${compressedBytes.size} bytes")
                                                    val b64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                                                    imageUrls.add("data:image/jpeg;base64,$b64")
                                                }
                                            } catch (e: Exception) {
                                                timber.log.Timber.w(e, "Base64 fallback failed for image upload")
                                            }
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

                            // Web parity: summary maps to description
                            // Backend RentableItem schema requires summary (non-empty).
                            // Fall back to title to prevent 400 validation error.
                            val trimmedDesc = description.trim()
                            val summary = trimmedDesc.ifBlank { title.trim() }

                            // Web parity: pricing_type uses unit value (hour/day/week/month)
                            val pricingMode = selectedPricingUnit.value

                            // Web parity: build prices map { hour: X, day: 0, month: 0 }
                            val pricesMap = mutableMapOf<String, Double>()
                            pricesMap["hour"] = if (pricingMode == "hour") resolvedPrice else 0.0
                            pricesMap["day"] = if (pricingMode == "day") resolvedPrice else 0.0
                            pricesMap["month"] = if (pricingMode == "month") resolvedPrice else 0.0

                            // Web parity: always build availability, even for non-schedule listings
                            val availabilityPayload = if (hasSchedule) {
                                when (selectedPricingUnit) {
                                    PricingUnit.DAY, PricingUnit.WEEK -> AvailabilityPayload(
                                        start_time = checkinTime,
                                        end_time = checkoutTime,
                                        available_days = selectedDays.toList().ifEmpty { listOf(1, 2, 3, 4, 5) },
                                        timezone = timezone,
                                        instant_booking = false,
                                    )
                                    else -> AvailabilityPayload(
                                        start_time = startTime,
                                        end_time = endTime,
                                        available_days = if (selectedPricingUnit == PricingUnit.MONTH) null
                                            else selectedDays.toList().ifEmpty { listOf(1, 2, 3, 4, 5) },
                                        timezone = timezone,
                                        instant_booking = false,
                                    )
                                }
                            } else {
                                // Web parity: non-schedule listings still send default availability
                                AvailabilityPayload(
                                    start_time = "09:00",
                                    end_time = "17:00",
                                    available_days = listOf(1, 2, 3, 4, 5),
                                    timezone = timezone,
                                    instant_booking = false,
                                )
                            }

                            // Web parity: location includes street (address), city, state, country.
                            // We rely on the autocomplete picker (validated at step 1) to
                            // populate city/state; the previous fallback comma-parser
                            // produced garbage values like city="IL 62701", state="USA".
                            val locationPayload = if (listingMode != ListingMode.SPLIT) {
                                LocationPayload(
                                    street = address,
                                    street_address = address,
                                    city = city,
                                    state = state,
                                    country = "US",
                                    latitude = if (locationLat != 0.0) locationLat else null,
                                    longitude = if (locationLng != 0.0) locationLng else null,
                                )
                            } else null

                            timber.log.Timber.d(
                                "CreateListing: type=%s sub=%s price=%.2f pricingMode=%s images=%d address=%s city=%s state=%s",
                                listingMode.backendValue, subCategory, resolvedPrice,
                                pricingMode, imageUrls.size, address, city, state,
                            )

                            val request = CreateListingRequest(
                                listing_type = listingMode.backendValue,
                                subCategory = subCategory,
                                title = title.trim(),
                                description = trimmedDesc,
                                summary = summary,  // never empty — falls back to title
                                price = resolvedPrice,
                                pricing_type = pricingMode,
                                pricing = PricingPayload(
                                    base_price = resolvedPrice,
                                    unit = pricingMode,
                                    currency = "USD",
                                ),
                                prices = pricesMap,
                                address = if (schema.hasField(ListingField.LOCATION) &&
                                    listingMode != ListingMode.SPLIT) address else null,
                                amenities = if (schema.hasField(ListingField.AMENITIES))
                                    amenities.ifEmpty { null } else null,
                                details = if (schema.hasField(ListingField.AMENITIES))
                                    DetailsPayload(features = amenities.toList(), reviewCount = 0)
                                else null,
                                images = imageUrls.ifEmpty { null },
                                location = locationPayload,
                                available = true,
                                durations = if (pricingMode == "hour" &&
                                    schema.hasField(ListingField.SCHEDULE_HOURLY_DURATIONS)
                                ) {
                                    selectedDurations.toList().ifEmpty { listOf(60, 120) }
                                } else null,
                                minStay = if ((pricingMode == "day" || pricingMode == "week") &&
                                    schema.hasField(ListingField.SCHEDULE_STAY_LIMITS)) minStay else null,
                                maxStay = if ((pricingMode == "day" || pricingMode == "week") &&
                                    schema.hasField(ListingField.SCHEDULE_STAY_LIMITS)) maxStay else null,
                                minMonths = if (pricingMode == "month" &&
                                    schema.hasField(ListingField.SCHEDULE_MIN_MONTHS)) minMonths else null,
                                availableFrom = if (pricingMode == "month" &&
                                    schema.hasField(ListingField.SCHEDULE_AVAILABLE_FROM))
                                    availableFrom.ifBlank { null } else null,
                                availability = availabilityPayload,
                                shareType = if (listingMode == ListingMode.SPLIT) "SPLIT" else null,
                                share_type = if (listingMode == ListingMode.SPLIT) "SPLIT" else null,
                                totalCost = totalCost.toDoubleOrNull(),
                                deadlineAt = deadlineAt.ifBlank { null },
                                requirements = requirements.ifBlank { null },
                                // Accommodation capacity — only emitted when the
                                // subcategory schema actually surfaces these
                                // fields.  Parking / gear / service listings no
                                // longer ship bedroom counts to the backend.
                                maxGuests = if (schema.hasField(ListingField.MAX_GUESTS)) maxGuests else null,
                                bedrooms = if (schema.hasField(ListingField.BEDROOMS)) bedrooms else null,
                                bathrooms = if (schema.hasField(ListingField.BATHROOMS)) bathrooms else null,
                                // Parking schemas.
                                vehicleCapacity = if (schema.hasField(ListingField.VEHICLE_CAPACITY))
                                    vehicleCapacity else null,
                                parkingCovered = if (schema.hasField(ListingField.PARKING_COVERED))
                                    parkingCovered else null,
                                parkingEvCharging = if (schema.hasField(ListingField.PARKING_EV_CHARGING))
                                    parkingEvCharging else null,
                                parkingAccess247 = if (schema.hasField(ListingField.PARKING_ACCESS_24_7))
                                    parkingAccess247 else null,
                                parkingSizeLimit = if (schema.hasField(ListingField.PARKING_SIZE_LIMIT))
                                    parkingSizeLimit.trim().ifBlank { null } else null,
                                parkingSecurityFeatures = if (schema.hasField(ListingField.PARKING_SECURITY_FEATURES))
                                    parkingSecurityFeatures.toList().ifEmpty { null } else null,
                                // Item-rental schemas.
                                deposit = if (schema.hasField(ListingField.DEPOSIT))
                                    deposit.toDoubleOrNull() else null,
                                condition = if (schema.hasField(ListingField.CONDITION))
                                    condition.ifBlank { null } else null,
                                pickupDeliveryOptions = if (schema.hasField(ListingField.PICKUP_DELIVERY))
                                    pickupDeliveryOptions.toList().ifEmpty { null } else null,
                                // Service schemas.
                                serviceDurationMinutes = if (schema.hasField(ListingField.SERVICE_DURATION_MINUTES))
                                    serviceDurationMinutes else null,
                                // Wi-Fi access schema — only emitted when the
                                // subcategory opts in, so other listing types
                                // still send the exact same POST body shape.
                                wifiAccess = if (hasWifi) WifiAccessPayload(
                                    included = wifiIncluded,
                                    ssid = wifiSsid.trim(),
                                    password = wifiPassword,
                                    showAfterBooking = wifiShowAfterBooking,
                                    autoGenerateQrCode = wifiAutoQr,
                                    extensionPricePerHour = if (wifiExtensionEnabled)
                                        wifiExtensionPrice.toDoubleOrNull() else null,
                                    experienceTags = wifiExperienceTags.toList(),
                                ) else null,
                            )
                            onPublishListing(request)
                            // ViewModel handles the API call and emits success/error via effects.
                            // isPublishing is now derived from isUploadingImages || isApiPublishing,
                            // so it stays true until the ViewModel finishes the API call.
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
                color = PaceDreamColors.HostAccent,
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
                        schema = schema,
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
                        onPricingUnitChange = { newUnit ->
                            val oldUnit = selectedPricingUnit
                            selectedPricingUnit = newUnit
                            // Clear incompatible state when switching pricing modes
                            if (oldUnit != newUnit) {
                                when (oldUnit) {
                                    PricingUnit.HOUR -> {
                                        selectedDurations.clear()
                                        startTime = "09:00"
                                        endTime = "17:00"
                                    }
                                    PricingUnit.DAY, PricingUnit.WEEK -> {
                                        minStay = 1
                                        maxStay = 7
                                        checkinTime = "15:00"
                                        checkoutTime = "11:00"
                                        selectedDays.clear()
                                    }
                                    PricingUnit.MONTH -> {
                                        minMonths = 1
                                        availableFrom = ""
                                    }
                                }
                            }
                        },
                        onLocationLatLngChange = { lat, lng ->
                            locationLat = lat
                            locationLng = lng
                        },
                        maxGuests = maxGuests,
                        bedrooms = bedrooms,
                        bathrooms = bathrooms,
                        onMaxGuestsChange = { maxGuests = it.coerceIn(1, 32) },
                        onBedroomsChange = { bedrooms = it.coerceIn(0, 20) },
                        onBathroomsChange = { bathrooms = it.coerceIn(0, 20) },
                        vehicleCapacity = vehicleCapacity,
                        parkingCovered = parkingCovered,
                        parkingEvCharging = parkingEvCharging,
                        parkingAccess247 = parkingAccess247,
                        parkingSizeLimit = parkingSizeLimit,
                        parkingSecurityFeatures = parkingSecurityFeatures,
                        onVehicleCapacityChange = { vehicleCapacity = it.coerceIn(1, 20) },
                        onParkingCoveredChange = { parkingCovered = it },
                        onParkingEvChargingChange = { parkingEvCharging = it },
                        onParkingAccess247Change = { parkingAccess247 = it },
                        onParkingSizeLimitChange = { parkingSizeLimit = it },
                        onToggleParkingSecurity = { feature ->
                            if (parkingSecurityFeatures.contains(feature))
                                parkingSecurityFeatures.remove(feature)
                            else parkingSecurityFeatures.add(feature)
                        },
                        deposit = deposit,
                        condition = condition,
                        pickupDeliveryOptions = pickupDeliveryOptions,
                        onDepositChange = { deposit = it },
                        onConditionChange = { condition = it },
                        onTogglePickupDelivery = { opt ->
                            if (pickupDeliveryOptions.contains(opt))
                                pickupDeliveryOptions.remove(opt)
                            else pickupDeliveryOptions.add(opt)
                        },
                        serviceDurationMinutes = serviceDurationMinutes,
                        onServiceDurationChange = { serviceDurationMinutes = it.coerceIn(15, 600) },
                    )
                    step == wifiStepIndex -> WifiAccessStep(
                        included = wifiIncluded,
                        ssid = wifiSsid,
                        password = wifiPassword,
                        showAfterBooking = wifiShowAfterBooking,
                        autoQr = wifiAutoQr,
                        extensionEnabled = wifiExtensionEnabled,
                        extensionPrice = wifiExtensionPrice,
                        experienceTags = wifiExperienceTags,
                        pricingUnit = selectedPricingUnit,
                        selectedDurations = selectedDurations,
                        minStay = minStay,
                        minMonths = minMonths,
                        onIncludedChange = { wifiIncluded = it },
                        onSsidChange = { if (it.length <= 32) wifiSsid = it },
                        onPasswordChange = { if (it.length <= 63) wifiPassword = it },
                        onShowAfterBookingChange = { wifiShowAfterBooking = it },
                        onAutoQrChange = { wifiAutoQr = it },
                        onExtensionEnabledChange = { wifiExtensionEnabled = it },
                        onExtensionPriceChange = { raw ->
                            val cleaned = raw.replace(Regex("[^0-9.]"), "")
                            val parts = cleaned.split(".")
                            wifiExtensionPrice = if (parts.size > 1)
                                "${parts[0]}.${parts[1].take(2)}" else cleaned
                        },
                        onToggleExperienceTag = { tag ->
                            if (wifiExperienceTags.contains(tag)) wifiExperienceTags.remove(tag)
                            else wifiExperienceTags.add(tag)
                        },
                    )
                    step == scheduleStepIndex -> ScheduleAvailabilityStep(
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
                        framing = if (hasWifi) ScheduleFraming.WIFI_ACCESS
                            else ScheduleFraming.GENERIC,
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
                        hasWifi = hasWifi,
                        wifiIncluded = wifiIncluded,
                        wifiSsid = wifiSsid,
                        wifiShowAfterBooking = wifiShowAfterBooking,
                        wifiAutoQr = wifiAutoQr,
                        wifiExtensionEnabled = wifiExtensionEnabled,
                        wifiExtensionPrice = wifiExtensionPrice,
                        wifiExperienceTags = wifiExperienceTags,
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
                onValueChange = { if (it.length <= 100) onTitleChange(it) },
                label = { Text("Give it a clear, searchable title", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                supportingText = {
                    val len = title.trim().length
                    val color = when {
                        len == 0 -> PaceDreamColors.TextSecondary
                        len < 3 -> PaceDreamColors.Error
                        else -> PaceDreamColors.TextSecondary
                    }
                    Text("${len}/100 characters (min 3)", style = PaceDreamTypography.Caption2, color = color)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.HostAccent,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        FormSection(title = "Description") {
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 2000) onDescriptionChange(it) },
                label = { Text("Describe your listing (optional)", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                supportingText = {
                    val len = description.trim().length
                    // Soft warning only — short descriptions still publish,
                    // they just hint to the host that guests prefer detail.
                    val hint = if (len in 1 until 10) " \u2022 More detail helps guests decide"
                    else ""
                    Text(
                        "${len}/2000 characters$hint",
                        style = PaceDreamTypography.Caption2,
                        color = PaceDreamColors.TextSecondary,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.HostAccent,
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
            }
        }
    }
}

// ── Step: Photos · Location · Pricing (schema-driven) ──

@Composable
private fun PhotosLocationPricingStep(
    listingMode: ListingMode,
    schema: SubcategorySchema,
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
    onLocationLatLngChange: (Double, Double) -> Unit = { _, _ -> },
    maxGuests: Int = 1,
    bedrooms: Int = 0,
    bathrooms: Int = 0,
    onMaxGuestsChange: (Int) -> Unit = {},
    onBedroomsChange: (Int) -> Unit = {},
    onBathroomsChange: (Int) -> Unit = {},
    vehicleCapacity: Int = 1,
    parkingCovered: Boolean = false,
    parkingEvCharging: Boolean = false,
    parkingAccess247: Boolean = false,
    parkingSizeLimit: String = "",
    parkingSecurityFeatures: List<String> = emptyList(),
    onVehicleCapacityChange: (Int) -> Unit = {},
    onParkingCoveredChange: (Boolean) -> Unit = {},
    onParkingEvChargingChange: (Boolean) -> Unit = {},
    onParkingAccess247Change: (Boolean) -> Unit = {},
    onParkingSizeLimitChange: (String) -> Unit = {},
    onToggleParkingSecurity: (String) -> Unit = {},
    deposit: String = "",
    condition: String = "",
    pickupDeliveryOptions: List<String> = emptyList(),
    onDepositChange: (String) -> Unit = {},
    onConditionChange: (String) -> Unit = {},
    onTogglePickupDelivery: (String) -> Unit = {},
    serviceDurationMinutes: Int = 60,
    onServiceDurationChange: (Int) -> Unit = {},
) {
    val isSplit = listingMode == ListingMode.SPLIT
    val showLocation = schema.hasField(ListingField.LOCATION)
    val showPhotos = schema.hasField(ListingField.PHOTOS)
    val showAmenities = schema.hasField(ListingField.AMENITIES) && schema.amenityOptions.isNotEmpty()

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
        // Photos – gated by schema.  Subcategories that opt out simply
        // never surface the section (e.g. a pure remote service).
        if (showPhotos) {
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
        }  // end showPhotos

        // Location — only rendered when the schema declares it.  Services
        // that happen remotely (learning, online coaching) skip the section.
        if (showLocation) {
        FormSection(title = if (isSplit) "Location (optional)" else "Location") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val placesService = remember {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    LocationServiceEntryPoint::class.java
                ).placesAutocompleteService()
            }
            var addressSuggestions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
            var addressJob by remember { mutableStateOf<Job?>(null) }

            OutlinedTextField(
                value = address,
                onValueChange = { newAddr ->
                    onAddressChange(newAddr)
                    addressJob?.cancel()
                    if (newAddr.trim().length >= 3) {
                        addressJob = scope.launch {
                            delay(300)
                            addressSuggestions = placesService.getAddressAutocompletePredictions(newAddr.trim())
                        }
                    } else {
                        addressSuggestions = emptyList()
                    }
                },
                label = { Text("Search address\u2026", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.HostAccent,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
            // Address autocomplete suggestions
            if (addressSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column {
                        addressSuggestions.forEach { prediction ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddressChange(prediction.description)
                                        addressSuggestions = emptyList()
                                        // Fetch place details for lat/lng and parsed address components
                                        scope.launch {
                                            if (prediction.placeId.isNotBlank()) {
                                                // Google Places result — fetch full details
                                                val details = placesService.getPlaceDetails(prediction.placeId)
                                                if (details != null) {
                                                    if (details.city.isNotBlank()) onCityChange(details.city)
                                                    if (details.state.isNotBlank()) onStateChange(details.state)
                                                    onLocationLatLngChange(details.lat, details.lng)
                                                    return@launch
                                                }
                                            }
                                            // Device geocoder result or fallback — parse from secondaryText
                                            val parts = prediction.secondaryText.split(",").map { it.trim() }
                                            if (parts.isNotEmpty()) onCityChange(parts[0])
                                            if (parts.size >= 2) onStateChange(parts[1])
                                            // Try to get lat/lng via device geocoder
                                            val locationService = EntryPointAccessors.fromApplication(
                                                context.applicationContext,
                                                LocationServiceEntryPoint::class.java
                                            ).locationService()
                                            val coords = locationService.getLocationFromAddress(prediction.description)
                                            if (coords != null) {
                                                onLocationLatLngChange(coords.first, coords.second)
                                            }
                                        }
                                    }
                                    .padding(
                                        horizontal = PaceDreamSpacing.MD,
                                        vertical = PaceDreamSpacing.SM
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.LocationOn,
                                    contentDescription = null,
                                    tint = PaceDreamColors.TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prediction.mainText,
                                        style = PaceDreamTypography.Callout,
                                        fontWeight = FontWeight.Medium,
                                        color = PaceDreamColors.TextPrimary,
                                    )
                                    if (prediction.secondaryText.isNotBlank()) {
                                        Text(
                                            text = prediction.secondaryText,
                                            style = PaceDreamTypography.Caption,
                                            color = PaceDreamColors.TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Single-source address: city/state are auto-filled from the
            // place-details response, surfaced as a read-only preview so the
            // host can confirm without being asked to retype them.
            if (city.isNotBlank() || state.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(PaceDreamRadius.MD))
                        .background(PaceDreamColors.HostAccent.copy(alpha = 0.06f))
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                ) {
                    Icon(
                        PaceDreamIcons.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamColors.HostAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$city, $state".trim(' ', ','),
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Auto-filled from selected address",
                            style = PaceDreamTypography.Caption2,
                            color = PaceDreamColors.TextSecondary,
                        )
                    }
                    TextButton(
                        onClick = {
                            onCityChange("")
                            onStateChange("")
                        },
                    ) {
                        Text(
                            text = "Clear",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        }  // end showLocation

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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                    supportingText = {
                        Text("Minimum \$1.00", style = PaceDreamTypography.Caption2, color = PaceDreamColors.TextSecondary)
                    },
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.HostAccent,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Amenities — schema provides curated options, hidden if the
        // subcategory declares no relevant amenities (e.g. remote learning).
        if (showAmenities) {
            FormSection(title = "Amenities") {
                AmenitiesGrid(
                    options = schema.amenityOptions,
                    selectedAmenities = amenities,
                    onToggle = onToggleAmenity,
                )
            }
        }

        // Accommodation capacity — gated on the per-field schema flags,
        // not on listing mode.  A meeting room surfaces max_guests but
        // hides bedrooms / bathrooms; parking and gear hide them entirely.
        val hasCapacityFields = schema.hasField(ListingField.MAX_GUESTS) ||
            schema.hasField(ListingField.BEDROOMS) ||
            schema.hasField(ListingField.BATHROOMS)
        if (hasCapacityFields) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            FormSection(title = "Capacity") {
                Text(
                    text = "How many people fit and how many rooms the space has.",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                if (schema.hasField(ListingField.MAX_GUESTS)) {
                    CreateCapacityStepperRow(
                        label = "Max guests",
                        sublabel = "Up to 32.",
                        value = maxGuests,
                        minValue = 1,
                        maxValue = 32,
                        onValueChange = onMaxGuestsChange,
                    )
                }
                if (schema.hasField(ListingField.BEDROOMS)) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    CreateCapacityStepperRow(
                        label = "Bedrooms",
                        sublabel = "Separate sleeping rooms.",
                        value = bedrooms,
                        minValue = 0,
                        maxValue = 20,
                        onValueChange = onBedroomsChange,
                    )
                }
                if (schema.hasField(ListingField.BATHROOMS)) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    CreateCapacityStepperRow(
                        label = "Bathrooms",
                        sublabel = "Full and half baths combined.",
                        value = bathrooms,
                        minValue = 0,
                        maxValue = 20,
                        onValueChange = onBathroomsChange,
                    )
                }
            }
        }

        // Parking-specific details (Parking / EV Parking schemas).
        val hasParkingFields = schema.hasField(ListingField.VEHICLE_CAPACITY) ||
            schema.hasField(ListingField.PARKING_COVERED) ||
            schema.hasField(ListingField.PARKING_EV_CHARGING) ||
            schema.hasField(ListingField.PARKING_ACCESS_24_7) ||
            schema.hasField(ListingField.PARKING_SIZE_LIMIT) ||
            schema.hasField(ListingField.PARKING_SECURITY_FEATURES)
        if (hasParkingFields) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            FormSection(title = "Parking details") {
                if (schema.hasField(ListingField.VEHICLE_CAPACITY)) {
                    CreateCapacityStepperRow(
                        label = "Vehicle capacity",
                        sublabel = "How many vehicles fit in this spot.",
                        value = vehicleCapacity,
                        minValue = 1,
                        maxValue = 20,
                        onValueChange = onVehicleCapacityChange,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                }
                if (schema.hasField(ListingField.PARKING_COVERED)) {
                    ToggleOptionRow(
                        label = "Covered",
                        sublabel = "Sheltered from weather.",
                        checked = parkingCovered,
                        onCheckedChange = onParkingCoveredChange,
                    )
                }
                if (schema.hasField(ListingField.PARKING_EV_CHARGING)) {
                    ToggleOptionRow(
                        label = "EV charging",
                        sublabel = "A charger is available on site.",
                        checked = parkingEvCharging,
                        onCheckedChange = onParkingEvChargingChange,
                    )
                }
                if (schema.hasField(ListingField.PARKING_ACCESS_24_7)) {
                    ToggleOptionRow(
                        label = "24/7 access",
                        sublabel = "Guests can enter any time of day.",
                        checked = parkingAccess247,
                        onCheckedChange = onParkingAccess247Change,
                    )
                }
                if (schema.hasField(ListingField.PARKING_SIZE_LIMIT)) {
                    OutlinedTextField(
                        value = parkingSizeLimit,
                        onValueChange = onParkingSizeLimitChange,
                        label = { Text("Size limit (optional)", style = PaceDreamTypography.Callout) },
                        placeholder = {
                            Text("e.g. Up to full-size SUV",
                                style = PaceDreamTypography.Body,
                                color = PaceDreamColors.TextSecondary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PaceDreamColors.HostAccent,
                            unfocusedBorderColor = PaceDreamColors.Border,
                        ),
                    )
                }
                if (schema.hasField(ListingField.PARKING_SECURITY_FEATURES)) {
                    Text(
                        text = "Security features (optional)",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    AmenitiesGrid(
                        options = PARKING_SECURITY_OPTIONS,
                        selectedAmenities = parkingSecurityFeatures,
                        onToggle = onToggleParkingSecurity,
                    )
                }
            }
        }

        // Item-rental details (gear / camera / tech / etc.).
        val hasItemFields = schema.hasField(ListingField.DEPOSIT) ||
            schema.hasField(ListingField.CONDITION) ||
            schema.hasField(ListingField.PICKUP_DELIVERY)
        if (hasItemFields) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            FormSection(title = "Rental details") {
                if (schema.hasField(ListingField.DEPOSIT)) {
                    OutlinedTextField(
                        value = deposit,
                        onValueChange = { raw ->
                            val cleaned = raw.replace(Regex("[^0-9.]"), "")
                            val parts = cleaned.split(".")
                            val sanitized = if (parts.size > 1)
                                "${parts[0]}.${parts[1].take(2)}" else cleaned
                            onDepositChange(sanitized)
                        },
                        label = { Text("Refundable deposit (optional)", style = PaceDreamTypography.Callout) },
                        placeholder = { Text("0.00", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                        leadingIcon = { Text("$", style = PaceDreamTypography.Title3, color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PaceDreamColors.HostAccent,
                            unfocusedBorderColor = PaceDreamColors.Border,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                if (schema.hasField(ListingField.CONDITION)) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Text(
                        text = "Condition",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    SingleChoiceChips(
                        options = ITEM_CONDITION_OPTIONS,
                        selected = condition,
                        onSelect = onConditionChange,
                    )
                }
                if (schema.hasField(ListingField.PICKUP_DELIVERY)) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Text(
                        text = "Pickup & delivery",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    AmenitiesGrid(
                        options = PICKUP_DELIVERY_OPTIONS,
                        selectedAmenities = pickupDeliveryOptions,
                        onToggle = onTogglePickupDelivery,
                    )
                }
            }
        }

        // Service-specific details.
        if (schema.hasField(ListingField.SERVICE_DURATION_MINUTES)) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            FormSection(title = "Service details") {
                Text(
                    text = "How long each session typically lasts.",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                SingleChoiceChips(
                    options = SERVICE_DURATION_CHOICES.map { it.second },
                    selected = SERVICE_DURATION_CHOICES
                        .firstOrNull { it.first == serviceDurationMinutes }?.second ?: "",
                    onSelect = { label ->
                        val match = SERVICE_DURATION_CHOICES.firstOrNull { it.second == label }
                        if (match != null) onServiceDurationChange(match.first)
                    },
                )
            }
        }
    }
}

private val PARKING_SECURITY_OPTIONS = listOf(
    "Security camera",
    "Gated access",
    "Well lit",
    "Valet on site",
    "Attendant",
    "Alarm system",
)

private val ITEM_CONDITION_OPTIONS = listOf("New", "Like new", "Good", "Fair")

private val PICKUP_DELIVERY_OPTIONS = listOf(
    "In-person pickup",
    "Delivery available",
    "Shipping available",
    "Meetup flexible",
)

/** Service session length choices, paired as (minutes, display label). */
private val SERVICE_DURATION_CHOICES: List<Pair<Int, String>> = listOf(
    15 to "15 min",
    30 to "30 min",
    45 to "45 min",
    60 to "1 hr",
    90 to "1.5 hr",
    120 to "2 hr",
    180 to "3 hr",
    240 to "4 hr",
)

@Composable
private fun ToggleOptionRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = PaceDreamSpacing.XS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = sublabel,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = PaceDreamColors.HostAccent,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
    ) {
        options.forEach { option ->
            val isOn = option == selected
            FilterChip(
                selected = isOn,
                onClick = { onSelect(option) },
                label = { Text(option, style = PaceDreamTypography.Callout) },
                leadingIcon = if (isOn) {
                    { Icon(PaceDreamIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PaceDreamColors.HostAccent,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = PaceDreamColors.Card,
                    labelColor = PaceDreamColors.TextPrimary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PaceDreamColors.Border,
                    selectedBorderColor = PaceDreamColors.HostAccent,
                    enabled = true,
                    selected = isOn,
                ),
            )
        }
    }
}

/**
 * Stepper row used by the Create Listing wizard's Capacity section.
 * Mirrors the visual pattern from EditListingScreen.CapacityStepperRow but
 * lives here to avoid touching the Edit flow.  Bounds enforcement is the
 * caller's responsibility — the parent state writer coerces values in range.
 */
@Composable
private fun CreateCapacityStepperRow(
    label: String,
    sublabel: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
) {
    val displayValue = value.coerceIn(minValue, maxValue)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = sublabel,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange(displayValue - 1) },
                enabled = displayValue > minValue,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Remove,
                    contentDescription = "Decrease $label",
                    tint = if (displayValue > minValue)
                        PaceDreamColors.TextPrimary
                    else
                        PaceDreamColors.Gray400,
                )
            }
            Text(
                text = displayValue.toString(),
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .widthIn(min = 32.dp)
                    .padding(horizontal = PaceDreamSpacing.SM),
            )
            IconButton(
                onClick = { onValueChange(displayValue + 1) },
                enabled = displayValue < maxValue,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Add,
                    contentDescription = "Increase $label",
                    tint = if (displayValue < maxValue)
                        PaceDreamColors.TextPrimary
                    else
                        PaceDreamColors.Gray400,
                )
            }
        }
    }
}

// ── Step: Schedule & Availability (mode-aware, iOS parity) ──

private val HOURLY_DURATION_OPTIONS = listOf(
    15 to "15 min", 30 to "30 min", 60 to "1 hr", 120 to "2 hrs",
    180 to "3 hrs", 240 to "4 hrs", 360 to "6 hrs", 480 to "8 hrs",
    720 to "12 hrs", 1440 to "24 hrs"
)

/**
 * Controls the copy used on the availability step. Wi-Fi listings frame
 * this step as "how long the access stays valid", while generic listings
 * keep the legacy "operating hours / schedule" framing.
 */
private enum class ScheduleFraming { GENERIC, WIFI_ACCESS }

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
    framing: ScheduleFraming = ScheduleFraming.GENERIC,
) {
    val isWifi = framing == ScheduleFraming.WIFI_ACCESS
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
    ) {
        // Framing banner. Wi-Fi listings surface an explicit "booking
        // duration = connection time" tie-in so the host sees the
        // relationship before touching the form.
        if (isWifi) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    .background(PaceDreamColors.HostAccent.copy(alpha = 0.08f))
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            ) {
                Icon(
                    PaceDreamIcons.Bolt,
                    contentDescription = null,
                    tint = PaceDreamColors.HostAccent,
                    modifier = Modifier.size(16.dp),
                )
                Column {
                    Text(
                        text = "Access validity",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.HostAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Each booking reveals Wi-Fi for the length the guest pays for. The options below define those windows.",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        }
        // Pricing-mode pill (shared between framings).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                .background(PaceDreamColors.HostAccent.copy(alpha = 0.08f))
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            Icon(
                PaceDreamIcons.Schedule,
                contentDescription = null,
                tint = PaceDreamColors.HostAccent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = if (isWifi) "Sold by: ${pricingUnit.displayLabel}"
                    else "Pricing mode: ${pricingUnit.displayLabel}",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.HostAccent,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── HOURLY MODE ──
        if (pricingUnit == PricingUnit.HOUR) {
            Text(
                text = if (isWifi) "Access windows" else "Available Durations",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = if (isWifi)
                    "Each chip is a bookable session. Wi-Fi stays valid for the length of whichever one the guest picks."
                else
                    "Guests can book any of the selected durations.",
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
                            selectedContainerColor = PaceDreamColors.HostAccent,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = PaceDreamColors.Card,
                            labelColor = PaceDreamColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = PaceDreamColors.Border,
                            selectedBorderColor = PaceDreamColors.HostAccent,
                            enabled = true,
                            selected = isSelected,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Text(
                text = if (isWifi) "Hosting hours" else "Operating Hours",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            if (isWifi) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = "Guests can only start a session during this window.",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                )
            }
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                        focusedBorderColor = PaceDreamColors.HostAccent,
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
                    focusedBorderColor = PaceDreamColors.HostAccent,
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
                    focusedBorderColor = PaceDreamColors.HostAccent,
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
                            .background(if (isOn) PaceDreamColors.HostAccent else PaceDreamColors.HostAccent.copy(alpha = 0.06f))
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
                focusedBorderColor = PaceDreamColors.HostAccent,
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
    hasWifi: Boolean = false,
    wifiIncluded: Boolean = false,
    wifiSsid: String = "",
    wifiShowAfterBooking: Boolean = true,
    wifiAutoQr: Boolean = true,
    wifiExtensionEnabled: Boolean = false,
    wifiExtensionPrice: String = "",
    wifiExperienceTags: List<String> = emptyList(),
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
                                    PaceDreamColors.HostAccent.copy(alpha = 0.08f),
                                    PaceDreamColors.HostAccent.copy(alpha = 0.03f)
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
                        color = PaceDreamColors.HostAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(PaceDreamRadius.Round))
                            .background(PaceDreamColors.HostAccent.copy(alpha = 0.1f))
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

        // Wi-Fi summary — only rendered for Wi-Fi listings so we do not
        // leak empty "Wi-Fi" rows into non-Wi-Fi review cards.
        if (hasWifi) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            Text(
                text = "Wi-Fi access",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            SummaryRow("Included", if (wifiIncluded) "Yes" else "Not included")
            if (wifiIncluded) {
                SummaryRow("Network", wifiSsid.ifBlank { "—" })
                SummaryRow(
                    "Reveal",
                    if (wifiShowAfterBooking) "After booking confirms"
                    else "Visible on listing",
                )
                SummaryRow(
                    "QR code",
                    if (wifiAutoQr) "Auto-generated for each booking"
                    else "Off",
                )
                if (wifiExtensionEnabled) {
                    val ext = wifiExtensionPrice.toDoubleOrNull() ?: 0.0
                    SummaryRow(
                        "Extensions",
                        "$${String.format("%.2f", ext)}/hr",
                    )
                } else {
                    SummaryRow("Extensions", "Off")
                }
                if (wifiExperienceTags.isNotEmpty()) {
                    SummaryRow("Tags", wifiExperienceTags.joinToString(", "))
                }
            }
        }
    }
}

// ── Phase 4: Publish Success (iOS: ListingPublishSuccessView) ──
// iOS parity: shows green checkmark, "Submitted!", Under Review banner,
// cover image preview, and 3 CTAs: View Listing, Go to My Listings, Back to Home.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishSuccessScreen(
    listingId: String,
    title: String,
    coverUrl: String? = null,
    onViewListing: (String) -> Unit,
    onGoToMyListings: () -> Unit,
    onBackToHome: () -> Unit,
) {
    // "View Listing" navigates to the real host listing detail screen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Green checkmark with animated entrance
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PaceDreamColors.Success.copy(alpha = 0.15f),
                            PaceDreamColors.Success.copy(alpha = 0.05f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                PaceDreamIcons.CheckCircle,
                contentDescription = null,
                tint = PaceDreamColors.Success,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Text(
            text = "Your listing is submitted!",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = title,
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 22.dp),
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        // Under Review banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.LG))
                .background(Color(0xFFFFA500).copy(alpha = 0.10f))
                .border(1.dp, Color(0xFFFFA500).copy(alpha = 0.2f), RoundedCornerShape(PaceDreamRadius.LG))
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                PaceDreamIcons.Schedule,
                contentDescription = null,
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Under Review",
                    style = PaceDreamTypography.Callout,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.TextPrimary,
                )
                Text(
                    text = "Your listing is being reviewed and will be visible once approved. We\u2019ll notify you when it\u2019s live.",
                    style = PaceDreamTypography.Caption,
                    fontWeight = FontWeight.Medium,
                    color = PaceDreamColors.TextSecondary,
                    lineHeight = PaceDreamTypography.Caption.lineHeight,
                )
            }
        }

        // Cover image preview
        if (coverUrl != null && coverUrl.startsWith("http")) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            AsyncImage(
                model = coverUrl,
                contentDescription = "Listing cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.LG)),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // What's next section
        Text(
            text = "What\u2019s next?",
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PaceDreamSpacing.MD),
        )

        // 3 CTAs: View Listing, Go to My Listings, Back to Home
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PaceDreamSpacing.XL),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            // Primary: View Listing → navigate to host listing detail
            Button(
                onClick = { onViewListing(listingId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    PaceDreamIcons.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text("View Listing", style = PaceDreamTypography.Button)
            }

            // Secondary: Go to My Listings
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
                Icon(
                    PaceDreamIcons.ListIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    "Go to My Listings",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Tertiary: Back to Home
            TextButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
            ) {
                Icon(
                    PaceDreamIcons.Home,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
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
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
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
            .background(PaceDreamColors.HostAccent.copy(alpha = 0.06f))
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
                    targetValue = if (isSelected) PaceDreamColors.HostAccent else PaceDreamColors.TextSecondary,
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
                                    PaceDreamColors.HostAccent.copy(alpha = 0.2f),
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
                    selectedContainerColor = PaceDreamColors.HostAccent,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = PaceDreamColors.Card,
                    labelColor = PaceDreamColors.TextPrimary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PaceDreamColors.Border,
                    selectedBorderColor = PaceDreamColors.HostAccent,
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
// Amenity options are now owned by the schema registry
// (see `SubcategorySchema.amenityOptions`).

// ── Wi-Fi Access Step ──────────────────────────────────────────────
//
// Design intent:
//   - The dedicated step exists because on Wi-Fi listings, the network
//     IS the product. Amenities chips were not communicating that.
//   - Guest-value framing over router terminology: tags like
//     "Work Friendly" and "Zoom Ready" ship in the search facets; the
//     host does not see "802.11ac" or "WPA2 PSK".
//   - The guest preview card mirrors the exact card we will render on
//     the post-booking confirmation screen, so hosts can see the
//     outcome of their choices in real time.
//   - Extension pricing is optional — the flow works end-to-end with
//     the toggle off. When on, the per-hour price ties directly into
//     the booking duration selected on the next step.
//   - Router integrations (Unifi, Aruba, Meraki) are intentionally out
//     of scope; the model is structured to accept a `routerIntegration`
//     block later without touching the UI.

/**
 * Guest-value experience tags shown on the Wi-Fi step. These are what
 * guests actually search and filter on — not router capability chips.
 */
private data class WifiExperienceTag(
    val label: String,
    val icon: ImageVector,
    val helper: String,
)

private val WIFI_EXPERIENCE_TAGS: List<WifiExperienceTag> = listOf(
    WifiExperienceTag(
        "Work Friendly", PaceDreamIcons.Laptop,
        "Quiet, desk-space vibe for focused work.",
    ),
    WifiExperienceTag(
        "Zoom Ready", PaceDreamIcons.Videocam,
        "Stable enough for live video meetings.",
    ),
    WifiExperienceTag(
        "High Speed Internet", PaceDreamIcons.Bolt,
        "Fast downloads, low buffering.",
    ),
    WifiExperienceTag(
        "Private Network", PaceDreamIcons.Lock,
        "Not shared with a public hotspot.",
    ),
    WifiExperienceTag(
        "Time-Limited Access", PaceDreamIcons.Schedule,
        "Only valid during the booking window.",
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WifiAccessStep(
    included: Boolean,
    ssid: String,
    password: String,
    showAfterBooking: Boolean,
    autoQr: Boolean,
    extensionEnabled: Boolean,
    extensionPrice: String,
    experienceTags: List<String>,
    pricingUnit: PricingUnit,
    selectedDurations: List<Int>,
    minStay: Int,
    minMonths: Int,
    onIncludedChange: (Boolean) -> Unit,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onShowAfterBookingChange: (Boolean) -> Unit,
    onAutoQrChange: (Boolean) -> Unit,
    onExtensionEnabledChange: (Boolean) -> Unit,
    onExtensionPriceChange: (String) -> Unit,
    onToggleExperienceTag: (String) -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG),
    ) {
        // Hero card — product framing. Makes it unambiguous that Wi-Fi
        // is the main product, not a bonus amenity.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(
                containerColor = PaceDreamColors.HostAccent.copy(alpha = 0.06f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PaceDreamColors.HostAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        PaceDreamIcons.Wifi,
                        contentDescription = null,
                        tint = PaceDreamColors.HostAccent,
                        modifier = Modifier.size(PaceDreamIconSize.MD),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wi-Fi is the product",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Guests connect the moment their booking starts. No router jargon — tell them what it feels like to use.",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── Include Wi-Fi ──
        FormSection(title = "Include Wi-Fi with every booking") {
            ToggleOptionRow(
                label = if (included) "Wi-Fi included" else "Wi-Fi not included",
                sublabel = if (included)
                    "Guests will see network details after booking."
                else
                    "Turn on if your listing ships with bookable internet.",
                checked = included,
                onCheckedChange = onIncludedChange,
            )
        }

        if (!included) return@Column  // Nothing else is relevant below.

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── Network details ──
        FormSection(title = "Network details") {
            OutlinedTextField(
                value = ssid,
                onValueChange = onSsidChange,
                label = { Text("Network name (SSID)", style = PaceDreamTypography.Callout) },
                placeholder = {
                    Text(
                        "e.g. MyCafe_5G",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                    )
                },
                leadingIcon = {
                    Icon(
                        PaceDreamIcons.Wifi,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                supportingText = {
                    Text(
                        "${ssid.trim().length}/32 characters",
                        style = PaceDreamTypography.Caption2,
                        color = PaceDreamColors.TextSecondary,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.HostAccent,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password", style = PaceDreamTypography.Callout) },
                leadingIcon = {
                    Icon(
                        PaceDreamIcons.Lock,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) PaceDreamIcons.VisibilityOff
                                else PaceDreamIcons.Visibility,
                            contentDescription = if (passwordVisible) "Hide password"
                                else "Show password",
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                visualTransformation = if (passwordVisible)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                supportingText = {
                    val len = password.length
                    val color = if (len in 1..7) PaceDreamColors.Error
                        else PaceDreamColors.TextSecondary
                    Text(
                        "At least 8 characters. Stored securely.",
                        style = PaceDreamTypography.Caption2,
                        color = color,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.HostAccent,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── Access settings ──
        FormSection(title = "Access settings") {
            ToggleOptionRow(
                label = "Reveal only after booking",
                sublabel = "Guests see the network and password after checkout completes.",
                checked = showAfterBooking,
                onCheckedChange = onShowAfterBookingChange,
            )
            ToggleOptionRow(
                label = "Auto-generate QR code",
                sublabel = "We create a one-tap QR code for every confirmed booking.",
                checked = autoQr,
                onCheckedChange = onAutoQrChange,
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── Extensions ──
        FormSection(title = "Let guests extend their session") {
            ToggleOptionRow(
                label = "Allow paid extensions",
                sublabel = "Guests can top up connection time without rebooking.",
                checked = extensionEnabled,
                onCheckedChange = onExtensionEnabledChange,
            )
            if (extensionEnabled) {
                OutlinedTextField(
                    value = extensionPrice,
                    onValueChange = onExtensionPriceChange,
                    label = {
                        Text(
                            "Extension price",
                            style = PaceDreamTypography.Callout,
                        )
                    },
                    placeholder = {
                        Text(
                            "0.00",
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary,
                        )
                    },
                    leadingIcon = {
                        Text(
                            "$",
                            style = PaceDreamTypography.Title3,
                            color = PaceDreamColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    trailingIcon = {
                        Text(
                            "/hour",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextTertiary,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.HostAccent,
                        unfocusedBorderColor = PaceDreamColors.Border,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── Experience tags (replaces weak amenity labels) ──
        FormSection(title = "What it feels like to use") {
            Text(
                text = "Pick the tags guests will see on your listing.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            ) {
                WIFI_EXPERIENCE_TAGS.forEach { tag ->
                    val isOn = experienceTags.contains(tag.label)
                    FilterChip(
                        selected = isOn,
                        onClick = { onToggleExperienceTag(tag.label) },
                        label = { Text(tag.label, style = PaceDreamTypography.Callout) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isOn) PaceDreamIcons.Check else tag.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaceDreamColors.HostAccent,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = PaceDreamColors.Card,
                            labelColor = PaceDreamColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = PaceDreamColors.Border,
                            selectedBorderColor = PaceDreamColors.HostAccent,
                            enabled = true,
                            selected = isOn,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // ── Guest preview ──
        WifiGuestPreviewCard(
            ssid = ssid,
            password = password,
            autoQr = autoQr,
            showAfterBooking = showAfterBooking,
            validityNote = formatWifiValidity(
                pricingUnit = pricingUnit,
                selectedDurations = selectedDurations,
                minStay = minStay,
                minMonths = minMonths,
            ),
        )
    }
}

/**
 * Small informational card mirroring what the guest will see after
 * booking. We render a masked password and a stylised QR placeholder
 * instead of a real QR — the real one is minted server-side at booking
 * time.
 */
@Composable
private fun WifiGuestPreviewCard(
    ssid: String,
    password: String,
    autoQr: Boolean,
    showAfterBooking: Boolean,
    validityNote: String,
) {
    Column {
        Text(
            text = "Guest preview",
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = PaceDreamSpacing.XS,
                bottom = PaceDreamSpacing.SM,
            ),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                PaceDreamColors.Border.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
            ) {
                // QR placeholder — drawn with a nested 5x5 grid so the
                // card reads like a real guest surface.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.MD))
                            .background(Color.White)
                            .border(
                                1.dp,
                                PaceDreamColors.Border.copy(alpha = 0.5f),
                                RoundedCornerShape(PaceDreamRadius.MD),
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (autoQr) {
                            WifiQrPlaceholder()
                        } else {
                            Icon(
                                PaceDreamIcons.Wifi,
                                contentDescription = null,
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    if (autoQr) {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Text(
                            text = "Scan to join",
                            style = PaceDreamTypography.Caption2,
                            color = PaceDreamColors.TextSecondary,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS),
                    ) {
                        Icon(
                            PaceDreamIcons.Wifi,
                            contentDescription = null,
                            tint = PaceDreamColors.HostAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = ssid.ifBlank { "Your network name" },
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = maskWifiPassword(password),
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            PaceDreamIcons.Schedule,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = validityNote,
                            style = PaceDreamTypography.Caption2,
                            color = PaceDreamColors.TextSecondary,
                        )
                    }
                    if (showAfterBooking) {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                PaceDreamIcons.Lock,
                                contentDescription = null,
                                tint = PaceDreamColors.TextTertiary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Unlocks after booking",
                                style = PaceDreamTypography.Caption2,
                                color = PaceDreamColors.TextTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Simple 7x7 block pattern that reads like a QR code at a glance. */
@Composable
private fun WifiQrPlaceholder() {
    // Fixed pseudo-random pattern — avoids Random() rebuilds on recomposition.
    val pattern = listOf(
        "1110111", "1000101", "1011101", "1010100",
        "0101011", "1101001", "1010111",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        pattern.forEach { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                row.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                if (ch == '1') PaceDreamColors.TextPrimary
                                else Color.Transparent
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Translates the booking duration selections into a human-readable
 * "how long the access is valid" line. This is the bridge the host
 * needs to understand that duration = connection time.
 */
private fun formatWifiValidity(
    pricingUnit: PricingUnit,
    selectedDurations: List<Int>,
    minStay: Int,
    minMonths: Int,
): String {
    return when (pricingUnit) {
        PricingUnit.HOUR -> {
            if (selectedDurations.isEmpty()) {
                "Valid for the booked session window"
            } else {
                val sorted = selectedDurations.sorted()
                val min = formatMinutes(sorted.first())
                val max = formatMinutes(sorted.last())
                if (min == max) "Valid for $min per booking"
                else "Valid for $min – $max per booking"
            }
        }
        PricingUnit.DAY, PricingUnit.WEEK ->
            "Valid for the full $minStay-day+ stay"
        PricingUnit.MONTH ->
            "Valid month-to-month ($minMonths month min.)"
    }
}

private fun formatMinutes(total: Int): String {
    return when {
        total < 60 -> "$total min"
        total % 60 == 0 -> "${total / 60} hr"
        else -> "${total / 60}h ${total % 60}m"
    }
}

/**
 * Masks a Wi-Fi password for the guest preview. Empty password shows a
 * neutral placeholder so the card still feels like a real receipt.
 */
private fun maskWifiPassword(password: String): String {
    if (password.isEmpty()) return "••••••••"
    val visibleCount = 2.coerceAtMost(password.length)
    val bullets = "•".repeat((password.length - visibleCount).coerceAtLeast(6))
    return password.take(visibleCount) + bullets
}
