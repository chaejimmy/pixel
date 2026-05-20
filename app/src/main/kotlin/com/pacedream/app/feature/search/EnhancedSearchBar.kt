package com.pacedream.app.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.PaceDreamSearchBar
import com.pacedream.common.composables.components.SearchDateRange
import com.pacedream.common.composables.components.SearchMode
import com.pacedream.common.composables.components.DatePicker as CommonDatePicker
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.core.location.CurrentLocationState
import com.shourov.apps.pacedream.core.location.PlacePrediction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thin host that adapts the marketplace search composition
 * ([com.pacedream.common.composables.components.PaceDreamSearchBar]) to the
 * existing Explore-screen API: it maps the in-app [SearchTab] enum to the
 * design-system [SearchMode], keeps the legacy single-date ISO string
 * contract, and adds the place-autocomplete dropdown that lives between
 * the WHERE field and the dates field. New callers should target the
 * design-system component directly.
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
    placeSuggestions: List<PlacePrediction> = emptyList(),
    onPlaceSuggestionClick: (PlacePrediction) -> Unit = {},
    /**
     * Current state of the location-detection flow.  When [Loading] we
     * swap the chip for a spinner; when permanently denied we surface a
     * small inline row with an "Open Settings" CTA right under the WHERE
     * field so the user has a clear recovery path.
     */
    currentLocationState: CurrentLocationState = CurrentLocationState.Idle,
    onOpenLocationSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PaceDreamSearchBar(
        mode = selectedTab.toSearchMode(),
        onModeChange = { onTabSelected(it.toSearchTab()) },
        whatQuery = whatQuery,
        onWhatQueryChange = onWhatQueryChange,
        whereQuery = whereQuery,
        onWhereQueryChange = onWhereQueryChange,
        // Legacy callers own the picker; we delegate display + click via
        // the override slots so the design-system bar's built-in range
        // picker stays inactive at this call site.
        dateRange = SearchDateRange(),
        onDateRangeChange = { /* unused — legacy caller owns the picker */ },
        datesLabelOverride = selectedDate.orEmpty(),
        onDatesClickOverride = onDateClick,
        onUseMyLocation = onUseMyLocation,
        onSearchClick = onSearchClick,
        modifier = modifier,
        isLocating = currentLocationState is CurrentLocationState.Loading,
        locationPermanentlyDenied = currentLocationState is CurrentLocationState.PermissionPermanentlyDenied,
        onOpenLocationSettings = onOpenLocationSettings,
        suggestionsContent = if (placeSuggestions.isNotEmpty()) {
            { PlaceSuggestionsList(placeSuggestions, onPlaceSuggestionClick) }
        } else null,
    )
}

@Composable
private fun PlaceSuggestionsList(
    suggestions: List<PlacePrediction>,
    onClick: (PlacePrediction) -> Unit,
) {
    Spacer(modifier = Modifier.size(PaceDreamSpacing.XS))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.Surface,
        tonalElevation = 2.dp,
    ) {
        Column {
            suggestions.forEach { prediction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(prediction) }
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
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prediction.mainText,
                            style = PaceDreamTypography.Callout,
                            fontWeight = FontWeight.Medium,
                            color = PaceDreamColors.TextPrimary,
                        )
                        if (prediction.secondaryText.isNotBlank()) {
                            Text(
                                text = prediction.secondaryText,
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun SearchTab.toSearchMode(): SearchMode = when (this) {
    SearchTab.SPACES -> SearchMode.USE
    SearchTab.ITEMS -> SearchMode.BORROW
    SearchTab.SERVICES -> SearchMode.SPLIT
}

private fun SearchMode.toSearchTab(): SearchTab = when (this) {
    SearchMode.USE -> SearchTab.SPACES
    SearchMode.BORROW -> SearchTab.ITEMS
    SearchMode.SPLIT -> SearchTab.SERVICES
}

/**
 * Date picker helper retained for legacy callers (single-date ISO string).
 * New code should use [SearchDateRange] + the built-in range picker on
 * [PaceDreamSearchBar].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDatePickerState(
    initialDate: Long? = null,
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
            onDismiss = { showDatePicker = false },
        )
    }

    val openDatePicker = { showDatePicker = true }

    return Triple(selectedDateDisplay, selectedDateISO, openDatePicker)
}
