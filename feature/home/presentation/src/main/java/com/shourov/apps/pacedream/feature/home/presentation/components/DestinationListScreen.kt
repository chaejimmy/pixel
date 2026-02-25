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
fun DestinationListScreen(
    onBackClick: () -> Unit,
    onDestinationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Header
        MinimalDashboardHeader(
            title = "Destinations",
            subtitle = "Explore amazing places",
            onBackClick = onBackClick
        )
        
        // Destinations Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.LG)
        ) {
            item {
                // Popular Destinations
                Text(
                    text = "Popular Destinations",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                
                val popularDestinations = listOf(
                    "New York", "London", "Paris", "Tokyo", "Sydney", "Dubai"
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(popularDestinations) { destination ->
                        PaceDreamDestinationCard(
                            name = destination,
                            imageUrl = null,
                            onClick = { onDestinationClick(destination) }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                
                // All Destinations
                Text(
                    text = "All Destinations",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary
                )
            }
            
            // Mock destinations
            val allDestinations = listOf(
                "Amsterdam", "Barcelona", "Berlin", "Boston", "Chicago", "Copenhagen",
                "Dublin", "Edinburgh", "Florence", "Hamburg", "Helsinki", "Istanbul",
                "Lisbon", "Madrid", "Milan", "Munich", "Oslo", "Prague", "Rome", "Stockholm"
            )
            
            items(allDestinations) { destination ->
                DestinationListItem(
                    destination = destination,
                    propertyCount = (10..50).random(),
                    onClick = { onDestinationClick(destination) }
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    }
}

@Composable
private fun DestinationListItem(
    destination: String,
    propertyCount: Int,
    onClick: () -> Unit,
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
            Column {
                Text(
                    text = destination,
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary
                )
                
                Text(
                    text = "$propertyCount properties available",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            Icon(
                imageVector = PaceDreamIcons.ArrowForward,
                contentDescription = "View destination",
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
    }
}
