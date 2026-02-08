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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
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
 * iOS-style multi-step Create Listing screen
 * Follows Apple HIG grouped inset form pattern with step-by-step wizard flow
 */

private const val TOTAL_STEPS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBackClick: () -> Unit = {},
    onPublishSuccess: (String) -> Unit = {}
) {
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
    var isHourly by remember { mutableStateOf(true) }
    val selectedAmenities = remember { mutableStateListOf<String>() }
    var maxGuests by remember { mutableStateOf("") }
    var isInstantBook by remember { mutableStateOf(false) }

    val stepTitles = listOf(
        "Property Type",
        "Location",
        "Details",
        "Amenities & Pricing",
        "Review & Publish"
    )

    val progress = (currentStep + 1).toFloat() / TOTAL_STEPS

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
                            imageVector = if (currentStep > 0) Icons.Default.ArrowBack else Icons.Default.Close,
                            contentDescription = if (currentStep > 0) "Back" else "Close"
                        )
                    }
                },
                actions = {
                    Text(
                        text = "Step ${currentStep + 1} of $TOTAL_STEPS",
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
                totalSteps = TOTAL_STEPS,
                canProceed = when (currentStep) {
                    0 -> propertyType.isNotBlank()
                    1 -> city.isNotBlank() && country.isNotBlank()
                    2 -> title.isNotBlank() && description.isNotBlank()
                    3 -> basePrice.isNotBlank()
                    4 -> true
                    else -> false
                },
                onBack = { if (currentStep > 0) currentStep-- },
                onNext = {
                    if (currentStep < TOTAL_STEPS - 1) {
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
                when (step) {
                    0 -> PropertyTypeStep(
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
                        title = title,
                        description = description,
                        maxGuests = maxGuests,
                        onTitleChange = { title = it },
                        onDescriptionChange = { description = it },
                        onMaxGuestsChange = { maxGuests = it },
                    )
                    3 -> AmenitiesPricingStep(
                        selectedAmenities = selectedAmenities,
                        onToggleAmenity = { amenity ->
                            if (selectedAmenities.contains(amenity)) selectedAmenities.remove(amenity)
                            else selectedAmenities.add(amenity)
                        },
                        basePrice = basePrice,
                        currency = currency,
                        isHourly = isHourly,
                        isInstantBook = isInstantBook,
                        onBasePriceChange = { basePrice = it },
                        onCurrencyChange = { currency = it },
                        onHourlyChange = { isHourly = it },
                        onInstantBookChange = { isInstantBook = it },
                    )
                    4 -> ReviewStep(
                        title = title,
                        propertyType = propertyType,
                        location = "$city, $state, $country",
                        price = basePrice,
                        currency = currency,
                        isHourly = isHourly,
                        amenities = selectedAmenities,
                        isInstantBook = isInstantBook,
                    )
                }
            }
        }
    }
}

// ── Step 1: Property Type ──────────────────────────────────────

@Composable
private fun PropertyTypeStep(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val types = listOf(
        PropertyTypeOption("Apartment", Icons.Default.Home, "A unit within a building"),
        PropertyTypeOption("House", Icons.Default.Home, "An entire house"),
        PropertyTypeOption("Studio", Icons.Default.Home, "A single-room dwelling"),
        PropertyTypeOption("Loft", Icons.Default.Home, "An open-plan space"),
        PropertyTypeOption("Villa", Icons.Default.Home, "A luxury standalone"),
        PropertyTypeOption("Office", Icons.Default.Home, "A workspace for rent"),
        PropertyTypeOption("Event Space", Icons.Default.Home, "A venue for events"),
        PropertyTypeOption("Parking", Icons.Default.Home, "A parking spot"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG)
    ) {
        Text(
            text = "What type of property are you listing?",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Choose the category that best describes your space.",
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
                            Icons.Default.Check,
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

// ── Step 2: Location ───────────────────────────────────────────

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

        // iOS grouped inset form style
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
                        Icons.Default.LocationOn,
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

// ── Step 3: Details ────────────────────────────────────────────

@Composable
private fun DetailsStep(
    title: String,
    description: String,
    maxGuests: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMaxGuestsChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Tell us about your space",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "A great title and description help guests find your listing.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        FormSection(title = "Listing Info") {
            FormTextField(
                value = title,
                onValueChange = onTitleChange,
                label = "Title",
                placeholder = "e.g. Cozy Downtown Studio",
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description", style = PaceDreamTypography.Callout) },
                placeholder = { Text("Describe what makes your space special...", style = PaceDreamTypography.Body) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )
            FormTextField(
                value = maxGuests,
                onValueChange = onMaxGuestsChange,
                label = "Maximum guests",
                keyboardType = KeyboardType.Number,
                placeholder = "e.g. 4",
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Photo upload section
        FormSection(title = "Photos") {
            Text(
                "Add at least 5 photos to showcase your space.",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                // Upload button
                PhotoUploadPlaceholder(
                    onClick = { /* TODO: open image picker */ }
                )
                PhotoUploadPlaceholder(onClick = {})
                PhotoUploadPlaceholder(onClick = {})
            }
        }
    }
}

// ── Step 4: Amenities & Pricing ────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesPricingStep(
    selectedAmenities: List<String>,
    onToggleAmenity: (String) -> Unit,
    basePrice: String,
    currency: String,
    isHourly: Boolean,
    isInstantBook: Boolean,
    onBasePriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onHourlyChange: (Boolean) -> Unit,
    onInstantBookChange: (Boolean) -> Unit,
) {
    val allAmenities = listOf(
        "Wi-Fi", "Air Conditioning", "Heating", "Kitchen",
        "Washer", "Dryer", "Free Parking", "Pool",
        "Hot Tub", "Gym", "Elevator", "Smoke Detector",
        "First Aid Kit", "Fire Extinguisher", "TV", "Workspace",
        "Pet Friendly", "EV Charger", "Security Camera", "Doorman"
    )

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

            // Hourly vs Daily toggle - iOS grouped row style
            FormToggleRow(
                title = "Hourly pricing",
                subtitle = "Charge guests per hour instead of per day",
                checked = isHourly,
                onCheckedChange = onHourlyChange,
            )

            FormToggleRow(
                title = "Instant Book",
                subtitle = "Guests can book without waiting for approval",
                checked = isInstantBook,
                onCheckedChange = onInstantBookChange,
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Amenities
        Text(
            text = "What amenities do you offer?",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Select all that apply.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            allAmenities.forEach { amenity ->
                val isSelected = selectedAmenities.contains(amenity)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleAmenity(amenity) },
                    label = {
                        Text(
                            amenity,
                            style = PaceDreamTypography.Callout,
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Default.Check,
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

// ── Step 5: Review ─────────────────────────────────────────────

@Composable
private fun ReviewStep(
    title: String,
    propertyType: String,
    location: String,
    price: String,
    currency: String,
    isHourly: Boolean,
    amenities: List<String>,
    isInstantBook: Boolean,
) {
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
                        Icons.Default.CameraAlt,
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
                    text = "$propertyType · $location",
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
                    Text(
                        text = if (isHourly) " / hour" else " / day",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        // Summary sections
        ReviewSummarySection(title = "Amenities") {
            if (amenities.isEmpty()) {
                Text("No amenities selected", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
            } else {
                Text(
                    text = amenities.joinToString(" · "),
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
                Icons.Default.Add,
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
