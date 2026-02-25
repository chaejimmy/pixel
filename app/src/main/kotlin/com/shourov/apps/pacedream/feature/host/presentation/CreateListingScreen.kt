package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * Listing mode determines which categories and flow to show.
 * SHARE / BORROW: item-based listings (Tools, Games, Toys, Micromobility, Other)
 * USE: space-based listings (Apartment, Office, Event Space, etc.)
 */
enum class ListingMode(val displayName: String) {
    SHARE("Share"),
    BORROW("Borrow"),
    USE("Use"),
}

/**
 * Duration options for Schedule & Availability.
 * Platform-optimized for short-term sharing and borrowing.
 */
private data class DurationOption(
    val id: String,
    val label: String,
    val description: String,
)

private val SHARE_BORROW_DURATIONS = listOf(
    DurationOption("1h", "1 hour", "Quick use"),
    DurationOption("2h", "2 hours", "Short session"),
    DurationOption("4h", "4 hours", "Half day"),
    DurationOption("12h", "12 hours", "Half day+"),
    DurationOption("24h", "24 hours", "Full day"),
    DurationOption("2d", "2 days", "Weekend"),
    DurationOption("3d", "3 days", "Long weekend"),
    DurationOption("1w", "1 week", "Weekly"),
    DurationOption("2w", "2 weeks", "Bi-weekly"),
    DurationOption("1m", "1 month", "Monthly"),
)

private val USE_DURATIONS = listOf(
    DurationOption("1h", "1 hour", "Quick use"),
    DurationOption("2h", "2 hours", "Short session"),
    DurationOption("4h", "4 hours", "Half day"),
    DurationOption("12h", "12 hours", "Half day+"),
    DurationOption("24h", "24 hours", "Full day"),
    DurationOption("2d", "2 days", "Weekend"),
    DurationOption("3d", "3 days", "Long weekend"),
    DurationOption("1w", "1 week", "Weekly"),
    DurationOption("1m", "1 month", "Monthly"),
)

