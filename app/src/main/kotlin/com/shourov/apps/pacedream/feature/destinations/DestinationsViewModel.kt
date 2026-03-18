package com.shourov.apps.pacedream.feature.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.home.presentation.components.DestinationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

data class DestinationsUiState(
    val isLoading: Boolean = true,
    val popularDestinations: List<DestinationItem> = emptyList(),
    val allDestinations: List<DestinationItem> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class DestinationsViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val _state = MutableStateFlow(DestinationsUiState())
    val state: StateFlow<DestinationsUiState> = _state.asStateFlow()

    init {
        loadDestinations()
    }

    fun retry() = loadDestinations()

    private fun loadDestinations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            // Try popular destinations endpoint first
            val popularUrl = appConfig.buildApiUrl("properties", "destinations")
            val allUrl = appConfig.buildApiUrl("destinations")

            val popularResult = apiClient.get(popularUrl, includeAuth = false)
            val allResult = apiClient.get(allUrl, includeAuth = false)

            val popular = when (popularResult) {
                is ApiResult.Success -> parseDestinations(popularResult.data, isPopular = true)
                is ApiResult.Failure -> {
                    Timber.w("Failed to load popular destinations: ${popularResult.error}")
                    emptyList()
                }
            }

            val all = when (allResult) {
                is ApiResult.Success -> parseDestinations(allResult.data, isPopular = false)
                is ApiResult.Failure -> {
                    Timber.w("Failed to load all destinations: ${allResult.error}")
                    emptyList()
                }
            }

            if (popular.isEmpty() && all.isEmpty() && popularResult is ApiResult.Failure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Unable to load destinations. Please try again."
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    popularDestinations = popular,
                    allDestinations = all,
                    errorMessage = null
                )
            }
        }
    }

    private fun parseDestinations(raw: String, isPopular: Boolean): List<DestinationItem> {
        return try {
            val root = json.parseToJsonElement(raw)

            // Handle multiple response shapes: { data: [...] }, { destinations: [...] }, or [...]
            val items: JsonArray = when {
                root is JsonArray -> root
                root is JsonObject -> {
                    root.jsonObject["data"]?.jsonArray
                        ?: root.jsonObject["destinations"]?.jsonArray
                        ?: root.jsonObject["results"]?.jsonArray
                        ?: return emptyList()
                }
                else -> return emptyList()
            }

            items.mapNotNull { element ->
                val obj = element.jsonObject
                val name = obj["name"]?.jsonPrimitive?.contentOrNull
                    ?: obj["city"]?.jsonPrimitive?.contentOrNull
                    ?: obj["title"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null

                DestinationItem(
                    name = name,
                    propertyCount = obj["propertyCount"]?.jsonPrimitive?.intOrNull
                        ?: obj["property_count"]?.jsonPrimitive?.intOrNull
                        ?: obj["listingCount"]?.jsonPrimitive?.intOrNull
                        ?: obj["listing_count"]?.jsonPrimitive?.intOrNull,
                    imageUrl = obj["imageUrl"]?.jsonPrimitive?.contentOrNull
                        ?: obj["image_url"]?.jsonPrimitive?.contentOrNull
                        ?: obj["image"]?.jsonPrimitive?.contentOrNull,
                    isPopular = isPopular
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse destinations response")
            emptyList()
        }
    }
}
