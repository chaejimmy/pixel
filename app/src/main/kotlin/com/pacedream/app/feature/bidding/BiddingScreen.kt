package com.pacedream.app.feature.bidding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons
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

// ── Data Models (iOS BidListView parity) ────────────────────────

enum class BidStatus(val label: String) {
    PENDING("Pending"), ACCEPTED("Accepted"), REJECTED("Rejected"),
    EXPIRED("Expired"), WITHDRAWN("Withdrawn");

    val color: Color get() = when (this) {
        PENDING -> Color(0xFFFF9800); ACCEPTED -> Color(0xFF4CAF50)
        REJECTED -> Color(0xFFF44336); EXPIRED -> Color(0xFF9E9E9E)
        WITHDRAWN -> Color(0xFF607D8B)
    }
    val isActive: Boolean get() = this == PENDING
}

@Serializable
data class Bid(
    val id: String = "",
    @SerialName("listingId") val listingId: String = "",
    @SerialName("bidderName") val bidderName: String = "",
    @SerialName("bidAmount") val bidAmount: Double = 0.0,
    @SerialName("checkIn") val checkIn: String = "",
    @SerialName("checkOut") val checkOut: String = "",
    val status: String = "pending",
    val message: String? = null,
    @SerialName("createdAt") val createdAt: String = ""
) {
    val bidStatus: BidStatus
        get() = BidStatus.entries.firstOrNull { it.name.equals(status, ignoreCase = true) }
            ?: BidStatus.PENDING
    val formattedAmount: String get() = "$${String.format("%,.0f", bidAmount)}"
    val dateRange: String get() = "${checkIn.take(10)} - ${checkOut.take(10)}"
}

@Serializable
data class BidsEnvelope(
    val status: Boolean? = null, val success: Boolean? = null,
    val data: List<Bid>? = null, val bids: List<Bid>? = null
) { val resolvedBids: List<Bid> get() = data ?: bids ?: emptyList() }

@Serializable
data class BidEnvelope(
    val status: Boolean? = null, val data: Bid? = null, val bid: Bid? = null
) { val resolvedBid: Bid? get() = data ?: bid }

@Serializable
data class CreateBidRequest(
    @SerialName("listingId") val listingId: String,
    @SerialName("bidAmount") val bidAmount: Double,
    @SerialName("checkIn") val checkIn: String,
    @SerialName("checkOut") val checkOut: String,
    val message: String? = null
)

// ── Repository ──────────────────────────────────────────────────

@Singleton
class BiddingRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    private inline fun <reified T> decode(raw: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): ApiResult<T> =
        try { ApiResult.Success(json.decodeFromString(deserializer, raw)) }
        catch (e: Exception) { Timber.e(e, "Parse error"); ApiResult.Failure(ApiError.DecodingError()) }

    suspend fun getBids(page: Int = 1, limit: Int = 20): ApiResult<BidsEnvelope> {
        val url = appConfig.buildApiUrl("bids", queryParams = mapOf("page" to page.toString(), "limit" to limit.toString()))
        return when (val r = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> decode(r.data, BidsEnvelope.serializer())
            is ApiResult.Failure -> r
        }
    }

    suspend fun createBid(request: CreateBidRequest): ApiResult<String> {
        val url = appConfig.buildApiUrl("bids")
        return apiClient.post(url, json.encodeToString(CreateBidRequest.serializer(), request), includeAuth = true)
    }

    suspend fun withdrawBid(bidId: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("bids", bidId, "withdraw")
        return apiClient.put(url, "{}", includeAuth = true)
    }

    suspend fun getBidDetail(bidId: String): ApiResult<BidEnvelope> {
        val url = appConfig.buildApiUrl("bids", bidId)
        return when (val r = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> decode(r.data, BidEnvelope.serializer())
            is ApiResult.Failure -> r
        }
    }

    suspend fun getListingBids(listingId: String): ApiResult<BidsEnvelope> {
        val url = appConfig.buildApiUrl("bids", queryParams = mapOf("listingId" to listingId))
        return when (val r = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> decode(r.data, BidsEnvelope.serializer())
            is ApiResult.Failure -> r
        }
    }
}

// ── ViewModel ───────────────────────────────────────────────────

data class BiddingUiState(
    val bids: List<Bid> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showCreateSheet: Boolean = false,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class BiddingViewModel @Inject constructor(
    private val repository: BiddingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiddingUiState(isLoading = true))
    val uiState: StateFlow<BiddingUiState> = _uiState.asStateFlow()

    init { loadBids() }

    fun loadBids() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getBids()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(bids = result.data.resolvedBids, isLoading = false, isRefreshing = false)
                }
                is ApiResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = "Failed to load bids")
                }
            }
        }
    }

    fun refresh() { _uiState.update { it.copy(isRefreshing = true) }; loadBids() }

    fun toggleCreateSheet() { _uiState.update { it.copy(showCreateSheet = !it.showCreateSheet) } }

    fun createBid(request: CreateBidRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (repository.createBid(request)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(showCreateSheet = false, isSubmitting = false) }
                    loadBids()
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isSubmitting = false, error = "Failed to create bid") }
            }
        }
    }

    fun withdrawBid(bidId: String) {
        viewModelScope.launch {
            when (repository.withdrawBid(bidId)) {
                is ApiResult.Success -> loadBids()
                is ApiResult.Failure -> _uiState.update { it.copy(error = "Failed to withdraw bid") }
            }
        }
    }
}

// ── Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiddingScreen(
    onBackClick: () -> Unit = {},
    viewModel: BiddingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bids", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { viewModel.toggleCreateSheet() }) {
                        Text("New Bid", style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold), color = PaceDreamColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PaceDreamColors.Primary)
                    }
                }
                uiState.error != null && uiState.bids.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error ?: "Error", color = PaceDreamColors.TextSecondary)
                            Spacer(Modifier.height(PaceDreamSpacing.MD))
                            Button(onClick = { viewModel.loadBids() }) { Text("Retry") }
                        }
                    }
                }
                uiState.bids.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No bids yet", style = PaceDreamTypography.Title2, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(PaceDreamSpacing.XS))
                            Text("Your bids on listings will appear here.", color = PaceDreamColors.TextSecondary, style = PaceDreamTypography.Body)
                            Spacer(Modifier.height(PaceDreamSpacing.MD))
                            Button(
                                onClick = { viewModel.toggleCreateSheet() },
                                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                                shape = RoundedCornerShape(PaceDreamRadius.Round)
                            ) { Text("Place a Bid") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD)
                    ) {
                        items(uiState.bids, key = { it.id }) { bid ->
                            BidCard(bid = bid, onWithdraw = { viewModel.withdrawBid(bid.id) })
                            Spacer(Modifier.height(PaceDreamSpacing.SM))
                        }
                        item { Spacer(Modifier.height(PaceDreamSpacing.XL)) }
                    }
                }
            }
        }
    }

    if (uiState.showCreateSheet) {
        CreateBidSheet(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { viewModel.toggleCreateSheet() },
            onSubmit = { viewModel.createBid(it) }
        )
    }
}

// ── Bid Card ────────────────────────────────────────────────────

@Composable
private fun BidCard(bid: Bid, onWithdraw: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(bid.formattedAmount, style = PaceDreamTypography.Title1, fontWeight = FontWeight.Bold, color = PaceDreamColors.TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(bid.dateRange, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                }
                StatusBadge(status = bid.bidStatus)
            }
            if (!bid.message.isNullOrBlank()) {
                Spacer(Modifier.height(PaceDreamSpacing.SM))
                Text(bid.message, style = PaceDreamTypography.Body, color = PaceDreamColors.TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Submitted ${bid.createdAt.take(10)}", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary, modifier = Modifier.weight(1f))
                if (bid.bidStatus.isActive) {
                    TextButton(onClick = onWithdraw, contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM, vertical = 0.dp)) {
                        Text("Withdraw", color = PaceDreamColors.Error, fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Caption)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BidStatus) {
    Surface(shape = RoundedCornerShape(PaceDreamRadius.Round), color = status.color.copy(alpha = 0.12f)) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold, color = status.color
        )
    }
}

// ── Create Bid Bottom Sheet ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBidSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateBidRequest) -> Unit
) {
    var listingId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var checkIn by remember { mutableStateOf("") }
    var checkOut by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val isValid = listingId.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
        && checkIn.isNotBlank() && checkOut.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PaceDreamColors.Background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text("Place a Bid", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(PaceDreamSpacing.MD))

            OutlinedTextField(
                value = listingId, onValueChange = { listingId = it },
                label = { Text("Listing ID") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD), singleLine = true
            )
            Spacer(Modifier.height(PaceDreamSpacing.SM))

            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Bid Amount ($)") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD), singleLine = true
            )
            Spacer(Modifier.height(PaceDreamSpacing.SM))

            Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                OutlinedTextField(
                    value = checkIn, onValueChange = { checkIn = it },
                    label = { Text("Check-in") }, placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(PaceDreamRadius.MD), singleLine = true
                )
                OutlinedTextField(
                    value = checkOut, onValueChange = { checkOut = it },
                    label = { Text("Check-out") }, placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(PaceDreamRadius.MD), singleLine = true
                )
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))

            OutlinedTextField(
                value = message, onValueChange = { if (it.length <= 500) message = it },
                label = { Text("Message (optional)") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            )
            Text("${message.length}/500", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary, modifier = Modifier.align(Alignment.End))

            Spacer(Modifier.height(PaceDreamSpacing.LG))
            Button(
                onClick = {
                    onSubmit(CreateBidRequest(listingId, amount.toDoubleOrNull() ?: 0.0, checkIn, checkOut, message.ifBlank { null }))
                },
                enabled = isValid && !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.Round)
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Submit Bid", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(PaceDreamSpacing.XL))
        }
    }
}
