package com.shourov.apps.pacedream.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    val unreadCount = chats.count { it.unreadCount > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Messages",
                            style = PaceDreamTypography.Title1,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount unread",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.Primary
                            )
                        }
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = onNewMessageClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PaceDreamColors.Primary
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Add,
                            contentDescription = "New Message",
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXL)
        ) {
            // Search Bar
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    color = PaceDreamColors.Card,
                    tonalElevation = PaceDreamElevation.XS
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Search,
                            contentDescription = "Search",
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(
                            text = "Search messages...",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextTertiary
                        )
                    }
                }
            }

            // Chat list or empty state
            if (chats.isEmpty()) {
                item {
                    PaceDreamEmptyState(
                        icon = PaceDreamIcons.Message,
                        title = "No messages yet",
                        subtitle = "Start a conversation with your guests or hosts",
                        actionText = "Start Chatting",
                        onActionClick = onNewMessageClick
                    )
                }
            } else {
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
    val hasUnread = chat.unreadCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (hasUnread)
                PaceDreamColors.Primary.copy(alpha = 0.04f)
            else
                PaceDreamColors.Card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.name.split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString(""),
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(PaceDreamColors.Background)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(PaceDreamColors.Success)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        text = chat.timestamp,
                        style = PaceDreamTypography.Caption2,
                        color = if (hasUnread) PaceDreamColors.Primary else PaceDreamColors.TextTertiary
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
                        style = PaceDreamTypography.Caption,
                        color = if (hasUnread) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary,
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(PaceDreamColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                style = PaceDreamTypography.Caption2.copy(fontSize = 10.sp),
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
            .fillMaxWidth()
            .padding(PaceDreamSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.XL)
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Text(
            text = title,
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = subtitle,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.75f)
        )

        actionText?.let { text ->
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Button(
                onClick = { onActionClick?.invoke() },
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.XL, vertical = PaceDreamSpacing.SM)
            ) {
                Text(
                    text = text,
                    style = PaceDreamTypography.Callout,
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
