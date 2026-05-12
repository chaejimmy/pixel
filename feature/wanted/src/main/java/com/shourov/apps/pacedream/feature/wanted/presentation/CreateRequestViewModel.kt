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
import com.shourov.apps.pacedream.feature.wanted.model.FieldErrors
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
        loadCategories()
    }

    /**
     * Fetch the per-type category taxonomy from the backend on screen
     * entry. Failures are silently swallowed — the UI state already carries
     * the hardcoded [WantedCategoriesByType] fallback, so the dropdown
     * stays usable on cold start and offline. Success overwrites the
     * fallback in-place; the repository caches the result for the rest of
     * the process so re-entering the screen is free.
     */
    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories()
                .onSuccess { remote ->
                    Timber.d("post_request_categories_loaded types=${remote.keys.joinToString(",") { it.key }}")
                    _state.update { current ->
                        // Reconcile the in-flight form: if the user already
                        // picked a category that no longer exists for their
                        // type, fall back to the first server option.
                        val options = remote[current.form.type].orEmpty()
                        val pickedStillValid = options.any { it.key == current.form.category }
                        val nextForm = if (pickedStillValid || options.isEmpty()) {
                            current.form
                        } else {
                            current.form.copy(category = options.first().key)
                        }
                        current.copy(categoriesByType = remote, form = nextForm)
                    }
                }
                .onFailure { e ->
                    Timber.w(e, "post_request_categories_load_failed (falling back to local taxonomy)")
                }
        }
    }

    fun update(transform: (CreateRequestForm) -> CreateRequestForm) {
        _state.update { current ->
            val nextForm = transform(current.form)
            // If the user changed type, reset the category to the first
            // option for the new type so we never POST a category that
            // doesn't belong to its parent type.
            val typeChanged = nextForm.type != current.form.type
            val safeForm = if (typeChanged) {
                val firstCategory = current.categoriesByType[nextForm.type]
                    ?.firstOrNull()
                    ?.key
                    ?: WantedCategoriesByType[nextForm.type]?.firstOrNull()?.key
                    ?: "other"
                nextForm.copy(category = firstCategory)
            } else nextForm
            current.copy(
                form = safeForm,
                fieldErrors = computeFieldErrors(safeForm),
                error = null,
            )
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
        val current = _state.value
        val form = current.form
        if (current.uploading) return
        // The submit button is gated on live validity in the UI, so reaching
        // this point with errors implies a programmatic call — bail rather
        // than POST an invalid request.
        if (!current.fieldErrors.isEmpty() || !current.requiredFieldsPresent) return

        val budgetValue = form.budget.takeIf { it.isNotBlank() }
            ?.replace(",", "")
            ?.toDoubleOrNull()

        // The autocomplete sheet either populates a [SelectedPlace] or leaves
        // the field null. We don't fabricate a payload when nothing is picked —
        // see acceptance criteria for the "empty state" behaviour.
        val location = form.location?.let { place ->
            LocationDto(
                city = place.city.trim().ifBlank { null },
                state = place.region.trim().ifBlank { null },
                country = place.country.trim().ifBlank { null },
                lat = place.lat,
                lng = place.lng,
            ).takeIf {
                !it.city.isNullOrBlank() || !it.state.isNullOrBlank() ||
                    !it.country.isNullOrBlank() || it.lat != null || it.lng != null
            }
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

    /**
     * Field-level validation that runs on every keystroke. Errors here are
     * for content the user actually typed — empty required fields are
     * tracked separately via [CreateRequestUiState.requiredFieldsPresent] so
     * we don't shout "too short" at a field the user hasn't reached yet.
     */
    private fun computeFieldErrors(form: CreateRequestForm): FieldErrors {
        val title = form.title.trim()
        val titleError = when {
            title.length > CreateRequestUiState.TITLE_MAX_LENGTH ->
                "Title is too long (${CreateRequestUiState.TITLE_MAX_LENGTH} max)"
            else -> null
        }
        val description = form.description.trim()
        val descriptionError = when {
            description.length > CreateRequestUiState.DESCRIPTION_MAX_LENGTH ->
                "Description is too long (${CreateRequestUiState.DESCRIPTION_MAX_LENGTH} max)"
            else -> null
        }
        val budgetText = form.budget.trim()
        val budgetError = if (budgetText.isEmpty()) {
            null
        } else {
            val parsed = budgetText.replace(",", "").toDoubleOrNull()
            when {
                parsed == null -> "Enter a valid budget, or leave it blank if negotiable."
                parsed < 0 -> "Budget can't be negative."
                else -> null
            }
        }
        return FieldErrors(
            titleError = titleError,
            descriptionError = descriptionError,
            budgetError = budgetError,
        )
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
