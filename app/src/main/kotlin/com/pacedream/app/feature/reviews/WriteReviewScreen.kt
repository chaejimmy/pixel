package com.pacedream.app.feature.reviews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

// ── UI State ────────────────────────────────────────────────────

enum class ReviewEligibility {
    LOADING,
    ELIGIBLE,
    BOOKING_NOT_COMPLETED,
    ALREADY_REVIEWED,
    BOOKING_NOT_FOUND,
    ERROR
}

data class WriteReviewUiState(
    val eligibility: ReviewEligibility = ReviewEligibility.LOADING,
    val bookingTitle: String = "",
    val bookingLocation: String = "",
    val bookingId: String = "",
    val listingId: String = "",
    val existingReviewId: String? = null,
    val rating: Int = 0,
    val comment: String = "",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null
) {
    val canSubmit: Boolean
        get() = eligibility == ReviewEligibility.ELIGIBLE && rating > 0 && !isSubmitting
}

// ── ViewModel ───────────────────────────────────────────────────

@HiltViewModel
class WriteReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewsRepository,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val bookingId: String = savedStateHandle.get<String>("bookingId").orEmpty()
    private val listingId: String = savedStateHandle.get<String>("listingId").orEmpty()
    private val bookingTitle: String = savedStateHandle.get<String>("bookingTitle").orEmpty()
    private val bookingLocation: String = savedStateHandle.get<String>("bookingLocation").orEmpty()
    private val bookingStatus: String = savedStateHandle.get<String>("bookingStatus").orEmpty()

    private val _uiState = MutableStateFlow(
        WriteReviewUiState(
            bookingId = bookingId,
            listingId = listingId,
            bookingTitle = bookingTitle,
            bookingLocation = bookingLocation
        )
    )
    val uiState: StateFlow<WriteReviewUiState> = _uiState.asStateFlow()

    // Guard against double-tap submission
    private var submissionInFlight = false

    init {
        checkEligibility()
    }

    private fun checkEligibility() {
        viewModelScope.launch {
            _uiState.update { it.copy(eligibility = ReviewEligibility.LOADING) }

            if (bookingId.isBlank()) {
                _uiState.update { it.copy(eligibility = ReviewEligibility.BOOKING_NOT_FOUND) }
                return@launch
            }

            // Check booking status first (passed from navigation or fetch fresh)
            val isCompleted = isBookingCompleted()
            if (!isCompleted) {
                _uiState.update { it.copy(eligibility = ReviewEligibility.BOOKING_NOT_COMPLETED) }
                return@launch
            }

            // Check if already reviewed by fetching user's reviews and checking for this booking
            val existingReview = checkExistingReview()
            if (existingReview != null) {
                _uiState.update {
                    it.copy(
                        eligibility = ReviewEligibility.ALREADY_REVIEWED,
                        existingReviewId = existingReview
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(eligibility = ReviewEligibility.ELIGIBLE) }
        }
    }

    private suspend fun isBookingCompleted(): Boolean {
        // First use the status passed via navigation
        if (bookingStatus.isNotBlank()) {
            val normalized = bookingStatus.trim().lowercase()
            if (normalized == "completed" || normalized == "finished") return true
            if (normalized in setOf("pending", "confirmed", "cancelled", "canceled", "rejected", "declined")) return false
        }

        // Fallback: fetch booking from API to verify status
        return try {
            val url = appConfig.buildApiUrl("bookings", bookingId)
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val parsed = json.parseToJsonElement(result.data)
                    val rootObj = parsed as? kotlinx.serialization.json.JsonObject ?: return false
                    val dataObj = rootObj["data"]
                        ?.let { it as? kotlinx.serialization.json.JsonObject } ?: rootObj
                    val status = dataObj["status"]
                        ?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
                        ?.trim()?.lowercase() ?: ""
                    status == "completed" || status == "finished"
                }
                is ApiResult.Failure -> {
                    bookingStatus.trim().lowercase().let { it == "completed" || it == "finished" }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify booking status")
            bookingStatus.trim().lowercase().let { it == "completed" || it == "finished" }
        }
    }

    private suspend fun checkExistingReview(): String? {
        return try {
            when (val result = repository.getUserReviews()) {
                is ApiResult.Success -> {
                    val reviews = result.data.resolvedReviews
                    reviews.find { it.bookingId == bookingId }?.id
                }
                is ApiResult.Failure -> null // Can't verify, allow attempt (server will reject duplicates)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check existing reviews")
            null
        }
    }

    fun setRating(value: Int) {
        _uiState.update { it.copy(rating = value, submitError = null) }
    }

    fun setComment(value: String) {
        if (value.length <= 2000) {
            _uiState.update { it.copy(comment = value, submitError = null) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(submitError = null) }
    }

    fun submitReview() {
        if (submissionInFlight) return
        val state = _uiState.value
        if (!state.canSubmit) return

        submissionInFlight = true
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val request = SubmitReviewRequest(
                    listingId = listingId.takeIf { it.isNotBlank() },
                    bookingId = bookingId.takeIf { it.isNotBlank() },
                    rate = state.rating,
                    comment = state.comment.trim()
                )
                when (val result = repository.submitReview(request)) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                    }
                    is ApiResult.Failure -> {
                        val errorMsg = result.error.message
                            ?.takeIf { it.isNotBlank() && !it.contains("<!DOCTYPE", ignoreCase = true) }
                            ?: "Something went wrong. Please try again."
                        _uiState.update { it.copy(isSubmitting = false, submitError = errorMsg) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Review submission failed")
                _uiState.update {
                    it.copy(isSubmitting = false, submitError = "Something went wrong. Please try again.")
                }
            } finally {
                submissionInFlight = false
            }
        }
    }
}

// ── Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewScreen(
    onBack: () -> Unit,
    onReviewSubmitted: () -> Unit = {},
    viewModel: WriteReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Navigate back on success after showing brief feedback
    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            kotlinx.coroutines.delay(1200)
            onReviewSubmitted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave a Review", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onBack()
                    }) {
                        Icon(
                            PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        when (uiState.eligibility) {
            ReviewEligibility.LOADING -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            }

            ReviewEligibility.BOOKING_NOT_FOUND -> {
                IneligibleState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = PaceDreamIcons.Error,
                    title = "Booking not found",
                    message = "We couldn't find the booking you're trying to review.",
                    onBack = onBack
                )
            }

            ReviewEligibility.BOOKING_NOT_COMPLETED -> {
                IneligibleState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = PaceDreamIcons.Schedule,
                    title = "Not yet available",
                    message = "Reviews can be submitted after the service is completed.",
                    onBack = onBack
                )
            }

            ReviewEligibility.ALREADY_REVIEWED -> {
                IneligibleState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = PaceDreamIcons.CheckCircle,
                    title = "Already reviewed",
                    message = "You've already submitted a review for this booking.",
                    onBack = onBack
                )
            }

            ReviewEligibility.ERROR -> {
                IneligibleState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = PaceDreamIcons.Error,
                    title = "Something went wrong",
                    message = "Please try again later.",
                    onBack = onBack
                )
            }

            ReviewEligibility.ELIGIBLE -> {
                if (uiState.submitSuccess) {
                    SuccessState(
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                } else {
                    ReviewForm(
                        uiState = uiState,
                        onRatingChanged = viewModel::setRating,
                        onCommentChanged = viewModel::setComment,
                        onSubmit = {
                            keyboardController?.hide()
                            viewModel.submitReview()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .imePadding(),
                        scrollState = scrollState
                    )
                }
            }
        }
    }

    // Error snackbar
    uiState.submitError?.let { error ->
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError()
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Review Form ─────────────────────────────────────────────────

@Composable
private fun ReviewForm(
    uiState: WriteReviewUiState,
    onRatingChanged: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = PaceDreamSpacing.MD),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PaceDreamSpacing.SM))

        // Booking context
        if (uiState.bookingTitle.isNotBlank()) {
            Text(
                text = uiState.bookingTitle,
                style = PaceDreamTypography.Body,
                fontWeight = FontWeight.Medium,
                color = PaceDreamColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.bookingLocation.isNotBlank()) {
                Spacer(Modifier.height(PaceDreamSpacing.XXS))
                Text(
                    text = uiState.bookingLocation,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(PaceDreamSpacing.LG))
        }

        // Star rating
        Text(
            text = "How was your experience?",
            style = PaceDreamTypography.Title3,
            fontWeight = FontWeight.SemiBold,
            color = PaceDreamColors.TextPrimary
        )

        Spacer(Modifier.height(PaceDreamSpacing.MD))

        StarRatingSelector(
            rating = uiState.rating,
            onRatingChanged = onRatingChanged
        )

        Spacer(Modifier.height(PaceDreamSpacing.XS))

        // Rating label
        val ratingLabel = when (uiState.rating) {
            1 -> "Poor"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Very Good"
            5 -> "Excellent"
            else -> ""
        }
        Text(
            text = ratingLabel,
            style = PaceDreamTypography.Callout,
            fontWeight = FontWeight.Medium,
            color = if (uiState.rating > 0) PaceDreamColors.Primary else Color.Transparent,
            modifier = Modifier.height(24.dp) // Fixed height to prevent layout shift
        )

        Spacer(Modifier.height(PaceDreamSpacing.LG))

        // Comment field
        Text(
            text = "Write a review (optional)",
            style = PaceDreamTypography.Body,
            fontWeight = FontWeight.Medium,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))

        OutlinedTextField(
            value = uiState.comment,
            onValueChange = onCommentChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 160.dp),
            placeholder = {
                Text(
                    "Share your experience...",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextTertiary
                )
            },
            textStyle = PaceDreamTypography.Body.copy(color = PaceDreamColors.TextPrimary),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaceDreamColors.Primary,
                unfocusedBorderColor = PaceDreamColors.Divider,
                cursorColor = PaceDreamColors.Primary
            ),
            maxLines = 6
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your review helps others make better decisions.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${uiState.comment.length}/2000",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary
            )
        }

        Spacer(Modifier.height(PaceDreamSpacing.XL))

        // Submit button
        Button(
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(PaceDreamButtonHeight.MD),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary,
                disabledContainerColor = PaceDreamColors.Divider
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(PaceDreamSpacing.SM))
                Text(
                    "Submitting...",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
            } else {
                Text(
                    "Submit Review",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.canSubmit) Color.White else PaceDreamColors.TextTertiary
                )
            }
        }

        Spacer(Modifier.height(PaceDreamSpacing.XL))
    }
}

