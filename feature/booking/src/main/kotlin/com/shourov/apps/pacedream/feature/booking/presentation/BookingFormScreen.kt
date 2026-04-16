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

package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.buttons.GradientProcessButton
import com.pacedream.common.composables.buttons.PrimaryTextButton
import com.pacedream.common.composables.components.PaceDreamHeroHeader
import com.pacedream.common.composables.components.PaceDreamPropertyImage
import com.pacedream.common.composables.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingFormScreen(
    propertyId: String,
    modifier: Modifier = Modifier,
    viewModel: BookingFormViewModel = hiltViewModel(),
    onBookingCreated: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(propertyId) {
        viewModel.loadProperty(propertyId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .statusBarsPadding()
    ) {
        PaceDreamHeroHeader(
            title = "Reserve",
            subtitle = uiState.propertyName.ifEmpty { "Complete your reservation" }
        )

        if (uiState.isLoading) {
            BookingFormLoadingState()
        } else {
            BookingFormContent(
                uiState = uiState,
                onDurationChange = viewModel::onDurationChange,
                onStartDateChange = viewModel::onStartDateChange,
                onEndDateChange = viewModel::onEndDateChange,
                onStartTimeChange = viewModel::onStartTimeChange,
                onEndTimeChange = viewModel::onEndTimeChange,
                onSpecialRequestsChange = viewModel::onSpecialRequestsChange,
                onGuestCountChange = viewModel::onGuestCountChange,
                onClearError = viewModel::clearError,
                onBookNow = {
                    viewModel.createBooking {
                        onBookingCreated(it)
                    }
                }
            )
        }
    }
}

@Composable
private fun BookingFormContent(
    uiState: BookingFormUiState,
    onDurationChange: (Int) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onSpecialRequestsChange: (String) -> Unit,
    onGuestCountChange: (Int) -> Unit,
    onClearError: () -> Unit,
    onBookNow: () -> Unit
) {
    val isSelectionComplete = uiState.startDate.isNotEmpty() &&
            uiState.startTime.isNotEmpty() &&
            uiState.selectedDuration > 0

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 140.dp) // Space for sticky bottom bar
        ) {
            // Error Banner
            uiState.error?.let { error ->
                ErrorBanner(error = error, onDismiss = onClearError)
            }

            // Step 1: Duration
            SectionHeader(
                stepNumber = 1,
                title = "Duration",
                subtitle = "How long do you need?"
            )
            DurationChipRow(
                selectedDuration = uiState.selectedDuration,
                onDurationChange = onDurationChange
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Step 2: Date
            SectionHeader(
                stepNumber = 2,
                title = "Date",
                subtitle = "Pick your day"
            )
            DateSelector(
                startDate = uiState.startDate,
                onDateChange = onStartDateChange
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Step 3: Time
            SectionHeader(
                stepNumber = 3,
                title = "Start Time",
                subtitle = if (uiState.startDate.isNotEmpty()) "Available times" else "Select a date first"
            )
            TimeSlotGrid(
                selectedTime = uiState.startTime,
                onTimeChange = onStartTimeChange,
                enabled = uiState.startDate.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Guests
            GuestCountSection(
                guestCount = uiState.guestCount,
                onGuestCountChange = onGuestCountChange
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Special Requests (collapsible)
            SpecialRequestsSection(
                specialRequests = uiState.specialRequests,
                onSpecialRequestsChange = onSpecialRequestsChange
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        }

        // Sticky Bottom: Booking Summary + CTA
        BookingSummaryBar(
            uiState = uiState,
            isSelectionComplete = isSelectionComplete,
            onBookNow = onBookNow,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─── Section Header with Step ────────────────────────────────────

@Composable
private fun SectionHeader(
    stepNumber: Int,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = PaceDreamColors.Primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "$stepNumber",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.OnPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))
        Column {
            Text(
                text = title,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ─── Duration Chips ──────────────────────────────────────────────

private data class DurationOption(val minutes: Int, val label: String)

private val DURATION_OPTIONS = listOf(
    DurationOption(15, "15m"),
    DurationOption(30, "30m"),
    DurationOption(60, "1h"),
    DurationOption(120, "2h"),
    DurationOption(180, "3h"),
    DurationOption(240, "4h")
)

@Composable
private fun DurationChipRow(
    selectedDuration: Int,
    onDurationChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        DURATION_OPTIONS.forEach { option ->
            val isSelected = selectedDuration == option.minutes
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Card,
                label = "chipBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Border,
                label = "chipBorder"
            )
            val elevation by animateDpAsState(
                targetValue = if (isSelected) 4.dp else 0.dp,
                label = "chipElevation"
            )

            Surface(
                onClick = { onDurationChange(option.minutes) },
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = elevation,
                modifier = Modifier.height(42.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            PaceDreamIcons.Check,
                            contentDescription = null,
                            tint = PaceDreamColors.OnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = option.label,
                        style = PaceDreamTypography.Callout,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PaceDreamColors.OnPrimary else PaceDreamColors.TextPrimary
                    )
                }
            }
        }
    }
}

// ─── Date Selector ───────────────────────────────────────────────

@Composable
private fun DateSelector(
    startDate: String,
    onDateChange: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    val dates = remember {
        (0..13).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            cal.time
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        dates.forEach { date ->
            val dateStr = dateFormat.format(date)
            val isSelected = startDate == dateStr
            val isToday = dateFormat.format(Calendar.getInstance().time) == dateStr
            val bgColor by animateColorAsState(
                targetValue = when {
                    isSelected -> PaceDreamColors.Primary
                    isToday -> PaceDreamColors.Primary.copy(alpha = 0.08f)
                    else -> PaceDreamColors.Card
                },
                label = "dateBg"
            )

            Surface(
                onClick = { onDateChange(dateStr) },
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                color = bgColor,
                border = if (isToday && !isSelected) BorderStroke(1.dp, PaceDreamColors.Primary.copy(alpha = 0.3f)) else null,
                modifier = Modifier.width(64.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = displayFormat.format(date).uppercase(),
                        style = PaceDreamTypography.Caption2,
                        color = if (isSelected) PaceDreamColors.OnPrimary.copy(alpha = 0.7f) else PaceDreamColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayFormat.format(date),
                        style = PaceDreamTypography.Title3,
                        color = if (isSelected) PaceDreamColors.OnPrimary else PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = monthFormat.format(date),
                        style = PaceDreamTypography.Caption2,
                        color = if (isSelected) PaceDreamColors.OnPrimary.copy(alpha = 0.7f) else PaceDreamColors.TextSecondary
                    )
                    if (isToday) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) PaceDreamColors.OnPrimary else PaceDreamColors.Primary,
                            modifier = Modifier.size(5.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

// ─── Time Slot Grid ──────────────────────────────────────────────

private data class TimeSlotOption(val time: String, val label: String)

private val MORNING_SLOTS = listOf(
    TimeSlotOption("08:00", "8:00 AM"),
    TimeSlotOption("09:00", "9:00 AM"),
    TimeSlotOption("10:00", "10:00 AM"),
    TimeSlotOption("11:00", "11:00 AM")
)
private val AFTERNOON_SLOTS = listOf(
    TimeSlotOption("12:00", "12:00 PM"),
    TimeSlotOption("13:00", "1:00 PM"),
    TimeSlotOption("14:00", "2:00 PM"),
    TimeSlotOption("15:00", "3:00 PM"),
    TimeSlotOption("16:00", "4:00 PM")
)
private val EVENING_SLOTS = listOf(
    TimeSlotOption("17:00", "5:00 PM"),
    TimeSlotOption("18:00", "6:00 PM"),
    TimeSlotOption("19:00", "7:00 PM"),
    TimeSlotOption("20:00", "8:00 PM")
)

@Composable
private fun TimeSlotGrid(
    selectedTime: String,
    onTimeChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        if (!enabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray50),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.LG),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        PaceDreamIcons.CalendarToday,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        text = "Select a date to see available times",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
            return
        }

        TimeSlotSection("Morning", MORNING_SLOTS, selectedTime, onTimeChange)
        TimeSlotSection("Afternoon", AFTERNOON_SLOTS, selectedTime, onTimeChange)
        TimeSlotSection("Evening", EVENING_SLOTS, selectedTime, onTimeChange)
    }
}

@Composable
private fun TimeSlotSection(
    label: String,
    slots: List<TimeSlotOption>,
    selectedTime: String,
    onTimeChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
        )
        // Use FlowRow-style layout: 3 columns
        val rows = slots.chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                row.forEach { slot ->
                    val isSelected = selectedTime == slot.time
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.1f) else PaceDreamColors.Card,
                        label = "slotBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Border,
                        label = "slotBorder"
                    )

                    Surface(
                        onClick = { onTimeChange(slot.time) },
                        shape = RoundedCornerShape(PaceDreamRadius.SM),
                        color = bgColor,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = borderColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = slot.label,
                                style = PaceDreamTypography.Callout,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextPrimary
                            )
                        }
                    }
                }
                // Fill remaining space if row has < 3 items
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
    }
}

