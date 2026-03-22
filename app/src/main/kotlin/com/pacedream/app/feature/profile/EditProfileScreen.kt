package com.pacedream.app.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Edit Profile UI State
// ─────────────────────────────────────────────────────────────────────────────

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val location: String = "",
    val avatarUrl: String? = null,
    val selectedImageUri: Uri? = null
)

sealed class EditProfileEvent {
    data class ShowSuccess(val message: String) : EditProfileEvent()
    data class ShowError(val message: String) : EditProfileEvent()
}

// ─────────────────────────────────────────────────────────────────────────────
// Edit Profile ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProfileEvent>()
    val events: SharedFlow<EditProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Pre-populate from SessionManager's cached user
            sessionManager.currentUser.value?.let { user ->
                _uiState.update {
                    it.copy(
                        firstName = user.firstName ?: "",
                        lastName = user.lastName ?: "",
                        email = user.email ?: "",
                        phone = user.phone ?: "",
                        avatarUrl = user.profileImage
                    )
                }
            }

            // Fetch latest profile from API
            val url = appConfig.buildApiUrl("account", "me")
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    try {
                        val element = json.parseToJsonElement(result.data)
                        val obj = element.jsonObject
                        val userData = obj["data"]?.jsonObject
                            ?: obj["user"]?.jsonObject
                            ?: obj

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                firstName = userData["firstName"]?.jsonPrimitive?.content
                                    ?: userData["first_name"]?.jsonPrimitive?.content
                                    ?: it.firstName,
                                lastName = userData["lastName"]?.jsonPrimitive?.content
                                    ?: userData["last_name"]?.jsonPrimitive?.content
                                    ?: it.lastName,
                                email = userData["email"]?.jsonPrimitive?.content
                                    ?: it.email,
                                phone = userData["phone"]?.jsonPrimitive?.content
                                    ?: it.phone,
                                bio = userData["bio"]?.jsonPrimitive?.content
                                    ?: userData["about"]?.jsonPrimitive?.content
                                    ?: it.bio,
                                location = userData["location"]?.jsonPrimitive?.content
                                    ?: userData["city"]?.jsonPrimitive?.content
                                    ?: it.location,
                                avatarUrl = userData["profileImage"]?.jsonPrimitive?.content
                                    ?: userData["profile_image"]?.jsonPrimitive?.content
                                    ?: userData["avatar"]?.jsonPrimitive?.content
                                    ?: it.avatarUrl
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse profile response")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                is ApiResult.Failure -> {
                    Timber.w("Failed to fetch profile: ${result.error.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onFirstNameChanged(value: String) {
        _uiState.update { it.copy(firstName = value) }
    }

    fun onLastNameChanged(value: String) {
        _uiState.update { it.copy(lastName = value) }
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun onBioChanged(value: String) {
        _uiState.update { it.copy(bio = value) }
    }

    fun onLocationChanged(value: String) {
        _uiState.update { it.copy(location = value) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.firstName.isBlank()) {
                _events.emit(EditProfileEvent.ShowError("First name is required."))
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            try {
                val body = buildJsonObject {
                    put("firstName", state.firstName.trim())
                    put("lastName", state.lastName.trim())
                    put("phone", state.phone.trim())
                    put("bio", state.bio.trim())
                    put("location", state.location.trim())
                }.toString()

                val url = appConfig.buildApiUrl("account", "me")
                when (val result = apiClient.put(url, body, includeAuth = true)) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.emit(EditProfileEvent.ShowSuccess("Profile updated successfully."))
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.emit(
                            EditProfileEvent.ShowError(
                                result.error.message ?: "Failed to update profile."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save profile")
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(EditProfileEvent.ShowError("An unexpected error occurred."))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Edit Profile Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * EditProfileScreen - Edit user profile
 *
 * iOS Parity:
 * - Profile photo selection/update
 * - First name, last name, email (read-only), phone fields
 * - Bio/about text
 * - Location
 * - Save button
 * - Back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot events for snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProfileEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is EditProfileEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        style = PaceDreamTypography.Title2
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            PaceDreamColors.Primary.copy(alpha = 0.06f),
                            PaceDreamColors.Primary.copy(alpha = 0.03f),
                            PaceDreamColors.Background
                        )
                    )
                )
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = PaceDreamSpacing.MD),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                    // ── Profile Photo ────────────────────────────────────
                    ProfilePhotoSection(
                        avatarUrl = uiState.avatarUrl,
                        selectedImageUri = uiState.selectedImageUri,
                        firstName = uiState.firstName,
                        lastName = uiState.lastName,
                        onPhotoClick = { photoPickerLauncher.launch("image/*") }
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

                    // ── Form Fields ──────────────────────────────────────
                    EditProfileTextField(
                        label = "First Name",
                        value = uiState.firstName,
                        onValueChange = viewModel::onFirstNameChanged,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                    EditProfileTextField(
                        label = "Last Name",
                        value = uiState.lastName,
                        onValueChange = viewModel::onLastNameChanged,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                    EditProfileTextField(
                        label = "Email",
                        value = uiState.email,
                        onValueChange = {},
                        readOnly = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                    EditProfileTextField(
                        label = "Phone",
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChanged,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                    EditProfileTextField(
                        label = "Bio",
                        value = uiState.bio,
                        onValueChange = viewModel::onBioChanged,
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        )
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                    EditProfileTextField(
                        label = "Location",
                        value = uiState.location,
                        onValueChange = viewModel::onLocationChanged,
                        leadingIcon = {
                            Icon(
                                imageVector = PaceDreamIcons.LocationOn,
                                contentDescription = null,
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

                    // ── Save Button ──────────────────────────────────────
                    Button(
                        onClick = { viewModel.saveProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaceDreamColors.Primary,
                            disabledContainerColor = PaceDreamColors.Primary.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                "Saving...",
                                style = PaceDreamTypography.Button.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Save Changes",
                                style = PaceDreamTypography.Button.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XXL))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Photo Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfilePhotoSection(
    avatarUrl: String?,
    selectedImageUri: Uri?,
    firstName: String,
    lastName: String,
    onPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clickable(onClick = onPhotoClick),
        contentAlignment = Alignment.Center
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(6.dp, CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .clip(CircleShape)
                .background(PaceDreamColors.Gray100),
            contentAlignment = Alignment.Center
        ) {
            when {
                selectedImageUri != null -> {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                avatarUrl != null -> {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                else -> {
                    // Initials fallback (iOS parity)
                    val initials = listOf(firstName, lastName)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")
                    Text(
                        text = initials.ifEmpty { "?" },
                        style = PaceDreamTypography.Title1.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
        }

        // Camera icon overlay (bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .size(34.dp)
                .shadow(4.dp, CircleShape)
                .background(PaceDreamColors.Primary, CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.CameraAlt,
                contentDescription = "Change photo",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Styled Text Field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = PaceDreamColors.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            leadingIcon = leadingIcon,
            textStyle = PaceDreamTypography.Body.copy(
                color = if (readOnly) PaceDreamColors.TextSecondary else PaceDreamColors.TextPrimary
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaceDreamColors.Primary,
                unfocusedBorderColor = PaceDreamColors.Border,
                disabledBorderColor = PaceDreamColors.Gray200,
                focusedContainerColor = PaceDreamColors.Card,
                unfocusedContainerColor = PaceDreamColors.Card,
                cursorColor = PaceDreamColors.Primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
