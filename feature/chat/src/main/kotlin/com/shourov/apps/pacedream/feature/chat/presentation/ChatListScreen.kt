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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.components.PaceDreamHeroHeader
import com.pacedream.common.composables.components.PaceDreamUserAvatar
import com.pacedream.common.composables.theme.PaceDreamDesignSystem
import com.shourov.apps.pacedream.model.MessageModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
    onChatClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadChats()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamDesignSystem.PaceDreamColors.Background)
    ) {
        PaceDreamHeroHeader(
            title = "Messages",
            subtitle = "Chat with hosts and guests",
            onNotificationClick = { /* Handle notification */ }
        )
        
        when {
            uiState.isLoading -> {
                ChatListLoadingState()
            }
            uiState.chats.isEmpty() -> {
                ChatListEmptyState()
            }
            else -> {
                ChatListContent(
                    chats = uiState.chats,
                    onChatClick = onChatClick
                )
            }
        }
    }
}

@Composable
private fun ChatListContent(
    chats: List<ChatItem>,
    onChatClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamDesignSystem.PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamDesignSystem.PaceDreamSpacing.SM)
    ) {
        items(chats) { chat ->
            ChatItemCard(
                chat = chat,
                onClick = { onChatClick(chat.chatId) }
            )
        }
    }
}

@Composable
private fun ChatItemCard(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamDesignSystem.PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(PaceDreamDesignSystem.PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamDesignSystem.PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            PaceDreamUserAvatar(
                imageUrl = chat.otherUserAvatar,
                contentDescription = "Avatar of ${chat.otherUserName}",
                modifier = Modifier.size(50.dp)
            )
            
            Spacer(modifier = Modifier.width(PaceDreamDesignSystem.PaceDreamSpacing.MD))
            
            // Chat Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.otherUserName,
                        style = PaceDreamDesignSystem.PaceDreamTypography.Headline,
                        color = PaceDreamDesignSystem.PaceDreamColors.OnCard,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = formatTime(chat.lastMessageTime),
                        style = PaceDreamDesignSystem.PaceDreamTypography.Caption,
                        color = PaceDreamDesignSystem.PaceDreamColors.OnCard.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(PaceDreamDesignSystem.PaceDreamSpacing.XS))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = PaceDreamDesignSystem.PaceDreamTypography.Body,
                        color = PaceDreamDesignSystem.PaceDreamColors.OnCard.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (chat.unreadCount > 0) {
                        Badge(
                            containerColor = PaceDreamDesignSystem.PaceDreamColors.Primary,
                            contentColor = PaceDreamDesignSystem.PaceDreamColors.OnPrimary
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                style = PaceDreamDesignSystem.PaceDreamTypography.Caption
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = PaceDreamDesignSystem.PaceDreamColors.Primary
        )
    }
}

@Composable
private fun ChatListEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "No messages",
                tint = PaceDreamDesignSystem.PaceDreamColors.OnBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamDesignSystem.PaceDreamSpacing.MD))
            Text(
                text = "No messages yet",
                style = PaceDreamDesignSystem.PaceDreamTypography.Headline,
                color = PaceDreamDesignSystem.PaceDreamColors.OnBackground
            )
            Spacer(modifier = Modifier.height(PaceDreamDesignSystem.PaceDreamSpacing.SM))
            Text(
                text = "Start a conversation with hosts or guests",
                style = PaceDreamDesignSystem.PaceDreamTypography.Body,
                color = PaceDreamDesignSystem.PaceDreamColors.OnBackground.copy(alpha = 0.7f)
            )
        }
    }
}

data class ChatItem(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserAvatar: String?,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0
)

private fun formatTime(timeString: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val time = format.parse(timeString)
        val now = Date()
        
        val diffInMillis = now.time - (time?.time ?: 0)
        val diffInMinutes = diffInMillis / (1000 * 60)
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24
        
        when {
            diffInMinutes < 1 -> "Now"
            diffInMinutes < 60 -> "${diffInMinutes}m"
            diffInHours < 24 -> "${diffInHours}h"
            diffInDays < 7 -> "${diffInDays}d"
            else -> {
                val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                outputFormat.format(time ?: Date())
            }
        }
    } catch (e: Exception) {
        timeString
    }
}
