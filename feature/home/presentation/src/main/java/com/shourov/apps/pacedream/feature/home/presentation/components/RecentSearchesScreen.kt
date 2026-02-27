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
import androidx.compose.foundation.lazy.items
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*

@Composable
fun RecentSearchesScreen(
    onBackClick: () -> Unit,
    onSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // No fake seed data: this starts empty until we wire persistent history.
    // (Keeping UI + clear/remove affordances for future integration.)
    var recentSearches by rememberSaveable { mutableStateOf(emptyList<String>()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Header
        MinimalDashboardHeader(
            title = "Recent Searches",
            subtitle = "Your search history",
            onBackClick = onBackClick,
            onActionClick = { 
                recentSearches = emptyList() // Clear all searches
            }
        )
        
        // Recent Searches Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.LG)
        ) {
            if (recentSearches.isEmpty()) {
                item {
                    PaceDreamEmptyState(
                        title = "No Recent Searches",
                        description = "Search for a city or listing and it will appear here.",
                        icon = PaceDreamIcons.Search
                    )
                }
            } else {
                item {
                    Text(
                        text = "Recent Searches",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                }
                
                items(recentSearches) { search ->
                    RecentSearchItem(
                        location = search,
                        onClick = { onSearchClick(search) },
                        onRemoveClick = {
                            recentSearches = recentSearches.filter { it != search }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                }
            }
        }
    }
}

@Composable
private fun RecentSearchItem(
    location: String,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.History,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
                
                Text(
                    text = location,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary
                )
            }
            
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Close,
                    contentDescription = "Remove search",
                    tint = PaceDreamColors.TextTertiary,
                    modifier = Modifier.size(PaceDreamIconSize.XS)
                )
            }
        }
    }
}
