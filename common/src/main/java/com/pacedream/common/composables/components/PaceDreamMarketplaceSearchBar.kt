/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package com.pacedream.common.composables.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

/**
 * Marketplace search composition matching the pacedream.com hero search:
 * a Use/Borrow/Split mode tab strip above a stack of three labelled fields
 * (WHAT, WHERE, DATES) and a primary CTA. Promoted out of the app module
 * `EnhancedSearchBar` so feature modules and the design-system showcase
 * share one canonical implementation (closes DSQ #13).
 *
 * Why an overload of [PaceDreamSearchBar] rather than a new name: the
 * single-field bar in this package is also the brand search component;
 * this overload is the same brand object with a richer composition. Kotlin
 * disambiguates by parameter list — pass the [mode] first and the compiler
 * picks the right one.
 */
enum class SearchMode(val label: String) {
    USE("Use"),
    BORROW("Borrow"),
    SPLIT("Split"),
}

/**
 * Date range value passed in and out of the search bar. Stored as
 * UTC-midnight epoch millis so it round-trips through the Material3
 * `rememberDateRangePickerState` without timezone drift.
 */
data class SearchDateRange(
    val startMillis: Long? = null,
    val endMillis: Long? = null,
) {
    val isEmpty: Boolean get() = startMillis == null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaceDreamSearchBar(
    mode: SearchMode,
    onModeChange: (SearchMode) -> Unit,
    whatQuery: String,
    onWhatQueryChange: (String) -> Unit,
    whereQuery: String,
    onWhereQueryChange: (String) -> Unit,
    dateRange: SearchDateRange,
    onDateRangeChange: (SearchDateRange) -> Unit,
    onUseMyLocation: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocating: Boolean = false,
    locationPermanentlyDenied: Boolean = false,
    onOpenLocationSettings: () -> Unit = {},
    locationDeniedMessage: String = "Location is off — enable it in Settings, or search manually.",
    whatPlaceholder: String = "Search or type keywords (e.g., meeting rooms, nap pods)",
    wherePlaceholder: String = "City, address, landmark",
    datesPlaceholder: String = "Add dates",
    /**
     * Optional override for the dates field. When non-null, the WHEN
     * field renders [datesLabelOverride] in place of the formatted
     * [dateRange], and tapping it invokes [onDatesClickOverride] instead
     * of opening the built-in range picker. Used by legacy hosts that
     * still own their own picker; new callers should leave both null.
     */
    datesLabelOverride: String? = null,
    onDatesClickOverride: (() -> Unit)? = null,
    suggestionsContent: (@Composable () -> Unit)? = null,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val useBuiltInPicker = onDatesClickOverride == null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card,
        shadowElevation = 0.dp,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = PaceDreamGlass.BorderWidth,
                    color = PaceDreamColors.GlassBorder,
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                )
                .padding(PaceDreamSpacing.SM2),
        ) {
            PaceDreamSegmentedTabs(
                selected = mode,
                onSelect = onModeChange,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            ModernSearchField(
                label = "What",
                value = whatQuery,
                onValueChange = onWhatQueryChange,
                placeholder = whatPlaceholder,
                leadingIcon = PaceDreamIcons.Search,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            ModernSearchField(
                label = "Where",
                value = whereQuery,
                onValueChange = onWhereQueryChange,
                placeholder = wherePlaceholder,
                leadingIcon = PaceDreamIcons.LocationOn,
                trailingContent = {
                    UseMyLocationChip(
                        isLocating = isLocating,
                        onClick = onUseMyLocation,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (locationPermanentlyDenied) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                LocationDeniedRow(
                    message = locationDeniedMessage,
                    onOpenSettings = onOpenLocationSettings,
                )
            }

            suggestionsContent?.invoke()

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            ModernSearchField(
                label = "When",
                value = datesLabelOverride ?: formatDateRangeLabel(dateRange),
                onValueChange = {},
                placeholder = datesPlaceholder,
                leadingIcon = PaceDreamIcons.CalendarToday,
                readOnly = true,
                onClick = onDatesClickOverride ?: { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = "Search",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (useBuiltInPicker && showDatePicker) {
        DateRangePickerSheet(
            initial = dateRange,
            onDismiss = { showDatePicker = false },
            onConfirm = { range ->
                onDateRangeChange(range)
                showDatePicker = false
            },
        )
    }
}

/**
 * Reusable iOS-style segmented control over [SearchMode]. Lives next to the
 * search bar so feature surfaces (Home hero, search header) can render the
 * same tabs without duplicating the pill-indicator styling.
 */
@Composable
fun PaceDreamSegmentedTabs(
    selected: SearchMode,
    onSelect: (SearchMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.XS),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS),
        ) {
            SearchMode.entries.forEach { mode ->
                val isSelected = selected == mode
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PaceDreamColors.Card else Color.Transparent,
                    animationSpec = tween(200),
                    label = "tab_bg",
                )
                val elevation by animateDpAsState(
                    targetValue = if (isSelected) 2.dp else 0.dp,
                    animationSpec = tween(200),
                    label = "tab_elevation",
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .clickable { onSelect(mode) },
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    color = bgColor,
                    shadowElevation = elevation,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.MD,
                            vertical = PaceDreamSpacing.SM,
                        ),
                    ) {
                        Text(
                            text = mode.label,
                            style = PaceDreamTypography.Subheadline,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UseMyLocationChip(
    isLocating: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = !isLocating,
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM),
    ) {
        if (isLocating) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = PaceDreamColors.Primary,
            )
        } else {
            Icon(
                imageVector = PaceDreamIcons.MyLocation,
                contentDescription = "Use my location",
                modifier = Modifier.size(16.dp),
                tint = PaceDreamColors.Primary,
            )
        }
        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
        Text(
            text = if (isLocating) "Locating…" else "Use my location",
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.Primary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LocationDeniedRow(
    message: String,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.Surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PaceDreamIcons.LocationOn,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = message,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            TextButton(
                onClick = onOpenSettings,
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM),
            ) {
                Text(
                    text = "Open Settings",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ModernSearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = PaceDreamSpacing.XS,
                bottom = PaceDreamSpacing.XS,
            ),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextTertiary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = trailingContent?.let { { it() } },
            readOnly = readOnly,
            enabled = !readOnly,
            singleLine = true,
            modifier = if (onClick != null && readOnly) {
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            } else {
                Modifier.fillMaxWidth()
            },
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PaceDreamColors.Surface,
                unfocusedContainerColor = PaceDreamColors.Surface,
                disabledContainerColor = PaceDreamColors.Surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = PaceDreamColors.TextPrimary,
                unfocusedTextColor = PaceDreamColors.TextPrimary,
                disabledTextColor = PaceDreamColors.TextPrimary,
            ),
            textStyle = PaceDreamTypography.Callout,
        )
    }
}

/**
 * Material3 date-range picker hosted in a dialog. Returns the selected
 * range (UTC-midnight epoch millis) via [onConfirm]; OK is disabled until
 * a start date is picked so the caller can't receive an incomplete range.
 *
 * Past dates are non-selectable — the marketplace doesn't support booking
 * yesterday and surfacing them as picks invariably ends in a backend
 * rejection that the user can't recover from inside the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerSheet(
    initial: SearchDateRange,
    onDismiss: () -> Unit,
    onConfirm: (SearchDateRange) -> Unit,
) {
    val todayUtcMillis = remember {
        LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial.startMillis,
        initialSelectedEndDateMillis = initial.endMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= todayUtcMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedStartDateMillis != null,
                onClick = {
                    onConfirm(
                        SearchDateRange(
                            startMillis = pickerState.selectedStartDateMillis,
                            endMillis = pickerState.selectedEndDateMillis,
                        ),
                    )
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        colors = DatePickerDefaults.colors(),
    ) {
        DateRangePicker(
            state = pickerState,
            showModeToggle = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Human-readable form of a [SearchDateRange] for the dates field display.
 * Empty string when no start is selected so the placeholder shows; a
 * single date when start == end (Airbnb behaviour for "one night" picks),
 * otherwise "Apr 12 – Apr 18".
 */
fun formatDateRangeLabel(range: SearchDateRange): String {
    val start = range.startMillis ?: return ""
    val end = range.endMillis
    val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
    val startLabel = fmt.format(Date(start))
    return if (end == null || end == start) startLabel else "$startLabel – ${fmt.format(Date(end))}"
}
