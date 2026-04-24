@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.shourov.apps.pacedream.feature.inbox.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.components.PaceDreamErrorState
import com.pacedream.common.composables.components.PaceDreamLoadingState
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.inbox.model.Message
import com.shourov.apps.pacedream.feature.inbox.model.MessageStatus
import com.shourov.apps.pacedream.feature.inbox.model.ThreadDetailEvent
import com.shourov.apps.pacedream.feature.inbox.model.ThreadDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    threadId: String,
    viewModel: ThreadViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    (uiState as? ThreadDetailUiState.Success)?.let { state ->
                        Text(
                            text = state.thread.displayName,
                            style = PaceDreamTypography.Headline
                        )
                    } ?: Text("Chat", style = PaceDreamTypography.Headline)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "thread_state"
        ) { state ->
            when (state) {
                is ThreadDetailUiState.Loading -> PaceDreamLoadingState(
                    message = "Loading messages…",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
                is ThreadDetailUiState.Success -> SuccessState(
                    state = state,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.padding(padding)
                )
                is ThreadDetailUiState.Error -> PaceDreamErrorState(
                    title = "Couldn't load chat",
                    description = state.message,
                    onRetryClick = { viewModel.onEvent(ThreadDetailEvent.Refresh) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessState(
    state: ThreadDetailUiState.Success,
    onEvent: (ThreadDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var messageText by rememberSaveable { mutableStateOf("") }

    // Scroll to bottom when new message is sent
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Messages list (reversed so newest at bottom, displayed from top)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(ThreadDetailEvent.Refresh) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true, // Newest messages at bottom
                contentPadding = PaddingValues(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM
                ),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = state.messages,
                    key = { _, msg -> msg.id }
                ) { index, message ->
                    val previous = state.messages.getOrNull(index - 1)
                    val next = state.messages.getOrNull(index + 1)
                    val isCurrentUser = message.senderId == state.currentUserId
                    val prevSameSender = previous?.senderId == message.senderId
                    val nextSameSender = next?.senderId == message.senderId

                    MessageBubble(
                        message = message,
                        isCurrentUser = isCurrentUser,
                        isGroupedWithPrevious = prevSameSender,
                        isGroupedWithNext = nextSameSender,
                        onRetry = { onEvent(ThreadDetailEvent.RetryMessage(message.id)) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }

                // Load more indicator
                if (state.hasMore) {
                    item(key = "load_more") {
                        if (!state.isLoadingMore) {
                            LaunchedEffect(Unit) {
                                onEvent(ThreadDetailEvent.LoadMore)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaceDreamSpacing.MD),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PaceDreamColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Send error banner (matches iOS moderation/error banner pattern)
        state.sendError?.let { error ->
            SendErrorBanner(
                message = error,
                onDismiss = { onEvent(ThreadDetailEvent.DismissSendError) }
            )
        }

        // Upload progress bar
        AnimatedVisibility(visible = state.isUploading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = PaceDreamColors.Primary
            )
        }

        // Photo preview tray
        AnimatedVisibility(visible = state.pendingPhotoUris.isNotEmpty()) {
            PhotoPreviewTray(
                photoUris = state.pendingPhotoUris,
                onRemove = { uri -> onEvent(ThreadDetailEvent.RemovePhoto(uri)) }
            )
        }

        // Message input with photo attachment
        MessageInputBar(
            text = messageText,
            onTextChange = { messageText = it },
            onSend = {
                if (state.pendingPhotoUris.isNotEmpty()) {
                    onEvent(ThreadDetailEvent.SendMediaMessage(messageText, state.pendingPhotoUris))
                    messageText = ""
                } else if (messageText.isNotBlank()) {
                    onEvent(ThreadDetailEvent.SendMessage(messageText))
                    messageText = ""
                }
            },
            onAttachPhotos = { uris -> onEvent(ThreadDetailEvent.AddPhotos(uris)) },
            isSending = state.isSending || state.isUploading,
            canSend = state.canSend && (messageText.isNotBlank() || state.pendingPhotoUris.isNotEmpty()),
            attachmentsEnabled = state.attachmentsEnabled
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    isGroupedWithPrevious: Boolean,
    isGroupedWithNext: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val maxWidth = (configuration.screenWidthDp * 0.75).dp

    // When messages are consecutive from the same sender, tighten the bubble corners
    // so the conversation reads as grouped "blocks".
    val topStart = if (isCurrentUser) {
        PaceDreamRadius.MD
    } else {
        if (isGroupedWithPrevious) PaceDreamRadius.SM else PaceDreamRadius.MD
    }
    val topEnd = if (isCurrentUser) {
        if (isGroupedWithPrevious) PaceDreamRadius.SM else PaceDreamRadius.MD
    } else {
        PaceDreamRadius.MD
    }
    val bottomStart = if (isCurrentUser) {
        PaceDreamRadius.MD
    } else {
        if (isGroupedWithNext) PaceDreamRadius.SM else PaceDreamRadius.MD
    }
    val bottomEnd = if (isCurrentUser) {
        if (isGroupedWithNext) PaceDreamRadius.SM else PaceDreamRadius.MD
    } else {
        PaceDreamRadius.MD
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = maxWidth)
        ) {
            Card(
                modifier = if (message.isFailed) {
                    Modifier.clickable(onClick = onRetry)
                } else {
                    Modifier
                },
                shape = RoundedCornerShape(
                    topStart = topStart,
                    topEnd = topEnd,
                    bottomStart = bottomStart,
                    bottomEnd = bottomEnd,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        message.isFailed && isCurrentUser -> PaceDreamColors.Primary.copy(alpha = 0.6f)
                        isCurrentUser -> PaceDreamColors.Primary
                        else -> PaceDreamColors.Surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.MD,
                        vertical = PaceDreamSpacing.SM
                    )
                ) {
                    Text(
                        text = message.text,
                        style = PaceDreamTypography.Body,
                        color = if (isCurrentUser) Color.White else PaceDreamColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Only show timestamp at the end of a grouped block to reduce noise.
                    if (!isGroupedWithNext) {
                        Text(
                            text = message.formattedTime,
                            style = PaceDreamTypography.Caption,
                            color = if (isCurrentUser)
                                Color.White.copy(alpha = 0.7f)
                            else
                                PaceDreamColors.TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Message status indicator - matches iOS "Sending…" / "Failed • Tap to retry"
            if (message.isTemp) {
                Spacer(modifier = Modifier.height(2.dp))
                when (message.status) {
                    MessageStatus.SENDING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = PaceDreamColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sending\u2026",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                    MessageStatus.FAILED -> {
                        Text(
                            text = "Failed \u2022 Tap to retry",
                            style = PaceDreamTypography.Caption,
                            color = Color(0xFFE53935),
                            modifier = Modifier
                                .padding(horizontal = PaceDreamSpacing.SM)
                                .clickable(onClick = onRetry)
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

/**
 * Error banner shown above composer when send fails.
 * Matches iOS error/moderation warning banner pattern.
 */
@Composable
private fun SendErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // Light orange background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = message,
                style = PaceDreamTypography.Caption,
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
private fun PhotoPreviewTray(
    photoUris: List<Uri>,
    onRemove: (Uri) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface)
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(photoUris) { uri ->
                Box(modifier = Modifier.size(72.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    )
                    IconButton(
                        onClick = { onRemove(uri) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(22.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove photo",
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachPhotos: (List<Uri>) -> Unit,
    isSending: Boolean,
    canSend: Boolean,
    attachmentsEnabled: Boolean
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = ThreadViewModel.MAX_PHOTOS
        )
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAttachPhotos(uris)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM
                ),
            verticalAlignment = Alignment.Bottom
        ) {
            // Photo attachment button
            IconButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = attachmentsEnabled && !isSending,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Image,
                    contentDescription = "Attach photo",
                    tint = if (attachmentsEnabled)
                        PaceDreamColors.Primary
                    else
                        PaceDreamColors.TextSecondary.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        "Type a message...",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4,
                enabled = !isSending
            )

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (canSend)
                            PaceDreamColors.Primary
                        else
                            PaceDreamColors.Border,
                        CircleShape
                    )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

