package com.pacedream.app.feature.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import com.pacedream.common.composables.theme.PaceDreamSpacing
// ── Data Models (iOS ReviewModel parity) ─────────────────────────

@Serializable
data class ReviewUser(
    val id: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    @SerialName("profileImage") val profileImage: String? = null
) {
    val resolvedName: String get() = name ?: "Anonymous"
    val resolvedAvatar: String? get() = avatar ?: profileImage
}

@Serializable
data class ReviewCategoryRatings(
    val cleanliness: Double? = null,
    val accuracy: Double? = null,
    val communication: Double? = null,
    val location: Double? = null,
    @SerialName("checkIn") val checkIn: Double? = null,
    val value: Double? = null,
    val comfort: Double? = null,
    val convenience: Double? = null
)

@Serializable
data class ReviewHostResponse(
    val comment: String? = null,
    @SerialName("createdAt") val createdAt: String? = null
)

@Serializable
data class Review(
    val id: String = "",
    val user: ReviewUser? = null,
    @SerialName("overallRating") val overallRating: Double = 0.0,
    val rating: Double = 0.0,
    @SerialName("rate") val rate: Double = 0.0,
    @SerialName("categoryRatings") val categoryRatings: ReviewCategoryRatings? = null,
    val comment: String? = null,
    val photos: List<String> = emptyList(),
    @SerialName("hostResponse") val hostResponse: ReviewHostResponse? = null,
    @SerialName("helpfulCount") val helpfulCount: Int = 0,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("listingId") val listingId: String? = null,
    @SerialName("bookingId") val bookingId: String? = null,
    val status: String? = null
) {
    val resolvedRating: Double get() = when {
        overallRating > 0 -> overallRating
        rating > 0 -> rating
        rate > 0 -> rate
        else -> 0.0
    }
}

@Serializable
data class ReviewsEnvelope(
    val status: Boolean? = null,
    val success: Boolean? = null,
    val data: List<Review>? = null,
    val reviews: List<Review>? = null,
    val total: Int? = null,
    val page: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
    @SerialName("ratingBreakdown") val ratingBreakdown: List<RatingBreakdownItem>? = null
) {
    val resolvedReviews: List<Review> get() = data ?: reviews ?: emptyList()
}

@Serializable
data class RatingBreakdownItem(
    val rating: Int = 0,
    val count: Int = 0,
    val percentage: Double = 0.0
)

@Serializable
data class SubmitReviewRequest(
    @SerialName("listing_id") val listingId: String? = null,
    @SerialName("booking_id") val bookingId: String? = null,
    val rate: Int = 0,
    val comment: String = "",
    @SerialName("categoryRatings") val categoryRatings: ReviewCategoryRatings? = null
)

enum class ReviewTab(val label: String) {
    ALL("All"),
    POSITIVE("Positive"),
    NEGATIVE("Negative"),
    WITH_PHOTOS("With Photos")
}

// ── Repository ───────────────────────────────────────────────────

