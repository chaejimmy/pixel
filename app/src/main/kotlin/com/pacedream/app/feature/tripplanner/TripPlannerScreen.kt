package com.pacedream.app.feature.tripplanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.pacedream.common.composables.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ── Data Models (iOS TripPlannerView parity) ─────────────────────

enum class TripStatus { PLANNING, BOOKED, COMPLETED, CANCELLED }
enum class QuickSearchTab(val label: String) { HOSTELS("Hostels"), ROOMS("Rooms"), TOURS("Tours") }

@Serializable
data class TripPlan(
    val id: String = "", val name: String = "",
    @SerialName("fromCity") val fromCity: String = "",
    @SerialName("toCity") val toCity: String = "",
    @SerialName("departureDate") val departureDate: String = "",
    @SerialName("returnDate") val returnDate: String = "",
    val travelers: Int = 1, val status: String = TripStatus.PLANNING.name,
    @SerialName("createdAt") val createdAt: String = ""
) {
    val tripStatus: TripStatus
        get() = TripStatus.entries.firstOrNull { it.name.equals(status, ignoreCase = true) } ?: TripStatus.PLANNING
}

@Serializable
data class Tour(
    val id: String = "", val title: String = "", val description: String = "",
    @SerialName("imageUrl") val imageUrl: String? = null,
    val city: String = "", val duration: String = "",
    val price: Double = 0.0, val rating: Double = 0.0
)

@Serializable
data class CreateTripRequest(
    val name: String,
    @SerialName("fromCity") val fromCity: String,
    @SerialName("toCity") val toCity: String,
    @SerialName("departureDate") val departureDate: String,
    @SerialName("returnDate") val returnDate: String,
    val travelers: Int = 1
)

@Serializable
data class TripsEnvelope(val data: List<TripPlan>? = null, val trips: List<TripPlan>? = null) {
    val resolvedTrips: List<TripPlan> get() = data ?: trips ?: emptyList()
}

@Serializable
data class ToursEnvelope(val data: List<Tour>? = null, val tours: List<Tour>? = null) {
    val resolvedTours: List<Tour> get() = data ?: tours ?: emptyList()
}

// ── Repository ───────────────────────────────────────────────────

@Singleton
class TripPlannerRepository @Inject constructor(
    private val apiClient: ApiClient, private val appConfig: AppConfig, private val json: Json
) {
    suspend fun getTrips(): ApiResult<TripsEnvelope> {
        val url = appConfig.buildApiUrl("trips")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> try {
                ApiResult.Success(json.decodeFromString(TripsEnvelope.serializer(), result.data))
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse trips"); ApiResult.Failure(com.pacedream.app.core.network.ApiError.DecodingError())
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun createTrip(request: CreateTripRequest): ApiResult<String> {
        val url = appConfig.buildApiUrl("trips")
        return apiClient.post(url, json.encodeToString(CreateTripRequest.serializer(), request), includeAuth = true)
    }

    suspend fun deleteTrip(tripId: String): ApiResult<String> =
        apiClient.delete(appConfig.buildApiUrl("trips", tripId), includeAuth = true)

    suspend fun getTours(city: String): ApiResult<ToursEnvelope> {
        val url = appConfig.buildApiUrl("tours") + "?city=$city"
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> try {
                ApiResult.Success(json.decodeFromString(ToursEnvelope.serializer(), result.data))
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse tours"); ApiResult.Failure(com.pacedream.app.core.network.ApiError.DecodingError())
            }
            is ApiResult.Failure -> result
        }
    }
}

// ── ViewModel ────────────────────────────────────────────────────

data class TripPlannerUiState(
    val trips: List<TripPlan> = emptyList(), val tours: List<Tour> = emptyList(),
    val selectedTab: QuickSearchTab = QuickSearchTab.HOSTELS,
    val fromCity: String = "", val toCity: String = "",
    val departureDate: String = "", val returnDate: String = "",
    val isLoading: Boolean = false, val isRefreshing: Boolean = false,
    val error: String? = null, val showCreateDialog: Boolean = false,
    val isSearchingTours: Boolean = false
)

@HiltViewModel
class TripPlannerViewModel @Inject constructor(
    private val repository: TripPlannerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripPlannerUiState(isLoading = true))
    val uiState: StateFlow<TripPlannerUiState> = _uiState.asStateFlow()

    init { loadTrips() }

    fun loadTrips() { viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        when (val result = repository.getTrips()) {
            is ApiResult.Success -> _uiState.update { it.copy(trips = result.data.resolvedTrips, isLoading = false, isRefreshing = false) }
            is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "Failed to load trips") }
        }
    }}

    fun refresh() { _uiState.update { it.copy(isRefreshing = true) }; loadTrips() }
    fun selectTab(tab: QuickSearchTab) { _uiState.update { it.copy(selectedTab = tab) } }
    fun updateFromCity(v: String) { _uiState.update { it.copy(fromCity = v) } }
    fun updateToCity(v: String) { _uiState.update { it.copy(toCity = v) } }
    fun updateDepartureDate(v: String) { _uiState.update { it.copy(departureDate = v) } }
    fun updateReturnDate(v: String) { _uiState.update { it.copy(returnDate = v) } }
    fun toggleCreateDialog() { _uiState.update { it.copy(showCreateDialog = !it.showCreateDialog) } }
    fun swapCities() { _uiState.update { it.copy(fromCity = it.toCity, toCity = it.fromCity) } }

    fun searchTours() { val city = _uiState.value.toCity.ifBlank { return }; viewModelScope.launch {
        _uiState.update { it.copy(isSearchingTours = true) }
        when (val result = repository.getTours(city)) {
            is ApiResult.Success -> _uiState.update { it.copy(tours = result.data.resolvedTours, isSearchingTours = false) }
            is ApiResult.Failure -> _uiState.update { it.copy(isSearchingTours = false) }
        }
    }}

    fun createTrip(name: String, travelers: Int) { val s = _uiState.value
        if (s.fromCity.isBlank() || s.toCity.isBlank()) return
        viewModelScope.launch {
            val req = CreateTripRequest(name.ifBlank { "${s.fromCity} to ${s.toCity}" }, s.fromCity, s.toCity, s.departureDate, s.returnDate, travelers)
            when (repository.createTrip(req)) {
                is ApiResult.Success -> { _uiState.update { it.copy(showCreateDialog = false) }; loadTrips() }
                is ApiResult.Failure -> {}
            }
        }
    }

    fun deleteTrip(id: String) { viewModelScope.launch {
        when (repository.deleteTrip(id)) { is ApiResult.Success -> loadTrips(); is ApiResult.Failure -> {} }
    }}
}

// ── Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlannerScreen(onBackClick: () -> Unit = {}, viewModel: TripPlannerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Planner", style = PaceDreamTypography.Title1, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back") } },
                actions = { TextButton(onClick = { viewModel.toggleCreateDialog() }) { Text("New Trip", color = PaceDreamColors.Primary, fontWeight = FontWeight.SemiBold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PaceDreamColors.Primary) }
            } else if (uiState.error != null && uiState.trips.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error", color = PaceDreamColors.TextSecondary)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Button(onClick = { viewModel.loadTrips() }) { Text("Retry") }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD)) {
                    item {
                        QuickSearchTabs(uiState.selectedTab) { viewModel.selectTab(it) }
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                    }
                    item {
                        SearchFormCard(uiState, viewModel)
                        Spacer(Modifier.height(PaceDreamSpacing.LG))
                    }
                    if (uiState.tours.isNotEmpty()) {
                        item { Text("Tours", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold); Spacer(Modifier.height(PaceDreamSpacing.SM)) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                                items(uiState.tours, key = { it.id }) { TourCard(it) }
                            }
                            Spacer(Modifier.height(PaceDreamSpacing.LG))
                        }
                    }
                    item { Text("My Trips", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold); Spacer(Modifier.height(PaceDreamSpacing.SM)) }
                    if (uiState.trips.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { Text("No trips planned yet", color = PaceDreamColors.TextSecondary) } }
                    } else {
                        items(uiState.trips, key = { it.id }) { trip ->
                            TripCard(trip) { viewModel.deleteTrip(trip.id) }; Spacer(Modifier.height(PaceDreamSpacing.SM))
                        }
                    }
                    item { Spacer(Modifier.height(PaceDreamSpacing.XL)) }
                }
            }
        }
    }
    if (uiState.showCreateDialog) {
        CreateTripSheet(uiState.fromCity, uiState.toCity, onDismiss = { viewModel.toggleCreateDialog() }) { name, travelers -> viewModel.createTrip(name, travelers) }
    }
}

// ── Components ───────────────────────────────────────────────────

@Composable
private fun QuickSearchTabs(selectedTab: QuickSearchTab, onTabSelected: (QuickSearchTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
        QuickSearchTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            FilterChip(selected = selected, onClick = { onTabSelected(tab) },
                label = { Text(tab.label, style = PaceDreamTypography.Caption, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaceDreamColors.Primary, selectedLabelColor = Color.White))
        }
    }
}

