package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.host.data.CalendarTimeSlot
import com.shourov.apps.pacedream.feature.host.data.TimeSlotStatus
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingCalendarScreen(
    onBackClick: () -> Unit = {},
    viewModel: ListingCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Availability",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        if (uiState.listingTitle.isNotBlank()) {
                            Text(
                                text = uiState.listingTitle,
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showBlockTimeSheet() },
                containerColor = PaceDreamColors.HostAccent,
                contentColor = Color.White,
                shape = RoundedCornerShape(PaceDreamRadius.Round)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = "Block Time",
                    style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PaceDreamColors.HostAccent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp) // space for FAB
            ) {
                // Monthly Calendar
                item {
                    MonthlyCalendar(
                        selectedDate = uiState.selectedDate,
                        currentMonth = uiState.currentMonth,
                        currentYear = uiState.currentYear,
                        datesWithEvents = uiState.datesWithEvents,
                        onDateSelected = { viewModel.selectDate(it) },
                        onMonthChanged = { month, year -> viewModel.changeMonth(month, year) }
                    )
                }

                // Section header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time Slots",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                            SlotLegendDot(color = PaceDreamColors.HostAccent, label = "Free")
                            SlotLegendDot(color = PaceDreamColors.Blue, label = "Booked")
                            SlotLegendDot(color = PaceDreamColors.Error, label = "Blocked")
                        }
                    }
                }

                // Time slot list
                items(uiState.timeSlots, key = { it.id }) { slot ->
                    TimeSlotRow(
                        slot = slot,
                        onRemoveBlock = { viewModel.removeBlockedTime(it) }
                    )
                }
            }
        }
    }

    // Block Time Bottom Sheet
    if (uiState.showBlockTimeSheet) {
        BlockTimeBottomSheet(
            selectedDate = uiState.selectedDate,
            startTime = uiState.blockStartTime,
            endTime = uiState.blockEndTime,
            reason = uiState.blockReason,
            onStartTimeChanged = { viewModel.updateBlockStartTime(it) },
            onEndTimeChanged = { viewModel.updateBlockEndTime(it) },
            onReasonChanged = { viewModel.updateBlockReason(it) },
            onSave = { viewModel.saveBlockedTime() },
            onDismiss = { viewModel.dismissBlockTimeSheet() }
        )
    }
}

// ── Monthly Calendar ────────────────────────────────────────────

@Composable
private fun MonthlyCalendar(
    selectedDate: String,
    currentMonth: Int,
    currentYear: Int,
    datesWithEvents: Set<String>,
    onDateSelected: (String) -> Unit,
    onMonthChanged: (Int, Int) -> Unit
) {
    val cal = remember { Calendar.getInstance() }
    var displayMonth by remember { mutableIntStateOf(currentMonth) }
    var displayYear by remember { mutableIntStateOf(currentYear) }

    // Sync with external state
    LaunchedEffect(currentMonth, currentYear) {
        displayMonth = currentMonth
        displayYear = currentYear
    }

    val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val dayHeaders = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Calculate days in month
    cal.set(displayYear, displayMonth, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val todayCal = Calendar.getInstance()
    val todayDate = String.format(
        "%04d-%02d-%02d",
        todayCal.get(Calendar.YEAR),
        todayCal.get(Calendar.MONTH) + 1,
        todayCal.get(Calendar.DAY_OF_MONTH)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (displayMonth == 0) {
                        displayMonth = 11
                        displayYear--
                    } else {
                        displayMonth--
                    }
                    onMonthChanged(displayMonth, displayYear)
                }) {
                    Icon(
                        imageVector = PaceDreamIcons.ArrowBack,
                        contentDescription = "Previous month",
                        tint = PaceDreamColors.HostAccent
                    )
                }

                Text(
                    text = "${monthNames[displayMonth]} $displayYear",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )

                IconButton(onClick = {
                    if (displayMonth == 11) {
                        displayMonth = 0
                        displayYear++
                    } else {
                        displayMonth++
                    }
                    onMonthChanged(displayMonth, displayYear)
                }) {
                    Icon(
                        imageVector = PaceDreamIcons.ArrowForward,
                        contentDescription = "Next month",
                        tint = PaceDreamColors.HostAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { day ->
                    Text(
                        text = day,
                        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                        color = PaceDreamColors.TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            // Calendar grid
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1

                        if (day in 1..daysInMonth) {
                            val dateStr = String.format(
                                "%04d-%02d-%02d",
                                displayYear,
                                displayMonth + 1,
                                day
                            )
                            val isSelected = dateStr == selectedDate
                            val isToday = dateStr == todayDate
                            val hasEvent = dateStr in datesWithEvents

                            CalendarDayCell(
                                day = day,
                                isSelected = isSelected,
                                isToday = isToday,
                                hasEvent = hasEvent,
                                onClick = { onDateSelected(dateStr) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> PaceDreamColors.HostAccent
        isToday -> PaceDreamColors.HostAccent.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> PaceDreamColors.HostAccent
        else -> PaceDreamColors.TextPrimary
    }

    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$day",
            style = PaceDreamTypography.Callout.copy(
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor,
            textAlign = TextAlign.Center
        )
        // Event dot indicator
        if (hasEvent) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White else PaceDreamColors.HostAccent
                    )
            )
        }
    }
}

// ── Time Slot Row ───────────────────────────────────────────────

@Composable
private fun TimeSlotRow(
    slot: CalendarTimeSlot,
    onRemoveBlock: (String) -> Unit
) {
    val (bgColor, statusColor, statusIcon) = when (slot.status) {
        TimeSlotStatus.AVAILABLE -> Triple(
            Color.Transparent,
            PaceDreamColors.HostAccent,
            PaceDreamIcons.CheckCircle
        )
        TimeSlotStatus.BOOKED -> Triple(
            PaceDreamColors.Blue.copy(alpha = 0.06f),
            PaceDreamColors.Blue,
            PaceDreamIcons.Person
        )
        TimeSlotStatus.BLOCKED -> Triple(
            PaceDreamColors.Error.copy(alpha = 0.06f),
            PaceDreamColors.Error,
            PaceDreamIcons.Schedule
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time range
        Column(modifier = Modifier.width(64.dp)) {
            Text(
                text = slot.startTime,
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = slot.endTime,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary
            )
        }

        // Vertical status indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))

        // Status icon
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )

        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

        // Label
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (slot.status) {
                    TimeSlotStatus.AVAILABLE -> "Available"
                    TimeSlotStatus.BOOKED -> "Booked"
                    TimeSlotStatus.BLOCKED -> "Blocked"
                },
                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = statusColor
            )
            if (slot.label.isNotBlank() && slot.status != TimeSlotStatus.AVAILABLE) {
                Text(
                    text = slot.label,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Remove block button
        if (slot.status == TimeSlotStatus.BLOCKED) {
            IconButton(
                onClick = { onRemoveBlock(slot.id) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Close,
                    contentDescription = "Remove block",
                    tint = PaceDreamColors.Error,
                    modifier = Modifier.size(PaceDreamIconSize.XS)
                )
            }
        }
    }

    HorizontalDivider(
        color = PaceDreamColors.Border,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 64.dp + 3.dp + PaceDreamSpacing.SM2)
    )
}

// ── Legend Dot ───────────────────────────────────────────────────

@Composable
private fun SlotLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = PaceDreamTypography.Caption2,
            color = PaceDreamColors.TextTertiary
        )
    }
}