@Singleton
class ReviewsRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    suspend fun getUserReviews(page: Int = 1, limit: Int = 20): ApiResult<ReviewsEnvelope> {
        val url = appConfig.buildApiUrl("reviews", queryParams = mapOf("page" to page.toString(), "limit" to limit.toString()))
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(json.decodeFromString(ReviewsEnvelope.serializer(), result.data))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse reviews")
                    ApiResult.Failure(com.pacedream.app.core.network.ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun getPropertyReviews(propertyId: String, page: Int = 1): ApiResult<ReviewsEnvelope> {
        val url = appConfig.buildApiUrl("reviews", "property", propertyId, queryParams = mapOf("page" to page.toString(), "limit" to "20"))
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(json.decodeFromString(ReviewsEnvelope.serializer(), result.data))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse property reviews")
                    ApiResult.Failure(com.pacedream.app.core.network.ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun submitReview(request: SubmitReviewRequest): ApiResult<String> {
        val url = appConfig.buildApiUrl("reviews")
        val body = json.encodeToString(SubmitReviewRequest.serializer(), request)
        return apiClient.post(url, body, includeAuth = true)
    }

    suspend fun updateReview(reviewId: String, request: SubmitReviewRequest): ApiResult<String> {
        val url = appConfig.buildApiUrl("reviews", reviewId)
        val body = json.encodeToString(SubmitReviewRequest.serializer(), request)
        return apiClient.put(url, body, includeAuth = true)
    }

    suspend fun deleteReview(reviewId: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("reviews", reviewId)
        return apiClient.delete(url, includeAuth = true)
    }

    suspend fun markHelpful(reviewId: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("reviews", reviewId, "helpful")
        return apiClient.post(url, "{}", includeAuth = true)
    }

    suspend fun reportReview(reviewId: String, reason: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("reviews", reviewId, "report")
        val body = json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
            }
        )
        return apiClient.post(url, body, includeAuth = true)
    }
}

// ── ViewModel ────────────────────────────────────────────────────

data class ReviewsUiState(
    val reviews: List<Review> = emptyList(),
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val ratingBreakdown: List<RatingBreakdownItem> = emptyList(),
    val selectedTab: ReviewTab = ReviewTab.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showWriteReview: Boolean = false,
    /** Current user id used to decide which reviews expose edit/delete. */
    val currentUserId: String? = null,
    /** True while an edit or delete request is in flight. */
    val isMutating: Boolean = false
) {
    val filteredReviews: List<Review> get() = when (selectedTab) {
        ReviewTab.ALL -> reviews
        ReviewTab.POSITIVE -> reviews.filter { it.resolvedRating >= 4.0 }
        ReviewTab.NEGATIVE -> reviews.filter { it.resolvedRating < 3.0 }
        ReviewTab.WITH_PHOTOS -> reviews.filter { it.photos.isNotEmpty() }
    }
}

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val repository: ReviewsRepository,
    private val sessionManager: com.pacedream.app.core.auth.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsUiState(isLoading = true))
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    init {
        loadReviews()
        // Observe the current user id so the screen can light up the
        // edit / delete affordance only on rows owned by the signed-in
        // user.  When unauthenticated the id is null and every row
        // renders as read-only, exactly like today.
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                _uiState.update { it.copy(currentUserId = user?.id) }
            }
        }
    }

    fun loadReviews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getUserReviews()) {
                is ApiResult.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            reviews = data.resolvedReviews,
                            averageRating = data.averageRating ?: 0.0,
                            totalReviews = data.total ?: data.resolvedReviews.size,
                            ratingBreakdown = data.ratingBreakdown ?: emptyList(),
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "Failed to load reviews") }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadReviews()
    }

    fun selectTab(tab: ReviewTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleWriteReview() {
        _uiState.update { it.copy(showWriteReview = !it.showWriteReview) }
    }

    fun markHelpful(reviewId: String) {
        viewModelScope.launch {
            try {
                repository.markHelpful(reviewId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark review as helpful")
            }
        }
    }

    fun submitReview(listingId: String?, rating: Int, comment: String) {
        viewModelScope.launch {
            try {
                val request = SubmitReviewRequest(listingId = listingId, rate = rating, comment = comment)
                when (repository.submitReview(request)) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(showWriteReview = false) }
                        loadReviews()
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(error = "Failed to submit review") }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to submit review")
                _uiState.update { it.copy(error = "Failed to submit review") }
            }
        }
    }

    fun deleteReview(reviewId: String) {
        if (reviewId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null) }
            try {
                when (repository.deleteReview(reviewId)) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(isMutating = false) }
                        loadReviews()
                    }
                    is ApiResult.Failure -> {
                        _uiState.update {
                            it.copy(isMutating = false, error = "Failed to delete review")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete review")
                _uiState.update { it.copy(isMutating = false, error = "Failed to delete review") }
            }
        }
    }

    /**
     * Update an existing review's rating and comment.  Uses the same
     * SubmitReviewRequest shape as submitReview so the backend contract
     * stays a single source of truth.  categoryRatings are left null
     * because the mobile write UI does not collect them today.
     */
    fun updateReview(reviewId: String, rating: Int, comment: String) {
        if (reviewId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null) }
            try {
                val request = SubmitReviewRequest(
                    rate = rating,
                    comment = comment,
                )
                when (repository.updateReview(reviewId, request)) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(isMutating = false) }
                        loadReviews()
                    }
                    is ApiResult.Failure -> {
                        _uiState.update {
                            it.copy(isMutating = false, error = "Failed to update review")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update review")
                _uiState.update { it.copy(isMutating = false, error = "Failed to update review") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// ── Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit = {},
    viewModel: ReviewsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Edit / delete drivers — local to this screen so the sheet and
    // dialog can stay simple; the VM is the source of truth for the
    // network action itself.
    var editTarget by remember { mutableStateOf<Review?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.toggleWriteReview() }) {
                        Text(
                            "Write Review",
                            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                            color = PaceDreamColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            } else if (uiState.error != null && uiState.reviews.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error", color = PaceDreamColors.TextSecondary)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Button(onClick = { viewModel.loadReviews() }) { Text("Retry") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD)
                ) {
                    // Rating Summary
                    item {
                        ReviewsSummaryCard(
                            averageRating = uiState.averageRating,
                            totalReviews = uiState.totalReviews,
                            breakdown = uiState.ratingBreakdown
                        )
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                    }

                    // Tab Selector
                    item {
                        ReviewTabSelector(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                    }

                    // Reviews List
                    val filtered = uiState.filteredReviews
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = PaceDreamSpacing.XXL),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No reviews in this category", color = PaceDreamColors.TextSecondary)
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { review ->
                            val isMine = !uiState.currentUserId.isNullOrBlank() &&
                                !review.user?.id.isNullOrBlank() &&
                                review.user?.id == uiState.currentUserId
                            ReviewCard(
                                review = review,
                                isMine = isMine,
                                isMutating = uiState.isMutating,
                                onHelpful = { viewModel.markHelpful(review.id) },
                                onEdit = { editTarget = review },
                                onDelete = { pendingDeleteId = review.id },
                            )
                            Spacer(Modifier.height(PaceDreamSpacing.SM))
                        }
                    }

                    item { Spacer(Modifier.height(PaceDreamSpacing.XL)) }
                }
            }
        }
    }

    // Write Review Bottom Sheet
    if (uiState.showWriteReview) {
        WriteReviewSheet(
            onDismiss = { viewModel.toggleWriteReview() },
            onSubmit = { rating, comment -> viewModel.submitReview(null, rating, comment) }
        )
    }

    // Edit Review Bottom Sheet — rating + comment only (categoryRatings
    // are left untouched because the mobile write UI does not collect
    // them; the backend PATCH preserves anything we do not send).
    editTarget?.let { target ->
        EditReviewSheet(
            initialRating = target.resolvedRating.toInt().coerceIn(0, 5),
            initialComment = target.comment.orEmpty(),
            isSubmitting = uiState.isMutating,
            onDismiss = { editTarget = null },
            onSubmit = { rating, comment ->
                viewModel.updateReview(target.id, rating, comment)
                editTarget = null
            },
        )
    }

    // Delete confirmation — destructive action must be explicit.
    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = {
                Text(
                    text = "Delete this review?",
                    style = PaceDreamTypography.Title3,
                )
            },
            text = {
                Text(
                    text = "This will permanently remove your review. You can always write a new one later.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReview(id)
                        pendingDeleteId = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel", color = PaceDreamColors.TextPrimary)
                }
            },
            containerColor = PaceDreamColors.CardBackground,
            shape = RoundedCornerShape(PaceDreamRadius.LG),
        )
    }
}

