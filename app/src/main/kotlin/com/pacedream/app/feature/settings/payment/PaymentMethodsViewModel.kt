package com.pacedream.app.feature.settings.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentMethodsUiState(
    val isLoading: Boolean = false,
    val isCreatingSetupIntent: Boolean = false,
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val errorMessage: String? = null,
    val unauthorized: Boolean = false
)

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    private val repository: PaymentMethodsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentMethodsUiState())
    val uiState: StateFlow<PaymentMethodsUiState> = _uiState.asStateFlow()

    fun loadPaymentMethods() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.fetchPaymentMethods()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            paymentMethods = result.data,
                            errorMessage = null,
                            unauthorized = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    if (result.error is ApiError.Unauthorized) {
                        sessionManager.signOut()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                paymentMethods = emptyList(),
                                errorMessage = "Please log in to continue.",
                                unauthorized = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.error.message
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun createSetupIntent(): ApiResult<String> {
        _uiState.update { it.copy(isCreatingSetupIntent = true, errorMessage = null) }
        val result = repository.createSetupIntent()
        _uiState.update { it.copy(isCreatingSetupIntent = false) }
        return result
    }

    fun setDefault(paymentMethodId: String) {
        viewModelScope.launch {
            when (val result = repository.setDefault(paymentMethodId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            paymentMethods = result.data,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    fun deletePaymentMethod(id: String) {
        viewModelScope.launch {
            when (val result = repository.deletePaymentMethod(id)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            paymentMethods = result.data,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(errorMessage = result.error.message)
                    }
                }
            }
        }
    }
}