/**
 * iOS-style multi-step Create Listing screen
 * Follows Apple HIG grouped inset form pattern with step-by-step wizard flow.
 *
 * Flow for SHARE / BORROW (items):
 *   1. Category  →  2. Details & Photos  →  3. Pricing & Schedule  →  4. Review
 *
 * Flow for USE (spaces):
 *   1. Property Type  →  2. Location  →  3. Details & Photos  →  4. Pricing & Schedule  →  5. Review
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    listingMode: ListingMode = ListingMode.SHARE,
    onBackClick: () -> Unit = {},
    onPublishSuccess: (String) -> Unit = {}
) {
    val totalSteps = if (listingMode == ListingMode.USE) 5 else 4
    var currentStep by remember { mutableIntStateOf(0) }

    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var basePrice by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var isInstantBook by remember { mutableStateOf(false) }
    var itemCondition by remember { mutableStateOf("") }
    val selectedDurations = remember { mutableStateListOf<String>() }
    var showMoreDurations by remember { mutableStateOf(false) }

    val stepTitles = if (listingMode == ListingMode.USE) {
        listOf("Property Type", "Location", "Details & Photos", "Pricing & Schedule", "Review")
    } else {
        listOf("Category", "Details & Photos", "Pricing & Schedule", "Review")
    }

    val progress = (currentStep + 1).toFloat() / totalSteps

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stepTitles[currentStep],
                        style = PaceDreamTypography.Headline,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onBackClick()
                    }) {
                        Icon(
                            imageVector = if (currentStep > 0) PaceDreamIcons.ArrowBack else PaceDreamIcons.Close,
                            contentDescription = if (currentStep > 0) "Back" else "Close"
                        )
                    }
                },
                actions = {
                    Text(
                        text = "Step ${currentStep + 1} of $totalSteps",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        modifier = Modifier.padding(end = PaceDreamSpacing.MD)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        bottomBar = {
            CreateListingBottomBar(
                currentStep = currentStep,
                totalSteps = totalSteps,
                canProceed = when {
                    // Step 0 is always Category/Property Type selection
                    currentStep == 0 -> propertyType.isNotBlank()
                    // USE mode has Location as step 1
                    listingMode == ListingMode.USE && currentStep == 1 ->
                        city.isNotBlank() && country.isNotBlank()
                    // Details step: USE=2, SHARE/BORROW=1
                    (listingMode == ListingMode.USE && currentStep == 2) ||
                    (listingMode != ListingMode.USE && currentStep == 1) ->
                        title.isNotBlank() && description.isNotBlank()
                    // Pricing & Schedule step: USE=3, SHARE/BORROW=2
                    (listingMode == ListingMode.USE && currentStep == 3) ||
                    (listingMode != ListingMode.USE && currentStep == 2) ->
                        basePrice.isNotBlank() && selectedDurations.isNotEmpty()
                    // Review step is always last
                    currentStep == totalSteps - 1 -> true
                    else -> false
                },
                onBack = { if (currentStep > 0) currentStep-- },
                onNext = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        onPublishSuccess("new-listing-id")
                    }
                }
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress bar - iOS style thin
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
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
                label = "step_transition"
            ) { step ->
                if (listingMode == ListingMode.USE) {
                    // USE mode: 5-step flow
                    when (step) {
                        0 -> PropertyTypeStep(
                            listingMode = listingMode,
                            selectedType = propertyType,
                            onTypeSelected = { propertyType = it }
                        )
                        1 -> LocationStep(
                            address = address,
                            city = city,
                            state = state,
                            country = country,
                            zipCode = zipCode,
                            onAddressChange = { address = it },
                            onCityChange = { city = it },
                            onStateChange = { state = it },
                            onCountryChange = { country = it },
                            onZipCodeChange = { zipCode = it },
                        )
                        2 -> DetailsStep(
                            listingMode = listingMode,
                            title = title,
                            description = description,
                            itemCondition = itemCondition,
                            onTitleChange = { title = it },
                            onDescriptionChange = { description = it },
                            onItemConditionChange = { itemCondition = it },
                        )
                        3 -> PricingScheduleStep(
                            listingMode = listingMode,
                            basePrice = basePrice,
                            currency = currency,
                            isInstantBook = isInstantBook,
                            selectedDurations = selectedDurations,
                            showMoreDurations = showMoreDurations,
                            onBasePriceChange = { basePrice = it },
                            onCurrencyChange = { currency = it },
                            onInstantBookChange = { isInstantBook = it },
                            onToggleDuration = { id ->
                                if (selectedDurations.contains(id)) selectedDurations.remove(id)
                                else selectedDurations.add(id)
                            },
                            onShowMoreDurations = { showMoreDurations = true },
                        )
                        4 -> ReviewStep(
                            listingMode = listingMode,
                            title = title,
                            propertyType = propertyType,
                            location = "$city, $state, $country",
                            price = basePrice,
                            currency = currency,
                            selectedDurations = selectedDurations,
                            isInstantBook = isInstantBook,
                            itemCondition = itemCondition,
                        )
                    }
                } else {
                    // SHARE / BORROW mode: 4-step flow
                    when (step) {
                        0 -> PropertyTypeStep(
                            listingMode = listingMode,
                            selectedType = propertyType,
                            onTypeSelected = { propertyType = it }
                        )
                        1 -> DetailsStep(
                            listingMode = listingMode,
                            title = title,
                            description = description,
                            itemCondition = itemCondition,
                            onTitleChange = { title = it },
                            onDescriptionChange = { description = it },
                            onItemConditionChange = { itemCondition = it },
                        )
                        2 -> PricingScheduleStep(
                            listingMode = listingMode,
                            basePrice = basePrice,
                            currency = currency,
                            isInstantBook = isInstantBook,
                            selectedDurations = selectedDurations,
                            showMoreDurations = showMoreDurations,
                            onBasePriceChange = { basePrice = it },
                            onCurrencyChange = { currency = it },
                            onInstantBookChange = { isInstantBook = it },
                            onToggleDuration = { id ->
                                if (selectedDurations.contains(id)) selectedDurations.remove(id)
                                else selectedDurations.add(id)
                            },
                            onShowMoreDurations = { showMoreDurations = true },
                        )
                        3 -> ReviewStep(
                            listingMode = listingMode,
                            title = title,
                            propertyType = propertyType,
                            location = if (city.isNotBlank()) "$city, $state, $country" else "",
                            price = basePrice,
                            currency = currency,
                            selectedDurations = selectedDurations,
                            isInstantBook = isInstantBook,
                            itemCondition = itemCondition,
                        )
                    }
                }
            }
        }
    }
}

// ── Step 1: Category / Property Type ─────────────────────────

@Composable
private fun PropertyTypeStep(
    listingMode: ListingMode,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val types = when (listingMode) {
        ListingMode.SHARE, ListingMode.BORROW -> listOf(
            PropertyTypeOption("Tools", PaceDreamIcons.Build, "Power tools, hand tools, garden equipment"),
            PropertyTypeOption("Games", PaceDreamIcons.SportsEsports, "Board games, video games, consoles"),
            PropertyTypeOption("Toys", PaceDreamIcons.SmartToy, "Kids toys, outdoor play, collectibles"),
            PropertyTypeOption("Micromobility", PaceDreamIcons.DirectionsBike, "E-scooters, bikes, skateboards"),
            PropertyTypeOption("Other", PaceDreamIcons.MoreHoriz, "Anything else you want to list"),
        )
        ListingMode.USE -> listOf(
            PropertyTypeOption("Apartment", PaceDreamIcons.Home, "A unit within a building"),
            PropertyTypeOption("House", PaceDreamIcons.Home, "An entire house"),
            PropertyTypeOption("Studio", PaceDreamIcons.Home, "A single-room dwelling"),
            PropertyTypeOption("Loft", PaceDreamIcons.Home, "An open-plan space"),
            PropertyTypeOption("Office", PaceDreamIcons.Home, "A workspace for rent"),
            PropertyTypeOption("Event Space", PaceDreamIcons.Home, "A venue for events"),
            PropertyTypeOption("Parking", PaceDreamIcons.Home, "A parking spot"),
        )
    }

    val headerText = when (listingMode) {
        ListingMode.SHARE -> "What are you sharing?"
        ListingMode.BORROW -> "What can people borrow?"
        ListingMode.USE -> "What type of space are you listing?"
    }

    val subtitleText = when (listingMode) {
        ListingMode.SHARE, ListingMode.BORROW -> "Pick the category that best fits your item."
        ListingMode.USE -> "Choose the category that best describes your space."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG)
    ) {
        Text(
            text = headerText,
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = subtitleText,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        types.forEach { type ->
            val isSelected = selectedType == type.name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaceDreamSpacing.SM)
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            PaceDreamColors.Primary,
                            RoundedCornerShape(PaceDreamRadius.LG)
                        ) else Modifier
                    ),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.06f)
                    else PaceDreamColors.Card
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                onClick = { onTypeSelected(type.name) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.MD))
                            .background(
                                if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.12f)
                                else PaceDreamColors.Gray100
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = type.name,
                            tint = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.name,
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary,
                        )
                        Text(
                            text = type.description,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                        )
                    }
                    if (isSelected) {
                        Icon(
                            PaceDreamIcons.Check,
                            contentDescription = "Selected",
                            tint = PaceDreamColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Location Step (USE mode only) ────────────────────────────

@Composable
private fun LocationStep(
    address: String,
    city: String,
    state: String,
    country: String,
    zipCode: String,
    onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onZipCodeChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Where is your property?",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Guests will only get the exact address after booking.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        FormSection(title = "Address") {
            FormTextField(
                value = address,
                onValueChange = onAddressChange,
                label = "Street address",
            )
            FormTextField(
                value = city,
                onValueChange = onCityChange,
                label = "City",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                FormTextField(
                    value = state,
                    onValueChange = onStateChange,
                    label = "State / Province",
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = zipCode,
                    onValueChange = onZipCodeChange,
                    label = "ZIP Code",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                )
            }
            FormTextField(
                value = country,
                onValueChange = onCountryChange,
                label = "Country",
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        // Map placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        PaceDreamIcons.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Text(
                        "Pin your location on the map",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
        }
    }
}

// ── Details & Photos Step ────────────────────────────────────

@Composable
private fun DetailsStep(
    listingMode: ListingMode,
    title: String,
    description: String,
    itemCondition: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onItemConditionChange: (String) -> Unit,
) {
    val isItemListing = listingMode == ListingMode.SHARE || listingMode == ListingMode.BORROW

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG)
    ) {
        Text(
            text = if (isItemListing) "Describe your item" else "Tell us about your space",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = if (isItemListing)
                "A clear title and description help people find your listing."
            else
                "A great title and description help guests find your listing.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        FormSection(title = "Listing Info") {
            FormTextField(
                value = title,
                onValueChange = onTitleChange,
                label = "Title",
                placeholder = if (isItemListing) "e.g. DeWalt Power Drill Set" else "e.g. Cozy Downtown Studio",
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description", style = PaceDreamTypography.Callout) },
                placeholder = {
                    Text(
                        if (isItemListing)
                            "Describe the item, what's included, and any usage notes..."
                        else
                            "Describe what makes your space special...",
                        style = PaceDreamTypography.Body
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )

            // Item condition (share/borrow only)
            if (isItemListing) {
                ItemConditionSelector(
                    selectedCondition = itemCondition,
                    onConditionSelected = onItemConditionChange,
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Photo upload section
        FormSection(title = "Photos") {
            Text(
                if (isItemListing)
                    "Add photos from different angles to show condition."
                else
                    "Add at least 5 photos to showcase your space.",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                PhotoUploadPlaceholder(onClick = { /* TODO: open image picker */ })
                PhotoUploadPlaceholder(onClick = {})
                PhotoUploadPlaceholder(onClick = {})
            }
        }
    }
}

