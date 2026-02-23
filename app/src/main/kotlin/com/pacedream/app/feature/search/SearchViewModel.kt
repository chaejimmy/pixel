package com.pacedream.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
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
import kotlinx.serialization.json.JsonObject
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

        val queryParams = mutableMapOf<String, String?>()
        if (state.query.isNotBlank()) queryParams["q"] = state.query
        state.selectedCategory?.let { queryParams["category"] = it }

        val endpoint = when (state.selectedTab) {
            SearchTab.USE -> "properties/filter-rentable-items-by-group/time_based"
            SearchTab.BORROW -> "gear-rentals/get/hourly-rental-gear/tech_gear"
            SearchTab.SPLIT -> "roommate/get/room-stay"
        }

        val url = appConfig.buildApiUrl(
            endpoint,
            queryParams = queryParams
        )

        when (val result = apiClient.get(url, includeAuth = false)) {
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private fun parseSearchResults(responseBody: String, tab: SearchTab): List<SearchResultItem> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject

            val dataArray = obj["data"]?.let {
                when (it) {
                    is JsonArray -> it
                    is JsonObject -> it["items"]?.jsonArray ?: it["data"]?.jsonArray
                    else -> null
                }
            } ?: obj["items"]?.jsonArray ?: return emptyList()

            dataArray.mapNotNull { item ->
                try {
                    val itemObj = item.jsonObject
                    val id = itemObj["_id"]?.jsonPrimitive?.content
                        ?: itemObj["id"]?.jsonPrimitive?.content
                        ?: return@mapNotNull null

                    val title = itemObj["name"]?.jsonPrimitive?.content
                        ?: itemObj["title"]?.jsonPrimitive?.content
                        ?: "Listing"

                    val imageUrl = itemObj["images"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                        ?: itemObj["image"]?.jsonPrimitive?.content

                    val locationObj = itemObj["location"]
                    val location = when (locationObj) {
                        is JsonObject -> locationObj["city"]?.jsonPrimitive?.content
                        else -> locationObj?.jsonPrimitive?.content
                    }

                    val price = parsePrice(itemObj)
                    val rating = itemObj["rating"]?.jsonPrimitive?.doubleOrNull

                    val type = when (tab) {
                        SearchTab.USE -> "time-based"
                        SearchTab.BORROW -> "gear"
                        SearchTab.SPLIT -> "split-stay"
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

    private fun parsePrice(obj: JsonObject): String? {
        return try {
            obj["dynamic_price"]?.jsonArray?.firstOrNull()?.jsonObject?.let { price ->
                val priceValue = price["price"]?.jsonPrimitive?.doubleOrNull
                priceValue?.let { formatPrice(it, "hr") }
            }
            ?: obj["pricing"]?.jsonObject?.let { pricing ->
                val hourlyFrom = pricing["hourlyFrom"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["hourly_from"]?.jsonPrimitive?.doubleOrNull
                val basePrice = pricing["basePrice"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["base_price"]?.jsonPrimitive?.doubleOrNull
                val freq = pricing["frequencyLabel"]?.jsonPrimitive?.content
                    ?: pricing["frequency"]?.jsonPrimitive?.content
                val amount = hourlyFrom ?: basePrice
                amount?.let { formatPrice(it, freq?.lowercase() ?: "hr") }
            }
            ?: obj["price"]?.let { price ->
                when (price) {
                    is JsonObject -> {
                        val amount = price["amount"]?.jsonPrimitive?.doubleOrNull
                        amount?.let { formatPrice(it, "hr") }
                    }
                    else -> {
                        val priceValue = price.jsonPrimitive.doubleOrNull
                        priceValue?.let { formatPrice(it, "hr") }
                    }
                }
            }
        } catch (_: Exception) { null }
    }

    private fun formatPrice(amount: Double, unit: String): String {
        val formatted = if (amount == amount.toInt().toDouble()) amount.toInt().toString()
        else "%.2f".format(amount).trimEnd('0').trimEnd('.')
        return "$$formatted/$unit"
    }
}

enum class SearchTab(val label: String) {
    USE("Use"),
    BORROW("Borrow"),
    SPLIT("Split")
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
    val selectedTab: SearchTab = SearchTab.USE,
    val selectedCategory: String? = null,
    val results: List<SearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
