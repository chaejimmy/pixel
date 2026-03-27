package com.pacedream.app.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * InboxScreen - Thread list with unread counts
 *
 * iOS Parity:
 * - GET /v1/inbox/threads with pagination
 * - GET /v1/inbox/unread-counts for badges
 * - Tolerant decoding for threads
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel(),
    onThreadClick: (String) -> Unit,
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            "Messages",
                            style = PaceDreamTypography.Title1,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
                )
            }
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // iOS parity: Guest/Host mode toggle
            ModeToggle(
                selectedMode = uiState.selectedMode,
                guestUnreadCount = uiState.guestUnreadCount,
                hostUnreadCount = uiState.hostUnreadCount,
                onModeSelected = { viewModel.switchMode(it) }
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PaceDreamColors.Primary)
                        }
                    }

                    uiState.error != null -> {
                        ErrorState(
                            message = uiState.error ?: "An unexpected error occurred",
                            onRetryClick = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    uiState.threads.isEmpty() -> {
                        EmptyState(
                            isHostMode = uiState.selectedMode == "host",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp)
                        ) {
                            items(
                                items = uiState.threads,
                                key = { it.id }
                            ) { thread ->
                                ThreadItem(
                                    thread = thread,
                                    onClick = { onThreadClick(thread.id) }
                                )
                            }

                            // Load more when reaching end
                            if (uiState.hasMore && !uiState.isLoadingMore) {
                                item {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMore()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = PaceDreamColors.Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // end Column
    }
}

@Composable
private fun ThreadItem(
    thread: InboxThread,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar with online/unread indicator
            Box {
                AsyncImage(
                    model = thread.participantAvatar?.takeIf { it.isNotBlank() },
                    contentDescription = thread.participantName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PaceDreamColors.Gray100, CircleShape)
                )

                // Unread indicator dot
                if (thread.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(PaceDreamColors.Primary, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                // Name and time row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.participantName,
                        style = PaceDreamTypography.Body,
                        fontWeight = if (thread.isUnread) FontWeight.SemiBold else FontWeight.Medium,
                        color = PaceDreamColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = thread.formattedTime,
                        style = PaceDreamTypography.Caption,
                        color = if (thread.isUnread)
                            PaceDreamColors.Primary
                        else
                            PaceDreamColors.TextSecondary,
                        fontWeight = if (thread.isUnread) FontWeight.Medium else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Listing name if available
                thread.listingName?.let { listingName ->
                    Text(
                        text = listingName,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Last message preview
                Text(
                    text = thread.lastMessage,
                    style = PaceDreamTypography.Footnote,
                    color = if (thread.isUnread)
                        PaceDreamColors.TextPrimary
                    else
                        PaceDreamColors.TextTertiary,
                    fontWeight = if (thread.isUnread) FontWeight.Normal else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Subtle divider aligned with text content
        HorizontalDivider(
            modifier = Modifier.padding(start = 86.dp, end = PaceDreamSpacing.MD),
            thickness = 0.5.dp,
            color = PaceDreamColors.Gray100
        )
    }
}

@Composable
private fun EmptyState(
    isHostMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    PaceDreamColors.Primary.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.Mail,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = PaceDreamColors.Primary.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Text(
            text = "No messages yet",
            style = PaceDreamTypography.Title3,
            fontWeight = FontWeight.SemiBold,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = if (isHostMode)
                "Guest conversations will appear here when they reach out."
            else
                "Your conversations will appear here when you message a host or receive a booking inquiry.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    PaceDreamColors.Error.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = PaceDreamColors.Error.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Text(
            text = "Something went wrong",
            style = PaceDreamTypography.Title3,
            fontWeight = FontWeight.SemiBold,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Try Again", style = PaceDreamTypography.Button)
        }
    }
}

/**
 * iOS parity: Guest/Host mode toggle with unread badges
 * Segmented control style matching iOS design
 */
@Composable
private fun ModeToggle(
    selectedMode: String,
    guestUnreadCount: Int,
    hostUnreadCount: Int,
    onModeSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(bottom = PaceDreamSpacing.SM),
        shape = RoundedCornerShape(12.dp),
        color = PaceDreamColors.Gray100,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeButton(
                label = "Guest",
                badgeCount = guestUnreadCount,
                isActive = selectedMode == "guest",
                onClick = { onModeSelected("guest") },
                modifier = Modifier.weight(1f)
            )
            ModeButton(
                label = "Host",
                badgeCount = hostUnreadCount,
                isActive = selectedMode == "host",
                onClick = { onModeSelected("host") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    badgeCount: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) Color.White else Color.Transparent,
        shadowElevation = if (isActive) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = PaceDreamTypography.Subheadline,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isActive) PaceDreamColors.TextPrimary
                       else PaceDreamColors.TextSecondary
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = PaceDreamColors.Error,
                            shape = CircleShape
                        )
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = PaceDreamTypography.Caption,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
