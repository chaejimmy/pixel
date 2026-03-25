package com.shourov.apps.pacedream.feature.host.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.ConnectAccount
import com.shourov.apps.pacedream.feature.host.data.ConnectAccountStatus
import com.shourov.apps.pacedream.feature.host.data.ConnectBalance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StripeConnectOnboardingScreen(
    onBackClick: () -> Unit = {},
    viewModel: StripeConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payment Setup",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PaceDreamColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(bottom = PaceDreamSpacing.LG)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PaceDreamSpacing.LG),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = PaceDreamIcons.CreditCard,
                    contentDescription = null,
                    tint = PaceDreamColors.HostAccent,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                Text(
                    text = "Stripe Connect Setup",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = "Set up your Stripe Connect account to receive payments from guests",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Status Card
            ConnectAccountStatusCard(
                account = uiState.connectAccount,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Action Buttons
            Column(
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                val account = uiState.connectAccount

                if (account == null || account.status == ConnectAccountStatus.NOT_CREATED) {
                    // Create Account Button
                    Button(
                        onClick = { viewModel.createConnectAccount() },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        } else {
                            Icon(
                                imageVector = PaceDreamIcons.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        }
                        Text(
                            text = "Create Stripe Connect Account",
                            style = PaceDreamTypography.Button,
                            color = Color.White
                        )
                    }
                } else if (account.status == ConnectAccountStatus.PENDING || account.status == ConnectAccountStatus.UNDER_REVIEW) {
                    // Complete Onboarding Button
                    Button(
                        onClick = {
                            viewModel.startOnboarding { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Accent),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(
                            text = "Complete Onboarding",
                            style = PaceDreamTypography.Button,
                            color = Color.White
                        )
                    }
                } else if (account.status == ConnectAccountStatus.ENABLED) {
                    // Open Dashboard Button
                    Button(
                        onClick = {
                            viewModel.openDashboard { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(
                            text = "Open Stripe Dashboard",
                            style = PaceDreamTypography.Button,
                            color = Color.White
                        )
                    }
                }

                // Refresh Status Button
                TextButton(
                    onClick = { viewModel.refreshAccountStatus() },
                    enabled = !uiState.isRefreshing
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            color = PaceDreamColors.HostAccent,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    } else {
                        Icon(
                            imageVector = PaceDreamIcons.History,
                            contentDescription = null,
                            tint = PaceDreamColors.HostAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    }
                    Text(
                        text = "Refresh Status",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.HostAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Benefits List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.MD),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    Text(
                        text = "Benefits of Stripe Connect:",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    BenefitRow(icon = PaceDreamIcons.AttachMoney, text = "Receive payments directly to your bank account")
                    BenefitRow(icon = PaceDreamIcons.Security, text = "Secure and PCI-compliant payment processing")
                    BenefitRow(icon = PaceDreamIcons.Schedule, text = "Fast payouts (2-7 business days)")
                    BenefitRow(icon = PaceDreamIcons.Language, text = "Support for 135+ currencies")
                    BenefitRow(icon = PaceDreamIcons.TrendingUp, text = "Detailed earnings and transaction reports")
                }
            }

            // Balance Preview
            uiState.balance?.let { balance ->
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                BalancePreviewCard(
                    balance = balance,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                )
            }
        }
    }

    // Error alert
    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

// ============================================================================
// Connect Account Status Card - matches iOS ConnectAccountStatusCard
// ============================================================================
@Composable
private fun ConnectAccountStatusCard(
    account: ConnectAccount?,
    modifier: Modifier = Modifier
) {
    val statusColor = when (account?.status) {
        null, ConnectAccountStatus.NOT_CREATED -> PaceDreamColors.TextSecondary
        ConnectAccountStatus.PENDING, ConnectAccountStatus.UNDER_REVIEW -> PaceDreamColors.Warning
        ConnectAccountStatus.ENABLED -> PaceDreamColors.Success
        ConnectAccountStatus.RESTRICTED, ConnectAccountStatus.REJECTED -> PaceDreamColors.Error
    }

    val statusIcon = when (account?.status) {
        null, ConnectAccountStatus.NOT_CREATED -> PaceDreamIcons.Add
        ConnectAccountStatus.PENDING, ConnectAccountStatus.UNDER_REVIEW -> PaceDreamIcons.Schedule
        ConnectAccountStatus.ENABLED -> PaceDreamIcons.CheckCircle
        ConnectAccountStatus.RESTRICTED -> PaceDreamIcons.Warning
        ConnectAccountStatus.REJECTED -> PaceDreamIcons.Close
    }

    val statusTitle = when (account?.status) {
        null, ConnectAccountStatus.NOT_CREATED -> "No Account"
        ConnectAccountStatus.PENDING -> "Account Pending"
        ConnectAccountStatus.UNDER_REVIEW -> "Under Review"
        ConnectAccountStatus.ENABLED -> "Account Active"
        ConnectAccountStatus.RESTRICTED -> "Account Restricted"
        ConnectAccountStatus.REJECTED -> "Account Rejected"
    }

    val statusDescription = when (account?.status) {
        null, ConnectAccountStatus.NOT_CREATED -> "Create a Stripe Connect account to start receiving payments"
        ConnectAccountStatus.PENDING -> "Complete the onboarding process to activate your account"
        ConnectAccountStatus.UNDER_REVIEW -> "Your account is being reviewed by Stripe"
        ConnectAccountStatus.ENABLED -> "Your account is active and ready to receive payments"
        ConnectAccountStatus.RESTRICTED -> "Your account has restrictions. Please check your Stripe dashboard"
        ConnectAccountStatus.REJECTED -> "Your account was rejected. Please contact support"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, statusColor, RoundedCornerShape(PaceDreamRadius.MD)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Column {
                    Text(
                        text = statusTitle,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                    Text(
                        text = statusDescription,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            // Requirements
            account?.let { acct ->
                if (acct.status != ConnectAccountStatus.ENABLED) {
                    acct.requirements?.let { requirements ->
                        if (requirements.resolvedCurrentlyDue.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            Text(
                                text = "Required:",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                            requirements.resolvedCurrentlyDue.forEach { requirement ->
                                Text(
                                    text = "  \u2022 $requirement",
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Account Details
                if (acct.id.isNotBlank()) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Account ID",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                            Text(
                                text = acct.id,
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Charges Enabled",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                            Text(
                                text = if (acct.chargesEnabled) "Yes" else "No",
                                style = PaceDreamTypography.Caption,
                                color = if (acct.chargesEnabled) PaceDreamColors.Success else PaceDreamColors.Error
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Balance Preview Card - matches iOS BalancePreviewCard
// ============================================================================
@Composable
private fun BalancePreviewCard(
    balance: ConnectBalance,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = "Current Balance",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Available",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = formatBalanceAmount(balance.available.firstOrNull()?.amount ?: 0),
                        style = PaceDreamTypography.Title2,
                        color = PaceDreamColors.HostAccent
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Pending",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = formatBalanceAmount(balance.pending.firstOrNull()?.amount ?: 0),
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.Accent
                    )
                }
            }
        }
    }
}

// ============================================================================
// Benefit Row - matches iOS BenefitRow
// ============================================================================
@Composable
private fun BenefitRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = PaceDreamSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.HostAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        Text(
            text = text,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary
        )
    }
}

private fun formatBalanceAmount(amountInCents: Int): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)
    return formatter.format(amountInCents / 100.0)
}
