package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedType

// ============================================================================
// Categories response envelope
//
// GET /v1/requests/categories returns the per-type category taxonomy.
// Accepted shapes (envelope keys match the rest of the requests API):
//   { status, data: { space: [...], item: [...], service: [...] } }
//   { ok,     data: { ... } }
//   {           categories: { ... } }
//
// Each list entry is { key, label }. Unknown top-level keys are ignored so
// the backend can ship new types ahead of a coordinated app release.
// ============================================================================

data class CategoriesResponse(
    val status: Boolean = false,
    val ok: Boolean = false,
    val message: String? = null,
    val data: Map<String, List<CategoryOptionDto>>? = null,
    val categories: Map<String, List<CategoryOptionDto>>? = null,
) {
    val payload: Map<String, List<CategoryOptionDto>>
        get() = data ?: categories ?: emptyMap()
}

data class CategoryOptionDto(
    val key: String? = null,
    val label: String? = null,
)

/**
 * Project the wire payload onto the typed domain map. Entries with a
 * missing key or label are dropped — the dropdown can't render them and
 * the server can't validate them on POST either.
 */
fun CategoriesResponse.toDomain(): Map<WantedType, List<WantedCategoryOption>> =
    payload.mapNotNull { (typeKey, options) ->
        val type = WantedType.entries.firstOrNull { it.key.equals(typeKey, ignoreCase = true) }
            ?: return@mapNotNull null
        val mapped = options.mapNotNull { dto ->
            val key = dto.key?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val label = dto.label?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            WantedCategoryOption(key = key, label = label)
        }
        if (mapped.isEmpty()) null else type to mapped
    }.toMap()