@Composable
private fun SearchFormCard(uiState: TripPlannerUiState, viewModel: TripPlannerViewModel) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(PaceDreamRadius.LG), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column(Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(value = uiState.fromCity, onValueChange = { viewModel.updateFromCity(it) },
                        label = { Text("From") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(PaceDreamRadius.MD))
                    Spacer(Modifier.height(PaceDreamSpacing.XS))
                    OutlinedTextField(value = uiState.toCity, onValueChange = { viewModel.updateToCity(it) },
                        label = { Text("To") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(PaceDreamRadius.MD))
                }
                Spacer(Modifier.width(PaceDreamSpacing.XS))
                IconButton(onClick = { viewModel.swapCities() }) { Icon(PaceDreamIcons.SwapVert, "Swap cities", tint = PaceDreamColors.Primary) }
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                OutlinedTextField(value = uiState.departureDate, onValueChange = { viewModel.updateDepartureDate(it) },
                    label = { Text("Departure") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(PaceDreamRadius.MD))
                OutlinedTextField(value = uiState.returnDate, onValueChange = { viewModel.updateReturnDate(it) },
                    label = { Text("Return") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(PaceDreamRadius.MD))
            }
            Spacer(Modifier.height(PaceDreamSpacing.MD))
            Button(onClick = { viewModel.searchTours() }, enabled = !uiState.isSearchingTours && uiState.toCity.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.Round)) {
                if (uiState.isSearchingTours) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Search", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TripCard(trip: TripPlan, onDelete: () -> Unit) {
    val statusColor = when (trip.tripStatus) {
        TripStatus.PLANNING -> PaceDreamColors.Primary; TripStatus.BOOKED -> PaceDreamColors.Success
        TripStatus.COMPLETED -> PaceDreamColors.TextSecondary; TripStatus.CANCELLED -> PaceDreamColors.Error
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(PaceDreamRadius.MD), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column(Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(trip.name, fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Body)
                    Spacer(Modifier.height(2.dp))
                    Text("${trip.fromCity} -> ${trip.toCity}", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                }
                Surface(shape = RoundedCornerShape(PaceDreamRadius.SM), color = statusColor.copy(alpha = 0.12f)) {
                    Text(trip.tripStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)) {
                if (trip.departureDate.isNotBlank()) Column {
                    Text("Departure", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                    Text(trip.departureDate.take(10), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                }
                if (trip.returnDate.isNotBlank()) Column {
                    Text("Return", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                    Text(trip.returnDate.take(10), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Travelers", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                    Text("${trip.travelers}", style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                    Icon(PaceDreamIcons.Delete, null, Modifier.size(14.dp), tint = PaceDreamColors.Error)
                    Spacer(Modifier.width(4.dp)); Text("Delete", style = PaceDreamTypography.Caption, color = PaceDreamColors.Error)
                }
            }
        }
    }
}

@Composable
private fun TourCard(tour: Tour) {
    Card(Modifier.width(220.dp), shape = RoundedCornerShape(PaceDreamRadius.MD), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column {
            AsyncImage(model = tour.imageUrl, contentDescription = tour.title,
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = PaceDreamRadius.MD, topEnd = PaceDreamRadius.MD)),
                contentScale = ContentScale.Crop)
            Column(Modifier.padding(PaceDreamSpacing.SM)) {
                Text(tour.title, fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(tour.city, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                Spacer(Modifier.height(PaceDreamSpacing.XS))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PaceDreamIcons.Star, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(String.format("%.1f", tour.rating), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("$${"%.0f".format(tour.price)}", fontWeight = FontWeight.Bold, style = PaceDreamTypography.Body, color = PaceDreamColors.Primary)
                }
                if (tour.duration.isNotBlank()) Text(tour.duration, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripSheet(fromCity: String, toCity: String, onDismiss: () -> Unit, onSubmit: (String, Int) -> Unit) {
    var tripName by remember { mutableStateOf("") }
    var travelers by remember { mutableStateOf("1") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PaceDreamColors.Background, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
        Column(Modifier.padding(PaceDreamSpacing.MD)) {
            Text("Create Trip", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(PaceDreamSpacing.MD))
            OutlinedTextField(value = tripName, onValueChange = { tripName = it }, label = { Text("Trip Name") },
                placeholder = { Text("$fromCity to $toCity") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(PaceDreamRadius.MD))
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            if (fromCity.isNotBlank() && toCity.isNotBlank()) {
                Surface(shape = RoundedCornerShape(PaceDreamRadius.SM), color = PaceDreamColors.Primary.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Text("$fromCity -> $toCity", Modifier.padding(PaceDreamSpacing.SM), style = PaceDreamTypography.Body, color = PaceDreamColors.Primary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(PaceDreamSpacing.SM))
            }
            OutlinedTextField(value = travelers, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) travelers = it },
                label = { Text("Number of Travelers") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(PaceDreamRadius.MD))
            Spacer(Modifier.height(PaceDreamSpacing.LG))
            Button(onClick = { onSubmit(tripName, travelers.toIntOrNull() ?: 1) }, enabled = fromCity.isNotBlank() && toCity.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.Round)) {
                Text("Create Trip", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(PaceDreamSpacing.XL))
        }
    }
}