@Composable
private fun ReviewsSummaryCard(
    averageRating: Double,
    totalReviews: Int,
    breakdown: List<RatingBreakdownItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f", averageRating),
                    style = PaceDreamTypography.LargeTitle,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(Modifier.width(PaceDreamSpacing.SM))
                Column {
                    Row {
                        repeat(5) { i ->
                            Icon(
                                PaceDreamIcons.Star,
                                contentDescription = null,
                                tint = if (i < averageRating.toInt()) PaceDreamColors.Warning else PaceDreamColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        "Based on $totalReviews reviews",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            if (breakdown.isNotEmpty()) {
                Spacer(Modifier.height(PaceDreamSpacing.MD))
                breakdown.sortedByDescending { it.rating }.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = PaceDreamSpacing.XXS)
                    ) {
                        Text("${item.rating}", style = PaceDreamTypography.Caption, modifier = Modifier.width(16.dp))
                        Icon(PaceDreamIcons.Star, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(PaceDreamSpacing.XS))
                        LinearProgressIndicator(
                            progress = { (item.percentage / 100).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(PaceDreamRadius.XS)),
                            color = PaceDreamColors.Primary,
                            trackColor = PaceDreamColors.Divider
                        )
                        Spacer(Modifier.width(PaceDreamSpacing.XS))
                        Text("${item.count}", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewTabSelector(
    selectedTab: ReviewTab,
    onTabSelected: (ReviewTab) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
        ReviewTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            FilterChip(
                selected = selected,
                onClick = { onTabSelected(tab) },
                label = { Text(tab.label, style = PaceDreamTypography.Caption, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PaceDreamColors.Primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    isMine: Boolean = false,
    isMutating: Boolean = false,
    onHelpful: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.user?.resolvedAvatar,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(PaceDreamColors.Divider)
                )
                Spacer(Modifier.width(PaceDreamSpacing.SM))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.user?.resolvedName ?: "Guest", fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Body)
                    Text(review.createdAt.take(10), style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                }
                Row {
                    repeat(5) { i ->
                        Icon(
                            PaceDreamIcons.Star, null,
                            tint = if (i < review.resolvedRating.toInt()) PaceDreamColors.Warning else PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.width(PaceDreamSpacing.XS))
                    Text(String.format("%.1f", review.resolvedRating), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
                }
            }

            // Category ratings
            review.categoryRatings?.let { cats ->
                Spacer(Modifier.height(PaceDreamSpacing.SM))
                Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    cats.cleanliness?.let { CategoryRatingChip("Cleanliness", it) }
                    cats.accuracy?.let { CategoryRatingChip("Accuracy", it) }
                    cats.communication?.let { CategoryRatingChip("Communication", it) }
                    cats.value?.let { CategoryRatingChip("Value", it) }
                }
            }

            if (!review.comment.isNullOrBlank()) {
                Spacer(Modifier.height(PaceDreamSpacing.SM))
                Text(
                    review.comment,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Host Response
            review.hostResponse?.let { response ->
                Spacer(Modifier.height(PaceDreamSpacing.SM))
                Surface(
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    color = PaceDreamColors.Primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(PaceDreamSpacing.SM)) {
                        Text("Host Response", style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold, color = PaceDreamColors.Primary)
                        Spacer(Modifier.height(PaceDreamSpacing.XS))
                        Text(response.comment ?: "", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextPrimary)
                    }
                }
            }

            // Actions
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // intentional: 0.dp contentPadding so the TextButton's icon+label hug the leading edge
                TextButton(onClick = onHelpful, contentPadding = PaddingValues(0.dp)) {
                    Icon(PaceDreamIcons.ThumbUp, null, modifier = Modifier.size(14.dp), tint = PaceDreamColors.TextSecondary)
                    Spacer(Modifier.width(PaceDreamSpacing.XS))
                    Text("Helpful (${review.helpfulCount})", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                }

                // Owner-only affordances — hidden on other users' reviews so
                // the card reads identically for read-only viewers.
                if (isMine) {
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onEdit,
                        enabled = !isMutating,
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM),
                    ) {
                        Icon(
                            PaceDreamIcons.Edit,
                            contentDescription = "Edit review",
                            modifier = Modifier.size(14.dp),
                            tint = PaceDreamColors.Primary,
                        )
                        Spacer(Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            "Edit",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    TextButton(
                        onClick = onDelete,
                        enabled = !isMutating,
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM),
                    ) {
                        Icon(
                            PaceDreamIcons.Delete,
                            contentDescription = "Delete review",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            "Delete",
                            style = PaceDreamTypography.Caption,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRatingChip(label: String, rating: Double) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.SM),
        color = PaceDreamColors.Divider.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
            Spacer(Modifier.width(PaceDreamSpacing.XS))
            Text(String.format("%.1f", rating), style = PaceDreamTypography.Caption, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteReviewSheet(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PaceDreamColors.Background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text("Write a Review", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(PaceDreamSpacing.MD))

            Text("Overall Rating", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(PaceDreamSpacing.XS))
            Row {
                repeat(5) { i ->
                    val stars = i + 1
                    IconButton(onClick = { rating = stars }) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = if (stars == 1) "Rate 1 star" else "Rate $stars stars",
                            tint = if (i < rating) PaceDreamColors.Warning else PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(PaceDreamSpacing.MD))
            Text("Your Review", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(PaceDreamSpacing.XS))
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 2000) comment = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Share your experience...") },
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            )
            Text("${comment.length}/2000", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary, modifier = Modifier.align(Alignment.End))

            Spacer(Modifier.height(PaceDreamSpacing.LG))
            Button(
                onClick = { onSubmit(rating, comment) },
                enabled = rating > 0,
                modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.Round)
            ) {
                Text("Submit Review", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(PaceDreamSpacing.XL))
        }
    }
}

/**
 * Bottom sheet used to edit a review that already exists.  Prepopulated
 * with the review's current rating and comment; submit button mirrors
 * the loading state driven by the ViewModel's isMutating flag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReviewSheet(
    initialRating: Int,
    initialComment: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var rating by remember { mutableIntStateOf(initialRating.coerceIn(0, 5)) }
    var comment by remember { mutableStateOf(initialComment) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PaceDreamColors.Background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text("Edit your review", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(PaceDreamSpacing.MD))

            Text("Overall Rating", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(PaceDreamSpacing.XS))
            Row {
                repeat(5) { i ->
                    val stars = i + 1
                    IconButton(onClick = { rating = stars }) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = if (stars == 1) "Rate 1 star" else "Rate $stars stars",
                            tint = if (i < rating) PaceDreamColors.Warning else PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(PaceDreamSpacing.MD))
            Text("Your Review", style = PaceDreamTypography.Body, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(PaceDreamSpacing.XS))
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 2000) comment = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Share your experience...") },
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            )
            Text(
                "${comment.length}/2000",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary,
                modifier = Modifier.align(Alignment.End),
            )

            Spacer(Modifier.height(PaceDreamSpacing.LG))
            Button(
                onClick = { onSubmit(rating, comment) },
                enabled = rating > 0 && !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(PaceDreamSpacing.SM))
                }
                Text("Save changes", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(PaceDreamSpacing.XL))
        }
    }
}
