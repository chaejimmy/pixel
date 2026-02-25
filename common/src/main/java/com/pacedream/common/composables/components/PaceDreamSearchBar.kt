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

package com.pacedream.common.composables.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

@Composable
fun PaceDreamSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    placeholder: String = "Search properties, locations...",
    showFilter: Boolean = true,
    showVoice: Boolean = false,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            // Search Icon
            Icon(
                imageVector = PaceDreamIcons.Search,
                contentDescription = "Search",
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(PaceDreamIconSize.MD)
            )
            
            // Search Field
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { 
                    Text(
                        text = placeholder,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextTertiary
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = isEnabled,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = PaceDreamColors.TextPrimary,
                    unfocusedTextColor = PaceDreamColors.TextPrimary
                ),
                textStyle = PaceDreamTypography.Callout,
                singleLine = true
            )
            
            // Voice Search Button
            if (showVoice) {
                IconButton(
                    onClick = onVoiceClick,
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Mic,
                        contentDescription = "Voice search",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
            }
            
            // Filter Button
            if (showFilter) {
                IconButton(
                    onClick = onFilterClick,
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.FilterList,
                        contentDescription = "Filter",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { 
            Text(
                text = placeholder,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextTertiary
            )
        },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = PaceDreamIcons.Search,
                contentDescription = "Search",
                tint = PaceDreamColors.TextSecondary
            )
        },
        trailingIcon = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = PaceDreamIcons.ArrowForward,
                    contentDescription = "Search",
                    tint = PaceDreamColors.Primary
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PaceDreamColors.Primary,
            unfocusedBorderColor = PaceDreamColors.Gray300,
            focusedTextColor = PaceDreamColors.TextPrimary,
            unfocusedTextColor = PaceDreamColors.TextPrimary
        ),
        textStyle = PaceDreamTypography.Callout,
        singleLine = true,
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    )
}

@Composable
fun SearchBarWithSuggestions(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    placeholder: String = "Search properties, locations...",
    modifier: Modifier = Modifier
) {
    var showSuggestions by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Search Bar
        PaceDreamSearchBar(
            query = query,
            onQueryChange = { newQuery ->
                onQueryChange(newQuery)
                showSuggestions = newQuery.isNotEmpty() && suggestions.isNotEmpty()
            },
            onSearchClick = onSearchClick,
            onFilterClick = onFilterClick,
            placeholder = placeholder
        )
        
        // Suggestions Dropdown
        if (showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PaceDreamSpacing.XS),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Column {
                    suggestions.take(5).forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onSuggestionClick(suggestion)
                                    showSuggestions = false
                                }
                                .padding(PaceDreamSpacing.MD),
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
                                text = suggestion,
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Gray100
        ),
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption,
            color = if (isSelected) Color.White else PaceDreamColors.TextSecondary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.SM
            )
        )
    }
}

@Composable
fun FilterChipGroup(
    filters: List<String>,
    selectedFilters: Set<String>,
    onFilterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(filters) { filter ->
            FilterChip(
                label = filter,
                isSelected = selectedFilters.contains(filter),
                onClick = { onFilterClick(filter) }
            )
        }
    }
}

@Composable
fun SearchResultItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.MD)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            Icon(
                imageVector = PaceDreamIcons.ArrowForward,
                contentDescription = "View details",
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
    }
}
