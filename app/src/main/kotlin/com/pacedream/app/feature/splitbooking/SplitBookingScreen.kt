package com.pacedream.app.feature.splitbooking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

// -- Data Models (iOS SplitBookingView / SplitStayService parity) -----

enum class SplitStatus { PENDING, ACTIVE, COMPLETED, CANCELLED, EXPIRED }
enum class PaymentStatus { UNPAID, PROCESSING, PAID, FAILED, REFUNDED }

@Serializable
data class SplitParticipant(
    val id: String = "",
    @SerialName("userId") val userId: String = "",
    val name: String = "",
    @SerialName("paymentStatus") val paymentStatus: String = PaymentStatus.UNPAID.name,
    @SerialName("joinedAt") val joinedAt: String? = null
) {
    val resolvedPaymentStatus: PaymentStatus
        get() = PaymentStatus.entries.firstOrNull { it.name.equals(paymentStatus, true) } ?: PaymentStatus.UNPAID
}

@Serializable
data class HoldWindow(
    val duration: Long = 0L,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("isActive") val isActive: Boolean = false,
    @SerialName("remainingTime") val remainingTime: Long = 0L
)

@Serializable
data class SplitBookingData(
    val id: String = "",
    @SerialName("bookingId") val bookingId: String = "",
    @SerialName("roomId") val roomId: String? = null,
    @SerialName("totalAmount") val totalAmount: Double = 0.0,
    @SerialName("splitAmount") val splitAmount: Double = 0.0,
    val participants: List<SplitParticipant> = emptyList(),
    val status: String = SplitStatus.PENDING.name,
    @SerialName("paymentStatus") val paymentStatus: String = PaymentStatus.UNPAID.name,
    @SerialName("holdWindow") val holdWindow: HoldWindow? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
    val messages: List<SplitMessage> = emptyList()
) {
    val resolvedStatus: SplitStatus
        get() = SplitStatus.entries.firstOrNull { it.name.equals(status, true) } ?: SplitStatus.PENDING
    val paidCount: Int get() = participants.count { it.resolvedPaymentStatus == PaymentStatus.PAID }
}

@Serializable
data class SplitMessage(
    val id: String = "",
    val text: String = "",
    val type: String = "system",
    @SerialName("createdAt") val createdAt: String = ""
)

@Serializable
data class SplitBookingEnvelope(
    val status: Boolean? = null,
    val data: SplitBookingData? = null
)

@Serializable
data class SplitListEnvelope(
    val status: Boolean? = null,
    val data: List<SplitBookingData>? = null,
    val active: List<SplitBookingData>? = null,
    val history: List<SplitBookingData>? = null
)

// -- Repository -------------------------------------------------------

