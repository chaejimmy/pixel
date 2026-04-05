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

package com.shourov.apps.pacedream.feature.chat.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.components.PaceDreamUserAvatar
import com.pacedream.common.composables.theme.PaceDreamDesignSystem
import com.shourov.apps.pacedream.model.MessageAttachment
import com.shourov.apps.pacedream.model.MessageModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    chatId: String,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaceDreamDesignSystem.PaceDreamColors.Background)
                .imePadding()
        ) {
            // Chat Header
            ChatHeader(
                otherUserName = uiState.otherUserName,
                otherUserAvatar = uiState.otherUserAvatar,
                onBackClick = onBackClick
            )

            // Messages List
            MessagesList(
                messages = uiState.messages,
                currentUserId = uiState.currentUserId,
                onImageClick = { url -> fullScreenImageUrl = url },
                onRetryClick = viewModel::retryFailedMessage,
                modifier = Modifier.weight(1f)
            )

            // Send error banner
            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = PaceDreamDesignSystem.PaceDreamSpacing.MD, vertical = PaceDreamDesignSystem.PaceDreamSpacing.SM),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearError, modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.MD)) {
                                Icon(
                                    imageVector = PaceDreamIcons.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.SM),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Upload error banner
            AnimatedVisibility(visible = uiState.uploadError != null) {
                uiState.uploadError?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = PaceDreamDesignSystem.PaceDreamSpacing.MD, vertical = PaceDreamDesignSystem.PaceDreamSpacing.SM),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearError, modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.MD)) {
                                Icon(
                                    imageVector = PaceDreamIcons.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.SM),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Upload progress bar
            AnimatedVisibility(visible = uiState.isUploading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = PaceDreamDesignSystem.PaceDreamColors.Primary
                )
            }

            // Photo preview tray
            AnimatedVisibility(visible = uiState.pendingPhotos.isNotEmpty()) {
                PhotoPreviewTray(
                    photos = uiState.pendingPhotos,
                    onRemove = viewModel::removePendingPhoto,
                    attachmentsEnabled = uiState.attachmentsEnabled
                )
            }

            // Message Input
            MessageInput(
                message = uiState.newMessage,
                onMessageChange = viewModel::onMessageChange,
                onSendMessage = viewModel::sendMessage,
                onAttachPhotos = viewModel::addPendingPhotos,
                isSending = uiState.isSending || uiState.isUploading,
                canSend = uiState.canSend,
                attachmentsEnabled = uiState.attachmentsEnabled
            )
        }

        // Full-screen image viewer
        fullScreenImageUrl?.let { url ->
            FullScreenImageViewer(
                imageUrl = url,
                onDismiss = { fullScreenImageUrl = null }
            )
        }
    }
}

@Composable
private fun ChatHeader(
    otherUserName: String,
    otherUserAvatar: String?,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PaceDreamDesignSystem.PaceDreamColors.Surface,
        shadowElevation = PaceDreamDesignSystem.PaceDreamElevation.LG
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamDesignSystem.PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = PaceDreamIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = PaceDreamDesignSystem.PaceDreamColors.OnSurface
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.SM))

            PaceDreamUserAvatar(
                imageUrl = otherUserAvatar,
                contentDescription = "Avatar of $otherUserName",
                modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.XL)
            )

            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.MD))

            Column {
                Text(
                    text = otherUserName,
                    style = PaceDreamDesignSystem.PaceDreamTypography.Headline,
                    color = PaceDreamDesignSystem.PaceDreamColors.OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Active now",
                    style = PaceDreamDesignSystem.PaceDreamTypography.Caption,
                    color = PaceDreamDesignSystem.PaceDreamColors.OnSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { /* Handle call */ }) {
                Icon(
                    imageVector = PaceDreamIcons.Call,
                    contentDescription = "Call",
                    tint = PaceDreamDesignSystem.PaceDreamColors.OnSurface
                )
            }

            IconButton(onClick = { /* Handle video call */ }) {
                Icon(
                    imageVector = PaceDreamIcons.Videocam,
                    contentDescription = "Video call",
                    tint = PaceDreamDesignSystem.PaceDreamColors.OnSurface
                )
            }
        }
    }
}

