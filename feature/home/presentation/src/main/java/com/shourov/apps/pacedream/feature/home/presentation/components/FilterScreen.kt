/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.composables.buttons.CompactProcessButton
import com.pacedream.common.composables.buttons.OutlineProcessButton
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    onBackClick: () -> Unit,
    onApplyFilters: (FilterCriteria) -> Unit,
    initial: FilterCriteria = FilterCriteria(),
    modifier: Modifier = Modifier
) {
    var selectedAmenities by remember { mutableStateOf(initial.amenities) }
    var priceRange by remember {
        mutableStateOf(
            (initial.minPrice ?: 0).toFloat()..(initial.maxPrice ?: 1000).toFloat()
        )
    }
    var selectedPropertyType by remember { mutableStateOf(initial.propertyType.orEmpty()) }
    var checkIn by remember { mutableStateOf(initial.checkInEpochDay) }
    var checkOut by remember { mutableStateOf(initial.checkOutEpochDay) }
    var adults by remember { mutableStateOf(initial.adults) }
    var children by remember { mutableStateOf(initial.children) }
    var infants by remember { mutableStateOf(initial.infants) }
    var pets by remember { mutableStateOf(initial.pets) }
    var bedrooms by remember { mutableStateOf(initial.bedrooms) }
    var beds by remember { mutableStateOf(initial.beds) }
    var bathrooms by remember { mutableStateOf(initial.bathrooms) }
    var instantBookOnly by remember { mutableStateOf(initial.instantBookOnly) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        MinimalDashboardHeader(
            title = "Filters",
            onBackClick = onBackClick,
            onActionClick = {
                selectedAmenities = emptySet()
                priceRange = 0f..1000f
                selectedPropertyType = ""
                checkIn = null
                checkOut = null
                adults = 0
                children = 0
                infants = 0
                pets = 0
                bedrooms = null
                beds = null
                bathrooms = null
                instantBookOnly = false
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.LG)
        ) {
            item { Spacer(modifier = Modifier.height(PaceDreamSpacing.MD)) }

            // ── Dates ──────────────────────────────────────────────────────
            item {
                FilterSectionHeader("Dates")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                DateRangeRow(
                    checkInEpochDay = checkIn,
                    checkOutEpochDay = checkOut,
                    onClick = { showDatePicker = true }
                )
            }

            // ── Guests ─────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                FilterSectionHeader("Guests")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                GuestStepper(
                    label = "Adults",
                    sublabel = "Ages 13 or above",
                    value = adults,
                    min = 0,
                    onChange = { adults = it }
                )
                GuestStepper(
                    label = "Children",
                    sublabel = "Ages 2–12",
                    value = children,
                    min = 0,
                    onChange = { children = it }
                )
                GuestStepper(
                    label = "Infants",
                    sublabel = "Under 2",
                    value = infants,
                    min = 0,
                    onChange = { infants = it }
                )
                GuestStepper(
                    label = "Pets",
                    sublabel = "Service animals always allowed",
                    value = pets,
                    min = 0,
                    onChange = { pets = it }
                )
            }

            // ── Property type ──────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                FilterSectionHeader("Property Type")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                val propertyTypes = listOf("Apartment", "House", "Villa", "Condo", "Studio")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    items(propertyTypes) { type ->
                        FilterChip(
                            label = type,
                            isSelected = selectedPropertyType == type,
                            onClick = {
                                selectedPropertyType = if (selectedPropertyType == type) "" else type
                            }
                        )
                    }
                }
            }

            // ── Price range ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                FilterSectionHeader("Price Range")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = "$${priceRange.start.toInt()} – $${priceRange.endInclusive.toInt()}",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
                RangeSlider(
                    value = priceRange,
                    onValueChange = { priceRange = it },
                    valueRange = 0f..2000f,
                    steps = 19
                )
            }

            // ── Rooms & beds ───────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                FilterSectionHeader("Rooms and beds")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                CountSelectorRow(
                    label = "Bedrooms",
                    value = bedrooms,
                    onChange = { bedrooms = it }
                )
                CountSelectorRow(
                    label = "Beds",
                    value = beds,
                    onChange = { beds = it }
                )
                CountSelectorRow(
                    label = "Bathrooms",
                    value = bathrooms,
                    onChange = { bathrooms = it }
                )
            }

            // ── Booking options ────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                FilterSectionHeader("Booking options")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Instant Book",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        Text(
                            text = "Listings you can book without waiting for host approval",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = instantBookOnly,
                        onCheckedChange = { instantBookOnly = it }
                    )
                }
            }

            // ── Amenities ──────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                FilterSectionHeader("Amenities")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                val amenities = listOf(
                    "WiFi", "Parking", "Pool", "Gym", "Kitchen",
                    "AC", "TV", "Pet Friendly", "Balcony", "Garden"
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    items(amenities) { amenity ->
                        AmenityChip(
                            amenity = amenity,
                            icon = when (amenity) {
                                "WiFi" -> PaceDreamIcons.Wifi
                                "Parking" -> PaceDreamIcons.LocalParking
                                "Pool" -> PaceDreamIcons.Pool
                                "Gym" -> PaceDreamIcons.FitnessCenter
                                "Kitchen" -> PaceDreamIcons.Kitchen
                                "AC" -> PaceDreamIcons.Air
                                "TV" -> PaceDreamIcons.Tv
                                "Pet Friendly" -> PaceDreamIcons.Pets
                                "Balcony" -> PaceDreamIcons.Balcony
                                "Garden" -> PaceDreamIcons.Yard
                                else -> PaceDreamIcons.Star
                            },
                            isSelected = selectedAmenities.contains(amenity),
                            onClick = {
                                selectedAmenities = if (selectedAmenities.contains(amenity)) {
                                    selectedAmenities - amenity
                                } else {
                                    selectedAmenities + amenity
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL)) }
        }

        // Apply / Cancel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.LG),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                OutlineProcessButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                    text = "Cancel",
                )

                CompactProcessButton(
                    onClick = {
                        onApplyFilters(
                            FilterCriteria(
                                checkInEpochDay = checkIn,
                                checkOutEpochDay = checkOut,
                                adults = adults,
                                children = children,
                                infants = infants,
                                pets = pets,
                                propertyType = selectedPropertyType.takeIf { it.isNotBlank() },
                                minPrice = priceRange.start.toInt().takeIf { it > 0 },
                                maxPrice = priceRange.endInclusive.toInt()
                                    .takeIf { it < 2000 },
                                bedrooms = bedrooms,
                                beds = beds,
                                bathrooms = bathrooms,
                                amenities = selectedAmenities,
                                instantBookOnly = instantBookOnly,
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    text = "Apply Filters",
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = checkIn?.let {
                LocalDate.ofEpochDay(it)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            },
            initialSelectedEndDateMillis = checkOut?.let {
                LocalDate.ofEpochDay(it)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    checkIn = state.selectedStartDateMillis?.let { millisToEpochDay(it) }
                    checkOut = state.selectedEndDateMillis?.let { millisToEpochDay(it) }
                    showDatePicker = false
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = state)
        }
    }
}

private fun millisToEpochDay(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

@Composable
private fun FilterSectionHeader(text: String) {
    Text(
        text = text,
        style = PaceDreamTypography.Title3,
        color = PaceDreamColors.TextPrimary
    )
}

@Composable
private fun DateRangeRow(
    checkInEpochDay: Long?,
    checkOutEpochDay: Long?,
    onClick: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val label = when {
        checkInEpochDay != null && checkOutEpochDay != null ->
            "${LocalDate.ofEpochDay(checkInEpochDay).format(formatter)} – ${LocalDate.ofEpochDay(checkOutEpochDay).format(formatter)}"
        checkInEpochDay != null -> "From ${LocalDate.ofEpochDay(checkInEpochDay).format(formatter)}"
        else -> "Add dates"
    }
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = PaceDreamIcons.Calendar,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        Text(label)
    }
}

@Composable
private fun GuestStepper(
    label: String,
    sublabel: String,
    value: Int,
    min: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = sublabel,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
        StepperButton(
            icon = PaceDreamIcons.Remove,
            enabled = value > min,
            onClick = { if (value > min) onChange(value - 1) }
        )
        Text(
            text = value.toString(),
            style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.SemiBold),
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
        )
        StepperButton(
            icon = PaceDreamIcons.Add,
            enabled = true,
            onClick = { onChange(value + 1) }
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
        shape = CircleShape
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CountSelectorRow(
    label: String,
    value: Int?,
    onChange: (Int?) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = PaceDreamSpacing.SM)) {
        Text(
            text = label,
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
            val options = listOf<Pair<String, Int?>>(
                "Any" to null,
                "1" to 1,
                "2" to 2,
                "3" to 3,
                "4" to 4,
                "5+" to 5,
            )
            items(options) { (text, count) ->
                FilterChip(
                    label = text,
                    isSelected = value == count,
                    onClick = { onChange(count) }
                )
            }
        }
    }
}
