package com.pacedream.app.feature.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.DatePicker as CommonDatePicker
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modernized Enhanced Search Bar with Liquid Glass styling.
 * Features segmented tab control, unified search fields, and glass-morphism card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSearchBar(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    whatQuery: String,
    onWhatQueryChange: (String) -> Unit,
    whereQuery: String,
    onWhereQueryChange: (String) -> Unit,
    selectedDate: String?,
    onDateClick: () -> Unit,
    onUseMyLocation: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.XXL),
        color = PaceDreamColors.Card,
        shadowElevation = 0.dp,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = PaceDreamGlass.BorderWidth,
                    color = PaceDreamColors.GlassBorder,
                    shape = RoundedCornerShape(PaceDreamRadius.XXL)
                )
                .padding(PaceDreamSpacing.MD)
        ) {
            // Segmented tab control
            SegmentedTabRow(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // WHAT field
            ModernSearchField(
                label = "What",
                value = whatQuery,
                onValueChange = onWhatQueryChange,
                placeholder = "Studios, gear, meeting rooms...",
                leadingIcon = PaceDreamIcons.Search,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // WHERE field with location button
            ModernSearchField(
                label = "Where",
                value = whereQuery,
                onValueChange = onWhereQueryChange,
                placeholder = "City or neighborhood",
                leadingIcon = PaceDreamIcons.LocationOn,
                trailingContent = {
                    TextButton(
                        onClick = onUseMyLocation,
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.MyLocation,
                            contentDescription = "Use my location",
                            modifier = Modifier.size(16.dp),
                            tint = PaceDreamColors.Primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nearby",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // DATES field
            ModernSearchField(
                label = "When",
                value = selectedDate ?: "",
                onValueChange = {},
                placeholder = "Add dates",
                leadingIcon = PaceDreamIcons.CalendarToday,
                readOnly = true,
                onClick = onDateClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Search button with gradient
            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                ),
                shape = RoundedCornerShape(PaceDreamRadius.LG)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = "Search",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Modern segmented control that replaces basic FilterChips.
 * Visually similar to iOS segmented control with pill-shaped indicator.
 */
@Composable
private fun SegmentedTabRow(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = SearchTab.entries

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.Surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.XS),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PaceDreamColors.Card else Color.Transparent,
                    animationSpec = tween(200),
                    label = "tab_bg"
                )
                val elevation by animateDpAsState(
                    targetValue = if (isSelected) 2.dp else 0.dp,
                    animationSpec = tween(200),
                    label = "tab_elevation"
                )
                val tabText = when (tab) {
                    SearchTab.USE -> "Use"
                    SearchTab.BORROW -> "Borrow"
                    SearchTab.SPLIT -> "Split"
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .clickable { onTabSelected(tab) },
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    color = bgColor,
                    shadowElevation = elevation
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.MD,
                            vertical = PaceDreamSpacing.SM
                        )
                    ) {
                        Text(
                            text = tabText,
                            style = PaceDreamTypography.Subheadline,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modernized search field with cleaner styling:
 * - Filled background instead of outlined border
 * - Inline label
 * - Rounded pill shape
 */
@Composable
private fun ModernSearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = PaceDreamSpacing.XS, bottom = PaceDreamSpacing.XS)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                readOnly = readOnly,
                enabled = !readOnly,
                singleLine = true,
                modifier = (if (onClick != null && readOnly) {
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                } else {
                    Modifier.weight(1f)
                }),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PaceDreamColors.Surface,
                    unfocusedContainerColor = PaceDreamColors.Surface,
                    disabledContainerColor = PaceDreamColors.Surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = PaceDreamColors.TextPrimary,
                    unfocusedTextColor = PaceDreamColors.TextPrimary
                ),
                textStyle = PaceDreamTypography.Callout
            )
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * Date picker helper with formatted date string
 * Returns both display string and ISO date string for API
 */
@OptIn(ExperimentalMaterial3Api::class)
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