@Singleton
class SplitBookingRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    suspend fun createSplit(bookingId: String, roomId: String?): ApiResult<SplitBookingData> {
        val url = appConfig.buildApiUrl("split-bookings")
        val body = buildString {
            append("""{"bookingId":"$bookingId"""")
            roomId?.let { append(""","roomId":"$it"""") }
            append("}")
        }
        return parseOne(apiClient.post(url, body, includeAuth = true))
    }

    suspend fun getSplit(id: String): ApiResult<SplitBookingData> =
        parseOne(apiClient.get(appConfig.buildApiUrl("split-bookings", id), includeAuth = true))

    suspend fun joinSplit(splitId: String): ApiResult<SplitBookingData> =
        parseOne(apiClient.post(appConfig.buildApiUrl("split-bookings", "join"), """{"splitId":"$splitId"}""", includeAuth = true))

    suspend fun declineSplit(splitId: String): ApiResult<SplitBookingData> =
        parseOne(apiClient.post(appConfig.buildApiUrl("split-bookings", splitId, "decline"), "{}", includeAuth = true))

    suspend fun processPayment(splitId: String): ApiResult<SplitBookingData> =
        parseOne(apiClient.post(appConfig.buildApiUrl("split-bookings", splitId, "pay"), "{}", includeAuth = true))

    suspend fun getActiveSplits(): ApiResult<List<SplitBookingData>> =
        parseList(apiClient.get(appConfig.buildApiUrl("split-bookings", queryParams = mapOf("status" to "active")), includeAuth = true))

    suspend fun getSplitHistory(): ApiResult<List<SplitBookingData>> =
        parseList(apiClient.get(appConfig.buildApiUrl("split-bookings", queryParams = mapOf("status" to "history")), includeAuth = true))

    private fun parseOne(result: ApiResult<String>): ApiResult<SplitBookingData> = when (result) {
        is ApiResult.Success -> try {
            val envelope = json.decodeFromString(SplitBookingEnvelope.serializer(), result.data)
            envelope.data?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ApiError.DecodingError())
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse split booking"); ApiResult.Failure(ApiError.DecodingError())
        }
        is ApiResult.Failure -> result
    }

    private fun parseList(result: ApiResult<String>): ApiResult<List<SplitBookingData>> = when (result) {
        is ApiResult.Success -> try {
            val envelope = json.decodeFromString(SplitListEnvelope.serializer(), result.data)
            ApiResult.Success(envelope.data ?: envelope.active ?: envelope.history ?: emptyList())
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse split list"); ApiResult.Failure(ApiError.DecodingError())
        }
        is ApiResult.Failure -> result
    }
}

// -- ViewModel --------------------------------------------------------

data class SplitBookingUiState(
    val split: SplitBookingData? = null,
    val activeSplits: List<SplitBookingData> = emptyList(),
    val historySplits: List<SplitBookingData> = emptyList(),
    val countdownSeconds: Long = 0L,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SplitBookingViewModel @Inject constructor(
    private val repository: SplitBookingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplitBookingUiState())
    val uiState: StateFlow<SplitBookingUiState> = _uiState.asStateFlow()

    fun loadSplit(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getSplit(id)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(split = result.data, isLoading = false, isRefreshing = false) }
                    result.data.holdWindow?.let { startCountdown(it.remainingTime) }
                }
                is ApiResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = "Failed to load split booking")
                }
            }
        }
    }

    fun loadSplitList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val activeResult = repository.getActiveSplits()
            val historyResult = repository.getSplitHistory()
            _uiState.update {
                it.copy(
                    activeSplits = (activeResult as? ApiResult.Success)?.data ?: emptyList(),
                    historySplits = (historyResult as? ApiResult.Success)?.data ?: emptyList(),
                    isLoading = false, isRefreshing = false
                )
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        _uiState.value.split?.let { loadSplit(it.id) } ?: loadSplitList()
    }

    fun joinSplit(splitId: String) { performAction { repository.joinSplit(splitId) } }
    fun declineSplit(splitId: String) { performAction { repository.declineSplit(splitId) } }
    fun processPayment(splitId: String) { performAction { repository.processPayment(splitId) } }

    private fun performAction(action: suspend () -> ApiResult<SplitBookingData>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            when (val result = action()) {
                is ApiResult.Success -> _uiState.update { it.copy(split = result.data, isProcessing = false) }
                is ApiResult.Failure -> _uiState.update { it.copy(isProcessing = false, error = "Action failed. Please try again.") }
            }
        }
    }

    private fun startCountdown(initialSeconds: Long) {
        viewModelScope.launch {
            var remaining = initialSeconds
            while (remaining > 0) {
                _uiState.update { it.copy(countdownSeconds = remaining) }
                delay(1_000L); remaining--
            }
            _uiState.update { it.copy(countdownSeconds = 0) }
        }
    }
}