// ── Star Rating Selector ────────────────────────────────────────

@Composable
private fun StarRatingSelector(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starIndex = index + 1
            val isSelected = starIndex <= rating
            val tint by animateColorAsState(
                targetValue = if (isSelected) PaceDreamColors.StarRating else PaceDreamColors.TextTertiary,
                animationSpec = tween(150),
                label = "starColor"
            )
            Icon(
                imageVector = if (isSelected) PaceDreamIcons.Star else PaceDreamIcons.StarOutlined,
                contentDescription = "Rate $starIndex star${if (starIndex > 1) "s" else ""}",
                tint = tint,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRatingChanged(starIndex) }
            )
        }
    }
}

// ── Ineligible State ────────────────────────────────────────────

@Composable
private fun IneligibleState(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = PaceDreamSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(PaceDreamIconSize.XXL),
            tint = PaceDreamColors.TextTertiary
        )
        Spacer(Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = title,
            style = PaceDreamTypography.Title3,
            fontWeight = FontWeight.SemiBold,
            color = PaceDreamColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(PaceDreamSpacing.LG))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            Text("Go Back", color = PaceDreamColors.Primary)
        }
    }
}

// ── Success State ───────────────────────────────────────────────

@Composable
private fun SuccessState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = PaceDreamIcons.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(PaceDreamIconSize.XXXL),
            tint = PaceDreamColors.Success
        )
        Spacer(Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = "Thank you!",
            style = PaceDreamTypography.Title2,
            fontWeight = FontWeight.Bold,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Your review has been submitted.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary
        )
    }
}
