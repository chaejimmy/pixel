package com.shourov.apps.pacedream.feature.host.presentation

import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.*
import com.shourov.apps.pacedream.feature.host.presentation.components.*
import timber.log.Timber
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEarningsScreen(
    viewModel: HostEarningsViewModel = hiltViewModel(),
    onSignInClick: () -> Unit = {}
) {
    val uiState by viewModel.earningsUiState.collectAsStateWithLifecycle()
    val screenState = uiState.screenState
    val isReady = screenState is EarningsScreenState.Ready
    val presentingUrl by viewModel.presentingUrl.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val inlineError by viewModel.inlineError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Track whether we launched Custom Tabs so we know to refresh on resume
    var didLaunchStripe by remember { mutableStateOf(false) }

    // iOS parity: open Stripe URL in Chrome Custom Tabs (in-app browser)
    LaunchedEffect(presentingUrl) {
        val url = presentingUrl ?: return@LaunchedEffect
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(0xFF5527D7.toInt())
                        .setNavigationBarColor(0xFF5527D7.toInt())
                        .build()
                )
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
            didLaunchStripe = true
            Timber.d("[Earnings] Launched Stripe onboarding in Custom Tab: $url")
        } catch (e: Exception) {
            Timber.e(e, "[Earnings] Failed to launch Custom Tab, trying fallback")
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                didLaunchStripe = true
            } catch (e2: Exception) {
                Timber.e(e2, "[Earnings] Failed to open Stripe URL in any browser")
            }
        }
        // Clear URL immediately so it won't re-launch on recomposition.
        // Data refresh happens in LifecycleResumeEffect when user returns.
        viewModel.clearPresentingUrl()
    }

    // iOS parity: auto-refresh data when returning from Stripe Custom Tab
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && didLaunchStripe) {
                didLaunchStripe = false
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Earnings",
                        style = PaceDreamTypography.Title1,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (isReady) {
                        val ready = screenState as EarningsScreenState.Ready
                        val availableBalance = ready.dashboard.balances?.available ?: 0.0
                        Button(
                            onClick = { viewModel.showPayoutSheet() },
                            enabled = availableBalance > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                            shape = RoundedCornerShape(PaceDreamRadius.Round),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.height(PaceDreamButtonHeight.SM)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.AttachMoney,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                            Text(
                                text = "Payout",
                                style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (screenState) {
                is EarningsScreenState.Loading ->
                    LoadingContent()

                is EarningsScreenState.SessionExpired ->
                    SessionExpiredContent(onSignInClick = onSignInClick)

                is EarningsScreenState.StripeNotConnected ->
                    StripeNotConnectedContent(
                        isBusy = isBusy,
                        onConnectClick = { viewModel.openOnboarding() }
                    )

                is EarningsScreenState.StripePending ->
                    StripePendingContent(
                        requirements = screenState.requirements,
                        disabledReason = screenState.disabledReason,
                        isBusy = isBusy,
                        onCompleteSetupClick = { viewModel.openOnboarding() }
                    )

                is EarningsScreenState.Ready ->
                    ReadyContent(
                        uiState = uiState,
                        dashboard = screenState.dashboard,
                        hasEarnings = screenState.hasEarnings,
                        viewModel = viewModel
                    )

                is EarningsScreenState.Error ->
                    ErrorContent(message = screenState.message, onRetry = { viewModel.refreshData() })
            }
        }

        // Payout Request Bottom Sheet
        if (uiState.showPayoutSheet) {
            val dashboard = uiState.dashboard
            PayoutRequestBottomSheet(
                availableAmount = dashboard?.balances?.available ?: 0.0,
                currency = dashboard?.balances?.currency ?: "usd",
                error = uiState.payoutError,
                onDismiss = { viewModel.hidePayoutSheet() },
                onPayoutRequested = { amount -> viewModel.requestPayout(amount) }
            )
        }

        // Inline error from Stripe link creation
        inlineError?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearInlineError() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearInlineError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// ── State 1: Loading ──────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PaceDreamColors.HostAccent)
    }
}

// ── State 2: Session Expired / Signed Out ─────────────────────

@Composable
private fun SessionExpiredContent(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.TextSecondary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.Person,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Text(
            text = "Sign in to view earnings",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = "Your session has expired. Please sign in again to access your earnings and payout information.",
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onSignInClick,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
            shape = RoundedCornerShape(PaceDreamRadius.SM),
            modifier = Modifier.fillMaxWidth(0.6f),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                text = "Sign In",
                style = PaceDreamTypography.Subheadline,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── State 3: Stripe Not Connected ─────────────────────────────

@Composable
private fun StripeNotConnectedContent(isBusy: Boolean, onConnectClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.HostAccent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.CreditCard,
                contentDescription = null,
                tint = PaceDreamColors.HostAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Text(
            text = "Set up payouts",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = "Link your bank account via Stripe to receive earnings from bookings.",
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onConnectClick,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
            shape = RoundedCornerShape(PaceDreamRadius.SM),
            modifier = Modifier.fillMaxWidth(0.7f),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            }
            Text(
                text = "Set up payouts",
                style = PaceDreamTypography.Subheadline,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            if (!isBusy) {
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Icon(
                    imageVector = PaceDreamIcons.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PaceDreamIcons.Lock, null, tint = PaceDreamColors.TextTertiary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Secured by Stripe", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary, fontWeight = FontWeight.Medium)
        }
    }
}

// ── State 4: Stripe Pending ───────────────────────────────────

@Composable
private fun StripePendingContent(
    requirements: List<String>,
    disabledReason: String?,
    isBusy: Boolean,
    onCompleteSetupClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Compact status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM),
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        ) {
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaceDreamColors.Warning.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(PaceDreamIcons.CreditCard, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Payout setup in progress",
                            style = PaceDreamTypography.Subheadline,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Complete your Stripe setup to start receiving earnings.",
                            style = PaceDreamTypography.Footnote,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }

                // Requirements (if any)
                if (requirements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PaceDreamRadius.SM))
                            .background(PaceDreamColors.Warning.copy(alpha = 0.06f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Remaining steps", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                        requirements.forEach { req ->
                            Text("• ${req.replace("_", " ")}", style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                        }
                    }
                }

                // Disabled reason
                disabledReason?.let { reason ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PaceDreamRadius.SM))
                            .background(PaceDreamColors.Error.copy(alpha = 0.06f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(PaceDreamIcons.Info, null, tint = PaceDreamColors.Error, modifier = Modifier.size(PaceDreamIconSize.SM))
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(reason.replace("_", " "), style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCompleteSetupClick,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    }
                    Text("Continue Stripe setup", style = PaceDreamTypography.Subheadline, color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (!isBusy) {
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Icon(PaceDreamIcons.ArrowForward, null, tint = Color.White, modifier = Modifier.size(PaceDreamIconSize.SM))
                    }
                }
            }
        }
    }
}