// -- SplitBookingScreen -----------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBookingScreen(
    splitId: String,
    onBackClick: () -> Unit = {},
    viewModel: SplitBookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(splitId) { viewModel.loadSplit(splitId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Booking", style = PaceDreamTypography.Title1, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing, onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
                uiState.error != null && uiState.split == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error", color = PaceDreamColors.TextSecondary)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Button(onClick = { viewModel.loadSplit(splitId) }) { Text("Retry") }
                    }
                }
                else -> uiState.split?.let { split ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD)
                    ) {
                        item { StatusCard(split); Spacer(Modifier.height(PaceDreamSpacing.MD)) }
                        item { PaymentInfoCard(split); Spacer(Modifier.height(PaceDreamSpacing.MD)) }
                        if (split.holdWindow?.isActive == true) {
                            item { HoldWindowCard(uiState.countdownSeconds); Spacer(Modifier.height(PaceDreamSpacing.MD)) }
                        }
                        item {
                            ActionButtons(split, uiState.isProcessing,
                                onJoin = { viewModel.joinSplit(split.id) },
                                onPay = { viewModel.processPayment(split.id) },
                                onDecline = { viewModel.declineSplit(split.id) })
                            Spacer(Modifier.height(PaceDreamSpacing.MD))
                        }
                        if (split.messages.isNotEmpty()) {
                            item { Text("Activity", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold); Spacer(Modifier.height(PaceDreamSpacing.SM)) }
                            items(split.messages, key = { it.id }) { msg -> MessageRow(msg); Spacer(Modifier.height(PaceDreamSpacing.XS)) }
                        }
                        item { Spacer(Modifier.height(PaceDreamSpacing.XL)) }
                    }
                }
            }
        }
    }
}

// -- Detail composables -----------------------------------------------