// ─── Guests ──────────────────────────────────────────────────────

@Composable
private fun GuestCountSection(
    guestCount: Int,
    onGuestCountChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    PaceDreamIcons.People,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    "Guests",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onGuestCountChange((guestCount - 1).coerceAtLeast(1)) },
                    enabled = guestCount > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (guestCount > 1) PaceDreamColors.Gray100 else PaceDreamColors.Gray100.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                PaceDreamIcons.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(16.dp),
                                tint = if (guestCount > 1) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
                Text(
                    "$guestCount",
                    style = PaceDreamTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { onGuestCountChange((guestCount + 1).coerceAtMost(20)) },
                    enabled = guestCount < 20,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (guestCount < 20) PaceDreamColors.Gray100 else PaceDreamColors.Gray100.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                PaceDreamIcons.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(16.dp),
                                tint = if (guestCount < 20) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Special Requests ────────────────────────────────────────────

@Composable
private fun SpecialRequestsSection(
    specialRequests: String,
    onSpecialRequestsChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            color = PaceDreamColors.Card
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Special Requests (Optional)",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary
                )
                Icon(
                    if (expanded) PaceDreamIcons.ExpandLess else PaceDreamIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = specialRequests,
                onValueChange = onSpecialRequestsChange,
                placeholder = { Text("Any special requirements?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PaceDreamSpacing.SM),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            )
        }
    }
}

// ─── Booking Summary Bar (Sticky Bottom) ─────────────────────────

@Composable
private fun BookingSummaryBar(
    uiState: BookingFormUiState,
    isSelectionComplete: Boolean,
    onBookNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationLabel = DURATION_OPTIONS.find { it.minutes == uiState.selectedDuration }?.label ?: ""
    val hasDate = uiState.startDate.isNotEmpty()
    val hasTime = uiState.startTime.isNotEmpty()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = PaceDreamColors.Card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM2)
        ) {
            // Summary row
            AnimatedVisibility(
                visible = durationLabel.isNotEmpty() || hasDate || hasTime,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaceDreamSpacing.SM),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection summary chips
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                    ) {
                        if (durationLabel.isNotEmpty()) {
                            SummaryChip(
                                icon = PaceDreamIcons.AccessTime,
                                text = durationLabel
                            )
                        }
                        if (hasDate) {
                            val displayDate = try {
                                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outFmt = SimpleDateFormat("MMM d", Locale.getDefault())
                                fmt.parse(uiState.startDate)?.let { outFmt.format(it) } ?: uiState.startDate
                            } catch (_: Exception) { uiState.startDate }
                            SummaryChip(
                                icon = PaceDreamIcons.CalendarToday,
                                text = displayDate
                            )
                        }
                        if (hasTime) {
                            val displayTime = try {
                                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val outFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                                fmt.parse(uiState.startTime)?.let { outFmt.format(it) } ?: uiState.startTime
                            } catch (_: Exception) { uiState.startTime }
                            SummaryChip(
                                icon = PaceDreamIcons.Schedule,
                                text = displayTime
                            )
                        }
                    }

                    // Estimated price
                    if (uiState.totalPrice > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${uiState.currency} ${String.format("%.2f", uiState.totalPrice)}",
                                style = PaceDreamTypography.Headline,
                                color = PaceDreamColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "estimated",
                                style = PaceDreamTypography.Caption2,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // CTA Button — iOS hero action pattern, locked while submitting
            GradientProcessButton(
                onClick = { if (!uiState.isSubmitting) onBookNow() },
                isEnabled = isSelectionComplete && !uiState.isSubmitting,
                text = when {
                    uiState.isSubmitting -> "Reserving..."
                    !isSelectionComplete -> "Select duration, date & time"
                    uiState.totalPrice > 0 -> "Reserve - ${uiState.currency} ${String.format("%.2f", uiState.totalPrice)}"
                    else -> "Reserve"
                },
            )

            if (isSelectionComplete) {
                Text(
                    "You won't be charged yet",
                    style = PaceDreamTypography.Caption2,
                    color = PaceDreamColors.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = PaceDreamSpacing.XS)
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = PaceDreamColors.Primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Error Banner ────────────────────────────────────────────────

@Composable
private fun ErrorBanner(error: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Error.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.SM2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                PaceDreamIcons.Error,
                contentDescription = null,
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = error,
                modifier = Modifier.weight(1f),
                color = PaceDreamColors.TextPrimary,
                style = PaceDreamTypography.Callout
            )
            PrimaryTextButton(
                text = "Dismiss",
                onClick = onDismiss,
                modifier = Modifier,
            )
        }
    }
}

// ─── Loading State ───────────────────────────────────────────────

@Composable
private fun BookingFormLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = PaceDreamColors.Primary
        )
    }
}

data class BookingFormUiState(
    val isLoading: Boolean = false,
    val propertyId: String = "",
    val propertyName: String = "",
    val propertyImage: String? = null,
    val basePrice: Double = 0.0,
    val currency: String = "USD",
    val selectedDuration: Int = 60,
    val startDate: String = "",
    val endDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val specialRequests: String = "",
    val guestCount: Int = 1,
    val totalPrice: Double = 0.0,
    val error: String? = null,
    /** True while a booking creation request is in flight — prevents double-submit. */
    val isSubmitting: Boolean = false
)
