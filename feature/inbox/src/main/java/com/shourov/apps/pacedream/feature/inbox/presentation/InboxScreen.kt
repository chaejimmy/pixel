package com.shourov.apps.pacedream.feature.inbox.presentation

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.components.PaceDreamEmptyState
import com.pacedream.common.composables.components.PaceDreamErrorState
import com.pacedream.common.composables.components.PaceDreamLoadingState
import com.pacedream.common.composables.shimmerEffect
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.feature.inbox.model.InboxEvent
import com.shourov.apps.pacedream.feature.inbox.model.InboxMode
import com.shourov.apps.pacedream.feature.inbox.model.InboxUiState
import com.shourov.apps.pacedream.feature.inbox.model.Thread
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel(),
    onNavigateToThread: (String) -> Unit,
    onShowAuthSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadMoreRequestedForSize = remember { mutableIntStateOf(-1) }
    
    // Handle navigation
    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
            when (navigation) {
                is InboxNavigation.ToThread -> onNavigateToThread(navigation.threadId)
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        style = PaceDreamTypography.Title2,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Unread badge in title
                    (uiState as? InboxUiState.Success)?.let { state ->
                        val totalUnread = state.unreadCounts.totalUnread
                        if (totalUnread > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(end = PaceDreamSpacing.MD)
                                    .size(24.dp)
                                    .background(PaceDreamColors.Primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (totalUnread > 99) "99+" else totalUnread.toString(),
                                    style = PaceDreamTypography.Caption,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "inbox_state"
            ) { state ->
                when (state) {
                    is InboxUiState.Loading -> InboxSkeletonList()
                    is InboxUiState.Success -> SuccessState(
                        state = state,
                        onEvent = viewModel::onEvent,
                        loadMoreRequestedForSize = loadMoreRequestedForSize
                    )
                    is InboxUiState.Error -> PaceDreamErrorState(
                        title = "Couldn't load messages",
                        description = state.message,
                        onRetryClick = { viewModel.onEvent(InboxEvent.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    )
                    is InboxUiState.Empty -> PaceDreamEmptyState(
                        title = "No messages yet",
                        description = "Start a conversation with hosts or guests",
                        icon = Icons.Default.Message,
                        modifier = Modifier.fillMaxSize()
                    )
                    is InboxUiState.RequiresAuth -> PaceDreamEmptyState(
                        title = "Sign in to view messages",
                        description = "Connect with hosts and guests",
                        icon = Icons.Default.Lock,
                        actionText = "Sign In",
                        onActionClick = onShowAuthSheet,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessState(
    state: InboxUiState.Success,
    onEvent: (InboxEvent) -> Unit,
    loadMoreRequestedForSize: androidx.compose.runtime.MutableIntState
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(InboxEvent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mode toggle chips (Guest/Host)
            ModeToggleRow(
                selectedMode = state.mode,
                guestUnread = state.unreadCounts.guestUnread,
                hostUnread = state.unreadCounts.hostUnread,
                onModeSelected = { onEvent(InboxEvent.ModeChanged(it)) }
            )
            
            // Thread list
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM
                ),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.threads,
                    key = { it.id }
                ) { thread ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onEvent(InboxEvent.ArchiveThread(thread))
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                                    .background(PaceDreamColors.Warning.copy(alpha = 0.18f))
                                    .padding(horizontal = PaceDreamSpacing.MD),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                                ) {
                                    Text(
                                        text = "Archive",
                                        style = PaceDreamTypography.Callout,
                                        color = PaceDreamColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = PaceDreamColors.TextPrimary
                                    )
                                }
                            }
                        }
                    ) {
                        ThreadCard(
                            thread = thread,
                            onClick = { onEvent(InboxEvent.ThreadClicked(thread)) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
                
                // Load more indicator
                if (state.hasMore) {
                    item {
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
                        
                        // Trigger pagination only once per list-size to avoid event spam.
                        LaunchedEffect(state.threads.size) {
                            if (loadMoreRequestedForSize.intValue != state.threads.size) {
                                loadMoreRequestedForSize.intValue = state.threads.size
                                onEvent(InboxEvent.LoadMore)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggleRow(
    selectedMode: InboxMode,
    guestUnread: Int,
    hostUnread: Int,
    onModeSelected: (InboxMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        InboxMode.entries.forEach { mode ->
            val unread = when (mode) {
                InboxMode.GUEST -> guestUnread
                InboxMode.HOST -> hostUnread
            }
            
            FilterChip(
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(mode.displayName)
                        if (unread > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        if (mode == selectedMode) Color.White else PaceDreamColors.Primary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unread > 99) "99" else unread.toString(),
                                    style = PaceDreamTypography.Caption.copy(fontSize = 10.sp),
                                    color = if (mode == selectedMode) PaceDreamColors.Primary else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PaceDreamColors.Primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun ThreadCard(
    thread: Thread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = if (thread.hasUnread) 
                PaceDreamColors.Primary.copy(alpha = 0.05f) 
            else 
                PaceDreamColors.Card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (thread.avatarUrl != null) {
                    AsyncImage(
                        model = thread.avatarUrl,
                        contentDescription = thread.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = thread.displayName.take(1).uppercase(),
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.displayName,
                        style = PaceDreamTypography.Headline,
                        fontWeight = if (thread.hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = thread.formattedTime,
                        style = PaceDreamTypography.Caption,
                        color = if (thread.hasUnread) PaceDreamColors.Primary else PaceDreamColors.TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.lastMessagePreview,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Unread badge
                    if (thread.hasUnread) {
                        Box(
                            modifier = Modifier
                                .padding(start = PaceDreamSpacing.SM)
                                .size(20.dp)
                                .background(PaceDreamColors.Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (thread.unreadCount > 9) "9+" else thread.unreadCount.toString(),
                                style = PaceDreamTypography.Caption.copy(fontSize = 10.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Listing info if available
                thread.listing?.let { listing ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Re: ${listing.title}",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxSkeletonList(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(8) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(PaceDreamColors.Border.copy(alpha = 0.35f))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaceDreamColors.Border.copy(alpha = 0.35f))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaceDreamColors.Border.copy(alpha = 0.25f))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}


