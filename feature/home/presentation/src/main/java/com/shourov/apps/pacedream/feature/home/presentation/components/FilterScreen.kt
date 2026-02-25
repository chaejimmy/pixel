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
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*

@Composable
fun FilterScreen(
    onBackClick: () -> Unit,
    onApplyFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var priceRange by remember { mutableStateOf(0f..1000f) }
    var selectedPropertyType by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Header
        MinimalDashboardHeader(
            title = "Filters",
            onBackClick = onBackClick,
            onActionClick = { /* Clear all filters */ }
        )
        
        // Filter Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.LG)
        ) {
            item {
                // Property Type
                Text(
                    text = "Property Type",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                
                val propertyTypes = listOf("Apartment", "House", "Villa", "Condo", "Studio")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
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
            
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                
                // Price Range
                Text(
                    text = "Price Range",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                
                Text(
                    text = "$${priceRange.start.toInt()} - $${priceRange.endInclusive.toInt()}",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
                
                // Price range slider would go here
                // RangeSlider(
                //     value = priceRange,
                //     onValueChange = { priceRange = it },
                //     valueRange = 0f..2000f,
                //     steps = 19
                // )
            }
            
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                
                // Amenities
                Text(
                    text = "Amenities",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                
                val amenities = listOf(
                    "WiFi", "Parking", "Pool", "Gym", "Kitchen", 
                    "AC", "TV", "Pet Friendly", "Balcony", "Garden"
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
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
                                val newAmenities = selectedAmenities.toMutableSet()
                                if (newAmenities.contains(amenity)) {
                                    newAmenities.remove(amenity)
                                } else {
                                    newAmenities.add(amenity)
                                }
                                selectedAmenities = newAmenities
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            }
        }
        
        // Apply Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
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
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onApplyFilters,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    }
}
