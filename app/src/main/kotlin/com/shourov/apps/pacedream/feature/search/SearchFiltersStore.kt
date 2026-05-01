package com.shourov.apps.pacedream.feature.search

import com.shourov.apps.pacedream.feature.home.presentation.components.FilterCriteria
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-singleton that holds the active [FilterCriteria] applied to the
 * search results pipeline.
 *
 * The Search screen and the Filter screen are sibling Compose destinations
 * that don't share a ViewModel scope.  Routing the filter state through a
 * shared store decouples them: [com.shourov.apps.pacedream.navigation.DashboardNavigation]
 * writes here when the user taps Apply, and [SearchViewModel] observes
 * the flow and re-runs the query.  The store also lets the Filter screen
 * read back the currently-applied criteria so reopening it shows what
 * the user previously selected.
 *
 * Singleton-scoped so the criteria survive navigating away from Search
 * and back within the same app process; cleared on explicit user action
 * (`clear()`) rather than on backstack pops, which matches Airbnb's
 * sticky-filter UX.
 */
@Singleton
class SearchFiltersStore @Inject constructor() {

    private val _criteria = MutableStateFlow(FilterCriteria())

    /** Currently-applied filters; never null, defaults to an empty criteria. */
    val criteria: StateFlow<FilterCriteria> = _criteria.asStateFlow()

    fun update(next: FilterCriteria) {
        _criteria.value = next
    }

    fun clear() {
        _criteria.value = FilterCriteria()
    }
}

/**
 * Entry point used by Compose composables (e.g. the FILTER route handler
 * in DashboardNavigation) that need to read or write the active filter
 * criteria but aren't ViewModel-scoped.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SearchFiltersStoreEntryPoint {
    fun searchFiltersStore(): SearchFiltersStore
}

/**
 * Compact human-readable summary of the active filter set, suitable for
 * a single Timber.d line in debug builds.  Lists only fields that are
 * actually constraining the query — empty/zero/null fields are omitted
 * so the log scans cleanly even when most filters are at their defaults.
 *
 * NOT for user-facing display; the search summary bar reads structured
 * fields off [SearchUiState] directly.  Releases never see this string
 * (Timber's DebugTree is planted only in BuildConfig.DEBUG).
 */
internal fun FilterCriteria.toDebugSummary(): String {
    if (isEmpty) return "FilterCriteria(empty)"
    val parts = buildList {
        if (checkInEpochDay != null || checkOutEpochDay != null) {
            add("dates=$checkInEpochDay..$checkOutEpochDay")
        }
        if (totalGuests > 0) add("guests=$totalGuests")
        if (infants > 0) add("infants=$infants")
        if (pets > 0) add("pets=$pets")
        if (!propertyType.isNullOrBlank()) add("propertyType=$propertyType")
        if (minPrice != null) add("min=$minPrice")
        if (maxPrice != null) add("max=$maxPrice")
        if (bedrooms != null) add("bedrooms=$bedrooms")
        if (beds != null) add("beds=$beds")
        if (bathrooms != null) add("baths=$bathrooms")
        if (instantBookOnly) add("instantBook=true")
        if (amenities.isNotEmpty()) add("amenities=${amenities.joinToString(",")}")
    }
    return "FilterCriteria(${parts.joinToString(" ")})"
}

