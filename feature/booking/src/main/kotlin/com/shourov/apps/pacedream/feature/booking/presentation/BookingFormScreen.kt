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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.components.PaceDreamHeroHeader
import com.pacedream.common.composables.components.PaceDreamPropertyImage
import com.pacedream.common.composables.theme.*

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
            title = "Book Property",
            subtitle = "Complete your reservation"
        )

        if (uiState.isLoading) {
            BookingFormLoadingState()
        } else {
            BookingFormContent(
                uiState = uiState,
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
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onSpecialRequestsChange: (String) -> Unit,
    onGuestCountChange: (Int) -> Unit,
    onClearError: () -> Unit,
    onBookNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(PaceDreamSpacing.MD)
    ) {
        // Property Summary
        PropertySummaryCard(
            propertyName = uiState.propertyName,
            propertyImage = uiState.propertyImage,
            basePrice = uiState.basePrice,
            currency = uiState.currency
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        // Error Banner (iOS parity: inline error, not toast)
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = CardDefaults.cardColors(
                    containerColor = PaceDreamColors.Error.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
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
                        text = error.removePrefix("Server error 200: "),
                        modifier = Modifier.weight(1f),
                        color = PaceDreamColors.TextPrimary,
                        style = PaceDreamTypography.Callout
                    )
                    TextButton(onClick = onClearError) {
                        Text("Dismiss", color = PaceDreamColors.Primary, style = PaceDreamTypography.Caption)
                    }
                }
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        }

        // Booking Details Form
        BookingDetailsForm(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            startTime = uiState.startTime,
            endTime = uiState.endTime,
            specialRequests = uiState.specialRequests,
            guestCount = uiState.guestCount,
            onStartDateChange = onStartDateChange,
            onEndDateChange = onEndDateChange,
            onStartTimeChange = onStartTimeChange,
            onEndTimeChange = onEndTimeChange,
            onSpecialRequestsChange = onSpecialRequestsChange,
            onGuestCountChange = onGuestCountChange
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        // Price Summary
        PriceSummaryCard(
            basePrice = uiState.basePrice,
            currency = uiState.currency,
            totalPrice = uiState.totalPrice
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        // Reassurance text
        Text(
            "You won't be charged yet",
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        // Book Now Button - iOS 26 style
        Button(
            onClick = onBookNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(PaceDreamButtonHeight.LG),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = if (uiState.totalPrice > 0) {
                    "Book Now - ${uiState.currency} ${String.format("%.2f", uiState.totalPrice)}"
                } else {
                    "Book Now"
                },
                style = PaceDreamTypography.Button,
                color = PaceDreamColors.OnPrimary
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
    }
}

@Composable
private fun PropertySummaryCard(
    propertyName: String,
    propertyImage: String?,
    basePrice: Double,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            PaceDreamPropertyImage(
                imageUrl = propertyImage,
                contentDescription = "Property: $propertyName",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
            )

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = propertyName,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.OnCard,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                Text(
                    text = "$currency ${String.format("%.2f", basePrice)} per hour",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BookingDetailsForm(
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String,
    specialRequests: String,
    guestCount: Int,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onSpecialRequestsChange: (String) -> Unit,
    onGuestCountChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            Text(
                text = "Booking Details",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.OnCard,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Date Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = onStartDateChange,
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        Icon(PaceDreamIcons.CalendarToday, contentDescription = "Select date")
                    }
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = onEndDateChange,
                    label = { Text("End Date") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        Icon(PaceDreamIcons.CalendarToday, contentDescription = "Select date")
                    }
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Time Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = onStartTimeChange,
                    label = { Text("Start Time") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        Icon(PaceDreamIcons.AccessTime, contentDescription = "Select time")
                    }
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = onEndTimeChange,
                    label = { Text("End Time") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        Icon(PaceDreamIcons.AccessTime, contentDescription = "Select time")
                    }
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Guest Count Selector (iOS parity)
            HorizontalDivider(color = PaceDreamColors.Outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Guests",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.OnCard,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "How many guests?",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onGuestCountChange((guestCount - 1).coerceAtLeast(1)) },
                        enabled = guestCount > 1
                    ) {
                        Surface(
                            shape = RoundedCornerShape(PaceDreamRadius.Round),
                            color = if (guestCount > 1) PaceDreamColors.Gray100 else PaceDreamColors.Gray100.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                PaceDreamIcons.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.padding(6.dp).size(18.dp),
                                tint = if (guestCount > 1) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary
                            )
                        }
                    }
                    Text(
                        "$guestCount",
                        style = PaceDreamTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = { onGuestCountChange((guestCount + 1).coerceAtMost(20)) },
                        enabled = guestCount < 20
                    ) {
                        Surface(
                            shape = RoundedCornerShape(PaceDreamRadius.Round),
                            color = if (guestCount < 20) PaceDreamColors.Gray100 else PaceDreamColors.Gray100.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                PaceDreamIcons.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.padding(6.dp).size(18.dp),
                                tint = if (guestCount < 20) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            HorizontalDivider(color = PaceDreamColors.Outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Special Requests
            OutlinedTextField(
                value = specialRequests,
                onValueChange = onSpecialRequestsChange,
                label = { Text("Special Requests (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

@Composable
private fun PriceSummaryCard(
    basePrice: Double,
    currency: String,
    totalPrice: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            Text(
                text = "Price Summary",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.OnCard,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Base Price",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = "$currency ${String.format("%.2f", basePrice)}",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.OnCard
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            val serviceFee = totalPrice * 0.1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Service Fee",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = "$currency ${String.format("%.2f", serviceFee)}",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.OnCard
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            HorizontalDivider(color = PaceDreamColors.Border.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.OnCard,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$currency ${String.format("%.2f", totalPrice)}",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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
    val startDate: String = "",
    val endDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val specialRequests: String = "",
    val guestCount: Int = 1,
    val totalPrice: Double = 0.0,
    val error: String? = null
)