// ── Item Condition Selector ──────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemConditionSelector(
    selectedCondition: String,
    onConditionSelected: (String) -> Unit,
) {
    val conditions = listOf("New", "Like New", "Good", "Fair")

    Column {
        Text(
            text = "Condition",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            conditions.forEach { condition ->
                val isSelected = selectedCondition == condition
                FilterChip(
                    selected = isSelected,
                    onClick = { onConditionSelected(condition) },
                    label = { Text(condition, style = PaceDreamTypography.Callout) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                PaceDreamIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
    }
}

// ── Pricing & Schedule Step ──────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PricingScheduleStep(
    listingMode: ListingMode,
    basePrice: String,
    currency: String,
    isInstantBook: Boolean,
    selectedDurations: List<String>,
    showMoreDurations: Boolean,
    onBasePriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onInstantBookChange: (Boolean) -> Unit,
    onToggleDuration: (String) -> Unit,
    onShowMoreDurations: () -> Unit,
) {
    val allDurations = if (listingMode == ListingMode.USE) USE_DURATIONS else SHARE_BORROW_DURATIONS
    // Show first 5 by default, all when expanded
    val visibleDurations = if (showMoreDurations) allDurations else allDurations.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG)
    ) {
        // Pricing
        Text(
            text = "Set your price",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        FormSection(title = "Pricing") {
            FormTextField(
                value = basePrice,
                onValueChange = onBasePriceChange,
                label = "Base price ($currency)",
                keyboardType = KeyboardType.Decimal,
                placeholder = "0.00",
            )

            FormToggleRow(
                title = "Instant Book",
                subtitle = "People can book without waiting for approval",
                checked = isInstantBook,
                onCheckedChange = onInstantBookChange,
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Schedule & Availability
        Text(
            text = "Schedule & Availability",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Select the durations you want to offer. You can set different prices for each later.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            visibleDurations.forEach { duration ->
                val isSelected = selectedDurations.contains(duration.id)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleDuration(duration.id) },
                    label = {
                        Column {
                            Text(
                                duration.label,
                                style = PaceDreamTypography.Callout,
                            )
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                PaceDreamIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

        // "+ More durations" button
        if (!showMoreDurations) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            TextButton(onClick = onShowMoreDurations) {
                Icon(
                    PaceDreamIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PaceDreamColors.Primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "More durations",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (selectedDurations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(
                    containerColor = PaceDreamColors.Primary.copy(alpha = 0.06f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    Text(
                        text = "Selected durations",
                        style = PaceDreamTypography.Footnote,
                        color = PaceDreamColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    val selectedLabels = allDurations
                        .filter { selectedDurations.contains(it.id) }
                        .joinToString(" · ") { it.label }
                    Text(
                        text = selectedLabels,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                    )
                }
            }
        }
    }
}

// ── Review Step ──────────────────────────────────────────────

@Composable
private fun ReviewStep(
    listingMode: ListingMode,
    title: String,
    propertyType: String,
    location: String,
    price: String,
    currency: String,
    selectedDurations: List<String>,
    isInstantBook: Boolean,
    itemCondition: String,
) {
    val isItemListing = listingMode == ListingMode.SHARE || listingMode == ListingMode.BORROW
    val allDurations = if (listingMode == ListingMode.USE) USE_DURATIONS else SHARE_BORROW_DURATIONS
    val durationLabels = allDurations
        .filter { selectedDurations.contains(it.id) }
        .joinToString(" · ") { it.label }

    // Determine the shortest duration for price display
    val shortestDuration = allDurations
        .firstOrNull { selectedDurations.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Review your listing",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Make sure everything looks good before publishing.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Preview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.XL),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(PaceDreamColors.Gray100),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        PaceDreamIcons.CameraAlt,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Text(
                        "Add photos to see preview",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
            }

            Column(modifier = Modifier.padding(PaceDreamSpacing.LG)) {
                Text(
                    text = title.ifBlank { "Untitled Listing" },
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = buildString {
                        append(propertyType)
                        if (location.isNotBlank()) append(" · $location")
                        if (isItemListing && itemCondition.isNotBlank()) append(" · $itemCondition")
                    },
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currency $price",
                        style = PaceDreamTypography.Title2,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (shortestDuration != null) {
                        Text(
                            text = " / ${shortestDuration.label}",
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Duration summary
        ReviewSummarySection(title = "Available Durations") {
            if (durationLabels.isBlank()) {
                Text("No durations selected", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
            } else {
                Text(
                    text = durationLabels,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                )
            }
        }

        // Condition (item listings only)
        if (isItemListing && itemCondition.isNotBlank()) {
            ReviewSummarySection(title = "Condition") {
                Text(
                    text = itemCondition,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                )
            }
        }

        ReviewSummarySection(title = "Booking") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isInstantBook) "Instant Book enabled" else "Host approval required",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                )
            }
        }

        // Listing type badge
        ReviewSummarySection(title = "Listing Type") {
            Text(
                text = listingMode.displayName,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextPrimary,
            )
        }
    }
}

// ── Shared Components ──────────────────────────────────────────

@Composable
private fun CreateListingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Card.copy(alpha = PaceDreamGlass.ThickAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PaceDreamSpacing.LG,
                    vertical = PaceDreamSpacing.MD
                )
                .padding(
                    bottom = WindowInsets.systemBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                ),
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
                enabled = canProceed,
                modifier = Modifier.height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = if (currentStep == totalSteps - 1) "Publish Listing" else "Continue",
                    style = PaceDreamTypography.Button,
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = PaceDreamSpacing.XS,
                bottom = PaceDreamSpacing.SM
            )
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
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = PaceDreamTypography.Callout) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) }
        } else null,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PaceDreamColors.Primary,
            unfocusedBorderColor = PaceDreamColors.Border,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable
private fun FormToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextPrimary,
            )
            Text(
                text = subtitle,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PaceDreamColors.Success,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = PaceDreamColors.Gray100,
            ),
        )
    }
}

@Composable
private fun PhotoUploadPlaceholder(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                PaceDreamIcons.Add,
                contentDescription = "Add photo",
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ReviewSummarySection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = PaceDreamSpacing.LG)) {
        Text(
            text = title,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                content()
            }
        }
    }
}

private data class PropertyTypeOption(
    val name: String,
    val icon: ImageVector,
    val description: String,
)