// ── Block Time Bottom Sheet ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockTimeBottomSheet(
    selectedDate: String,
    startTime: String,
    endTime: String,
    reason: String,
    onStartTimeChanged: (String) -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onReasonChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaceDreamColors.Card,
        shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.MD)
        ) {
            Text(
                text = "Block Time",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            Text(
                text = formatDisplayDate(selectedDate),
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            // Start Time Picker
            Text(
                text = "Start Time",
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            TimePickerDropdown(
                selectedTime = startTime,
                onTimeSelected = onStartTimeChanged
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // End Time Picker
            Text(
                text = "End Time",
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            TimePickerDropdown(
                selectedTime = endTime,
                onTimeSelected = onEndTimeChanged
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Reason
            Text(
                text = "Reason (optional)",
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChanged,
                placeholder = {
                    Text(
                        "e.g. Maintenance, Personal use",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.HostAccent,
                    unfocusedBorderColor = PaceDreamColors.Border,
                    cursorColor = PaceDreamColors.HostAccent
                ),
                singleLine = true,
                textStyle = PaceDreamTypography.Body
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            // Save button
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = "Block Time",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDropdown(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Generate time options (every 30 minutes)
    val timeOptions = remember {
        buildList {
            for (h in 0..23) {
                add(String.format("%02d:00", h))
                add(String.format("%02d:30", h))
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = formatTime12h(selectedTime),
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaceDreamColors.HostAccent,
                unfocusedBorderColor = PaceDreamColors.Border
            ),
            textStyle = PaceDreamTypography.Body
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            timeOptions.forEach { time ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = formatTime12h(time),
                            style = PaceDreamTypography.Body,
                            color = if (time == selectedTime) PaceDreamColors.HostAccent
                                else PaceDreamColors.TextPrimary
                        )
                    },
                    onClick = {
                        onTimeSelected(time)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ── Display Helpers ─────────────────────────────────────────────

private fun formatTime12h(time24: String): String {
    val parts = time24.split(":")
    if (parts.size < 2) return time24
    val hour = parts[0].toIntOrNull() ?: return time24
    val minute = parts[1]
    val amPm = if (hour < 12) "AM" else "PM"
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$h12:$minute $amPm"
}

private fun formatDisplayDate(dateStr: String): String {
    // "2026-04-03" -> "Friday, April 3, 2026"
    val parts = dateStr.split("-")
    if (parts.size != 3) return dateStr
    val year = parts[0].toIntOrNull() ?: return dateStr
    val month = parts[1].toIntOrNull() ?: return dateStr
    val day = parts[2].toIntOrNull() ?: return dateStr

    val cal = Calendar.getInstance()
    cal.set(year, month - 1, day)

    val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val dayOfWeek = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val monthName = monthNames[month - 1]
    return "$dayOfWeek, $monthName $day, $year"
}