// ── State 5: Ready (Connected — with or without earnings) ─────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    uiState: HostEarningsUiState,
    dashboard: EarningsDashboardResponse,
    hasEarnings: Boolean,
    viewModel: HostEarningsViewModel
) {
    val tabs = listOf("Balance", "Transfers", "Payouts")

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Selector
        PrimaryTabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = PaceDreamColors.Background,
            contentColor = PaceDreamColors.HostAccent
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = {
                        Text(
                            text = title,
                            style = PaceDreamTypography.Subheadline,
                            fontWeight = if (uiState.selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (uiState.selectedTab == index) PaceDreamColors.HostAccent else PaceDreamColors.TextSecondary
                        )
                    }
                )
            }
        }

        when (uiState.selectedTab) {
            0 -> BalanceTabContent(
                dashboard = dashboard,
                hasEarnings = hasEarnings,
                onPayoutClick = { viewModel.showPayoutSheet() }
            )
            1 -> TransfersTabContent(transactions = dashboard.transactions)
            2 -> PayoutsTabContent(
                payouts = dashboard.payouts,
                onRequestPayout = { viewModel.showPayoutSheet() }
            )
        }
    }
}

// ── State 6: Error ────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(PaceDreamIconSize.XXL)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = message,
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        OutlinedButton(
            onClick = onRetry,
            shape = RoundedCornerShape(PaceDreamRadius.SM),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.HostAccent)
        ) {
            Text("Try Again", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Balance Tab ───────────────────────────────────────────────

@Composable
private fun BalanceTabContent(
    dashboard: EarningsDashboardResponse,
    hasEarnings: Boolean,
    onPayoutClick: () -> Unit
) {
    val balances = dashboard.balances

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (balances != null) {
            // Available Balance — hero card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
                    shape = RoundedCornerShape(PaceDreamRadius.XL)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.LG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Available Balance", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextSecondary)
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            formatDollarAmount(balances.available, balances.currency),
                            style = PaceDreamTypography.LargeTitle.copy(fontSize = 36.sp),
                            color = PaceDreamColors.HostAccent,
                            fontWeight = FontWeight.Bold
                        )
                        if (balances.available > 0) {
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                            Button(
                                onClick = onPayoutClick,
                                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                                shape = RoundedCornerShape(PaceDreamRadius.Round),
                                modifier = Modifier.fillMaxWidth(0.6f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(PaceDreamIcons.AttachMoney, null, modifier = Modifier.size(PaceDreamIconSize.SM), tint = Color.White)
                                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                                Text("Request Payout", style = PaceDreamTypography.Subheadline, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Pending Balance — only if non-zero
            if (balances.pending > 0) {
                item {
                    BalanceRow(
                        icon = PaceDreamIcons.Schedule,
                        iconTint = PaceDreamColors.Warning,
                        label = "Pending",
                        sublabel = "Settling in 2–7 business days",
                        amount = formatDollarAmount(balances.pending, balances.currency),
                        amountColor = PaceDreamColors.Accent
                    )
                }
            }

            // Settling — only if non-zero
            if (balances.settling > 0) {
                item {
                    BalanceRow(
                        icon = PaceDreamIcons.Info,
                        iconTint = PaceDreamColors.Warning,
                        label = "Settling on Stripe",
                        sublabel = balances.settlingNote,
                        amount = formatDollarAmount(balances.settling, balances.currency),
                        amountColor = PaceDreamColors.Warning
                    )
                }
            }

            // Lifetime earnings — compact stat row
            if (hasEarnings) {
                item {
                    BalanceRow(
                        icon = PaceDreamIcons.Payment,
                        iconTint = PaceDreamColors.HostAccent,
                        label = "Lifetime Earnings",
                        sublabel = null,
                        amount = formatDollarAmount(balances.lifetime, balances.currency),
                        amountColor = PaceDreamColors.HostAccent
                    )
                }
            }

            // Payout rules — collapsible, secondary
            dashboard.payoutRules?.let { rules ->
                if (rules.shortBookingRule.isNotBlank() || rules.longBookingRule.isNotBlank()) {
                    item { PayoutRulesCard(rules) }
                }
            }
        } else if (!hasEarnings) {
            item {
                EarningsEmptyState(
                    PaceDreamIcons.AttachMoney,
                    "No earnings yet",
                    "Your balance will appear here once you receive bookings."
                )
            }
        }
    }
}

// ── Balance Row (reusable compact row) ────────────────────────

@Composable
private fun BalanceRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    sublabel: String?,
    amount: String,
    amountColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(PaceDreamIconSize.SM))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                sublabel?.let {
                    Text(it, style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                }
            }
            Text(amount, style = PaceDreamTypography.Headline, color = amountColor, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Payout Rules (replaces massive PayoutTimelineCard) ────────

@Composable
private fun PayoutRulesCard(rules: DashboardPayoutRules) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PaceDreamIcons.Info, null, tint = PaceDreamColors.TextSecondary, modifier = Modifier.size(PaceDreamIconSize.SM))
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    "How payouts work",
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) PaceDreamIcons.ExpandLess else PaceDreamIcons.ExpandMore,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                if (rules.shortBookingRule.isNotBlank()) {
                    Text(
                        "Short bookings (< ${rules.shortBookingThresholdHours}h)",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(rules.shortBookingRule, style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                }
                if (rules.longBookingRule.isNotBlank()) {
                    Text(
                        "Long bookings",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(rules.longBookingRule, style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                }
            }
        }
    }
}

// ── Transfers Tab ─────────────────────────────────────────────

@Composable
private fun TransfersTabContent(transactions: List<DashboardTransaction>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (transactions.isEmpty()) {
            item {
                EarningsEmptyState(
                    PaceDreamIcons.Payment,
                    "No transfers yet",
                    "Earnings from completed bookings will appear here."
                )
            }
        } else {
            items(transactions, key = { it.id }) { TransactionRow(it) }
        }
    }
}

@Composable
private fun TransactionRow(transaction: DashboardTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.HostAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(PaceDreamIcons.Payment, null, tint = PaceDreamColors.HostAccent, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description ?: transaction.bookingType ?: "Booking earning",
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatDate(transaction.createdAt ?: ""),
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (transaction.grossAmount > 0) {
                    Text(
                        "Gross: ${formatDollarAmount(transaction.grossAmount)} · Fee: ${formatDollarAmount(transaction.stripeProcessingFee)}",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextTertiary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatDollarAmount(transaction.amount, transaction.currency),
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = transaction.payoutStatus ?: transaction.status ?: "")
            }
        }
    }
}

