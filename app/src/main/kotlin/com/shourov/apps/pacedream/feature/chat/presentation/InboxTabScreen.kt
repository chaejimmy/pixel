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

/**
 * InboxTabScreen - legacy stub.
 *
 * The real inbox is handled by the InboxScreen composable in feature/inbox
 * which is fully backend-driven via InboxViewModel + InboxRepository
 * (GET /v1/inbox/threads, GET /v1/inbox/unread-counts).
 *
 * This stub shows an empty state to prevent any hardcoded/fake data
 * from being displayed. It is NOT used in the main navigation graph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTabScreen(
    onChatClick: (String) -> Unit = {},
    onNewMessageClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
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
        // Empty state - real inbox is via InboxScreen (feature/inbox module)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PaceDreamEmptyState(
                icon = PaceDreamIcons.Message,
                title = "No messages yet",
                subtitle = "Start a conversation with your guests or hosts",
                actionText = "Start Chatting",
                onActionClick = onNewMessageClick
            )
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
