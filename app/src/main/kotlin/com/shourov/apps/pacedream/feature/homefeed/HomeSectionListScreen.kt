package com.shourov.apps.pacedream.feature.homefeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.core.network.api.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class HomeSectionListViewModel @Inject constructor(
    private val repo: HomeFeedRepository
) : ViewModel() {
    var isRefreshing by mutableStateOf(false)
        private set

    var items by mutableStateOf<List<HomeCard>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var page1 by mutableStateOf(1)
        private set

    var hasMore by mutableStateOf(true)
        private set

    fun refresh(section: HomeSectionKey) {
        viewModelScope.launch {
            isRefreshing = true
            page1 = 1
            load(section, reset = true)
            isRefreshing = false
        }
    }

    fun loadMore(section: HomeSectionKey) {
        if (!hasMore) return
        viewModelScope.launch {
            load(section, reset = false)
        }
    }

    private suspend fun load(section: HomeSectionKey, reset: Boolean) {
        errorMessage = null
        val nextPage = if (reset) 1 else (page1 + 1)

        // Match the home feed data source per section:
        // - SPACES uses the curated hourly endpoint (shareType=USE on /poc/listings)
        // - ITEMS uses shareType=BORROW
        // - SERVICES uses shareType=SHARE with client-side filtering
        val res = when (section) {
            HomeSectionKey.SPACES -> {
                if (nextPage == 1) {
                    // First page: use curated endpoint like the home feed
                    repo.getCuratedHourly(limit = 24)
                } else {
                    // Subsequent pages: paginate via standard listings
                    repo.getListingsShareTypePage(shareType = "USE", page1 = nextPage, limit = 24)
                }
            }
            else -> {
                val shareType = section.shareType ?: return
                repo.getListingsShareTypePage(shareType = shareType, page1 = nextPage, limit = 24)
            }
        }

        when (res) {
            is ApiResult.Success -> {
                // Client-side filtering: SERVICES and SPACES both hit shareType=SHARE,
                // so separate them by subcategory (same logic as HomeFeedViewModel).
                val filtered = when (section) {
                    HomeSectionKey.SERVICES -> res.data.filter {
                        it.subCategory?.lowercase() in SERVICE_SUBCATEGORY_IDS
                    }
                    HomeSectionKey.SPACES -> res.data.filter {
                        it.subCategory?.lowercase() !in SERVICE_SUBCATEGORY_IDS
                    }
                    else -> res.data
                }
                page1 = nextPage
                items = if (reset) filtered else (items + filtered)
                hasMore = res.data.size >= 24
            }
            is ApiResult.Failure -> {
                hasMore = false
                errorMessage = res.error.message ?: "Failed to load"
            }
        }
    }

    companion object {
        private val SERVICE_SUBCATEGORY_IDS = setOf(
            "home_help", "moving_help", "cleaning_organizing", "everyday_help",
            "fitness", "learning", "creative",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSectionListScreen(
    section: HomeSectionKey,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeSectionListViewModel = hiltViewModel()
) {
    LaunchedEffect(section) {
        viewModel.refresh(section)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(section.displayTitle, style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = com.pacedream.common.icon.PaceDreamIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refresh(section) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaceDreamColors.Background),
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                items(viewModel.items, key = { it.id }) { item ->
                    Card(
                        onClick = { onListingClick(item.id) },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        shape = RoundedCornerShape(com.pacedream.common.composables.theme.PaceDreamRadius.MD)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                        ) {
                            // Thumbnail image
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.imageUrl?.takeIf { it.isNotBlank() })
                                    .crossfade(200)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(
                                        topStart = com.pacedream.common.composables.theme.PaceDreamRadius.MD,
                                        bottomStart = com.pacedream.common.composables.theme.PaceDreamRadius.MD
                                    ))
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(PaceDreamSpacing.MD),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.title,
                                    style = PaceDreamTypography.Headline,
                                    color = PaceDreamColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                item.location?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = PaceDreamTypography.Caption,
                                        color = PaceDreamColors.TextSecondary,
                                        maxLines = 1
                                    )
                                }
                                item.priceText?.let {
                                    Text(
                                        text = it,
                                        style = PaceDreamTypography.Callout,
                                        color = PaceDreamColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                viewModel.errorMessage?.let { msg ->
                    item {
                        Text(msg, style = PaceDreamTypography.Body, color = PaceDreamColors.Error)
                    }
                }

                if (viewModel.hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaceDreamSpacing.MD),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PaceDreamColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        LaunchedEffect(Unit) { viewModel.loadMore(section) }
                    }
                } else {
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

