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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.components.PaceDreamUserAvatar
import com.pacedream.common.composables.theme.PaceDreamDesignSystem
import com.shourov.apps.pacedream.model.MessageModel
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
    
    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamDesignSystem.PaceDreamColors.Background)
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
            modifier = Modifier.weight(1f)
        )
        
        // Message Input
        MessageInput(
            message = uiState.newMessage,
            onMessageChange = viewModel::onMessageChange,
            onSendMessage = viewModel::sendMessage,
            isSending = uiState.isSending
        )
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
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamDesignSystem.PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PaceDreamDesignSystem.PaceDreamColors.OnSurface
                )
            }
            
            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.SM))
            
            PaceDreamUserAvatar(
                imageUrl = otherUserAvatar,
                contentDescription = "Avatar of $otherUserName",
                modifier = Modifier.size(40.dp)
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
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = PaceDreamDesignSystem.PaceDreamColors.OnSurface
                )
            }
            
            IconButton(onClick = { /* Handle video call */ }) {
                Icon(
                    imageVector = Icons.Default.Videocam,
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
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(PaceDreamDesignSystem.PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamDesignSystem.PaceDreamSpacing.SM)
    ) {
        items(messages) { message ->
            MessageBubble(
                message = message,
                isFromCurrentUser = message.senderId == currentUserId
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageModel,
    isFromCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            PaceDreamUserAvatar(
                imageUrl = null, // This would come from user data
                contentDescription = "User avatar",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.SM))
        }
        
        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
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
                    text = message.content,
                    style = PaceDreamDesignSystem.PaceDreamTypography.Body,
                    color = if (isFromCurrentUser) {
                        PaceDreamDesignSystem.PaceDreamColors.OnPrimary
                    } else {
                        PaceDreamDesignSystem.PaceDreamColors.OnSurfaceVariant
                    },
                    modifier = Modifier.padding(PaceDreamDesignSystem.PaceDreamSpacing.SM)
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamDesignSystem.PaceDreamSpacing.XS))
            
            Text(
                text = formatMessageTime(message.timestamp),
                style = PaceDreamDesignSystem.PaceDreamTypography.Caption,
                color = PaceDreamDesignSystem.PaceDreamColors.OnBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isSending: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PaceDreamDesignSystem.PaceDreamColors.Surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamDesignSystem.PaceDreamSpacing.MD),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(PaceDreamDesignSystem.PaceDreamRadius.MD)
            )
            
            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.SM))
            
            FloatingActionButton(
                onClick = { if (message.isNotBlank()) onSendMessage() },
                modifier = Modifier.size(48.dp),
                containerColor = PaceDreamDesignSystem.PaceDreamColors.Primary,
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
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message"
                    )
                }
            }
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
