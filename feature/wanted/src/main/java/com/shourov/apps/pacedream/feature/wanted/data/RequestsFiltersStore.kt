package com.shourov.apps.pacedream.feature.wanted.data

import android.content.Context
import android.content.SharedPreferences
import com.shourov.apps.pacedream.feature.wanted.model.FilterState
import com.shourov.apps.pacedream.feature.wanted.model.RequestSort
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the most recent [FilterState] applied to the Requests feed so the
 * next session restores the same filters. Mirrors the pattern used by
 * [com.shourov.apps.pacedream.feature.wishlist.data.FavoritesCache] — a tiny
 * SharedPreferences-backed store is enough here; we don't need Flow APIs
 * because the ViewModel only reads the value once on init.
 */
interface RequestsFiltersStore {
    fun load(): FilterState
    fun save(state: FilterState)
}

@Singleton
class RequestsFiltersStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RequestsFiltersStore {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun load(): FilterState {
        val typeKey = prefs.getString(KEY_TYPE, null)
        val category = prefs.getString(KEY_CATEGORY, null)
        val sortKey = prefs.getString(KEY_SORT, null)
        return FilterState(
            type = typeKey?.let { key ->
                WantedType.entries.firstOrNull { it.key == key }
            },
            category = category,
            sort = RequestSort.fromKey(sortKey),
        )
    }

    override fun save(state: FilterState) {
        prefs.edit()
            .putString(KEY_TYPE, state.type?.key)
            .putString(KEY_CATEGORY, state.category)
            .putString(KEY_SORT, state.sort.key)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "pacedream_requests_filters"
        const val KEY_TYPE = "type"
        const val KEY_CATEGORY = "category"
        const val KEY_SORT = "sort"
    }
}
