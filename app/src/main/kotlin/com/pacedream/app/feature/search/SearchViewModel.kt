package com.pacedream.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.pacedream.app.feature.listing.ListingPriceFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            performSearch()
        }
    }

    fun onTabChanged(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (_uiState.value.query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch { performSearch() }
        }
    }

    fun onCategoryChanged(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        if (_uiState.value.query.isNotBlank() || category != null) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch { performSearch() }
        }
    }

    fun search() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch() }
    }

    private suspend fun performSearch() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Map tab to the web API's "category" parameter (iOS parity).
        // All first-stage categories use /poc/listings endpoint.
        val backendCategory: String = when (state.selectedTab) {
            SearchTab.SPACES -> "time_based"
            SearchTab.ITEMS -> "hourly_rental_gear"
            SearchTab.SERVICES -> "time_based"  // Services share the time-based endpoint
        }

        val category: String = when (state.selectedTab) {
            SearchTab.SPACES -> "time-based"
            SearchTab.ITEMS -> "hourly-rental-gears"
            SearchTab.SERVICES -> "time-based"
        }

        val queryParams = buildMap<String, String?> {
            put("status", "published")
            put("limit", "24")
            put("skip_pagination", "true")
            put("category", backendCategory)
            if (state.query.isNotBlank()) put("q", state.query)
        }

        // Primary: backend API directly (website parity)
        val primaryUrl = appConfig.buildApiUrl("poc", "listings", queryParams = queryParams)

        Timber.d("Search primary URL: $primaryUrl")
        val primaryResult = apiClient.get(primaryUrl, includeAuth = false)
        if (primaryResult is ApiResult.Success) {
            val items = parseSearchResults(primaryResult.data, state.selectedTab)
            _uiState.update {
                it.copy(isLoading = false, results = items, errorMessage = null)
            }
            return
        }

        // Fallback: backend /v1/search endpoint (iOS parity)
        // Only fall back when q exists (matches iOS behavior)
        val q = state.query.trim()
        if (q.isBlank()) {
            _uiState.update { it.copy(isLoading = false, results = emptyList()) }
            return
        }
        val fallbackUrl = appConfig.buildApiUrl(
            "search",
            queryParams = buildMap {
                put("q", q)
                put("page", "0")
                put("perPage", "24")
                put("category", category)
            }
        )

        Timber.d("Search fallback URL: $fallbackUrl")
        when (val result = apiClient.get(fallbackUrl, includeAuth = false)) {
            is ApiResult.Success -> {
                val items = parseSearchResults(result.data, state.selectedTab)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = items,
                        errorMessage = null
                    )
                }
            }
            is ApiResult.Failure -> {
                Timber.e("Search failed: ${result.error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    /**
     * Tolerant search results parsing.
     * Accepts multiple response formats:
     * - direct array
     * - { items: [] } / { listings: [] } / { data: [] }
     * - { data: { items: [] } } / { data: { listings: [] } }
     */
    private fun parseSearchResults(responseBody: String, tab: SearchTab): List<SearchResultItem> {
        return try {
            val root = json.parseToJsonElement(responseBody)
            val dataArray = findArray(
                root,
                listOf("hits"),
                listOf("items"),
                listOf("listings"),
                listOf("data", "hits"),
                listOf("data", "listings"),
                listOf("data", "items"),
                listOf("data")
            ) ?: JsonArray(emptyList())

            dataArray.mapNotNull { item ->
                try {
                    val itemObj = item.jsonObject
                    val id = itemObj["_id"].stringOrNull()
                        ?: itemObj["objectID"].stringOrNull()
                        ?: itemObj["id"].stringOrNull()
                        ?: itemObj["listingId"].stringOrNull()
                        ?: return@mapNotNull null

                    val title = ListingPriceFormatter.stripTrailingPriceFromTitle(
                        itemObj["title"].stringOrNull()
                            ?: itemObj["name"].stringOrNull()
                            ?: "Listing"
                    )

                    val imageUrl = itemObj["image"].stringOrNull()
                        ?: itemObj["imageUrl"].stringOrNull()
                        ?: itemObj["thumbnail"].stringOrNull()
                        ?: itemObj["images"]?.jsonArray?.firstOrNull().stringOrNull()
                        ?: itemObj["gallery"]?.jsonObject?.get("images")?.jsonArray?.firstOrNull().stringOrNull()
                        ?: itemObj["galleryImages"]?.jsonArray?.firstOrNull().stringOrNull()

                    val location = itemObj["city"].stringOrNull()
                        ?: (itemObj["location"] as? JsonObject)?.get("city").stringOrNull()
                        ?: (itemObj["location"] as? JsonObject)?.get("address").stringOrNull()
                        ?: itemObj["location"].stringOrNull()
                        ?: (itemObj["address"] as? JsonObject)?.get("city").stringOrNull()

                    val price = itemObj["priceText"].stringOrNull()
                        ?: ListingPriceFormatter.parseListingPrice(itemObj)
                    val rating = itemObj["rating"]?.jsonPrimitive?.doubleOrNull
                        ?: itemObj["avgRating"]?.jsonPrimitive?.doubleOrNull

                    val type = when (tab) {
                        SearchTab.SPACES -> "space"
                        SearchTab.ITEMS -> "item"
                        SearchTab.SERVICES -> "service"
                    }

                    SearchResultItem(
                        id = id,
                        title = title,
                        imageUrl = imageUrl,
                        location = location,
                        price = price,
                        rating = rating,
                        type = type
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse search result item")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse search results")
            emptyList()
        }
    }

    private fun JsonElement?.stringOrNull(): String? {
        val s = this?.jsonPrimitive?.contentOrNull?.trim()
        return s?.takeIf { it.isNotBlank() }
    }

    private fun findArray(root: JsonElement, vararg paths: List<String>): JsonArray? {
        for (path in paths) {
            val found = navigate(root, path)
            when (found) {
                is JsonArray -> return found
                is JsonObject -> {
                    found["items"]?.let { if (it is JsonArray) return it }
                    found["listings"]?.let { if (it is JsonArray) return it }
                    found["data"]?.let { if (it is JsonArray) return it }
                }
                else -> Unit
            }
        }
        return null
    }

    private fun navigate(root: JsonElement, path: List<String>): JsonElement? {
        var current: JsonElement? = root
        for (key in path) {
            val obj = current as? JsonObject ?: return null
            current = obj[key]
        }
        return current
    }

}

enum class SearchTab(val label: String) {
    SPACES("Spaces"),
    ITEMS("Items"),
    SERVICES("Services")
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val location: String?,
    val price: String?,
    val rating: Double?,
    val type: String
)

data class SearchUiState(
    val query: String = "",
    val selectedTab: SearchTab = SearchTab.SPACES,


    val selectedCategory: String? = null,
    val results: List<SearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
