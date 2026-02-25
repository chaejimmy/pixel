package com.pacedream.app.feature.settings.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.app.core.auth.TokenStorage
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPaymentMethodsScreen(
    onBackClick: () -> Unit,
    viewModel: PaymentMethodsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val publishableKey = remember { StripePublishableKeyProvider.getPublishableKey() }
    val isStripeConfigured = !publishableKey.isNullOrBlank()

    val paymentSheet = rememberPaymentSheet(
        onResult = { result ->
            when (result) {
                is PaymentSheetResult.Completed -> {
                    scope.launch {
                        snackbarHostState.showSnackbar("Card added successfully.")
                    }
                    viewModel.loadPaymentMethods()
                }
                is PaymentSheetResult.Canceled -> {
                    // No-op
                }
                is PaymentSheetResult.Failed -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            result.error.localizedMessage
                                ?: "Unable to add card. Please try again."
                        )
                    }
                }
            }
        }
    )

    LaunchedEffect(publishableKey, context) {
        if (isStripeConfigured) {
            PaymentConfiguration.init(
                context = context,
                publishableKey = publishableKey!!
            )
        } else {
            Timber.w("Stripe publishable key is missing; disabling Add Card.")
        }
        viewModel.loadPaymentMethods()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            if (message.isNotBlank()) {
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    SettingsPaymentMethodsContent(
        uiState = uiState,
        onBackClick = onBackClick,
        isStripeConfigured = isStripeConfigured,
        onAddCardClick = {
            scope.launch {
                when (val result = viewModel.createSetupIntent()) {
                    is com.pacedream.app.core.network.ApiResult.Success -> {
                        val clientSecret = result.data
                        paymentSheet.presentWithSetupIntent(
                            clientSecret,
                            PaymentSheet.Configuration(
                                merchantDisplayName = "PaceDream"
                            )
                        )
                    }
                    is com.pacedream.app.core.network.ApiResult.Failure -> {
                        snackbarHostState.showSnackbar(
                            result.error.message
                                ?: "Unable to start card setup. Please try again."
                        )
                    }
                }
            }
        },
        onSetDefault = { id -> viewModel.setDefault(id) },
        onDelete = { id -> viewModel.deletePaymentMethod(id) },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPaymentMethodsContent(
    uiState: PaymentMethodsUiState,
    onBackClick: () -> Unit,
    isStripeConfigured: Boolean,
    onAddCardClick: () -> Unit,
    onSetDefault: (String) -> Unit,
    onDelete: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val showDeleteDialog: MutableState<PaymentMethod?> = remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null && uiState.paymentMethods.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { /* reload */ }) {
                            Text("Retry")
                        }
                    }
                }

                uiState.paymentMethods.isEmpty() -> {
                    EmptyPaymentMethodsState(
                        isStripeConfigured = isStripeConfigured,
                        isCreatingSetupIntent = uiState.isCreatingSetupIntent,
                        onAddCardClick = onAddCardClick
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (!isStripeConfigured) {
                            StripeMissingWarning()
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(
                            onClick = onAddCardClick,
                            enabled = isStripeConfigured && !uiState.isCreatingSetupIntent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isCreatingSetupIntent) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text("Add Card")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.paymentMethods) { method ->
                                PaymentMethodCard(
                                    method = method,
                                    onSetDefault = { onSetDefault(method.id) },
                                    onDelete = { showDeleteDialog.value = method }
                                )
                            }
                        }
                    }
                }
            }

            showDeleteDialog.value?.let { method ->
                AlertDialog(
                    onDismissRequest = { showDeleteDialog.value = null },
                    title = { Text("Remove payment method") },
                    text = { Text("Are you sure you want to remove this payment method?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDelete(method.id)
                                showDeleteDialog.value = null
                            }
                        ) {
                            Text("Remove")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteDialog.value = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyPaymentMethodsState(
    isStripeConfigured: Boolean,
    isCreatingSetupIntent: Boolean,
    onAddCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = PaceDreamIcons.CreditCard,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No payment methods",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add a card to securely store your payment details with Stripe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddCardClick,
            enabled = isStripeConfigured && !isCreatingSetupIntent,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCreatingSetupIntent) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }
            Text("Add Card")
        }
        if (!isStripeConfigured) {
            Spacer(modifier = Modifier.height(12.dp))
            StripeMissingWarning()
        }
    }
}

@Composable
private fun StripeMissingWarning() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Stripe is not configured. Add Card is disabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PaceDreamIcons.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(0.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = method.brand.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "•••• •••• •••• ${method.last4}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Expires ${formatExpiry(method.expMonth, method.expYear)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (method.isDefault) {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Text("Default")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!method.isDefault) {
                    Text(
                        text = "Set as default",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onSetDefault)
                    )
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onDelete)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatExpiry(expMonth: Int?, expYear: Int?): String {
    if (expMonth == null || expYear == null) return "--/--"
    val month = expMonth.coerceIn(1, 12).toString().padStart(2, '0')
    val yearShort = (expYear % 100).toString().padStart(2, '0')
    return "$month/$yearShort"
}

/**
 * Helper to safely access Stripe publishable key from BuildConfig.
 */
object StripePublishableKeyProvider {
    fun getPublishableKey(): String? {
        return try {
            val value = com.shourov.apps.pacedream.BuildConfig.STRIPE_PUBLISHABLE_KEY
            value.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun rememberPaymentSheet(
    onResult: (PaymentSheetResult) -> Unit
): PaymentSheet {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
        ?: throw IllegalStateException("PaymentSheet requires a ComponentActivity context")
    return remember(activity) {
        PaymentSheet(activity, onResult)
    }
}

