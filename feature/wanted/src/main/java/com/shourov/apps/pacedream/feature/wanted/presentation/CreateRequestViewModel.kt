package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.model.CreateRequestForm
import com.shourov.apps.pacedream.feature.wanted.model.CreateRequestUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val repository: WantedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateRequestUiState())
    val state: StateFlow<CreateRequestUiState> = _state.asStateFlow()

    fun update(transform: (CreateRequestForm) -> CreateRequestForm) {
        _state.update { it.copy(form = transform(it.form), error = null) }
    }

    fun submit() {
        val form = _state.value.form
        val title = form.title.trim()
        val description = form.description.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(error = "Title is required") }
            return
        }
        if (description.isEmpty()) {
            _state.update { it.copy(error = "Description is required") }
            return
        }
        val budgetValue = form.budget.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (form.budget.isNotBlank() && (budgetValue == null || budgetValue < 0)) {
            _state.update { it.copy(error = "Enter a valid budget") }
            return
        }

        val body = CreateRequestBody(
            type = form.type,
            title = title,
            description = description,
            location = form.location.trim(),
            dateTime = form.dateTime.takeIf { it.isNotBlank() },
            budget = budgetValue,
            imageUrl = form.imageUrl,
        )

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            repository.createRequest(body)
                .onSuccess { created ->
                    _state.update {
                        it.copy(submitting = false, createdId = created.id)
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to create request")
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = e.message ?: "Couldn't post request",
                        )
                    }
                }
        }
    }
}