@Composable
private fun MessagesList(
    messages: List<MessageModel>,
    currentUserId: String,
    onImageClick: (String) -> Unit,
    onRetryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Track keyboard visibility via IME insets
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottomPx > 0

    // Determine if user is near the bottom of the list (within last 3 items)
    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) true
            else lastVisibleItem != null && lastVisibleItem.index >= totalItems - 3
        }
    }

    // Auto-scroll when new message arrives (only if user is near bottom)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isNearBottom) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-scroll when keyboard opens (only if user is near bottom)
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && messages.isNotEmpty() && isNearBottom) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Debug: log keyboard height changes
    LaunchedEffect(imeBottomPx) {
        val imeHeightDp = with(density) { imeBottomPx.toDp() }
        Timber.d("Chat: keyboard height=%s (visible=%s)", imeHeightDp, isKeyboardVisible)
    }

    // Debug: log scroll position and message count
    LaunchedEffect(listState.firstVisibleItemIndex, messages.size) {
        Timber.d(
            "Chat: scrollPosition=%d, totalMessages=%d, nearBottom=%s",
            listState.firstVisibleItemIndex,
            messages.size,
            isNearBottom
        )
    }

    // Deduplicate messages by ID to prevent LazyColumn key crash.
    // Duplicates can come from Room cache, $or queries, or optimistic inserts.
    val dedupedMessages = remember(messages) {
        messages.distinctBy { it.id.ifBlank { it.hashCode().toString() } }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(PaceDreamDesignSystem.PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamDesignSystem.PaceDreamSpacing.SM, Alignment.Bottom)
    ) {
        items(dedupedMessages, key = { it.id.ifBlank { "msg_${it.hashCode()}" } }) { message ->
            MessageBubble(
                message = message,
                isFromCurrentUser = message.senderId == currentUserId,
                onImageClick = onImageClick,
                onRetryClick = onRetryClick
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageModel,
    isFromCurrentUser: Boolean,
    onImageClick: (String) -> Unit,
    onRetryClick: (String) -> Unit
) {
    val isFailed = message.status == "failed"
    val isSending = message.status == "sending"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            PaceDreamUserAvatar(
                imageUrl = null,
                contentDescription = "User avatar",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.SM))
        }

        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Image attachments
            if (message.hasImageAttachments) {
                PhotoGrid(
                    attachments = message.imageAttachments,
                    onImageClick = onImageClick,
                    isFromCurrentUser = isFromCurrentUser
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Text content (skip placeholder text for media messages)
            val displayText = message.displayText
            if (displayText.isNotBlank() && !(message.hasImageAttachments && displayText.startsWith("Sending "))) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFromCurrentUser) {
                            PaceDreamDesignSystem.PaceDreamColors.Primary
                        } else {
                            PaceDreamDesignSystem.PaceDreamColors.SurfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(
                        topStart = PaceDreamDesignSystem.PaceDreamRadius.MD,
                        topEnd = PaceDreamDesignSystem.PaceDreamRadius.MD,
                        bottomStart = if (isFromCurrentUser) PaceDreamDesignSystem.PaceDreamRadius.MD else PaceDreamDesignSystem.PaceDreamRadius.SM,
                        bottomEnd = if (isFromCurrentUser) PaceDreamDesignSystem.PaceDreamRadius.SM else PaceDreamDesignSystem.PaceDreamRadius.MD
                    )
                ) {
                    Text(
                        text = displayText,
                        style = PaceDreamDesignSystem.PaceDreamTypography.Body,
                        color = if (isFromCurrentUser) {
                            PaceDreamDesignSystem.PaceDreamColors.OnPrimary
                        } else {
                            PaceDreamDesignSystem.PaceDreamColors.OnSurfaceVariant
                        },
                        modifier = Modifier.padding(PaceDreamDesignSystem.PaceDreamSpacing.SM)
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamDesignSystem.PaceDreamSpacing.XS))

            // Status row: timestamp + sending/failed indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMessageTime(message.timestamp ?: message.createdAt ?: ""),
                    style = PaceDreamDesignSystem.PaceDreamTypography.Caption,
                    color = PaceDreamDesignSystem.PaceDreamColors.OnBackground.copy(alpha = 0.6f)
                )

                if (isSending) {
                    Spacer(modifier = Modifier.width(4.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = PaceDreamDesignSystem.PaceDreamColors.OnBackground.copy(alpha = 0.5f)
                    )
                }

                if (isFailed) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Failed",
                        style = PaceDreamDesignSystem.PaceDreamTypography.Caption,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Retry",
                        style = PaceDreamDesignSystem.PaceDreamTypography.Caption,
                        color = PaceDreamDesignSystem.PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { message.id?.let { onRetryClick(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    attachments: List<MessageAttachment>,
    onImageClick: (String) -> Unit,
    isFromCurrentUser: Boolean
) {
    val shape = RoundedCornerShape(
        topStart = PaceDreamDesignSystem.PaceDreamRadius.MD,
        topEnd = PaceDreamDesignSystem.PaceDreamRadius.MD,
        bottomStart = if (isFromCurrentUser) PaceDreamDesignSystem.PaceDreamRadius.MD else PaceDreamDesignSystem.PaceDreamRadius.SM,
        bottomEnd = if (isFromCurrentUser) PaceDreamDesignSystem.PaceDreamRadius.SM else PaceDreamDesignSystem.PaceDreamRadius.MD
    )

    when (attachments.size) {
        1 -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(attachments[0].displayUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = attachments[0].name ?: "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .heightIn(max = 240.dp)
                    .clip(shape)
                    .clickable { onImageClick(attachments[0].url) }
            )
        }
        2 -> {
            Row(
                modifier = Modifier
                    .clip(shape)
                    .widthIn(max = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                attachments.forEach { att ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(att.displayUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = att.name ?: "Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clickable { onImageClick(att.url) }
                    )
                }
            }
        }
        else -> {
            // Grid layout for 3+ images
            val rows = attachments.chunked(2)
            Column(
                modifier = Modifier
                    .clip(shape)
                    .widthIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rows.forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEachIndexed { colIndex, att ->
                            val index = rowIndex * 2 + colIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (attachments.size <= 4) 120.dp else 100.dp)
                                    .clickable { onImageClick(att.url) }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(att.displayUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = att.name ?: "Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // "+N" overlay on last visible image if there are more
                                if (index == 3 && attachments.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${attachments.size - 4}",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                        // Pad odd rows
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    // Only show first 2 rows (4 images) for large sets
                    if (rowIndex >= 1 && attachments.size > 4) return@Column
                }
            }
        }
    }
}

@Composable
private fun PhotoPreviewTray(
    photos: List<PendingPhoto>,
    onRemove: (Uri) -> Unit,
    attachmentsEnabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PaceDreamDesignSystem.PaceDreamColors.Surface
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos, key = { it.id }) { photo ->
                Box(modifier = Modifier.size(72.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    // Remove button
                    IconButton(
                        onClick = { onRemove(photo.uri) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(22.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Close,
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
private fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachPhotos: (List<Uri>) -> Unit,
    isSending: Boolean,
    canSend: Boolean,
    attachmentsEnabled: Boolean
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = ChatViewModel.MAX_PHOTOS_PER_MESSAGE
        )
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAttachPhotos(uris)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PaceDreamDesignSystem.PaceDreamColors.Surface,
        shadowElevation = PaceDreamDesignSystem.PaceDreamElevation.LG
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamDesignSystem.PaceDreamSpacing.MD),
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
                    tint = if (attachmentsEnabled) {
                        PaceDreamDesignSystem.PaceDreamColors.OnSurface.copy(alpha = 0.7f)
                    } else {
                        PaceDreamDesignSystem.PaceDreamColors.OnSurface.copy(alpha = 0.3f)
                    }
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = {
                    Text(
                        "Type a message...",
                        style = PaceDreamDesignSystem.PaceDreamTypography.Body,
                        color = PaceDreamDesignSystem.PaceDreamColors.OnSurface.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(PaceDreamDesignSystem.PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamDesignSystem.PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamDesignSystem.PaceDreamColors.OnSurface.copy(alpha = 0.12f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.SM))

            FloatingActionButton(
                onClick = { if (canSend) onSendMessage() },
                modifier = Modifier.size(48.dp),
                containerColor = if (canSend) {
                    PaceDreamDesignSystem.PaceDreamColors.Primary
                } else {
                    PaceDreamDesignSystem.PaceDreamColors.Primary.copy(alpha = 0.4f)
                },
                contentColor = PaceDreamDesignSystem.PaceDreamColors.OnPrimary
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PaceDreamDesignSystem.PaceDreamColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = PaceDreamIcons.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full screen photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) {} // Prevent dismiss on image click
            )
        }
    }
}

private fun formatMessageTime(timeString: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val time = format.parse(timeString)
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outputFormat.format(time ?: Date())
    } catch (e: Exception) {
        timeString
    }
}
