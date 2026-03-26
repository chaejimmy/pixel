@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.pacedream.common.composables.components.PaceDreamErrorState
import com.pacedream.common.composables.components.PaceDreamLoadingState
import com.pacedream.common.composables.shimmerEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.feature.inbox.model.InboxEvent
import com.shourov.apps.pacedream.feature.inbox.model.InboxUiState
import com.shourov.apps.pacedream.feature.inbox.model.Thread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel(),
    onNavigateToThread: (String) -> Unit,
    onShowAuthSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadMoreRequestedForSize = remember { mutableIntStateOf(-1) }

    // Refresh threads when returning from a thread (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle navigation - mark thread as read locally when navigating (matches iOS)
    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
            when (navigation) {
                is InboxNavigation.ToThread -> {
                    viewModel.markThreadReadLocally(navigation.threadId)
                    onNavigateToThread(navigation.threadId)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Messages",
                            style = PaceDreamTypography.Title1,
                            fontWeight = FontWeight.Bold
                        )
                        // Unread count badge next to title
                        (uiState as? InboxUiState.Success)?.let { state ->
                            val totalUnread = state.unreadCounts.totalUnread
                            if (totalUnread > 0) {
                                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(PaceDreamColors.Primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (totalUnread > 99) "99+" else totalUnread.toString(),
                                        style = PaceDreamTypography.Caption.copy(fontSize = 10.sp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                    is InboxUiState.Empty -> InboxEmptyState(
                        modifier = Modifier.fillMaxSize()
                    )
                    is InboxUiState.RequiresAuth -> AuthRequiredState(
                        onShowAuthSheet = onShowAuthSheet,
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
                                .fillMaxSize()
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
                                    imageVector = PaceDreamIcons.Archive,
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

/**
 * Improved empty state with richer visual presentation and helpful guidance.
 */
@Composable
private fun InboxEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = PaceDreamSpacing.XL),
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
                imageVector = PaceDreamIcons.Message,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = PaceDreamColors.Primary.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Text(
            text = "No messages yet",
            style = PaceDreamTypography.Title3,
            fontWeight = FontWeight.Bold,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "When you contact a host or receive a booking inquiry, your conversations will appear here.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
        )
    }
}

/**
 * Auth-required state with sign-in prompt.
 */
@Composable
private fun AuthRequiredState(
    onShowAuthSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = PaceDreamSpacing.XL),
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
                imageVector = PaceDreamIcons.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = PaceDreamColors.Primary.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Text(
            text = "Sign in to view messages",
            style = PaceDreamTypography.Title3,
            fontWeight = FontWeight.Bold,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Message hosts, ask questions, and manage your bookings — all in one place.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Button(
            onClick = onShowAuthSheet,
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            Text(
                text = "Sign In",
                style = PaceDreamTypography.Button,
                fontWeight = FontWeight.SemiBold
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
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thread.avatarUrl)
                            .crossfade(200)
                            .size(coil.size.Size(96, 96))
                            .build(),
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
        items(5) {
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
