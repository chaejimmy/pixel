package com.pacedream.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeSectionListViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SectionListUiState())
    val uiState: StateFlow<SectionListUiState> = _uiState.asStateFlow()
    
    companion object {
        private val SERVICE_SHARE_CATEGORIES = setOf(
            "HOME_HELP", "MOVING_HELP", "CLEANING_ORGANIZING", "EVERYDAY_HELP",
            "FITNESS", "LEARNING", "CREATIVE", "OTHER_SERVICE"
        )
        private val SERVICE_SUBCATEGORY_IDS = setOf(
            "home_help", "moving_help", "cleaning_organizing", "everyday_help",
            "fitness", "learning", "creative", "other_service"
        )
    }

    private var currentSectionType: String = ""
    
    fun loadSection(sectionType: String) {
        currentSectionType = sectionType
        fetchListings(sectionType)
    }
    
    fun refresh() {
        fetchListings(currentSectionType)
    }
    
    private fun fetchListings(sectionType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isRefreshing = true, error = null) }
            
            val url = when (sectionType) {
                "hourly-spaces" -> appConfig.buildApiUrl(
                    "properties", "filter-rentable-items-by-group", "time_based",
                    queryParams = mapOf("item_type" to "room")
                )
                "rent-gear" -> appConfig.buildApiUrl("gear-rentals", "get", "hourly-rental-gear", "tech_gear")
                "split-stays" -> appConfig.buildApiUrl(
                    "listings",
                    queryParams = mapOf("shareType" to "SPLIT", "page" to "1", "limit" to "50")
                )
                "services" -> appConfig.buildApiUrl(
                    "poc", "listings",
                    queryParams = mapOf(
                        "shareType" to "USE",
                        "status" to "published",
                        "limit" to "50",
                        "skip_pagination" to "true"
                    )
                )
                else -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "Unknown section type") }
                    return@launch
                }
            }
            
            when (val result = apiClient.get(url, includeAuth = false)) {
                is ApiResult.Success -> {
                    var items = parseListings(result.data, sectionType)
                    // For the services section, filter to only service subcategories
                    if (sectionType == "services") {
                        items = items.filter { item ->
                            item.shareCategory in SERVICE_SHARE_CATEGORIES
                                || item.subCategory?.lowercase() in SERVICE_SUBCATEGORY_IDS
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = items,
                            error = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }
    
    private fun parseListings(responseBody: String, type: String): List<HomeListingItem> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            val dataArray = obj["data"]?.jsonArray
                ?: obj["results"]?.jsonArray
                ?: obj["items"]?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("items")?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("results")?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("listings")?.jsonArray
                ?: return emptyList()
            
            dataArray.mapNotNull { item ->
                try {
                    val itemObj = item.jsonObject
                    HomeListingItem(
                        id = itemObj["_id"]?.jsonPrimitive?.content
                            ?: itemObj["id"]?.jsonPrimitive?.content
                            ?: return@mapNotNull null,
                        title = itemObj["name"]?.jsonPrimitive?.content
                            ?: itemObj["title"]?.jsonPrimitive?.content
                            ?: "Listing",
                        imageUrl = itemObj["images"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                            ?: itemObj["primaryImage"]?.jsonPrimitive?.content
                            ?: itemObj["image"]?.jsonPrimitive?.content,
                        location = itemObj["location"]?.let { loc ->
                            when (loc) {
                                is JsonObject -> {
                                    val city = loc["city"]?.jsonPrimitive?.content
                                    val state = loc["state"]?.jsonPrimitive?.content
                                    listOfNotNull(city, state).joinToString(", ").ifBlank { null }
                                }
                                else -> runCatching { loc.jsonPrimitive.content }.getOrNull()
                            }
                        },
                        price = parsePrice(itemObj),
                        rating = itemObj["rating"]?.jsonPrimitive?.doubleOrNull,
                        type = type,
                        shareCategory = (itemObj["shareCategory"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                        subCategory = (itemObj["subCategory"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: (itemObj["roomType"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: (itemObj["listing_type"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse listing item")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse listings response")
            emptyList()
        }
    }
    
    private fun parsePrice(obj: JsonObject): String? {
        return try {
            // Try dynamic_price first (for hourly spaces)
            obj["dynamic_price"]?.jsonArray?.firstOrNull()?.jsonObject?.let { price ->
                val priceValue = price["price"]?.jsonPrimitive?.doubleOrNull
                    ?: price["price"]?.jsonPrimitive?.content?.toDoubleOrNull()
                priceValue?.let { formatPrice(it, "hr") }
            }
            // Try pricing object
            ?: obj["pricing"]?.jsonObject?.let { pricing ->
                val hourlyFrom = pricing["hourlyFrom"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["hourly_from"]?.jsonPrimitive?.doubleOrNull
                val basePrice = pricing["basePrice"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["base_price"]?.jsonPrimitive?.doubleOrNull
                val frequency = pricing["frequencyLabel"]?.jsonPrimitive?.content
                    ?: pricing["frequency"]?.jsonPrimitive?.content
                    ?: pricing["unit"]?.jsonPrimitive?.content
                
                val amount = hourlyFrom ?: basePrice
                val unit = frequency?.lowercase()?.takeIf { it.isNotBlank() } ?: "hr"
                amount?.let { formatPrice(it, unit) }
            }
            // Try price object
            ?: obj["price"]?.let { price ->
                when (price) {
                    is JsonObject -> {
                        val amount = price["amount"]?.jsonPrimitive?.doubleOrNull
                            ?: price["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        val unit = price["unit"]?.jsonPrimitive?.content?.lowercase() ?: "hr"
                        amount?.let { formatPrice(it, unit) }
                    }
                    else -> {
                        val prim = price as? kotlinx.serialization.json.JsonPrimitive
                        val priceValue = prim?.doubleOrNull
                            ?: prim?.content?.toDoubleOrNull()
                        priceValue?.let { formatPrice(it, "hr") }
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format price to match iOS format: "$12/hr" (no spaces, lowercase unit)
     */
    private fun formatPrice(amount: Double, unit: String): String {
        val formattedAmount = if (amount == amount.toInt().toDouble()) {
            amount.toInt().toString()
        } else {
            "%.2f".format(amount).trimEnd('0').trimEnd('.')
        }
        val normalizedUnit = unit.lowercase().trim()
        return "$$formattedAmount/$normalizedUnit"
    }
}

data class SectionListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<HomeListingItem> = emptyList(),
    val error: String? = null
)


