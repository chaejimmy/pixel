package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Read-only field that opens a Material 3 TimePicker dialog.  The
 * caller stores the value as the 24h "HH:mm" string the backend expects
 * — the picker is just a visual convenience, so every existing payload
 * contract is preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(value) { parseTimeOrDefault(value) }
    var open by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = if (value.isBlank()) "Select time" else value,
        onValueChange = {},
        readOnly = true,
        enabled = false, // Disable input; the field is a button.
        label = { Text(label, style = PaceDreamTypography.Callout) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true },
        trailingIcon = {
            Icon(
                imageVector = PaceDreamIcons.AccessTime,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
            )
        },
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = PaceDreamColors.TextPrimary,
            disabledBorderColor = PaceDreamColors.Border,
            disabledLabelColor = PaceDreamColors.TextSecondary,
            disabledTrailingIconColor = PaceDreamColors.TextSecondary,
            focusedBorderColor = PaceDreamColors.HostAccent,
        ),
    )

    if (open) {
        val state = rememberTimePickerState(
            initialHour = parsed.hour,
            initialMinute = parsed.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    val t = LocalTime.of(state.hour, state.minute)
                    onValueChange(t.format(TIME_FMT))
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = state) },
        )
    }
}

/**
 * Read-only field that opens a Material 3 DatePicker dialog.  Emits
 * ISO-8601 "yyyy-MM-dd" strings so the backend contract is unchanged.
 * Disallows past dates by default (the wizard uses dates for "available
 * from" / "deadline" — both forward-looking).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowPast: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    val initialMillis = remember(value) {
        runCatching {
            LocalDate.parse(value, DATE_FMT)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    OutlinedTextField(
        value = if (value.isBlank()) "Select date" else value,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label, style = PaceDreamTypography.Callout) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true },
        trailingIcon = {
            Icon(
                imageVector = PaceDreamIcons.CalendarToday,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
            )
        },
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = PaceDreamColors.TextPrimary,
            disabledBorderColor = PaceDreamColors.Border,
            disabledLabelColor = PaceDreamColors.TextSecondary,
            disabledTrailingIconColor = PaceDreamColors.TextSecondary,
            focusedBorderColor = PaceDreamColors.HostAccent,
        ),
    )

    if (open) {
        val selectable = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (allowPast) return true
                val d = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneOffset.UTC).toLocalDate()
                return !d.isBefore(LocalDate.now())
            }
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = selectable,
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(d.format(DATE_FMT))
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/**
 * Searchable time-zone picker backed by [ZoneId.getAvailableZoneIds].
 * Emits valid IANA ids (e.g. "America/New_York") — callers can keep the
 * field as a plain `String` for draft + backend parity.  Opens a
 * full-screen-ish dialog so the search list has room to breathe on
 * small phones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezoneField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Time zone",
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value.ifBlank { "Select time zone" },
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label, style = PaceDreamTypography.Callout) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true },
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = PaceDreamColors.TextPrimary,
            disabledBorderColor = PaceDreamColors.Border,
            disabledLabelColor = PaceDreamColors.TextSecondary,
            focusedBorderColor = PaceDreamColors.HostAccent,
        ),
    )

    if (open) {
        val allZones = remember { ZoneId.getAvailableZoneIds().sorted() }
        var query by remember { mutableStateOf(value) }
        val filtered = remember(query, allZones) {
            if (query.isBlank()) allZones
            else allZones.filter { it.contains(query, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Choose time zone", style = PaceDreamTypography.Headline) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text("Search") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Search,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                    )
                    Spacer(Modifier.height(PaceDreamSpacing.SM))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 360.dp),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    ) {
                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp),
                            ) {
                                Text(
                                    "No matches",
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.TextSecondary,
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(filtered, key = { it }) { tz ->
                                    Text(
                                        text = tz,
                                        style = PaceDreamTypography.Body,
                                        color = PaceDreamColors.TextPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onValueChange(tz)
                                                open = false
                                            }
                                            .heightIn(min = 40.dp)
                                            .padding(
                                                horizontal = PaceDreamSpacing.MD,
                                                vertical = PaceDreamSpacing.SM,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Close") }
            },
        )
    }
}

private fun parseTimeOrDefault(value: String): LocalTime = try {
    if (value.isBlank()) LocalTime.of(9, 0) else LocalTime.parse(value, TIME_FMT)
} catch (e: DateTimeParseException) {
    LocalTime.of(9, 0)
}
