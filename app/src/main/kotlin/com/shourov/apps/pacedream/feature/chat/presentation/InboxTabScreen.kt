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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTabScreen(
    onChatClick: (String) -> Unit = {},
    onNewMessageClick: () -> Unit = {}
) {
    // Sample data - replace with actual data from ViewModel
    val chats = remember {
        listOf(
            ChatData(
                id = "1",
                name = "Sarah Johnson",
                lastMessage = "Thank you for the great stay!",
                timestamp = "2 min ago",
                unreadCount = 2,
                isOnline = true,
                avatarUrl = null
            ),
            ChatData(
                id = "2",
                name = "Mike Chen",
                lastMessage = "Is the property available this weekend?",
                timestamp = "1 hour ago",
                unreadCount = 0,
                isOnline = false,
                avatarUrl = null
            ),
            ChatData(
                id = "3",
                name = "Emily Davis",
                lastMessage = "I'll be arriving at 3 PM",
                timestamp = "3 hours ago",
                unreadCount = 1,
                isOnline = true,
                avatarUrl = null
            ),
            ChatData(
                id = "4",
                name = "David Wilson",
                lastMessage = "The check-in process was smooth",
                timestamp = "1 day ago",
                unreadCount = 0,
                isOnline = false,
                avatarUrl = null
            ),
            ChatData(
                id = "5",
                name = "Lisa Brown",
                lastMessage = "Can I extend my stay?",
                timestamp = "2 days ago",
                unreadCount = 0,
                isOnline = false,
                avatarUrl = null
            )
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Messages",
                    style = PaceDreamTypography.LargeTitle,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${chats.count { it.unreadCount > 0 }} unread messages",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            IconButton(
                onClick = onNewMessageClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Message",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Search Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.LG)
                .height(PaceDreamSearchBar.Height),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PaceDreamSpacing.MD),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                
                Text(
                    text = "Search messages...",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        // Chats List
        if (chats.isEmpty()) {
            PaceDreamEmptyState(
                icon = Icons.Default.Message,
                title = "No messages yet",
                subtitle = "Start a conversation with your guests or hosts",
                actionText = "Start Chatting",
                onActionClick = onNewMessageClick
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
            ) {
                items(chats) { chat ->
                    ChatItem(
                        chat = chat,
                        onClick = { onChatClick(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatItem(
    chat: ChatData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.name.first().toString(),
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Online indicator
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(PaceDreamColors.Success)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = chat.timestamp,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(PaceDreamColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                style = PaceDreamTypography.Caption.copy(fontSize = 10.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaceDreamEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        Text(
            text = title,
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        Text(
            text = subtitle,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
        )
        
        actionText?.let { text ->
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Button(
                onClick = { onActionClick?.invoke() },
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
            ) {
                Text(
                    text = text,
                    style = PaceDreamTypography.Body,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

data class ChatData(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val avatarUrl: String?
)