@Composable
private fun StatusCard(split: SplitBookingData) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column(Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
                Spacer(Modifier.weight(1f)); StatusBadge(split.resolvedStatus)
            }
            Spacer(Modifier.height(PaceDreamSpacing.MD))
            Text("Participants", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            split.participants.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(PaceDreamColors.Primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(p.name.take(1).uppercase(), style = PaceDreamTypography.Caption, fontWeight = FontWeight.Bold, color = PaceDreamColors.Primary)
                    }
                    Spacer(Modifier.width(PaceDreamSpacing.SM))
                    Text(p.name, style = PaceDreamTypography.Body, modifier = Modifier.weight(1f))
                    PaymentStatusBadge(p.resolvedPaymentStatus)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SplitStatus) {
    val (color, label) = when (status) {
        SplitStatus.PENDING -> PaceDreamColors.Warning to "Pending"
        SplitStatus.ACTIVE -> PaceDreamColors.Primary to "Active"
        SplitStatus.COMPLETED -> Color(0xFF34C759) to "Completed"
        SplitStatus.CANCELLED -> PaceDreamColors.Error to "Cancelled"
        SplitStatus.EXPIRED -> PaceDreamColors.TextSecondary to "Expired"
    }
    Surface(shape = RoundedCornerShape(PaceDreamRadius.Round), color = color.copy(alpha = 0.12f)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun PaymentStatusBadge(status: PaymentStatus) {
    val (color, label) = when (status) {
        PaymentStatus.UNPAID -> PaceDreamColors.TextSecondary to "Unpaid"
        PaymentStatus.PROCESSING -> PaceDreamColors.Warning to "Processing"
        PaymentStatus.PAID -> Color(0xFF34C759) to "Paid"
        PaymentStatus.FAILED -> PaceDreamColors.Error to "Failed"
        PaymentStatus.REFUNDED -> PaceDreamColors.TextSecondary to "Refunded"
    }
    Text(label, style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold, color = color)
}

@Composable
private fun PaymentInfoCard(split: SplitBookingData) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column(Modifier.padding(PaceDreamSpacing.MD)) {
            Text("Payment Details", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            PaymentRow("Total Amount", String.format("$%.2f", split.totalAmount))
            PaymentRow("Per Person", String.format("$%.2f", split.splitAmount))
            PaymentRow("Participants", "${split.participants.size}")
            PaymentRow("Paid", "${split.paidCount} / ${split.participants.size}")
        }
    }
}

@Composable
private fun PaymentRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HoldWindowCard(countdownSeconds: Long) {
    val minutes = countdownSeconds / 60; val seconds = countdownSeconds % 60
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Warning.copy(alpha = 0.1f))) {
        Row(Modifier.padding(PaceDreamSpacing.MD), verticalAlignment = Alignment.CenterVertically) {
            Icon(PaceDreamIcons.Clock, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(PaceDreamSpacing.SM))
            Column(Modifier.weight(1f)) {
                Text("Hold Window Active", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
                Text("Room held while payment is completed", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
            }
            Text(String.format("%02d:%02d", minutes, seconds), style = PaceDreamTypography.Title2,
                fontWeight = FontWeight.Bold, color = if (countdownSeconds < 300) PaceDreamColors.Error else PaceDreamColors.Warning)
        }
    }
}

@Composable
private fun ActionButtons(split: SplitBookingData, isProcessing: Boolean, onJoin: () -> Unit, onPay: () -> Unit, onDecline: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
        when (split.resolvedStatus) {
            SplitStatus.PENDING -> {
                Button(onClick = onJoin, enabled = !isProcessing, modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.Round)) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Join Split", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onDecline, enabled = !isProcessing, modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                    shape = RoundedCornerShape(PaceDreamRadius.Round)) {
                    Text("Decline", fontWeight = FontWeight.SemiBold, color = PaceDreamColors.Error)
                }
            }
            SplitStatus.ACTIVE -> {
                Button(onClick = onPay, enabled = !isProcessing, modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.Round)) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Pay My Share", fontWeight = FontWeight.SemiBold)
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun MessageRow(message: SplitMessage) {
    Surface(shape = RoundedCornerShape(PaceDreamRadius.SM), color = PaceDreamColors.Divider.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(PaceDreamSpacing.SM), verticalAlignment = Alignment.CenterVertically) {
            Icon(PaceDreamIcons.Info, null, Modifier.size(16.dp), tint = PaceDreamColors.TextSecondary)
            Spacer(Modifier.width(PaceDreamSpacing.SM))
            Column(Modifier.weight(1f)) {
                Text(message.text, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextPrimary)
                Text(message.createdAt.take(16), style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
            }
        }
    }
}

// -- SplitBookingListScreen -------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBookingListScreen(
    onBackClick: () -> Unit = {},
    onSplitClick: (String) -> Unit = {},
    viewModel: SplitBookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadSplitList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Bookings", style = PaceDreamTypography.Title1, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
                uiState.activeSplits.isEmpty() && uiState.historySplits.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No split bookings yet", color = PaceDreamColors.TextSecondary)
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD)) {
                    if (uiState.activeSplits.isNotEmpty()) {
                        item { Text("Active", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold); Spacer(Modifier.height(PaceDreamSpacing.SM)) }
                        items(uiState.activeSplits, key = { it.id }) { split ->
                            SplitListCard(split) { onSplitClick(split.id) }; Spacer(Modifier.height(PaceDreamSpacing.SM))
                        }
                    }
                    if (uiState.historySplits.isNotEmpty()) {
                        item { Spacer(Modifier.height(PaceDreamSpacing.MD)); Text("History", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold); Spacer(Modifier.height(PaceDreamSpacing.SM)) }
                        items(uiState.historySplits, key = { it.id }) { split ->
                            SplitListCard(split) { onSplitClick(split.id) }; Spacer(Modifier.height(PaceDreamSpacing.SM))
                        }
                    }
                    item { Spacer(Modifier.height(PaceDreamSpacing.XL)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitListCard(split: SplitBookingData, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)) {
        Column(Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Booking #${split.bookingId.takeLast(6)}", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
                    Text("${split.participants.size} participants", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                }
                StatusBadge(split.resolvedStatus)
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Row {
                Text("Total: ", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                Text(String.format("$%.2f", split.totalAmount), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(PaceDreamSpacing.MD))
                Text("Your share: ", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                Text(String.format("$%.2f", split.splitAmount), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold, color = PaceDreamColors.Primary)
            }
        }
    }
}