// ── Payouts Tab ───────────────────────────────────────────────

@Composable
private fun PayoutsTabContent(payouts: List<DashboardPayout>, onRequestPayout: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (payouts.isEmpty()) {
            item {
                EarningsEmptyState(
                    PaceDreamIcons.AttachMoney,
                    "No payouts yet",
                    "Request a payout and your history will appear here."
                )
            }
        } else {
            items(payouts, key = { it.id }) { PayoutRow(it) }
        }
    }
}

@Composable
private fun PayoutRow(payout: DashboardPayout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(PaceDreamIcons.AttachMoney, null, tint = PaceDreamColors.Success, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    payout.description ?: "Bank payout",
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                val arrivalText = payout.arrivalDate?.let { "Arrives: ${formatDate(it)}" } ?: formatDate(payout.createdAt ?: "")
                Text(arrivalText, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.Medium)
                payout.destination?.let { dest ->
                    val bankInfo = listOfNotNull(dest.bankName, dest.last4?.let { "•••• $it" }).joinToString(" ")
                    if (bankInfo.isNotEmpty()) {
                        Text(bankInfo, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatDollarAmount(payout.amount, payout.currency),
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = payout.status)
            }
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val badgeColor = when (status.lowercase()) {
        "succeeded", "paid", "transferred", "paid_out" -> PaceDreamColors.Success
        "pending", "processing", "pending_settlement", "in_transit" -> PaceDreamColors.Warning
        "held", "ready_for_transfer" -> PaceDreamColors.HostAccent
        "failed", "canceled", "blocked", "clawed_back" -> PaceDreamColors.Error
        else -> PaceDreamColors.TextSecondary
    }
    Text(
        text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
        color = badgeColor,
        modifier = Modifier
            .background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ── Empty State ───────────────────────────────────────────────

@Composable
private fun EarningsEmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(PaceDreamIconSize.XXL))
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(title, style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.SemiBold), color = PaceDreamColors.TextPrimary)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        Text(
            subtitle,
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
        )
    }
}

