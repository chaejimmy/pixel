package com.pacedream.app.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.DatePicker as CommonDatePicker
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Search Bar with tabs and multi-field search
 * Matches website design: Use/Borrow/Split tabs + WHAT/WHERE/DATES fields
 */
enum class SearchTab {
    USE, BORROW, SPLIT
}

@Composable
fun EnhancedSearchBar(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    whatQuery: String,
    onWhatQueryChange: (String) -> Unit,
    whereQuery: String,
    onWhereQueryChange: (String) -> Unit,
    selectedDate: String?, // Display string
    onDateClick: () -> Unit,
    onUseMyLocation: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD)
        ) {
            // Tabs: Use, Borrow, Split
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchTab.values().forEach { tab ->
                    TabButton(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // WHAT field
            SearchField(
                label = "WHAT",
                value = whatQuery,
                onValueChange = onWhatQueryChange,
                placeholder = "Search or type keywords (e.g., meeting rooms, nap pods)",
                leadingIcon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            // WHERE field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                SearchField(
                    label = "WHERE",
                    value = whereQuery,
                    onValueChange = onWhereQueryChange,
                    placeholder = "City, address, landmark",
                    leadingIcon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onUseMyLocation,
                    modifier = Modifier.align(Alignment.Bottom)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Use my location",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Use my location",
                        style = PaceDreamTypography.Caption
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            // DATES field
            SearchField(
                label = "DATES",
                value = selectedDate ?: "",
                onValueChange = { /* Read-only, opens date picker */ },
                placeholder = "Add dates",
                leadingIcon = Icons.Default.CalendarToday,
                readOnly = true,
                onClick = onDateClick,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Search Button
            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search",
                    style = PaceDreamTypography.Body,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    tab: SearchTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabText = when (tab) {
        SearchTab.USE -> "Use"
        SearchTab.BORROW -> "Borrow"
        SearchTab.SPLIT -> "Split"
    }
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = tabText,
                style = PaceDreamTypography.Body,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PaceDreamColors.Primary,
            selectedLabelColor = Color.White,
            containerColor = Color.Transparent,
            labelColor = PaceDreamColors.TextSecondary
        ),
        modifier = modifier
    )
}

@Composable
private fun SearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary
                )
            },
            readOnly = readOnly,
            enabled = !readOnly,
            singleLine = true,
            modifier = if (onClick != null && readOnly) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaceDreamColors.Primary,
                unfocusedBorderColor = PaceDreamColors.Border
            )
        )
    }
}

/**
 * Date picker helper with formatted date string
 * Returns both display string and ISO date string for API
 */
@Composable
fun rememberDatePickerState(
    initialDate: Long? = null
): Triple<String?, String?, () -> Unit> {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateDisplay by remember { mutableStateOf<String?>(null) }
    var selectedDateISO by remember { mutableStateOf<String?>(null) }
    
    val displayFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }
    val isoFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    if (showDatePicker) {
        CommonDatePicker(
            title = "Select Date",
            onDateSelected = { dateMillis ->
                selectedDateDisplay = displayFormatter.format(Date(dateMillis))
                selectedDateISO = isoFormatter.format(Date(dateMillis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    
    val openDatePicker = { showDatePicker = true }
    
    return Triple(selectedDateDisplay, selectedDateISO, openDatePicker)
}
