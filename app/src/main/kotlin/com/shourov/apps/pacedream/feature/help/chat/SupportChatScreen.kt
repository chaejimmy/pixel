package com.shourov.apps.pacedream.feature.help.chat

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

/**
 * Customer-support chat screen.
 *
 * Design notes:
 *  - This is a HELP / SUPPORT conversation, not peer-to-peer messaging. The
 *    header, copy, and avatar all communicate "PaceDream Support" so users
 *    do not confuse it with the messaging inbox.
 *  - We follow the Material 3 + iOS-26-aligned design system. No raw hex.
 *  - Loading, empty, error, and closed states are explicit.
 *  - Backend errors are mapped to friendly copy in [SupportChatViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupportChatViewModel = hiltViewModel(),
    title: String = "Support",
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Keep the latest message in view as the conversation grows.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = PaceDreamColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = PaceDreamTypography.Headline,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            HeaderBanner(state = state)

            if (state.requiresGuestForm && state.session == null) {
                GuestContactCard(
                    name = state.guestName,
                    email = state.guestEmail,
                    onNameChange = viewModel::onGuestNameChanged,
                    onEmailChange = viewModel::onGuestEmailChanged,
                )
                HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (val ls = state.loadState) {
                    SupportChatUiState.LoadState.Starting -> StartingState()
                    is SupportChatUiState.LoadState.Error -> ErrorState(
                        message = ls.message,
                        onRetry = viewModel::refresh,
                    )
                    SupportChatUiState.LoadState.Idle,
                    SupportChatUiState.LoadState.Ready -> {
                        if (state.messages.isEmpty()) {
                            EmptyState()
                        } else {
                            MessagesList(
                                listState = listState,
                                messages = state.messages,
                                inlineError = state.inlineError,
                                onRetry = viewModel::retry,
                            )
                        }
                    }
                }
            }

            if (state.canEscalate) {
                EscalateBar(onClick = viewModel::escalate)
            }

            Composer(
                text = state.composerText,
                onTextChange = viewModel::onComposerTextChanged,
                onSend = viewModel::send,
                enabled = state.composerEnabled,
                isSending = state.isSending,
                session = state.session,
            )
        }
    }
}

// ============================================================================
// Header
// ============================================================================

@Composable
private fun HeaderBanner(state: SupportChatUiState) {
    val status = state.session?.status
    val subtitle = when (status) {
        SupportSessionStatus.PendingAdmin -> "A human teammate will reply shortly."
        SupportSessionStatus.Resolved -> "This conversation is marked resolved."
        SupportSessionStatus.Closed -> "This conversation is closed."
        else -> "We typically reply within a few minutes."
    }
    val pillTint = when (status) {
        SupportSessionStatus.PendingAdmin -> PaceDreamColors.Warning
        SupportSessionStatus.Resolved -> PaceDreamColors.Success
        SupportSessionStatus.Closed -> PaceDreamColors.TextSecondary
        else -> PaceDreamColors.Primary
    }
    val pillLabel = status?.pillLabel ?: SupportSessionStatus.Open.pillLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaceDreamColors.Surface)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(PaceDreamColors.Primary.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PaceDreamIcons.Help,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(PaceDreamSpacing.SM2))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PaceDream Support",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.TextSecondary,
            )
        }

        Surface(
            shape = RoundedCornerShape(PaceDreamRadius.Round),
            color = pillTint.copy(alpha = 0.12f),
        ) {
            Text(
                text = pillLabel,
                style = PaceDreamTypography.Caption,
                color = pillTint,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
    HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
}

// ============================================================================
// Guest contact form
// ============================================================================

@Composable
private fun GuestContactCard(
    name: String,
    email: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaceDreamColors.Surface)
            .padding(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
    ) {
        Text(
            text = "Tell us how to reach you",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "So we can follow up by email if we get disconnected.",
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Your name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            colors = pdTextFieldColors(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            colors = pdTextFieldColors(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ============================================================================
// Loading / empty / error
// ============================================================================

@Composable
private fun StartingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = PaceDreamColors.Primary, strokeWidth = 2.dp)
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Starting your conversation…",
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextSecondary,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = PaceDreamIcons.QuestionAnswer,
            contentDescription = null,
            tint = PaceDreamColors.Primary.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "How can we help?",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(PaceDreamSpacing.XXS))
        Text(
            text = "Send a message below to start. Our assistant answers right away and a teammate joins when needed.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.Warning,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
        )
        Spacer(Modifier.height(PaceDreamSpacing.SM))
        TextButton(onClick = onRetry) {
            Text(
                text = "Try again",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ============================================================================
// Messages list
// ============================================================================

@Composable
private fun MessagesList(
    listState: androidx.compose.foundation.lazy.LazyListState,
    messages: List<SupportMessage>,
    inlineError: String?,
    onRetry: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
    ) {
        items(messages, key = { it.id }) { msg ->
            SupportMessageBubble(message = msg, onRetry = onRetry)
        }
        if (inlineError != null) {
            item {
                InlineErrorBanner(message = inlineError)
            }
        }
    }
}

@Composable
private fun SupportMessageBubble(
    message: SupportMessage,
    onRetry: (String) -> Unit,
) {
    when (message.sender) {
        SupportSender.User -> UserBubble(message = message, onRetry = onRetry)
        SupportSender.System -> SystemBubble(message = message)
        else -> AgentBubble(message = message)
    }
}

@Composable
private fun UserBubble(
    message: SupportMessage,
    onRetry: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            color = if (message.isFailed) PaceDreamColors.Primary.copy(alpha = 0.7f) else PaceDreamColors.Primary,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                text = message.content,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.OnPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        if (message.isPending) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Sending…",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
        } else if (message.isFailed) {
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.clickable { onRetry(message.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Warning,
                    contentDescription = null,
                    tint = PaceDreamColors.Error,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Couldn't send — tap to retry",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.Error,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun AgentBubble(message: SupportMessage) {
    val accent = when (message.sender) {
        SupportSender.Admin -> PaceDreamColors.Success
        else -> PaceDreamColors.Primary
    }
    val bg = when (message.sender) {
        SupportSender.Admin -> PaceDreamColors.Success.copy(alpha = 0.10f)
        else -> PaceDreamColors.Surface
    }
    val label = when (message.sender) {
        SupportSender.Admin -> "PaceDream Team"
        SupportSender.Ai -> "PaceDream Assistant"
        else -> "Support"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(accent.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (message.sender == SupportSender.Admin)
                    PaceDreamIcons.Person else PaceDreamIcons.Help,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.widthIn(max = 320.dp)) {
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = bg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = message.content,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun SystemBubble(message: SupportMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(PaceDreamRadius.Round),
            color = PaceDreamColors.Surface,
        ) {
            Text(
                text = message.content,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun InlineErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                PaceDreamColors.Warning.copy(alpha = 0.12f),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            )
            .padding(PaceDreamSpacing.SM2),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.Warning,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(PaceDreamSpacing.SM))
        Text(
            text = message,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextPrimary,
        )
    }
}

// ============================================================================
// Escalate bar + composer
// ============================================================================

@Composable
private fun EscalateBar(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = 6.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = ButtonDefaults.buttonColors(
            containerColor = PaceDreamColors.Primary,
            contentColor = PaceDreamColors.OnPrimary,
        ),
    ) {
        Icon(
            imageVector = PaceDreamIcons.Person,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(PaceDreamSpacing.SM))
        Text(
            text = "Talk to a human",
            style = PaceDreamTypography.Headline,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Composer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    isSending: Boolean,
    session: SupportSession?,
) {
    val placeholder = when {
        !enabled -> "This conversation is closed."
        session == null -> "Describe what's happening…"
        else -> "Type a message"
    }
    val sendEnabled = enabled && !isSending && text.trim().isNotEmpty()

    Column {
        HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaceDreamColors.Background)
                .padding(horizontal = PaceDreamSpacing.SM2, vertical = PaceDreamSpacing.SM),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text(placeholder) },
                enabled = enabled,
                shape = RoundedCornerShape(PaceDreamRadius.XL),
                colors = pdTextFieldColors(),
                maxLines = 5,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
            )
            Spacer(Modifier.width(PaceDreamSpacing.SM))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (sendEnabled) PaceDreamColors.Primary
                        else PaceDreamColors.Primary.copy(alpha = 0.35f),
                        shape = CircleShape,
                    )
                    .clickable(enabled = sendEnabled, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = PaceDreamColors.OnPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = PaceDreamIcons.Send,
                        contentDescription = "Send message",
                        tint = PaceDreamColors.OnPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ============================================================================
// Shared text field theming
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun pdTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PaceDreamColors.Primary,
    unfocusedBorderColor = PaceDreamColors.Border,
    focusedTextColor = PaceDreamColors.TextPrimary,
    unfocusedTextColor = PaceDreamColors.TextPrimary,
    focusedPlaceholderColor = PaceDreamColors.TextSecondary,
    unfocusedPlaceholderColor = PaceDreamColors.TextSecondary,
    cursorColor = PaceDreamColors.Primary,
)
