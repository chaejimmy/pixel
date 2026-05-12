@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.core.location.LocationService
import com.shourov.apps.pacedream.core.location.LocationServiceEntryPoint
import com.shourov.apps.pacedream.core.location.PlacePrediction
import com.shourov.apps.pacedream.core.location.PlacesAutocompleteService
import com.shourov.apps.pacedream.feature.wanted.model.SelectedPlace
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Bottom-sheet location picker for "Post a Request".
 *
 * Lets the user either:
 *   1. Tap "Use current location" — requests ACCESS_FINE_LOCATION the
 *      first time, then reverse-geocodes the device's coordinates.
 *   2. Type a query → predictions arrive (debounced) → tap to fetch
 *      structured place details (city, region, country, lat, lng).
 *
 * On a successful pick we emit a [SelectedPlace] and dismiss.  Permission
 * denial degrades to manual search (no toast, no banner — the field above
 * makes the failure obvious).
 */
@OptIn(FlowPreview::class)
@Composable
fun LocationPickerSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlaceSelected: (SelectedPlace) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Services live in the app's Singleton component; reach them via
    // EntryPoint since this is just a Composable, not a @HiltViewModel.
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LocationServiceEntryPoint::class.java,
        )
    }
    val placesService: PlacesAutocompleteService = remember { entryPoint.placesAutocompleteService() }
    val locationService: LocationService = remember { entryPoint.locationService() }

    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun resolveCurrentLocation() {
        scope.launch {
            resolving = true
            try {
                val loc = locationService.getCurrentLocation()
                if (loc == null) {
                    permissionDenied = true
                    return@launch
                }
                val place = locationService.getPlaceFromLocation(loc.latitude, loc.longitude)
                if (place != null) {
                    onPlaceSelected(
                        SelectedPlace(
                            city = place.city,
                            region = place.state,
                            country = place.country,
                            lat = place.lat,
                            lng = place.lng,
                        ),
                    )
                } else {
                    // Coordinates without a geocoder hit still beat nothing.
                    onPlaceSelected(
                        SelectedPlace(
                            city = "",
                            region = "",
                            country = "",
                            lat = loc.latitude,
                            lng = loc.longitude,
                        ),
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "use_current_location_failed")
            } finally {
                resolving = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            permissionDenied = false
            resolveCurrentLocation()
        } else {
            permissionDenied = true
        }
    }

    // Debounce typing so we don't fire a request per keystroke.
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .distinctUntilChanged()
            .debounce(300L)
            .collect { q ->
                searchJob?.cancel()
                if (q.trim().length < 2) {
                    predictions = emptyList()
                    searching = false
                    return@collect
                }
                searchJob = scope.launch {
                    searching = true
                    try {
                        predictions = placesService.getAutocompletePredictions(q.trim())
                    } catch (e: Exception) {
                        Timber.w(e, "autocomplete_failed")
                        predictions = emptyList()
                    } finally {
                        searching = false
                    }
                }
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaceDreamColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD)
                .padding(bottom = PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            Text(
                text = "Where?",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )

            UseCurrentLocationRow(
                resolving = resolving,
                onClick = {
                    if (locationService.hasLocationPermission()) {
                        resolveCurrentLocation()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
            )

            if (permissionDenied) {
                Text(
                    text = "Location permission denied — search for a place instead.",
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextSecondary,
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search a city or place") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                    focusedTextColor = PaceDreamColors.TextPrimary,
                    unfocusedTextColor = PaceDreamColors.TextPrimary,
                    focusedPlaceholderColor = PaceDreamColors.TextSecondary,
                    unfocusedPlaceholderColor = PaceDreamColors.TextSecondary,
                    cursorColor = PaceDreamColors.Primary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier.fillMaxWidth(),
            )

            when {
                searching && predictions.isEmpty() -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = PaceDreamSpacing.SM),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = PaceDreamColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                predictions.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(predictions, key = { it.placeId.ifEmpty { it.description } }) { p ->
                            PredictionRow(
                                prediction = p,
                                onClick = {
                                    scope.launch {
                                        val details = placesService.getPlaceDetails(p.placeId)
                                        if (details != null) {
                                            onPlaceSelected(
                                                SelectedPlace(
                                                    city = details.city,
                                                    region = details.state,
                                                    country = details.country,
                                                    lat = details.lat,
                                                    lng = details.lng,
                                                ),
                                            )
                                        } else {
                                            // Device-geocoder predictions have no placeId,
                                            // so fall back to the prediction's text.
                                            onPlaceSelected(
                                                SelectedPlace(
                                                    city = p.mainText,
                                                    region = "",
                                                    country = "",
                                                    lat = null,
                                                    lng = null,
                                                ),
                                            )
                                        }
                                    }
                                },
                            )
                            HorizontalDivider(
                                color = PaceDreamColors.Border.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
                query.trim().length >= 2 && !searching -> {
                    Text(
                        text = "No matches — try a different search.",
                        style = PaceDreamTypography.Footnote,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun UseCurrentLocationRow(
    resolving: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .clickable(enabled = !resolving, onClick = onClick)
            .background(PaceDreamColors.Primary.copy(alpha = 0.10f))
            .padding(PaceDreamSpacing.SM2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (resolving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = PaceDreamColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    imageVector = PaceDreamIcons.LocationOn,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(PaceDreamSpacing.SM))
        Text(
            text = if (resolving) "Finding your location…" else "Use current location",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.Primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PredictionRow(
    prediction: PlacePrediction,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = PaceDreamSpacing.SM2),
    ) {
        Text(
            text = prediction.mainText.ifBlank { prediction.description },
            style = PaceDreamTypography.Body,
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
