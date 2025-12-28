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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Header
        MinimalDashboardHeader(
            title = "Search Properties",
            onBackClick = onBackClick,
            onActionClick = { /* Handle filter */ }
        )
        
        // Search Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.LG)
        ) {
            item {
                // Search Bar
                PaceDreamSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearchClick = { /* Handle search */ },
                    onFilterClick = { /* Handle filter */ },
                    placeholder = "Search properties, locations..."
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                
                // Filter Chips
                val filters = listOf("WiFi", "Parking", "Pool", "Gym", "Pet Friendly")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            label = filter,
                            isSelected = selectedFilters.contains(filter),
                            onClick = {
                                val newFilters = selectedFilters.toMutableSet()
                                if (newFilters.contains(filter)) {
                                    newFilters.remove(filter)
                                } else {
                                    newFilters.add(filter)
                                }
                                selectedFilters = newFilters
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                
                // Search Results
                Text(
                    text = "Search Results",
                    style = PaceDreamTypography.Title2,
                    color = PaceDreamColors.TextPrimary
                )
            }
            
            // Mock search results
            items(10) { index ->
                SearchResultItem(
                    title = "Property ${index + 1}",
                    subtitle = "Location ${index + 1}",
                    icon = Icons.Default.Home,
                    onClick = { onPropertyClick("property_$index") }
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    }
}
