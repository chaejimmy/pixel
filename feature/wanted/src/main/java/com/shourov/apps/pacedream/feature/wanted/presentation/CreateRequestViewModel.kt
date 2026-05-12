package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.upload.ImageUploader
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.LocationDto
import com.shourov.apps.pacedream.feature.wanted.model.CreateRequestForm
import com.shourov.apps.pacedream.feature.wanted.model.CreateRequestUiState
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoriesByType
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * State container for [CreateRequestScreen].
 *
 * Validation mirrors the web rules in `RequestWizard.tsx`:
 *  - title         3..200 chars (required)
 *  - description   10..2000 chars (required)
 *  - category      required, must be from the per-type list
 *  - budget        non-negative number, or unset for "negotiable"
 *
 * Backend errors are mapped to friendly copy in [submit]; raw exception
 * messages are never surfaced to the UI.
 */
@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val repository: WantedRepository,
    private val imageUploader: ImageUploader,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateRequestUiState())
    val state: StateFlow<CreateRequestUiState> = _state.asStateFlow()

    init {
        Timber.d("post_request_opened source=unknown")
    }

    fun update(transform: (CreateRequestForm) -> CreateRequestForm) {
        _state.update { current ->
            val nextForm = transform(current.form)
            // If the user changed type, reset the category to the first
            // option for the new type so we never POST a category that
            // doesn't belong to its parent type.
            val typeChanged = nextForm.type != current.form.type
            val safeForm = if (typeChanged) {
                val firstCategory = WantedCategoriesByType[nextForm.type]
                    ?.firstOrNull()
                    ?.key
                    ?: "other"
                nextForm.copy(category = firstCategory)
            } else nextForm
            current.copy(form = safeForm, error = null)
        }
    }

    /**
     * Convenience setter for the type field that also fires the category
     * reset logic. Useful from FilterChip onClick handlers in the UI.
     */
    fun selectType(type: WantedType) {
        update { it.copy(type = type) }
    }

    /**
     * Upload the picked image to object storage and stash the returned
     * public URL on the form. The picker hands us a `content://` URI that
     * is only meaningful on this device — POSTing it as `coverImageUrl`
     * would leave every other client rendering a broken image.
     */
    fun uploadCoverImage(uri: Uri) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    uploading = true,
                    error = null,
                    form = it.form.copy(imageUrl = null),
                )
            }
            imageUploader.uploadImage(uri)
                .onSuccess { publicUrl ->
                    Timber.d("post_request_cover_uploaded url=$publicUrl")
                    _state.update {
                        it.copy(
                            uploading = false,
                            form = it.form.copy(imageUrl = publicUrl),
                        )
                    }
                }
                .onFailure { e ->
                    Timber.w(e, "post_request_cover_upload_failed")
                    _state.update {
                        it.copy(
                            uploading = false,
                            error = "Couldn't upload image — try again or post without one.",
                        )
                    }
                }
        }
    }

    fun clearCoverImage() {
        _state.update {
            it.copy(
                uploading = false,
                form = it.form.copy(imageUrl = null),
            )
        }
    }

    fun submit() {
        val form = _state.value.form
        if (_state.value.uploading) return
        validate(form)?.let { err ->
            _state.update { it.copy(error = err) }
            return
        }

        val budgetValue = form.budget.takeIf { it.isNotBlank() }
            ?.replace(",", "")
            ?.toDoubleOrNull()

        val location = LocationDto(
            city = form.locationCity.trim().ifBlank { null },
            state = form.locationState.trim().ifBlank { null },
            country = form.locationCountry.trim().ifBlank { null },
        ).takeIf {
            !it.city.isNullOrBlank() || !it.state.isNullOrBlank() || !it.country.isNullOrBlank()
        }

        val isoStart = form.startDate?.toIsoUtcDate()
        val isoEnd = form.endDate?.toIsoUtcDate()?.takeIf { it != isoStart }

        val body = CreateRequestBody(
            type = form.type.key,
            category = form.category,
            title = form.title.trim(),
            description = form.description.trim(),
            location = location,
            date = isoStart,
            endDate = isoEnd,
            budget = budgetValue,
            coverImageUrl = form.imageUrl,
            imageSource = if (form.imageUrl != null) "uploaded" else null,
            tags = null,
        )

        Timber.d("post_request_submit_started type=${form.type.key} category=${form.category}")

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            repository.createRequest(body)
                .onSuccess { created ->
                    Timber.d("post_request_submit_succeeded requestId=${created.id}")
                    _state.update {
                        it.copy(submitting = false, createdId = created.id)
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "post_request_submit_failed")
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = friendlyError(e),
                        )
                    }
                }
        }
    }

    /**
     * Convert an epoch-millis value (assumed UTC midnight, as produced by
     * `DateRangePicker`) to its `yyyy-MM-dd` ISO-8601 representation.
     */
    private fun Long.toIsoUtcDate(): String =
        Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()

    private fun validate(form: CreateRequestForm): String? {
        val title = form.title.trim()
        if (title.length < 3) return "Add a short title (at least 3 characters)."
        if (title.length > 200) return "Title is too long (max 200 characters)."
        val description = form.description.trim()
        if (description.length < 10) return "Tell us a bit more — at least 10 characters."
        if (description.length > 2000) return "Description is too long (max 2000 characters)."
        if (form.category.isBlank()) return "Pick a category."
        val budgetText = form.budget.trim()
        if (budgetText.isNotEmpty()) {
            val parsed = budgetText.replace(",", "").toDoubleOrNull()
            if (parsed == null) return "Enter a valid budget, or leave it blank if negotiable."
            if (parsed < 0) return "Budget can't be negative."
        }
        return null
    }

    /**
     * Map every known failure mode to a short, user-friendly string.
     * Raw backend messages are never bubbled up — they are often vendor
     * stack traces or HTML, and a noisy alert isn't actionable.
     */
    private fun friendlyError(e: Throwable): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("401") || msg.contains("unauthor") ->
                "Please sign in again to post your request."
            msg.contains("403") -> "We can't post this request right now."
            msg.contains("429") -> "You're posting requests a bit too quickly. Please wait a moment."
            msg.contains("timeout") || msg.contains("unable to resolve") || msg.contains("network") ->
                "You appear to be offline. Please check your connection."
            else -> "Couldn't post your request. Please try again."
        }
    }
}