// ── Payout Bottom Sheet ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayoutRequestBottomSheet(
    availableAmount: Double,
    currency: String,
    error: String?,
    onDismiss: () -> Unit,
    onPayoutRequested: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedAmount by remember { mutableIntStateOf(0) }
    val quickAmounts = listOf(25, 50, 100, 250, 500)
    val availableBalanceText = formatDollarAmount(availableAmount, currency)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PaceDreamColors.Background,
        shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.LG)
                .padding(bottom = PaceDreamSpacing.XL)
        ) {
            Text("Request Payout", style = PaceDreamTypography.Title2, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text("Available: $availableBalanceText", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            // Error message
            error?.let {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    it,
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.Error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Text("Quick Amounts", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            val columns = 3
            val rows = (quickAmounts.size + columns - 1) / columns
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < quickAmounts.size) {
                            val quickAmount = quickAmounts[index]
                            val isSelected = selectedAmount == quickAmount
                            Button(
                                onClick = { amount = quickAmount.toString(); selectedAmount = quickAmount },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) PaceDreamColors.HostAccent else PaceDreamColors.Surface),
                                shape = RoundedCornerShape(PaceDreamRadius.SM),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("$$quickAmount", style = PaceDreamTypography.Subheadline, color = if (isSelected) Color.White else PaceDreamColors.HostAccent, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (row < rows - 1) Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text("Custom Amount", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; selectedAmount = 0 },
                placeholder = { Text("0.00", color = PaceDreamColors.TextTertiary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PaceDreamColors.HostAccent, unfocusedBorderColor = PaceDreamColors.Border)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            val parsedAmount = amount.toDoubleOrNull()
            Button(
                onClick = { parsedAmount?.let { onPayoutRequested(it) } },
                enabled = parsedAmount != null && parsedAmount > 0 && parsedAmount <= availableAmount,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.LG)
            ) {
                Text("Request Payout", style = PaceDreamTypography.Button, color = Color.White)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────

private fun formatDollarAmount(amount: Double, currency: String = "usd"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    try { formatter.currency = Currency.getInstance(currency.uppercase()) } catch (e: Exception) {
        timber.log.Timber.w(e, "Unknown currency code: $currency, using USD")
    }
    return formatter.format(amount)
}

private fun formatDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val outputFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (_: Exception) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (_: Exception) { dateString }
    }
}
